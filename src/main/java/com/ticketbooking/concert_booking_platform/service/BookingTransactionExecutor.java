package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.BookingItemRequest;
import com.ticketbooking.concert_booking_platform.dto.request.CreateBookingRequest;
import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.InsufficientTicketException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingTransactionExecutor {

    private static final int HOLD_MINUTES = 10;

    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;

    @Transactional
    public Booking executeCreateBooking(Long userId, CreateBookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found: " + request.getConcertId()));

        List<BookingItemRequest> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(BookingItemRequest::getTicketCategoryId))
                .toList();

        Booking booking = Booking.builder()
                .user(user)
                .concert(concert)
                .idempotencyKey(request.getIdempotencyKey())
                .status(BookingStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusMinutes(HOLD_MINUTES))
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BookingItemRequest itemReq : sortedItems) {
            TicketCategory category = ticketCategoryRepository
                    .findByIdForUpdate(itemReq.getTicketCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ticket category not found: " + itemReq.getTicketCategoryId()));

            if (!category.getConcert().getId().equals(concert.getId())) {
                throw new IllegalArgumentException(
                        "Ticket category " + category.getId() + " does not belong to concert " + concert.getId());
            }
            if (category.getAvailableQuantity() < itemReq.getQuantity()) {
                throw new InsufficientTicketException(
                        "Not enough tickets available for category: " + category.getName());
            }

            category.setAvailableQuantity(category.getAvailableQuantity() - itemReq.getQuantity());
            ticketCategoryRepository.save(category);

            BigDecimal lineTotal = category.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            BookingItem item = BookingItem.builder()
                    .ticketCategory(category)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(category.getPrice())
                    .build();
            booking.addItem(item);
        }

        booking.setTotalAmount(totalAmount);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(totalAmount);

        // Ép ghi SQL ngay để ném DataIntegrityViolationException tại đây nếu trùng key
        return bookingRepository.saveAndFlush(booking);
    }
}