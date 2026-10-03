package com.ticketbooking.concert_booking_platform.controller;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.ConfirmSeatProposalRequest;
import com.ticketbooking.concert_booking_platform.dto.request.ProposeSeatsRequest;
import com.ticketbooking.concert_booking_platform.dto.response.BookingResponse;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.entity.SeatProposal;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.SeatmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seatmap")
@RequiredArgsConstructor
public class SeatmapController {

    private final SeatmapService seatmapService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/propose")
    public ApiResponse<SeatProposal> propose(@Valid @RequestBody ProposeSeatsRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(seatmapService.proposeSeats(userId, request.getZoneId(), request.getQuantity()));
    }

    @PostMapping("/confirm/{proposalId}")
    public ApiResponse<BookingResponse> confirm(
            @PathVariable Long proposalId,
            @Valid @RequestBody ConfirmSeatProposalRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Booking booking = seatmapService.confirmProposal(userId, proposalId, request.getIdempotencyKey());
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PostMapping("/reject/{proposalId}")
    public ApiResponse<Void> reject(@PathVariable Long proposalId) {
        Long userId = currentUserProvider.getCurrentUserId();
        seatmapService.rejectProposal(userId, proposalId);
        return ApiResponse.success(null);
    }
}