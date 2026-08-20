package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Email;

public record OptionalEmailRequest(
        @Email String email
) {
}
