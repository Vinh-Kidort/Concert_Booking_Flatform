package com.ticketbooking.concert_booking_platform.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.ticketbooking.concert_booking_platform.dto.response.PaymentIntentResponse;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;

    /**
     * Creates a Stripe PaymentIntent for a PENDING booking's finalAmount.
     * The frontend uses the returned clientSecret with Stripe.js to collect
     * card details and confirm payment. We never touch card data directly.
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(Long userId, Long bookingId) throws StripeException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Can only create a payment intent for a PENDING booking, current status: " + booking.getStatus());
        }

        // Stripe expects the smallest currency unit (e.g. cents for USD).
        long amountInCents = booking.getFinalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .putMetadata("bookingId", String.valueOf(booking.getId()))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        booking.setStripePaymentIntentId(intent.getId());
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        bookingRepository.save(booking);

        log.info("Created Stripe PaymentIntent {} for booking {}", intent.getId(), bookingId);

        return PaymentIntentResponse.builder()
                .clientSecret(intent.getClientSecret())
                .bookingId(booking.getId())
                .paymentIntentId(intent.getId())
                .build();
    }
}