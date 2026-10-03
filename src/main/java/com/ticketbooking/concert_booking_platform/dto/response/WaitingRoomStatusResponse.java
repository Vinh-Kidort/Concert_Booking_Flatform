package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class WaitingRoomStatusResponse {
    private boolean admitted;
    private int position; // 1-indexed, -1 nếu chưa join
    private int totalInQueue;
}