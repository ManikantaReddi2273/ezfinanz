package com.ezfinanz.application;

import com.ezfinanz.auth.domain.User;
import com.ezfinanz.bank.repo.BankAccountRepository;
import com.ezfinanz.declaration.repo.DeclarationRepository;
import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import com.ezfinanz.eligibility.repo.EligibilityRepository;
import com.ezfinanz.kyc.repo.KycRepository;
import com.ezfinanz.loan.repo.EmiRepository;
import com.ezfinanz.selfie.domain.SelfieReviewStatus;
import com.ezfinanz.selfie.domain.SelfieSubmission;
import com.ezfinanz.selfie.repo.SelfieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationStatusService {

    private final KycRepository kycRepository;
    private final EligibilityRepository eligibilityRepository;
    private final EmiRepository emiRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DeclarationRepository declarationRepository;
    private final SelfieRepository selfieRepository;

    public ApplicationStatusService(
            KycRepository kycRepository,
            EligibilityRepository eligibilityRepository,
            EmiRepository emiRepository,
            BankAccountRepository bankAccountRepository,
            DeclarationRepository declarationRepository,
            SelfieRepository selfieRepository
    ) {
        this.kycRepository = kycRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.emiRepository = emiRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.declarationRepository = declarationRepository;
        this.selfieRepository = selfieRepository;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(User user) {
        Long userId = user.getId();
        boolean kyc = kycRepository.existsByUser_Id(userId);
        var eligibility = eligibilityRepository.findByUser_Id(userId);
        boolean eligibilityDone = eligibility.isPresent();
        boolean eligibilityPassed = eligibility
                .map(row -> row.getResult() == EligibilityResult.ELIGIBLE
                        || row.getResult() == EligibilityResult.PARTIALLY_ELIGIBLE)
                .orElse(false);
        String eligibilityResult = eligibility.map(row -> row.getResult().name()).orElse(null);
        boolean emi = emiRepository.existsByUser_Id(userId);
        boolean bank = bankAccountRepository.existsByUser_Id(userId);
        boolean declaration = declarationRepository.existsByUser_IdAndAcceptedIsTrue(userId);
        var selfie = selfieRepository.findByUser_Id(userId);
        boolean selfieSubmitted = selfie.isPresent();
        String selfieStatus = selfie.map(row -> row.getReviewStatus().name()).orElse(null);
        boolean disbursed = selfie.map(SelfieSubmission::isDisbursed).orElse(false);
        ApplicationStage stage = resolve(user, kyc, eligibility.orElse(null), eligibilityPassed, emi, bank, declaration, selfie.orElse(null));
        return new Snapshot(
                kyc,
                eligibilityDone,
                eligibilityPassed,
                eligibilityResult,
                emi,
                bank,
                declaration,
                selfieSubmitted,
                selfieStatus,
                disbursed,
                stage
        );
    }

    private static ApplicationStage resolve(
            User user,
            boolean kyc,
            EligibilityAssessment eligibility,
            boolean eligibilityPassed,
            boolean emi,
            boolean bank,
            boolean declaration,
            SelfieSubmission selfie
    ) {
        if (selfie != null && selfie.isDisbursed()) {
            return ApplicationStage.DISBURSED;
        }
        if (selfie != null && selfie.getReviewStatus() == SelfieReviewStatus.APPROVED) {
            return ApplicationStage.READY_FOR_DISBURSEMENT;
        }
        if (selfie != null && selfie.getReviewStatus() == SelfieReviewStatus.PENDING) {
            return ApplicationStage.WAITING_FOR_ADMIN_REVIEW;
        }
        if (selfie != null && selfie.getReviewStatus() == SelfieReviewStatus.REJECTED) {
            return ApplicationStage.SELFIE_REJECTED;
        }
        if (declaration) {
            return ApplicationStage.LIVE_SELFIE;
        }
        if (bank) {
            return ApplicationStage.DECLARATION;
        }
        if (emi) {
            return ApplicationStage.BANK;
        }
        if (eligibility != null && !eligibilityPassed) {
            return ApplicationStage.NOT_ELIGIBLE;
        }
        if (eligibilityPassed) {
            return ApplicationStage.EMI;
        }
        if (kyc) {
            return ApplicationStage.ELIGIBILITY;
        }
        if (user.isFullyVerified()) {
            return ApplicationStage.KYC;
        }
        return ApplicationStage.CONTACTS_PENDING;
    }

    public record Snapshot(
            boolean kycCompleted,
            boolean eligibilityCompleted,
            boolean eligibilityPassed,
            String eligibilityResult,
            boolean emiCompleted,
            boolean bankCompleted,
            boolean declarationCompleted,
            boolean selfieSubmitted,
            String selfieStatus,
            boolean disbursed,
            ApplicationStage stage
    ) {
        public String stageLabel() {
            return stage.getLabel();
        }
    }
}
