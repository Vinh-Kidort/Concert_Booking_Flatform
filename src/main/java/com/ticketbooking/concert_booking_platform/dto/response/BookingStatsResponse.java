package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BookingStatsResponse {
    private long totalBookings;
    private long pendingCount;
    private long confirmedCount;
    private long expiredCount;
    private long cancelledCount;
    private long failedCount;
}