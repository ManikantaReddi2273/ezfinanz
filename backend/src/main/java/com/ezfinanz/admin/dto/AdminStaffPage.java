package com.ezfinanz.admin.dto;

import java.util.List;

public record AdminStaffPage(
        boolean canCreateAdmins,
        List<AdminAccountResponse> admins
) {
}
