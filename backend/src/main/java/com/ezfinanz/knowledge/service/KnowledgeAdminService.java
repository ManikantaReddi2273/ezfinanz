package com.ezfinanz.knowledge.service;

import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.files.LocalFileStorage;
import com.ezfinanz.knowledge.domain.KnowledgeDocument;
import com.ezfinanz.knowledge.domain.KnowledgeDocumentStatus;
import com.ezfinanz.knowledge.dto.KnowledgeDocumentResponse;
import com.ezfinanz.knowledge.repo.KnowledgeDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeAdminService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final UserRepository userRepository;
    private final LocalFileStorage fileStorage;
    private final KnowledgeIndexService knowledgeIndexService;
    private final String superAdminEmail;

    public KnowledgeAdminService(
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            UserRepository userRepository,
            LocalFileStorage fileStorage,
            KnowledgeIndexService knowledgeIndexService,
            @Value("${app.admin.email:campusworks2273@gmail.com}") String superAdminEmail
    ) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.knowledgeIndexService = knowledgeIndexService;
        this.superAdminEmail = superAdminEmail.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> list(Long actorId) {
        requireSuperAdmin(actorId);
        return knowledgeDocumentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(KnowledgeDocumentResponse::from)
                .toList();
    }

    @Transactional
    public KnowledgeDocumentResponse upload(Long actorId, MultipartFile file, String title) {
        requireSuperAdmin(actorId);
        knowledgeIndexService.requireReady();
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED", "Choose a document to upload.");
        }
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String resolvedTitle = title == null || title.isBlank()
                ? stripExtension(originalName)
                : title.trim();
        if (resolvedTitle.length() > 200) {
            resolvedTitle = resolvedTitle.substring(0, 200);
        }

        KnowledgeDocument row = new KnowledgeDocument();
        row.setTitle(resolvedTitle);
        row.setOriginalName(originalName);
        row.setContentType(file.getContentType());
        row.setStatus(KnowledgeDocumentStatus.PENDING);
        row.setUploadedByUserId(actorId);
        row.setStoredPath("pending");
        knowledgeDocumentRepository.save(row);

        LocalFileStorage.StoredFile stored = fileStorage.saveKnowledgeDoc(row.getId(), file);
        row.setStoredPath(stored.relativePath());
        row.setOriginalName(stored.originalName());
        knowledgeDocumentRepository.save(row);

        try {
            knowledgeIndexService.index(row);
        } catch (ApiException ex) {
            row.setStatus(KnowledgeDocumentStatus.FAILED);
            row.setErrorMessage(ex.getMessage());
            knowledgeDocumentRepository.save(row);
            throw ex;
        } catch (Exception ex) {
            row.setStatus(KnowledgeDocumentStatus.FAILED);
            row.setErrorMessage("Indexing failed. Check OpenAI and Pinecone configuration.");
            knowledgeDocumentRepository.save(row);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "INDEX_FAILED",
                    "Could not index this document. Check OpenAI and Pinecone keys."
            );
        }
        return KnowledgeDocumentResponse.from(knowledgeDocumentRepository.save(row));
    }

    @Transactional
    public void delete(Long actorId, Long documentId) {
        requireSuperAdmin(actorId);
        KnowledgeDocument row = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found."));
        try {
            knowledgeIndexService.deleteVectors(documentId);
        } catch (ApiException ex) {
            // Still remove local record so admin can clean up; surface config errors clearly
            if ("SERVICE_NOT_CONFIGURED".equals(ex.getCode())) {
                // allow local delete when pinecone not configured
            } else {
                throw ex;
            }
        }
        try {
            Files.deleteIfExists(fileStorage.resolve(row.getStoredPath()));
        } catch (Exception ignored) {
            // ignore file cleanup failures
        }
        knowledgeDocumentRepository.delete(row);
    }

    private void requireSuperAdmin(Long actorId) {
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        if (user.getEmail() == null || !superAdminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "SUPERADMIN_REQUIRED",
                    "Only the super admin can manage the knowledge base."
            );
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return name;
        }
        return name.substring(0, dot);
    }
}
