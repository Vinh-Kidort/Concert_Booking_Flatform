package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.CompTicket;
import com.ticketbooking.concert_booking_platform.repository.CompTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckinServiceTest {

    @Mock private CompTicketRepository compTicketRepository;
    @InjectMocks private CheckinService checkinService;

    @Test
    void checkInByQrToken_validIssuedTicket_succeeds() {
        CompTicket ticket = CompTicket.builder().id(1L).recipientName("Jane").status("ISSUED").build();
        when(compTicketRepository.findByQrCodeToken("token-1")).thenReturn(Optional.of(ticket));
        when(compTicketRepository.tryCheckIn(1L)).thenReturn(1); // 1 row affected

        var result = checkinService.checkInByQrToken("token-1");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void checkInByQrToken_alreadyCheckedIn_racedUpdate_returnsFailure() {
        CompTicket ticket = CompTicket.builder().id(1L).recipientName("Jane").status("ISSUED").build();
        when(compTicketRepository.findByQrCodeToken("token-1")).thenReturn(Optional.of(ticket));
        when(compTicketRepository.tryCheckIn(1L)).thenReturn(0); // race lost, 0 rows affected

        var result = checkinService.checkInByQrToken("token-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("already been checked in");
    }

    @Test
    void checkInByQrToken_revokedTicket_returnsFailureWithoutAttemptingUpdate() {
        CompTicket ticket = CompTicket.builder().id(1L).recipientName("Jane").status("REVOKED").build();
        when(compTicketRepository.findByQrCodeToken("token-1")).thenReturn(Optional.of(ticket));

        var result = checkinService.checkInByQrToken("token-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("revoked");
    }
}