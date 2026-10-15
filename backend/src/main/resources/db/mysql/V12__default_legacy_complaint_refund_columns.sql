-- Các cột này chỉ được giữ để tương thích với dữ liệu khiếu nại cũ.
-- Luồng báo cáo sự cố mới không còn cho khách yêu cầu hoàn tiền theo sản phẩm.
ALTER TABLE complaints
    MODIFY COLUMN requested_refund_amount INTEGER NOT NULL DEFAULT 0,
    MODIFY COLUMN approved_refund_amount INTEGER NOT NULL DEFAULT 0,
    MODIFY COLUMN refund_status ENUM('NONE','PENDING','COMPLETED','REJECTED') NOT NULL DEFAULT 'NONE';

ALTER TABLE complaint_history
    MODIFY COLUMN refund_amount INTEGER NOT NULL DEFAULT 0;
