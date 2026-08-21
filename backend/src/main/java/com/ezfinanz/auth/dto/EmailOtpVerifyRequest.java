package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body to verify an email OTP during sign-up. */
public record EmailOtpVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp
) {
}
