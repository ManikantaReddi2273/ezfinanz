package com.ezfinanz.admin.service;

import com.ezfinanz.admin.dto.AdminAccountResponse;
import com.ezfinanz.admin.dto.AdminStaffPage;
import com.ezfinanz.admin.dto.CreateAdminRequest;
import com.ezfinanz.auth.domain.Role;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super-admin management of staff admin accounts (list, create, delete).
 */
@Service
public class AdminStaffService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String superAdminEmail;

    public AdminStaffService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:campusworks2273@gmail.com}") String superAdminEmail
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.superAdminEmail = superAdminEmail.trim().toLowerCase();
    }

    /** Returns admin accounts plus whether the actor may create more admins. */
    @Transactional(readOnly = true)
    public AdminStaffPage page(Long actorId) {
        return new AdminStaffPage(
                isSuperAdmin(actorId),
                userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN).stream()
                        .map(row -> AdminAccountResponse.from(row, superAdminEmail.equalsIgnoreCase(row.getEmail())))
                        .toList()
        );
    }

    /** Creates a new admin user (super admin only). */
    @Transactional
    public AdminAccountResponse create(Long actorId, CreateAdminRequest request) {
        requireSuperAdmin(actorId);
        String email = request.email().trim().toLowerCase();
        if (email.equals(superAdminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SUPERADMIN_RESERVED", "This email is reserved for the super admin.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists."
            );
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setFullName(request.fullName() == null || request.fullName().isBlank() ? "EZFINANZ Admin" : request.fullName().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        admin.setPhoneVerified(true);
        userRepository.save(admin);
        return AdminAccountResponse.from(admin, false);
    }

    /** Deletes a non-super admin account (super admin only). */
    @Transactional
    public void delete(Long actorId, Long adminId) {
        requireSuperAdmin(actorId);
        if (actorId.equals(adminId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_SELF", "You cannot delete your own admin account.");
        }
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "Admin account not found."));
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "Admin account not found.");
        }
        if (superAdminEmail.equalsIgnoreCase(admin.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_SUPERADMIN", "The super admin account cannot be deleted.");
        }
        userRepository.delete(admin);
    }

    private void requireSuperAdmin(Long actorId) {
        if (!isSuperAdmin(actorId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "SUPERADMIN_REQUIRED",
                    "Only the super admin can manage admin accounts."
            );
        }
    }

    private boolean isSuperAdmin(Long actorId) {
        return userRepository.findById(actorId)
                .map(User::getEmail)
                .filter(email -> superAdminEmail.equalsIgnoreCase(email))
                .isPresent();
    }
}
