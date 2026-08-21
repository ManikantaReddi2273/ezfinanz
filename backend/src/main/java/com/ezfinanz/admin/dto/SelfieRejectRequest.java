package com.ezfinanz.admin.dto;

/** Optional rejection reason when an admin rejects a selfie. */
public class SelfieRejectRequest {

    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
