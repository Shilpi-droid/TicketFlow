package com.ticketflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata + a Bearer-token scheme for the Swagger UI at /swagger-ui.html.
 *
 * With the security scheme declared, the UI shows an "Authorize" button: paste a
 * JWT from POST /auth/login there once and every protected call is sent with it.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI ticketflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketFlow API")
                        .version("v1")
                        .description("""
                                Seat-level event ticketing. Hold seats for 8 minutes, check out with an
                                Idempotency-Key, confirm via a payment webhook. No seat is ever sold twice.

                                Flow: POST /auth/register -> POST /events/{id}/holds -> POST /checkout
                                -> POST /webhooks/payment."""))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
