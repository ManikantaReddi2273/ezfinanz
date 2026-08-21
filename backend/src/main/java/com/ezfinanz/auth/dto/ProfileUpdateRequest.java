package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body to update the logged-in customer's display name. */
public record ProfileUpdateRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName
) {
}
