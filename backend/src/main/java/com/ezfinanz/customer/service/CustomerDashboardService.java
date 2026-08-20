package com.ezfinanz.customer.service;

import com.ezfinanz.application.ApplicationStage;
import com.ezfinanz.application.ApplicationStatusService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.customer.dto.CustomerDashboardResponse;
import com.ezfinanz.customer.dto.DashboardNotice;
import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.dto.EligibilityResponse;
import com.ezfinanz.eligibility.service.EligibilityCalculator;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.kyc.domain.KycProfile;
import com.ezfinanz.kyc.repo.KycRepository;
import com.ezfinanz.loan.dto.EmiQuoteResponse;
import com.ezfinanz.loan.repo.EmiRepository;
import com.ezfinanz.loan.service.EmiService;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;
import com.ezfinanz.selfie.repo.SelfieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerDashboardService {

    private final UserRepository userRepository;
    private final ApplicationStatusService applicationStatusService;
    private final EligibilityRepository eligibilityRepository;
    private final EmiRepository emiRepository;
    private final EmiService emiService;
    private final KycRepository kycRepository;
    private final SelfieRepository selfieRepository;

    public CustomerDashboardService(
            UserRepository userRepository,
            ApplicationStatusService applicationStatusService,
            EligibilityRepository eligibilityRepository,
            EmiRepository emiRepository,
            EmiService emiService,
            KycRepository kycRepository,
            SelfieRepository selfieRepository
    ) {
        this.userRepository = userRepository;
        this.applicationStatusService = applicationStatusService;
        this.eligibilityRepository = eligibilityRepository;
        this.emiRepository = emiRepository;
        this.emiService = emiService;
        this.kycRepository = kycRepository;
        this.selfieRepository = selfieRepository;
    }

    @Transactional(readOnly = true)
    public CustomerDashboardResponse load(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        ApplicationStatusService.Snapshot snapshot = applicationStatusService.snapshot(user);
        EligibilityAssessment eligibility = eligibilityRepository.findByUser_Id(userId).orElse(null);
        KycProfile kyc = kycRepository.findByUser_Id(userId).orElse(null);
        SelfieSubmission selfie = selfieRepository.findByUser_Id(userId).orElse(null);

        EligibilityResponse eligibilityDto = eligibility == null ? null : EligibilityResponse.from(eligibility);
        EmiQuoteResponse emi = null;
        if (snapshot.eligibilityPassed() && eligibility != null) {
            emi = emiRepository.findByUser_Id(userId)
                    .map(row -> EmiQuoteResponse.fromSaved(
                            EligibilityCalculator.MIN_LOAN,
                            eligibility.getMaxEligibleAmount(),
                            row
                    ))
                    .orElseGet(() -> emiService.quote(userId, eligibility.getMaxEligibleAmount(), 24));
        }

        Instant lastUpdated = user.getUpdatedAt();
        lastUpdated = later(lastUpdated, kyc == null ? null : kyc.getUpdatedAt());
        lastUpdated = later(lastUpdated, eligibility == null ? null : eligibility.getAssessedAt());
        lastUpdated = later(lastUpdated, emi == null ? null : emi.selectedAt());
        lastUpdated = later(lastUpdated, selfie == null ? null : selfie.getSubmittedAt());

        return new CustomerDashboardResponse(
                "EZF" + String.format("%09d", user.getId()),
                lastUpdated,
                statusBadge(snapshot.stage()),
                eligibility == null ? null : eligibility.getRequestedLoanAmount(),
                emi == null ? null : emi.tenureMonths(),
                emi == null ? null : emi.monthlyEmi(),
                eligibility == null ? null : eligibility.getCreditScore(),
                eligibilityDto,
                emi,
                kyc != null && kyc.getIdDocumentPath() != null && !kyc.getIdDocumentPath().isBlank(),
                kyc == null ? null : kyc.getIdDocumentOriginalName(),
                selfie != null,
                notices(user, snapshot, selfie)
        );
    }

    private static Instant later(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    private static String statusBadge(ApplicationStage stage) {
        return switch (stage) {
            case DISBURSED -> "Disbursed";
            case READY_FOR_DISBURSEMENT -> "Approved";
            case WAITING_FOR_ADMIN_REVIEW -> "Under Review";
            case SELFIE_REJECTED -> "Selfie Rejected";
            case NOT_ELIGIBLE -> "Not Eligible";
            case CONTACTS_PENDING -> "Email & Phone Pending";
            case KYC -> "KYC Details Pending";
            case ELIGIBILITY -> "Loan Eligibility Pending";
            case EMI -> "EMI Selection Pending";
            case BANK -> "Bank Account Pending";
            case DECLARATION -> "Declaration Pending";
            case LIVE_SELFIE -> "Selfie Verification Pending";
            case READY_TO_SUBMIT -> "Ready to Submit";
        };
    }

    private static List<DashboardNotice> notices(User user, ApplicationStatusService.Snapshot snapshot, SelfieSubmission selfie) {
        List<DashboardNotice> items = new ArrayList<>();
        if (!user.isEmailVerified() || !user.isPhoneVerified()) {
            items.add(new DashboardNotice("verify", "Verify email and phone to continue your application.", "verify"));
        }
        if (snapshot.selfieStatus() != null && SelfieReviewStatus.REJECTED.name().equals(snapshot.selfieStatus())) {
            items.add(new DashboardNotice("selfie-rejected", "Your selfie was rejected. Please capture a clearer photo.", "selfie"));
        } else if (selfie != null && selfie.getReviewStatus() == SelfieReviewStatus.DRAFT) {
            items.add(new DashboardNotice("send", "Your application is ready. Review your details and send it from Selfie Verification.", "selfie"));
        } else if (selfie != null && selfie.getReviewStatus() == SelfieReviewStatus.PENDING) {
            items.add(new DashboardNotice("review", "Your selfie is waiting for admin review.", "apply"));
        }
        if (snapshot.eligibilityPassed() && !snapshot.emiCompleted()) {
            items.add(new DashboardNotice("emi", "Choose tenure and confirm EMI terms.", "emi"));
        }
        if (items.isEmpty()) {
            items.add(new DashboardNotice("ok", "You are up to date. Continue the next application step.", "apply"));
        }
        return items;
    }
}
