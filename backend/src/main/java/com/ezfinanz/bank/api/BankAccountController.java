package com.ezfinanz.bank.api;

import com.ezfinanz.bank.dto.BankAccountRequest;
import com.ezfinanz.bank.dto.BankAccountResponse;
import com.ezfinanz.bank.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the customer's loan disbursement bank account.
 */
@RestController
@RequestMapping("/api/bank")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bank", description = "Disbursement bank account")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    /** Returns the saved disbursement bank account. */
    @GetMapping
    @Operation(summary = "Get saved disbursement account")
    public BankAccountResponse get(Authentication authentication) {
        return bankAccountService.get((Long) authentication.getPrincipal());
    }

    /** Creates or updates the account used for loan disbursement. */
    @PostMapping
    @Operation(summary = "Save disbursement bank account")
    public BankAccountResponse save(Authentication authentication, @Valid @RequestBody BankAccountRequest request) {
        return bankAccountService.save((Long) authentication.getPrincipal(), request);
    }
}
