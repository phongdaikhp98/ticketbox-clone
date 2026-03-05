# Project Overview
# Architecture
Layered architecture: Controller → Service → Repository
RESTful API design
JPA / Hibernate ORM for database access
# Coding Conventions
- Naming: camelCase cho biến/method, PascalCase cho class.
- Service layer chứa toàn bộ business logic — không viết logic trong Controller
- Repository layer chỉ dùng cho database queries
- Dùng DTO (Data Transfer Object) để truyền data giữa các layer, không expose Entity trực tiếp
- Validate input tại Controller layer dùng @Valid và Bean Validation
- Xử lý exception tập trung qua @ControllerAdvice
# Database — Oracle
Sequence thay vì AUTO_INCREMENT cho primary key
Tên bảng và cột viết HOA (Oracle convention), ví dụ: USERS, CREATED_DATE
Pagination dùng ROWNUM hoặc FETCH FIRST n ROWS ONLY (Oracle 12c+)
# API Design
- RESTful conventions: GET / POST / PUT / DELETE
- Response format nhất quán:
json    {
        "code": "Mã lỗi",
        "message": "Mô tả lỗi",
        "requestId": "Trả lại requestId của người gửi hoặc của hệ thống tự sinh"
        "timestamp": "Thời gian trả ra kết quả",
        "data": <Object dữ liệu>
    }
- HTTP status codes đúng chuẩn: 200, 201, 400, 401, 403, 404, 500
- Path Format
kebab-case: /v1/example/url-with-kebab-case
Không dùng dấu cách hoặc ký tự đặc biệt
Luôn thêm version: /v1/, /v2/
- RESTful Structure
GET /resources — danh sách
GET /resources/:id — chi tiết
POST /resources — tạo mới
PUT /resources/:id — update toàn bộ
PATCH /resources/:id — update một phần (optional)
DELETE /resources/:id — xóa
Custom actions: /resources/:id/lock, /resources/:id/unlock
- Pagination / Filter / Sort
Phân trang: ?page=1&size=10
Lọc: ?name=Bob
Sắp xếp: ?sort=name,desc
Tìm kiếm: ?search=Bob
#Testing
Viết unit test với JUnit 5 + Mockito
Test coverage tối thiểu cho Service layer
Dùng @SpringBootTest cho integration test
Hiện tại tỉ lệ unit test đang rất thấp
# What Claude Should Do
- Luôn hỏi clarify nếu yêu cầu chưa rõ trước khi viết code
- Ưu tiên sửa đúng layer (không bypass Service để gọi thẳng Repository từ Controller)
- Đề xuất index nếu thấy query có thể chậm
- Giữ nguyên coding style hiện tại của project khi thêm code mới
# What Claude Should NOT Do
- Không dùng MySQL/PostgreSQL syntax trong SQL query
- Không expose Entity class trực tiếp trong API response
- Không viết business logic trong Controller
- Không bỏ qua exception handling
- Nguyên tắc cuối: Đọc và apply / tuân thủ toàn bộ CLAUDE.md trước khi code.
