package com.ezfinanz.eligibility.dto;

import com.ezfinanz.eligibility.domain.CreditBand;
import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import com.ezfinanz.eligibility.domain.IncomeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** API view of an eligibility assessment, including DTI percent and reason list. */
public record EligibilityResponse(
        IncomeType incomeType,
        BigDecimal incomeAmount,
        BigDecimal requestedLoanAmount,
        int creditScore,
        BigDecimal outstandingDebts,
        String employerName,
        String designation,
        BigDecimal monthlyIncome,
        BigDecimal annualIncome,
        BigDecimal dtiRatio,
        BigDecimal dtiPercent,
        CreditBand creditBand,
        BigDecimal maxEligibleAmount,
        EligibilityResult result,
        List<String> reasons,
        Instant assessedAt
) {
    /** Maps a persisted assessment to the API response shape. */
    public static EligibilityResponse from(EligibilityAssessment row) {
        BigDecimal dtiPercent = row.getDtiRatio().multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
        List<String> reasons = row.getReasons() == null || row.getReasons().isBlank()
                ? List.of()
                : Arrays.stream(row.getReasons().split("\n")).filter(s -> !s.isBlank()).toList();
        return new EligibilityResponse(
                row.getIncomeType(),
                row.getIncomeAmount(),
                row.getRequestedLoanAmount(),
                row.getCreditScore(),
                row.getOutstandingDebts(),
                row.getEmployerName(),
                row.getDesignation(),
                row.getMonthlyIncome(),
                row.getAnnualIncome(),
                row.getDtiRatio(),
                dtiPercent,
                row.getCreditBand(),
                row.getMaxEligibleAmount(),
                row.getResult(),
                reasons,
                row.getAssessedAt()
        );
    }
}
