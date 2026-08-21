package com.ezfinanz.loan.api;

import com.ezfinanz.loan.dto.EmiQuoteResponse;
import com.ezfinanz.loan.dto.EmiSaveRequest;
import com.ezfinanz.loan.service.EmiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST API for quoting and confirming personal-loan EMI amount and tenure.
 */
@RestController
@RequestMapping("/api/emi")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "EMI", description = "Loan amount, tenure, and repayment terms")
public class EmiController {

    private final EmiService emiService;

    public EmiController(EmiService emiService) {
        this.emiService = emiService;
    }

    /** Live quote of EMI, fees, net disbursement, and IRR for an amount and tenure. */
    @GetMapping("/quote")
    @Operation(summary = "Live EMI, charges, disbursement, and IRR for an amount and tenure")
    public EmiQuoteResponse quote(
            Authentication authentication,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) Integer tenureMonths
    ) {
        return emiService.quote((Long) authentication.getPrincipal(), amount, tenureMonths);
    }

    /** Returns the customer's saved EMI selection. */
    @GetMapping
    @Operation(summary = "Get saved EMI terms")
    public EmiQuoteResponse get(Authentication authentication) {
        return emiService.get((Long) authentication.getPrincipal());
    }

    /** Persists the chosen principal and tenure as confirmed loan terms. */
    @PostMapping
    @Operation(summary = "Confirm loan amount and tenure")
    public EmiQuoteResponse save(Authentication authentication, @Valid @RequestBody EmiSaveRequest request) {
        return emiService.save((Long) authentication.getPrincipal(), request);
    }
}
