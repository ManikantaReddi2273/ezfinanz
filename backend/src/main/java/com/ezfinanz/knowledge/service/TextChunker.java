package com.ezfinanz.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits knowledge-document text into overlapping chunks for OpenAI embeddings and Pinecone RAG storage.
 */
@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP = 150;

    /**
     * Chunks text into ~800-character segments with overlap, preferring breaks at newlines or spaces.
     */
    public List<String> chunk(String text) {
        if (text == null) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= CHUNK_SIZE) {
            return List.of(normalized);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            if (end < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', end);
                if (breakAt <= start + (CHUNK_SIZE / 2)) {
                    breakAt = normalized.lastIndexOf(' ', end);
                }
                if (breakAt > start + (CHUNK_SIZE / 2)) {
                    end = breakAt;
                }
            }
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - OVERLAP, start + 1);
        }
        return chunks;
    }
}
