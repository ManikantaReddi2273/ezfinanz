package com.ezfinanz.admin.api;

import com.ezfinanz.admin.dto.AdminAccountResponse;
import com.ezfinanz.admin.dto.AdminStaffPage;
import com.ezfinanz.admin.dto.CreateAdminRequest;
import com.ezfinanz.admin.service.AdminStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the super admin to list, create, and delete staff admin accounts.
 */
@RestController
@RequestMapping("/api/admin/admins")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin staff", description = "Super admin can create additional admin logins")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    public AdminStaffController(AdminStaffService adminStaffService) {
        this.adminStaffService = adminStaffService;
    }

    /** Lists admin accounts and whether the caller may create more admins. */
    @GetMapping
    @Operation(summary = "List admin accounts")
    public AdminStaffPage list(Authentication authentication) {
        return adminStaffService.page((Long) authentication.getPrincipal());
    }

    /** Creates a new admin login (super admin only). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new admin (super admin only)")
    public AdminAccountResponse create(Authentication authentication, @Valid @RequestBody CreateAdminRequest request) {
        return adminStaffService.create((Long) authentication.getPrincipal(), request);
    }

    /** Deletes an admin account (super admin only; cannot delete self or super admin). */
    @DeleteMapping("/{adminId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an admin (super admin only)")
    public void delete(Authentication authentication, @PathVariable Long adminId) {
        adminStaffService.delete((Long) authentication.getPrincipal(), adminId);
    }
}
