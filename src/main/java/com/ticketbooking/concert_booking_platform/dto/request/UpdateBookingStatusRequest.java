package com.ticketbooking.concert_booking_platform.dto.request;

import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateBookingStatusRequest {

    @Schema(description = "Target status to transition the booking to", example = "CONFIRMED")
    @NotNull(message = "Status is required")
    private BookingStatus status;

    @Schema(description = "Reason or note for manual status update by Operator", example = "Payment verified manually via bank statement")
    private String note;
}