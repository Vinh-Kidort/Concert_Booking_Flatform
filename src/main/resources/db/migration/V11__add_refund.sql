ALTER TABLE bookings ADD COLUMN refund_amount DECIMAL(12,2);
ALTER TABLE bookings ADD COLUMN refund_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN stripe_refund_id VARCHAR(255);