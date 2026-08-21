package com.ezfinanz.admin.dto;

import com.ezfinanz.application.ApplicationStage;

import java.math.BigDecimal;
import java.time.Instant;

/** Compact admin list row summarizing a customer's application progress. */
public record AdminApplicationSummary(
        Long userId,
        String applicantName,
        String email,
        String phone,
        BigDecimal requestedLoanAmount,
        BigDecimal selectedLoanAmount,
        Integer tenureMonths,
        ApplicationStage currentStage,
        String currentStageLabel,
        Instant createdAt,
        Instant submittedAt
) {
}
