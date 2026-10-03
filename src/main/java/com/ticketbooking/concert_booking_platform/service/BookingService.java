package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.CreateBookingRequest;
import com.ticketbooking.concert_booking_platform.entity.*;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.InvalidBookingStatusTransitionException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final BookingTransactionExecutor bookingTransactionExecutor; // Inject Executor mới
    private final OutboxEventPublisher outboxEventPublisher;
    private final SeatRepository seatRepository;

    /**
     * Public entry point — deliberately NOT @Transactional.
     * Calls external bean bookingTransactionExecutor so Spring AOP proxy correctly handles
     * transaction rollback on DataIntegrityViolationException before falling back.
     */
    public Booking createBooking(Long userId, CreateBookingRequest request) {
        Optional<Booking> existing = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay for key={}, returning existing booking id={}",
                    request.getIdempotencyKey(), existing.get().getId());
            return existing.get();
        }

        try {
            return bookingTransactionExecutor.executeCreateBooking(userId, request);
        } catch (DataIntegrityViolationException e) {
            log.warn("Idempotency key race detected for key={}, retrying lookup in a fresh transaction",
                    request.getIdempotencyKey());
            // Lời gọi này diễn ra trong một Transaction MỚI và SẠCH hoàn toàn
            return bookingRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public Booking updateStatus(Long bookingId, BookingStatus newStatus, Long operatorId, String note) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        BookingStatus oldStatus = booking.getStatus();
        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new InvalidBookingStatusTransitionException(
                    String.format("Cannot transition booking %d from %s to %s", bookingId, oldStatus, newStatus));
        }

        boolean wasHolding = BookingStatus.ACTIVE_HOLD_STATUSES.contains(oldStatus);
        boolean releaseNeeded = wasHolding && newStatus != BookingStatus.CONFIRMED;

        if (releaseNeeded) {
            releaseInventory(booking);
        }

        booking.setStatus(newStatus);
        booking.setUpdatedBy(operatorId != null ? userRepository.getReferenceById(operatorId) : null);
        booking.setStatusNote(note);
        booking.setUpdatedAt(OffsetDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Write the outbox event in the SAME transaction as the status change —
        // this is what makes the pattern atomic. If anything in this method
        // rolls back, the event row rolls back too. See OutboxEventPublisher.
        if (newStatus == BookingStatus.CONFIRMED) {
            booking.getItems().stream()
                    .filter(item -> item.getSeat() != null)
                    .forEach(item -> {
                        Seat seat = seatRepository.findByIdInForUpdate(List.of(item.getSeat().getId()))
                                .stream().findFirst().orElseThrow();
                        seat.setStatus("BOOKED");
                        seatRepository.save(seat);
                    });
            outboxEventPublisher.publish(
                    "BOOKING",
                    booking.getId(),
                    "BookingConfirmed",
                    Map.of(
                            "bookingId", booking.getId(),
                            "userEmail", booking.getUser().getEmail(),
                            "userFullName", booking.getUser().getFullName(),
                            "concertTitle", booking.getConcert().getTitle(),
                            "finalAmount", booking.getFinalAmount().toString()
                    )
            );
        }

        return saved;
    }

    private void releaseInventory(Booking booking) {
        List<BookingItem> sortedItems = booking.getItems().stream()
                .sorted(Comparator.comparing(item ->
                        item.getTicketCategory() != null
                                ? item.getTicketCategory().getId()
                                : item.getSeat().getId()))
                .toList();

        for (BookingItem item : sortedItems) {
            if (item.getTicketCategory() != null) {
                TicketCategory category = ticketCategoryRepository
                        .findByIdForUpdate(item.getTicketCategory().getId())
                        .orElseThrow();
                category.setAvailableQuantity(category.getAvailableQuantity() + item.getQuantity());
                ticketCategoryRepository.save(category);
            } else if (item.getSeat() != null) {
                Seat seat = seatRepository.findByIdInForUpdate(List.of(item.getSeat().getId()))
                        .stream().findFirst().orElseThrow();
                seat.setStatus("AVAILABLE");
                seat.setBookingItemId(null);
                seatRepository.save(seat);
            }
        }

        if (booking.getVoucherCode() != null) {
            voucherRepository.findByCodeForUpdate(booking.getVoucherCode()).ifPresent(voucher -> {
                voucher.setUsedQuantity(Math.max(0, voucher.getUsedQuantity() - 1));
                voucherRepository.save(voucher);
            });
        }
    }
}