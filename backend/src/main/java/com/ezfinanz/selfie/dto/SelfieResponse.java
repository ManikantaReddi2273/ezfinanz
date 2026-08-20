package com.ezfinanz.selfie.dto;

import com.ezfinanz.application.ApplicationStage;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;

import java.time.Instant;

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
