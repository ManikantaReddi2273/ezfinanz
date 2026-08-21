package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Pattern;

/** Optional phone for logged-in verification; omit to use the account's existing phone. */
public record OptionalPhoneRequest(
        @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Enter a valid phone number")
        String phone
) {
}
