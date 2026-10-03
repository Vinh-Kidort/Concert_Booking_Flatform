package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.SeatRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRowRepository extends JpaRepository<SeatRow, Long> {
    List<SeatRow> findByZoneIdOrderByRowPriorityAsc(Long zoneId);
}