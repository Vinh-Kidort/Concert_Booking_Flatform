ALTER TABLE booking_items ADD COLUMN seat_id BIGINT REFERENCES seats(id);
ALTER TABLE booking_items ALTER COLUMN ticket_category_id DROP NOT NULL;
-- Một booking_item giờ thuộc VỀ MỘT trong hai nguồn: ticket_category (standing)
-- HOẶC seat (seating) — không cả hai, không cả không có.
ALTER TABLE booking_items ADD CONSTRAINT chk_booking_item_source
    CHECK (
        (ticket_category_id IS NOT NULL AND seat_id IS NULL) OR
        (ticket_category_id IS NULL AND seat_id IS NOT NULL)
        );