package com.ezfinanz.application;

import com.ezfinanz.bank.repo.BankAccountRepository;
import com.ezfinanz.declaration.repo.DeclarationRepository;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.files.LocalFileStorage;
import com.ezfinanz.loan.repo.EmiRepository;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;
import com.ezfinanz.selfie.repo.SelfieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationCascadeService {

    private final EligibilityRepository eligibilityRepository;
    private final EmiRepository emiRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DeclarationRepository declarationRepository;
    private final SelfieRepository selfieRepository;
    private final LocalFileStorage fileStorage;

    public ApplicationCascadeService(
            EligibilityRepository eligibilityRepository,
            EmiRepository emiRepository,
            BankAccountRepository bankAccountRepository,
            DeclarationRepository declarationRepository,
            SelfieRepository selfieRepository,
            LocalFileStorage fileStorage
    ) {
        this.eligibilityRepository = eligibilityRepository;
        this.emiRepository = emiRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.declarationRepository = declarationRepository;
        this.selfieRepository = selfieRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public void invalidateAfterKyc(Long userId) {
        eligibilityRepository.findByUser_Id(userId).ifPresent(eligibilityRepository::delete);
        invalidateAfterEligibility(userId);
    }

    @Transactional
    public void invalidateAfterEligibility(Long userId) {
        emiRepository.findByUser_Id(userId).ifPresent(emiRepository::delete);
        invalidateAfterEmi(userId);
    }

    @Transactional
    public void invalidateAfterEmi(Long userId) {
        bankAccountRepository.findByUser_Id(userId).ifPresent(bankAccountRepository::delete);
        invalidateAfterBank(userId);
    }

    @Transactional
    public void invalidateAfterBank(Long userId) {
        declarationRepository.findByUser_Id(userId).ifPresent(declarationRepository::delete);
        invalidateAfterDeclaration(userId);
    }

    @Transactional
    public void invalidateAfterDeclaration(Long userId) {
        clearDraftSelfie(userId);
    }

    private void clearDraftSelfie(Long userId) {
        selfieRepository.findByUser_Id(userId).ifPresent(row -> {
            if (row.isDisbursed()
                    || row.getReviewStatus() == SelfieReviewStatus.PENDING
                    || row.getReviewStatus() == SelfieReviewStatus.APPROVED) {
                return;
            }
            deletePhoto(row);
            selfieRepository.delete(row);
        });
    }

    private void deletePhoto(SelfieSubmission row) {
        if (row.getPhotoPath() == null) {
            return;
        }
        try {
            fileStorage.delete(row.getPhotoPath());
        } catch (Exception ignored) {
            // Best-effort cleanup; missing files are handled on read.
        }
    }
}
