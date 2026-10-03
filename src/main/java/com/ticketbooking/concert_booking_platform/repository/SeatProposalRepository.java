package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.SeatProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatProposalRepository extends JpaRepository<SeatProposal, Long> {
    Optional<SeatProposal> findByIdAndUserId(Long id, Long userId);
    List<SeatProposal> findByStatusAndExpiresAtBefore(String status, OffsetDateTime now);
}