ALTER TABLE order_groups
    ADD COLUMN payable_amount INTEGER NOT NULL DEFAULT 0 AFTER total,
    ADD COLUMN payment_session_id VARCHAR(80) NULL AFTER payable_amount,
    ADD COLUMN payment_request_key VARCHAR(100) NULL AFTER payment_session_id,
    ADD COLUMN payment_request_url VARCHAR(2048) NULL AFTER payment_request_key,
    ADD COLUMN payment_request_expires_at DATETIME(6) NULL AFTER payment_request_url,
    ADD COLUMN vnpay_transaction_id VARCHAR(100) NULL AFTER payment_request_expires_at,
    ADD COLUMN paid_at BIGINT NULL AFTER vnpay_transaction_id,
    ADD COLUMN partial_refund_amount INTEGER NOT NULL DEFAULT 0 AFTER paid_at,
    ADD COLUMN refunded_amount INTEGER NOT NULL DEFAULT 0 AFTER partial_refund_amount,
    ADD CONSTRAINT uk_order_groups_payment_session UNIQUE (payment_session_id),
    ADD CONSTRAINT uk_order_groups_vnpay_transaction UNIQUE (vnpay_transaction_id);

UPDATE order_groups SET payable_amount = total WHERE payable_amount = 0;

ALTER TABLE orders
    ADD COLUMN payment_allocated_amount INTEGER NOT NULL DEFAULT 0 AFTER partial_refund_amount;

ALTER TABLE restaurant_couriers
    MODIFY restaurant_id VARCHAR(50) NULL;

ALTER TABLE delivery_assignments
    MODIFY courier_id VARCHAR(50) NULL,
    MODIFY status ENUM('WAITING','ASSIGNED','DELIVERING','DELIVERED','FAILED','CANCELLED') NOT NULL;

ALTER TABLE vouchers
    ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'RESTAURANT' AFTER restaurant_id;

UPDATE vouchers
SET scope = CASE WHEN restaurant_id IS NULL THEN 'GLOBAL' ELSE 'RESTAURANT' END;

ALTER TABLE vouchers
    MODIFY scope ENUM('GLOBAL','RESTAURANT') NOT NULL;

CREATE INDEX idx_delivery_courier_status ON delivery_assignments(courier_id, status);
CREATE INDEX idx_order_groups_payment_session ON order_groups(payment_session_id);
