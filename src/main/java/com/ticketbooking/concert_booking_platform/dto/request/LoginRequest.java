package com.ticketbooking.concert_booking_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {


    @Schema(description = "Registered user email (e.g. alice@example.com, admin@ticketbooking.com)", example = "alice@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User password", example = "Password123!")
    @NotBlank(message = "Password is required")
    private String password;
}