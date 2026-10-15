# Luồng liên hệ nhân viên hỗ trợ

## Mục tiêu nghiệp vụ

Trợ lý AI trả lời câu hỏi đơn giản nhưng không tự hủy đơn, sửa thanh toán, hoàn tiền hoặc thay đổi trạng thái đơn. Khi cần con người kiểm tra, khách chủ động tạo một phiếu và tiếp tục trao đổi trong chính phiếu đó.

## Định tuyến tự động

| Vấn đề khách chọn | Nơi tiếp nhận | Có bắt buộc chọn đơn |
| --- | --- | --- |
| Thanh toán hoặc hoàn tiền | Quản trị viên FoodFast | Không |
| Tài khoản và đăng nhập | Quản trị viên FoodFast | Không |
| Thay đổi hoặc hủy đơn | Nhà hàng của đơn | Có |
| Món ăn thiếu, sai hoặc có vấn đề | Nhà hàng của đơn | Có |
| Giao hàng | Nhà hàng của đơn | Có |
| Vấn đề khác | Quản trị viên FoodFast | Không |

Frontend không gửi `restaurantId`, `customerId` hoặc vai trò người xử lý. Backend lấy các thông tin này từ JWT và đơn hàng đã xác minh để tránh giả mạo.

## Vòng đời phiếu

`OPEN` (đang chờ tiếp nhận) → `IN_PROGRESS` (nhân viên đã phản hồi) → `RESOLVED` (đã hoàn tất).

- Tin nhắn đầu tiên và ngữ cảnh trao đổi với trợ lý được lưu cùng phiếu.
- Phản hồi đầu tiên từ nhà hàng hoặc quản trị viên tự chuyển phiếu sang `IN_PROGRESS`.
- Phiếu `RESOLVED` chỉ được xem lịch sử, không thể gửi thêm tin nhắn.
- Khách chỉ xem được phiếu của mình; nhà hàng chỉ xem phiếu đúng nhà hàng; quản trị viên xem phiếu cấp hệ thống.
- Giao diện tự làm mới hội thoại mỗi 5 giây, đủ nhẹ và dễ giải thích cho phạm vi đồ án web.

## Tiêu chí nghiệm thu

- Khách chọn loại vấn đề, đơn liên quan và nhập mô tả tối thiểu 10 ký tự.
- Phiếu món ăn/giao hàng không thể tạo nếu thiếu đơn hợp lệ thuộc khách đang đăng nhập.
- Phiếu xuất hiện trong “Phiếu của tôi” và đúng hộp thư nhà hàng hoặc quản trị viên.
- Hai bên gửi và nhận được tin nhắn theo thứ tự thời gian.
- Khách khác nhận HTTP 403 khi cố truy cập cuộc trao đổi không thuộc mình.
- Khi nhân viên hoàn tất, khách vẫn xem được nội dung và kết quả nhưng không thể nhắn thêm.
