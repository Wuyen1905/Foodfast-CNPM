# Sprint Review

## Increment đã có

- Nhà hàng quản lý nhân viên giao hàng nội bộ.
- Đơn Ready được phân công kèm ETA và ghi chú.
- Luồng giao được kiểm soát bởi state machine.
- Khách xem tiến độ realtime mà không cần GPS.
- Trợ lý tra cứu an toàn và tạo phiếu hỗ trợ.
- Sản phẩm chính chỉ còn backend và frontend web.
- Hủy/từ chối đơn có lý do và xử lý chờ hoàn tiền.
- Nhà hàng tạm dừng nhận đơn, cấu hình giờ mở cửa.
- Đánh giá sau giao hàng; tùy chọn món và ghi chú từng món.
- Voucher có điều kiện và luồng đề xuất thay món có khách phê duyệt.
- Ba vòng đời đơn hàng, phân công giao và thanh toán có mô hình trạng thái riêng, không còn trộn ý nghĩa.
- Mọi chuyển trạng thái đơn được lưu lịch sử có actor, nguồn, lý do và thời điểm.
- Khách có trung tâm thông báo lưu bền vững, số chưa đọc và cập nhật realtime.

## Kết quả kỹ thuật

- 136 backend tests và 35 frontend tests chạy thành công.
- TypeScript không còn lỗi.
- Production build thành công.
- MockMvc E2E xác nhận đầy đủ luồng đặt món, bếp, phân công, giao, khách xác nhận, lịch sử và thông báo.
- Smoke test xác nhận luồng thất bại, phân công lại, hoàn tất, định tuyến phiếu hỗ trợ và tương thích mã nhà hàng cũ.
- Chi tiết nằm trong `docs/VERIFICATION_REPORT.md`.

## Tiêu chí demo

1. Khách tạo đơn.
2. Nhà hàng chuyển Pending sang Confirmed, Preparing và Ready.
3. Nhà hàng chọn nhân viên đang hoạt động của đúng cửa hàng.
4. Nhà hàng xác nhận đã bàn giao hoặc báo thất bại có lý do.
5. Khách thấy ETA, trạng thái mới và xác nhận đã nhận đủ món.
6. Backend chỉ chuyển Delivered khi đúng chủ đơn xác nhận.
7. Khách hỏi trạng thái bằng đúng số điện thoại.
8. Yêu cầu hoàn tiền tạo phiếu cho admin; vấn đề món ăn của đơn đã xác minh tạo phiếu cho nhà hàng.
9. Nhà hàng tạm dừng nhận đơn hoặc đóng cửa; khách không thể checkout.
10. Khách chọn kích cỡ/topping, dùng voucher và nhìn thấy đúng phân rã tổng tiền.
11. Nhà hàng đề xuất thay món; chỉ khi khách chấp nhận thì đơn và chênh lệch mới thay đổi.
12. Khách đánh giá một lần sau khi đơn Delivered.
13. Khách mở trung tâm thông báo, đọc thông báo của đúng tài khoản và đi tới đúng đơn.
14. Khách mở lịch sử đơn để xem chuỗi trạng thái và nguồn thao tác.

## Phản hồi cần thu

- ETA hiện tại có đủ cho khách hay cần khoảng thời gian dự kiến?
- Nhà hàng có cần số đơn tối đa trên mỗi nhân viên?
- Admin và nhà hàng cần giao diện hộp thư phiếu hỗ trợ ở mức nào?

Sprint Review chỉ hoàn tất về mặt stakeholder sau khi chủ sản phẩm chạy demo và ghi phản hồi.
