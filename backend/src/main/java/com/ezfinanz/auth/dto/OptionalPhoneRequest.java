package com.ezfinanz.auth.dto;

import jakarta.validation.constraints.Pattern;

public record OptionalPhoneRequest(
        @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Enter a valid phone number")
        String phone
) {
}
