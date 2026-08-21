package com.ticketbooking.concert_booking_platform.controller;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.ApplyVoucherRequest;
import com.ticketbooking.concert_booking_platform.dto.response.BookingResponse;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "4. Customer Vouchers", description = "Apply promotional vouchers to existing bookings")
@SecurityRequirement(name = "bearerAuth")
public class VoucherController {

    private final VoucherService voucherService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{bookingId}/apply-voucher")
    @Operation(summary = "Apply voucher to booking", description = "Applies a promotional code to a PENDING booking. Enforces global quota, expiry, minimum order value, and per-user limits.")
    public ApiResponse<BookingResponse> applyVoucher(
            @PathVariable Long bookingId,
            @Valid @RequestBody ApplyVoucherRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        BookingResponse response = voucherService.applyVoucherToBooking(userId, bookingId, request);
        return ApiResponse.success(response);
    }
}