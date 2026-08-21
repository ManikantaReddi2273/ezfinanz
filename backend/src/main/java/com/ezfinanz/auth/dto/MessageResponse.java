package com.ezfinanz.auth.dto;

/** Simple success message returned by auth endpoints that do not issue a JWT. */
public record MessageResponse(String message) {
}
