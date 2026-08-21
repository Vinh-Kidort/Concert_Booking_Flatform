package com.ticketbooking.concert_booking_platform.controller.admin;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.CreateConcertRequest;
import com.ticketbooking.concert_booking_platform.dto.request.UpdateConcertRequest;
import com.ticketbooking.concert_booking_platform.dto.response.ConcertResponse;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.ConcertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/concerts")
@RequiredArgsConstructor
@Tag(name = "5. Operation Dashboard - Concerts", description = "Manage, publish, update, and cancel concert events and ticket categories (OPERATOR / ADMIN roles)")
@SecurityRequirement(name = "bearerAuth")
public class AdminConcertController {

    private final ConcertService concertService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new concert event", description = "Create a concert event along with initial ticket categories and quotas (UPCOMING state).")
    public ApiResponse<ConcertResponse> createConcert(@Valid @RequestBody CreateConcertRequest request) {
        Concert concert = concertService.createConcert(request, currentUserProvider.getCurrentUserId());
        return ApiResponse.success(ConcertResponse.summary(concert));
    }

    @PutMapping("/{concertId}")
    @Operation(summary = "Update concert details", description = "Update title, venue, event date, description or ticket category details.")
    public ApiResponse<ConcertResponse> updateConcert(
            @PathVariable Long concertId, @RequestBody UpdateConcertRequest request) {
        Concert concert = concertService.updateConcert(concertId, request);
        return ApiResponse.success(ConcertResponse.summary(concert));
    }

    @PatchMapping("/{concertId}/publish")
    @Operation(summary = "Publish concert (Open Flash Sale)", description = "Transitions concert status from UPCOMING to ON_SALE, making tickets available for customer reservation.")
    public ApiResponse<ConcertResponse> publishConcert(@PathVariable Long concertId) {
        Concert concert = concertService.publishConcert(concertId);
        return ApiResponse.success(ConcertResponse.summary(concert));
    }

    @PatchMapping("/{concertId}/cancel")
    @Operation(summary = "Cancel concert event", description = "Transitions concert status to CANCELLED.")
    public ApiResponse<ConcertResponse> cancelConcert(@PathVariable Long concertId) {
        Concert concert = concertService.cancelConcert(concertId);
        return ApiResponse.success(ConcertResponse.summary(concert));
    }
}