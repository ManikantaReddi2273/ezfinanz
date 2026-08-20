package com.ezfinanz.selfie.service;

import com.ezfinanz.application.ApplicationStatusService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.bank.repo.BankAccountRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.declaration.repo.DeclarationRepository;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.files.LocalFileStorage;
import com.ezfinanz.kyc.repo.KycRepository;
import com.ezfinanz.loan.repo.EmiRepository;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;
import com.ezfinanz.selfie.dto.SelfieResponse;
import com.ezfinanz.selfie.repo.SelfieRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class SelfieService {

    private final SelfieRepository selfieRepository;
    private final DeclarationRepository declarationRepository;
    private final KycRepository kycRepository;
    private final EligibilityRepository eligibilityRepository;
    private final EmiRepository emiRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final LocalFileStorage fileStorage;
    private final ApplicationStatusService applicationStatusService;

    public SelfieService(
            SelfieRepository selfieRepository,
            DeclarationRepository declarationRepository,
            KycRepository kycRepository,
            EligibilityRepository eligibilityRepository,
            EmiRepository emiRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            LocalFileStorage fileStorage,
            ApplicationStatusService applicationStatusService
    ) {
        this.selfieRepository = selfieRepository;
        this.declarationRepository = declarationRepository;
        this.kycRepository = kycRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.emiRepository = emiRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.applicationStatusService = applicationStatusService;
    }

    @Transactional(readOnly = true)
    public SelfieResponse get(Long userId) {
        User user = requireUser(userId);
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SELFIE_NOT_FOUND", "No selfie has been saved yet."));
        return SelfieResponse.from(row, applicationStatusService.snapshot(user).stage());
    }

    @Transactional
    public SelfieResponse confirmDraft(Long userId, MultipartFile photo) {
        User user = requireUser(userId);
        if (!declarationRepository.existsByUser_IdAndAcceptedIsTrue(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DECLARATION_REQUIRED", "Accept the declaration before confirming a selfie.");
        }
        if (photo == null || photo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_REQUIRED", "Capture or upload a selfie.");
        }
        SelfieSubmission row = selfieRepository.findByUser_Id(userId).orElseGet(SelfieSubmission::new);
        boolean existing = row.getId() != null;
        if (row.isDisbursed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_DISBURSED", "This loan has already been disbursed.");
        }
        if (existing && row.getReviewStatus() == SelfieReviewStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_APPROVED", "The selfie is already approved.");
        }
        if (existing && row.getReviewStatus() == SelfieReviewStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "APPLICATION_SUBMITTED", "This application has already been submitted.");
        }
        LocalFileStorage.StoredFile stored = fileStorage.saveSelfie(userId, photo);
        if (stored == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_REQUIRED", "Capture or upload a selfie.");
        }
        row.setUser(user);
        row.setPhotoPath(stored.relativePath());
        row.setOriginalName(stored.originalName());
        row.setReviewStatus(SelfieReviewStatus.DRAFT);
        row.setRejectionReason(null);
        row.setReviewedAt(null);
        row.setReviewedByUserId(null);
        if (row.getSubmittedAt() == null) {
            row.setSubmittedAt(Instant.now());
        }
        selfieRepository.save(row);
        return SelfieResponse.from(row, applicationStatusService.snapshot(user).stage());
    }

    @Transactional
    public SelfieResponse sendApplication(Long userId) {
        User user = requireUser(userId);
        requireReadyToSubmit(userId, user);
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SELFIE_REQUIRED", "Confirm your selfie before sending the application."));
        if (row.getReviewStatus() != SelfieReviewStatus.DRAFT) {
            if (row.getReviewStatus() == SelfieReviewStatus.PENDING) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_SUBMITTED", "This application has already been sent.");
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELFIE_REQUIRED", "Confirm your selfie before sending the application.");
        }
        row.setReviewStatus(SelfieReviewStatus.PENDING);
        row.setSubmittedAt(Instant.now());
        row.setRejectionReason(null);
        row.setReviewedAt(null);
        row.setReviewedByUserId(null);
        selfieRepository.save(row);
        return SelfieResponse.from(row, applicationStatusService.snapshot(user).stage());
    }

    @Transactional(readOnly = true)
    public Resource photo(Long userId) {
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SELFIE_NOT_FOUND", "No selfie has been saved yet."));
        Path path = fileStorage.resolve(row.getPhotoPath());
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PHOTO_MISSING", "The selfie file is missing.");
        }
        return new FileSystemResource(path);
    }

    private void requireReadyToSubmit(Long userId, User user) {
        if (!user.isFullyVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CONTACTS_NOT_VERIFIED", "Verify email and phone before sending the application.");
        }
        if (!kycRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "KYC_REQUIRED", "Complete KYC before sending the application.");
        }
        var eligibility = eligibilityRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "ELIGIBILITY_REQUIRED", "Complete eligibility before sending the application."));
        if (eligibility.getResult() == EligibilityResult.NOT_ELIGIBLE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ELIGIBLE", "This application is not eligible to be sent.");
        }
        if (!emiRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMI_REQUIRED", "Confirm EMI terms before sending the application.");
        }
        if (!bankAccountRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BANK_REQUIRED", "Add a bank account before sending the application.");
        }
        if (!declarationRepository.existsByUser_IdAndAcceptedIsTrue(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DECLARATION_REQUIRED", "Accept the declaration before sending the application.");
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
    }
}
