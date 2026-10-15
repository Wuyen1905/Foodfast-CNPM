CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    restaurant_id VARCHAR(50),
    order_count INTEGER,
    created_at BIGINT,
    last_login_at BIGINT,
    suspension_reason VARCHAR(500),
    account_status_updated_at BIGINT,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE restaurants (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    rating DOUBLE NOT NULL,
    primary_color VARCHAR(20),
    secondary_color VARCHAR(20),
    accent_color VARCHAR(20),
    owner_id VARCHAR(50) NOT NULL,
    is_active BOOLEAN,
    admin_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    admin_status_reason VARCHAR(500),
    admin_status_updated_at BIGINT,
    accepting_orders BOOLEAN NOT NULL,
    pause_reason VARCHAR(300),
    opening_time VARCHAR(5) NOT NULL,
    closing_time VARCHAR(5) NOT NULL,
    created_at BIGINT,
    INDEX idx_restaurants_admin_status (admin_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price INTEGER NOT NULL,
    category VARCHAR(255) NOT NULL,
    image_url VARCHAR(1024),
    restaurant VARCHAR(255) NOT NULL,
    available BOOLEAN NOT NULL,
    customizable BOOLEAN NOT NULL,
    daily_limit INTEGER,
    CONSTRAINT ck_products_daily_limit CHECK (daily_limit IS NULL OR daily_limit > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_option_groups (
    id VARCHAR(80) PRIMARY KEY,
    product_id VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    selection_type VARCHAR(20) NOT NULL,
    required BOOLEAN NOT NULL,
    min_selections INTEGER NOT NULL,
    max_selections INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT fk_option_group_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_options (
    id VARCHAR(80) PRIMARY KEY,
    group_id VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    price_delta INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT fk_product_option_group FOREIGN KEY (group_id) REFERENCES product_option_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE vouchers (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    restaurant_id VARCHAR(60),
    discount_type VARCHAR(20) NOT NULL,
    discount_value INTEGER NOT NULL,
    min_order_amount INTEGER NOT NULL,
    max_discount INTEGER NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    usage_limit INTEGER NOT NULL,
    per_user_limit INTEGER NOT NULL,
    active BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
    id VARCHAR(60) PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    address LONGTEXT,
    restaurant VARCHAR(255),
    restaurant_id VARCHAR(255),
    user_id VARCHAR(255),
    payment_session_id VARCHAR(80),
    idempotency_key VARCHAR(100),
    idempotency_request_hash VARCHAR(64),
    payment_request_key VARCHAR(100),
    payment_request_url VARCHAR(2048),
    payment_request_expires_at DATETIME(6),
    payment_method VARCHAR(255),
    payment_status VARCHAR(255),
    note VARCHAR(1000),
    internal_notes VARCHAR(2000),
    vnpay_transaction_id VARCHAR(100),
    confirmed_at BIGINT,
    preparing_at BIGINT,
    ready_at BIGINT,
    delivering_at BIGINT,
    delivered_at BIGINT,
    cancelled_at BIGINT,
    paid_at BIGINT,
    cancellation_actor VARCHAR(20),
    cancellation_actor_id VARCHAR(100),
    cancellation_reason_code VARCHAR(50),
    cancellation_reason_detail VARCHAR(500),
    confirmed_by VARCHAR(100),
    status VARCHAR(32) NOT NULL,
    total INTEGER NOT NULL,
    subtotal INTEGER NOT NULL,
    shipping_fee INTEGER NOT NULL,
    tax_amount INTEGER NOT NULL,
    discount_amount INTEGER NOT NULL,
    voucher_code VARCHAR(40),
    voucher_name VARCHAR(120),
    partial_refund_amount INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_orders_vnpay_transaction UNIQUE (vnpay_transaction_id),
    INDEX idx_orders_user_created (user_id, created_at),
    INDEX idx_orders_restaurant_created (restaurant_id, created_at),
    INDEX idx_orders_payment_session (payment_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(40),
    name VARCHAR(255) NOT NULL,
    qty INTEGER NOT NULL,
    price INTEGER NOT NULL,
    product_name VARCHAR(200),
    product_image VARCHAR(2048),
    item_note VARCHAR(200),
    original_product_id VARCHAR(40),
    original_product_name VARCHAR(200),
    substitution_proposal_id BIGINT,
    order_id VARCHAR(60),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_item_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    option_id VARCHAR(80),
    group_name VARCHAR(120) NOT NULL,
    option_name VARCHAR(120) NOT NULL,
    price_delta INTEGER NOT NULL,
    CONSTRAINT fk_order_item_option_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(255),
    product_name VARCHAR(255),
    product_image VARCHAR(1024),
    unit_price DOUBLE,
    quantity INTEGER,
    restaurant_code VARCHAR(255),
    user_id VARCHAR(255),
    variant_key VARCHAR(1024) NOT NULL,
    selected_option_ids VARCHAR(1000),
    option_summary VARCHAR(1000),
    item_note VARCHAR(200),
    INDEX idx_cart_items_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE restaurant_couriers (
    id VARCHAR(50) PRIMARY KEY,
    restaurant_id VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(50),
    license_plate VARCHAR(30),
    active BOOLEAN NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE delivery_assignments (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(60) NOT NULL,
    restaurant_id VARCHAR(50) NOT NULL,
    courier_id VARCHAR(50) NOT NULL,
    status VARCHAR(24) NOT NULL,
    estimated_delivery_at BIGINT,
    delivery_note VARCHAR(1000),
    failure_reason VARCHAR(1000),
    assigned_at BIGINT NOT NULL,
    started_at BIGINT,
    delivered_at BIGINT,
    completion_source VARCHAR(30),
    completion_note VARCHAR(1000),
    failed_at BIGINT,
    cancelled_at BIGINT,
    updated_at BIGINT NOT NULL,
    version BIGINT,
    INDEX idx_delivery_restaurant (restaurant_id),
    INDEX idx_delivery_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE analytics (
    id VARCHAR(50) PRIMARY KEY,
    restaurant_id VARCHAR(50) NOT NULL,
    period VARCHAR(255) NOT NULL,
    revenue INTEGER NOT NULL,
    orders INTEGER NOT NULL,
    avg_order_value INTEGER NOT NULL,
    delivery_time INTEGER NOT NULL,
    created_at BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id VARCHAR(50) PRIMARY KEY,
    restaurant_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(60),
    customer_name VARCHAR(255),
    customer_phone VARCHAR(255),
    total BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN,
    INDEX idx_notifications_restaurant (restaurant_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reviews (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(60) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    restaurant_id VARCHAR(50) NOT NULL,
    rating INTEGER NOT NULL,
    comment VARCHAR(1000),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uk_review_order UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE substitution_proposals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(60) NOT NULL,
    order_item_id BIGINT NOT NULL,
    restaurant_id VARCHAR(60) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    original_product_id VARCHAR(40),
    original_product_name VARCHAR(200) NOT NULL,
    original_unit_price INTEGER NOT NULL,
    replacement_product_id VARCHAR(40) NOT NULL,
    replacement_product_name VARCHAR(200) NOT NULL,
    replacement_unit_price INTEGER NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    proposed_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE voucher_redemptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_code VARCHAR(40) NOT NULL,
    order_id VARCHAR(60) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_voucher_redemption_order UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE support_tickets (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(60),
    restaurant_id VARCHAR(50),
    customer_phone VARCHAR(20),
    customer_id VARCHAR(50),
    customer_name VARCHAR(150),
    intent VARCHAR(80) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    target VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    department VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER_SERVICE',
    assigned_admin_id VARCHAR(60),
    assigned_admin_name VARCHAR(150),
    sla_due_at BIGINT,
    resolution_note VARCHAR(1200),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    resolved_at BIGINT,
    INDEX idx_support_tickets_restaurant (restaurant_id, created_at),
    INDEX idx_support_ticket_sla (status, sla_due_at),
    INDEX idx_support_tickets_customer (customer_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE support_ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(50) NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    sender_id VARCHAR(60) NOT NULL,
    sender_name VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_support_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    INDEX idx_support_messages_ticket_created (ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE support_ticket_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(50) NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    actor_id VARCHAR(60) NOT NULL,
    note VARCHAR(1200),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_support_history_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    INDEX idx_support_history_ticket (ticket_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaints (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(60) NOT NULL,
    restaurant_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(60) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(1200) NOT NULL,
    requested_refund_amount INTEGER NOT NULL,
    approved_refund_amount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    refund_status VARCHAR(20) NOT NULL,
    handler_role VARCHAR(20),
    handler_id VARCHAR(60),
    resolution VARCHAR(1200),
    refund_transaction_reference VARCHAR(100),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    resolved_at BIGINT,
    INDEX idx_complaints_customer (customer_id, created_at),
    INDEX idx_complaints_restaurant (restaurant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_evidence_urls (
    complaint_id VARCHAR(50) NOT NULL,
    evidence_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_complaint_evidence FOREIGN KEY (complaint_id) REFERENCES complaints(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id VARCHAR(50) NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    actor_id VARCHAR(60) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(1200),
    refund_amount INTEGER NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(40) NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT uk_favorites_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_favorites_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE delivery_addresses (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    label VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line LONGTEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT fk_delivery_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_delivery_addresses_user_default (user_id, is_default, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    admin_id VARCHAR(50) NOT NULL,
    admin_name VARCHAR(255) NOT NULL,
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(80) NOT NULL,
    target_name VARCHAR(255) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    INDEX idx_admin_audit_logs_timestamp (timestamp DESC),
    INDEX idx_admin_audit_logs_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(60) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    actor_id VARCHAR(100),
    actor_role VARCHAR(24) NOT NULL,
    source VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_order_status_history_order_created (order_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customer_notifications (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(60),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    action_url VARCHAR(255),
    is_read BOOLEAN NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_customer_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_customer_notification_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_customer_notification_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
