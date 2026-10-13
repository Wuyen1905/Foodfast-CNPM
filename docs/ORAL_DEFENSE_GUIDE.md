# Gợi ý vấn đáp code

## Vì sao không làm cổng shipper?

Mỗi nhà hàng tự điều người giao. Nếu thêm tài khoản shipper, hệ thống phải có đăng ký, đăng nhập, phân quyền, nhận hoặc từ chối đơn và bảo mật vị trí. Phạm vi đó lớn hơn giá trị cần chứng minh. Vì vậy RestaurantCourier là dữ liệu thuộc nhà hàng, còn nhân viên nhà hàng thao tác trên dashboard.

## Vì sao có DeliveryAssignment riêng?

Order thể hiện vòng đời đặt và chuẩn bị món. DeliveryAssignment thể hiện ai được giao, ETA và từng mốc giao. Tách hai entity giúp lưu được lần giao thất bại và phân công lại mà không làm sai lịch sử đơn.

## State machine bảo vệ gì?

Backend chỉ cho phép các bước liền kề. Giao diện không phải lớp bảo vệ duy nhất. API không thể chuyển Pending thẳng sang Delivered; nhà hàng chỉ xác nhận bàn giao, còn backend chỉ nhận Delivered từ khách hàng sở hữu đơn qua DeliveryAssignmentService.

## Vì sao thanh toán tách khỏi giao hàng?

Thanh toán thành công chỉ chứng minh giao dịch tiền đã hoàn tất. Nhà hàng vẫn phải chuẩn bị và giao món. Vì vậy paymentStatus không được đổi OrderStatus thành Delivered.

## Realtime hoạt động thế nào?

Backend phát sự kiện đơn và phân công qua WebSocket. Khách nhận trạng thái và ETA. Không có tọa độ, bản đồ hoặc mô phỏng đường đi.

## Trợ lý có phải AI không?

Bản hiện tại là trợ lý phân loại ý định chạy cục bộ. Nó giải quyết FAQ, tra cứu đơn có xác minh và tạo SupportTicket. Nó không có quyền ghi vào Order hoặc Payment. Có thể thay bộ phân loại bằng mô hình ngôn ngữ sau khi có chính sách dữ liệu, nhưng lớp service vẫn giữ giới hạn quyền.

## Agile được áp dụng ra sao?

- Product Owner thay đổi ưu tiên từ mô phỏng sang nghiệp vụ thật.
- Product Backlog và Sprint Backlog ghi user story, acceptance criteria và độ ưu tiên.
- Increment được làm và kiểm tra theo từng lớp.
- JUnit, Vitest và CI cung cấp test automation.
- Refactoring loại bỏ phần cũ và tách các trách nhiệm.
- Dự án một người nên không tuyên bố pair programming hay Scrum đầy đủ.
