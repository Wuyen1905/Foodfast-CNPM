ALTER TABLE orders ADD COLUMN refund_transaction_reference VARCHAR(100);
ALTER TABLE orders ADD COLUMN refund_completed_at BIGINT;
ALTER TABLE orders ADD COLUMN refund_note VARCHAR(1200);

ALTER TABLE delivery_assignments
    ADD CONSTRAINT uk_delivery_assignment_order UNIQUE (order_id);

CREATE INDEX idx_orders_refund_transaction ON orders(refund_transaction_reference);
