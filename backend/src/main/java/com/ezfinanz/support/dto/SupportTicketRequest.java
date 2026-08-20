package com.ezfinanz.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketRequest(
        @NotBlank @Size(max = 160) String subject,
        @NotBlank @Size(max = 2000) String message
) {
}
