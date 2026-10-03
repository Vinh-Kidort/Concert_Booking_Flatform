package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProposeSeatsRequest {
    @NotNull
    private Long zoneId;
    @Min(1)
    private Integer quantity;
}