package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.exception.RefundNotAllowedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundPolicyTest {

    private final RefundPolicy policy = new RefundPolicy();
    private final BigDecimal amount = new BigDecimal("1000.00");

    @ParameterizedTest
    @CsvSource({
            "0",   // ngày diễn ra
            "1",
            "3"    // đúng biên - không được hủy
    })
    void within3Days_throwsRefundNotAllowed(long daysUntilEvent) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime eventDate = now.plusDays(daysUntilEvent);

        assertThatThrownBy(() -> policy.calculateRefundAmount(amount, eventDate, now))
                .isInstanceOf(RefundNotAllowedException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "4, 500.00",   // biên dưới của khoảng 50%
            "7, 500.00"    // biên trên của khoảng 50%
    })
    void fourToSevenDays_refunds50Percent(long daysUntilEvent, String expectedRefund) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime eventDate = now.plusDays(daysUntilEvent);

        BigDecimal result = policy.calculateRefundAmount(amount, eventDate, now);

        assertThat(result).isEqualByComparingTo(expectedRefund);
    }

    @ParameterizedTest
    @CsvSource({
            "8, 700.00",   // biên dưới của khoảng 70%
            "14, 700.00"   // biên trên của khoảng 70%
    })
    void eightToFourteenDays_refunds70Percent(long daysUntilEvent, String expectedRefund) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime eventDate = now.plusDays(daysUntilEvent);

        BigDecimal result = policy.calculateRefundAmount(amount, eventDate, now);

        assertThat(result).isEqualByComparingTo(expectedRefund);
    }

    @Test
    void fifteenDaysOrMore_refunds100Percent() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime eventDate = now.plusDays(15);

        BigDecimal result = policy.calculateRefundAmount(amount, eventDate, now);

        assertThat(result).isEqualByComparingTo("1000.00");
    }

    @Test
    void farInFuture_stillRefunds100Percent() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime eventDate = now.plusDays(90);

        BigDecimal result = policy.calculateRefundAmount(amount, eventDate, now);

        assertThat(result).isEqualByComparingTo("1000.00");
    }
}