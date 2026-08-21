package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body to resend an email verification OTP. */
public record EmailOtpRequest(
        @NotBlank @Email String email
) {
}
