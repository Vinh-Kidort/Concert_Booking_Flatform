package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RejectConcertRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}