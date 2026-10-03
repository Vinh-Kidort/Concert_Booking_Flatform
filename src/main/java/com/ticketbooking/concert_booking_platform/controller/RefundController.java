package com.ticketbooking.concert_booking_platform.controller;

import com.stripe.exception.StripeException;
import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.response.RefundResponse;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{bookingId}/refund")
    public ApiResponse<RefundResponse> requestRefund(@PathVariable Long bookingId) throws StripeException {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(refundService.requestRefund(userId, bookingId));
    }
}