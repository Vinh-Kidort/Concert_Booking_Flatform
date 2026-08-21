package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.CreateBookingRequest;
import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.InvalidBookingStatusTransitionException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TicketCategoryRepository ticketCategoryRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingTransactionExecutor bookingTransactionExecutor;

    @InjectMocks private BookingService bookingService;

    private TicketCategory vipCategory;

    @BeforeEach
    void setUp() {
        Concert concert = Concert.builder().id(10L).title("Test Concert").build();
        vipCategory = TicketCategory.builder()
                .id(100L).concert(concert).name("VIP")
                .price(new BigDecimal("500.00")).totalQuantity(10).availableQuantity(10)
                .build();
    }

    @Test
    void createBooking_idempotentReplay_returnsExistingBookingWithoutCallingExecutor() {
        Booking existing = Booking.builder().id(999L).idempotencyKey("key-1").build();
        when(bookingRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-1");

        Booking result = bookingService.createBooking(1L, request);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(bookingTransactionExecutor);
    }

    @Test
    void createBooking_newKey_delegatesToExecutor() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-2");
        Booking created = Booking.builder().id(1L).idempotencyKey("key-2").build();

        when(bookingRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(bookingTransactionExecutor.executeCreateBooking(1L, request)).thenReturn(created);

        Booking result = bookingService.createBooking(1L, request);

        assertThat(result).isSameAs(created);
        verify(bookingTransactionExecutor).executeCreateBooking(1L, request);
    }

    @Test
    void createBooking_executorThrowsDataIntegrityViolation_fallsBackToQueryingExistingBooking() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-3");
        Booking winnerBooking = Booking.builder().id(5L).idempotencyKey("key-3").build();

        when(bookingRepository.findByIdempotencyKey("key-3"))
                .thenReturn(Optional.empty())      // first check: not found yet
                .thenReturn(Optional.of(winnerBooking)); // fallback query after race: found
        when(bookingTransactionExecutor.executeCreateBooking(1L, request))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        Booking result = bookingService.createBooking(1L, request);

        assertThat(result).isSameAs(winnerBooking);
    }

    @Test
    void createBooking_executorThrowsDataIntegrityViolation_andFallbackFindsNothing_rethrowsOriginalException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-4");
        DataIntegrityViolationException original = new DataIntegrityViolationException("unexpected constraint violation");

        when(bookingRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        when(bookingTransactionExecutor.executeCreateBooking(1L, request)).thenThrow(original);

        assertThatThrownBy(() -> bookingService.createBooking(1L, request))
                .isSameAs(original);
    }

    @Test
    void updateStatus_invalidTransition_throwsAndDoesNotReleaseInventory() {
        Booking booking = Booking.builder().id(1L).status(BookingStatus.EXPIRED).items(List.of()).build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.updateStatus(1L, BookingStatus.CONFIRMED, 99L, "trying to revive"))
                .isInstanceOf(InvalidBookingStatusTransitionException.class);

        verify(ticketCategoryRepository, never()).findByIdForUpdate(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateStatus_pendingToCancelled_releasesInventoryBackToAvailableQuantity() {
        vipCategory.setAvailableQuantity(5);
        BookingItem item = BookingItem.builder()
                .ticketCategory(vipCategory).quantity(3).unitPrice(vipCategory.getPrice()).build();
        Booking booking = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).items(List.of(item)).voucherCode(null).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(ticketCategoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(vipCategory));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.updateStatus(1L, BookingStatus.CANCELLED, 99L, "customer requested");

        assertThat(vipCategory.getAvailableQuantity()).isEqualTo(8);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void updateStatus_awaitingPaymentToConfirmed_doesNotReleaseInventory() {
        vipCategory.setAvailableQuantity(5);
        BookingItem item = BookingItem.builder()
                .ticketCategory(vipCategory).quantity(3).unitPrice(vipCategory.getPrice()).build();
        Booking booking = Booking.builder()
                .id(1L).status(BookingStatus.AWAITING_PAYMENT).items(List.of(item)).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.updateStatus(1L, BookingStatus.CONFIRMED, null, null);

        verify(ticketCategoryRepository, never()).findByIdForUpdate(any());
        assertThat(vipCategory.getAvailableQuantity()).isEqualTo(5);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void updateStatus_bookingNotFound_throwsResourceNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateStatus(1L, BookingStatus.CANCELLED, 1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}