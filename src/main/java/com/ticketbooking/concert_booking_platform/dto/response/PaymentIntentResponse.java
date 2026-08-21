package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class PaymentIntentResponse {
    private String clientSecret;
    private Long bookingId;
    private String paymentIntentId;
}