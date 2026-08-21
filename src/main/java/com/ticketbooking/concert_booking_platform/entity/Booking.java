package com.ticketbooking.concert_booking_platform.entity;

import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Idempotency note: idempotency_key is UNIQUE at the DB level. Service layer
 * must (1) look up by idempotency_key first and return the existing booking
 * if found, and (2) catch DataIntegrityViolationException on insert (race
 * between two near-simultaneous requests with the same key) and re-query
 * instead of letting the error propagate. See BookingService.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Operator/Admin who last changed the status manually. Null for
     *  system-driven transitions (e.g. expiry job, mock payment callback). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "status_note", length = 500)
    private String statusNote;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    public void addItem(BookingItem item) {
        item.setBooking(this);
        items.add(item);
    }
}