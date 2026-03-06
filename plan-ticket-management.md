# Plan: Ticket Management (Full — A + B + C)

## Tổng quan
Sau khi thanh toán VNPay thành công → sinh vé tự động → user xem vé + QR + download PDF → organizer check-in bằng nhập mã hoặc scan QR camera.

---

## PHASE A — Sinh vé tự động (Backend)

### Step 1: Tạo model + enum + repository
- **TicketStatus enum**: `ISSUED`, `USED`, `CANCELLED`
- **Ticket entity** (`TICKETS` table):
  - `ID` (PK, sequence `TICKET_SEQ`)
  - `ORDER_ITEM_ID` (FK)
  - `USER_ID` (FK)
  - `EVENT_ID` (FK)
  - `TICKET_TYPE_ID` (FK)
  - `TICKET_CODE` (unique, format: `TBX-{yyyyMMdd}-{RANDOM6}`)
  - `QR_DATA` (JSON string: `{"code":"TBX-...","eventId":1,"userId":2}`)
  - `STATUS` (default: ISSUED)
  - `USED_AT` (nullable, timestamp khi check-in)
  - `CREATED_DATE`, `UPDATED_DATE`
- **TicketRepository**: findByUserId, findByTicketCode, findByEventIdAndStatus, etc.

### Step 2: Thêm lib ZXing (QR) + OpenPDF (PDF) vào build.gradle
- `com.google.zxing:core:3.5.3`
- `com.google.zxing:javase:3.5.3`
- `com.github.librepdf:openpdf:2.0.3`

### Step 3: Tạo TicketService
- `generateTickets(Order order)`: loop qua orderItems → sinh N tickets per item
- `generateTicketCode()`: `TBX-{yyyyMMdd}-{SecureRandom 6 chars}`
- `generateQrData(Ticket)`: JSON chứa code + eventId + userId
- `getMyTickets(userId, eventId?, status?, page, size)`: list vé của user
- `getTicketDetail(userId, ticketId)`: chi tiết 1 vé
- `checkIn(ticketCode, organizerId)`: validate + mark USED

### Step 4: Gọi generateTickets() trong OrderService.processVnPayIpn()
- Sau khi set status = COMPLETED, paymentStatus = SUCCESS
- Gọi `ticketService.generateTickets(order)`

---

## PHASE B — Xem vé, QR code, Download PDF (Backend + Frontend)

### Step 5: Tạo TicketController + DTOs
- **TicketResponse DTO**: id, ticketCode, status, eventTitle, eventDate, location, ticketTypeName, usedAt, createdDate
- **Endpoints**:
  - `GET /v1/tickets` — list vé của user (filter: eventId, status, phân trang)
  - `GET /v1/tickets/{id}` — chi tiết 1 vé
  - `GET /v1/tickets/{id}/qr` — trả QR image (PNG, content-type: image/png)
  - `GET /v1/tickets/{id}/pdf` — trả PDF download

### Step 6: QR Code generation service
- `QrCodeService.generateQrImage(String data, int width, int height)` → byte[] PNG
- Dùng ZXing `QRCodeWriter` + `MatrixToImageWriter`

### Step 7: PDF generation service
- `TicketPdfService.generateTicketPdf(Ticket ticket, byte[] qrImage)` → byte[]
- Layout PDF: Ticket info (event name, date, location, ticket type, code) + QR image
- Dùng OpenPDF (fork of iText)

### Step 8: Frontend — Trang danh sách vé `/tickets`
- Gọi `GET /v1/tickets` với phân trang
- Card hiển thị: event name, date, ticket type, status badge, ticket code
- Click → navigate tới `/tickets/[id]`
- Thêm link "My Tickets" vào Header

### Step 9: Frontend — Trang chi tiết vé `/tickets/[id]`
- Gọi `GET /v1/tickets/{id}`
- Hiển thị: full ticket info + QR code (load từ `/v1/tickets/{id}/qr`)
- Nút "Download PDF" → gọi `/v1/tickets/{id}/pdf` và trigger download

### Step 10: Frontend — ticket service + types
- `ticketService.ts`: getMyTickets(), getTicketDetail(), getQrUrl(), downloadPdf()
- `ticket.ts` types: Ticket, TicketStatus

---

## PHASE C — Check-in vé (Backend + Frontend)

### Step 11: Check-in API
- `POST /v1/tickets/check-in` body: `{ "ticketCode": "TBX-..." }`
- Chỉ ORGANIZER/ADMIN mới gọi được
- Validate:
  - Ticket tồn tại
  - Status = ISSUED (chưa dùng)
  - Event thuộc organizer đang check-in (hoặc ADMIN thì bỏ qua check này)
  - Event chưa quá hạn (optional)
- Kết quả: set status = USED, usedAt = now()
- Response trả ticket info (event, attendee name, ticket type) để organizer xác nhận

### Step 12: DTO cho check-in
- **CheckInRequest**: ticketCode (String)
- **CheckInResponse**: ticketCode, eventTitle, attendeeName, ticketTypeName, status, message

### Step 13: Frontend — Trang check-in `/organizer/check-in`
- 2 modes: Tab "Nhập mã" + Tab "Scan QR"
- **Nhập mã**: input text → submit → gọi check-in API → hiện kết quả
- **Scan QR**: dùng lib `html5-qrcode` mở camera → decode QR → auto gọi check-in API
- Hiển thị kết quả: success (xanh) / fail (đỏ) + thông tin vé
- Install: `npm install html5-qrcode`

### Step 14: Thêm link "Check-in" vào Header (chỉ hiện cho ORGANIZER/ADMIN)

---

## Tóm tắt files cần tạo/sửa

### Backend — TẠO MỚI
- `model/Ticket.java`
- `model/TicketStatus.java`
- `repository/TicketRepository.java`
- `service/TicketService.java`
- `service/QrCodeService.java`
- `service/TicketPdfService.java`
- `controller/TicketController.java`
- `dto/TicketResponse.java`
- `dto/CheckInRequest.java`
- `dto/CheckInResponse.java`

### Backend — SỬA
- `build.gradle` — thêm ZXing + OpenPDF dependencies
- `service/OrderService.java` — gọi ticketService.generateTickets() trong processVnPayIpn()
- `config/SecurityConfig.java` — cho phép ORGANIZER/ADMIN truy cập check-in endpoint

### Frontend — TẠO MỚI
- `lib/ticket-service.ts`
- `types/ticket.ts`
- `app/tickets/page.tsx` — danh sách vé
- `app/tickets/[id]/page.tsx` — chi tiết vé + QR + download PDF
- `app/organizer/check-in/page.tsx` — check-in page

### Frontend — SỬA
- `components/Header.tsx` — thêm "My Tickets" + "Check-in" links
- `package.json` — thêm `html5-qrcode`

---

## Thứ tự thực hiện
A (Step 1→4) → B (Step 5→10) → C (Step 11→14)
