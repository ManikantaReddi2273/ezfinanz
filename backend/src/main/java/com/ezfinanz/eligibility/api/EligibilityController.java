package com.ezfinanz.eligibility.api;

import com.ezfinanz.eligibility.dto.EligibilityRequest;
import com.ezfinanz.eligibility.dto.EligibilityResponse;
import com.ezfinanz.eligibility.service.EligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eligibility")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Eligibility", description = "Loan eligibility check")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @GetMapping
    @Operation(summary = "Get the latest eligibility assessment")
    public EligibilityResponse get(Authentication authentication) {
        return eligibilityService.get((Long) authentication.getPrincipal());
    }

    @PostMapping
    @Operation(summary = "Submit financial details and run eligibility")
    public EligibilityResponse assess(Authentication authentication, @Valid @RequestBody EligibilityRequest request) {
        return eligibilityService.assess((Long) authentication.getPrincipal(), request);
    }
}
