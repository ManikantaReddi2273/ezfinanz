package com.ezfinanz.admin.dto;

/** Optional message included when an admin approves a selfie review. */
public class ApplicationReviewRequest {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
