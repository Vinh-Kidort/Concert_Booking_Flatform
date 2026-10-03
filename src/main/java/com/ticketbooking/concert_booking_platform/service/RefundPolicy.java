package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.exception.RefundNotAllowedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Refund eligibility and amount, based purely on days-until-event. Kept
 * separate from RefundService so the tiered policy can be unit tested
 * without mocking any repository/Stripe dependency.
 *
 *   <= 3 days before event  : not eligible for cancellation refund
 *                              (ticket transfer is the only option)
 *    4 - 7 days              : 50% refund
 *    8 - 14 days              : 70% refund (30% cancellation fee)
 *   >= 15 days               : 100% refund
 */
@Component
public class RefundPolicy {

    public BigDecimal calculateRefundAmount(BigDecimal finalAmount, OffsetDateTime eventDate, OffsetDateTime now) {
        long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate.toLocalDate());

        BigDecimal refundRate = resolveRefundRate(daysUntilEvent);
        return finalAmount.multiply(refundRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveRefundRate(long daysUntilEvent) {
        if (daysUntilEvent <= 3) {
            throw new RefundNotAllowedException(
                    "Cannot cancel within 3 days of the event. Consider transferring the ticket instead.");
        } else if (daysUntilEvent <= 7) {
            return new BigDecimal("0.50");
        } else if (daysUntilEvent <= 14) {
            return new BigDecimal("0.70");
        } else {
            return BigDecimal.ONE;
        }
    }
}