package com.ezfinanz.auth.api;

import com.ezfinanz.auth.dto.AuthResponse;
import com.ezfinanz.auth.dto.EmailOtpRequest;
import com.ezfinanz.auth.dto.EmailOtpVerifyRequest;
import com.ezfinanz.auth.dto.GoogleLoginRequest;
import com.ezfinanz.auth.dto.LoginEmailRequest;
import com.ezfinanz.auth.dto.MessageResponse;
import com.ezfinanz.auth.dto.OptionalEmailRequest;
import com.ezfinanz.auth.dto.OptionalPhoneRequest;
import com.ezfinanz.auth.dto.OtpConfirmRequest;
import com.ezfinanz.auth.dto.ProfileUpdateRequest;
import com.ezfinanz.auth.dto.PhoneOtpSendRequest;
import com.ezfinanz.auth.dto.PhoneOtpVerifyRequest;
import com.ezfinanz.auth.dto.SignupEmailRequest;
import com.ezfinanz.auth.dto.UserResponse;
import com.ezfinanz.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for customer sign-up, login, OTP verification, and profile updates.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Sign-up and login with email or phone")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Creates an email/password account and sends a verification OTP. */
    @PostMapping("/signup/email")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign up with email and password; sends SMTP OTP")
    public MessageResponse signupEmail(@Valid @RequestBody SignupEmailRequest request) {
        return new MessageResponse(authService.signupEmail(request.email(), request.password(), request.fullName()).message());
    }

    /** Authenticates a verified email user and returns a JWT. */
    @PostMapping("/login/email")
    @Operation(summary = "Log in with email and password")
    public AuthResponse loginEmail(@Valid @RequestBody LoginEmailRequest request) {
        return authService.loginEmail(request.email(), request.password());
    }

    /** Signs in or registers a customer using a Google ID token. */
    @PostMapping("/login/google")
    @Operation(summary = "Log in or sign up with a Google ID token")
    public AuthResponse loginGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginGoogle(request.idToken());
    }

    /** Resends the email verification OTP for an unverified account. */
    @PostMapping("/otp/email/resend")
    @Operation(summary = "Resend email verification OTP")
    public MessageResponse resendEmailOtp(@Valid @RequestBody EmailOtpRequest request) {
        return new MessageResponse(authService.resendEmailOtp(request.email()).message());
    }

    /** Confirms the email OTP and returns a JWT. */
    @PostMapping("/otp/email/verify")
    @Operation(summary = "Verify email OTP and return JWT")
    public AuthResponse verifyEmailOtp(@Valid @RequestBody EmailOtpVerifyRequest request) {
        return authService.verifyEmailOtp(request.email(), request.otp());
    }

    /** Sends a phone OTP via Twilio; creates a customer if the number is new. */
    @PostMapping("/otp/phone/send")
    @Operation(summary = "Send phone OTP via Twilio; creates customer if new")
    public MessageResponse sendPhoneOtp(@Valid @RequestBody PhoneOtpSendRequest request) {
        return new MessageResponse(authService.sendPhoneOtp(request.phone()).message());
    }

    /** Confirms the phone OTP and returns a JWT. */
    @PostMapping("/otp/phone/verify")
    @Operation(summary = "Verify phone OTP and return JWT")
    public AuthResponse verifyPhoneOtp(@Valid @RequestBody PhoneOtpVerifyRequest request) {
        return authService.verifyPhoneOtp(request.phone(), request.otp());
    }

    /** Returns the authenticated user's profile and loan-application progress. */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Current authenticated user")
    public UserResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.me(userId);
    }

    /** Updates the logged-in customer's display name. */
    @PatchMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update the logged-in customer's display name")
    public UserResponse updateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile((Long) authentication.getPrincipal(), request.fullName());
    }

    /** Sends an email OTP so a logged-in customer can verify/add an email (step 2). */
    @PostMapping("/verification/email/send")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Send email OTP for the logged-in customer (step 2)")
    public MessageResponse sendEmailVerification(
            Authentication authentication,
            @Valid @RequestBody(required = false) OptionalEmailRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String email = request == null ? null : request.email();
        return new MessageResponse(authService.sendEmailVerification(userId, email).message());
    }

    /** Confirms the logged-in customer's email OTP. */
    @PostMapping("/verification/email/confirm")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Confirm email OTP for the logged-in customer")
    public UserResponse confirmEmailVerification(
            Authentication authentication,
            @Valid @RequestBody OtpConfirmRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.confirmEmailVerification(userId, request.otp());
    }

    /** Sends a phone OTP so a logged-in customer can verify/add a phone (step 2). */
    @PostMapping("/verification/phone/send")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Send phone OTP for the logged-in customer (step 2)")
    public MessageResponse sendPhoneVerification(
            Authentication authentication,
            @Valid @RequestBody(required = false) OptionalPhoneRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String phone = request == null ? null : request.phone();
        return new MessageResponse(authService.sendPhoneVerification(userId, phone).message());
    }

    /** Confirms the logged-in customer's phone OTP. */
    @PostMapping("/verification/phone/confirm")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Confirm phone OTP for the logged-in customer")
    public UserResponse confirmPhoneVerification(
            Authentication authentication,
            @Valid @RequestBody OtpConfirmRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.confirmPhoneVerification(userId, request.otp());
    }
}
