package com.ticketbooking.concert_booking_platform.controller.admin;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.response.CheckinResponse;
import com.ticketbooking.concert_booking_platform.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Check-in scanning is an OPERATOR/STAFF action performed at the venue
 * gate, hence living under /admin rather than /organizer. See
 * ASSUMPTIONS.md — a dedicated STAFF role for gate scanners (narrower than
 * full OPERATOR access) is a known future improvement, not implemented in
 * this scope.
 */
@RestController
@RequestMapping("/api/v1/admin/checkin")
@RequiredArgsConstructor
public class AdminCheckinController {

    private final CheckinService checkinService;

    @PostMapping("/scan")
    public ApiResponse<CheckinResponse> scan(@RequestParam String qrToken) {
        return ApiResponse.success(checkinService.checkInByQrToken(qrToken));
    }
}