package com.ezfinanz.customer.dto;

import com.ezfinanz.eligibility.dto.EligibilityResponse;
import com.ezfinanz.loan.dto.EmiQuoteResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerDashboardResponse(
        String applicationId,
        Instant lastUpdated,
        String statusBadge,
        BigDecimal requestedAmount,
        Integer tenureMonths,
        BigDecimal monthlyEmi,
        Integer creditScore,
        EligibilityResponse eligibility,
        EmiQuoteResponse emi,
        boolean hasKycDocument,
        String kycDocumentName,
        boolean hasSelfie,
        List<DashboardNotice> notices
) {
}
