# Quy tắc nghiệp vụ FoodFast

## Vai trò và phạm vi

- Khách hàng đặt món, thanh toán và theo dõi đơn trên web.
- Nhà hàng xác nhận, chuẩn bị đơn, quản lý nhân viên giao hàng và cập nhật tiến độ giao.
- Nhân viên giao hàng là nhân sự nội bộ của nhà hàng. Họ không có tài khoản hoặc cổng đăng nhập riêng.
- Quản trị viên xử lý tài khoản, thanh toán và các phiếu hỗ trợ cấp hệ thống.

## Quy tắc đơn hàng

| Mã | Quy tắc |
|---|---|
| BR-01 | Luồng chuẩn là Pending, Confirmed, Preparing, Ready, Delivering, Delivered. |
| BR-02 | Đơn có thể bị hủy trước khi bắt đầu giao. Delivered và Cancelled là trạng thái kết thúc. |
| BR-03 | Không được bỏ qua bước, ví dụ Pending không thể chuyển thẳng sang Ready hoặc Delivered. |
| BR-04 | Nhà hàng xác nhận đã bàn giao để chuyển Ready sang Delivering. Chỉ khách hàng sở hữu đơn xác nhận đã nhận hàng để chuyển Delivering sang Delivered. |
| BR-05 | Trạng thái thanh toán độc lập với trạng thái giao hàng. Thanh toán thành công không đồng nghĩa đã giao. |
| BR-22 | Mỗi giỏ hàng và mỗi đơn chỉ được chứa món của đúng một nhà hàng. Khi khách chọn món ở nhà hàng khác, hệ thống phải yêu cầu xác nhận xóa giỏ hiện tại trước khi chuyển nhà hàng. |
| BR-23 | Giỏ hàng thuộc về một khách hàng cụ thể. Khi tạo đơn, backend tự tra sản phẩm để xác minh nhà hàng, tên, giá và trạng thái còn bán; không tin các giá trị này từ trình duyệt. |
| BR-24 | Khi phiên VNPay hết hạn hoặc thất bại, khách được thanh toán lại trên cùng đơn đang chờ. Hệ thống phải tạo `vnp_TxnRef` mới, vô hiệu hóa phiên cũ và không tạo đơn hàng trùng. |
| BR-28 | Mọi lần hủy đơn phải ghi người thực hiện, vai trò, mã lý do, chi tiết và thời điểm. Không cho phép hủy đơn đã giao hoặc đã hủy. |
| BR-29 | Khách chỉ hủy đơn của chính mình; nhà hàng chỉ từ chối/hủy đơn thuộc nhà hàng mình. Nếu đơn đã thanh toán, trạng thái thanh toán chuyển sang chờ hoàn tiền. |
| BR-30 | Tổng tiền đơn bằng tạm tính món + phí giao + thuế - giảm giá. Mọi thành phần do backend tính và lưu riêng để đối soát. |
| BR-46 | Giỏ hàng và checkout chỉ hiển thị báo giá do backend trả về; tên món, giá gốc, giá tùy chọn, thuế, phí giao và voucher từ trình duyệt không được dùng để quyết định số tiền. |
| BR-47 | Mỗi lần khách bắt đầu đặt đơn có một `Idempotency-Key`. Gửi lặp cùng khóa và cùng nội dung phải trả về đúng đơn cũ; cùng khóa nhưng nội dung khác phải bị từ chối. |
| BR-48 | Mỗi lần tạo phiên VNPay có khóa chống trùng riêng. Cùng khóa phải trả đúng URL/`vnp_TxnRef` cũ; muốn thanh toán lại phiên hết hạn phải tạo khóa mới trên cùng đơn. |
| BR-49 | Callback VNPay được đối soát chữ ký, website, mã phiên và số tiền từ đơn đã lưu; callback lặp không được ghi nhận thanh toán hoặc gửi thông báo lần hai. |
| BR-55 | `OrderStatus`, `DeliveryStatus` và `PaymentStatus` là ba vòng đời độc lập. Không dùng trạng thái thanh toán để suy ra trạng thái chế biến/giao hàng và không dùng trạng thái phân công để thay thế trạng thái đơn. |
| BR-56 | Mọi lần đổi `OrderStatus` phải lưu trạng thái cũ/mới, người thực hiện, vai trò, nguồn thao tác, lý do và thời điểm. Lịch sử chỉ được xem bởi chủ đơn, nhà hàng sở hữu đơn hoặc quản trị viên. |
| BR-57 | Thông báo của khách phải được lưu bền vững, chỉ chủ tài khoản được đọc/đánh dấu đã đọc và có liên kết tới đúng đơn. WebSocket chỉ giúp cập nhật realtime, không thay thế dữ liệu đã lưu. |

## Quy tắc tài khoản

| Mã | Quy tắc |
|---|---|
| BR-25 | Khách đăng ký bằng họ tên, email, số điện thoại Việt Nam và mật khẩu. Email và số điện thoại phải duy nhất sau khi chuẩn hóa; mật khẩu không bao giờ được trả về trong API. |
| BR-26 | Sau khi đăng ký, khách có thể đăng nhập bằng email hoặc số điện thoại đã đăng ký. Email không phân biệt chữ hoa/thường; số điện thoại `+84`, `84` và đầu `0` được chuẩn hóa về cùng một định danh. |
| BR-27 | Địa chỉ giao hàng không thuộc hồ sơ đăng ký bắt buộc. Khách cung cấp/chọn địa chỉ tại bước thanh toán để một tài khoản có thể sử dụng nhiều địa chỉ. |
| BR-45 | Khi khách đã đăng nhập, tên và email của đơn được lấy từ tài khoản; bước xác nhận chỉ yêu cầu số điện thoại, địa chỉ giao hàng, ghi chú và phương thức thanh toán. |

## Quy tắc vận hành nhà hàng

| Mã | Quy tắc |
|---|---|
| BR-31 | Nhà hàng chỉ nhận đơn khi đang hoạt động, đang bật nhận đơn và thời điểm đặt nằm trong giờ mở cửa. Backend kiểm tra lại khi tạo đơn. |
| BR-32 | Chủ nhà hàng mới được tạm dừng/tiếp tục nhận đơn và đổi giờ hoạt động. Khi tạm dừng phải có lý do hiển thị cho khách. |
| BR-33 | Giờ mở/đóng dùng định dạng `HH:mm`; hệ thống hỗ trợ ca qua nửa đêm và không cho hai mốc giờ trùng nhau. |

## Quy tắc tùy chọn món và giảm giá

| Mã | Quy tắc |
|---|---|
| BR-34 | Nhóm tùy chọn có thể chọn một hoặc nhiều, bắt buộc hoặc không; số lượng lựa chọn phải nằm trong giới hạn của nhóm. |
| BR-35 | Backend tự tra mã tùy chọn, trạng thái còn bán và phần giá cộng; không tin giá hoặc tên tùy chọn do trình duyệt gửi. |
| BR-36 | Cùng một món nhưng khác tùy chọn/ghi chú là hai dòng giỏ hàng khác nhau. Khi tạo đơn phải lưu bản chụp tùy chọn và giá. |
| BR-37 | Voucher chỉ hợp lệ khi đang hoạt động, trong thời hạn, đúng nhà hàng, đủ giá trị tối thiểu và chưa vượt giới hạn tổng/mỗi khách. |
| BR-38 | Backend tính mức giảm theo phần trăm hoặc số tiền cố định, áp trần giảm nếu có và không để giảm vượt tạm tính. Lượt voucher được giải phóng khi đơn bị hủy. |

## Quy tắc thay món hết hàng

| Mã | Quy tắc |
|---|---|
| BR-39 | Nhà hàng chỉ đề xuất thay món cho đơn của mình ở giai đoạn chờ xác nhận, đã xác nhận hoặc đang chuẩn bị; mỗi dòng món chỉ có một đề xuất đang chờ. |
| BR-40 | Món thay thế phải còn bán, thuộc cùng nhà hàng, khác món cũ và không đắt hơn. Nhà hàng phải nhập lý do. |
| BR-41 | Chỉ chủ đơn được chấp nhận/từ chối đề xuất của đúng đơn, trong thời hạn. Từ chối hoặc hết hạn không đổi món. |
| BR-42 | Chỉ sau khi khách chấp nhận, hệ thống mới thay bản chụp món và tính lại tổng. Đơn VNPay rẻ hơn chuyển phần chênh lệch sang `partial_refund_pending`. |

## Quy tắc đánh giá

| Mã | Quy tắc |
|---|---|
| BR-43 | Chỉ chủ đơn đã giao mới được đánh giá; mỗi đơn chỉ có một đánh giá từ 1 đến 5 sao. |
| BR-44 | Điểm nhà hàng được tính lại từ các đánh giá hợp lệ, không nhận giá trị trung bình do frontend gửi. |

## Quy tắc giao hàng

| Mã | Quy tắc |
|---|---|
| BR-06 | Mỗi nhân viên giao hàng thuộc đúng một nhà hàng. |
| BR-07 | Chỉ nhân viên đang hoạt động và thuộc nhà hàng của đơn mới được phân công. |
| BR-08 | Chỉ đơn Ready mới được phân công. Mỗi đơn chỉ có một phân công đang hoạt động. |
| BR-09 | Luồng phân công là Assigned, Delivering, Delivered; nguồn xác nhận hoàn tất phải được lưu để truy vết. |
| BR-10 | Giao thất bại phải có lý do. Đơn quay về Ready để nhà hàng phân công lại. |
| BR-11 | Không thể ngừng hoạt động một nhân viên đang có đơn Assigned hoặc Delivering. |
| BR-12 | ETA phải nằm trong tương lai. Giao diện giới hạn từ 5 đến 240 phút. |
| BR-13 | Khách chỉ xem tên, phương tiện, biển số, ETA và trạng thái. Hệ thống không theo dõi GPS và không công khai số điện thoại nội bộ. |
| BR-19 | Đơn hàng và nhân viên giao hàng phải dùng mã nhà hàng chuẩn. Backend tự chuyển các bí danh cũ (`sweetdreams`, `aloha`) sang mã chuẩn và không yêu cầu người dùng xử lý mã kỹ thuật. |
| BR-20 | Nếu khách quên xác nhận, nhà hàng chỉ được hoàn tất ngoại lệ sau ETA 60 phút, phải nhập lý do tối thiểu 10 ký tự và hệ thống lưu nguồn `RESTAURANT_OVERRIDE`. |

## Quy tắc trợ lý hỗ trợ

| Mã | Quy tắc |
|---|---|
| BR-14 | Trợ lý chỉ tư vấn, tra cứu và định tuyến. Trợ lý không hủy đơn, hoàn tiền, sửa tài khoản hoặc đổi trạng thái. |
| BR-15 | Tra cứu đơn cần đúng mã đơn và phải xác minh chủ đơn: ưu tiên `userId` của tài khoản đăng nhập, số điện thoại đặt hàng chỉ là phương án dự phòng. |
| BR-16 | Vấn đề món ăn hoặc thay đổi đơn được chuyển thành phiếu cho đúng nhà hàng khi khách đã xác minh đơn. |
| BR-17 | Vấn đề thanh toán, hoàn tiền hoặc tài khoản được chuyển thành phiếu cho quản trị viên. |
| BR-18 | Mỗi lần chuyển hỗ trợ tạo một mã phiếu có thể theo dõi và đánh dấu đã giải quyết. |
| BR-21 | Câu hỏi trạng thái và ETA phải đọc dữ liệu Order/DeliveryAssignment thực tế, không trả thông tin giả định. |

## Quy tắc khiếu nại và hoàn tiền

| Mã | Quy tắc |
|---|---|
| BR-50 | Chỉ chủ đơn được tạo khiếu nại. Lỗi thiếu/sai/hỏng món chỉ báo sau khi đã giao; giao trễ được báo khi đang giao hoặc đã giao. |
| BR-51 | Mỗi đơn chỉ có một khiếu nại ở trạng thái Mới tiếp nhận hoặc Đang xử lý. Sai món và món hỏng bắt buộc có ít nhất một bằng chứng. |
| BR-52 | Nhà hàng chỉ xử lý khiếu nại thuộc nhà hàng mình; quản trị viên được xử lý toàn hệ thống. Duyệt hoặc từ chối phải có kết luận tối thiểu 10 ký tự. |
| BR-53 | Số tiền hoàn không vượt tổng đơn. Quyết định hoàn tiền chuyển sang chờ hoàn; chỉ quản trị viên được xác nhận đã hoàn bằng mã giao dịch. |
| BR-54 | Mọi thao tác tạo, bắt đầu xử lý, duyệt, từ chối và hoàn tiền phải lưu người thực hiện, vai trò, thời gian, trạng thái trước/sau, ghi chú và số tiền. |

## Ba mô hình trạng thái chuẩn

Tài liệu chi tiết và quan hệ phối hợp giữa ba vòng đời nằm tại `docs/STATE_MODEL.md`.

### Trạng thái đơn hàng (`OrderStatus`)

| Trạng thái hiện tại | Trạng thái hợp lệ tiếp theo |
|---|---|
| Pending | Confirmed, Cancelled |
| Confirmed | Preparing, Cancelled |
| Preparing | Ready, Cancelled |
| Ready | Delivering, Cancelled |
| Delivering | Delivered khi khách xác nhận; Delivered ngoại lệ sau ETA 60 phút có lý do; Ready khi nhà hàng báo giao thất bại có lý do |
| Delivered | Không có |
| Cancelled | Không có |

API web và API lịch sử cùng biểu diễn `Pending`, `Confirmed`, `In Progress`, `Ready`, `Delivering`, `Delivered`, `Cancelled`; mã Java và dữ liệu H2 dùng enum chữ hoa (`PENDING`, `CONFIRMED`, ...).

### Trạng thái phân công giao hàng (`DeliveryStatus`)

| Trạng thái hiện tại | Trạng thái hợp lệ tiếp theo |
|---|---|
| Assigned | Delivering, Failed, Cancelled |
| Delivering | Delivered, Failed, Cancelled |
| Delivered | Không có |
| Failed | Không có; muốn giao lại phải tạo phân công mới |
| Cancelled | Không có |

### Trạng thái thanh toán (`PaymentStatus`)

| Trạng thái hiện tại | Trạng thái hợp lệ tiếp theo |
|---|---|
| pending | paid, failed, expired, refund_pending |
| failed / expired | Tạo phiên thanh toán mới trên cùng đơn; phiên mới bắt đầu ở pending |
| paid | refund_pending, partial_refund_pending |
| refund_pending | refunded |
| partial_refund_pending | partial_refunded |
| refunded / partial_refunded | Không có |
