package com.ticketbooking.concert_booking_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmSeatProposalRequest {

    @Schema(description = "Unique client-generated key (UUID) to prevent duplicate booking creation",
            example = "c1b2a3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}