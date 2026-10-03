package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.response.CheckinResponse;
import com.ticketbooking.concert_booking_platform.entity.CompTicket;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.CompTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinService {

    private final CompTicketRepository compTicketRepository;

    @Transactional
    public CheckinResponse checkInByQrToken(String qrToken) {
        CompTicket ticket = compTicketRepository.findByQrCodeToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid QR code"));

        if ("REVOKED".equals(ticket.getStatus())) {
            return CheckinResponse.builder()
                    .success(false)
                    .message("This ticket has been revoked and is no longer valid")
                    .recipientOrHolderName(ticket.getRecipientName())
                    .build();
        }

        int updated = compTicketRepository.tryCheckIn(ticket.getId());
        if (updated == 0) {
            // Race lost, or already checked in by an earlier scan
            log.warn("Duplicate check-in attempt for comp ticket {}", ticket.getId());
            return CheckinResponse.builder()
                    .success(false)
                    .message("This ticket has already been checked in")
                    .recipientOrHolderName(ticket.getRecipientName())
                    .build();
        }

        log.info("Checked in comp ticket {} for {}", ticket.getId(), ticket.getRecipientName());
        return CheckinResponse.builder()
                .success(true)
                .message("Checked in successfully")
                .recipientOrHolderName(ticket.getRecipientName())
                .build();
    }
}