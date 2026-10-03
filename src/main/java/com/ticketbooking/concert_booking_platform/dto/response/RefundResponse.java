package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class RefundResponse {
    private Long bookingId;
    private BigDecimal refundAmount;
    private String stripeRefundId;
    private String status;
}