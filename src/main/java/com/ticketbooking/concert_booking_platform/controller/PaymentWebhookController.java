package com.ticketbooking.concert_booking_platform.controller;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.ticketbooking.concert_booking_platform.config.StripeConfig;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.entity.ProcessedWebhookEvent;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import com.ticketbooking.concert_booking_platform.repository.ProcessedWebhookEventRepository;
import com.ticketbooking.concert_booking_platform.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final StripeConfig stripeConfig;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;

    /**
     * Receives Stripe webhook events. NOT behind JWT auth (Stripe can't send
     * a bearer token) — instead trust is established purely via HMAC
     * signature verification against the raw request body.
     *
     * See SecurityConfig: this path is explicitly permitAll()'d.
     */
    @PostMapping("/api/v1/payments/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        // Idempotency guard: Stripe redelivers events on timeout/non-2xx
        // response. Recording (provider, event_id) as UNIQUE means a
        // redelivered event is safely ignored instead of double-confirming
        // a booking or double-crediting inventory.
        try {
            processedWebhookEventRepository.save(ProcessedWebhookEvent.builder()
                    .provider("STRIPE")
                    .eventId(event.getId())
                    .eventType(event.getType())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Stripe event {} already processed, skipping (redelivery)", event.getId());
            return ResponseEntity.ok("Already processed");
        }

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentFailed(event);
                default -> log.info("Unhandled Stripe event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook event {} (type={})", event.getId(), event.getType(), e);
            // Still return 200 to avoid Stripe retry storms for errors that are
            // likely to be permanent (bad data) rather than transient. If we later
            // see transient errors here, this is the place to add selective 5xx.
            return ResponseEntity.ok("Processed with internal error, see logs");
        }

        return ResponseEntity.ok("OK");
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = extractPaymentIntent(event);
        if (intent == null) {
            log.error("Could not deserialize PaymentIntent from event {} — payload may be malformed", event.getId());
            return;
        }

        Optional<Booking> bookingOpt = bookingRepository.findByStripePaymentIntentId(intent.getId());
        if (bookingOpt.isEmpty()) {
            log.warn("Received payment_intent.succeeded for unknown PaymentIntent: {}. Ignoring.", intent.getId());
            return;
        }

        Booking booking = bookingOpt.get();
        bookingService.updateStatus(booking.getId(), BookingStatus.CONFIRMED, null,
                "Confirmed via Stripe webhook, PaymentIntent=" + intent.getId());
        log.info("Booking {} confirmed via Stripe PaymentIntent {}", booking.getId(), intent.getId());
    }

    /**
     * Stripe's typed deserializer (getObject()) silently returns empty when the
     * event's API version (from the Stripe account/CLI) is newer than the
     * version the stripe-java SDK was compiled against — it does NOT throw, it
     * just returns Optional.empty(). Falling back to deserializeUnsafe() forces
     * deserialization against the raw JSON regardless of version mismatch,
     * which Stripe's own docs recommend for exactly this situation.
     * See: https://github.com/stripe/stripe-java#deserializing-webhook-events
     */
    private PaymentIntent extractPaymentIntent(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return (PaymentIntent) deserializer.getObject().get();
        }
        log.warn("Typed deserialization failed for event {} (API version mismatch), falling back to deserializeUnsafe()",
                event.getId());
        try {
            return (PaymentIntent) deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            log.error("deserializeUnsafe() also failed for event {}", event.getId(), e);
            return null;
        }
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent intent = extractPaymentIntent(event);
        if (intent == null) {
            log.error("Could not deserialize PaymentIntent from event {}", event.getId());
            return;
        }

        bookingRepository.findByStripePaymentIntentId(intent.getId()).ifPresentOrElse(
                booking -> bookingService.updateStatus(booking.getId(), BookingStatus.FAILED, null,
                        "Payment failed via Stripe webhook, PaymentIntent=" + intent.getId()),
                () -> log.warn("Received payment_intent.payment_failed for unknown PaymentIntent: {}. Ignoring.",
                        intent.getId())
        );
    }
}