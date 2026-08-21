package com.ezfinanz.support.api;

import com.ezfinanz.support.dto.SupportChatRequest;
import com.ezfinanz.support.dto.SupportChatResponse;
import com.ezfinanz.support.dto.SupportTicketRequest;
import com.ezfinanz.support.dto.SupportTicketResponse;
import com.ezfinanz.support.service.SupportChatService;
import com.ezfinanz.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer support REST API: human support tickets and the RAG-backed chatbot.
 */
@RestController
@RequestMapping("/api/support")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Support", description = "Customer help requests")
public class SupportController {

    private final SupportService supportService;
    private final SupportChatService supportChatService;

    public SupportController(SupportService supportService, SupportChatService supportChatService) {
        this.supportService = supportService;
        this.supportChatService = supportChatService;
    }

    /** Lists the authenticated customer's support tickets (Contact Support history). */
    @GetMapping
    @Operation(summary = "List this customer's support messages")
    public List<SupportTicketResponse> list(Authentication authentication) {
        return supportService.list((Long) authentication.getPrincipal());
    }

    /** Creates a support ticket and notifies the support inbox by email. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a support message")
    public SupportTicketResponse create(Authentication authentication, @Valid @RequestBody SupportTicketRequest request) {
        return supportService.create((Long) authentication.getPrincipal(), request);
    }

    /** Asks the RAG support chatbot (Pinecone retrieval + OpenAI reply). */
    @PostMapping("/chat")
    @Operation(summary = "Ask the EZFINANZ support chatbot (RAG)")
    public SupportChatResponse chat(Authentication authentication, @Valid @RequestBody SupportChatRequest request) {
        return supportChatService.chat((Long) authentication.getPrincipal(), request.message());
    }
}
