CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               aggregate_type VARCHAR(50) NOT NULL,   -- 'BOOKING'
                               aggregate_id BIGINT NOT NULL,          -- booking_id
                               event_type VARCHAR(100) NOT NULL,      -- 'BookingConfirmed'
                               payload JSONB NOT NULL,                -- toàn bộ dữ liệu cần cho consumer (email, tên concert, ...)
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED
                               retry_count INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_events_status_created ON outbox_events(status, created_at);