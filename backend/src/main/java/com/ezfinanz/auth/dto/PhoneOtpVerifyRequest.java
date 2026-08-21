package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body to verify a phone OTP and obtain a JWT. */
public record PhoneOtpVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Enter a valid phone number")
        String phone,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp
) {
}
