ALTER TABLE users ADD COLUMN account_status VARCHAR(20) DEFAULT 'active' NOT NULL;
ALTER TABLE users ADD COLUMN manager_permissions VARCHAR(500);

UPDATE users
SET account_status = 'suspended', role = 'customer'
WHERE LOWER(role) = 'suspended';

CREATE INDEX idx_users_role_status ON users (role, account_status);
