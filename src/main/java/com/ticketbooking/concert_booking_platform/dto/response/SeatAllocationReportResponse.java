package com.ticketbooking.concert_booking_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class SeatAllocationReportResponse {
    private Long ticketCategoryId;
    private String categoryName;
    private int totalQuantity;
    private int soldQuantity;
    private int compIssuedQuantity;
    private int compRemainingQuantity;
    private int publicAvailableQuantity;
    private int unallocatedQuantity;
}