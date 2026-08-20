package com.ezfinanz.declaration.dto;

import com.ezfinanz.declaration.domain.LoanDeclaration;

import java.time.Instant;

public record DeclarationResponse(
        boolean accepted,
        String termsVersion,
        Instant acceptedAt
) {
    public static DeclarationResponse from(LoanDeclaration row) {
        return new DeclarationResponse(row.isAccepted(), row.getTermsVersion(), row.getAcceptedAt());
    }

    public static DeclarationResponse empty(String termsVersion) {
        return new DeclarationResponse(false, termsVersion, null);
    }
}
