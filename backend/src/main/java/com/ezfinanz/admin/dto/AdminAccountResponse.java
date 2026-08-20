package com.ezfinanz.admin.dto;

import com.ezfinanz.auth.domain.User;

import java.time.Instant;

public record AdminAccountResponse(
        Long id,
        String email,
        String fullName,
        Instant createdAt,
        boolean superAdmin
) {
    public static AdminAccountResponse from(User user, boolean superAdmin) {
        return new AdminAccountResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt(), superAdmin);
    }
}
