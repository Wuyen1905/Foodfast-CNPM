# Retrospective

## Điều làm tốt

- Thay đổi phạm vi được chuyển thành quy tắc cụ thể trước khi xóa code cũ.
- Test phát hiện lỗi chuẩn hóa ký tự tiếng Việt trong bộ phân loại ý định.
- State machine gom luật chuyển trạng thái vào một nơi dễ kiểm tra.
- Tách DeliveryAssignment khỏi Order giữ được lịch sử giao lại.

## Điều cần cải thiện

- Trước increment này, backlog, điều kiện chấp nhận và Definition of Done chưa được lưu trong repository.
- Một số màn hình cũ có logic đổi trạng thái riêng, làm tăng nguy cơ không nhất quán.
- Dự án một người chưa có code review độc lập hoặc pair programming.
- Kiểm thử UI còn ít hơn kiểm thử service và utility.

## Hành động cho sprint tiếp theo

- Thêm thông báo realtime và số lượng phiếu chưa xử lý trên menu.
- Viết kiểm thử tích hợp cho toàn luồng đặt món đến Delivered.
- Nhờ một thành viên hoặc giảng viên review pull request để tạo bằng chứng review độc lập.
- Ghi acceptance test và kết quả demo ngay trong Sprint Review.
