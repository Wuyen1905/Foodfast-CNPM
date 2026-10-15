CREATE TABLE order_groups (
    id VARCHAR(60) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    customer_email VARCHAR(255),
    address LONGTEXT NOT NULL,
    note VARCHAR(1000),
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    subtotal INTEGER NOT NULL,
    shipping_fee INTEGER NOT NULL,
    tax_amount INTEGER NOT NULL,
    discount_amount INTEGER NOT NULL,
    total INTEGER NOT NULL,
    order_count INTEGER NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_order_groups_idempotency UNIQUE (idempotency_key),
    INDEX idx_order_groups_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE orders
    ADD COLUMN order_group_id VARCHAR(60) NULL AFTER user_id,
    ADD COLUMN group_position INTEGER NULL AFTER order_group_id,
    ADD CONSTRAINT fk_orders_order_group
        FOREIGN KEY (order_group_id) REFERENCES order_groups(id),
    ADD INDEX idx_orders_group_position (order_group_id, group_position);
