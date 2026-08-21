package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for email/password login. */
public record LoginEmailRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
