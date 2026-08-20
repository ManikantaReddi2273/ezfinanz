package com.ezfinanz.eligibility.service;

import com.ezfinanz.eligibility.domain.CreditBand;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import com.ezfinanz.eligibility.domain.IncomeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Personal-loan eligibility: CIBIL band, debt-to-income, and requested amount vs capacity.
 * Product limits are ₹25,000–₹15,00,000. Credit checks are simulated from the score the customer enters.
 */
public final class EligibilityCalculator {

    public static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("15000");
    public static final BigDecimal MIN_LOAN = new BigDecimal("25000");
    public static final BigDecimal MAX_LOAN = new BigDecimal("1500000");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    private EligibilityCalculator() {
    }

    public static Outcome evaluate(
            IncomeType incomeType,
            BigDecimal incomeAmount,
            BigDecimal requestedLoanAmount,
            int creditScore,
            BigDecimal outstandingDebts
    ) {
        BigDecimal monthly = incomeType == IncomeType.ANNUAL
                ? incomeAmount.divide(TWELVE, 2, RoundingMode.HALF_UP)
                : incomeAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal annual = monthly.multiply(TWELVE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal debts = outstandingDebts.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal requested = requestedLoanAmount.setScale(2, RoundingMode.HALF_UP);

        CreditBand band = creditBand(creditScore);
        List<String> reasons = new ArrayList<>();
        reasons.add(creditReason(band, creditScore));

        BigDecimal dti = annual.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : debts.divide(annual, 4, RoundingMode.HALF_UP);
        reasons.add(dtiReason(dti, debts));

        BigDecimal maxEligible = BigDecimal.ZERO;

        if (monthly.compareTo(MIN_MONTHLY_INCOME) < 0) {
            reasons.add("Monthly income is below ₹15,000, which is the minimum for this product.");
        } else if (band == CreditBand.POOR) {
            reasons.add("CIBIL below 650 does not meet the minimum credit requirement.");
        } else {
            BigDecimal fromIncome = monthly.multiply(BigDecimal.valueOf(incomeMultiplier(band)));
            BigDecimal dtiCap = dtiCap(band);
            BigDecimal fromDti = dtiCap.multiply(annual).subtract(debts).max(BigDecimal.ZERO);
            maxEligible = fromIncome.min(fromDti).min(MAX_LOAN);
            maxEligible = floorToThousand(maxEligible);
            if (maxEligible.compareTo(MIN_LOAN) < 0) {
                maxEligible = BigDecimal.ZERO;
                reasons.add("After income and existing debt, no amount meets the ₹25,000 minimum.");
            } else {
                reasons.add("Based on income and CIBIL, maximum offer is ₹"
                        + maxEligible.toPlainString().split("\\.")[0] + ".");
            }
        }

        EligibilityResult result;
        if (maxEligible.compareTo(BigDecimal.ZERO) <= 0) {
            result = EligibilityResult.NOT_ELIGIBLE;
            reasons.add("Not eligible for a personal loan on this profile.");
        } else if (requested.compareTo(maxEligible) <= 0) {
            result = EligibilityResult.ELIGIBLE;
            reasons.add("Requested amount is within the approved limit.");
        } else {
            result = EligibilityResult.PARTIALLY_ELIGIBLE;
            reasons.add("Requested amount exceeds capacity. A lower amount can still be offered.");
        }

        return new Outcome(monthly, annual, dti, band, maxEligible, result, List.copyOf(reasons));
    }

    static CreditBand creditBand(int score) {
        if (score >= 750) {
            return CreditBand.EXCELLENT;
        }
        if (score >= 700) {
            return CreditBand.GOOD;
        }
        if (score >= 650) {
            return CreditBand.FAIR;
        }
        return CreditBand.POOR;
    }

    private static int incomeMultiplier(CreditBand band) {
        return switch (band) {
            case EXCELLENT -> 18;
            case GOOD -> 12;
            case FAIR -> 8;
            case POOR -> 0;
        };
    }

    private static BigDecimal dtiCap(CreditBand band) {
        return switch (band) {
            case EXCELLENT -> new BigDecimal("0.50");
            case GOOD -> new BigDecimal("0.45");
            case FAIR -> new BigDecimal("0.40");
            case POOR -> BigDecimal.ZERO;
        };
    }

    private static String creditReason(CreditBand band, int score) {
        String label = switch (band) {
            case EXCELLENT -> "Excellent (750+)";
            case GOOD -> "Good (700–749)";
            case FAIR -> "Fair (650–699)";
            case POOR -> "Below policy minimum (under 650)";
        };
        return "CIBIL " + score + " is " + label + ".";
    }

    private static String dtiReason(BigDecimal dti, BigDecimal debts) {
        BigDecimal percent = dti.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
        return "Debt-to-income is " + percent.toPlainString()
                + "% on outstanding balances of ₹" + debts.setScale(0, RoundingMode.HALF_UP).toPlainString() + ".";
    }

    private static BigDecimal floorToThousand(BigDecimal amount) {
        return amount.divideToIntegralValue(THOUSAND).multiply(THOUSAND).setScale(2, RoundingMode.HALF_UP);
    }

    public record Outcome(
            BigDecimal monthlyIncome,
            BigDecimal annualIncome,
            BigDecimal dtiRatio,
            CreditBand creditBand,
            BigDecimal maxEligibleAmount,
            EligibilityResult result,
            List<String> reasons
    ) {
    }
}
