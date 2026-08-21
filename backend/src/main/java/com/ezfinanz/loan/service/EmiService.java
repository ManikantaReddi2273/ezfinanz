package com.ezfinanz.loan.service;

import com.ezfinanz.application.ApplicationCascadeService;
import com.ezfinanz.application.ApplicationLockService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.eligibility.service.EligibilityCalculator;
import com.ezfinanz.loan.domain.EmiSelection;
import com.ezfinanz.loan.dto.EmiQuoteResponse;
import com.ezfinanz.loan.dto.EmiSaveRequest;
import com.ezfinanz.loan.repo.EmiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quotes and saves EMI terms within the customer's eligible loan range.
 * Requires a successful eligibility result; updates may invalidate bank and later steps.
 */
@Service
public class EmiService {

    private final EmiRepository emiRepository;
    private final EligibilityRepository eligibilityRepository;
    private final UserRepository userRepository;
    private final ApplicationLockService applicationLockService;
    private final ApplicationCascadeService applicationCascadeService;

    public EmiService(
            EmiRepository emiRepository,
            EligibilityRepository eligibilityRepository,
            UserRepository userRepository,
            ApplicationLockService applicationLockService,
            ApplicationCascadeService applicationCascadeService
    ) {
        this.emiRepository = emiRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.userRepository = userRepository;
        this.applicationLockService = applicationLockService;
        this.applicationCascadeService = applicationCascadeService;
    }

    /** Calculates a live EMI quote without persisting; defaults amount/tenure when omitted. */
    @Transactional(readOnly = true)
    public EmiQuoteResponse quote(Long userId, BigDecimal requestedPrincipal, Integer tenureMonths) {
        EligibilityAssessment eligibility = requireEligible(userId);
        BigDecimal min = EligibilityCalculator.MIN_LOAN;
        BigDecimal max = eligibility.getMaxEligibleAmount();
        BigDecimal principal = requestedPrincipal != null ? requestedPrincipal : defaultPrincipal(eligibility);
        int tenure = tenureMonths != null ? tenureMonths : 12;
        validate(principal, tenure, min, max);
        LoanTermsCalculator.Quote quote = LoanTermsCalculator.calculate(principal, tenure, eligibility.getCreditBand());
        return EmiQuoteResponse.from(min, max, eligibility.getCreditBand(), quote, null);
    }

    /** Returns the customer's previously saved EMI selection. */
    @Transactional(readOnly = true)
    public EmiQuoteResponse get(Long userId) {
        EligibilityAssessment eligibility = requireEligible(userId);
        EmiSelection row = emiRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMI_NOT_FOUND", "EMI terms have not been saved yet."));
        return EmiQuoteResponse.fromSaved(EligibilityCalculator.MIN_LOAN, eligibility.getMaxEligibleAmount(), row);
    }

    /** Validates and persists confirmed principal and tenure with computed fees and EMI. */
    @Transactional
    public EmiQuoteResponse save(Long userId, EmiSaveRequest request) {
        applicationLockService.requireEditable(userId);
        boolean updating = emiRepository.existsByUser_Id(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        EligibilityAssessment eligibility = requireEligible(userId);
        BigDecimal min = EligibilityCalculator.MIN_LOAN;
        BigDecimal max = eligibility.getMaxEligibleAmount();
        validate(request.getPrincipal(), request.getTenureMonths(), min, max);
        LoanTermsCalculator.Quote quote = LoanTermsCalculator.calculate(
                request.getPrincipal(),
                request.getTenureMonths(),
                eligibility.getCreditBand()
        );

        EmiSelection row = emiRepository.findByUser_Id(userId).orElseGet(EmiSelection::new);
        row.setUser(user);
        row.setPrincipal(quote.principal());
        row.setTenureMonths(quote.tenureMonths());
        row.setCreditBand(eligibility.getCreditBand());
        row.setAnnualInterestPercent(quote.annualInterestPercent());
        row.setProcessingFee(quote.processingFee());
        row.setGstOnProcessingFee(quote.gstOnProcessingFee());
        row.setDocumentationFee(quote.documentationFee());
        row.setStampDuty(quote.stampDuty());
        row.setOtherCharges(quote.otherCharges());
        row.setTotalCharges(quote.totalCharges());
        row.setMonthlyEmi(quote.monthlyEmi());
        row.setTotalInterest(quote.totalInterest());
        row.setTotalRepayment(quote.totalRepayment());
        row.setNetDisbursement(quote.netDisbursement());
        row.setIrrPercent(quote.irrPercent());
        EmiQuoteResponse response = EmiQuoteResponse.fromSaved(min, max, emiRepository.save(row));
        if (updating) {
            applicationCascadeService.invalidateAfterEmi(userId);
        }
        return response;
    }

    private EligibilityAssessment requireEligible(Long userId) {
        EligibilityAssessment eligibility = eligibilityRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "ELIGIBILITY_REQUIRED", "Complete eligibility before selecting EMI terms."));
        if (eligibility.getResult() == EligibilityResult.NOT_ELIGIBLE
                || eligibility.getMaxEligibleAmount().compareTo(EligibilityCalculator.MIN_LOAN) < 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ELIGIBLE", "This profile is not eligible to select EMI terms.");
        }
        return eligibility;
    }

    private static BigDecimal defaultPrincipal(EligibilityAssessment eligibility) {
        BigDecimal requested = eligibility.getRequestedLoanAmount();
        BigDecimal max = eligibility.getMaxEligibleAmount();
        BigDecimal chosen = requested.min(max);
        if (chosen.compareTo(EligibilityCalculator.MIN_LOAN) < 0) {
            return EligibilityCalculator.MIN_LOAN.min(max);
        }
        return chosen.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validate(BigDecimal principal, Integer tenure, BigDecimal min, BigDecimal max) {
        if (tenure == null || !LoanTermsCalculator.TENURES.contains(tenure)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TENURE_INVALID", "Choose 6, 12, 18, 24, or 36 months.");
        }
        if (principal == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AMOUNT_INVALID", "Enter a loan amount.");
        }
        if (principal.compareTo(min) < 0 || principal.compareTo(max) > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "AMOUNT_INVALID",
                    "Loan amount must be between ₹" + min.toPlainString() + " and ₹" + max.toPlainString() + "."
            );
        }
    }
}
