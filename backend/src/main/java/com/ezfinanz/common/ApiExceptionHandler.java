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

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        if (ex.getStatus().is5xxServerError() || ex.getStatus().value() == 502) {
            log.error("API error {}: {}", ex.getCode(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Database constraint error", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "DATA_CONFLICT",
                        "This email or phone number is already registered to another account."
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "This API endpoint was not found."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("METHOD_NOT_ALLOWED", "This action is not supported."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Missing field: " + ex.getParameterName()));
    }

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
}
