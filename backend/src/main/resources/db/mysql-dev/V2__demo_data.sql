-- Development/test data. This migration is not loaded by the production profile.
INSERT INTO users (id, username, password, name, email, phone, role, restaurant_id, order_count, created_at) VALUES
('u1', 'admin', '$2a$12$EphHIavayfHpZt4ht96.NOuoC9Q3gu3GSI0tASAVMhzByDJae7nwi', 'Quản trị viên FoodFast', 'admin@foodfast.com', '0900000001', 'admin', NULL, 0, 1690000000000),
('u2', 'user', '$2a$12$pRThM4yZ8z8HNaWgznJZOurApyYO9DIMdAwMULeDQAoKaE9WywmTa', 'Khách hàng mẫu', 'user@example.com', '0123456789', 'customer', NULL, 12, 1690000000000),
('u3', 'sweetdreams', '$2a$12$h2w5hVPyQDozEx1zZk8qmORxQsUSwpdi8NorDYIgDo/bkcV8Fm.NK', 'Chủ tiệm SweetDreams', 'owner@sweetdreams.com', '0987654321', 'restaurant', 'rest_2', 0, 1690000000000),
('u4', 'user1', '$2a$12$JJ92Pw9gQDxQGpfLy5z2TObeVJNzau9YGq7jGjssWAGfIugElVv8O', 'Khách hàng thử nghiệm', 'user1@example.com', '0912345678', 'customer', NULL, 5, 1690000000000),
('owner_aloha', 'aloha_restaurant', '$2a$12$IX.xrlQpbeoWHrqmlGCxRe3xCayUz4BtqkccLRUAtmv3yiRBY0UjO', 'Chủ nhà hàng Aloha', 'owner@alohakitchen.com', '0900000002', 'restaurant', 'restaurant_2', 0, 1690000000000);

INSERT INTO restaurants (id, name, description, category, location, rating, primary_color, secondary_color, accent_color, owner_id, is_active, accepting_orders, pause_reason, opening_time, closing_time, created_at) VALUES
('rest_1', 'Nhà hàng FoodFast', 'Nhà hàng FoodFast với dịch vụ giao hàng nội bộ', 'Đồ ăn nhanh', 'Trung tâm thành phố', 4.5, '#FF6600', '#FF8C00', '#FFA500', 'u1', true, true, NULL, '08:00', '22:00', 1690000000000),
('rest_2', 'Tiệm bánh SweetDreams', 'Bánh ngọt và món tráng miệng do nhân viên của tiệm giao tận nơi', 'Bánh ngọt và tráng miệng', 'Khu trung tâm thương mại', 4.8, '#E91E63', '#F06292', '#F8BBD9', 'u3', true, true, NULL, '08:00', '22:00', 1690000000000),
('restaurant_2', 'Nhà hàng Aloha', 'Món Á kết hợp phong cách Hawaii dành cho khách hàng bận rộn', 'Món Á và Hawaii', 'Thành phố Hồ Chí Minh', 4.7, '#ffcc70', '#ff9671', '#ffc75f', 'owner_aloha', true, true, NULL, '08:00', '22:00', 1690000000000);

INSERT INTO restaurant_couriers (id, restaurant_id, name, phone, vehicle_type, license_plate, active, created_at, updated_at) VALUES
('COURIER-SD-001', 'rest_2', 'Nguyễn Minh Anh', '0901234567', 'Xe máy', '59A1-12345', true, 1690000000000, 1690000000000),
('COURIER-SD-002', 'rest_2', 'Trần Quốc Bảo', '0902345678', 'Xe máy', '59B1-23456', true, 1690000000000, 1690000000000),
('COURIER-AK-001', 'restaurant_2', 'Lê Hoàng Nam', '0903456789', 'Xe máy', '59C1-34567', true, 1690000000000, 1690000000000),
('COURIER-AK-002', 'restaurant_2', 'Phạm Thu Hà', '0904567890', 'Xe máy điện', '59D1-45678', true, 1690000000000, 1690000000000);

INSERT INTO products (id, name, description, price, category, image_url, restaurant, available, customizable) VALUES
('sd-1', 'Bánh Donut', 'Bánh donut phủ đường giòn rụm, hương vị thơm ngon cho buổi sáng năng động', 25000, 'Bánh ngọt', 'https://th.bing.com/th/id/R.7a2114b6f278629b086c99a631c17765?rik=6Wiy3P2mDU64dA&riu=http%3a%2f%2fcdn.tgdd.vn%2fFiles%2f2021%2f07%2f28%2f1371385%2f2-cach-lam-banh-donut-nuong-va-chien-ngon-don-gian-tai-nha-202206031611571875.jpg&ehk=mlYN0Fber9i7dgMFrlWnqQ3hjz2S247pLeK7W9g4r0g%3d&risl=&pid=ImgRaw&r=0', 'SweetDreams', true, true),
('sd-2', 'Bánh Tiramisu', 'Bánh tiramisu truyền thống Ý với mascarpone và cà phê, hương vị đậm đà', 55000, 'Tráng miệng', 'https://food.fnr.sndimg.com/content/dam/images/food/fullset/2011/2/4/2/RX-FNM_030111-Sugar-Fix-005_s4x3.jpg.rend.hgtvcom.826.620.suffix/1371597326801.jpeg', 'SweetDreams', true, false),
('sd-3', 'Bánh Phô Mai Dâu', 'Bánh phô mai dâu tươi với lớp kem mềm mịn, vị ngọt thanh', 45000, 'Tráng miệng', 'https://japanjoy.vn/images/thumbs/0000995_pre-order-banh-pho-mai-dau-tay-dam-da-tokyo-hop-6-cai.png', 'SweetDreams', true, false),
('sd-4', 'Bánh Croissant', 'Bánh croissant Pháp giòn tan, bơ thơm lừng', 35000, 'Bánh ngọt', 'https://tahu.edu.vn/wp-content/uploads/2024/07/banh-Croissant.jpeg', 'SweetDreams', true, false),
('ak-1', 'Hamburger', 'Hamburger thịt bò nướng với rau tươi và phô mai, hương vị đậm đà', 79000, 'Món chính', 'https://tse4.mm.bing.net/th/id/OIP.pIB3_58RcdOaM26-dIGKnwHaE0?rs=1&pid=ImgDetMain&o=7&rm=3', 'Aloha', true, true),
('ak-2', 'Pizza Hawaii', 'Pizza Hawaii với phô mai tan chảy và dứa ngọt, hương vị hòa quyện hoàn hảo', 89000, 'Món chính', '/images/pizza-hawaii.svg', 'Aloha', true, true),
('ak-3', 'Cơm Chiên Hawaii', 'Cơm chiên Hawaii với thịt nướng và dứa tươi, hương vị nhiệt đới', 69000, 'Món chính', 'https://media-cdn.tripadvisor.com/media/photo-s/18/29/f2/5c/com-chien-cua-bi-n.jpg', 'Aloha', true, false),
('ak-4', 'Chả Giò Chiên', 'Chả giò chiên giòn với nhân thịt và rau củ tươi ngon', 45000, 'Món chính', 'https://tse3.mm.bing.net/th/id/OIP.7044maV58Afw74b7gOKZ6wHaEx?rs=1&pid=ImgDetMain&o=7&rm=3', 'Aloha', true, false);

INSERT INTO product_option_groups (id, product_id, name, selection_type, required, min_selections, max_selections, display_order, active) VALUES
('ak-2-size', 'ak-2', 'Kích cỡ', 'SINGLE', true, 1, 1, 1, true),
('ak-2-topping', 'ak-2', 'Topping thêm', 'MULTIPLE', false, 0, 2, 2, true),
('ak-1-cheese', 'ak-1', 'Phô mai', 'SINGLE', true, 1, 1, 1, true),
('sd-1-flavour', 'sd-1', 'Hương vị', 'SINGLE', true, 1, 1, 1, true);

INSERT INTO product_options (id, group_id, name, price_delta, display_order, active) VALUES
('ak-2-size-m', 'ak-2-size', 'Vừa', 0, 1, true),
('ak-2-size-l', 'ak-2-size', 'Lớn', 30000, 2, true),
('ak-2-extra-cheese', 'ak-2-topping', 'Thêm phô mai', 15000, 1, true),
('ak-2-extra-ham', 'ak-2-topping', 'Thêm thịt nguội', 20000, 2, true),
('ak-1-cheese-normal', 'ak-1-cheese', 'Phô mai thường', 0, 1, true),
('ak-1-cheese-double', 'ak-1-cheese', 'Gấp đôi phô mai', 12000, 2, true),
('sd-1-sugar', 'sd-1-flavour', 'Đường truyền thống', 0, 1, true),
('sd-1-chocolate', 'sd-1-flavour', 'Sô-cô-la', 5000, 2, true);

INSERT INTO vouchers (code, name, restaurant_id, discount_type, discount_value, min_order_amount, max_discount, starts_at, ends_at, usage_limit, per_user_limit, active) VALUES
('WELCOME20', 'Giảm 20% cho đơn đầu', NULL, 'PERCENT', 20, 80000, 40000, '2025-01-01 00:00:00', '2030-12-31 23:59:59', 1000, 1, true),
('ALOHA30K', 'Aloha giảm 30.000 đ', 'restaurant_2', 'FIXED', 30000, 150000, 30000, '2025-01-01 00:00:00', '2030-12-31 23:59:59', 500, 2, true),
('SWEET10', 'SweetDreams giảm 10%', 'rest_2', 'PERCENT', 10, 50000, 20000, '2025-01-01 00:00:00', '2030-12-31 23:59:59', 500, 2, true);

INSERT INTO orders (id, customer_name, customer_phone, customer_email, address, restaurant, restaurant_id, user_id, payment_session_id, payment_method, payment_status, note, status, subtotal, shipping_fee, tax_amount, discount_amount, partial_refund_amount, total, created_at, updated_at) VALUES
('ORDER-001', 'Nguyễn Văn A', '0123456789', 'nguyenvana@example.com', '123 Đường ABC, Quận 1, TP.HCM', 'SweetDreams', 'rest_2', 'u2', NULL, 'cash', 'paid', 'Giao nhanh giúp em', 'DELIVERING', 105000, 15000, 10500, 0, 0, 130500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ORDER-002', 'Trần Thị B', '0987654321', 'tranthib@example.com', '456 Đường XYZ, Quận 2, TP.HCM', 'Aloha', 'restaurant_2', 'u4', NULL, 'vnpay', 'paid', NULL, 'PREPARING', 158000, 15000, 15800, 0, 0, 188800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ORDER-003', 'Lê Văn C', '0912345678', 'levanc@example.com', '789 Đường DEF, Quận 3, TP.HCM', 'SweetDreams', 'rest_2', 'u2', NULL, 'cash', 'pending', NULL, 'PENDING', 105000, 15000, 10500, 0, 0, 130500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO delivery_assignments (id, order_id, restaurant_id, courier_id, status, estimated_delivery_at, delivery_note, failure_reason, assigned_at, started_at, delivered_at, failed_at, cancelled_at, updated_at, version) VALUES
('DELIVERY-001', 'ORDER-001', 'rest_2', 'COURIER-SD-001', 'DELIVERING', 1893457800000, 'Giao nhanh, gọi khách trước khi đến', NULL, 1690000000000, 1690000300000, NULL, NULL, NULL, 1690000300000, 0);

INSERT INTO order_items (product_id, name, qty, price, product_name, order_id) VALUES
('sd-1', 'Bánh Donut', 2, 25000, 'Bánh Donut', 'ORDER-001'),
('sd-2', 'Bánh Tiramisu', 1, 55000, 'Bánh Tiramisu', 'ORDER-001'),
('ak-1', 'Hamburger', 2, 79000, 'Hamburger', 'ORDER-002'),
('sd-3', 'Bánh Phô Mai Dâu', 1, 45000, 'Bánh Phô Mai Dâu', 'ORDER-003'),
('sd-4', 'Bánh Croissant', 1, 35000, 'Bánh Croissant', 'ORDER-003'),
('sd-1', 'Bánh Donut', 1, 25000, 'Bánh Donut', 'ORDER-003');

INSERT INTO analytics (id, restaurant_id, period, revenue, orders, avg_order_value, delivery_time, created_at) VALUES
('analytics-rest_2-day', 'rest_2', 'day', 261000, 2, 130500, 18, 1700000000000),
('analytics-restaurant_2-day', 'restaurant_2', 'day', 188800, 1, 188800, 20, 1700000000000),
('analytics-rest_2-week', 'rest_2', 'week', 500000, 10, 50000, 18, 1700000000000),
('analytics-restaurant_2-week', 'restaurant_2', 'week', 800000, 8, 100000, 20, 1700000000000);

INSERT INTO notifications (id, restaurant_id, order_id, customer_name, customer_phone, total, status, timestamp, is_read) VALUES
('notif-001', 'rest_2', 'ORDER-001', 'Nguyễn Văn A', '0123456789', 130500, 'DELIVERING', 1700000000000, false),
('notif-002', 'restaurant_2', 'ORDER-002', 'Trần Thị B', '0987654321', 188800, 'PREPARING', 1700000002000, false),
('notif-003', 'rest_2', 'ORDER-003', 'Lê Văn C', '0912345678', 130500, 'PENDING', 1700000004000, true);
