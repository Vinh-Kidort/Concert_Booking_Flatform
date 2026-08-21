package com.ticketbooking.concert_booking_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateBookingRequest {

    @Schema(description = "ID of the concert to book tickets for", example = "1")
    @NotNull
    private Long concertId;

    @Schema(description = "Client-generated unique key to prevent duplicate bookings from network retries", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank
    private String idempotencyKey;

    @NotEmpty
    @Valid
    private List<BookingItemRequest> items;
}