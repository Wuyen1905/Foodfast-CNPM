# Nhật ký kiểm tra tiến độ

Dự án có một developer nên tài liệu này là bản điều chỉnh của Daily Scrum, không phải biên bản họp nhóm.

## 29/09/2026

**Đã hoàn thành**

- Chốt thay đổi phạm vi: nhà hàng tự quản lý người giao, không cổng shipper, không GPS, web-only.
- Xây mô hình giao hàng, dashboard phân công, cập nhật khách qua WebSocket.
- Thay luồng cũ, xóa ba dự án mobile và sửa script chạy.
- Thêm trợ lý an toàn, xác minh đơn và lưu phiếu hỗ trợ.
- Thêm state machine, tách thanh toán khỏi giao hàng và bổ sung test.

**Vướng mắc**

- Code cũ đồng nhất thanh toán thành công với Delivered.
- PATCH nhân viên có thể vô tình kích hoạt lại nhân viên.
- README và file môi trường dùng UTF-16, cần chuyển mã hóa trước khi cập nhật.

**Tiếp theo**

- Chạy toàn bộ test và production build.
- Chạy kiểm thử API trọn luồng trên backend mới.
- Demo bản web và thu phản hồi chấp nhận từ chủ sản phẩm.

## Mẫu cho lần làm việc sau

- Đã làm từ lần trước:
- Vướng mắc:
- Việc sẽ làm:
- PBI liên quan:
