CREATE TABLE organizations (
                               id BIGSERIAL PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE organization_members (
                                      id BIGSERIAL PRIMARY KEY,
                                      organization_id BIGINT NOT NULL REFERENCES organizations(id),
                                      user_id BIGINT NOT NULL REFERENCES users(id),
                                      member_role VARCHAR(20) NOT NULL, -- OWNER, STAFF
                                      added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      UNIQUE (organization_id, user_id),
                                      CONSTRAINT chk_member_role CHECK (member_role IN ('OWNER', 'STAFF'))
);
CREATE INDEX idx_org_members_user ON organization_members(user_id);

ALTER TABLE concerts ADD COLUMN organization_id BIGINT REFERENCES organizations(id);

-- Backfill: mỗi organizer_id hiện có trở thành 1 Organization riêng, họ là OWNER
INSERT INTO organizations (name)
SELECT DISTINCT u.full_name || '''s Organization'
FROM concerts c JOIN users u ON u.id = c.organizer_id
WHERE c.organizer_id IS NOT NULL;

-- (Thực hiện backfill organization_id + organization_members qua script/migration
--  Java callback nếu cần match chính xác theo tên, hoặc viết tay cho từng
--  organizer thật trong dữ liệu seed hiện có — xem ghi chú bên dưới)

CREATE INDEX idx_concerts_organization ON concerts(organization_id);