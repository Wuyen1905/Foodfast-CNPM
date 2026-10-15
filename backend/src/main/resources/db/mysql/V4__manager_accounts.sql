ALTER TABLE users
    ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER role,
    ADD COLUMN manager_permissions VARCHAR(500) NULL AFTER account_status;

UPDATE users
SET account_status = 'suspended', role = 'customer'
WHERE LOWER(role) = 'suspended';

CREATE INDEX idx_users_role_status ON users (role, account_status);
