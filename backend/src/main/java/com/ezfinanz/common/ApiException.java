package com.ezfinanz.common;

import org.springframework.http.HttpStatus;

/**
 * Domain/API error with an HTTP status and machine-readable code for clients.
 * Thrown by services and mapped to {@link ErrorResponse} by {@link ApiExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * @param status HTTP status to return
     * @param code   stable error code (e.g. {@code EMAIL_ALREADY_REGISTERED})
     * @param message user-facing message
     */
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
