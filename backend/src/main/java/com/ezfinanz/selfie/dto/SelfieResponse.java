package com.ezfinanz.selfie.dto;

import com.ezfinanz.application.ApplicationStage;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;

import java.time.Instant;

/** API view of selfie review status, disbursement flags, and application stage. */
public record SelfieResponse(
        boolean submitted,
        SelfieReviewStatus reviewStatus,
        String rejectionReason,
        Instant submittedAt,
        boolean disbursed,
        Instant disbursedAt,
        ApplicationStage applicationStage,
        String applicationStageLabel
) {
    /** Maps a selfie entity plus resolved stage into the API response. */
    public static SelfieResponse from(SelfieSubmission row, ApplicationStage stage) {
        return new SelfieResponse(
                true,
                row.getReviewStatus(),
                row.getRejectionReason(),
                row.getSubmittedAt(),
                row.isDisbursed(),
                row.getDisbursedAt(),
                stage,
                stage.getLabel()
        );
    }
}
