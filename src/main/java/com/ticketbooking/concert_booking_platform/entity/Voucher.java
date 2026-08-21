package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Quota Enforcement & Concurrency Control:
 *
 * 1. Global Quota: Enforced via total_quantity / used_quantity using Pessimistic Locking
 *    (SELECT ... FOR UPDATE) via {@code VoucherRepository#findByCodeForUpdate}.
 *
 * 2. Per-User Usage Limit: Enforced to 1 active booking per user per voucher code via a 2-tier guard:
 *    - Soft pre-check: {@code BookingRepository#countByUserIdAndVoucherCodeAndStatusNotIn}.
 *    - Hard DB constraint: Partial Unique Index {@code idx_booking_voucher_per_user_active} on
 *      bookings(voucher_code, user_id) WHERE status NOT IN ('CANCELLED', 'EXPIRED', 'FAILED').
 *      This prevents TOCTOU race conditions at the database level without requiring heavy locks.
 */
@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "used_quantity", nullable = false)
    @Builder.Default
    private Integer usedQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Transient
    public boolean isValidNow(OffsetDateTime now) {
        boolean afterStart = startDate == null || !now.isBefore(startDate);
        boolean beforeEnd = endDate == null || !now.isAfter(endDate);
        boolean hasQuota = usedQuantity < totalQuantity;
        return afterStart && beforeEnd && hasQuota;
    }
}