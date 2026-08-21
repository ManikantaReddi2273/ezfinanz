package com.ezfinanz.knowledge.repo;

import com.ezfinanz.knowledge.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA access for RAG knowledge-document metadata rows.
 */
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    /** Returns all knowledge documents ordered by upload time, newest first. */
    List<KnowledgeDocument> findAllByOrderByCreatedAtDesc();
}
