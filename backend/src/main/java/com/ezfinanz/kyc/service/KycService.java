package com.ezfinanz.kyc.service;

import com.ezfinanz.application.ApplicationCascadeService;
import com.ezfinanz.application.ApplicationLockService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.files.LocalFileStorage;
import com.ezfinanz.kyc.domain.Gender;
import com.ezfinanz.kyc.domain.IdType;
import com.ezfinanz.kyc.domain.KycProfile;
import com.ezfinanz.kyc.dto.KycResponse;
import com.ezfinanz.kyc.repo.KycRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;

/**
 * Persists and validates customer KYC profiles, including optional ID document storage.
 * Editing KYC after later steps may cascade-invalidate downstream application data.
 */
@Service
public class KycService {

    private final KycRepository kycRepository;
    private final UserRepository userRepository;
    private final LocalFileStorage fileStorage;
    private final ApplicationLockService applicationLockService;
    private final ApplicationCascadeService applicationCascadeService;

    public KycService(
            KycRepository kycRepository,
            UserRepository userRepository,
            LocalFileStorage fileStorage,
            ApplicationLockService applicationLockService,
            ApplicationCascadeService applicationCascadeService
    ) {
        this.kycRepository = kycRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.applicationLockService = applicationLockService;
        this.applicationCascadeService = applicationCascadeService;
    }

    /** Loads the KYC profile for the given customer, or throws if none exists. */
    @Transactional(readOnly = true)
    public KycResponse get(Long userId) {
        KycProfile profile = kycRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC_NOT_FOUND", "KYC has not been submitted yet."));
        return KycResponse.from(profile);
    }

    /**
     * Creates or updates KYC for a verified customer while the application is still editable.
     * Optionally stores an uploaded ID document and invalidates later steps on update.
     */
    @Transactional
    public KycResponse save(
            Long userId,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String addressLine,
            String city,
            String state,
            String pincode,
            IdType idType,
            String idNumber,
            MultipartFile document
    ) {
        applicationLockService.requireEditable(userId);
        boolean updating = kycRepository.existsByUser_Id(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        if (!user.isFullyVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "CONTACTS_NOT_VERIFIED",
                    "Verify email and phone before submitting KYC."
            );
        }
        validate(dateOfBirth, pincode, idType, idNumber);

        KycProfile profile = kycRepository.findByUser_Id(userId).orElseGet(KycProfile::new);
        profile.setUser(user);
        profile.setFullName(fullName.trim());
        profile.setDateOfBirth(dateOfBirth);
        profile.setGender(gender);
        profile.setAddressLine(addressLine.trim());
        profile.setCity(city.trim());
        profile.setState(state.trim());
        profile.setPincode(pincode.trim());
        profile.setIdType(idType);
        profile.setIdNumber(idNumber.trim().toUpperCase());

        if (document != null && !document.isEmpty()) {
            LocalFileStorage.StoredFile stored = fileStorage.saveKycDocument(userId, document);
            if (stored != null) {
                profile.setIdDocumentPath(stored.relativePath());
                profile.setIdDocumentOriginalName(stored.originalName());
            }
        }

        user.setFullName(profile.getFullName());
        userRepository.save(user);
        KycResponse response = KycResponse.from(kycRepository.save(profile));
        if (updating) {
            applicationCascadeService.invalidateAfterKyc(userId);
        }
        return response;
    }

    /** Returns the stored ID document as a downloadable resource. */
    @Transactional(readOnly = true)
    public Resource document(Long userId) {
        KycProfile profile = kycRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KYC_NOT_FOUND", "KYC has not been submitted yet."));
        if (profile.getIdDocumentPath() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "No ID document was uploaded.");
        }
        if (!fileStorage.exists(profile.getIdDocumentPath())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "The ID document file is missing.");
        }
        return fileStorage.asResource(profile.getIdDocumentPath(), profile.getIdDocumentOriginalName());
    }

    private void validate(LocalDate dateOfBirth, String pincode, IdType idType, String idNumber) {
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now().minusYears(18))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AGE_INVALID", "Applicant must be at least 18 years old.");
        }
        if (Period.between(dateOfBirth, LocalDate.now()).getYears() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AGE_INVALID", "Enter a valid date of birth.");
        }
        if (pincode == null || !pincode.trim().matches("^[0-9]{6}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PINCODE_INVALID", "Enter a 6-digit pincode.");
        }
        String id = idNumber == null ? "" : idNumber.trim().toUpperCase();
        if (idType == IdType.PAN && !id.matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Enter a valid PAN (for example ABCDE1234F).");
        }
        if (idType == IdType.AADHAAR && !id.matches("^[0-9]{12}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Enter a 12-digit Aadhaar number.");
        }
        if (id.isBlank() || id.length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Enter a valid ID number.");
        }
    }
}
