package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CheckinResponse {
    private boolean success;
    private String message;
    private String recipientOrHolderName;
}