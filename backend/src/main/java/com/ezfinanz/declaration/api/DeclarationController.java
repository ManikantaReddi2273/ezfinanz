package com.ezfinanz.declaration.api;

import com.ezfinanz.declaration.dto.DeclarationRequest;
import com.ezfinanz.declaration.dto.DeclarationResponse;
import com.ezfinanz.declaration.service.DeclarationService;
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
@RequestMapping("/api/declaration")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Declaration", description = "Loan application declaration")
public class DeclarationController {

    private final DeclarationService declarationService;

    public DeclarationController(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @GetMapping
    @Operation(summary = "Get declaration status")
    public DeclarationResponse get(Authentication authentication) {
        return declarationService.get((Long) authentication.getPrincipal());
    }

    @PostMapping
    @Operation(summary = "Accept the loan declaration")
    public DeclarationResponse accept(Authentication authentication, @Valid @RequestBody DeclarationRequest request) {
        return declarationService.accept((Long) authentication.getPrincipal(), request);
    }
}
