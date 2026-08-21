package com.ezfinanz.auth.dto;

/**
 * Successful auth payload: JWT, token type, and the authenticated {@link UserResponse}.
 */
public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    /** Builds a Bearer token response. */
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
