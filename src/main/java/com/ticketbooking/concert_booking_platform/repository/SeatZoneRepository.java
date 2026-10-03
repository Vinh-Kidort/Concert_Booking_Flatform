package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.SeatZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatZoneRepository extends JpaRepository<SeatZone, Long> {
    List<SeatZone> findByConcertId(Long concertId);
}