ALTER TABLE bookings ADD COLUMN stripe_payment_intent_id VARCHAR(255);
CREATE INDEX idx_bookings_stripe_payment_intent ON bookings(stripe_payment_intent_id);

-- Idempotent webhook handling: Stripe may redeliver the same event multiple
-- times (their documented retry behavior). We record processed event IDs
-- so a redelivered webhook is a no-op instead of double-confirming a booking.
CREATE TABLE processed_webhook_events (
                                          id BIGSERIAL PRIMARY KEY,
                                          provider VARCHAR(50) NOT NULL,          -- 'STRIPE', future: 'VNPAY'
                                          event_id VARCHAR(255) NOT NULL,
                                          event_type VARCHAR(100) NOT NULL,
                                          processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                          UNIQUE (provider, event_id)
);