package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class CreateSeatZoneRequest {

    @NotBlank
    private String name;

    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotEmpty
    @Valid
    private List<RowDefinition> rows;

    @Getter @Setter
    public static class RowDefinition {
        @NotBlank
        private String rowLabel;

        @NotNull @Min(1)
        private Integer rowPriority;

        @NotNull @Min(1) @Max(200) // giới hạn hợp lý, tránh tạo hàng nghìn ghế vô ý do nhập sai
        private Integer seatCount;
    }
}