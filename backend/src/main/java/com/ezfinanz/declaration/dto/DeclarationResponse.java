package com.ezfinanz.declaration.dto;

import com.ezfinanz.declaration.domain.LoanDeclaration;

import java.time.Instant;

/** API view of declaration acceptance status and terms version. */
public record DeclarationResponse(
        boolean accepted,
        String termsVersion,
        Instant acceptedAt
) {
    /** Maps a persisted declaration to the API response. */
    public static DeclarationResponse from(LoanDeclaration row) {
        return new DeclarationResponse(row.isAccepted(), row.getTermsVersion(), row.getAcceptedAt());
    }

    /** Empty (not yet accepted) response for the given terms version. */
    public static DeclarationResponse empty(String termsVersion) {
        return new DeclarationResponse(false, termsVersion, null);
    }
}
