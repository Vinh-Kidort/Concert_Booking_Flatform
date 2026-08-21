package com.ticketbooking.concert_booking_platform.dto.response;

import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TicketCategoryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer availableQuantity;
    private Integer totalQuantity;

    public static TicketCategoryResponse from(TicketCategory tc) {
        return TicketCategoryResponse.builder()
                .id(tc.getId())
                .name(tc.getName())
                .price(tc.getPrice())
                .availableQuantity(tc.getAvailableQuantity())
                .totalQuantity(tc.getTotalQuantity())
                .build();
    }
}