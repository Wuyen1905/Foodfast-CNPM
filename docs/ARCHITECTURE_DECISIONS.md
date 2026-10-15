# Quyết định kiến trúc và nghiệp vụ

## ADR-01: Quản trị viên điều phối người giao tập trung

**Quyết định:** Không xây cổng shipper. Quản trị viên quản lý hồ sơ, hàng đợi nhiệm vụ và phân công shipper; nhà hàng chỉ chuẩn bị món đến trạng thái sẵn sàng giao.

**Lý do:** Một lần đặt có thể tách thành nhiều đơn con nên điều phối tập trung giúp mỗi đơn có đúng một nhiệm vụ, kiểm soát một shipper chỉ giao một đơn và tránh nhà hàng nhìn hoặc thao tác dữ liệu của nhau.

**Đánh đổi:** Shipper không tự cập nhật trạng thái. Quản trị viên ghi nhận bắt đầu hoặc giao thất bại; khách hàng xác nhận Delivered.

## ADR-02: Không theo dõi GPS

**Quyết định:** Khách theo dõi trạng thái và ETA qua WebSocket.

**Lý do:** GPS cần ứng dụng trên thiết bị người giao, quyền vị trí, bản đồ, bảo mật và xử lý dữ liệu sai lệch. Những phần này nằm ngoài phạm vi web-only.

## ADR-03: Tách vòng đời đơn, giao hàng và thanh toán

**Quyết định:** Order, DeliveryAssignment và paymentStatus là ba phần dữ liệu riêng.

**Lý do:** Đã thanh toán chưa có nghĩa đã giao. Một lần giao thất bại cũng không làm mất lịch sử đơn hoặc thanh toán.

## ADR-04: Trợ lý hỗ trợ an toàn

**Quyết định:** Bản hiện tại dùng bộ phân loại ý định chạy cục bộ, tra cứu có xác minh chủ đơn bằng `userId` (số điện thoại là dự phòng), đọc trạng thái/ETA từ dữ liệu thật và tạo SupportTicket.

**Lý do:** Không gửi nội dung chat, mã đơn hoặc số điện thoại tới dịch vụ AI bên ngoài khi chưa có chính sách riêng tư và sự đồng ý của người dùng. Kiến trúc cho phép thay bộ phân loại bằng mô hình AI sau này mà không trao quyền thay đổi dữ liệu nghiệp vụ cho mô hình.

## ADR-05: Chỉ triển khai web

**Quyết định:** Sản phẩm được hỗ trợ gồm Spring Boot backend và React frontend-web.

**Lý do:** Một giao diện responsive giúp nhóm tập trung vào nghiệp vụ, kiểm thử và chất lượng thay vì duy trì ba ứng dụng mobile trùng chức năng.

## ADR-06: Chuẩn hóa định danh nhà hàng tại biên hệ thống

**Quyết định:** Backend lưu mã chuẩn `rest_2` cho SweetDreams và `restaurant_2` cho Aloha. Bí danh cũ vẫn được chấp nhận khi đọc dữ liệu và so khớp nghiệp vụ.

**Lý do:** Giao diện cũ từng lưu tên rút gọn trong đơn nhưng nhân viên giao hàng dùng khóa chính của nhà hàng. Chuẩn hóa khi ghi, đồng thời hỗ trợ bí danh khi đọc, ngăn lỗi từ dữ liệu lịch sử mà không đẩy mã kỹ thuật ra cho người dùng.

## ADR-07: Tách bên thực hiện và bên xác nhận giao thành công

**Quyết định:** Nhà hàng không được tự chuyển đơn đang giao sang Delivered. Nút hoàn tất thuộc trang đơn của khách hàng và backend kiểm tra `customerId` sở hữu đơn trước khi chấp nhận.

**Lý do:** Nhà hàng là bên điều phối nên không nên đồng thời tự xác nhận kết quả cuối. Cơ chế hai phía giúp giảm thao tác nhầm, tạo dấu vết `completionSource=CUSTOMER` và hợp lý hơn khi xử lý khiếu nại.

**Ngoại lệ:** Nếu khách quên thao tác, nhà hàng chỉ được hoàn tất sau ETA 60 phút, phải nhập lý do và hệ thống lưu `completionSource=RESTAURANT_OVERRIDE`. Không tự động hoàn tất theo thời gian vì thời gian trôi qua không chứng minh khách đã nhận món.

## ADR-08: Giỏ nhiều nhà hàng, nhóm đặt hàng và đơn con

**Quyết định:** Giỏ được chứa món từ nhiều nhà hàng. Khi xác nhận, backend tạo một nhóm đặt hàng và tách đúng một đơn con cho mỗi nhà hàng.

**Lý do:** Đơn con giữ phí, giảm giá, thuế, trạng thái, nhiệm vụ giao và khoản đối soát riêng. Nhờ vậy từng nhà hàng chỉ xử lý phần của mình nhưng khách vẫn thanh toán và theo dõi theo một nhóm.

**Bảo vệ phía máy chủ:** Backend tự lấy tên, giá, tình trạng còn bán và nhà hàng từ bảng sản phẩm, sau đó tự nhóm sản phẩm. Một giao dịch VNPay có thể trả cả nhóm; IPN hợp lệ mới phân bổ số tiền xuống các đơn con.

## ADR-09: Đối soát nhà hàng bằng bản chụp bất biến

**Quyết định:** Khi nhân viên quản lý lập đợt đối soát, backend chụp các thành phần tiền của từng đơn vào bảng chi tiết và gắn ràng buộc duy nhất trên mã đơn.

**Lý do:** Giá, voucher, tỷ lệ hoa hồng hoặc khoản hoàn có thể thay đổi về sau. Bản chụp giúp giải thích số tiền đã trả và ràng buộc duy nhất ngăn một giao dịch được đối soát hai lần.

**Quyền:** Nhân viên có quyền quyết toán được lập và kiểm tra đợt; nhân viên có quyền đối soát thanh toán mới được đánh dấu đã trả. Vai trò Manager không truy cập API Admin.
