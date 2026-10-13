# Tích hợp VNPay Sandbox

## Mục tiêu nghiệp vụ

Luồng thanh toán VNPay tách biệt với trạng thái xử lý/giao hàng:

1. Khách đặt món, backend tạo đơn với `paymentStatus=pending`.
2. Đơn VNPay chưa thanh toán không xuất hiện trong danh sách xử lý của nhà hàng.
3. Backend lấy tổng tiền đã tính trong cơ sở dữ liệu, tạo mã giao dịch riêng và ký URL bằng HMAC-SHA512.
4. VNPay trả kết quả qua Return URL và IPN.
5. Backend kiểm tra chữ ký, `TmnCode`, mã giao dịch và số tiền trước khi cập nhật.
6. Chỉ khi kết quả hợp lệ và thành công, đơn chuyển sang `paymentStatus=paid` và nhà hàng mới nhận thông báo.
7. Callback lặp được xử lý an toàn, không tạo thông báo thanh toán lần hai.

Frontend không giữ `HashSecret`, không tự đánh dấu đã thanh toán và không được gọi API sửa `paymentStatus`.

## Cấu hình

Các giá trị nhạy cảm được đọc từ biến môi trường, không ghi vào Git:

```powershell
$env:VNPAY_ENABLED="true"
$env:VNPAY_TMN_CODE="MA_WEBSITE_VNPAY_CAP"
$env:VNPAY_HASH_SECRET="CHUOI_BI_MAT_VNPAY_CAP"
$env:VNPAY_RETURN_URL="https://TEN-TUNNEL.trycloudflare.com/api/payment/vnpay/browser-return"
```

Sau khi đặt biến trong cùng cửa sổ PowerShell, chạy backend:

```powershell
cd D:\FoodFast\backend
.\mvnw.cmd spring-boot:run
```

### Cách chạy an toàn trên Windows

1. Mở `backend/.env.vnpay.local` và điền các giá trị VNPay đã cấp.
2. Không gửi file này cho người khác và không commit lên Git.
3. Chạy:

```powershell
cd D:\FoodFast\backend
.\start-vnpay-sandbox.ps1
```

Script sẽ từ chối khởi động nếu `TmnCode`, `HashSecret`, URL thanh toán hoặc Return URL còn trống. Credential chỉ được nạp vào process backend và không được in ra terminal.

### Khởi động tự động, không sửa Return URL thủ công

Sau khi đã điền credential một lần trong `backend/.env.vnpay.local`, từ thư mục gốc dự án chạy:

```powershell
cd D:\FoodFast
.\start-foodfast-vnpay.ps1
```

Script tự mở frontend, tạo Cloudflare Quick Tunnel trỏ thẳng vào backend, lấy URL HTTPS mới và truyền Return URL cho backend trong bộ nhớ. Backend đưa trình duyệt về lại `http://localhost:5173/vnpay-return`, vì vậy phiên đăng nhập local được giữ nguyên. File credential không bị sửa. Giữ terminal chạy script mở trong suốt lúc kiểm thử VNPay; nhấn `Ctrl+C` khi muốn dừng tunnel.

Nếu frontend đã được chạy riêng ở cổng `5173`, có thể dùng:

```powershell
.\start-foodfast-vnpay.ps1 -SkipFrontend
```

Giá trị mặc định khác:

- `VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`
- `VNPAY_EXPIRE_MINUTES=15`

## URL cấu hình trên VNPay

Với tunnel trỏ thẳng vào backend ở cổng `8080`:

- Website/Return URL: `https://TEN-TUNNEL.trycloudflare.com/api/payment/vnpay/browser-return`
- IPN URL: `https://TEN-TUNNEL.trycloudflare.com/api/payment/vnpay/ipn`

Endpoint `browser-return` chỉ chuyển trình duyệt về frontend local; frontend vẫn gọi backend để xác minh chữ ký, còn IPN hợp lệ mới được phép xác nhận đơn đã thanh toán. Quick Tunnel có thể đổi URL sau khi khởi động lại; launcher tự cập nhật `VNPAY_RETURN_URL` trong bộ nhớ nhưng cấu hình Merchant vẫn phải dùng IPN URL mới nếu VNPay yêu cầu đăng ký trước. Khi triển khai thật nên dùng tên miền hoặc Named Tunnel cố định.

## API chính

- `GET /api/payment/vnpay/status`: kiểm tra cấu hình đã sẵn sàng.
- `POST /api/payment/vnpay/create`: tạo URL từ `orderId` đã lưu; không nhận số tiền từ trình duyệt.
- `GET /api/payment/vnpay/return`: backend xác minh dữ liệu Return URL cho giao diện.
- `GET /api/payment/vnpay/ipn`: nhận thông báo máy chủ từ VNPay.

## Kịch bản kiểm thử

1. Thanh toán thành công: đơn chuyển `pending -> paid`, nhà hàng nhận đúng một thông báo.
2. Khách hủy/thanh toán thất bại: đơn chuyển `pending -> failed`, nhà hàng không nhận đơn.
3. Sai chữ ký: backend từ chối, không cập nhật dữ liệu.
4. Sai số tiền: backend trả mã IPN `04`, không cập nhật dữ liệu.
5. Gửi lại IPN: backend nhận biết đơn đã xác nhận và không xử lý lặp.
6. Frontend tự PATCH `paymentStatus`: backend trả HTTP `403`.

Tài liệu VNPay: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
