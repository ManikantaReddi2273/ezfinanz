package com.ezfinanz.admin.api;

import com.ezfinanz.admin.dto.AdminApplicationDetail;
import com.ezfinanz.admin.dto.AdminApplicationSummary;
import com.ezfinanz.admin.dto.ApplicationReviewRequest;
import com.ezfinanz.admin.dto.SelfieRejectRequest;
import com.ezfinanz.admin.service.AdminApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Application review and disbursement")
public class AdminApplicationController {

    private final AdminApplicationService adminApplicationService;

    public AdminApplicationController(AdminApplicationService adminApplicationService) {
        this.adminApplicationService = adminApplicationService;
    }

    @GetMapping
    @Operation(summary = "List all customer applications")
    public List<AdminApplicationSummary> list() {
        return adminApplicationService.list();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "View full application journey")
    public AdminApplicationDetail get(@PathVariable Long userId) {
        return adminApplicationService.get(userId);
    }

    @PostMapping("/{userId}/selfie/approve")
    @Operation(summary = "Approve the live selfie")
    public AdminApplicationDetail approve(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestBody(required = false) ApplicationReviewRequest request
    ) {
        String message = request == null ? null : request.getMessage();
        return adminApplicationService.approveSelfie((Long) authentication.getPrincipal(), userId, message);
    }

    @PostMapping("/{userId}/selfie/reject")
    @Operation(summary = "Reject the live selfie")
    public AdminApplicationDetail reject(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestBody(required = false) SelfieRejectRequest request
    ) {
        String reason = request == null ? null : request.getReason();
        return adminApplicationService.rejectSelfie((Long) authentication.getPrincipal(), userId, reason);
    }

    @PostMapping("/{userId}/disburse")
    @Operation(summary = "Confirm loan disbursement")
    public AdminApplicationDetail disburse(Authentication authentication, @PathVariable Long userId) {
        return adminApplicationService.disburse((Long) authentication.getPrincipal(), userId);
    }

    @GetMapping("/{userId}/kyc-document")
    @Operation(summary = "View KYC ID document")
    public ResponseEntity<Resource> kycDocument(@PathVariable Long userId) {
        Resource resource = adminApplicationService.kycDocument(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/{userId}/selfie")
    @Operation(summary = "View submitted selfie")
    public ResponseEntity<Resource> selfie(@PathVariable Long userId) {
        Resource resource = adminApplicationService.selfiePhoto(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
