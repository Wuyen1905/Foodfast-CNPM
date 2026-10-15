ALTER TABLE order_groups ADD COLUMN payable_amount INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE order_groups ADD COLUMN payment_session_id VARCHAR(80);
ALTER TABLE order_groups ADD COLUMN payment_request_key VARCHAR(100);
ALTER TABLE order_groups ADD COLUMN payment_request_url VARCHAR(2048);
ALTER TABLE order_groups ADD COLUMN payment_request_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE order_groups ADD COLUMN vnpay_transaction_id VARCHAR(100);
ALTER TABLE order_groups ADD COLUMN paid_at BIGINT;
ALTER TABLE order_groups ADD COLUMN partial_refund_amount INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE order_groups ADD COLUMN refunded_amount INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE order_groups ADD CONSTRAINT uk_order_groups_payment_session UNIQUE (payment_session_id);
ALTER TABLE order_groups ADD CONSTRAINT uk_order_groups_vnpay_transaction UNIQUE (vnpay_transaction_id);
UPDATE order_groups SET payable_amount = total WHERE payable_amount = 0;

ALTER TABLE orders ADD COLUMN payment_allocated_amount INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE restaurant_couriers ALTER COLUMN restaurant_id DROP NOT NULL;
ALTER TABLE delivery_assignments ALTER COLUMN courier_id DROP NOT NULL;

ALTER TABLE vouchers ADD COLUMN scope VARCHAR(20) DEFAULT 'RESTAURANT' NOT NULL;
UPDATE vouchers SET scope = CASE WHEN restaurant_id IS NULL THEN 'GLOBAL' ELSE 'RESTAURANT' END;

CREATE INDEX idx_delivery_courier_status ON delivery_assignments(courier_id, status);
CREATE INDEX idx_order_groups_payment_session ON order_groups(payment_session_id);
