package com.ezfinanz.auth.service;

import com.ezfinanz.auth.dto.AuthResponse;
import com.ezfinanz.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final AuthService authService;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String frontendUrl;

    public GoogleOAuthService(
            AuthService authService,
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret}") String clientSecret,
            @Value("${app.google.redirect-uri}") String redirectUri,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl
    ) {
        this.authService = authService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
        this.restClient = RestClient.create();
    }

    public String buildAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "online")
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUriString();
    }

    public String completeOAuthCallback(String code) {
        String idToken = exchangeCodeForIdToken(code);
        AuthResponse authResponse = authService.loginGoogle(idToken);
        return frontendUrl + "/auth/google/callback?token="
                + URLEncoder.encode(authResponse.token(), StandardCharsets.UTF_8);
    }

    public String buildFailureRedirect(String message) {
        return frontendUrl + "/auth/google/callback?error="
                + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private String exchangeCodeForIdToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            JsonNode response = restClient.post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.hasNonNull("id_token")) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "GOOGLE_TOKEN_EXCHANGE_FAILED",
                        "Google did not return a sign-in token."
                );
            }
            return response.get("id_token").asText();
        } catch (RestClientResponseException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "GOOGLE_TOKEN_EXCHANGE_FAILED",
                    "Google sign-in could not be completed. Check the OAuth redirect URI in Google Cloud."
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "GOOGLE_TOKEN_EXCHANGE_FAILED",
                    "Google sign-in could not be completed."
            );
        }
    }
}
