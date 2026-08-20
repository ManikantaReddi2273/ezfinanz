package com.ezfinanz.declaration.service;

import com.ezfinanz.application.ApplicationCascadeService;
import com.ezfinanz.application.ApplicationLockService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.bank.repo.BankAccountRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.declaration.domain.LoanDeclaration;
import com.ezfinanz.declaration.dto.DeclarationRequest;
import com.ezfinanz.declaration.dto.DeclarationResponse;
import com.ezfinanz.declaration.repo.DeclarationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeclarationService {

    public static final String TERMS_VERSION = "2026-08";

    private final DeclarationRepository declarationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final ApplicationLockService applicationLockService;
    private final ApplicationCascadeService applicationCascadeService;

    public DeclarationService(
            DeclarationRepository declarationRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            ApplicationLockService applicationLockService,
            ApplicationCascadeService applicationCascadeService
    ) {
        this.declarationRepository = declarationRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.applicationLockService = applicationLockService;
        this.applicationCascadeService = applicationCascadeService;
    }

    @Transactional(readOnly = true)
    public DeclarationResponse get(Long userId) {
        requireBank(userId);
        return declarationRepository.findByUser_Id(userId)
                .map(DeclarationResponse::from)
                .orElseGet(() -> DeclarationResponse.empty(TERMS_VERSION));
    }

    @Transactional
    public DeclarationResponse accept(Long userId, DeclarationRequest request) {
        applicationLockService.requireEditable(userId);
        boolean updating = declarationRepository.findByUser_Id(userId).map(LoanDeclaration::isAccepted).orElse(false);
        requireBank(userId);
        if (!request.isAccepted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DECLARATION_REQUIRED", "Accept the declaration to continue.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        LoanDeclaration row = declarationRepository.findByUser_Id(userId).orElseGet(LoanDeclaration::new);
        row.setUser(user);
        row.setAccepted(true);
        row.setTermsVersion(TERMS_VERSION);
        DeclarationResponse response = DeclarationResponse.from(declarationRepository.save(row));
        if (updating) {
            applicationCascadeService.invalidateAfterDeclaration(userId);
        }
        return response;
    }

    private void requireBank(Long userId) {
        if (!bankAccountRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BANK_REQUIRED", "Add a bank account before confirming the declaration.");
        }
    }
}
