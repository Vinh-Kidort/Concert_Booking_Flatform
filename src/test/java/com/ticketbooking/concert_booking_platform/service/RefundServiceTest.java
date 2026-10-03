package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.entity.User;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.RefundNotAllowedException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private RefundPolicy refundPolicy;
    @InjectMocks private RefundService refundService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).build();
        booking = Booking.builder()
                .id(10L).user(user).status(BookingStatus.CONFIRMED)
                .stripePaymentIntentId("pi_test123")
                .build();
    }

    @Test
    void requestRefund_notConfirmed_throwsRefundNotAllowed() {
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> refundService.requestRefund(1L, 10L))
                .isInstanceOf(RefundNotAllowedException.class);
    }

    @Test
    void requestRefund_wrongUser_throwsResourceNotFound() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> refundService.requestRefund(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requestRefund_alreadyRequested_throwsRefundNotAllowed() {
        booking.setRefundRequestedAt(OffsetDateTime.now());
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> refundService.requestRefund(1L, 10L))
                .isInstanceOf(RefundNotAllowedException.class);
    }
}