package com.ezfinanz.knowledge.dto;

import com.ezfinanz.knowledge.domain.KnowledgeDocument;
import com.ezfinanz.knowledge.domain.KnowledgeDocumentStatus;

import java.time.Instant;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        String originalName,
        String contentType,
        KnowledgeDocumentStatus status,
        int chunkCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument row) {
        return new KnowledgeDocumentResponse(
                row.getId(),
                row.getTitle(),
                row.getOriginalName(),
                row.getContentType(),
                row.getStatus(),
                row.getChunkCount(),
                row.getErrorMessage(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
