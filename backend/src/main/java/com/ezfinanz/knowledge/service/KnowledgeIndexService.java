package com.ezfinanz.knowledge.service;

import com.ezfinanz.ai.OpenAiClient;
import com.ezfinanz.ai.PineconeClient;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.files.LocalFileStorage;
import com.ezfinanz.knowledge.domain.KnowledgeDocument;
import com.ezfinanz.knowledge.domain.KnowledgeDocumentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeIndexService {

    private final DocumentTextExtractor textExtractor;
    private final TextChunker textChunker;
    private final OpenAiClient openAiClient;
    private final PineconeClient pineconeClient;
    private final LocalFileStorage fileStorage;

    public KnowledgeIndexService(
            DocumentTextExtractor textExtractor,
            TextChunker textChunker,
            OpenAiClient openAiClient,
            PineconeClient pineconeClient,
            LocalFileStorage fileStorage
    ) {
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.openAiClient = openAiClient;
        this.pineconeClient = pineconeClient;
        this.fileStorage = fileStorage;
    }

    public void requireReady() {
        openAiClient.requireConfigured();
        pineconeClient.requireConfigured();
    }

    public void index(KnowledgeDocument document) {
        requireReady();
        Path path = fileStorage.resolve(document.getStoredPath());
        String text = textExtractor.extract(path, document.getOriginalName());
        List<String> chunks = textChunker.chunk(text);
        if (chunks.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_DOCUMENT",
                    "This document has no readable text to index."
            );
        }
        List<float[]> embeddings = openAiClient.embed(chunks);
        List<PineconeClient.VectorRecord> vectors = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", document.getId());
            metadata.put("title", document.getTitle());
            metadata.put("chunkIndex", i);
            metadata.put("text", truncate(chunks.get(i), 3500));
            vectors.add(new PineconeClient.VectorRecord(
                    document.getId() + "-" + i,
                    embeddings.get(i),
                    metadata
            ));
        }
        pineconeClient.upsert(vectors);
        document.setChunkCount(chunks.size());
        document.setStatus(KnowledgeDocumentStatus.INDEXED);
        document.setErrorMessage(null);
    }

    public void deleteVectors(Long documentId) {
        if (pineconeClient.isConfigured()) {
            pineconeClient.deleteByDocumentId(documentId);
        }
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
