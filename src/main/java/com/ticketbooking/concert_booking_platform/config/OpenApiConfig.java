package com.ticketbooking.concert_booking_platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI concertBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Concert Ticket Booking Platform API")
                        .description("""
                                Backend API for a concert ticket booking platform, covering both
                                customer-facing booking flows and internal operation workflows.

                                Auth: call POST /api/v1/auth/login to obtain a JWT, then click
                                "Authorize" above and paste the token (no "Bearer " prefix needed,
                                it's added automatically).

                                Seeded test accounts (see V2__seed_data.sql), all with password
                                "Password123!":
                                - admin@concert.com (ADMIN)
                                - operator@concert.com (OPERATOR)
                                - customer1@concert.com (CUSTOMER)
                                """)
                        .version("v1.0")
                        .contact(new Contact().name("Backend Engineer").email("you@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}