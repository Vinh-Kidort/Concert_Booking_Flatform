ALTER TABLE ticket_categories ADD COLUMN comp_quantity INT NOT NULL DEFAULT 0;

CREATE TABLE comp_tickets (
                              id BIGSERIAL PRIMARY KEY,
                              ticket_category_id BIGINT NOT NULL REFERENCES ticket_categories(id),
                              issued_by BIGINT NOT NULL REFERENCES users(id),
                              recipient_name VARCHAR(255) NOT NULL,
                              recipient_email VARCHAR(255),
                              recipient_type VARCHAR(30) NOT NULL,
                              qr_code_token VARCHAR(255) NOT NULL UNIQUE,
                              status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
                              issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              checked_in_at TIMESTAMP WITH TIME ZONE,
                              CONSTRAINT chk_comp_ticket_status CHECK (status IN ('ISSUED', 'CHECKED_IN', 'REVOKED')),
                              CONSTRAINT chk_comp_ticket_recipient_type CHECK (recipient_type IN ('GUEST', 'SPONSOR', 'VIP', 'STAFF'))
);

CREATE INDEX idx_comp_tickets_category ON comp_tickets(ticket_category_id);
CREATE INDEX idx_comp_tickets_category_status ON comp_tickets(ticket_category_id, status);

ALTER TABLE ticket_categories ADD CONSTRAINT chk_comp_quantity_valid
    CHECK (comp_quantity >= 0 AND comp_quantity <= total_quantity);