package com.ezfinanz.support.dto;

import com.ezfinanz.support.domain.SupportTicket;

import java.time.Instant;

public record SupportTicketResponse(
        Long id,
        String subject,
        String message,
        Instant createdAt
) {
    public static SupportTicketResponse from(SupportTicket row) {
        return new SupportTicketResponse(row.getId(), row.getSubject(), row.getMessage(), row.getCreatedAt());
    }
}
