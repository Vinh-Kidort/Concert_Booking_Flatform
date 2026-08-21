package com.ticketbooking.concert_booking_platform.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, AWAITING_PAYMENT, true",
            "PENDING, EXPIRED, true",
            "PENDING, CANCELLED, true",
            "PENDING, CONFIRMED, false",       // must go through AWAITING_PAYMENT
            "AWAITING_PAYMENT, CONFIRMED, true",
            "AWAITING_PAYMENT, FAILED, true",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, PENDING, false",       // no going backwards
            "EXPIRED, PENDING, false",         // terminal state
            "CANCELLED, CONFIRMED, false",     // terminal state
            "FAILED, PENDING, false"           // terminal state
    })
    void canTransitionTo_followsDefinedStateMachine(String from, String to, boolean expected) {
        BookingStatus fromStatus = BookingStatus.valueOf(from);
        BookingStatus toStatus = BookingStatus.valueOf(to);

        assertThat(fromStatus.canTransitionTo(toStatus)).isEqualTo(expected);
    }

    @Test
    void terminalStates_haveNoOutgoingTransitions() {
        assertThat(BookingStatus.EXPIRED.canTransitionTo(BookingStatus.EXPIRED)).isFalse();
        for (BookingStatus target : BookingStatus.values()) {
            assertThat(BookingStatus.EXPIRED.canTransitionTo(target)).isFalse();
            assertThat(BookingStatus.CANCELLED.canTransitionTo(target)).isFalse();
            assertThat(BookingStatus.FAILED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void activeHoldStatuses_onlyContainsPendingAndAwaitingPayment() {
        assertThat(BookingStatus.ACTIVE_HOLD_STATUSES)
                .containsExactlyInAnyOrder(BookingStatus.PENDING, BookingStatus.AWAITING_PAYMENT);
    }
}