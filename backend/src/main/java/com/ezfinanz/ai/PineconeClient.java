package com.ezfinanz.ai;

import com.ezfinanz.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PineconeClient {

    private final String apiKey;
    private final String indexHost;
    private final String namespace;
    private final RestClient restClient;

    public PineconeClient(
            @Value("${app.pinecone.api-key:}") String apiKey,
            @Value("${app.pinecone.index-host:}") String indexHost,
            @Value("${app.pinecone.namespace:ezfinanz-support}") String namespace
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.indexHost = normalizeHost(indexHost);
        this.namespace = namespace == null || namespace.isBlank() ? "ezfinanz-support" : namespace.trim();
        this.restClient = this.indexHost.isBlank()
                ? null
                : RestClient.builder().baseUrl(this.indexHost).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !indexHost.isBlank() && restClient != null;
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SERVICE_NOT_CONFIGURED",
                    "Pinecone is not configured. Add app.pinecone.api-key and app.pinecone.index-host in application.properties."
            );
        }
    }

    public String namespace() {
        return namespace;
    }

    public void upsert(List<VectorRecord> vectors) {
        requireConfigured();
        if (vectors == null || vectors.isEmpty()) {
            return;
        }
        List<Map<String, Object>> payload = new ArrayList<>(vectors.size());
        for (VectorRecord vector : vectors) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", vector.id());
            row.put("values", toBoxed(vector.values()));
            row.put("metadata", vector.metadata());
            payload.add(row);
        }
        try {
            restClient.post()
                    .uri("/vectors/upsert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Api-Key", apiKey)
                    .body(Map.of(
                            "vectors", payload,
                            "namespace", namespace
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PINECONE_UPSERT_FAILED",
                    "Pinecone upsert failed: " + truncate(ex.getResponseBodyAsString())
            );
        }
    }

    @SuppressWarnings("unchecked")
    public List<Match> query(float[] values, int topK) {
        requireConfigured();
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Api-Key", apiKey)
                    .body(Map.of(
                            "vector", toBoxed(values),
                            "topK", topK,
                            "includeMetadata", true,
                            "namespace", namespace
                    ))
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("matches") instanceof List<?> matches)) {
                return List.of();
            }
            List<Match> result = new ArrayList<>();
            for (Object matchObj : matches) {
                if (!(matchObj instanceof Map<?, ?> match)) {
                    continue;
                }
                String id = match.get("id") == null ? "" : match.get("id").toString();
                double score = match.get("score") instanceof Number n ? n.doubleValue() : 0.0;
                Map<String, Object> metadata = match.get("metadata") instanceof Map<?, ?> meta
                        ? castMetadata(meta)
                        : Map.of();
                result.add(new Match(id, score, metadata));
            }
            return result;
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PINECONE_QUERY_FAILED",
                    "Pinecone query failed: " + truncate(ex.getResponseBodyAsString())
            );
        }
    }

    public void deleteByDocumentId(long documentId) {
        requireConfigured();
        try {
            restClient.post()
                    .uri("/vectors/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Api-Key", apiKey)
                    .body(Map.of(
                            "filter", Map.of("documentId", Map.of("$eq", documentId)),
                            "namespace", namespace
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PINECONE_DELETE_FAILED",
                    "Pinecone delete failed: " + truncate(ex.getResponseBodyAsString())
            );
        }
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String value = host.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static List<Float> toBoxed(float[] values) {
        List<Float> boxed = new ArrayList<>(values.length);
        for (float value : values) {
            boxed.add(value);
        }
        return boxed;
    }

    private static Map<String, Object> castMetadata(Map<?, ?> meta) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<?, ?> entry : meta.entrySet()) {
            if (entry.getKey() != null) {
                out.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return out;
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "unknown error";
        }
        return value.length() > 240 ? value.substring(0, 240) + "…" : value;
    }

    public record VectorRecord(String id, float[] values, Map<String, Object> metadata) {
    }

    public record Match(String id, double score, Map<String, Object> metadata) {
    }
}
