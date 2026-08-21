package com.ezfinanz.application;

import com.ezfinanz.common.ApiException;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.repo.SelfieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Prevents edits to application steps once the loan journey is submitted, approved, or disbursed.
 */
@Service
public class ApplicationLockService {

    private final SelfieRepository selfieRepository;

    public ApplicationLockService(SelfieRepository selfieRepository) {
        this.selfieRepository = selfieRepository;
    }

    /** Throws if the customer's application is locked against further changes. */
    public void requireEditable(Long userId) {
        selfieRepository.findByUser_Id(userId).ifPresent(row -> {
            if (row.isDisbursed()
                    || row.getReviewStatus() == SelfieReviewStatus.APPROVED
                    || row.getReviewStatus() == SelfieReviewStatus.PENDING) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "APPLICATION_LOCKED",
                        "This application has been submitted and cannot be changed."
                );
            }
        });
    }
}
