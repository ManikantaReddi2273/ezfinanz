package com.ezfinanz.knowledge.domain;

/**
 * Lifecycle of a knowledge document in the RAG pipeline: awaiting index, successfully indexed, or failed.
 */
public enum KnowledgeDocumentStatus {
    PENDING,
    INDEXED,
    FAILED
}
