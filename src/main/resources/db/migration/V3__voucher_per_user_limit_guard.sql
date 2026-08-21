-- V3__voucher_per_user_limit_guard.sql
CREATE UNIQUE INDEX idx_booking_voucher_per_user_active
    ON bookings (voucher_code, user_id)
    WHERE voucher_code IS NOT NULL
  AND status NOT IN ('CANCELLED', 'EXPIRED', 'FAILED');