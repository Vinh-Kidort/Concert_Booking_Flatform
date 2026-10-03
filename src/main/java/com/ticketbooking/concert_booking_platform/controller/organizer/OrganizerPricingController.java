package com.ticketbooking.concert_booking_platform.controller.organizer;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.ApplyDiscountRequest;
import com.ticketbooking.concert_booking_platform.dto.response.PriceChangeAuditResponse;
import com.ticketbooking.concert_booking_platform.dto.response.TicketCategoryResponse;
import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.PricingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizer/ticket-categories")
@RequiredArgsConstructor
@Tag(name = "9. Organizer - Pricing", description = "Organizer endpoints to apply dynamic discounts and view price audit history")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerPricingController {

    private final PricingService pricingService;
    private final CurrentUserProvider currentUserProvider;

    @PatchMapping("/{ticketCategoryId}/discount")
    public ApiResponse<TicketCategoryResponse> applyDiscount(
            @PathVariable Long ticketCategoryId,
            @Valid @RequestBody ApplyDiscountRequest request) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        TicketCategory category = pricingService.applyDiscount(organizerId, ticketCategoryId, request.getNewPrice());
        return ApiResponse.success(TicketCategoryResponse.from(category));
    }

    @GetMapping("/{ticketCategoryId}/price-history")
    public ApiResponse<Page<PriceChangeAuditResponse>> priceHistory(
            @PathVariable Long ticketCategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        Page<PriceChangeAuditResponse> result = pricingService
                .getPriceHistory(organizerId, ticketCategoryId, PageRequest.of(page, size))
                .map(PriceChangeAuditResponse::from);
        return ApiResponse.success(result);
    }
}