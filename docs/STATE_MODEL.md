# Mô hình trạng thái FoodFast

Tài liệu này là nguồn sự thật duy nhất cho ba vòng đời độc lập của một đơn. Không gộp trạng thái thanh toán hoặc phân công giao hàng vào `OrderStatus`.

## 1. Trạng thái đơn hàng (`OrderStatus`)

```text
PENDING -> CONFIRMED -> PREPARING -> READY -> DELIVERING -> DELIVERED
   |           |            |         |
   +-----------+------------+---------+-> CANCELLED (theo chính sách)

DELIVERING -> READY khi lần giao thất bại và cần phân công lại
```

| Trạng thái | Ý nghĩa | Ai được chuyển tiếp |
|---|---|---|
| `PENDING` | Đơn đã tạo, đang chờ nhà hàng tiếp nhận | Nhà hàng xác nhận; khách/nhà hàng hủy theo chính sách |
| `CONFIRMED` | Nhà hàng đã nhận đơn | Nhà hàng bắt đầu chuẩn bị |
| `PREPARING` | Bếp đang chuẩn bị | Nhà hàng đánh dấu sẵn sàng |
| `READY` | Món đã sẵn sàng, có thể phân công giao | Nhà hàng phân công và bắt đầu giao |
| `DELIVERING` | Nhân viên đang giao | Khách xác nhận nhận; nhà hàng hoàn tất ngoại lệ; hoặc báo thất bại |
| `DELIVERED` | Đơn hoàn tất | Trạng thái kết thúc |
| `CANCELLED` | Đơn bị hủy/từ chối có lý do | Trạng thái kết thúc |

Mọi chuyển đổi được lưu tại `order_status_history` gồm trạng thái cũ/mới, người thực hiện, vai trò, nguồn, lý do và thời điểm.

Quy ước biểu diễn: Java/H2 dùng enum chữ hoa; JSON gửi cho web dùng `Pending`, `Confirmed`, `In Progress`, `Ready`, `Delivering`, `Delivered`, `Cancelled` cho cả đơn hiện tại và lịch sử.

## 2. Trạng thái phân công giao hàng (`DeliveryStatus`)

```text
ASSIGNED -> DELIVERING -> DELIVERED
     |            |
     +------------+-> FAILED
     +------------+-> CANCELLED
```

- `ASSIGNED`: đã chọn nhân viên nhưng người giao chưa xuất phát.
- `DELIVERING`: đã nhận món và bắt đầu giao; lúc này `OrderStatus` mới chuyển sang `DELIVERING`.
- `FAILED`: phải có lý do; `OrderStatus` quay lại `READY` để phân công lại.
- `DELIVERED`: chỉ hoàn tất khi khách xác nhận hoặc nhà hàng dùng ngoại lệ sau ETA 60 phút và có lý do.

## 3. Trạng thái thanh toán (`PaymentStatus`)

Các giá trị được quản lý bởi `PaymentStatusPolicy`, độc lập với tiến độ chế biến:

```text
pending -> paid
   |\-> failed / expired -> tạo phiên mới trên cùng đơn
   \-> refund_pending -> refunded
paid -> partial_refund_pending -> partial_refunded
```

- COD chuyển từ `pending` sang `paid` khi giao thành công.
- VNPay chỉ chuyển `paid` từ IPN hợp lệ sau khi kiểm tra chữ ký, mã phiên và số tiền.
- Callback lặp không tạo thêm thanh toán hoặc thông báo.
- Hủy đơn đã thanh toán chuyển sang `refund_pending`, không đổi ngược trạng thái đơn.

## 4. Quan hệ giữa ba vòng đời

1. VNPay phải `paid` trước khi nhà hàng thấy và xử lý đơn; COD được tiếp nhận ngay.
2. Chỉ `OrderStatus.READY` mới được tạo `DeliveryStatus.ASSIGNED`.
3. `ASSIGNED` không làm đổi `OrderStatus`; chỉ khi bắt đầu giao mới chuyển đơn sang `DELIVERING`.
4. Giao thất bại kết thúc lượt phân công hiện tại và đưa đơn về `READY`.
5. Hoàn tiền thuộc thanh toán, không biến đơn `DELIVERED` thành trạng thái chế biến trước đó.
