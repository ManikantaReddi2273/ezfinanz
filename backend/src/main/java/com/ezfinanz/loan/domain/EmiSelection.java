package com.ezfinanz.loan.domain;

import com.ezfinanz.auth.domain.User;
import com.ezfinanz.eligibility.domain.CreditBand;
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
@Table(name = "emi_selections")
public class EmiSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false)
    private int tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditBand creditBand;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal annualInterestPercent;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal processingFee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal gstOnProcessingFee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal documentationFee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal stampDuty;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal otherCharges;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCharges;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyEmi;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalInterest;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRepayment;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netDisbursement;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal irrPercent;

    @Column(nullable = false)
    private Instant selectedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (selectedAt == null) {
            selectedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        Instant now = Instant.now();
        updatedAt = now;
        selectedAt = now;
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

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public CreditBand getCreditBand() {
        return creditBand;
    }

    public void setCreditBand(CreditBand creditBand) {
        this.creditBand = creditBand;
    }

    public BigDecimal getAnnualInterestPercent() {
        return annualInterestPercent;
    }

    public void setAnnualInterestPercent(BigDecimal annualInterestPercent) {
        this.annualInterestPercent = annualInterestPercent;
    }

    public BigDecimal getProcessingFee() {
        return processingFee;
    }

    public void setProcessingFee(BigDecimal processingFee) {
        this.processingFee = processingFee;
    }

    public BigDecimal getGstOnProcessingFee() {
        return gstOnProcessingFee;
    }

    public void setGstOnProcessingFee(BigDecimal gstOnProcessingFee) {
        this.gstOnProcessingFee = gstOnProcessingFee;
    }

    public BigDecimal getDocumentationFee() {
        return documentationFee;
    }

    public void setDocumentationFee(BigDecimal documentationFee) {
        this.documentationFee = documentationFee;
    }

    public BigDecimal getStampDuty() {
        return stampDuty;
    }

    public void setStampDuty(BigDecimal stampDuty) {
        this.stampDuty = stampDuty;
    }

    public BigDecimal getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(BigDecimal otherCharges) {
        this.otherCharges = otherCharges;
    }

    public BigDecimal getTotalCharges() {
        return totalCharges;
    }

    public void setTotalCharges(BigDecimal totalCharges) {
        this.totalCharges = totalCharges;
    }

    public BigDecimal getMonthlyEmi() {
        return monthlyEmi;
    }

    public void setMonthlyEmi(BigDecimal monthlyEmi) {
        this.monthlyEmi = monthlyEmi;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalRepayment() {
        return totalRepayment;
    }

    public void setTotalRepayment(BigDecimal totalRepayment) {
        this.totalRepayment = totalRepayment;
    }

    public BigDecimal getNetDisbursement() {
        return netDisbursement;
    }

    public void setNetDisbursement(BigDecimal netDisbursement) {
        this.netDisbursement = netDisbursement;
    }

    public BigDecimal getIrrPercent() {
        return irrPercent;
    }

    public void setIrrPercent(BigDecimal irrPercent) {
        this.irrPercent = irrPercent;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
