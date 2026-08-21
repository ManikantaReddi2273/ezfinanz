package com.ezfinanz.auth.dto;

import com.ezfinanz.application.ApplicationStatusService;
import com.ezfinanz.auth.domain.Role;
import com.ezfinanz.auth.domain.User;

/**
 * User profile returned to the client, including loan-application progress flags and stage.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean emailVerified,
        boolean phoneVerified,
        boolean fullyVerified,
        boolean kycCompleted,
        boolean eligibilityCompleted,
        boolean eligibilityPassed,
        String eligibilityResult,
        boolean emiCompleted,
        boolean bankCompleted,
        boolean declarationCompleted,
        boolean selfieSubmitted,
        String selfieStatus,
        boolean disbursed,
        String applicationStage,
        String applicationStageLabel
) {
    /** Maps a {@link User} and application status snapshot to the API DTO. */
    public static UserResponse from(User user, ApplicationStatusService.Snapshot snapshot) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.isFullyVerified(),
                snapshot.kycCompleted(),
                snapshot.eligibilityCompleted(),
                snapshot.eligibilityPassed(),
                snapshot.eligibilityResult(),
                snapshot.emiCompleted(),
                snapshot.bankCompleted(),
                snapshot.declarationCompleted(),
                snapshot.selfieSubmitted(),
                snapshot.selfieStatus(),
                snapshot.disbursed(),
                snapshot.stage().name(),
                snapshot.stageLabel()
        );
    }
}
