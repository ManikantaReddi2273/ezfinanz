package com.ezfinanz.kyc.api;

import com.ezfinanz.kyc.dto.KycResponse;
import com.ezfinanz.kyc.dto.KycSaveRequest;
import com.ezfinanz.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST API for customer KYC (identity, address, and optional ID document upload).
 */
@RestController
@RequestMapping("/api/kyc")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "KYC", description = "Know Your Customer details")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    /** Returns the logged-in customer's submitted KYC profile. */
    @GetMapping
    @Operation(summary = "Get submitted KYC for the logged-in customer")
    public KycResponse get(Authentication authentication) {
        return kycService.get((Long) authentication.getPrincipal());
    }

    /** Creates or updates KYC details, optionally attaching an ID document file. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create or update KYC, optionally with an ID document")
    public KycResponse save(
            Authentication authentication,
            @Valid @ModelAttribute KycSaveRequest request,
            @RequestPart(value = "document", required = false) MultipartFile document
    ) {
        return kycService.save(
                (Long) authentication.getPrincipal(),
                request.getFullName(),
                request.getDateOfBirth(),
                request.getGender(),
                request.getAddressLine(),
                request.getCity(),
                request.getState(),
                request.getPincode(),
                request.getIdType(),
                request.getIdNumber(),
                document
        );
    }

    /** Streams the customer's uploaded ID document for inline viewing/download. */
    @GetMapping("/document")
    @Operation(summary = "Download the uploaded ID document")
    public ResponseEntity<Resource> document(Authentication authentication) {
        Resource resource = kycService.document((Long) authentication.getPrincipal());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
