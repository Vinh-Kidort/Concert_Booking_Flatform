package com.ticketbooking.concert_booking_platform.controller.organizer;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.RejectConcertRequest;
import com.ticketbooking.concert_booking_platform.dto.response.ConcertResponse;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.ConcertService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizer/concerts")
@RequiredArgsConstructor
@Tag(name = "8. Organizer - Concerts", description = "Organizer endpoints to manage, approve, or reject concerts")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerConcertController {

    private final ConcertService concertService;
    private final CurrentUserProvider currentUserProvider;

    /** Lists only concerts owned by the current organizer. */
    @GetMapping
    public ApiResponse<Page<ConcertResponse>> myConcerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        Page<ConcertResponse> result = concertService
                .listByOrganizationMember(organizerId, PageRequest.of(page, size))
                .map(ConcertResponse::summary);
        return ApiResponse.success(result);
    }

    @PatchMapping("/{concertId}/approve")
    public ApiResponse<ConcertResponse> approveConcert(@PathVariable Long concertId) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        Concert concert = concertService.approveConcert(organizerId, concertId);
        return ApiResponse.success(ConcertResponse.summary(concert));
    }

    @PatchMapping("/{concertId}/reject")
    public ApiResponse<ConcertResponse> rejectConcert(
            @PathVariable Long concertId,
            @Valid @RequestBody RejectConcertRequest request) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        Concert concert = concertService.rejectConcert(organizerId, concertId, request.getRejectionReason());
        return ApiResponse.success(ConcertResponse.summary(concert));
    }
}