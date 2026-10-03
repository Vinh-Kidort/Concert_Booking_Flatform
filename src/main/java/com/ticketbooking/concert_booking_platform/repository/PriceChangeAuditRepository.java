package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.PriceChangeAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceChangeAuditRepository extends JpaRepository<PriceChangeAudit, Long> {
    Page<PriceChangeAudit> findByTicketCategoryIdOrderByChangedAtDesc(Long ticketCategoryId, Pageable pageable);
}