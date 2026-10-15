# Tiêu chí nghiệm thu luồng 11, 12 và 14

## Luồng 11 — Backend quyết định toàn bộ giá

**User story:** Là khách hàng, tôi muốn mọi màn hình nhìn thấy cùng một tổng tiền đáng tin cậy để không bị thay đổi giá giữa giỏ hàng, checkout và đơn đã tạo.

### Luồng chính

1. Web gửi `productId`, số lượng, mã tùy chọn và voucher tới `/api/orders/quote`.
2. Backend tra giá món/topping còn hiệu lực, xác minh một nhà hàng, tính tạm tính, phí giao, thuế, giảm giá và tổng.
3. Giỏ hàng và checkout hiển thị đúng response báo giá.
4. Khi tạo đơn, backend chạy lại cùng quy tắc và lưu bản chụp từng thành phần tiền.
5. Mọi màn hình sau đặt hàng đọc số đã lưu, không tự tính lại.

### Trường hợp chấp nhận

- Sửa `price` hoặc `name` trong request không làm thay đổi giá/tên lưu trong đơn.
- Topping hết bán hoặc không thuộc món bị từ chối.
- Voucher sai nhà hàng/hết hạn/vượt lượt bị từ chối.
- Tổng hiển thị ở checkout bằng tổng lưu trong đơn và bằng số tiền gửi VNPay.

## Luồng 12 — Chống tạo đơn và thanh toán trùng

**User story:** Là khách hàng, khi mạng chậm hoặc bấm nút nhiều lần, tôi chỉ muốn có một đơn và một phiên thanh toán.

### Luồng chính

1. Web sinh một `Idempotency-Key` khi bắt đầu lần đặt hàng.
2. Các lần retry giữ nguyên khóa và nội dung.
3. Backend lưu khóa duy nhất cùng SHA-256 của nội dung nghiệp vụ.
4. Request đầu tạo đơn và trả `201`; request lặp trả chính đơn đó với `200` và header `Idempotent-Replayed: true`.
5. Tạo VNPay dùng khóa con riêng. Cùng khóa trả cùng URL; phiên hết hạn dùng khóa mới nhưng giữ nguyên order.
6. Callback VNPay dùng khóa/pessimistic lock của phiên; callback lặp chỉ trả kết quả đã xác nhận.

### Trường hợp chấp nhận

- Double click không tạo hai order.
- Retry do timeout trả đúng mã order ban đầu.
- Cùng khóa nhưng đổi giỏ hàng trả `409`.
- Callback thành công hai lần chỉ phát một cập nhật/thông báo.
- Số tiền callback khác tổng lưu bị từ chối.

## Luồng 14 — Khiếu nại và hoàn tiền có truy vết

**User story:** Là khách hàng, tôi muốn báo thiếu món/sai món/món hỏng/giao trễ và theo dõi quyết định hoàn tiền minh bạch.

### Trạng thái

`OPEN → REVIEWING → RESOLVED` hoặc `OPEN/REVIEWING → REJECTED`.

Hoàn tiền độc lập: `NONE → PENDING → COMPLETED` hoặc `REJECTED`.

### Trách nhiệm

| Vai trò | Quyền |
|---|---|
| Khách hàng | Tạo khiếu nại cho đơn của mình, gửi mô tả/bằng chứng/số tiền đề nghị, xem lịch sử. |
| Nhà hàng | Chỉ xem đơn của mình, bắt đầu xử lý, duyệt số tiền hoặc từ chối kèm kết luận. |
| Quản trị viên | Xem toàn hệ thống, xử lý và xác nhận giao dịch hoàn tiền. |

### Trường hợp chấp nhận

- Khách không thể khiếu nại đơn người khác.
- Sai món/món hỏng thiếu bằng chứng bị từ chối.
- Không thể mở đồng thời hai khiếu nại cho một đơn.
- Số tiền duyệt không vượt tổng đơn.
- Hoàn một phần đặt `partial_refund_pending`, hoàn toàn bộ đặt `refund_pending`.
- Sau khi quản trị nhập mã giao dịch, trạng thái thành `partial_refunded` hoặc `refunded`.
- Nhật ký thể hiện đủ ai, làm gì, lúc nào, từ trạng thái nào sang trạng thái nào và số tiền.

## Kết quả kiểm thử tự động ngày 01/10/2026

- Backend: 83 test, 0 lỗi.
- Frontend: 32 test, 0 lỗi.
- TypeScript: biên dịch kiểu thành công.
