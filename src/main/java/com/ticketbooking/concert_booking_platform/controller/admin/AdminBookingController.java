package com.ticketbooking.concert_booking_platform.controller.admin;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.UpdateBookingStatusRequest;
import com.ticketbooking.concert_booking_platform.dto.response.BookingResponse;
import com.ticketbooking.concert_booking_platform.dto.response.BookingStatsResponse;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "6. Operation Dashboard - Bookings", description = "Monitor, inspect, and manually update booking statuses (OPERATOR / ADMIN roles)")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "List all bookings (Monitoring)", description = "Retrieve a paginated list of all system bookings, optionally filtered by status (PENDING, CONFIRMED, EXPIRED, CANCELLED, FAILED).")
    public ApiResponse<Page<BookingResponse>> listBookings(
            @Parameter(description = "Filter by booking status (optional)")
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Booking> result = (status != null)
                ? bookingRepository.findByStatus(status, PageRequest.of(page, size))
                : bookingRepository.findAll(PageRequest.of(page, size));
        return ApiResponse.success(result.map(BookingResponse::from));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking detail by ID", description = "Operators can inspect any booking detail regardless of owner.")
    public ApiResponse<BookingResponse> getBooking(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PatchMapping("/{bookingId}/status")
    @Operation(summary = "Manually update booking status", description = "Manual override by Operator/Admin — e.g. mark FAILED for suspicious bookings or force CONFIRMED. Validates allowed state transitions.")
    public ApiResponse<BookingResponse> updateStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        Long operatorId = currentUserProvider.getCurrentUserId();
        Booking booking = bookingService.updateStatus(
                bookingId, request.getStatus(), operatorId, request.getNote());
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get booking statistics (Dashboard overview)", description = "Aggregated count of total bookings and count per status for operations monitoring.")
    public ApiResponse<BookingStatsResponse> getStats() {
        BookingStatsResponse stats = BookingStatsResponse.builder()
                .totalBookings(bookingRepository.count())
                .pendingCount(bookingRepository.countByStatus(BookingStatus.PENDING))
                .confirmedCount(bookingRepository.countByStatus(BookingStatus.CONFIRMED))
                .expiredCount(bookingRepository.countByStatus(BookingStatus.EXPIRED))
                .cancelledCount(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .failedCount(bookingRepository.countByStatus(BookingStatus.FAILED))
                .build();
        return ApiResponse.success(stats);
    }
}