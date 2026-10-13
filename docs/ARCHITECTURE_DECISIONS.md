# Quyết định kiến trúc và nghiệp vụ

## ADR-01: Nhà hàng tự điều phối người giao

**Quyết định:** Không xây cổng shipper. Nhà hàng quản lý hồ sơ nhân viên và thao tác giao hàng trong dashboard hiện có.

**Lý do:** Quy mô đồ án nhỏ, mỗi cửa hàng đã có người giao nội bộ. Một cổng riêng sẽ kéo theo đăng ký, xác thực, phân quyền, nhận đơn và xử lý tranh chấp nhưng không tạo thêm giá trị cho luồng cốt lõi.

**Đánh đổi:** Nhân viên giao hàng không tự cập nhật trạng thái. Nhà hàng chịu trách nhiệm phân công, xác nhận bàn giao và báo Failed; khách hàng xác nhận Delivered.

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

## ADR-08: Một nhà hàng cho mỗi giỏ hàng và đơn hàng

**Quyết định:** Mỗi khách hàng có giỏ riêng và một giỏ chỉ chứa món của một nhà hàng. Khi chọn món thuộc nhà hàng khác, giao diện hỏi khách có muốn xóa giỏ hiện tại để chuyển nhà hàng hay không.

**Lý do:** Mỗi nhà hàng tự xác nhận, chuẩn bị và điều phối người giao. Nếu một đơn chứa nhiều nhà hàng thì không thể xác định bên chịu trách nhiệm, phí giao hàng, ETA và trạng thái xử lý chung một cách nhất quán.

**Bảo vệ phía máy chủ:** Backend tự lấy tên, giá, tình trạng còn bán và nhà hàng từ bảng sản phẩm. API từ chối HTTP 400 nếu các `productId` thuộc nhiều nhà hàng và trả HTTP 409 nếu khách cố thêm món của nhà hàng khác vào giỏ hiện tại.
