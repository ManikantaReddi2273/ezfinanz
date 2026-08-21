package com.ezfinanz.knowledge.service;

import com.ezfinanz.ai.OpenAiClient;
import com.ezfinanz.ai.PineconeClient;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.files.LocalFileStorage;
import com.ezfinanz.knowledge.domain.KnowledgeDocument;
import com.ezfinanz.knowledge.domain.KnowledgeDocumentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes knowledge documents into Pinecone for RAG: extract text, chunk, embed via OpenAI, upsert vectors.
 * Reads source files from Supabase Storage via {@link LocalFileStorage}.
 */
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

    /** Ensures OpenAI and Pinecone are configured before indexing. */
    public void requireReady() {
        openAiClient.requireConfigured();
        pineconeClient.requireConfigured();
    }

    /**
     * Builds and upserts RAG vectors for a document, then marks it {@link KnowledgeDocumentStatus#INDEXED}.
     */
    public void index(KnowledgeDocument document) {
        requireReady();
        byte[] bytes = fileStorage.readBytes(document.getStoredPath());
        String text = textExtractor.extract(bytes, document.getOriginalName());
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

    /** Removes all Pinecone vectors for a knowledge document (no-op if Pinecone is not configured). */
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
