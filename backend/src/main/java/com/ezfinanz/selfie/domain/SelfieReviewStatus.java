package com.ezfinanz.selfie.domain;

/** Review lifecycle of a live selfie: draft, pending review, approved, or rejected. */
public enum SelfieReviewStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED
}
