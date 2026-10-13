# FoodFast CNPM

FoodFast là đồ án Công nghệ phần mềm về đặt món và giao hàng nội bộ nhà hàng. Khách hàng đặt món trên website, nhà hàng tiếp nhận và chế biến đơn, nhân viên giao hàng do nhà hàng quản lý, còn quản trị viên giám sát toàn hệ thống.

## Chức năng chính

- Khách hàng: đăng ký, đăng nhập, xem thực đơn, chọn tùy chọn món, quản lý giỏ hàng, địa chỉ, yêu thích, mã giảm giá, đặt lại đơn cũ và theo dõi đơn.
- Nhà hàng: quản lý món ăn, tùy chọn, số lượng bán trong ngày, khuyến mãi, hàng đợi bếp, đơn hàng, nhân viên giao hàng, khiếu nại và phiếu hỗ trợ.
- Quản trị viên: theo dõi vận hành, nhà hàng, khách hàng, đơn hàng, mã giảm giá, yêu cầu hỗ trợ, đối soát thanh toán và nhật ký hệ thống.
- Thanh toán: thanh toán khi nhận hàng hoặc VNPay Sandbox; kết quả máy chủ VNPay gửi về là nguồn xác nhận thanh toán trực tuyến chính thức.
- Cập nhật thời gian thực: trạng thái đơn, giao hàng, thông báo và hỗ trợ được đồng bộ bằng WebSocket.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Giao diện | React, TypeScript, Vite, Styled Components |
| Máy chủ | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| Dữ liệu | H2 dạng tệp, Flyway |
| Tích hợp | VNPay Sandbox, Cloudflare Tunnel, WebSocket |
| Kiểm thử | JUnit, Vitest, Testing Library |

## Cấu trúc dự án

```text
Foodfast-CNPM/
├── backend/                     # API, bảo mật, nghiệp vụ và cơ sở dữ liệu
├── frontend-web/                # Giao diện khách hàng, nhà hàng và quản trị viên
├── docs/                        # Tài liệu nghiệp vụ, trạng thái và VNPay
│   ├── agile/                   # Tài liệu Scrum
│   └── specifications/          # Đặc tả Word dùng cho đồ án
├── .github/workflows/ci.yml     # Kiểm thử tự động trên GitHub
├── run_all.bat                  # Chạy nhanh trên Windows
├── run_all.sh                   # Chạy nhanh trên Linux và macOS
└── start-foodfast-vnpay.ps1     # Chạy môi trường thử VNPay
```

`frontend-web/` là giao diện đang được sử dụng. Các thư mục giao diện cũ không thuộc bản công bố này.

## Yêu cầu môi trường

- Java 17 trở lên.
- Maven 3.9 trở lên.
- Node.js 20.19 trở lên và npm 10 trở lên.
- Không cần Docker hoặc máy chủ cơ sở dữ liệu riêng.
- Chỉ cần `cloudflared` khi thử VNPay Sandbox.

## Chạy nhanh

Tại thư mục gốc của dự án:

```powershell
# Windows
.\run_all.bat
```

```bash
# Linux hoặc macOS
bash ./run_all.sh
```

Chạy thủ công trong hai cửa sổ lệnh:

```powershell
cd backend
mvn spring-boot:run
```

```powershell
cd frontend-web
npm ci
npm run dev:fast
```

- Website: `http://localhost:5173`
- API: `http://localhost:8080/api`
- Kiểm tra máy chủ: `http://localhost:8080/api/health`
- H2 Console trong môi trường phát triển: `http://localhost:8080/h2-console`

Flyway tự cập nhật cấu trúc dữ liệu khi backend khởi động. Dữ liệu chạy thủ công được lưu cục bộ trong `backend/runtime-data/` và không được đưa lên GitHub.

## Thử thanh toán VNPay Sandbox

Tạo cấu hình cục bộ một lần:

```powershell
Copy-Item backend\.env.vnpay.local.example backend\.env.vnpay.local
```

Điền `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` vào tệp vừa tạo. Đây là thông tin riêng, không được commit hoặc chia sẻ công khai.

Khởi động frontend, đường hầm HTTPS và backend:

```powershell
.\start-foodfast-vnpay.ps1
```

Nếu frontend đã chạy ở cổng 5173:

```powershell
.\start-foodfast-vnpay.ps1 -SkipFrontend
```

Hướng dẫn chi tiết nằm tại [docs/VNPAY_SANDBOX.md](docs/VNPAY_SANDBOX.md).

## Tài khoản dùng thử

| Vai trò | Tên đăng nhập | Mật khẩu |
| --- | --- | --- |
| Quản trị viên | `admin` | `admin123` |
| Khách hàng | `user` | `user123` |
| Khách hàng thứ hai | `user1` | `user1123` |
| Nhà hàng SweetDreams | `sweetdreams` | `sweet123` |
| Nhà hàng Aloha | `aloha_restaurant` | `aloha123` |

Các tài khoản trên chỉ dành cho môi trường học tập và thử nghiệm.

## Kiểm thử và đóng gói

```powershell
cd backend
mvn --batch-mode test

cd ..\frontend-web
npm ci
npm test
npm run type-check
npm run build
```

GitHub Actions tự chạy các bước kiểm thử và đóng gói này khi có thay đổi được đẩy lên kho mã nguồn.

## Tài liệu

- [Mô hình trạng thái](docs/STATE_MODEL.md)
- [Hướng dẫn VNPay Sandbox](docs/VNPAY_SANDBOX.md)
- [Tài liệu Agile và Scrum](docs/agile/)
- [Đặc tả yêu cầu](docs/specifications/)

## Giới hạn phạm vi

- Không có cổng đăng nhập riêng cho shipper; nhân viên giao hàng được nhà hàng quản lý và phân công.
- Không theo dõi GPS thời gian thực.
- Không tách thực thể hóa đơn; đơn hàng lưu tổng tiền và thông tin thanh toán cần thiết.
- H2 dạng tệp phù hợp cho đồ án và chạy thử cục bộ, chưa phải cấu hình triển khai quy mô lớn.
