package com.ezfinanz.admin.dto;

import com.ezfinanz.auth.domain.User;

import java.time.Instant;

/** API view of an admin staff account, including whether it is the super admin. */
public record AdminAccountResponse(
        Long id,
        String email,
        String fullName,
        Instant createdAt,
        boolean superAdmin
) {
    /** Maps a user entity to an admin account response. */
    public static AdminAccountResponse from(User user, boolean superAdmin) {
        return new AdminAccountResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt(), superAdmin);
    }
}
