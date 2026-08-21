package com.ezfinanz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI metadata for the EZFINANZ API, including Bearer JWT security.
 */
@Configuration
public class OpenApiConfig {

    /** Builds the OpenAPI document shown in Swagger UI. */
    @Bean
    public OpenAPI ezfinanzOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EZFINANZ API")
                        .version("v1")
                        .description("Personal loan application API. Phase 1: sign-up and login."))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
