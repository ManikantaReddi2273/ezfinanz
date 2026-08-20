package com.ezfinanz.files;

import com.ezfinanz.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stores application files in Supabase Storage (used for both local and production).
 */
@Service
public class LocalFileStorage {

    private final RestClient restClient;
    private final String bucket;
    private final boolean configured;

    public LocalFileStorage(
            @Value("${app.supabase.url:}") String supabaseUrl,
            @Value("${app.supabase.key:}") String supabaseKey,
            @Value("${app.supabase.bucket:ezfinanz-files}") String bucket
    ) {
        this.bucket = bucket == null || bucket.isBlank() ? "ezfinanz-files" : bucket.trim();
        String base = supabaseUrl == null ? "" : supabaseUrl.trim().replaceAll("/+$", "");
        String key = supabaseKey == null ? "" : supabaseKey.trim();
        this.configured = !base.isBlank() && !key.isBlank();
        if (configured) {
            this.restClient = RestClient.builder()
                    .baseUrl(base + "/storage/v1")
                    .defaultHeader("Authorization", "Bearer " + key)
                    .defaultHeader("apikey", key)
                    .build();
        } else {
            this.restClient = null;
        }
    }

    public StoredFile saveKycDocument(Long userId, MultipartFile file) {
        return save(userId, "kyc", file, Set.of("jpg", "jpeg", "png", "webp", "pdf"), "ID document");
    }

    public StoredFile saveSelfie(Long userId, MultipartFile file) {
        return save(userId, "selfie", file, Set.of("jpg", "jpeg", "png", "webp"), "Selfie");
    }

    public StoredFile saveKnowledgeDoc(Long documentId, MultipartFile file) {
        return save(documentId, "knowledge", file, Set.of("txt", "md", "pdf"), "Knowledge document");
    }

    private StoredFile save(Long userId, String folderName, MultipartFile file, Set<String> allowed, String label) {
        requireConfigured();
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", label + " must be 5 MB or smaller.");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        if (ext.isBlank() && file.getContentType() != null) {
            String type = file.getContentType();
            ext = switch (type) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "application/pdf" -> "pdf";
                case "text/plain" -> "txt";
                case "text/markdown" -> "md";
                default -> "";
            };
        }
        if ("heic".equals(ext) || "heif".equals(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TYPE_INVALID", "Save or export the photo as JPG or PNG, then upload again.");
        }
        if (!allowed.contains(ext)) {
            String allowedLabel = allowed.contains("txt")
                    ? "Upload a TXT, MD, or PDF file."
                    : allowed.contains("pdf") && allowed.contains("jpg")
                    ? "Upload a JPG, PNG, WEBP, or PDF file."
                    : "Upload a JPG, PNG, or WEBP file.";
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TYPE_INVALID", allowedLabel);
        }
        String relativePath = folderName + "/" + userId + "/" + UUID.randomUUID() + "." + ext;
        try {
            upload(relativePath, file.getBytes(), contentType(ext, file.getContentType()));
            return new StoredFile(relativePath, original);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_SAVE_FAILED", "Could not store the " + label.toLowerCase() + ".");
        }
    }

    public void upload(String relativePath, byte[] bytes, String contentType) {
        requireConfigured();
        try {
            restClient.post()
                    .uri(objectUri(relativePath))
                    .contentType(MediaType.parseMediaType(contentType == null || contentType.isBlank()
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                            : contentType))
                    .header("x-upsert", "true")
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_SAVE_FAILED",
                    "Could not store file in Supabase Storage (" + ex.getStatusCode().value() + ")."
            );
        }
    }

    public byte[] readBytes(String relativePath) {
        requireConfigured();
        try {
            byte[] body = restClient.get()
                    .uri(objectUri(relativePath))
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "The file is missing.");
            }
            return body;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "The file is missing.");
            }
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_READ_FAILED",
                    "Could not read file from Supabase Storage."
            );
        }
    }

    public Resource asResource(String relativePath, String filename) {
        byte[] bytes = readBytes(relativePath);
        String name = filename == null || filename.isBlank() ? relativePath : filename;
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    public boolean exists(String relativePath) {
        if (!configured || relativePath == null || relativePath.isBlank()) {
            return false;
        }
        try {
            readBytes(relativePath);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    public void delete(String relativePath) {
        if (!configured || relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            restClient.delete()
                    .uri(objectUri(relativePath))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ignored) {
            // Best-effort cleanup
        }
    }

    private String objectUri(String relativePath) {
        StringBuilder uri = new StringBuilder("/object/").append(bucket);
        for (String part : normalize(relativePath).split("/")) {
            if (part.isBlank()) {
                continue;
            }
            uri.append('/').append(URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return uri.toString();
    }

    private void requireConfigured() {
        if (!configured) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "STORAGE_NOT_CONFIGURED",
                    "Supabase Storage is not configured. Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY (or SUPABASE_ANON_KEY)."
            );
        }
    }

    private static String normalize(String relativePath) {
        String path = relativePath.replace('\\', '/').replaceAll("^/+", "");
        if (path.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_INVALID", "Invalid file path.");
        }
        return path;
    }

    private static String contentType(String ext, String fallback) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            default -> fallback == null || fallback.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : fallback;
        };
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String relativePath, String originalName) {
    }
}
