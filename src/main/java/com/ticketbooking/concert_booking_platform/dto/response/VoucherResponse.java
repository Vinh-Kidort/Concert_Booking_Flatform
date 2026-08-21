package com.ticketbooking.concert_booking_platform.dto.response;

import com.ticketbooking.concert_booking_platform.entity.Voucher;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal minOrderValue;
    private Integer totalQuantity;
    private Integer usedQuantity;
    private Integer remainingQuantity;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    public static VoucherResponse from(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .discountAmount(v.getDiscountAmount())
                .minOrderValue(v.getMinOrderValue())
                .totalQuantity(v.getTotalQuantity())
                .usedQuantity(v.getUsedQuantity())
                .remainingQuantity(Math.max(0, v.getTotalQuantity() - v.getUsedQuantity()))
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .build();
    }
}