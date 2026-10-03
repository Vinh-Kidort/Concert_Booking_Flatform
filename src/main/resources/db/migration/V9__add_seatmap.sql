CREATE TABLE seat_zones (
                            id BIGSERIAL PRIMARY KEY,
                            concert_id BIGINT NOT NULL REFERENCES concerts(id),
                            name VARCHAR(100) NOT NULL,
                            price DECIMAL(12,2) NOT NULL,
                            original_price DECIMAL(12,2) NOT NULL,
                            total_quantity INT NOT NULL,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seat_rows (
                           id BIGSERIAL PRIMARY KEY,
                           zone_id BIGINT NOT NULL REFERENCES seat_zones(id),
                           row_label VARCHAR(10) NOT NULL,
                           row_priority INT NOT NULL, -- số nhỏ hơn = gần sân khấu hơn = ưu tiên trước
                           seat_count INT NOT NULL,
                           UNIQUE (zone_id, row_label)
);

CREATE TABLE seats (
                       id BIGSERIAL PRIMARY KEY,
                       row_id BIGINT NOT NULL REFERENCES seat_rows(id),
                       seat_number INT NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, PROPOSED, HELD, BOOKED
                       booking_item_id BIGINT REFERENCES booking_items(id),
                       UNIQUE (row_id, seat_number),
                       CONSTRAINT chk_seat_status CHECK (status IN ('AVAILABLE', 'PROPOSED', 'HELD', 'BOOKED'))
);
CREATE INDEX idx_seats_row_status ON seats(row_id, status);

CREATE TABLE seat_proposals (
                                id BIGSERIAL PRIMARY KEY,
                                zone_id BIGINT NOT NULL REFERENCES seat_zones(id),
                                user_id BIGINT NOT NULL REFERENCES users(id),
                                seat_ids TEXT NOT NULL, -- comma-separated, đơn giản cho scope này
                                is_split BOOLEAN NOT NULL DEFAULT FALSE,
                                group_count INT NOT NULL DEFAULT 1,
                                status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, EXPIRED, REJECTED
                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_seat_proposals_status_expires ON seat_proposals(status, expires_at);