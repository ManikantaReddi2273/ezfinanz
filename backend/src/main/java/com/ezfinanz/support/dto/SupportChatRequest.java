package com.ezfinanz.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportChatRequest(
        @NotBlank @Size(max = 1000) String message
) {
}
