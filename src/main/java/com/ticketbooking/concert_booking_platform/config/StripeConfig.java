package com.ticketbooking.concert_booking_platform.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        // Note: intentionally not pinning Stripe.API_VERSION here — the
        // stripe-java SDK version in use may expose this differently across
        // releases. Instead, webhook handlers use deserializeUnsafe() as a
        // fallback in PaymentWebhookController to handle any API version
        // mismatch between the Stripe account and this SDK version. See
        // ARCHITECTURE.md for details on this known SDK behavior.
    }
}