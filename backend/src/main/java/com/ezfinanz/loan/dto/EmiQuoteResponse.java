package com.ezfinanz.loan.dto;

import com.ezfinanz.eligibility.domain.CreditBand;
import com.ezfinanz.loan.domain.EmiSelection;
import com.ezfinanz.loan.service.LoanTermsCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** API view of an EMI quote or saved selection, including allowed amount bounds and tenures. */
public record EmiQuoteResponse(
        BigDecimal minAmount,
        BigDecimal maxAmount,
        List<Integer> tenures,
        CreditBand creditBand,
        BigDecimal principal,
        int tenureMonths,
        BigDecimal annualInterestPercent,
        BigDecimal processingFee,
        BigDecimal gstOnProcessingFee,
        BigDecimal documentationFee,
        BigDecimal stampDuty,
        BigDecimal otherCharges,
        BigDecimal totalCharges,
        BigDecimal monthlyEmi,
        BigDecimal totalInterest,
        BigDecimal totalRepayment,
        BigDecimal netDisbursement,
        BigDecimal irrPercent,
        Instant selectedAt
) {
    /** Builds a response from a live calculator quote. */
    public static EmiQuoteResponse from(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            CreditBand band,
            LoanTermsCalculator.Quote quote,
            Instant selectedAt
    ) {
        return new EmiQuoteResponse(
                minAmount,
                maxAmount,
                LoanTermsCalculator.TENURES,
                band,
                quote.principal(),
                quote.tenureMonths(),
                quote.annualInterestPercent(),
                quote.processingFee(),
                quote.gstOnProcessingFee(),
                quote.documentationFee(),
                quote.stampDuty(),
                quote.otherCharges(),
                quote.totalCharges(),
                quote.monthlyEmi(),
                quote.totalInterest(),
                quote.totalRepayment(),
                quote.netDisbursement(),
                quote.irrPercent(),
                selectedAt
        );
    }

    /** Builds a response from a persisted EMI selection. */
    public static EmiQuoteResponse fromSaved(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            EmiSelection row
    ) {
        return new EmiQuoteResponse(
                minAmount,
                maxAmount,
                LoanTermsCalculator.TENURES,
                row.getCreditBand(),
                row.getPrincipal(),
                row.getTenureMonths(),
                row.getAnnualInterestPercent(),
                row.getProcessingFee(),
                row.getGstOnProcessingFee(),
                row.getDocumentationFee(),
                row.getStampDuty(),
                row.getOtherCharges(),
                row.getTotalCharges(),
                row.getMonthlyEmi(),
                row.getTotalInterest(),
                row.getTotalRepayment(),
                row.getNetDisbursement(),
                row.getIrrPercent(),
                row.getSelectedAt()
        );
    }
}
