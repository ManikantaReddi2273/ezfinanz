package com.ezfinanz.admin.dto;

import com.ezfinanz.application.ApplicationStage;
import com.ezfinanz.bank.dto.BankAccountResponse;
import com.ezfinanz.declaration.dto.DeclarationResponse;
import com.ezfinanz.eligibility.dto.EligibilityResponse;
import com.ezfinanz.kyc.dto.KycResponse;
import com.ezfinanz.loan.dto.EmiQuoteResponse;
import com.ezfinanz.selfie.dto.SelfieResponse;

/** Full admin view of one customer's loan application across all journey steps. */
public record AdminApplicationDetail(
        Long userId,
        String applicantName,
        String email,
        String phone,
        boolean emailVerified,
        boolean phoneVerified,
        boolean fullyVerified,
        ApplicationStage currentStage,
        String currentStageLabel,
        KycResponse kyc,
        EligibilityResponse eligibility,
        EmiQuoteResponse emi,
        BankAccountResponse bankAccount,
        DeclarationResponse declaration,
        SelfieResponse selfie
) {
}
