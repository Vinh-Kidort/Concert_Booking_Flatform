ALTER TABLE ticket_categories ADD COLUMN original_price DECIMAL(12,2);
ALTER TABLE ticket_categories ADD COLUMN discounted_price DECIMAL(12,2);
ALTER TABLE ticket_categories ADD COLUMN discount_applied_at TIMESTAMP WITH TIME ZONE;

-- Backfill existing rows so original_price is never null for pre-existing data
UPDATE ticket_categories SET original_price = price WHERE original_price IS NULL;

ALTER TABLE ticket_categories ALTER COLUMN original_price SET NOT NULL;

CREATE TABLE price_change_audits (
                                     id BIGSERIAL PRIMARY KEY,
                                     ticket_category_id BIGINT NOT NULL REFERENCES ticket_categories(id),
                                     changed_by BIGINT NOT NULL REFERENCES users(id),
                                     old_price DECIMAL(12,2) NOT NULL,
                                     new_price DECIMAL(12,2) NOT NULL,
                                     changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_price_change_audits_category ON price_change_audits(ticket_category_id);