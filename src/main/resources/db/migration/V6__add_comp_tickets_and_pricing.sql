ALTER TABLE concerts ADD COLUMN organizer_id BIGINT REFERENCES users(id);
ALTER TABLE concerts ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE concerts ADD COLUMN rejection_reason VARCHAR(500);
ALTER TABLE concerts ADD COLUMN reviewed_by BIGINT REFERENCES users(id);
ALTER TABLE concerts ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_concerts_organizer ON concerts(organizer_id);
CREATE INDEX idx_concerts_approval_status ON concerts(approval_status);

ALTER TABLE concerts ADD CONSTRAINT chk_concert_approval_status
    CHECK (approval_status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'));