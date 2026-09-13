# HƯỚNG DẪN KIỂM THỬ MANUAL CHI TIẾT: SHEET ST-PROF (ST-PROF-001 -> ST-PROF-006)

> **Dự án**: Tutor Connect System (TCS)  
> **Tester phụ trách**: DucHM  
> **Phạm vi kiểm thử**: Profile & Verification Module (ST-PROF-001 -> ST-PROF-006)  
> **Môi trường Web**:
> - Frontend: `http://localhost:5173`
> - Backend API: `http://localhost:8080`
> - Database: MySQL `tutorconnectsystem` trên `localhost:3306`

---

## 1. TỔNG QUAN DỮ LIỆU KIỂM THỬ & TÀI KHOẢN (TEST DATA)

Toàn bộ các tài khoản kiểm thử đã được khởi tạo sẵn trong Database với mật khẩu thống nhất:

| Email | Vai trò (Role) | Trạng thái ban đầu | Mật khẩu | Mục đích sử dụng |
| :--- | :--- | :--- | :--- | :--- |
| **`admin01@tcs.test`** | `PLATFORM_ADMIN` | `ACTIVE` | `12345678` | Duyệt / Từ chối yêu cầu xác minh |
| **`tutor02@tcs.test`** | `TUTOR` | `UNDER_VERIFY` | `12345678` | Nộp hồ sơ, Nộp lại khi bị từ chối, Test validation, Test trust-gate |
| **`center01@tcs.test`** | `TUTOR_CENTER` | `UNDER_VERIFY` | `12345678` | Nộp hồ sơ pháp lý trung tâm & kích hoạt vận hành |
| **`tutor01@tcs.test`** | `TUTOR` | `VERIFIED` | `12345678` | Khai báo lịch rảnh & kiểm tra lịch |

### Thư mục chứa File Test mẫu (`test_assets/`)
Hệ thống đã chuẩn bị sẵn các file test tại đường dẫn: `c:\Users\Admin\Documents\GitHub\Tutor-Connect-System-TCS-\test_assets\`
- `degree.pdf`: File PDF bằng cấp hợp lệ (316 bytes)
- `id.jpg`: File ảnh JPG CCCD hợp lệ (149 bytes)
- `business_license.pdf`: File PDF giấy phép kinh doanh hợp lệ (316 bytes)
- `virus.exe`: File thực thi giả lập đuôi `.exe` có header PE/MZ (dùng test chặn định dạng)
- `file_60MB.pdf`: File dung lượng 60MB (dùng test chặn quá dung lượng 10MB)

---

## 2. KỊCH BẢN KIỂM THỬ TỪNG TEST CASE (STEP-BY-STEP)

---

###  ST-PROF-001: Tutor submits documents and Admin approves

- **Mục tiêu**: Gia sư nộp hồ sơ xác minh danh tính và Admin duyệt thành công. Sau khi duyệt, trạng thái chuyển thành `VERIFIED`, tính năng ứng tuyển lớp mở ra và có thông báo gửi về.
- **Tài khoản**: `tutor02@tcs.test` & `admin01@tcs.test`
- **File sử dụng**: `id.jpg`, `degree.pdf`

#### Các bước thực hiện:
1. **Bước 1 (Tutor đăng nhập)**:
   - Truy cập: `http://localhost:5173/login`
   - Nhập `tutor02@tcs.test` / `12345678` -> Bấm **Đăng nhập**.
2. **Bước 2 (Mở form xác minh & Tải giấy tờ)**:
   - Truy cập: `http://localhost:5173/identity/verification`
   - Tại mục thông tin CCCD: Đã có sẵn dữ liệu định danh (`Gia Sư Kiểm Thử 02`, CCCD `001200000002`).
   - Tích chọn checkbox: *"Tôi đã kiểm tra và xác nhận thông tin CCCD quét được ở trên là chính xác"*.
   - Tại slot **CCCD/CMND mặt trước**: Bấm tải lên file `test_assets/id.jpg`.
   - Tại slot **CCCD/CMND mặt sau**: Bấm tải lên file `test_assets/id.jpg`.
   - Tại slot **Bằng cấp và chứng chỉ khác** (tùy chọn): Bấm tải lên file `test_assets/degree.pdf`.
3. **Bước 3 (Gửi yêu cầu)**:
   - Bấm nút **Gửi yêu cầu xác minh**.
   - Màn hình chuyển sang trạng thái **Đang chờ xét duyệt** (Badge vàng: `Đang chờ duyệt` - `SUBMITTED`).
4. **Bước 4 (Admin duyệt yêu cầu)**:
   - Đăng xuất tài khoản Tutor.
   - Đăng nhập bằng tài khoản `admin01@tcs.test` / `12345678`.
   - Truy cập trang kiểm duyệt Admin: `http://localhost:5173/platform/verifications`.
   - Tìm hồ sơ của `tutor02@tcs.test` (loại `Gia sư` / `TUTOR_PROFILE`), bấm vào xem chi tiết các file đã nộp.
   - Bấm nút **Phê duyệt** (Approve). Trạng thái hồ sơ chuyển sang `Đã duyệt` (VERIFIED).
5. **Bước 5 (Tutor kiểm tra kết quả & quyền lợi)**:
   - Đăng xuất Admin, đăng nhập lại bằng `tutor02@tcs.test`.
   - **Chuông thông báo**: Bấm vào icon quả chuông -> Nhận được thông báo *"Hồ sơ xác minh được duyệt"*.
   - **Trang xác minh**: Vào `http://localhost:5173/identity/verification` -> Hiển thị huy hiệu xanh **Đã xác minh** (`VERIFIED`).
   - **Tính năng mở khóa (Trust-gated)**:
     - Truy cập `http://localhost:5173/tim-yeu-cau-giang-day`.
     - Bấm vào một lớp học (vd: Lớp Toán 12 Cầu Giấy) -> Nút **Ứng tuyển** cho phép mở modal nộp đơn ứng tuyển bình thường.

#### Kết quả mong đợi:
- `verification_status` chuyển thành `VERIFIED`.
- Nhận được thông báo phê duyệt.
- Mở khóa tính năng ứng tuyển lớp học.

---

###  ST-PROF-002: Admin rejects documents, tutor resubmits

- **Mục tiêu**: Admin từ chối hồ sơ kèm lý do; Gia sư nhận được lý do, nộp lại phiên bản mới; Admin duyệt phiên bản mới và phiên bản cũ được đánh dấu vô hiệu hóa (superseded).
- **Tài khoản**: `admin01@tcs.test` & `tutor02@tcs.test`
- **File sử dụng**: `id.jpg`, `degree.pdf`
- **Dữ liệu test**: Lý do: `'Blurry degree, resubmit a clear copy'`

#### Các bước thực hiện:
1. **Bước 1 (Admin từ chối hồ sơ)**:
   - Đăng nhập `admin01@tcs.test` -> Truy cập `http://localhost:5173/platform/verifications`.
   - Chọn hồ sơ của `tutor02@tcs.test`.
   - Bấm **Từ chối** (Reject), nhập lý do: `Blurry degree, resubmit a clear copy`.
   - Xác nhận từ chối. Hồ sơ chuyển trạng thái `REJECTED`.
2. **Bước 2 (Tutor nhận lý do)**:
   - Đăng nhập `tutor02@tcs.test` / `12345678`.
   - Kiểm tra quả chuông thông báo -> Có thông báo: *"Hồ sơ xác minh bị từ chối. Lý do: Blurry degree, resubmit a clear copy..."*.
   - Vào `http://localhost:5173/identity/verification` -> Giao diện hiển thị trạng thái **Hồ sơ bị từ chối** kèm lý do, xuất hiện nút **Gửi lại yêu cầu xác minh**.
3. **Bước 3 (Tutor nộp lại hồ sơ)**:
   - Bấm nút **Gửi lại yêu cầu xác minh**.
   - Tải lên lại các giấy tờ mới rõ nét (`id.jpg`, `degree.pdf`).
   - Tích chọn xác nhận thông tin CCCD.
   - Bấm **Gửi yêu cầu xác minh**. Tạo thành công hồ sơ mới ở trạng thái `SUBMITTED`.
4. **Bước 4 (Admin duyệt phiên bản mới)**:
   - Đăng nhập lại `admin01@tcs.test` -> Vào `http://localhost:5173/platform/verifications`.
   - Mở hồ sơ phiên bản mới của `tutor02@tcs.test` -> Bấm **Phê duyệt** (Approve).
5. **Bước 5 (Xác nhận tính năng Superseded)**:
   - Đăng nhập lại `tutor02@tcs.test` -> Vào `http://localhost:5173/identity/verification`.
   - Xem bảng **Lịch sử xác minh**:
     - Hồ sơ mới nhất có trạng thái `Đã xác minh` (`VERIFIED`).
     - Hồ sơ cũ trước đó được đánh dấu là `Đã vô hiệu` (*"Hồ sơ này đã được thay thế bởi hồ sơ xác minh mới #..."*).

#### Kết quả mong đợi:
- Hồ sơ bị từ chối kèm lý do rõ ràng.
- Gia sư nộp lại được hồ sơ mới.
- Sau khi duyệt hồ sơ mới, hồ sơ cũ được đánh dấu superseded (vô hiệu hóa).

---

###  ST-PROF-003: Center submits a business license and is approved

- **Mục tiêu**: Trung tâm gia sư nộp các chứng từ pháp lý (Giấy phép ĐKKD, Giấy phép GD, MST, CCCD người đại diện), Admin duyệt và Trung tâm được kích hoạt các chức năng vận hành (Tạo lớp học, Tuyển gia sư).
- **Tài khoản**: `center01@tcs.test` & `admin01@tcs.test`
- **File sử dụng**: `business_license.pdf`, `id.jpg`, `degree.pdf`

#### Các bước thực hiện:
1. **Bước 1 (Center đăng nhập)**:
   - Truy cập: `http://localhost:5173/login`.
   - Nhập `center01@tcs.test` / `12345678` -> Bấm **Đăng nhập**.
2. **Bước 2 (Nộp 5 chứng từ pháp lý)**:
   - Truy cập: `http://localhost:5173/identity/verification`.
   - Tiêu đề trang: **Xác minh trung tâm gia sư**.
   - Thông tin CCCD người đại diện đã được nhận diện (`Nguyễn Văn Đại Diện`).
   - Tích chọn xác nhận thông tin CCCD người đại diện.
   - Tải file vào đủ 5 slot chứng từ pháp lý theo quy định TCS:
     1. *Giấy ĐKKD*: Tải `test_assets/business_license.pdf`.
     2. *Giấy phép hoạt động giáo dục*: Tải `test_assets/business_license.pdf`.
     3. *Mã số thuế / Đăng ký thuế*: Tải `test_assets/degree.pdf` (hoặc file PDF).
     4. *CCCD mặt trước người đại diện*: Tải `test_assets/id.jpg`.
     5. *CCCD mặt sau người đại diện*: Tải `test_assets/id.jpg`.
   - Bấm **Gửi yêu cầu xác minh**. Màn hình chuyển sang chờ xét duyệt (`SUBMITTED`).
3. **Bước 3 (Admin duyệt hồ sơ trung tâm)**:
   - Đăng nhập `admin01@tcs.test` -> Vào `http://localhost:5173/platform/verifications`.
   - Mở hồ sơ của `center01@tcs.test` (loại `TUTOR_CENTER_LICENSE`), kiểm tra 5 chứng từ -> Bấm **Phê duyệt** (Approve).
4. **Bước 4 (Kiểm tra kích hoạt vận hành)**:
   - Đăng nhập lại `center01@tcs.test`.
   - Nhận thông báo phê duyệt tại icon quả chuông.
   - Truy cập trang quản lý trung tâm: `http://localhost:5173/center`.
   - Bấm **Tạo lớp học mới** (hoặc tạo tin tuyển dụng gia sư): Form tạo lớp mở ra thành công, không còn bị chặn bởi lỗi `VerificationRequiredException`.

#### Kết quả mong đợi:
- Giấy phép trung tâm được phê duyệt.
- Trạng thái trung tâm chuyển sang `VERIFIED`.
- Mở khóa tính năng tạo lớp học và tuyển dụng gia sư cho trung tâm.

---

###  ST-PROF-004: Upload documents with wrong format / oversize / missing required

- **Mục tiêu**: Kiểm tra tính năng validation khi tải lên tài liệu: Chặn file sai định dạng (vd: `.exe`), chặn file quá dung lượng (vd: `60MB`), và chặn submit khi thiếu tài liệu bắt buộc.
- **Tài khoản**: `tutor02@tcs.test`
- **File sử dụng**: `virus.exe`, `file_60MB.pdf`

#### Các bước thực hiện:
- **Case A1 (Chặn sai định dạng - .exe)**:
  1. Tại trang nộp hồ sơ (`http://localhost:5173/identity/verification`), chọn một ô tải file bất kỳ.
  2. Chọn file `test_assets/virus.exe`.
  3. **Kết quả**: Hệ thống chặn và hiển thị thông báo lỗi: *"File type not allowed. Allowed: PDF, JPEG, PNG, WEBP"* (hoặc lỗi tải tệp). File không được đưa vào danh sách tải lên.
- **Case A2 (Chặn quá dung lượng - 60MB)**:
  1. Chọn ô tải file bất kỳ, chọn file `test_assets/file_60MB.pdf` (file 60MB vượt ngưỡng cho phép 10MB).
  2. **Kết quả**: Hệ thống chặn và hiển thị thông báo lỗi: *"File size exceeds 10MB limit"* (hoặc lỗi dung lượng vượt quá giới hạn). File không được đưa vào danh sách tải lên.
- **Case B (Chặn submit khi thiếu trường bắt buộc)**:
  1. Chỉ tải 1 file chứng chỉ tự chọn ở mục *"Bằng cấp và chứng chỉ khác"*, để trống 2 ô bắt buộc: CCCD mặt trước và CCCD mặt sau.
  2. Bấm nút **Gửi yêu cầu xác minh**.
  3. **Kết quả**: Form không cho gửi, hiển thị thông báo màu đỏ: *"Vui lòng tải lên: CCCD/CMND mặt trước, CCCD/CMND mặt sau."* Không có request xác minh mới nào được tạo trong Database.

#### Kết quả mong đợi:
- File `.exe` và file 60MB bị từ chối ngay lập tức kèm thông báo lỗi rõ ràng.
- Không thể submit hồ sơ nếu thiếu chứng từ bắt buộc.

---

###  ST-PROF-005: Unverified account attempts a trust-gated action

- **Mục tiêu**: Xác thực rằng tài khoản chưa xác minh danh tính sẽ bị chặn khi thực hiện các hành động cần độ tin cậy cao (Ứng tuyển lớp học, Nhận hợp đồng).
- **Tài khoản**: `tutor02@tcs.test` (khi chưa xác minh - `UNDER_VERIFY`).

#### Các bước thực hiện:
1. Đăng nhập với tài khoản `tutor02@tcs.test`.
2. Vào trang danh sách lớp học mở: `http://localhost:5173/tim-yeu-cau-giang-day`.
3. Bấm vào một lớp học để xem chi tiết (vd: Lớp Toán 12 Cầu Giấy).
4. Bấm vào nút **Ứng tuyển** (Apply to class).
5. **Kết quả**:
   - Hệ thống chặn hành động và hiển thị cảnh báo: *"Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp."*
   - Điều hướng hoặc hướng dẫn người dùng tới trang `http://localhost:5173/identity/verification`.
   - Kiểm tra trong Database: Không có bản ghi ứng tuyển (`tutor_applications`) nào được tạo.

#### Kết quả mong đợi:
- Thao tác ứng tuyển bị chặn hoàn toàn kèm thông báo yêu cầu xác minh danh tính. Không sinh bản ghi rác.

---

###  ST-PROF-006: Declare teaching availability and sync Google Calendar

- **Mục tiêu**: Khai báo lịch rảnh giảng dạy của gia sư, kiểm tra lưu khung giờ và đối chiếu tính năng đồng bộ Google Calendar.
- **Tài khoản**: `tutor01@tcs.test` / `12345678` (Gia sư `VERIFIED`).
- **Dữ liệu test**: Khung giờ Thứ Hai 18:00 - 20:00 (`Mon 18:00-20:00`).

#### Các bước thực hiện:
1. **Bước 1 (Khai báo lịch rảnh)**:
   - Đăng nhập bằng `tutor01@tcs.test` / `12345678`.
   - Xem lịch dạy / thời khóa biểu tại: `http://localhost:5173/tutor/schedule` (hoặc `/lich-ca-nhan`).
   - Khai báo khung giờ rảnh: Gửi yêu cầu lưu lịch rảnh Thứ Hai (`dayOfWeek = 2`, `startTime = 18:00`, `endTime = 20:00`).
2. **Bước 2 (Kiểm tra xung đột lịch - Conflict check)**:
   - Khi gia sư đã có ca dạy hoặc khung giờ cố định trùng với giờ của một lớp học khác (ví dụ cùng lúc Thứ Hai 18:00-20:00), hệ thống sẽ kích hoạt hàm kiểm tra xung đột lịch (`scheduleConflictOf`) và cảnh báo: *"Bạn đã có lịch dạy trùng với lịch của lớp này nên chưa thể ứng tuyển."*
3. **Bước 3 (Đánh giá đồng bộ Google Calendar)**:
   - *Thực tế triển khai trong mã nguồn hiện tại*:
     - Bảng `tutor_availabilities` đã có sẵn cột `google_calendar_event_id` trong DB.
     - Hệ thống đã hoàn thiện đăng nhập SSO Google (`/api/identity/google`).
     - Hướng dẫn FAQ (#76) mô tả tính năng đồng bộ lịch tại `/tutor/schedule`.
     - Tuy nhiên, tính năng đồng bộ hai chiều trực tiếp với Google Calendar API (Calendar REST API v3) hiện đang ở dạng kiến trúc dữ liệu / placeholder (chưa có credentials OAuth client riêng cho Google Calendar API).
   - *Ghi nhận kết quả test manual*:
     - Khai báo khung giờ và kiểm tra xung đột: **PASS**.
     - Đồng bộ Google Calendar: **Ghi chú Pending / Mapped Architecture** theo hiện trạng mã nguồn.

---

## 3. CÁC LỆNH SQL HỖ TRỢ NHANH DÀNH CHO TESTER

Khi kiểm thử manual, nếu tester cần reset trạng thái dữ liệu để test lại từ đầu, hãy chạy các lệnh tiện ích sau:

```sql
USE tutorconnectsystem;

-- 1. Reset tutor02 về trạng thái CHƯA XÁC MINH (để test lại ST-PROF-001 hoặc ST-PROF-005):
UPDATE tutors SET verification_status = 'UNDER_VERIFY' 
WHERE user_id = (SELECT user_id FROM users WHERE email = 'tutor02@tcs.test');

-- 2. Xóa các hồ sơ xác minh cũ của tutor02 để nộp lại form sạch:
DELETE FROM verification_documents WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = (SELECT user_id FROM users WHERE email = 'tutor02@tcs.test'));
DELETE FROM verification_histories WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = (SELECT user_id FROM users WHERE email = 'tutor02@tcs.test'));
DELETE FROM verification_requests WHERE user_id = (SELECT user_id FROM users WHERE email = 'tutor02@tcs.test');

-- 3. Reset center01 về trạng thái CHƯA XÁC MINH (để test lại ST-PROF-003):
UPDATE tutor_centers SET verification_status = 'UNDER_VERIFY' 
WHERE user_id = (SELECT user_id FROM users WHERE email = 'center01@tcs.test');
DELETE FROM verification_documents WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = (SELECT user_id FROM users WHERE email = 'center01@tcs.test'));
DELETE FROM verification_histories WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = (SELECT user_id FROM users WHERE email = 'center01@tcs.test'));
DELETE FROM verification_requests WHERE user_id = (SELECT user_id FROM users WHERE email = 'center01@tcs.test');
```
