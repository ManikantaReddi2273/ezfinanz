package com.ezfinanz.loan.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class EmiSaveRequest {

    @NotNull
    private BigDecimal principal;

    @NotNull
    private Integer tenureMonths;

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }
}
