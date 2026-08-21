package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LoginResponse {
    private String accessToken;
    private Long userId;
    private String role;
}