package com.ezfinanz.kyc.dto;

import com.ezfinanz.kyc.domain.Gender;
import com.ezfinanz.kyc.domain.IdType;
import com.ezfinanz.kyc.domain.KycProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

/** API view of a submitted KYC profile, including computed age and document presence. */
public record KycResponse(
        String fullName,
        LocalDate dateOfBirth,
        int age,
        Gender gender,
        String addressLine,
        String city,
        String state,
        String pincode,
        IdType idType,
        String idNumber,
        boolean hasDocument,
        String documentFileName,
        Instant submittedAt
) {
    /** Maps a persisted KYC entity to the API response shape. */
    public static KycResponse from(KycProfile profile) {
        int age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
        return new KycResponse(
                profile.getFullName(),
                profile.getDateOfBirth(),
                age,
                profile.getGender(),
                profile.getAddressLine(),
                profile.getCity(),
                profile.getState(),
                profile.getPincode(),
                profile.getIdType(),
                profile.getIdNumber(),
                profile.getIdDocumentPath() != null,
                profile.getIdDocumentOriginalName(),
                profile.getSubmittedAt()
        );
    }
}
