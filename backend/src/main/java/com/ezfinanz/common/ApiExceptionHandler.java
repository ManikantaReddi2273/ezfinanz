package com.ezfinanz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global REST exception mapper that converts failures into consistent {@link ErrorResponse} JSON.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Maps intentional {@link ApiException}s to their declared status and code. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        if (ex.getStatus().is5xxServerError() || ex.getStatus().value() == 502) {
            log.error("API error {}: {}", ex.getCode(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    /** Returns the first field validation error for request-body binding failures. */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        String message = "Validation failed";
        if (ex instanceof MethodArgumentNotValidException manv && !manv.getBindingResult().getFieldErrors().isEmpty()) {
            message = manv.getBindingResult().getFieldErrors().get(0).getField()
                    + ": " + manv.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException bind && !bind.getBindingResult().getFieldErrors().isEmpty()) {
            message = bind.getBindingResult().getFieldErrors().get(0).getField()
                    + ": " + bind.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /** Handles constraint violations from method-level {@code @Validated} parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    /** Maps Spring Security bad-credentials failures to a client-safe 401. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    /** Translates unique-constraint / DB integrity conflicts into friendly conflict messages. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Database constraint error", ex);
        String detail = rootMessage(ex);
        String lower = detail.toLowerCase();
        if (lower.contains("selfie_submissions") || lower.contains("review_status")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(
                            "SELFIE_SAVE_CONFLICT",
                            "Could not save the selfie. Please retake the photo and try Confirm Selfie again."
                    ));
        }
        if (lower.contains("email") || lower.contains("phone") || lower.contains("users")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(
                            "DATA_CONFLICT",
                            "This email or phone number is already registered to another account."
                    ));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "DATA_CONFLICT",
                        "Could not save this change because it conflicts with existing data."
                ));
    }

    /** Handles unknown API paths. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "This API endpoint was not found."));
    }

    /** Handles HTTP methods not allowed on an endpoint. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("METHOD_NOT_ALLOWED", "This action is not supported."));
    }

    /** Handles missing required query/form parameters. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Missing field: " + ex.getParameterName()));
    }

    /** Catch-all for unexpected errors; logs and returns a generic 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unhandled error", ex);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Something went wrong";
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", message));
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
