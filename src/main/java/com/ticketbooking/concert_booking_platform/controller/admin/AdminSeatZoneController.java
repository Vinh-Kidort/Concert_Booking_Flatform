package com.ticketbooking.concert_booking_platform.controller.admin;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.CreateSeatZoneRequest;
import com.ticketbooking.concert_booking_platform.entity.SeatZone;
import com.ticketbooking.concert_booking_platform.service.SeatmapAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/concerts/{concertId}/seat-zones")
@RequiredArgsConstructor
public class AdminSeatZoneController {

    private final SeatmapAdminService seatmapAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeatZone> createSeatZone(
            @PathVariable Long concertId,
            @Valid @RequestBody CreateSeatZoneRequest request) {
        return ApiResponse.success(seatmapAdminService.createSeatZone(concertId, request));
    }

    @GetMapping
    public ApiResponse<List<SeatZone>> listSeatZones(@PathVariable Long concertId) {
        return ApiResponse.success(seatmapAdminService.getZonesByConcert(concertId));
    }
}