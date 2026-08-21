package com.ezfinanz.admin.service;

import com.ezfinanz.admin.dto.AdminApplicationDetail;
import com.ezfinanz.admin.dto.AdminApplicationSummary;
import com.ezfinanz.application.ApplicationStatusService;
import com.ezfinanz.auth.domain.Role;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.bank.dto.BankAccountResponse;
import com.ezfinanz.bank.repo.BankAccountRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.declaration.dto.DeclarationResponse;
import com.ezfinanz.declaration.repo.DeclarationRepository;
import com.ezfinanz.eligibility.dto.EligibilityResponse;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.eligibility.service.EligibilityCalculator;
import com.ezfinanz.kyc.dto.KycResponse;
import com.ezfinanz.kyc.repo.KycRepository;
import com.ezfinanz.kyc.service.KycService;
import com.ezfinanz.loan.dto.EmiQuoteResponse;
import com.ezfinanz.loan.repo.EmiRepository;
import com.ezfinanz.notify.EmailOtpService;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;
import com.ezfinanz.selfie.dto.SelfieResponse;
import com.ezfinanz.selfie.repo.SelfieRepository;
import com.ezfinanz.selfie.service.SelfieService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin-facing operations to inspect customer applications, review selfies, and confirm disbursement.
 */
@Service
public class AdminApplicationService {

    private final UserRepository userRepository;
    private final ApplicationStatusService applicationStatusService;
    private final KycRepository kycRepository;
    private final KycService kycService;
    private final EligibilityRepository eligibilityRepository;
    private final EmiRepository emiRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DeclarationRepository declarationRepository;
    private final SelfieRepository selfieRepository;
    private final SelfieService selfieService;
    private final EmailOtpService emailOtpService;

    public AdminApplicationService(
            UserRepository userRepository,
            ApplicationStatusService applicationStatusService,
            KycRepository kycRepository,
            KycService kycService,
            EligibilityRepository eligibilityRepository,
            EmiRepository emiRepository,
            BankAccountRepository bankAccountRepository,
            DeclarationRepository declarationRepository,
            SelfieRepository selfieRepository,
            SelfieService selfieService,
            EmailOtpService emailOtpService
    ) {
        this.userRepository = userRepository;
        this.applicationStatusService = applicationStatusService;
        this.kycRepository = kycRepository;
        this.kycService = kycService;
        this.eligibilityRepository = eligibilityRepository;
        this.emiRepository = emiRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.declarationRepository = declarationRepository;
        this.selfieRepository = selfieRepository;
        this.selfieService = selfieService;
        this.emailOtpService = emailOtpService;
    }

    /** Lists all customer applications as admin summaries. */
    @Transactional(readOnly = true)
    public List<AdminApplicationSummary> list() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.CUSTOMER).stream()
                .map(this::toSummary)
                .toList();
    }

    /** Loads the full application journey for one customer. */
    @Transactional(readOnly = true)
    public AdminApplicationDetail get(Long userId) {
        User user = requireCustomer(userId);
        var snapshot = applicationStatusService.snapshot(user);
        KycResponse kyc = kycRepository.findByUser_Id(userId).map(KycResponse::from).orElse(null);
        EligibilityResponse eligibility = eligibilityRepository.findByUser_Id(userId).map(EligibilityResponse::from).orElse(null);
        EmiQuoteResponse emi = emiRepository.findByUser_Id(userId)
                .map(row -> EmiQuoteResponse.fromSaved(
                        EligibilityCalculator.MIN_LOAN,
                        eligibility != null ? eligibility.maxEligibleAmount() : EligibilityCalculator.MIN_LOAN,
                        row
                ))
                .orElse(null);
        BankAccountResponse bank = bankAccountRepository.findByUser_Id(userId).map(BankAccountResponse::from).orElse(null);
        DeclarationResponse declaration = declarationRepository.findByUser_Id(userId).map(DeclarationResponse::from).orElse(null);
        SelfieResponse selfie = selfieRepository.findByUser_Id(userId)
                .map(row -> SelfieResponse.from(row, snapshot.stage()))
                .orElse(null);
        return new AdminApplicationDetail(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.isFullyVerified(),
                snapshot.stage(),
                snapshot.stageLabel(),
                kyc,
                eligibility,
                emi,
                bank,
                declaration,
                selfie
        );
    }

    /** Approves a pending selfie and emails the applicant an approval update. */
    @Transactional
    public AdminApplicationDetail approveSelfie(Long adminId, Long userId, String message) {
        User user = requireCustomer(userId);
        SelfieSubmission row = requirePendingSelfie(userId);
        row.setReviewStatus(SelfieReviewStatus.APPROVED);
        row.setRejectionReason(null);
        row.setReviewedAt(Instant.now());
        row.setReviewedByUserId(adminId);
        selfieRepository.save(row);
        notifyApplicant(
                user,
                "Approved",
                message == null || message.isBlank()
                        ? "Your application has been approved. We will proceed with the next steps for disbursement."
                        : message
        );
        return get(userId);
    }

    /** Rejects a pending selfie with an optional reason and emails the applicant. */
    @Transactional
    public AdminApplicationDetail rejectSelfie(Long adminId, Long userId, String reason) {
        User user = requireCustomer(userId);
        SelfieSubmission row = requirePendingSelfie(userId);
        String rejectionReason = reason == null || reason.isBlank() ? null : reason.trim();
        row.setReviewStatus(SelfieReviewStatus.REJECTED);
        row.setRejectionReason(rejectionReason);
        row.setReviewedAt(Instant.now());
        row.setReviewedByUserId(adminId);
        selfieRepository.save(row);
        notifyApplicant(
                user,
                "Rejected",
                rejectionReason == null
                        ? "Your application was rejected. Please review your details, update what is needed, and send the application again."
                        : rejectionReason
        );
        return get(userId);
    }

    /** Marks an approved application as disbursed by the acting admin. */
    @Transactional
    public AdminApplicationDetail disburse(Long adminId, Long userId) {
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SELFIE_REQUIRED", "No selfie has been submitted."));
        if (row.getReviewStatus() != SelfieReviewStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELFIE_NOT_APPROVED", "Approve the selfie before disbursement.");
        }
        if (row.isDisbursed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_DISBURSED", "This loan is already disbursed.");
        }
        row.setDisbursed(true);
        row.setDisbursedAt(Instant.now());
        row.setDisbursedByUserId(adminId);
        selfieRepository.save(row);
        return get(userId);
    }

    /** Returns the customer's KYC ID document for admin viewing. */
    @Transactional(readOnly = true)
    public Resource kycDocument(Long userId) {
        requireCustomer(userId);
        return kycService.document(userId);
    }

    /** Returns the customer's selfie photo for admin viewing. */
    @Transactional(readOnly = true)
    public Resource selfiePhoto(Long userId) {
        requireCustomer(userId);
        return selfieService.photo(userId);
    }

    private void notifyApplicant(User user, String statusLabel, String message) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        emailOtpService.sendApplicationReviewUpdate(
                user.getEmail(),
                user.getFullName(),
                user.getId(),
                statusLabel,
                message
        );
    }

    private AdminApplicationSummary toSummary(User user) {
        var snapshot = applicationStatusService.snapshot(user);
        var eligibility = eligibilityRepository.findByUser_Id(user.getId());
        var emi = emiRepository.findByUser_Id(user.getId());
        var selfie = selfieRepository.findByUser_Id(user.getId());
        return new AdminApplicationSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                eligibility.map(row -> row.getRequestedLoanAmount()).orElse(null),
                emi.map(row -> row.getPrincipal()).orElse(null),
                emi.map(row -> row.getTenureMonths()).orElse(null),
                snapshot.stage(),
                snapshot.stageLabel(),
                user.getCreatedAt(),
                selfie.map(SelfieSubmission::getSubmittedAt).orElse(user.getUpdatedAt())
        );
    }

    private SelfieSubmission requirePendingSelfie(Long userId) {
        requireCustomer(userId);
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SELFIE_NOT_FOUND", "No selfie has been submitted."));
        if (row.isDisbursed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_DISBURSED", "This loan has already been disbursed.");
        }
        if (row.getReviewStatus() != SelfieReviewStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_PENDING", "This selfie is not waiting for review.");
        }
        return row;
    }

    private User requireCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Application not found."));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Application not found.");
        }
        return user;
    }
}
