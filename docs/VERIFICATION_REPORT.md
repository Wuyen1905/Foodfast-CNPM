# Báo cáo kiểm chứng ngày 09/10/2026

## Kiểm tra tự động

| Hạng mục | Kết quả |
|---|---|
| Backend JUnit/Mockito/MockMvc E2E | 136 test passed |
| Frontend Vitest | 35 test passed |
| TypeScript | Không có lỗi |
| ESLint | Không có lỗi hoặc cảnh báo |
| Vite production build | Thành công, 1.442 modules |

## Kiểm chứng Sprint trạng thái, lịch sử và thông báo

| Hạng mục | Kết quả thực tế |
|---|---|
| Chuẩn hóa trạng thái | `OrderStatus`, `DeliveryStatus` và `PaymentStatus` được mô tả riêng trong `docs/STATE_MODEL.md` và `docs/BUSINESS_RULES.md` |
| Lịch sử đơn | Mỗi lần đổi trạng thái lưu trạng thái cũ/mới, actor, vai trò, nguồn, lý do và thời điểm; API kiểm tra quyền trước khi trả dữ liệu |
| Trung tâm thông báo | Khách xem số chưa đọc, danh sách lưu trong H2, đánh dấu từng thông báo/tất cả; WebSocket cập nhật realtime |
| Migration | Flyway chạy đủ 10 migration; có bảng `order_status_history` và `customer_notifications` cùng chỉ mục/khóa ngoại |
| E2E đặt món và giao hàng | Đăng nhập khách/nhà hàng, tạo COD, xác nhận, chuẩn bị, sẵn sàng, phân công, bắt đầu giao, khách xác nhận, kiểm tra lịch sử và thông báo |
| Frontend test ổn định | Vitest chạy một worker, không chạy song song file để tránh kết quả phụ thuộc máy |

## Smoke test nghiệp vụ trên H2

| Tình huống | Kết quả thực tế |
|---|---|
| Chuyển đơn In Progress sang Ready | Thành công |
| Gọi API đơn hàng để nhảy Ready sang Delivered | Bị chặn với HTTP 409 |
| Phân công người đúng nhà hàng | Thành công |
| Đơn dùng mã cũ `sweetdreams`, nhân viên dùng mã chuẩn `rest_2` | Chuẩn hóa về `rest_2` và phân công thành công |
| Tìm đơn SweetDreams bằng mã cũ hoặc mã chuẩn | Cả hai cách đều trả đúng đơn |
| Nhà hàng gọi đường hoàn tất giao cũ | Endpoint đã bị loại bỏ, trả HTTP 404 |
| Khách sở hữu đơn xác nhận đã nhận | Delivery và Order cùng chuyển sang Delivered, nguồn xác nhận là CUSTOMER |
| Khách khác xác nhận đơn không thuộc sở hữu | Bị backend từ chối với HTTP 403 |
| Nhà hàng hoàn tất ngoại lệ trước khi hết thời gian chờ | Bị backend từ chối với HTTP 409 |
| Nhà hàng hoàn tất ngoại lệ sau ETA 60 phút | Thành công khi có lý do hợp lệ; lưu nguồn RESTAURANT_OVERRIDE |
| Hoàn tất ngoại lệ với lý do quá ngắn | Bị từ chối |
| Báo giao thất bại có lý do | DeliveryAssignment thành Failed, Order quay về Ready |
| Phân công lại người khác | Thành công |
| Bắt đầu và hoàn tất lần giao thứ hai | DeliveryAssignment và Order đều hoàn tất |
| Chat tra cứu bằng đúng số điện thoại | Trả trạng thái thật của đơn |
| Yêu cầu hoàn tiền | Tạo SupportTicket cho ADMIN |
| Báo thiếu món của đơn đã xác minh | Tạo SupportTicket cho RESTAURANT |
| Hỏi trạng thái bằng tài khoản sở hữu đơn nhưng số điện thoại đã đổi | Cho phép tra cứu bằng `userId`, trả trạng thái tiếng Việt |
| Hỏi thời gian giao của đơn đang giao | Trả ETA và tên nhân viên từ phân công thực tế |
| Đánh dấu phiếu đã xử lý | Trạng thái chuyển sang RESOLVED |
| Web gọi backend qua Vite proxy | HTTP 200, nhận 3 nhà hàng mẫu |
| Thêm Pizza Aloha vào giỏ rồi thêm Donut SweetDreams | Backend từ chối HTTP 409 với mã `CART_RESTAURANT_CONFLICT`; giỏ vẫn chỉ còn món Aloha |
| Gọi API tạo đơn trực tiếp với món của hai nhà hàng | Backend từ chối HTTP 400 với mã `INVALID_ORDER_RESTAURANT` |
| Dữ liệu tên và giá món bị sửa ở request tạo đơn | Backend bỏ qua dữ liệu không đáng tin và dùng tên, giá từ bảng sản phẩm |
| Tạo lại thanh toán sau khi phiên VNPay hết hạn | Giữ nguyên đơn hàng, thay phiên cũ bằng `vnp_TxnRef` mới và tiếp tục ở trạng thái chờ thanh toán |
| Đăng ký khách mới không truyền tên đăng nhập riêng | HTTP 201; backend dùng email đã chuẩn hóa làm định danh |
| API trả dữ liệu tài khoản sau đăng ký | Không trả trường mật khẩu |
| Đăng nhập tài khoản vừa đăng ký bằng email | HTTP 200 |
| Đăng nhập tài khoản vừa đăng ký bằng số điện thoại | HTTP 200 |

## Cảnh báo không chặn phát hành

Vite cảnh báo bundle JavaScript chính khoảng 1,33 MB (gzip khoảng 396 KB) và đề xuất chia nhỏ bằng dynamic import. Cảnh báo này không làm build thất bại, nhưng nên được đưa vào backlog tối ưu hiệu năng.
# Nghiệm thu bổ sung ngày 01/10/2026 — hạng mục 1–5, 9 và 10

## Cổng chất lượng

- Backend: 136/136 JUnit/MockMvc test đạt, không có failure/error/skipped.
- Frontend: 35/35 Vitest test đạt.
- `tsc --noEmit`: đạt.
- ESLint với `--max-warnings 0`: đạt.
- Production build: đạt (1.442 module được build).
- Spring Boot khởi động thành công trên cổng 8080; frontend phản hồi 200 trên cổng 5173.

## Smoke test qua frontend proxy `5173/api`

| Hạng mục | Kết quả quan sát |
|---|---|
| Hủy đơn | Sai chủ đơn bị chặn 403; đúng chủ đơn chuyển Cancelled và lưu actor/reason/detail/time. |
| Tạm dừng nhận đơn | Sai chủ nhà hàng bị chặn 403; checkout khi tạm dừng bị chặn 409 kèm lý do; khôi phục thành công. |
| Giờ mở cửa | Ngoài khung giờ, `openNow=false`; checkout bị chặn 409 và trả đúng giờ phục vụ. |
| Đánh giá | Sai chủ đơn bị chặn 403; đánh giá 5 sao được lưu; lần hai bị chặn 409; rating nhà hàng tính lại. |
| Tùy chọn món | Thiếu kích cỡ bắt buộc bị chặn 400; Pizza cỡ Lớn + phô mai có giá 134.000đ và lưu ghi chú/snapshot. |
| Voucher | `ALOHA30K` trừ đúng 30.000đ; tạm tính 203.000đ + phí 15.000đ + thuế 20.300đ - giảm 30.000đ = 208.300đ. |
| Thay món | Phản hồi qua sai order URL bị chặn 400; đúng chủ đơn chấp nhận thì Hamburger đổi sang Cơm Chiên Hawaii, tổng còn 166.800đ và `partial_refund_pending=22.000đ`. |

Tài liệu demo chi tiết: `docs/FEATURES_1_5_9_10_ACCEPTANCE.md`.

## Nghiệm thu nhóm đơn, giao hàng và đối soát ngày 15/10/2026

| Hạng mục | Kết quả thực tế |
|---|---|
| Nhóm đơn nhiều nhà hàng | Một lần đặt hàng tạo nhóm đơn và các đơn con; mỗi nhà hàng chỉ truy cập đơn thuộc nhà hàng mình. |
| Thanh toán nhóm | Một giao dịch VNPay thanh toán toàn nhóm và phân bổ xuống từng đơn; COD chỉ được ghi nhận khi giao thành công. |
| Từ chối và hoàn tiền | Hỗ trợ một nhà hàng từ chối, hoàn tiền một phần và lưu số tiền hoàn theo từng đơn con. |
| Điều phối giao hàng | Quản trị viên quản lý người giao hàng, tạo một nhiệm vụ cho mỗi đơn con, phân công hoặc đổi người giao; mỗi người chỉ có một nhiệm vụ đang giao. |
| Đối soát nhà hàng | Backend tính tiền món, thuế nhà hàng, giảm giá nhà hàng chịu, hoa hồng, hoàn tiền và điều chỉnh; giao diện không cho nhập tùy ý lợi nhuận. |
| Vòng đời đối soát | Chờ kiểm tra, chờ thanh toán, đã thanh toán; lưu người và thời điểm lập, kiểm tra, thanh toán. |
| Chống đối soát lặp | Mỗi đơn chỉ được gắn vào một chi tiết đối soát; khóa dữ liệu khi lập đợt và ràng buộc duy nhất tại cơ sở dữ liệu. |
| Phân quyền nhân viên quản lý | `quanly` chỉ xem vận hành; `quanly1` được đối soát tài chính; cả hai không truy cập API quản trị viên. |
| Bảo vệ bí mật | Tệp cấu hình MySQL và VNPay cục bộ bị Git bỏ qua; kho mã chỉ chứa tệp ví dụ không có khóa thật. |

### Cổng chất lượng hiện tại

- Backend: 158 kiểm thử, 0 lỗi; 1 kiểm thử MySQL được bỏ qua trong bộ mặc định theo điều kiện môi trường.
- MySQL 8.4: bộ kiểm thử tích hợp riêng đạt; Flyway áp dụng đủ 10 migration và xác minh tài khoản nhân viên quản lý được băm bằng BCrypt.
- Frontend: 37 kiểm thử đạt; kiểm tra kiểu TypeScript đạt; bản dựng production thành công với 1.446 module.
- Kiểm thử nghiệp vụ bao phủ phân tách dữ liệu nhà hàng, phân quyền Manager/Admin, nhóm đơn nhiều nhà hàng, VNPay, COD, hoàn tiền một phần và giới hạn một nhiệm vụ đang giao cho mỗi shipper.
