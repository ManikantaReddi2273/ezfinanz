package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Email;

/** Optional email for logged-in verification; omit to use the account's existing email. */
public record OptionalEmailRequest(
        @Email String email
) {
}
