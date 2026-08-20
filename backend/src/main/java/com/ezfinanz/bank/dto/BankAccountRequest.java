package com.ezfinanz.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BankAccountRequest {

    @NotBlank
    @Size(max = 120)
    private String accountHolderName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be 9 to 18 digits")
    private String accountNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "Enter a valid IFSC (for example HDFC0001234)")
    private String ifscCode;

    @NotBlank
    @Size(max = 120)
    private String bankName;

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
}
