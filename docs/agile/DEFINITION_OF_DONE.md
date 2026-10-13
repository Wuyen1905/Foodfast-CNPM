# Definition of Done

Một Product Backlog Item chỉ được đánh dấu Done khi:

- Điều kiện chấp nhận và quy tắc nghiệp vụ liên quan đã rõ.
- Backend từ chối trạng thái hoặc dữ liệu không hợp lệ, không chỉ dựa vào giao diện.
- Không làm lộ dữ liệu nội bộ không cần thiết cho khách hàng.
- Unit test liên quan chạy thành công.
- Frontend test chạy thành công và production build tạo được.
- Luồng cũ không còn được import hoặc gọi từ ứng dụng chính.
- README và tài liệu nghiệp vụ phản ánh đúng hành vi hiện tại.
- Có thể trình bày mục đích của entity, service, controller và quyết định thiết kế khi vấn đáp.
- Increment có thể khởi động bằng script dự án và truy cập trên web.

Các hạng mục cần người dùng thật xác nhận sẽ giữ trạng thái Backlog hoặc In Review, không tự ghi Done.
