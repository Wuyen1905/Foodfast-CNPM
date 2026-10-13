-- Địa chỉ giao hàng là nội dung tự do, không bị giới hạn bởi độ dài VARCHAR cũ.
ALTER TABLE delivery_addresses ALTER COLUMN address_line CHARACTER LARGE OBJECT;
ALTER TABLE orders ALTER COLUMN address CHARACTER LARGE OBJECT;
