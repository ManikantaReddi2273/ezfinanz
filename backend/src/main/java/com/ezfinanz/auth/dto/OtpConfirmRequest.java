package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body with a 6-digit OTP to confirm email or phone verification. */
public record OtpConfirmRequest(
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp
) {
}
