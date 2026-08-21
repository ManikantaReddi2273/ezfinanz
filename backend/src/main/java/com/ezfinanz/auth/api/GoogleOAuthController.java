package com.ezfinanz.auth.api;

import com.ezfinanz.auth.service.GoogleOAuthService;
import com.ezfinanz.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Browser redirect flow for Google OAuth: start sign-in and handle the callback
 * by redirecting back to the frontend with a JWT or error.
 */
@RestController
@RequestMapping("/api/auth/google")
@Tag(name = "Google auth", description = "Google OAuth redirect login")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    public GoogleOAuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    /** Redirects the browser to Google's authorization page. */
    @GetMapping("/start")
    @Operation(summary = "Redirect the browser to Google sign-in")
    public void start(HttpServletResponse response) throws IOException {
        response.sendRedirect(googleOAuthService.buildAuthorizationUrl());
    }

    /** Exchanges the Google auth code and redirects to the frontend with token or error. */
    @GetMapping("/callback")
    @Operation(summary = "Google OAuth callback — exchanges code and redirects to the frontend")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        if (error != null && !error.isBlank()) {
            response.sendRedirect(googleOAuthService.buildFailureRedirect("Google sign-in was cancelled."));
            return;
        }
        if (code == null || code.isBlank()) {
            response.sendRedirect(googleOAuthService.buildFailureRedirect("Google did not return an authorization code."));
            return;
        }

        try {
            response.sendRedirect(googleOAuthService.completeOAuthCallback(code));
        } catch (ApiException ex) {
            response.sendRedirect(googleOAuthService.buildFailureRedirect(ex.getMessage()));
        }
    }
}
