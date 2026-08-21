
-- 1. Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', -- CUSTOMER, OPERATOR, ADMIN
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Concerts
CREATE TABLE concerts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255) NOT NULL,
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPCOMING', -- UPCOMING, ON_SALE, ENDED, CANCELLED
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Ticket Categories (VIP, Standard...)
CREATE TABLE ticket_categories (
    id BIGSERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL REFERENCES concerts(id),
    name VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_available_quantity_range
    CHECK (available_quantity >= 0 AND available_quantity <= total_quantity)
);

-- 4. Bookings
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    concert_id BIGINT NOT NULL REFERENCES concerts(id),
    idempotency_key VARCHAR(255) UNIQUE, -- Chống duplicate request
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, AWAITING_PAYMENT, CONFIRMED, EXPIRED, CANCELLED, FAILED
    total_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) DEFAULT 0,
    final_amount DECIMAL(12, 2) NOT NULL,
    voucher_code VARCHAR(50),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- Thời gian hết hạn giữ vé (VD +10 phút)
    updated_by BIGINT REFERENCES users(id),
    status_note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Booking Items
CREATE TABLE booking_items (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    ticket_category_id BIGINT NOT NULL REFERENCES ticket_categories(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12, 2) NOT NULL
);

-- 6. Vouchers
CREATE TABLE vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_amount DECIMAL(12, 2) NOT NULL,
    min_order_value DECIMAL(12, 2) DEFAULT 0,
    total_quantity INT NOT NULL,
    used_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_used_quantity_range
    CHECK (used_quantity >= 0 AND used_quantity <= total_quantity)
);


-- Indexes tối ưu truy vấn
CREATE INDEX idx_concerts_status ON concerts(status);
CREATE INDEX idx_ticket_categories_concert ON ticket_categories(concert_id);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_status_expires ON bookings(status, expires_at);
CREATE INDEX idx_bookings_concert ON bookings(concert_id);
CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);
CREATE INDEX idx_booking_items_ticket_category ON booking_items(ticket_category_id);
CREATE INDEX idx_vouchers_code ON vouchers(code);