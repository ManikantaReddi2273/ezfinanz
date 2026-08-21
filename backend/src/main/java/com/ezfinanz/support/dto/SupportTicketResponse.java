package com.ezfinanz.support.dto;

import com.ezfinanz.support.domain.SupportTicket;

import java.time.Instant;

/**
 * API view of a support ticket returned to the customer.
 */
public record SupportTicketResponse(
        Long id,
        String subject,
        String message,
        Instant createdAt
) {
    /** Maps a persisted ticket entity to the API response. */
    public static SupportTicketResponse from(SupportTicket row) {
        return new SupportTicketResponse(row.getId(), row.getSubject(), row.getMessage(), row.getCreatedAt());
    }
}
