CREATE TABLE order_groups (
    id VARCHAR(60) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    customer_email VARCHAR(255),
    address CLOB NOT NULL,
    note VARCHAR(1000),
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    subtotal INTEGER NOT NULL,
    shipping_fee INTEGER NOT NULL,
    tax_amount INTEGER NOT NULL,
    discount_amount INTEGER NOT NULL,
    total INTEGER NOT NULL,
    order_count INTEGER NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_order_groups_user_created ON order_groups(user_id, created_at);

ALTER TABLE orders ADD COLUMN order_group_id VARCHAR(60);
ALTER TABLE orders ADD COLUMN group_position INTEGER;
ALTER TABLE orders ADD CONSTRAINT fk_orders_order_group
    FOREIGN KEY (order_group_id) REFERENCES order_groups(id);
CREATE INDEX idx_orders_group_position ON orders(order_group_id, group_position);
