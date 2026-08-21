package com.ticketbooking.concert_booking_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyVoucherRequest {

    @Schema(description = "Promotional voucher code (e.g., WELCOME10, FLASH5)", example = "WELCOME10")
    @NotBlank(message = "Voucher code is required")
    private String voucherCode;
}