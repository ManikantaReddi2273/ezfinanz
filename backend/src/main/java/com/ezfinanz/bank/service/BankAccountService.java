package com.ezfinanz.bank.service;

import com.ezfinanz.application.ApplicationLockService;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.bank.domain.BankAccount;
import com.ezfinanz.bank.dto.BankAccountRequest;
import com.ezfinanz.bank.dto.BankAccountResponse;
import com.ezfinanz.bank.repo.BankAccountRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.loan.repo.EmiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final EmiRepository emiRepository;
    private final UserRepository userRepository;
    private final ApplicationLockService applicationLockService;

    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            EmiRepository emiRepository,
            UserRepository userRepository,
            ApplicationLockService applicationLockService
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.emiRepository = emiRepository;
        this.userRepository = userRepository;
        this.applicationLockService = applicationLockService;
    }

    @Transactional(readOnly = true)
    public BankAccountResponse get(Long userId) {
        requireEmi(userId);
        BankAccount row = bankAccountRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BANK_NOT_FOUND", "Bank account has not been added yet."));
        return BankAccountResponse.from(row);
    }

    @Transactional
    public BankAccountResponse save(Long userId, BankAccountRequest request) {
        applicationLockService.requireEditable(userId);
        requireEmi(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        BankAccount row = bankAccountRepository.findByUser_Id(userId).orElseGet(BankAccount::new);
        row.setUser(user);
        row.setAccountHolderName(request.getAccountHolderName().trim());
        row.setAccountNumber(request.getAccountNumber().trim());
        row.setIfscCode(request.getIfscCode().trim().toUpperCase());
        row.setBankName(request.getBankName().trim());
        return BankAccountResponse.from(bankAccountRepository.save(row));
    }

    private void requireEmi(Long userId) {
        if (!emiRepository.existsByUser_Id(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMI_REQUIRED", "Confirm EMI terms before adding a bank account.");
        }
    }
}
