package com.ticketbooking.concert_booking_platform.dto.response;

import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.entity.BookingItem;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class BookingResponse {
    private Long id;
    private Long concertId;
    private String concertTitle;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String voucherCode;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private List<BookingItemResponse> items;

    public static BookingResponse from(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .concertId(b.getConcert().getId())
                .concertTitle(b.getConcert().getTitle())
                .status(b.getStatus())
                .totalAmount(b.getTotalAmount())
                .discountAmount(b.getDiscountAmount())
                .finalAmount(b.getFinalAmount())
                .voucherCode(b.getVoucherCode())
                .expiresAt(b.getExpiresAt())
                .createdAt(b.getCreatedAt())
                .items(b.getItems().stream().map(BookingItemResponse::from).toList())
                .build();
    }

    @Getter
    @Builder
    public static class BookingItemResponse {
        private Long ticketCategoryId;
        private String categoryName;
        private Integer quantity;
        private BigDecimal unitPrice;

        public static BookingItemResponse from(BookingItem item) {
            return BookingItemResponse.builder()
                    .ticketCategoryId(item.getTicketCategory().getId())
                    .categoryName(item.getTicketCategory().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
        }
    }
}