package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body carrying a Google ID token for client-side Google sign-in. */
public record GoogleLoginRequest(@NotBlank String idToken) {
}
