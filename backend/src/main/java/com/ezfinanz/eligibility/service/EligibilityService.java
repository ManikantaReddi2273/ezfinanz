package com.ezfinanz.eligibility.service;

import com.ezfinanz.application.ApplicationCascadeService;
import com.ezfinanz.application.ApplicationLockService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.dto.EligibilityRequest;
import com.ezfinanz.eligibility.dto.EligibilityResponse;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.kyc.repo.KycRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Orchestrates eligibility assessment: requires KYC, runs the calculator, and stores results.
 * Updating eligibility may cascade-invalidate EMI and later application steps.
 */
@Service
public class EligibilityService {

    private final EligibilityRepository eligibilityRepository;
    private final UserRepository userRepository;
    private final KycRepository kycRepository;
    private final ApplicationLockService applicationLockService;
    private final ApplicationCascadeService applicationCascadeService;

    public EligibilityService(
            EligibilityRepository eligibilityRepository,
            UserRepository userRepository,
            KycRepository kycRepository,
            ApplicationLockService applicationLockService,
            ApplicationCascadeService applicationCascadeService
    ) {
        this.eligibilityRepository = eligibilityRepository;
        this.userRepository = userRepository;
        this.kycRepository = kycRepository;
        this.applicationLockService = applicationLockService;
        this.applicationCascadeService = applicationCascadeService;
    }

    /** Returns the persisted eligibility assessment for the customer. */
    @Transactional(readOnly = true)
    public EligibilityResponse get(Long userId) {
        EligibilityAssessment row = eligibilityRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ELIGIBILITY_NOT_FOUND", "Eligibility has not been checked yet."));
        return EligibilityResponse.from(row);
    }

    /** Evaluates and saves eligibility from income, credit score, debts, and requested amount. */
    @Transactional
    public EligibilityResponse assess(Long userId, EligibilityRequest request) {
        applicationLockService.requireEditable(userId);
        boolean updating = eligibilityRepository.existsByUser_Id(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        if (!user.isFullyVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "CONTACTS_NOT_VERIFIED",
                    "Verify email and phone before checking eligibility."
            );
        }
        if (!kycRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "KYC_REQUIRED", "Complete KYC before checking eligibility.");
        }
        if (request.getRequestedLoanAmount().compareTo(EligibilityCalculator.MAX_LOAN) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AMOUNT_INVALID", "Maximum loan amount is ₹15,00,000.");
        }

        EligibilityCalculator.Outcome outcome = EligibilityCalculator.evaluate(
                request.getIncomeType(),
                request.getIncomeAmount(),
                request.getRequestedLoanAmount(),
                request.getCreditScore(),
                request.getOutstandingDebts() == null ? BigDecimal.ZERO : request.getOutstandingDebts()
        );

        EligibilityAssessment row = eligibilityRepository.findByUser_Id(userId).orElseGet(EligibilityAssessment::new);
        row.setUser(user);
        row.setIncomeType(request.getIncomeType());
        row.setIncomeAmount(request.getIncomeAmount());
        row.setRequestedLoanAmount(request.getRequestedLoanAmount());
        row.setCreditScore(request.getCreditScore());
        row.setOutstandingDebts(request.getOutstandingDebts() == null ? BigDecimal.ZERO : request.getOutstandingDebts());
        row.setEmployerName(request.getEmployerName().trim());
        row.setDesignation(request.getDesignation().trim());
        row.setMonthlyIncome(outcome.monthlyIncome());
        row.setAnnualIncome(outcome.annualIncome());
        row.setDtiRatio(outcome.dtiRatio());
        row.setCreditBand(outcome.creditBand());
        row.setMaxEligibleAmount(outcome.maxEligibleAmount());
        row.setResult(outcome.result());
        row.setReasons(String.join("\n", outcome.reasons()));
        EligibilityResponse response = EligibilityResponse.from(eligibilityRepository.save(row));
        if (updating) {
            applicationCascadeService.invalidateAfterEligibility(userId);
        }
        return response;
    }
}
