# Security Manual Test Results

## 1. Test Overview
Biên bản kiểm tra các cơ chế bảo mật cho Platform Operations bao gồm: xác thực (Authentication Guard), phân quyền theo Role (Authorization Guard), an toàn cơ sở dữ liệu (SQLi), an toàn giao diện (XSS), và phòng chống xung đột lợi ích (Self-Penalty & Self-Revocation Prevention).

---

## 2. Test Cases & Detailed Payloads

### Test 1: Authentication Guard (HTTP 401)
- **Mục tiêu**: Kiểm tra request không có JWT Bearer Token không thể truy cập các endpoint nội bộ platform.
- **Request**:
  ```bash
  curl -i -X GET http://localhost:8080/api/platform/dashboard
  ```
- **Response**:
  ```http
  HTTP/1.1 401 Unauthorized
  Content-Type: application/json

  {"status":401,"error":"Unauthorized","message":"Full authentication is required to access this resource"}
  ```
- **Status**: ✅ **PASSED**

---

### Test 2: Role Authorization Guard (HTTP 403)
- **Mục tiêu**: Người dùng có role thông thường (`CLIENT` hoặc `TUTOR`) không được phép truy cập dashboard hoặc dữ liệu quản trị viên.
- **Request**:
  ```bash
  curl -i -X GET http://localhost:8080/api/platform/dashboard \
    -H "Authorization: Bearer <CLIENT_JWT_TOKEN>"
  ```
- **Response**:
  ```http
  HTTP/1.1 403 Forbidden
  Content-Type: application/json

  {"status":403,"error":"Forbidden","message":"Access Denied: Requires PLATFORM_ADMIN authority"}
  ```
- **Status**: ✅ **PASSED**

---

### Test 3: SQL Injection Prevention trong Reason & Text Fields
- **Mục tiêu**: Kiểm tra Hibernate/JPA Parameterized Queries ngăn chặn các chuỗi khai thác SQL Injection.
- **Request**:
  ```bash
  curl -i -X POST http://localhost:8080/api/platform/penalties \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
    -d "{\"userId\":2,\"penaltyType\":\"WARNING\",\"reason\":\"Test'; DROP TABLE users; -- Vi pham noi quy chat\"}"
  ```
- **Response**:
  ```http
  HTTP/1.1 201 Created
  Content-Type: application/json

  {"penaltyId":12,"userId":2,"penaltyType":"WARNING","status":"ACTIVE","reason":"Test'; DROP TABLE users; -- Vi pham noi quy chat"}
  ```
- **Kết quả**: Chuỗi ký tự được lưu trữ an toàn dưới dạng string literal, không thực thi lệnh SQL phá hoại, cấu trúc bảng `users` được bảo toàn nguyên vẹn.
- **Status**: ✅ **PASSED**

---

### Test 4: XSS Prevention trong Admin Notes
- **Mục tiêu**: Kiểm tra dữ liệu chứa `<script>alert('XSS')</script>` khi lưu và render trên React UI không bị thực thi script.
- **Request**:
  ```bash
  curl -i -X PATCH http://localhost:8080/api/platform/reports/1 \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
    -d "{\"action\":\"DISMISS\",\"adminNotes\":\"<script>alert('XSS')</script> Báo cáo không có căn cứ xử lý\"}"
  ```
- **Kết quả**: React JSX tự động escape các thẻ HTML/Script khi render text node, hiển thị text thuần, không có bất kỳ JavaScript injection nào xảy ra.
- **Status**: ✅ **PASSED**

---

### Test 5a: Self-Penalty Creation Prevention
- **Mục tiêu**: Quản trị viên không thể tự tạo hình phạt áp dụng cho tài khoản của chính mình.
- **Request**:
  ```bash
  curl -i -X POST http://localhost:8080/api/platform/penalties \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <ADMIN_1_TOKEN>" \
    -d "{\"userId\":1,\"penaltyType\":\"WARNING\",\"reason\":\"Tự phạt bản thân vì thao tác nhầm lẫn trong ca trực\"}"
  ```
- **Response**:
  ```http
  HTTP/1.1 400 Bad Request
  Content-Type: application/json

  {"status":400,"error":"Bad Request","message":"Quản trị viên không thể tự áp dụng hình phạt."}
  ```
- **Status**: ✅ **PASSED**

---

### Test 5b: Self-Penalty Revocation Prevention
- **Mục tiêu**: Quản trị viên không thể tự thu hồi/hủy bỏ hình phạt đã được áp dụng lên tài khoản của chính mình.
- **Request**:
  ```bash
  curl -i -X POST http://localhost:8080/api/platform/penalties/99/revoke \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <ADMIN_1_TOKEN>" \
    -d "{\"revokedReason\":\"Quản trị viên tự gỡ bỏ hình phạt của mình\"}"
  ```
- **Response**:
  ```http
  HTTP/1.1 400 Bad Request
  Content-Type: application/json

  {"status":400,"error":"Bad Request","message":"Quản trị viên không thể tự thu hồi hình phạt của chính mình."}
  ```
- **Status**: ✅ **PASSED**
