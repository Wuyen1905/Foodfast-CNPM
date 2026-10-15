-- Chỉ sửa hai tài khoản demo. Mật khẩu gốc là quanly123 và được lưu bằng BCrypt.
UPDATE users
SET password = '$2a$12$j6O29xAasESTA7LAPPtQGuhumdCvPWj8IS.DSX0PBaBwimFPK4BIa'
WHERE username IN ('quanly', 'quanly1')
  AND role = 'manager';
