# Ticketbox Clone

Ứng dụng đặt vé sự kiện trực tuyến — clone Ticketbox.vn — xây dựng với Spring Boot 3 + Next.js 14.

---

## Tech Stack

| Layer | Công nghệ |
|-------|-----------|
| **Backend** | Spring Boot 3.5.0, Java 17, Spring Security, Spring Data JPA |
| **Database** | Oracle DB (sequence-based PK, UPPER_CASE column naming) |
| **Cache / Rate Limit** | Redis |
| **Frontend** | Next.js 16 (App Router), React 19, TypeScript 5, Tailwind CSS |
| **Auth** | JWT stateless (access 24h, refresh 7d), OAuth2 Google |
| **Payment** | VNPay sandbox (HMAC-SHA512) |
| **Storage** | Cloudinary (image upload) |
| **Email** | Resend SMTP, Thymeleaf HTML templates |
| **Build** | Gradle (BE), npm (FE) |

---

## Tính năng

### Người dùng
- Đăng ký / đăng nhập (local + Google OAuth2), quên mật khẩu
- Xem & cập nhật hồ sơ cá nhân, upload avatar
- Giỏ hàng, danh sách yêu thích
- Mua vé → thanh toán VNPay, chọn ghế ngồi (nếu sự kiện có sơ đồ chỗ)
- Xem đơn hàng, hủy đơn hàng (trước 24h khi sự kiện diễn ra)
- Nhập mã giảm giá khi thanh toán
- Xem vé (QR code), tải vé PDF (A5 landscape), chuyển nhượng vé
- Yêu cầu hoàn tiền

### Nhà tổ chức (ORGANIZER)
- CRUD sự kiện: tiêu đề, mô tả, danh mục, tags, hình ảnh, địa điểm, ngày giờ
- Tạo / quản lý loại vé (tên, giá, số lượng)
- Thiết lập sơ đồ chỗ ngồi (section → ghế, số lượng tùy chỉnh)
- Nhân bản sự kiện
- Check-in bằng mã vé hoặc quét QR camera
- Dashboard: KPI tổng quan, thống kê theo sự kiện, danh sách người tham dự
- Xuất báo cáo Excel (doanh thu, đơn hàng, người dùng)
- Nộp đơn xin lên ORGANIZER (từ tài khoản CUSTOMER)

### Quản trị viên (ADMIN)
- Dashboard hệ thống: tổng users, sự kiện, doanh thu, đơn hàng, vé, check-in
- Quản lý người dùng: tìm kiếm, đổi vai trò, bật/tắt tài khoản
- Quản lý sự kiện: toggle nổi bật, đặt thứ tự nổi bật, đổi trạng thái
- Quản lý đơn hàng, xử lý hoàn tiền
- Quản lý mã giảm giá (PERCENTAGE / FLAT, giới hạn lượt dùng, thời hạn)
- Duyệt/từ chối đơn xin ORGANIZER
- Lịch sử hành động (Audit Log): theo dõi mọi thay đổi do admin thực hiện
- CRUD danh mục sự kiện

---

## Cài đặt

### Yêu cầu
- Java 17+
- Node.js 18+
- Oracle Database 12c+
- Redis

### Backend

```bash
cd backend

# Tạo file cấu hình local (không commit)
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# Điền thông tin DB, Redis, Cloudinary, Resend, VNPay vào application-local.yml

./gradlew bootRun
# Server chạy tại http://localhost:8083
```

### Frontend

```bash
cd frontend

cp .env.example .env.local
# Điền NEXT_PUBLIC_API_URL=http://localhost:8083

npm install
npm run dev
# App chạy tại http://localhost:3000
```

---

## Cấu hình

### `application-local.yml` (backend)

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE
    username: YOUR_DB_USER
    password: YOUR_DB_PASSWORD
  data:
    redis:
      host: localhost
      port: 6379

cloudinary:
  cloud-name: YOUR_CLOUD_NAME
  api-key: YOUR_API_KEY
  api-secret: YOUR_API_SECRET

resend:
  api-key: YOUR_RESEND_API_KEY
  from-email: noreply@yourdomain.com

vnpay:
  tmn-code: YOUR_TMN_CODE
  hash-secret: YOUR_HASH_SECRET
  url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  return-url: http://localhost:3000/payment/vnpay-return
```

---

## API

Swagger UI: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

### Các nhóm endpoint chính

| Prefix | Mô tả | Quyền |
|--------|-------|-------|
| `POST /v1/auth/*` | Đăng ký, đăng nhập, refresh token, reset mật khẩu | Public |
| `GET /v1/events/**` | Danh sách & chi tiết sự kiện, filter | Public |
| `POST /v1/events` | Tạo sự kiện | ORGANIZER |
| `PUT /v1/events/:id` | Cập nhật sự kiện | ORGANIZER (owner) |
| `GET /v1/cart` | Giỏ hàng | CUSTOMER |
| `POST /v1/orders/checkout` | Thanh toán | Authenticated |
| `POST /v1/payment/vnpay/create-url` | Tạo URL thanh toán VNPay | Authenticated |
| `GET /v1/tickets` | Danh sách vé của tôi | Authenticated |
| `POST /v1/tickets/check-in` | Check-in vé | ORGANIZER / ADMIN |
| `GET /v1/dashboard/*` | Dashboard nhà tổ chức | ORGANIZER / ADMIN |
| `GET /v1/admin/*` | Quản trị hệ thống | ADMIN |
| `POST /v1/promo-codes/validate` | Kiểm tra mã giảm giá | Authenticated |
| `GET /v1/seat-maps/events/:id` | Sơ đồ chỗ ngồi | Public |

### Response format

```json
{
  "code": "200",
  "message": "Success",
  "requestId": "abc-123",
  "timestamp": "2026-03-23T10:00:00",
  "data": {}
}
```

---

## Kiến trúc

```
backend/
├── controller/     # REST endpoints, input validation (@Valid)
├── service/        # Business logic
├── repository/     # Database queries (Spring Data JPA)
├── model/          # JPA entities (Oracle naming convention)
├── dto/            # Request / Response DTOs
├── security/       # JWT filter, UserDetailsImpl
├── config/         # SecurityConfig, CloudinaryConfig, AppProperties
└── exception/      # GlobalExceptionHandler (@ControllerAdvice)

frontend/
├── src/app/        # Next.js App Router pages
│   ├── admin/      # Quản trị viên
│   ├── organizer/  # Nhà tổ chức
│   ├── events/     # Sự kiện
│   ├── orders/     # Đơn hàng
│   └── tickets/    # Vé
├── src/components/ # Shared components
├── src/lib/        # API service layer
├── src/types/      # TypeScript interfaces
└── src/context/    # React Context (Auth, Cart)
```

---

## Thanh toán VNPay (Test)

| Trường | Giá trị |
|--------|---------|
| Ngân hàng | NCB |
| Số thẻ | 9704198526191432198 |
| Tên chủ thẻ | NGUYEN VAN A |
| Ngày hết hạn | 07/15 |
| OTP | 123456 |

---

## Tính năng đang phát triển

- MoMo / ZaloPay / Banking QR payment
- Event reminders (email notification trước sự kiện)

---

## Tác giả

**PhongNH** — [ticketbox by PhongNH](http://localhost:3000)
