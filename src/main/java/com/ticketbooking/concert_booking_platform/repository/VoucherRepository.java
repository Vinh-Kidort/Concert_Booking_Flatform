package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    /**
     * Locks the voucher row (SELECT ... FOR UPDATE) before checking/
     * decrementing quota, same rationale as
     * TicketCategoryRepository#findByIdForUpdate. Must be called inside the
     * same @Transactional boundary as the ticket-category lock in
     * BookingService, so voucher quota and ticket stock are consistent.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.code = :code")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);
}