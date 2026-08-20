package com.ezfinanz.files;

import com.ezfinanz.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.files.directory:uploads}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
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
        try {
            Path folder = root.resolve(folderName).resolve(String.valueOf(userId));
            Files.createDirectories(folder);
            String storedName = UUID.randomUUID() + "." + ext;
            Path target = folder.resolve(storedName);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(root.relativize(target).toString().replace('\\', '/'), original);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_SAVE_FAILED", "Could not store the " + label.toLowerCase() + ".");
        }
    }

    public Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_INVALID", "Invalid file path.");
        }
        return path;
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
