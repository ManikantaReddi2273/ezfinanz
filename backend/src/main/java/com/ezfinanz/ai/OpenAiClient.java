package com.ezfinanz.ai;

import com.ezfinanz.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;
    private final int embeddingDimensions;
    private final RestClient restClient;

    public OpenAiClient(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.chat-model:gpt-4o-mini}") String chatModel,
            @Value("${app.openai.embedding-model:text-embedding-3-small}") String embeddingModel,
            @Value("${app.openai.embedding-dimensions:1024}") int embeddingDimensions
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SERVICE_NOT_CONFIGURED",
                    "OpenAI is not configured. Add app.openai.api-key in application.properties."
            );
        }
    }

    @SuppressWarnings("unchecked")
    public List<float[]> embed(List<String> texts) {
        requireConfigured();
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", embeddingModel);
            body.put("input", texts);
            if (embeddingDimensions > 0) {
                body.put("dimensions", embeddingDimensions);
            }
            Map<String, Object> response = restClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("data") instanceof List<?> data)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_EMBED_FAILED", "OpenAI returned an empty embedding response.");
            }
            List<float[]> vectors = new ArrayList<>(data.size());
            for (Object rowObj : data) {
                if (!(rowObj instanceof Map<?, ?> row) || !(row.get("embedding") instanceof List<?> embedding)) {
                    continue;
                }
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = ((Number) embedding.get(i)).floatValue();
                }
                vectors.add(vector);
            }
            if (vectors.size() != texts.size()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_EMBED_FAILED", "OpenAI embedding count did not match input.");
            }
            return vectors;
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_EMBED_FAILED",
                    "OpenAI embeddings failed: " + truncate(ex.getResponseBodyAsString())
            );
        }
    }

    public float[] embedOne(String text) {
        return embed(List.of(text)).getFirst();
    }

    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userMessage) {
        requireConfigured();
        try {
            Map<String, Object> body = Map.of(
                    "model", chatModel,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    )
            );
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_CHAT_FAILED", "OpenAI returned an empty chat response.");
            }
            Object first = choices.getFirst();
            if (!(first instanceof Map<?, ?> choice) || !(choice.get("message") instanceof Map<?, ?> message)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_CHAT_FAILED", "OpenAI chat response was malformed.");
            }
            Object content = message.get("content");
            if (content == null || content.toString().isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_CHAT_FAILED", "OpenAI returned an empty reply.");
            }
            return content.toString().trim();
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_CHAT_FAILED",
                    "OpenAI chat failed: " + truncate(ex.getResponseBodyAsString())
            );
        }
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "unknown error";
        }
        return value.length() > 240 ? value.substring(0, 240) + "…" : value;
    }
}
