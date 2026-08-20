package com.ezfinanz.eligibility.domain;

import com.ezfinanz.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "eligibility_assessments")
public class EligibilityAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncomeType incomeType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal incomeAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal requestedLoanAmount;

    @Column(nullable = false)
    private int creditScore;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingDebts;

    @Column(nullable = false, length = 120)
    private String employerName;

    @Column(nullable = false, length = 120)
    private String designation;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal annualIncome;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal dtiRatio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditBand creditBand;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal maxEligibleAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EligibilityResult result;

    @Column(nullable = false, length = 2000)
    private String reasons;

    @Column(nullable = false)
    private Instant assessedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (assessedAt == null) {
            assessedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        Instant now = Instant.now();
        updatedAt = now;
        assessedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public BigDecimal getDtiRatio() {
        return dtiRatio;
    }

    public void setDtiRatio(BigDecimal dtiRatio) {
        this.dtiRatio = dtiRatio;
    }

    public CreditBand getCreditBand() {
        return creditBand;
    }

    public void setCreditBand(CreditBand creditBand) {
        this.creditBand = creditBand;
    }

    public BigDecimal getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public void setMaxEligibleAmount(BigDecimal maxEligibleAmount) {
        this.maxEligibleAmount = maxEligibleAmount;
    }

    public EligibilityResult getResult() {
        return result;
    }

    public void setResult(EligibilityResult result) {
        this.result = result;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
