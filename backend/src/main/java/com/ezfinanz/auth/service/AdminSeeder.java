package com.ezfinanz.auth.service;

import com.ezfinanz.auth.domain.Role;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.full-name}") String adminFullName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
    }

    @Override
    public void run(String... args) {
        String email = adminEmail.trim().toLowerCase();
        User admin = userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN).stream().findFirst())
                .orElseGet(User::new);
        admin.setEmail(email);
        admin.setFullName(adminFullName);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        admin.setPhoneVerified(true);
        userRepository.save(admin);
    }
}
