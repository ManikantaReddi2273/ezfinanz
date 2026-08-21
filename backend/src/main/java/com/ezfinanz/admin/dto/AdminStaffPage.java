package com.ezfinanz.admin.dto;

import java.util.List;

/** Admin staff listing payload: create permission flag plus admin accounts. */
public record AdminStaffPage(
        boolean canCreateAdmins,
        List<AdminAccountResponse> admins
) {
}
