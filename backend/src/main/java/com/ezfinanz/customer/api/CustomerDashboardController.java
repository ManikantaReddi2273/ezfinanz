package com.ezfinanz.customer.api;

import com.ezfinanz.customer.dto.CustomerDashboardResponse;
import com.ezfinanz.customer.service.CustomerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer dashboard", description = "Aggregated application summary for the logged-in customer")
public class CustomerDashboardController {

    private final CustomerDashboardService customerDashboardService;

    public CustomerDashboardController(CustomerDashboardService customerDashboardService) {
        this.customerDashboardService = customerDashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Application summary, EMI quote, documents flags, and notices")
    public CustomerDashboardResponse dashboard(Authentication authentication) {
        return customerDashboardService.load((Long) authentication.getPrincipal());
    }
}
