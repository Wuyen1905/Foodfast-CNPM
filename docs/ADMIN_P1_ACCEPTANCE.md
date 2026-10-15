# FoodFast Admin P1 — Tiêu chí nghiệm thu

## 1. Trung tâm vận hành

- Hiển thị đơn hôm nay và doanh thu hôm nay/tuần/tháng theo múi giờ `Asia/Ho_Chi_Minh`.
- Hiển thị các việc cần xử lý: giao thất bại, thanh toán cần đối soát, khiếu nại mở, hoàn tiền chờ xử lý, nhà hàng chờ duyệt và yêu cầu hỗ trợ quá hạn.
- Mỗi thẻ điều hướng tới danh sách tương ứng đã được lọc; danh sách đối soát và giao thất bại dùng chính các mã đơn backend trả về.
- Doanh thu dùng `RevenuePolicy`, không cộng đơn hủy, đơn chưa đủ điều kiện ghi nhận hoặc khoản đã hoàn.

## 2. Vòng đời nhà hàng

Luồng hợp lệ:

- `PENDING -> ACTIVE | CLOSED`
- `ACTIVE -> SUSPENDED | CLOSED`
- `SUSPENDED -> ACTIVE | CLOSED`
- `CLOSED -> PENDING`

Mọi lần chuyển trạng thái phải có lý do tối thiểu 5 ký tự và được ghi vào nhật ký quản trị. Nhà hàng không ở trạng thái `ACTIVE` sẽ ngừng nhận đơn.

## 3. Hồ sơ khách hàng

- Danh sách có trạng thái tài khoản, lần đăng nhập gần nhất và lý do khóa gần nhất.
- Hồ sơ chi tiết gồm tài khoản, đơn hàng, thanh toán, khiếu nại và sổ địa chỉ.
- Tạm ngưng/kích hoạt lại bắt buộc có lý do và lưu thời điểm thay đổi.

## 4. Hàng đợi hỗ trợ và hoàn tiền

- Gom phiếu hỗ trợ cấp hệ thống và khiếu nại vào một hàng đợi.
- Sắp xếp ưu tiên theo: quá hạn xử lý, mức ưu tiên, thời gian tạo.
- Phiếu hỗ trợ có bộ phận, mức ưu tiên, người phụ trách, hạn xử lý, trạng thái và lịch sử.
- Hạn xử lý mặc định: yêu cầu hoàn tiền/tài chính hoặc khẩn cấp 4 giờ, khiếu nại 8 giờ, hỗ trợ thông thường 24 giờ.
- Hoàn tất phiếu bắt buộc nhập kết quả tối thiểu 5 ký tự.
- Khiếu nại/hoàn tiền vẫn dùng quy trình nghiệp vụ chuyên biệt và được liên kết từ hàng đợi chung.

## 5. Kiểm tra đã thực hiện

- Flyway V6 chạy thành công trên H2 trống và cơ sở dữ liệu đang dùng.
- Backend đóng gói thành công.
- Frontend kiểm tra kiểu và build production thành công.
- 32 kiểm thử frontend đạt.
- Kiểm thử mô hình vòng đời nhà hàng và hạn xử lý phiếu hỗ trợ đạt.
- Đăng nhập admin và các API P1 đã được kiểm tra trên server local.
