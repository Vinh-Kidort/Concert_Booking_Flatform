package com.ticketbooking.concert_booking_platform.controller;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.response.WaitingRoomStatusResponse;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.WaitingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/waiting-room")
@RequiredArgsConstructor
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{concertId}/join")
    public ApiResponse<String> join(@PathVariable Long concertId) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(waitingRoomService.joinQueue(userId, concertId));
    }

    @GetMapping("/{concertId}/status")
    public ApiResponse<WaitingRoomStatusResponse> status(@PathVariable Long concertId) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(waitingRoomService.getStatus(userId, concertId));
    }

    @PostMapping("/{concertId}/admission-token")
    public ApiResponse<String> getAdmissionToken(@PathVariable Long concertId) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(waitingRoomService.issueAdmissionTokenIfEligible(userId, concertId));
    }
}