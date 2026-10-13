# Sprint: Trạng thái, truy vết và thông báo khách hàng

## Sprint Goal

Làm cho tiến trình đơn hàng nhất quán, truy vết được và chủ động thông báo cho khách; đồng thời tạo cổng kiểm thử lặp lại cho toàn bộ luồng đặt món và giao hàng.

## Sprint Backlog

| Hạng mục | Kết quả bàn giao | Tiêu chí chấp nhận | Trạng thái |
|---|---|---|---|
| Chuẩn hóa ba loại trạng thái | `docs/STATE_MODEL.md`, quy tắc nghiệp vụ và README thống nhất | Phân biệt rõ đơn hàng, phân công giao và thanh toán | Done |
| Lịch sử trạng thái đơn | Bảng lịch sử, service ghi chuyển đổi, API và giao diện chi tiết đơn | Có trạng thái cũ/mới, actor, vai trò, nguồn, lý do, thời điểm và kiểm tra quyền | Done |
| Trung tâm thông báo khách hàng | Thông báo lưu trong DB, số chưa đọc, đọc một/tất cả và realtime | Khách chỉ thấy thông báo của mình; nhấn thông báo mở đúng đơn | Done |
| E2E đặt món và giao hàng | `OrderDeliveryE2ETest` chạy qua HTTP với xác thực thật | Từ đăng nhập/tạo đơn tới giao xong; kiểm tra lịch sử và thông báo | Done |
| Ổn định frontend/test | Test chạy tuần tự, bỏ route/file trùng và placeholder không dùng | TypeScript, Vitest và production build cùng đạt | Done |

## Bằng chứng hoàn thành

- Flyway áp dụng thành công 10 migration trên H2.
- Backend: 136/136 test đạt.
- Frontend: 35/35 test đạt.
- TypeScript và Vite production build đạt.
- Báo cáo chi tiết: `docs/VERIFICATION_REPORT.md`.

## Phần chưa thuộc Sprint

- Tối ưu chia nhỏ bundle frontend lớn hơn 500 kB.
- Thông báo qua email/SMS/push bên ngoài trình duyệt.
- Theo dõi GPS nhân viên giao hàng.

Các mục trên không ảnh hưởng tiêu chí hoàn thành Sprint và được giữ trong backlog để tránh mở rộng phạm vi không kiểm soát.
