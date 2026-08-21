package com.ticketbooking.concert_booking_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingItemRequest {

    @Schema(description = "ID of the ticket category (e.g., VIP, Standard)", example = "1")
    @NotNull
    private Long ticketCategoryId;

    @Schema(description = "Number of tickets to reserve", example = "2")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}