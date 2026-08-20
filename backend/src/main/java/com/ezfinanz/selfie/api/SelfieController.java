package com.ezfinanz.selfie.api;

import com.ezfinanz.selfie.dto.SelfieResponse;
import com.ezfinanz.selfie.service.SelfieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/selfie")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Selfie", description = "Live selfie for admin review")
public class SelfieController {

    private final SelfieService selfieService;

    public SelfieController(SelfieService selfieService) {
        this.selfieService = selfieService;
    }

    @GetMapping
    @Operation(summary = "Get selfie review status")
    public SelfieResponse get(Authentication authentication) {
        return selfieService.get((Long) authentication.getPrincipal());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Confirm a live selfie draft (before sending the application)")
    public SelfieResponse confirmDraft(
            Authentication authentication,
            @RequestParam("photo") MultipartFile photo
    ) {
        return selfieService.confirmDraft((Long) authentication.getPrincipal(), photo);
    }

    @PostMapping("/send-application")
    @Operation(summary = "Send the completed application for admin review")
    public SelfieResponse sendApplication(Authentication authentication) {
        return selfieService.sendApplication((Long) authentication.getPrincipal());
    }

    @GetMapping("/photo")
    @Operation(summary = "View the submitted selfie")
    public ResponseEntity<Resource> photo(Authentication authentication) {
        Resource resource = selfieService.photo((Long) authentication.getPrincipal());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
