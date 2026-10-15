ALTER TABLE orders
    ADD COLUMN refund_transaction_reference VARCHAR(100) NULL AFTER payment_allocated_amount,
    ADD COLUMN refund_completed_at BIGINT NULL AFTER refund_transaction_reference,
    ADD COLUMN refund_note VARCHAR(1200) NULL AFTER refund_completed_at;

ALTER TABLE delivery_assignments
    ADD CONSTRAINT uk_delivery_assignment_order UNIQUE (order_id);

CREATE INDEX idx_orders_refund_transaction ON orders(refund_transaction_reference);
