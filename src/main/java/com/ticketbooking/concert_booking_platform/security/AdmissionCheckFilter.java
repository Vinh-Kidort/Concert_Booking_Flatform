package com.ticketbooking.concert_booking_platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gates high-load endpoints (booking/seat reservation creation) behind a
 * waiting-room admission token. Distinct from JwtAuthenticationFilter,
 * which establishes WHO the user is — this filter establishes WHETHER they
 * are currently allowed to transact, independent of identity.
 */
@Component
@RequiredArgsConstructor
public class AdmissionCheckFilter extends OncePerRequestFilter {

    private final QueueTokenService queueTokenService;

    @Value("${app.waiting-room.enabled:false}")
    private boolean enabled;

    private static final String[] GATED_PATHS = {
            "/api/v1/bookings",
            "/api/v1/seatmap/propose"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        boolean isGated = "POST".equalsIgnoreCase(request.getMethod())
                && java.util.Arrays.stream(GATED_PATHS).anyMatch(p -> request.getRequestURI().equals(p));

        if (isGated) {
            String admissionToken = request.getHeader("X-Admission-Token");
            String concertIdHeader = request.getHeader("X-Concert-Id"); // client gửi kèm để verify đúng concert

            if (admissionToken == null || concertIdHeader == null
                    || !queueTokenService.isValidAdmissionToken(admissionToken, Long.valueOf(concertIdHeader))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false,\"error\":{\"code\":\"ADMISSION_REQUIRED\",\"message\":\"Please join the waiting room and wait to be admitted before booking.\"}}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}