package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateVoucherRequest {

    @NotBlank(message = "Voucher code is required")
    private String code;

    @NotNull(message = "Discount amount is required")
    @Min(value = 1, message = "Discount amount must be greater than 0")
    private BigDecimal discountAmount;

    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;

    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
}