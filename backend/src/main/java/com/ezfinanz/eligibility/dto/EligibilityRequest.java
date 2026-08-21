package com.ezfinanz.eligibility.dto;

import com.ezfinanz.eligibility.domain.IncomeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Request body for submitting financial details used in eligibility assessment. */
public class EligibilityRequest {

    @NotNull
    private IncomeType incomeType;

    @NotNull
    @DecimalMin(value = "1.00", message = "Enter your income")
    private BigDecimal incomeAmount;

    @NotNull
    @DecimalMin(value = "25000.00", message = "Minimum loan amount is ₹25,000")
    private BigDecimal requestedLoanAmount;

    @Min(300)
    @Max(900)
    private int creditScore;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal outstandingDebts;

    @NotBlank
    private String employerName;

    @NotBlank
    private String designation;

    public IncomeType getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(IncomeType incomeType) {
        this.incomeType = incomeType;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public void setIncomeAmount(BigDecimal incomeAmount) {
        this.incomeAmount = incomeAmount;
    }

    public BigDecimal getRequestedLoanAmount() {
        return requestedLoanAmount;
    }

    public void setRequestedLoanAmount(BigDecimal requestedLoanAmount) {
        this.requestedLoanAmount = requestedLoanAmount;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public BigDecimal getOutstandingDebts() {
        return outstandingDebts;
    }

    public void setOutstandingDebts(BigDecimal outstandingDebts) {
        this.outstandingDebts = outstandingDebts;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
