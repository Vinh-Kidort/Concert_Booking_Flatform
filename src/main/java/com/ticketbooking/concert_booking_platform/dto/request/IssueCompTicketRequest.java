package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class IssueCompTicketRequest {

    @NotBlank
    private String recipientName;

    private String recipientEmail;

    @NotBlank
    @Pattern(regexp = "GUEST|SPONSOR|VIP|STAFF", message = "recipientType must be one of GUEST, SPONSOR, VIP, STAFF")
    private String recipientType;
}