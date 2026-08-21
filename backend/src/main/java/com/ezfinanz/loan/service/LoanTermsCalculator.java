package com.ezfinanz.loan.service;

import com.ezfinanz.eligibility.domain.CreditBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reducing-balance EMI, upfront fees deducted from disbursement, and effective annual IRR
 * from net cash received vs EMI outflows.
 */
public final class LoanTermsCalculator {

    public static final List<Integer> TENURES = List.of(6, 12, 18, 24, 36);
    public static final BigDecimal PROCESSING_FEE_RATE = new BigDecimal("0.02");
    public static final BigDecimal GST_RATE = new BigDecimal("0.18");
    public static final BigDecimal DOCUMENTATION_FEE = new BigDecimal("499.00");
    public static final BigDecimal STAMP_DUTY = new BigDecimal("200.00");

    private LoanTermsCalculator() {
    }

    /** Annual interest percent offered for the given CIBIL credit band. */
    public static BigDecimal annualInterestPercent(CreditBand band) {
        return switch (band) {
            case EXCELLENT -> new BigDecimal("10.99");
            case GOOD -> new BigDecimal("13.99");
            case FAIR -> new BigDecimal("16.99");
            case POOR -> new BigDecimal("18.99");
        };
    }

    /** Builds a full quote: EMI schedule totals, upfront charges, net disbursement, and IRR. */
    public static Quote calculate(BigDecimal principal, int tenureMonths, CreditBand band) {
        BigDecimal amount = principal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal annualPercent = annualInterestPercent(band);
        double p = amount.doubleValue();
        double monthlyRate = annualPercent.doubleValue() / 12.0 / 100.0;
        double emiRaw;
        if (monthlyRate == 0) {
            emiRaw = p / tenureMonths;
        } else {
            double factor = Math.pow(1 + monthlyRate, tenureMonths);
            emiRaw = p * monthlyRate * factor / (factor - 1);
        }
        BigDecimal monthlyEmi = BigDecimal.valueOf(emiRaw).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepayment = monthlyEmi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalRepayment.subtract(amount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal processingFee = amount.multiply(PROCESSING_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstOnProcessing = processingFee.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherCharges = DOCUMENTATION_FEE.add(STAMP_DUTY);
        BigDecimal totalCharges = processingFee.add(gstOnProcessing).add(otherCharges);
        BigDecimal netDisbursement = amount.subtract(totalCharges).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        double irrMonthly = monthlyIrr(netDisbursement.doubleValue(), monthlyEmi.doubleValue(), tenureMonths, monthlyRate);
        BigDecimal irrPercent = BigDecimal.valueOf((Math.pow(1 + irrMonthly, 12) - 1) * 100)
                .setScale(2, RoundingMode.HALF_UP);

        return new Quote(
                amount,
                tenureMonths,
                annualPercent,
                processingFee,
                gstOnProcessing,
                DOCUMENTATION_FEE,
                STAMP_DUTY,
                otherCharges,
                totalCharges,
                monthlyEmi,
                totalInterest,
                totalRepayment,
                netDisbursement,
                irrPercent
        );
    }

    static double monthlyIrr(double netDisbursement, double emi, int n, double guess) {
        double r = guess > 0 ? guess : 0.01;
        for (int i = 0; i < 40; i++) {
            double npv = netDisbursement;
            double derivative = 0;
            for (int t = 1; t <= n; t++) {
                double denom = Math.pow(1 + r, t);
                npv -= emi / denom;
                derivative += t * emi / Math.pow(1 + r, t + 1);
            }
            if (Math.abs(derivative) < 1e-12) {
                break;
            }
            double next = r - npv / derivative;
            if (next <= -0.99 || next > 2) {
                break;
            }
            if (Math.abs(next - r) < 1e-10) {
                return next;
            }
            r = next;
        }
        return r;
    }

    /** Immutable loan-terms breakdown produced by {@link #calculate}. */
    public record Quote(
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
            BigDecimal irrPercent
    ) {
    }
}
