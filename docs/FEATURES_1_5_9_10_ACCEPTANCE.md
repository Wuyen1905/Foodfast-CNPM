# Nghiệm thu các hạng mục 1–5, 9 và 10

Tài liệu này liên kết **user story → quy tắc nghiệp vụ → cách demo → kiểm thử**. Đây là bằng chứng phù hợp để trình bày theo Scrum/XP trong môn Công nghệ phần mềm.

## 1. Hủy/từ chối đơn có lý do

**User story:** Là khách hoặc nhà hàng, tôi muốn hủy/từ chối theo đúng giai đoạn và lưu lý do để có thể truy vết tranh chấp.

**Đã làm**

- Popup lý do riêng cho khách và nhà hàng; có nhóm lý do chuẩn và chi tiết tự do.
- Backend kiểm tra chủ đơn/nhà hàng sở hữu, trạng thái hiện tại và cấm hủy trạng thái kết thúc.
- Lưu `actor`, `actorId`, `reasonCode`, `detail`, `cancelledAt`.
- Đơn đã thanh toán chuyển thanh toán sang chờ hoàn tiền; voucher được trả lại lượt.
- Không cho frontend đổi thẳng trạng thái sang `Cancelled` để lách quy tắc.

**Demo:** Mở một đơn chờ xác nhận → bấm Hủy/Từ chối → chọn lý do → xem lý do và người hủy trong chi tiết đơn.

**Test liên kết:** `OrderCancellationServiceTest`.

## 2. Nhà hàng tạm dừng/tiếp tục nhận đơn

**User story:** Là chủ nhà hàng, tôi muốn tạm dừng nhận đơn khi quá tải và cho khách biết lý do.

**Đã làm**

- Tab Vận hành trong dashboard cho phép bật/tắt nhận đơn và nhập lý do.
- Chỉ tài khoản chủ sở hữu mới đổi được trạng thái.
- Menu và thanh toán hiển thị trạng thái; backend chặn tạo đơn ngay cả khi frontend bị can thiệp.

**Demo:** Đăng nhập SweetDreams → Vận hành → tạm dừng với lý do “Quá tải bếp” → thử đặt bánh bằng tài khoản khách → hệ thống từ chối và hiển thị lý do → bật nhận đơn lại.

**Test liên kết:** `RestaurantOperationsServiceTest`.

## 3. Giờ mở cửa/đóng cửa

**User story:** Là khách, tôi chỉ muốn đặt khi nhà hàng thực sự đang hoạt động.

**Đã làm**

- Chủ nhà hàng cấu hình giờ mở/đóng dạng `HH:mm`.
- API nhà hàng trả `openNow`; frontend hiển thị giờ hoạt động và khóa đặt hàng khi đóng cửa.
- Backend kiểm tra lại tại thời điểm tạo đơn và hỗ trợ ca qua nửa đêm.

**Demo:** Đổi giờ hoạt động thành khoảng không chứa giờ hiện tại → trang khách hiển thị Đã đóng cửa và không cho thanh toán → khôi phục `08:00–22:00`.

**Test liên kết:** `RestaurantOperationsServiceTest`.

## 4. Đánh giá sau giao hàng

**User story:** Là khách đã nhận hàng, tôi muốn đánh giá trải nghiệm để phản hồi cho nhà hàng.

**Đã làm**

- Form 1–5 sao và nhận xét xuất hiện trong chi tiết đơn đã giao.
- Chỉ chủ đơn, đúng trạng thái `Delivered`, mỗi đơn một đánh giá.
- Backend tính lại điểm trung bình nhà hàng từ dữ liệu đánh giá.

**Demo:** Hoàn tất đơn → khách mở chi tiết → chọn sao/gửi nhận xét → tải lại vẫn thấy đánh giá → thử gửi lần hai bị từ chối.

**Test liên kết:** `ReviewServiceTest`.

## 5. Kích cỡ, topping và ghi chú từng món

**User story:** Là khách, tôi muốn tùy chọn món và thấy đúng giá trước khi thêm vào giỏ.

**Đã làm**

- Nhóm chọn một/chọn nhiều, bắt buộc, số lượng tối thiểu/tối đa và phần giá cộng.
- Trang chi tiết hiển thị giá tức thời, kiểm tra lựa chọn bắt buộc và cho nhập ghi chú món.
- Hai cấu hình khác nhau của cùng món là hai dòng giỏ hàng.
- Backend tính lại giá và lưu bản chụp tùy chọn trong đơn.

**Dữ liệu demo:** Pizza Hawaii có cỡ Vừa/Lớn và tối đa 2 topping; Hamburger có phô mai; Donut có hương vị.

**Test liên kết:** `ProductOptionPricingServiceTest`, `CartServiceTest`, `OrderRestaurantValidationTest`.

## 9. Mã giảm giá có điều kiện

**User story:** Là khách, tôi muốn nhập voucher và biết ngay mã có hợp lệ, được giảm bao nhiêu.

**Đã làm**

- Hỗ trợ giảm phần trăm/số tiền cố định, đơn tối thiểu, trần giảm, khung thời gian, nhà hàng, tổng lượt và lượt mỗi khách.
- Checkout hiển thị mã, tên ưu đãi và số tiền giảm; backend xác thực lại khi tạo đơn.
- Lượt dùng được giữ theo đơn và giải phóng khi hủy.

**Mã demo**

| Mã | Điều kiện |
|---|---|
| `WELCOME20` | Toàn hệ thống, giảm 20%, đơn từ 80.000đ, tối đa 40.000đ, 1 lần/khách |
| `ALOHA30K` | Aloha, giảm 30.000đ, đơn từ 150.000đ |
| `SWEET10` | SweetDreams, giảm 10%, đơn từ 50.000đ, tối đa 20.000đ |

**Test liên kết:** `VoucherServiceTest`.

## 10. Đề xuất thay món khi hết hàng

**User story:** Là nhà hàng, tôi muốn đề xuất món thay thế; là khách, tôi muốn tự quyết định trước khi đơn bị thay đổi.

**Đã làm**

- Nút Đề xuất thay món chỉ xuất hiện trong các giai đoạn cho phép.
- Danh sách thay thế chỉ có món còn bán, cùng nhà hàng và không đắt hơn; nhà hàng nhập lý do và thời hạn phản hồi.
- Chỉ chủ đơn có thể chấp nhận/từ chối. URL phải chứa đúng mã đơn của đề xuất.
- Chỉ khi chấp nhận mới đổi món và tính lại tiền; hết hạn/từ chối không đổi đơn.
- Nếu đơn VNPay rẻ hơn, chênh lệch được ghi là chờ hoàn một phần; giữ lại tên món gốc để truy vết.

**Demo:** Dùng `ORDER-002` → nhà hàng Aloha đề xuất thay Hamburger bằng món rẻ hơn → khách `user1` chấp nhận → xem tổng tiền và trạng thái chờ hoàn phần chênh lệch.

**Test liên kết:** `SubstitutionServiceTest`.

## Kết quả kiểm tra tự động

| Cổng chất lượng | Kết quả 01/10/2026 |
|---|---:|
| Backend Maven/JUnit | 76/76 test đạt |
| Frontend Vitest | 32/32 test đạt |
| TypeScript | Không có lỗi |
| Production build | Thành công |

## Tài khoản demo

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Khách SweetDreams | `user` | `user123` |
| Khách Aloha | `user1` | `user1123` |
| Nhà hàng SweetDreams | `sweetdreams` | `sweet123` |
| Nhà hàng Aloha | `aloha_restaurant` | `aloha123` |

> Dữ liệu chạy thủ công dùng H2 file nên được giữ lại khi khởi động lại backend; bộ test dùng H2 in-memory biệt lập.
