package com.ticketbooking.concert_booking_platform.dto.response;

import com.ticketbooking.concert_booking_platform.entity.CompTicket;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class CompTicketResponse {
    private Long id;
    private String recipientName;
    private String recipientEmail;
    private String recipientType;
    private String qrCodeToken;
    private String status;
    private OffsetDateTime issuedAt;
    private OffsetDateTime checkedInAt;

    public static CompTicketResponse from(CompTicket ticket) {
        return CompTicketResponse.builder()
                .id(ticket.getId())
                .recipientName(ticket.getRecipientName())
                .recipientEmail(ticket.getRecipientEmail())
                .recipientType(ticket.getRecipientType())
                .qrCodeToken(ticket.getQrCodeToken())
                .status(ticket.getStatus())
                .issuedAt(ticket.getIssuedAt())
                .checkedInAt(ticket.getCheckedInAt())
                .build();
    }
}