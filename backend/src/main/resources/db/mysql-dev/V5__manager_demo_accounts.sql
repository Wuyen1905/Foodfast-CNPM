INSERT INTO users (
    id, username, password, name, email, phone, role, account_status,
    manager_permissions, restaurant_id, order_count, created_at
) VALUES
(
    'manager_1', 'quanly',
    '$2a$12$HeYncolj.7AbIQQIrOAuxe.KMoQrt57T4ChGl75JHi1quYfs8a9KW',
    'Nhân viên quản lý vận hành', 'quanly@foodfast.local', '0900000011',
    'manager', 'active', 'ORDER_MANAGEMENT,CUSTOMER_SUPPORT', NULL, 0, 1690000000000
),
(
    'manager_2', 'quanly1',
    '$2a$12$HeYncolj.7AbIQQIrOAuxe.KMoQrt57T4ChGl75JHi1quYfs8a9KW',
    'Nhân viên quản lý tài chính', 'quanly1@foodfast.local', '0900000012',
    'manager', 'active', 'PAYMENT_RECONCILIATION,RESTAURANT_SETTLEMENT', NULL, 0, 1690000000000
);
