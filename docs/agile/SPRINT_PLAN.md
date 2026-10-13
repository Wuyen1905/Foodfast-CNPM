# Sprint Plan: Chuyển sang giao hàng nội bộ

## Sprint Goal

Tạo một increment web chạy được cho luồng đặt món, chuẩn bị, phân công nhân viên, giao hàng và hỗ trợ khách mà không phụ thuộc ứng dụng mobile hoặc mô phỏng vị trí.

## Sprint Backlog

| Task | PBI | Kết quả kiểm chứng |
|---|---|---|
| Mô hình RestaurantCourier và DeliveryAssignment | PBI-01, PBI-02 | Entity, repository, service, API và unit test |
| State machine giao hàng và xử lý thất bại | PBI-03 | Test chuyển trạng thái hợp lệ và không hợp lệ |
| Dashboard quản lý và phân công | PBI-01, PBI-02 | Nhà hàng chọn người giao, ETA và ghi chú |
| Trang khách và WebSocket | PBI-04 | Timeline đổi theo sự kiện, không dùng GPS |
| Trợ lý và SupportTicket | PBI-05 | Tra cứu có xác minh, tạo phiếu đúng vai trò |
| Xóa phần cũ và mobile | PBI-06, PBI-07 | Build không phụ thuộc thư mục đã xóa |
| Tự động hóa kiểm thử | PBI-08 | Maven test, Vitest và Vite build |

## Cách làm

- Phân tích quy tắc trước khi viết endpoint.
- Chia increment theo thứ tự backend, dashboard nhà hàng, trang khách, trợ lý, dọn phần cũ.
- Chạy lại test sau mỗi thay đổi có ảnh hưởng tới nghiệp vụ.
- Dùng phản hồi của chủ sản phẩm để đổi phạm vi từ cổng shipper sang nhân viên nội bộ.

Đây là dự án một sinh viên nên Scrum được điều chỉnh: chủ dự án kiêm Product Owner và developer. Không tuyên bố áp dụng pair programming hoặc Daily Scrum của một nhóm khi chưa có bằng chứng.
