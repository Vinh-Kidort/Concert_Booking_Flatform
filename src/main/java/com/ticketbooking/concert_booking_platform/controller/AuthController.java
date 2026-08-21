package com.ticketbooking.concert_booking_platform.controller;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.LoginRequest;
import com.ticketbooking.concert_booking_platform.dto.response.LoginResponse;
import com.ticketbooking.concert_booking_platform.entity.User;
import com.ticketbooking.concert_booking_platform.repository.UserRepository;
import com.ticketbooking.concert_booking_platform.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "Login API for all user roles (Customer, Operator, Admin)")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate using email and password to receive a Bearer JWT access token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials or request format")
    })
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
        return ApiResponse.success(response);
    }
}