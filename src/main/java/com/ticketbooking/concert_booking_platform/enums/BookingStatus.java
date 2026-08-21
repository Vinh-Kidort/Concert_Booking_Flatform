package com.ticketbooking.concert_booking_platform.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Booking lifecycle states.
 *
 * PENDING --> AWAITING_PAYMENT --> CONFIRMED
 *    |               |
 *    +--> EXPIRED <--+
 *    +--> CANCELLED <--+
 * AWAITING_PAYMENT --> FAILED
 * CONFIRMED --> CANCELLED (manual, by Operator only — e.g. concert cancelled;
 *               no automatic refund handling, see ASSUMPTIONS.md)
 *
 * See ARCHITECTURE.md for the full state transition diagram.
 */
public enum BookingStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    EXPIRED,
    CANCELLED,
    FAILED;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PENDING,
                EnumSet.of(AWAITING_PAYMENT, EXPIRED, CANCELLED));
        ALLOWED_TRANSITIONS.put(AWAITING_PAYMENT,
                EnumSet.of(CONFIRMED, FAILED, EXPIRED, CANCELLED));
        ALLOWED_TRANSITIONS.put(CONFIRMED,
                EnumSet.of(CANCELLED));
        ALLOWED_TRANSITIONS.put(EXPIRED,
                EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED,
                EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(FAILED,
                EnumSet.noneOf(BookingStatus.class));
    }

    /**
     * Whether moving from this state to {@code target} is a valid transition.
     * Use this in BookingService / OperationService before persisting any
     * manual or automatic status change, to reject invalid jumps such as
     * EXPIRED -> CONFIRMED.
     */
    public boolean canTransitionTo(BookingStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /** States that still hold inventory (available_quantity not yet released). */
    public static final Set<BookingStatus> ACTIVE_HOLD_STATUSES =
            EnumSet.of(PENDING, AWAITING_PAYMENT);

    /** Terminal states where inventory/voucher quota must be released. */
    public static final Set<BookingStatus> RELEASE_INVENTORY_STATUSES =
            EnumSet.of(EXPIRED, CANCELLED, FAILED);
}