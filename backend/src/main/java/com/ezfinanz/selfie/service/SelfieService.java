package com.ezfinanz.selfie.service;

import com.ezfinanz.application.ApplicationStatusService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.declaration.repo.DeclarationRepository;
import com.ezfinanz.files.LocalFileStorage;
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
    private final UserRepository userRepository;
    private final LocalFileStorage fileStorage;
    private final ApplicationStatusService applicationStatusService;

    public SelfieService(
            SelfieRepository selfieRepository,
            DeclarationRepository declarationRepository,
            UserRepository userRepository,
            LocalFileStorage fileStorage,
            ApplicationStatusService applicationStatusService
    ) {
        this.selfieRepository = selfieRepository;
        this.declarationRepository = declarationRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.applicationStatusService = applicationStatusService;
    }

    @Transactional(readOnly = true)
    public SelfieResponse get(Long userId) {
        User user = requireUser(userId);
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SELFIE_NOT_FOUND", "No selfie has been submitted yet."));
        return SelfieResponse.from(row, applicationStatusService.snapshot(user).stage());
    }

    @Transactional
    public SelfieResponse submit(Long userId, MultipartFile photo) {
        User user = requireUser(userId);
        if (!declarationRepository.existsByUser_IdAndAcceptedIsTrue(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DECLARATION_REQUIRED", "Accept the declaration before submitting a selfie.");
        }
        if (photo == null || photo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_REQUIRED", "Capture or upload a selfie.");
        }
        SelfieSubmission row = selfieRepository.findByUser_Id(userId).orElseGet(SelfieSubmission::new);
        if (row.isDisbursed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_DISBURSED", "This loan has already been disbursed.");
        }
        if (row.getReviewStatus() == SelfieReviewStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_APPROVED", "The selfie is already approved.");
        }
        LocalFileStorage.StoredFile stored = fileStorage.saveSelfie(userId, photo);
        if (stored == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_REQUIRED", "Capture or upload a selfie.");
        }
        row.setUser(user);
        row.setPhotoPath(stored.relativePath());
        row.setOriginalName(stored.originalName());
        row.setReviewStatus(SelfieReviewStatus.PENDING);
        row.setRejectionReason(null);
        row.setReviewedAt(null);
        row.setReviewedByUserId(null);
        row.setSubmittedAt(Instant.now());
        selfieRepository.save(row);
        return SelfieResponse.from(row, applicationStatusService.snapshot(user).stage());
    }

    @Transactional(readOnly = true)
    public Resource photo(Long userId) {
        SelfieSubmission row = selfieRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SELFIE_NOT_FOUND", "No selfie has been submitted yet."));
        Path path = fileStorage.resolve(row.getPhotoPath());
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PHOTO_MISSING", "The selfie file is missing.");
        }
        return new FileSystemResource(path);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
    }
}
