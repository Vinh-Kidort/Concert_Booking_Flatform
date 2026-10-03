package com.ticketbooking.concert_booking_platform.enums;
import com.stripe.model.tax.Registration;
import org.flywaydb.core.api.output.DashboardRenderer;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Approval workflow for a concert, independent of its sale lifecycle
 * (ConcertStatus: UPCOMING/ON_SALE/ENDED/CANCELLED). A concert can be
 * APPROVED but still UPCOMING (not yet published for sale) — these are two
 * separate axes on purpose, not one combined enum.
 *
 * DRAFT --> PENDING_REVIEW --> APPROVED
 *                |                 |
 *                v                 |
 *            REJECTED              |
 *                |                 |
 *                +--> (Operator edits, resubmits) --> PENDING_REVIEW
 *
 * Only an APPROVED concert may be transitioned to ConcertStatus.ON_SALE by
 * an Operator (enforced in ConcertService#publishConcert).
 */

public enum ConcertApprovalStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED;

    private static final Map<ConcertApprovalStatus, Set<ConcertApprovalStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ConcertApprovalStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DRAFT, EnumSet.of(PENDING_REVIEW));
        ALLOWED_TRANSITIONS.put(PENDING_REVIEW, EnumSet.of(APPROVED, REJECTED));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.noneOf(ConcertApprovalStatus.class));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.of(PENDING_REVIEW));
    }

    public boolean canTransitionTo(ConcertApprovalStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}



