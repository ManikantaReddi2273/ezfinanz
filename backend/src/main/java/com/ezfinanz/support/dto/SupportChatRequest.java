package com.ezfinanz.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for a RAG support-chatbot question.
 */
public record SupportChatRequest(
        @NotBlank @Size(max = 1000) String message
) {
}
