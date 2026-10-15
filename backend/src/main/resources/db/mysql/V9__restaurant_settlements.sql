ALTER TABLE orders
    ADD COLUMN voucher_scope ENUM('GLOBAL','RESTAURANT') NULL AFTER voucher_name,
    ADD COLUMN restaurant_discount_amount INT NOT NULL DEFAULT 0 AFTER voucher_scope;

UPDATE orders o
LEFT JOIN vouchers v ON UPPER(v.code) = UPPER(o.voucher_code)
SET o.voucher_scope = v.scope,
    o.restaurant_discount_amount = CASE WHEN v.scope = 'RESTAURANT' THEN o.discount_amount ELSE 0 END
WHERE o.voucher_code IS NOT NULL;

CREATE TABLE restaurant_settlements (
    id VARCHAR(60) PRIMARY KEY,
    restaurant_id VARCHAR(50) NOT NULL,
    restaurant_name VARCHAR(160) NOT NULL,
    period_start DATETIME(6) NOT NULL,
    period_end DATETIME(6) NOT NULL,
    status ENUM('PENDING_REVIEW','PENDING_PAYMENT','PAID') NOT NULL,
    order_count INT NOT NULL DEFAULT 0,
    food_amount BIGINT NOT NULL DEFAULT 0,
    restaurant_tax_amount BIGINT NOT NULL DEFAULT 0,
    restaurant_discount_amount BIGINT NOT NULL DEFAULT 0,
    platform_discount_amount BIGINT NOT NULL DEFAULT 0,
    commission_rate_bps INT NOT NULL,
    commission_amount BIGINT NOT NULL DEFAULT 0,
    refund_amount BIGINT NOT NULL DEFAULT 0,
    adjustment_amount BIGINT NOT NULL DEFAULT 0,
    payable_amount BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(60) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    submitted_by VARCHAR(60) NULL,
    submitted_at DATETIME(6) NULL,
    paid_by VARCHAR(60) NULL,
    paid_at DATETIME(6) NULL,
    payment_reference VARCHAR(120) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_settlement_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    INDEX idx_settlement_status_created (status, created_at),
    INDEX idx_settlement_restaurant_period (restaurant_id, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE restaurant_settlement_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    settlement_id VARCHAR(60) NOT NULL,
    order_id VARCHAR(60) NOT NULL,
    food_amount BIGINT NOT NULL DEFAULT 0,
    restaurant_tax_amount BIGINT NOT NULL DEFAULT 0,
    restaurant_discount_amount BIGINT NOT NULL DEFAULT 0,
    platform_discount_amount BIGINT NOT NULL DEFAULT 0,
    commission_amount BIGINT NOT NULL DEFAULT 0,
    refund_amount BIGINT NOT NULL DEFAULT 0,
    payable_amount BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_settlement_order_batch FOREIGN KEY (settlement_id) REFERENCES restaurant_settlements(id),
    CONSTRAINT fk_settlement_order_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT uk_settlement_order UNIQUE (order_id),
    INDEX idx_settlement_orders_batch (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
