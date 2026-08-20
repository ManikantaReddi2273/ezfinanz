package com.ezfinanz.auth.service;

import com.ezfinanz.auth.domain.OtpChannel;
import com.ezfinanz.auth.domain.OtpPurpose;
import com.ezfinanz.auth.domain.Role;
import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.dto.AuthResponse;
import com.ezfinanz.auth.dto.UserResponse;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.application.ApplicationStatusService;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.config.JwtService;
import com.ezfinanz.notify.EmailOtpService;
import com.ezfinanz.notify.TwilioSmsService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailOtpService emailOtpService;
    private final TwilioSmsService twilioSmsService;
    private final JwtService jwtService;
    private final ApplicationStatusService applicationStatusService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            EmailOtpService emailOtpService,
            TwilioSmsService twilioSmsService,
            JwtService jwtService,
            ApplicationStatusService applicationStatusService,
            GoogleIdTokenVerifier googleIdTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailOtpService = emailOtpService;
        this.twilioSmsService = twilioSmsService;
        this.jwtService = jwtService;
        this.applicationStatusService = applicationStatusService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    public MessageOrAuth signupEmail(String email, String password, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        User existing = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existing != null) {
            if (existing.isEmailVerified()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EMAIL_ALREADY_REGISTERED",
                        "An account with this email already exists."
                );
            }
            sendEmailOtp(normalizedEmail, OtpPurpose.VERIFY_EMAIL);
            return MessageOrAuth.message("Verification code sent to your email.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(blankToNull(fullName));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.CUSTOMER);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        userRepository.save(user);
        sendEmailOtp(normalizedEmail, OtpPurpose.SIGNUP);
        return MessageOrAuth.message("Verification code sent to your email.");
    }

    @Transactional
    public AuthResponse loginEmail(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "Invalid email or password"
                ));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED",
                    "Verify your email before logging in."
            );
        }
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginGoogle(String idToken) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(idToken);
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_REQUIRED", "Google did not return an email address.");
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_UNVERIFIED", "Google email is not verified.");
        }

        String normalizedEmail = normalizeEmail(email);
        String name = blankToNull((String) payload.get("name"));

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(name);
            user.setRole(Role.CUSTOMER);
            user.setEmailVerified(true);
            user.setPhoneVerified(false);
            userRepository.save(user);
            return toAuthResponse(user);
        }

        user.setEmailVerified(true);
        if ((user.getFullName() == null || user.getFullName().isBlank()) && name != null) {
            user.setFullName(name);
        }
        userRepository.save(user);
        return toAuthResponse(user);
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idToken) {
        try {
            GoogleIdToken token = googleIdTokenVerifier.verify(idToken);
            if (token == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google sign-in could not be verified.");
            }
            return token.getPayload();
        } catch (ApiException ex) {
            throw ex;
        } catch (GeneralSecurityException | IOException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google sign-in could not be verified.");
        }
    }

    public MessageOrAuth resendEmailOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No account found for this email."));
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_VERIFIED", "Email is already verified.");
        }
        sendEmailOtp(normalizedEmail, OtpPurpose.VERIFY_EMAIL);
        return MessageOrAuth.message("Verification code sent to your email.");
    }

    public AuthResponse verifyEmailOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No account found for this email."));
        otpService.verify(normalizedEmail, OtpChannel.EMAIL, otp);
        user.setEmailVerified(true);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    public MessageOrAuth sendPhoneOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (userRepository.findByPhone(normalizedPhone).isEmpty()) {
            User created = new User();
            created.setPhone(normalizedPhone);
            created.setRole(Role.CUSTOMER);
            created.setEmailVerified(false);
            created.setPhoneVerified(false);
            userRepository.save(created);
        }
        twilioSmsService.sendOtp(normalizedPhone);
        return MessageOrAuth.message("Verification code sent to your phone.");
    }

    @Transactional
    public AuthResponse verifyPhoneOtp(String phone, String otp) {
        String normalizedPhone = normalizePhone(phone);
        User user = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No account found for this phone number."));
        twilioSmsService.checkOtp(normalizedPhone, otp);
        user.setPhoneVerified(true);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return toUserResponse(requireUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String fullName) {
        User user = requireUser(userId);
        user.setFullName(fullName.trim());
        userRepository.save(user);
        return toUserResponse(user);
    }

    public MessageOrAuth sendEmailVerification(Long userId, String requestedEmail) {
        User user = requireUser(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_REQUIRED", "Admin accounts do not need this step.");
        }
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_VERIFIED", "Email is already verified.");
        }

        String target = resolveEmailToVerify(user, requestedEmail);
        ensureEmailAvailable(target, user.getId());
        if (user.getEmail() == null || !target.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(target);
            user.setEmailVerified(false);
            userRepository.save(user);
        }
        sendEmailOtp(target, OtpPurpose.VERIFY_EMAIL);
        return MessageOrAuth.message("Verification code sent to your email.");
    }

    public UserResponse confirmEmailVerification(Long userId, String otp) {
        User user = requireUser(userId);
        if (user.getEmail() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "Add an email address first.");
        }
        otpService.verify(normalizeEmail(user.getEmail()), OtpChannel.EMAIL, otp);
        user.setEmailVerified(true);
        userRepository.save(user);
        return toUserResponse(user);
    }

    public MessageOrAuth sendPhoneVerification(Long userId, String requestedPhone) {
        User user = requireUser(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_REQUIRED", "Admin accounts do not need this step.");
        }
        if (user.isPhoneVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_VERIFIED", "Phone is already verified.");
        }

        String target = resolvePhoneToVerify(user, requestedPhone);
        ensurePhoneAvailable(target, user.getId());
        if (user.getPhone() == null || !target.equals(user.getPhone())) {
            user.setPhone(target);
            user.setPhoneVerified(false);
            userRepository.save(user);
        }
        twilioSmsService.sendOtp(target);
        return MessageOrAuth.message("Verification code sent to your phone.");
    }

    public UserResponse confirmPhoneVerification(Long userId, String otp) {
        User user = requireUser(userId);
        if (user.getPhone() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_REQUIRED", "Add a phone number first.");
        }
        twilioSmsService.checkOtp(user.getPhone(), otp);
        user.setPhoneVerified(true);
        userRepository.save(user);
        return toUserResponse(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
    }

    private String resolveEmailToVerify(User user, String requestedEmail) {
        if (requestedEmail != null && !requestedEmail.isBlank()) {
            return normalizeEmail(requestedEmail);
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return normalizeEmail(user.getEmail());
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "Enter an email address to verify.");
    }

    private String resolvePhoneToVerify(User user, String requestedPhone) {
        if (requestedPhone != null && !requestedPhone.isBlank()) {
            return normalizePhone(requestedPhone);
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_REQUIRED", "Enter a phone number to verify.");
    }

    private void ensureEmailAvailable(String email, Long userId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EMAIL_ALREADY_REGISTERED",
                        "This email is already used by another account."
                );
            }
        });
    }

    private void ensurePhoneAvailable(String phone, Long userId) {
        userRepository.findByPhone(phone).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "PHONE_ALREADY_REGISTERED",
                        "This phone number is already used by another account."
                );
            }
        });
    }

    private void sendEmailOtp(String email, OtpPurpose purpose) {
        String code = otpService.issue(email, OtpChannel.EMAIL, purpose);
        emailOtpService.sendOtp(email, code, otpService.getTtlMinutes());
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.createToken(user.getId(), user.getRole().name());
        return AuthResponse.of(token, toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.from(user, applicationStatusService.snapshot(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    static String normalizePhone(String phone) {
        String trimmed = phone.trim().replaceAll("[\\s-()]", "");
        if (trimmed.matches("^[0-9]{10}$")) {
            return "+91" + trimmed;
        }
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        return "+" + trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record MessageOrAuth(String message, AuthResponse auth) {
        static MessageOrAuth message(String message) {
            return new MessageOrAuth(message, null);
        }
    }
}
