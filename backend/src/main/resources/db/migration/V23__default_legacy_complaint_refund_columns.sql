-- Các cột này chỉ được giữ để tương thích với dữ liệu khiếu nại cũ.
-- Luồng báo cáo sự cố mới không còn cho khách yêu cầu hoàn tiền theo sản phẩm.
ALTER TABLE complaints ALTER COLUMN requested_refund_amount SET DEFAULT 0;
ALTER TABLE complaints ALTER COLUMN approved_refund_amount SET DEFAULT 0;
ALTER TABLE complaints ALTER COLUMN refund_status SET DEFAULT 'NONE';
ALTER TABLE complaint_history ALTER COLUMN refund_amount SET DEFAULT 0;
