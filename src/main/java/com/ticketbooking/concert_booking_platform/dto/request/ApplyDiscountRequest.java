package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ApplyDiscountRequest {

    @NotNull(message = "New price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "New price must be greater than 0")
    private BigDecimal newPrice;
}