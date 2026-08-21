package com.ezfinanz.common;

/**
 * Standard error body returned by the API: a stable {@code code} plus a user-facing {@code message}.
 */
public record ErrorResponse(String code, String message) {
}
