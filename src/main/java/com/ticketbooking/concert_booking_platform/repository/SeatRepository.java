package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRowIdAndStatusOrderBySeatNumberAsc(Long rowId, String status);

    /**
     * Locks a specific set of seats, in an order determined by the caller
     * (must already be sorted ascending by id before calling, same
     * deadlock-avoidance principle as ticket_categories locking).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id ASC")
    List<Seat> findByIdInForUpdate(@Param("ids") List<Long> ids);
}