package com.ticketbooking.concert_booking_platform.dto.response;

import com.ticketbooking.concert_booking_platform.entity.PriceChangeAudit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Builder
public class PriceChangeAuditResponse {
    private Long id;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private OffsetDateTime changedAt;

    public static PriceChangeAuditResponse from(PriceChangeAudit audit) {
        return PriceChangeAuditResponse.builder()
                .id(audit.getId())
                .oldPrice(audit.getOldPrice())
                .newPrice(audit.getNewPrice())
                .changedAt(audit.getChangedAt())
                .build();
    }
}