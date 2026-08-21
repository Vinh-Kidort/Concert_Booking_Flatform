package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    Page<Booking> findByConcertIdAndStatus(Long concertId, BookingStatus status, Pageable pageable);

    Optional<Booking> findByStripePaymentIntentId(String stripePaymentIntentId);

    Slice<Booking> findByStatusInAndExpiresAtBefore(
            Collection<BookingStatus> statuses, OffsetDateTime now, Pageable pageable);

    long countByUserIdAndVoucherCodeAndStatusNotIn(
            Long userId,
            String voucherCode,
            Collection<BookingStatus> excludedStatuses);

    long countByStatus(BookingStatus status);

    /**
     * Used by the scheduled expiry job — finds all bookings still holding
     * inventory (PENDING / AWAITING_PAYMENT) whose hold has passed
     * expiresAt, so they can be transitioned to EXPIRED and their stock/
     * voucher quota released.
     */
    Iterable<Booking> findByStatusInAndExpiresAtBefore(
            Collection<BookingStatus> statuses, OffsetDateTime now);
}