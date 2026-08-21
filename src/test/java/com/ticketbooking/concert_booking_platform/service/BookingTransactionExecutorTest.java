package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.BookingItemRequest;
import com.ticketbooking.concert_booking_platform.dto.request.CreateBookingRequest;
import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.exception.InsufficientTicketException;
import com.ticketbooking.concert_booking_platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingTransactionExecutorTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TicketCategoryRepository ticketCategoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConcertRepository concertRepository;

    @InjectMocks private BookingTransactionExecutor executor;

    private User user;
    private Concert concert;
    private TicketCategory vipCategory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("a@test.com").build();
        concert = Concert.builder().id(10L).title("Test Concert").status(ConcertStatus.ON_SALE).build();
        vipCategory = TicketCategory.builder()
                .id(100L).concert(concert).name("VIP")
                .price(new BigDecimal("500.00")).totalQuantity(10).availableQuantity(10)
                .build();
    }

    @Test
    void executeCreateBooking_sufficientStock_decrementsAvailableQuantityAndSavesBooking() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(vipCategory));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-2");
        request.setConcertId(10L);
        request.setItems(List.of(itemReq(100L, 3)));

        Booking result = executor.executeCreateBooking(1L, request);

        assertThat(vipCategory.getAvailableQuantity()).isEqualTo(7);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("1500.00");
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(ticketCategoryRepository).save(vipCategory);
    }

    @Test
    void executeCreateBooking_insufficientStock_throwsAndDoesNotSaveBooking() {
        vipCategory.setAvailableQuantity(2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(vipCategory));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-3");
        request.setConcertId(10L);
        request.setItems(List.of(itemReq(100L, 5)));

        assertThatThrownBy(() -> executor.executeCreateBooking(1L, request))
                .isInstanceOf(InsufficientTicketException.class);

        assertThat(vipCategory.getAvailableQuantity()).isEqualTo(2);
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void executeCreateBooking_ticketCategoryBelongsToDifferentConcert_throwsIllegalArgument() {
        Concert otherConcert = Concert.builder().id(999L).build();
        vipCategory.setConcert(otherConcert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(vipCategory));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-4");
        request.setConcertId(10L);
        request.setItems(List.of(itemReq(100L, 1)));

        assertThatThrownBy(() -> executor.executeCreateBooking(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to concert");
    }

    @Test
    void executeCreateBooking_multipleItems_locksTicketCategoriesInAscendingIdOrder() {
        TicketCategory catLow = TicketCategory.builder()
                .id(50L).concert(concert).name("Standard")
                .price(new BigDecimal("100.00")).totalQuantity(20).availableQuantity(20).build();
        TicketCategory catHigh = TicketCategory.builder()
                .id(200L).concert(concert).name("VVIP")
                .price(new BigDecimal("1000.00")).totalQuantity(5).availableQuantity(5).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(concertRepository.findById(10L)).thenReturn(Optional.of(concert));
        when(ticketCategoryRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(catLow));
        when(ticketCategoryRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(catHigh));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setIdempotencyKey("key-5");
        request.setConcertId(10L);
        request.setItems(List.of(itemReq(200L, 1), itemReq(50L, 1))); // descending on purpose

        executor.executeCreateBooking(1L, request);

        var inOrder = inOrder(ticketCategoryRepository);
        inOrder.verify(ticketCategoryRepository).findByIdForUpdate(50L);
        inOrder.verify(ticketCategoryRepository).findByIdForUpdate(200L);
    }

    private BookingItemRequest itemReq(Long categoryId, int qty) {
        BookingItemRequest r = new BookingItemRequest();
        r.setTicketCategoryId(categoryId);
        r.setQuantity(qty);
        return r;
    }
}