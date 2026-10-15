# Lộ trình hoàn thiện nghiệp vụ FoodFast

## 1. Quyết định phạm vi

FoodFast được xây dựng theo mô hình nhiều nhà hàng: nhà hàng xử lý món, quản trị viên điều phối giao hàng tập trung, còn nhân viên quản lý phụ trách đối soát tiền nhà hàng.

- Không xây dựng cổng hoặc ứng dụng riêng cho shipper.
- Không theo dõi GPS theo thời gian thực.
- Khách hàng theo dõi đơn bằng trạng thái, thời gian giao dự kiến và thông tin liên hệ.
- Nhà hàng chịu trách nhiệm xác nhận và chuẩn bị món; quản trị viên quản lý hồ sơ shipper, phân công và cập nhật nhiệm vụ giao hàng.
- AI chỉ tư vấn, tra cứu và định tuyến hỗ trợ; AI không tự ý hủy đơn, hoàn tiền hay đổi trạng thái.

Phạm vi này đủ logic cho đồ án và tránh biến hệ thống thành một nền tảng logistics quá lớn.

## 2. Luồng nghiệp vụ đích

```text
Khách chọn món từ một hoặc nhiều nhà hàng
  -> thêm món và tùy chọn món vào cùng giỏ
  -> nhập địa chỉ / ghi chú giao hàng
  -> hệ thống kiểm tra giờ mở cửa, tồn món, vùng giao và tính lại giá
  -> tạo một nhóm đặt hàng, tách một đơn con cho mỗi nhà hàng
  -> thanh toán toàn nhóm qua VNPay hoặc chọn tiền mặt
  -> từng nhà hàng xác nhận / từ chối đơn con có lý do
  -> chuẩn bị món
  -> đánh dấu sẵn sàng
  -> quản trị viên phân công shipper cho từng đơn con
  -> đang giao
  -> khách xác nhận đã nhận hoặc quản trị viên ghi nhận ngoại lệ có lý do
  -> khách đánh giá / báo sự cố nếu có
```

Trạng thái đơn đang áp dụng:

```text
PENDING -> CONFIRMED -> PREPARING -> READY -> DELIVERING -> DELIVERED
```

Các trạng thái liên quan được tách đúng đối tượng:

- `CANCELLED` là nhánh kết thúc của `OrderStatus`; hành động từ chối được biểu diễn bằng người thực hiện và lý do hủy.
- `ASSIGNED`, `FAILED` thuộc `DeliveryStatus`; giao thất bại đưa đơn về `READY` để phân công lại.
- `pending`, `paid`, `failed`, `expired`, `refund_pending`, `refunded` thuộc `PaymentStatusPolicy`.
- Mô hình đầy đủ và quy tắc phối hợp nằm tại `docs/STATE_MODEL.md`.

## 3. Những phần dự án đã có

- Giỏ hàng có thể chứa món của nhiều nhà hàng; backend tách thành nhóm và các đơn con độc lập.
- Dashboard nhà hàng chỉ hiển thị đơn của đúng nhà hàng.
- Quản trị viên quản lý tập trung hồ sơ shipper và phân công từng nhiệm vụ giao hàng.
- Có thời gian giao dự kiến, giao thất bại, giao lại và xác nhận đã nhận hàng.
- Có cơ chế tự hoàn tất nếu khách quên xác nhận.
- Có cập nhật trạng thái theo thời gian thực.
- Có thanh toán VNPay sandbox cho toàn nhóm đơn, phân bổ tiền xuống từng đơn con, chống callback lặp và tạo lại phiên hết hạn.
- Có cờ món đang bán/hết món và kiểm tra lại món khi tạo đơn.
- Có trợ lý hỗ trợ và phiếu yêu cầu hỗ trợ cơ bản.
- Đăng nhập nhà hàng và việc cô lập đơn theo nhà hàng đã được sửa.
- Hủy/từ chối có lý do, người thực hiện và hậu quả thanh toán.
- Tạm dừng nhận đơn và giờ mở cửa do đúng chủ nhà hàng quản lý.
- Đánh giá 1–5 sao sau giao hàng, mỗi đơn một lần.
- Tùy chọn món/topping/ghi chú với giá do backend xác minh.
- Voucher có thời hạn, phạm vi nhà hàng, đơn tối thiểu và giới hạn lượt dùng.
- Nhà hàng đề xuất thay món; khách chấp nhận/từ chối; hệ thống tính lại và ghi chờ hoàn phần chênh lệch.
- Có vai trò nhân viên quản lý, đợt đối soát nhà hàng, công thức tiền do backend tính, lịch sử kiểm tra và thanh toán.

## 4. Khoảng trống và mức ưu tiên

### P0 - Bắt buộc để nghiệp vụ đúng và bảo vệ dữ liệu

| Hạng mục | Vấn đề hiện tại | Kết quả cần đạt | Tiêu chí nghiệm thu chính |
|---|---|---|---|
| Cơ sở dữ liệu bền vững | **Đã hoàn thành:** H2 file lưu trên ổ đĩa và Flyway migration; H2 in-memory chỉ dùng cho test | Theo dõi mọi thay đổi schema bằng migration mới và sao lưu file trước demo | Khởi động lại không mất tài khoản, đơn và thanh toán; Hibernate validate schema khi khởi động |
| Xác thực và phân quyền | **Đã hoàn thành:** BCrypt, JWT, quyền sở hữu ở backend và WebSocket có xác thực/phạm vi | Tiếp tục mở rộng test khi thêm API mới | Khách không đọc/sửa đơn người khác; nhà hàng không đọc/sửa đơn nhà hàng khác; API admin bị chặn đúng quyền |
| Chống tạo đơn trùng | **Đã hoàn thành:** Idempotency-Key cho tạo đơn/VNPay và ràng buộc duy nhất ở DB | Duy trì khóa cho mọi thao tác tài chính mới | Gửi cùng một yêu cầu hai lần chỉ tạo một đơn/một giao dịch |
| Giá là nguồn sự thật tại backend | **Đã hoàn thành:** quote và tạo đơn cùng dùng giá món/tùy chọn/voucher từ backend | Không nhận giá hay tổng tiền do trình duyệt quyết định | Sửa giá ở trình duyệt không làm thay đổi số tiền đơn |
| Chính sách hủy đơn | **Đã hoàn thành:** khách/nhà hàng hủy có quyền, lý do và audit | Tiếp theo có thể bổ sung SLA duyệt hoàn tiền | Khách chỉ hủy trước mốc quy định; đơn đã trả tiền chuyển chờ hoàn; mọi lần hủy có nhật ký |
| Trạng thái cửa hàng | **Đã hoàn thành:** giờ mở cửa và tạm dừng nhận đơn có lý do | Tiếp theo có thể thêm lịch riêng theo thứ/ngày lễ | Không thể đặt khi đóng cửa; nhà hàng có thể tạm dừng và khách thấy lý do |

### P1 - Nghiệp vụ vận hành giống ứng dụng hiện đại

| Hạng mục | Cần bổ sung | Tiêu chí nghiệm thu chính |
|---|---|---|
| Hết món sau khi đặt | **Đã hoàn thành:** nhà hàng đề xuất món thay thế; khách chấp nhận/từ chối/hết hạn | Tổng tiền được tính lại, ghi chờ hoàn phần chênh và lưu món gốc |
| Tùy chọn món | **Đã hoàn thành:** kích cỡ, topping, hương vị, ghi chú và nhóm bắt buộc | Giá tùy chọn được backend kiểm tra; chi tiết được giữ nguyên trong đơn |
| Địa chỉ và vùng giao | **Đã có:** sổ địa chỉ, địa chỉ mặc định và ghi chú giao. **Cần bổ sung:** kiểm tra vùng phục vụ | Không nhận đơn ngoài vùng; phí và ETA được tính theo quy tắc rõ ràng |
| Xử lý sự cố và hoàn tiền | **Đã có:** thiếu/sai/đổ vỡ/giao trễ, ảnh bằng chứng, trạng thái và lịch sử. **Cần bổ sung:** đối soát hoàn tiền tự động | Mỗi vụ việc có người phụ trách, kết quả, số tiền hoàn và nhật ký |
| Đánh giá | **Đã hoàn thành:** khách đánh giá nhà hàng sau khi giao | Chỉ chủ đơn đã giao mới đánh giá; mỗi đơn một đánh giá; backend tính điểm trung bình |
| Thông báo | **Đã hoàn thành:** trung tâm thông báo khách hàng cho trạng thái, thanh toán, phân công và giao thất bại | Lưu bền vững trong H2, có số chưa đọc, liên kết đơn và cập nhật realtime |
| Biên nhận | Biên nhận chi tiết và lịch sử thanh toán | Tổng món, phí, thuế, giảm giá, thanh toán và hoàn tiền khớp nhau |

### P2 - Tăng điểm và trải nghiệm nhưng không cản luồng chính

- Đặt món theo lịch.
- **Đã hoàn thành:** Mã giảm giá có thời hạn, phạm vi nhà hàng, đơn tối thiểu và giới hạn lượt dùng.
- Đặt lại đơn cũ, món yêu thích và lịch sử tìm kiếm.
- **Đã hoàn thành:** Đơn nhóm nhiều nhà hàng, một giao dịch VNPay và phân bổ tiền xuống từng đơn con.
- **Đã hoàn thành:** Báo cáo thời gian xác nhận, thời gian chuẩn bị, thời gian giao, tỷ lệ hủy và giao đúng hẹn; hỗ trợ kỳ 7/30/90 ngày và xuất CSV.

## 5. Các quy tắc nghiệp vụ nên bảo vệ bằng mã

1. Một giỏ có thể chứa món của nhiều nhà hàng; mỗi nhà hàng tạo đúng một đơn con trong cùng nhóm.
2. Mỗi đơn con chỉ thuộc một khách và một nhà hàng.
3. Nhà hàng chỉ nhìn thấy và cập nhật đơn của chính mình.
4. Chỉ đơn đã thanh toán hoặc thanh toán tiền mặt hợp lệ mới được nhà hàng tiếp nhận.
5. Chỉ chuyển trạng thái theo chuỗi cho phép; không được nhảy trực tiếp từ chờ xác nhận sang đã giao.
6. Chỉ quản trị viên phân công shipper đang hoạt động; mỗi shipper chỉ có một nhiệm vụ đang giao.
7. Khách chỉ được xác nhận đã nhận đơn của chính mình và chỉ khi đơn đang giao.
8. Khách xác nhận đã nhận là nguồn hoàn tất thông thường; mọi hoàn tất ngoại lệ phải có người thực hiện, lý do và nguồn xác nhận.
9. Mọi thao tác hủy, từ chối, giao thất bại, hoàn tiền và thay đổi giá đều cần lý do và nhật ký.
10. Callback VNPay phải kiểm tra chữ ký, mã đơn, số tiền và không xử lý hai lần.
11. Giá, phí và giảm giá cuối cùng luôn do backend quyết định.
12. Dữ liệu món trong đơn phải là bản chụp tại thời điểm đặt, không đổi theo menu về sau.

## 6. Kế hoạch triển khai theo sprint

### Sprint 1 - Nền tảng an toàn

- Chuyển sang cơ sở dữ liệu bền vững và tạo migration.
- Hoàn thiện đăng ký/đăng nhập, mã hóa mật khẩu và phân quyền API.
- Đưa toàn bộ phép tính giá về backend.
- Chống tạo đơn và xử lý thanh toán lặp.
- Viết test cho các quy tắc quyền sở hữu khách/nhà hàng.

### Sprint 2 - Nhận và xử lý đơn

- Giờ mở cửa, tạm đóng, giới hạn nhận đơn.
- Chính sách khách hủy/nhà hàng từ chối và lý do.
- Tồn món và luồng đề xuất thay thế.
- Chuẩn hóa máy trạng thái đơn, lịch sử trạng thái và audit log.

### Sprint 3 - Giao hàng và hậu mãi

- Sổ địa chỉ, vùng giao, phí giao và ETA.
- Hoàn thiện giao thất bại, giao lại và tự xác nhận sau thời gian chờ.
- Phiếu sự cố, hoàn tiền và biên nhận.
- Đánh giá sau giao hàng.

### Sprint 4 - Tính năng tăng điểm

- Tùy chọn món/topping.
- Khuyến mãi và mã giảm giá.
- Đặt món theo lịch, đặt lại đơn.
- Dashboard chỉ số vận hành và tài liệu demo/vấn đáp.

## 7. Tài liệu phục vụ môn Công nghệ phần mềm

Mỗi sprint nên có:

- User story và acceptance criteria.
- Sơ đồ trạng thái đơn hàng.
- Sequence diagram cho đặt đơn, VNPay, phân công giao và hoàn tiền.
- Bảng business rules có mã định danh.
- Test case liên kết với từng business rule.
- Sprint backlog, kết quả demo và retrospective ngắn.
- Decision log giải thích vì sao không làm cổng shipper/GPS.

Cách này chứng minh dự án áp dụng Agile bằng sản phẩm chạy được, tiêu chí nghiệm thu và vòng lặp kiểm thử, thay vì chỉ ghi lý thuyết trong báo cáo.
