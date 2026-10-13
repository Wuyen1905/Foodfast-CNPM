# Product Backlog

**Product Goal:** Khách đặt món trên web và nhận cập nhật giao hàng đáng tin cậy, trong khi từng nhà hàng tự điều phối nhân viên giao hàng nội bộ.

| Ưu tiên | PBI | User story | Điều kiện chấp nhận | Điểm | Trạng thái |
|---:|---|---|---|---:|---|
| 1 | PBI-01 | Là nhà hàng, tôi muốn quản lý nhân viên giao hàng để chỉ phân công người thuộc cửa hàng mình. | Thêm, sửa, ngừng hoạt động; không ngừng người đang giao. | 5 | Done |
| 2 | PBI-02 | Là nhà hàng, tôi muốn phân công đơn Ready để kiểm soát trách nhiệm giao. | Kiểm tra nhà hàng, trạng thái nhân viên, ETA và một phân công hoạt động. | 8 | Done |
| 3 | PBI-03 | Là nhà hàng, tôi muốn cập nhật tiến độ và báo thất bại để đơn có thể giao lại. | Nhà hàng chuyển Assigned sang Delivering; Failed cần lý do và trả đơn về Ready. | 8 | Done |
| 18 | PBI-18 | Là khách hàng, tôi muốn xác nhận đã nhận đủ món để đơn chỉ hoàn tất khi đúng người nhận đồng ý. | Chỉ chủ đơn có nút xác nhận; backend kiểm tra customerId; lưu completionSource. | 5 | Done |
| 19 | PBI-19 | Là nhà hàng, tôi muốn xử lý khi khách quên xác nhận mà không làm mất khả năng truy vết. | Chỉ mở sau ETA 60 phút; bắt buộc lý do; lưu RESTAURANT_OVERRIDE. | 5 | Done |
| 4 | PBI-04 | Là khách hàng, tôi muốn thấy người giao, ETA và tiến độ theo thời gian thực. | Hiển thị timeline; không GPS; không công khai số điện thoại nhân viên. | 5 | Done |
| 5 | PBI-05 | Là khách hàng, tôi muốn trợ lý tra cứu và chuyển vấn đề tới đúng bộ phận. | Xác minh số điện thoại; tạo mã phiếu; không tự sửa dữ liệu nghiệp vụ. | 8 | Done |
| 6 | PBI-06 | Là nhóm phát triển, tôi muốn chỉ duy trì bản web để giảm phạm vi và lỗi trùng lặp. | Xóa ba dự án mobile; script và README chỉ còn backend và frontend-web. | 3 | Done |
| 7 | PBI-07 | Là nhóm phát triển, tôi muốn xóa mô phỏng cũ để mô hình nghiệp vụ nhất quán. | Không còn model, API, map, simulation hoặc UI của luồng cũ trong ứng dụng chính. | 5 | Done |
| 8 | PBI-08 | Là nhóm phát triển, tôi muốn kiểm thử tự động mỗi thay đổi để phát hiện hồi quy. | Backend test, frontend test và production build chạy trong CI. | 5 | Done |
| 9 | PBI-09 | Là quản trị viên hoặc nhà hàng, tôi muốn xem và giải quyết phiếu hỗ trợ trong giao diện. | Lọc phiếu Open theo đúng bộ phận, xem nội dung, đánh dấu Resolved. | 5 | Done |
| 10 | PBI-10 | Là chủ sản phẩm, tôi muốn kiểm thử chấp nhận với người dùng thật. | Khách và nhà hàng chạy trọn luồng trên dữ liệu thử, ghi lại lỗi và phản hồi. | 3 | Backlog |
| 11 | PBI-11 | Là khách/nhà hàng, tôi muốn hủy hoặc từ chối có lý do. | Kiểm tra quyền và trạng thái; lưu actor/lý do/thời điểm; đơn đã trả tiền chuyển chờ hoàn. | 8 | Done |
| 12 | PBI-12 | Là chủ nhà hàng, tôi muốn tạm dừng nhận đơn khi quá tải. | Chỉ chủ sở hữu thay đổi; khách thấy lý do; backend chặn checkout. | 5 | Done |
| 13 | PBI-13 | Là khách, tôi chỉ muốn đặt trong giờ nhà hàng mở cửa. | Cấu hình HH:mm, hỗ trợ qua nửa đêm, frontend và backend cùng bảo vệ. | 5 | Done |
| 14 | PBI-14 | Là khách đã nhận hàng, tôi muốn đánh giá nhà hàng. | Chỉ chủ đơn Delivered; 1–5 sao; mỗi đơn một lần; cập nhật trung bình. | 5 | Done |
| 15 | PBI-15 | Là khách, tôi muốn chọn kích cỡ/topping và ghi chú món. | Kiểm tra nhóm bắt buộc/giới hạn; backend tính giá; lưu snapshot và biến thể giỏ. | 8 | Done |
| 16 | PBI-16 | Là khách, tôi muốn dùng voucher có điều kiện rõ ràng. | Kiểm tra thời hạn/nhà hàng/đơn tối thiểu/lượt; backend tính giảm; trả lượt khi hủy. | 8 | Done |
| 17 | PBI-17 | Là nhà hàng, tôi muốn đề xuất thay món hết hàng và chờ khách quyết định. | Cùng nhà hàng, không đắt hơn; có hạn; chỉ chủ đơn phản hồi; tính lại và chờ hoàn chênh lệch. | 13 | Done |
| 20 | PBI-20 | Là các bên liên quan, tôi muốn xem lịch sử trạng thái đơn để biết ai đã làm gì và khi nào. | Mọi chuyển `OrderStatus` lưu trạng thái cũ/mới, actor, vai trò, nguồn, lý do và thời điểm; kiểm tra quyền khi xem. | 8 | Done |
| 21 | PBI-21 | Là khách hàng, tôi muốn nhận thông báo bền vững và realtime về đơn của mình. | Có số chưa đọc, đánh dấu đã đọc, liên kết đúng đơn; thông báo trạng thái, thanh toán, phân công và giao thất bại. | 8 | Done |
| 22 | PBI-22 | Là nhóm phát triển, tôi muốn kiểm thử E2E luồng đặt và giao món. | Test đi qua đăng nhập, tạo đơn, bếp, phân công, bắt đầu giao, khách xác nhận, lịch sử và thông báo. | 8 | Done |

Thứ tự ưu tiên dựa trên rủi ro nghiệp vụ. State machine và ràng buộc phân công được làm trước giao diện trang trí.
