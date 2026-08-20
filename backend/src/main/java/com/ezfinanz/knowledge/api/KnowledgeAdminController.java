package com.ezfinanz.knowledge.api;

import com.ezfinanz.knowledge.dto.KnowledgeDocumentResponse;
import com.ezfinanz.knowledge.service.KnowledgeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge/documents")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Knowledge base", description = "Super admin uploads docs for the support chatbot")
public class KnowledgeAdminController {

    private final KnowledgeAdminService knowledgeAdminService;

    public KnowledgeAdminController(KnowledgeAdminService knowledgeAdminService) {
        this.knowledgeAdminService = knowledgeAdminService;
    }

    @GetMapping
    @Operation(summary = "List knowledge documents")
    public List<KnowledgeDocumentResponse> list(Authentication authentication) {
        return knowledgeAdminService.list((Long) authentication.getPrincipal());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload and index a knowledge document")
    public KnowledgeDocumentResponse upload(
            Authentication authentication,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) {
        return knowledgeAdminService.upload((Long) authentication.getPrincipal(), file, title);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a knowledge document and its vectors")
    public void delete(Authentication authentication, @PathVariable Long documentId) {
        knowledgeAdminService.delete((Long) authentication.getPrincipal(), documentId);
    }
}
