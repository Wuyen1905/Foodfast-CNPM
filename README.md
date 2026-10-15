# FoodFast CNPM

FoodFast là đồ án Công nghệ phần mềm về đặt món và giao hàng. Khách hàng đặt món trên website, nhà hàng tiếp nhận và chế biến từng đơn con, quản trị viên điều phối giao hàng, còn nhân viên quản lý theo ca phụ trách vận hành và đối soát tiền nhà hàng.

## Chức năng chính

- Khách hàng: đăng ký, đăng nhập, xem thực đơn, chọn tùy chọn món, quản lý giỏ nhiều nhà hàng, địa chỉ, yêu thích, mã giảm giá, đặt lại đơn cũ và theo dõi các đơn con trong cùng một lần đặt.
- Nhà hàng: quản lý món ăn, tùy chọn, số lượng bán trong ngày, khuyến mãi, hàng đợi bếp, đơn hàng, khiếu nại và phiếu hỗ trợ; nhà hàng chỉ xác nhận món đã sẵn sàng giao.
- Quản trị viên: theo dõi vận hành, nhà hàng, khách hàng, đơn hàng, mã giảm giá, yêu cầu hỗ trợ, hồ sơ shipper, phân công giao hàng và nhật ký hệ thống.
- Nhân viên quản lý: hai tài khoản thay ca dùng chung một cổng nghiệp vụ; có thể theo dõi vận hành, xem đơn chờ đối soát, kiểm tra công thức tiền phải trả theo nhà hàng, chuyển đợt sang chờ thanh toán và lưu lịch sử thao tác theo từng tài khoản.
- Thanh toán: một giao dịch VNPay có thể thanh toán cả nhóm đơn; COD được ghi nhận khi từng đơn con giao thành công. Kết quả máy chủ VNPay gửi về là nguồn xác nhận thanh toán trực tuyến chính thức.
- Cập nhật thời gian thực: trạng thái đơn, giao hàng, thông báo và hỗ trợ được đồng bộ bằng WebSocket.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Giao diện | React, TypeScript, Vite, Styled Components |
| Máy chủ | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| Dữ liệu | MySQL 8.4, Flyway; H2 chỉ dùng cho kiểm thử nhanh |
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
- MySQL 8.4; trên máy Windows đã cấu hình dịch vụ `MySQL84` tự khởi động.
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
- Cơ sở dữ liệu phát triển: `foodfast_dev` trên MySQL cục bộ.

Flyway tự cập nhật cấu trúc MySQL khi backend khởi động. Cấu hình đăng nhập cục bộ nằm trong `backend/.env.mysql.local`, được Git bỏ qua. H2 vẫn được dùng riêng cho bộ kiểm thử để kiểm thử nhanh và độc lập.

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

## Tài khoản đăng nhập dùng thử

Các màn hình đăng nhập không tự điền thông tin. Nhập thủ công tài khoản phù hợp với cổng cần kiểm tra:

| Vai trò | Cổng đăng nhập | Tên đăng nhập | Mật khẩu | Phạm vi kiểm tra chính |
| --- | --- | --- | --- | --- |
| Quản trị viên | `http://localhost:5173/admin/login` | `admin` | `admin123` | Quản lý tài khoản, nhà hàng, mã giảm giá, shipper, điều phối giao hàng, hỗ trợ và nhật ký hệ thống |
| Nhân viên quản lý ca 1 | `http://localhost:5173/manager/login` | `quanly` | `quanly123` | Theo dõi vận hành và thực hiện quy trình đối soát nhà hàng |
| Nhân viên quản lý ca 2 | `http://localhost:5173/manager/login` | `quanly1` | `quanly123` | Thay ca trên cùng giao diện; thao tác vẫn được ghi theo đúng tài khoản đăng nhập |
| Khách hàng mẫu | `http://localhost:5173/login` | `user` | `user123` | Đặt món, thanh toán, theo dõi đơn, yêu thích, địa chỉ và hỗ trợ |
| Khách hàng thứ hai | `http://localhost:5173/login` | `user1` | `user1123` | Kiểm tra cách ly dữ liệu giữa hai khách hàng |
| Nhà hàng SweetDreams | `http://localhost:5173/login` | `sweetdreams` | `sweet123` | Quản lý món và xử lý các đơn con của SweetDreams |
| Nhà hàng Aloha | `http://localhost:5173/login` | `aloha_restaurant` | `aloha123` | Quản lý món và xử lý các đơn con của Aloha |

Sau khi đăng nhập, hệ thống tự chuyển người dùng đến đúng giao diện theo vai trò. Hai tài khoản `quanly` và `quanly1` có cùng phạm vi nghiệp vụ để thay ca, nhưng lịch sử vẫn lưu riêng người thực hiện và thời gian thao tác.

Các tài khoản trên chỉ được nạp trong cơ sở dữ liệu phát triển để học tập và trình diễn. Mật khẩu được lưu dưới dạng mã băm BCrypt, không lưu trực tiếp trong cơ sở dữ liệu. Không sử dụng các mật khẩu mẫu này khi triển khai thật.

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

- Không có cổng đăng nhập riêng cho shipper; quản trị viên quản lý hồ sơ và phân công shipper, mỗi shipper chỉ có một nhiệm vụ đang giao.
- Không theo dõi GPS thời gian thực.
- Không tách thực thể hóa đơn; đơn hàng lưu tổng tiền và thông tin thanh toán cần thiết.
- Cấu hình production dùng database `foodfast_prod`, không nạp tài khoản hoặc món ăn mẫu.
