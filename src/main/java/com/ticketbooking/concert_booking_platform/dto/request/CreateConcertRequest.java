package com.ticketbooking.concert_booking_platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Setter
public class CreateConcertRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String venue;

    @NotNull
    @Future(message = "Event date must be in the future")
    private OffsetDateTime eventDate;

    @NotEmpty
    @Valid
    private List<TicketCategoryRequest> ticketCategories;

    @Getter @Setter
    public static class TicketCategoryRequest {
        @NotBlank
        private String name;

        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        private java.math.BigDecimal price;

        @NotNull @Min(1)
        private Integer totalQuantity;
    }
}