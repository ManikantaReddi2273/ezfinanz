package com.ezfinanz.support.dto;

import java.util.List;

/**
 * RAG chatbot reply plus optional knowledge-document source titles used as context.
 */
public record SupportChatResponse(
        String reply,
        List<String> sources
) {
}
