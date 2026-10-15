-- Three additional demo couriers for centralized administrator dispatch.
-- restaurant_id is NULL because Admin may assign them to any restaurant order.
INSERT IGNORE INTO restaurant_couriers (
    id, restaurant_id, name, phone, vehicle_type, license_plate,
    active, created_at, updated_at
) VALUES
('COURIER-FF-001', NULL, 'Võ Thành Công', '0905678901', 'Xe máy', '59E1-56789', true, 1690000000000, 1690000000000),
('COURIER-FF-002', NULL, 'Đặng Ngọc Linh', '0906789012', 'Xe máy điện', '59F1-67890', true, 1690000000000, 1690000000000),
('COURIER-FF-003', NULL, 'Bùi Anh Tuấn', '0907890123', 'Xe máy', '59G1-78901', true, 1690000000000, 1690000000000);
