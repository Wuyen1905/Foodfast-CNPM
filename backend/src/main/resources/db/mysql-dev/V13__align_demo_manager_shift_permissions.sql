-- Hai tài khoản demo là hai nhân viên cùng vị trí, dùng để thay ca.
-- Cả hai có cùng bộ quyền; nhật ký vẫn ghi riêng tài khoản thực hiện.
UPDATE users
SET name = CASE username
        WHEN 'quanly' THEN 'Nhân viên quản lý ca 1'
        WHEN 'quanly1' THEN 'Nhân viên quản lý ca 2'
        ELSE name
    END,
    manager_permissions = 'ORDER_MANAGEMENT,CUSTOMER_SUPPORT,PAYMENT_RECONCILIATION,RESTAURANT_SETTLEMENT'
WHERE username IN ('quanly', 'quanly1')
  AND role = 'manager';
