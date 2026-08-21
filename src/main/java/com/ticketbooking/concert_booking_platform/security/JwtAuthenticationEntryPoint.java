package com.ticketbooking.concert_booking_platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs in the security filter chain (before DispatcherServlet), so
 * GlobalExceptionHandler cannot catch missing/invalid-token cases —
 * this produces the same ApiResponse-shaped JSON body directly.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("UNAUTHORIZED", "Missing or invalid authentication token"));
    }
}