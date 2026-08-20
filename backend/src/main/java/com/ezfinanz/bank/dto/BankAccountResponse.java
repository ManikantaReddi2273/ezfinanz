package com.ezfinanz.bank.dto;

import com.ezfinanz.bank.domain.BankAccount;

import java.time.Instant;

public record BankAccountResponse(
        String accountHolderName,
        String accountNumber,
        String accountNumberMasked,
        String ifscCode,
        String bankName,
        Instant updatedAt
) {
    public static BankAccountResponse from(BankAccount row) {
        String number = row.getAccountNumber();
        String last4 = number.length() <= 4 ? number : number.substring(number.length() - 4);
        String masked = "XXXX" + last4;
        return new BankAccountResponse(
                row.getAccountHolderName(),
                number,
                masked,
                row.getIfscCode(),
                row.getBankName(),
                row.getUpdatedAt()
        );
    }
}
