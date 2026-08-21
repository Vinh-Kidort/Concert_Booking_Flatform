package com.ticketbooking.concert_booking_platform.controller;

import com.stripe.exception.StripeException;
import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.response.PaymentIntentResponse;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{bookingId}/create-payment-intent")
    public ApiResponse<PaymentIntentResponse> createPaymentIntent(@PathVariable Long bookingId) throws StripeException {
        Long userId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(paymentService.createPaymentIntent(userId, bookingId));
    }
}