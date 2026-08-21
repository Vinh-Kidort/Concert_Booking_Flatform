package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByConcertId(Long concertId);

    /**
     * Locks the ticket_category row for the duration of the current
     * transaction (SELECT ... FOR UPDATE). Must be called inside a
     * @Transactional method in BookingService BEFORE reading
     * availableQuantity, so that concurrent booking requests for the same
     * category are serialized and cannot oversell.
     *
     * Do NOT use this for read-only availability checks on the customer
     * browse endpoints — only when actually about to reserve/release stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tc from TicketCategory tc where tc.id = :id")
    Optional<TicketCategory> findByIdForUpdate(@Param("id") Long id);
}