# FoodFast Backend - Spring Boot API

Spring Boot backend cho nền tảng đặt món và giao hàng nội bộ FoodFast.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.4

## Setup

### 1. Cơ sở dữ liệu

Ứng dụng phát triển dùng MySQL database `foodfast_dev`. Flyway đọc lược đồ từ `src/main/resources/db/mysql` và dữ liệu mẫu từ `src/main/resources/db/mysql-dev`. Profile `prod` dùng `foodfast_prod` và không nạp dữ liệu mẫu. H2 chỉ còn dùng trong profile `test`.

Tạo cấu hình cục bộ từ tệp mẫu nếu máy chưa có:

```powershell
Copy-Item .env.mysql.local.example .env.mysql.local
```

Điền mật khẩu của tài khoản MySQL `foodfast_app` vào `.env.mysql.local`. Tệp thật đã được Git bỏ qua.

### 2. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

Backend chạy tại `http://localhost:8080`.

### 3. Verify Backend is Running

Check the health endpoint:
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{"status":"ok","service":"foodfast-backend"}
```

## API Endpoints

### Orders
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get order by ID
- `POST /api/orders` - Create new order
- `PUT /api/orders/{id}` - Update order
- `PATCH /api/orders/{id}` - Partial update order
- `DELETE /api/orders/{id}` - Delete order

## Configuration

### application.properties

Key configurations:
- `server.port=8080` - Backend server port
- `spring.datasource.url` - Database connection URL
- `spring.web.cors.allowed-origins` - CORS allowed origins (frontend URL)

### CORS Configuration

CORS is configured to allow requests from `http://localhost:5173` (frontend Vite dev server).

## Development

### Running with IDE

1. Open the project in IntelliJ IDEA or Eclipse
2. Run `FoodFastApplication.main()`
3. Backend will start on port 8080

### Testing

```bash
# Run tests
mvn test
```

## Troubleshooting

### Port 8080 Already in Use

Change the port in `application.properties`:
```properties
server.port=8081
```

Update frontend `vite.config.ts` proxy target accordingly.

### Database Connection Error

1. Kiểm tra dịch vụ `MySQL84` đang chạy: `Get-Service MySQL84`.
2. Kiểm tra database `foodfast_dev` và tài khoản `foodfast_app` tồn tại.
3. Kiểm tra mật khẩu trong `.env.mysql.local`; không đưa tệp này lên Git.
4. Mở `http://localhost:8080/api/health` sau khi backend khởi động.

### CORS Errors

Ensure `CorsConfig.java` is properly configured and `application.properties` has correct CORS settings.

## Kiểm thử migration và nghiệp vụ

`mvn test` chạy Flyway trên H2 in-memory biệt lập, sau đó Hibernate kiểm tra migration khớp với toàn bộ entity trước khi chạy test API/nghiệp vụ.

Bộ kiểm thử migration MySQL chạy trên database riêng `foodfast_test` và chỉ được bật chủ động:

```powershell
mvn "-Dfoodfast.mysql.test=true" `
    "-Dfoodfast.mysql.url=jdbc:mysql://127.0.0.1:3306/foodfast_test?allowPublicKeyRetrieval=true" `
    "-Dfoodfast.mysql.username=foodfast_app" `
    "-Dfoodfast.mysql.password=<mat-khau-cuc-bo>" `
    "-Dtest=MySqlMigrationIntegrationTest" test
```

Bài kiểm thử này chỉ xóa và dựng lại `foodfast_test`; không đụng đến `foodfast_dev` hoặc `foodfast_prod`.
