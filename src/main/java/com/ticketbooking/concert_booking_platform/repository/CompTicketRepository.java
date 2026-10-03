package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.CompTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompTicketRepository extends JpaRepository<CompTicket, Long> {

    long countByTicketCategoryIdAndStatusNot(Long ticketCategoryId, String status);

    Page<CompTicket> findByTicketCategoryId(Long ticketCategoryId, Pageable pageable);

    Optional<CompTicket> findByQrCodeToken(String qrCodeToken);

    /**
     * Atomic check-in: only succeeds if the ticket is still ISSUED. If two
     * scanners hit the same QR code at the same instant (e.g. a shared/
     * photographed ticket), exactly one UPDATE affects a row — the other
     * gets 0 rows affected and must be treated as "already used", the same
     * atomic-update pattern used elsewhere in this codebase for inventory
     * decrements instead of a separate SELECT-then-UPDATE that would race.
     */
    @Modifying
    @Query("UPDATE CompTicket c SET c.status = 'CHECKED_IN', c.checkedInAt = CURRENT_TIMESTAMP " +
            "WHERE c.id = :id AND c.status = 'ISSUED'")
    int tryCheckIn(@Param("id") Long id);
}