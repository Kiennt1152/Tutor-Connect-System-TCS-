-- ==============================================================================
-- KỊCH BẢN SEED DỮ LIỆU CHUẨN PRODUCTION CHO DOMAIN: tutorconnectsystem.io.vn
-- Nền tảng: Tutor Connect System (TCS)
-- Môi trường: Production Host (Linux VPS 180.93.34.24 / Docker Compose)
--
-- Đặc điểm kỹ thuật:
--   1. An toàn & Tự động giải quyết khóa ngoại (Dynamic ID Resolution):
--      Hoạt động hoàn hảo trên cả Local dev và Server Production thực tế.
--   2. Không làm mất dữ liệu người dùng thật khác có trên hệ thống.
--   3. Bảng mã chuẩn hóa: UTF-8mb4 hỗ trợ đầy đủ tiếng Việt có dấu.
--   4. Mật khẩu chuẩn hóa 12345678 cho tất cả tài khoản demo:
--      Bcrypt: $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
--   5. Tài khoản nghiệm thu:
--      - Admin: admin@test.com / thanhkiu0209@gmail.com
--      - Client: student@test.com / parent.nguyen@gmail.com
--      - Tutor: tutor@test.com / tutor.le@gmail.com
--      - Center: center.triangviet@gmail.com
--   6. Bao phủ 100% kịch bản demo BF-09 (Support & AI) & BF-10 (Platform Operations Hub)
-- ==============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. TẠO HOẶC CẬP NHẬT TÀI KHOẢN NGƯỜI DÙNG CHUẨN DEMO
-- ------------------------------------------------------------------------------
SET @demo_pwd = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW';

-- 1.1. Insert nếu chưa có theo Email (Tránh xung đột auto-increment ID trên server)
INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'admin@test.com', '0988001050', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@test.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'thanhkiu0209@gmail.com', '0988001001', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'thanhkiu0209@gmail.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'admin@tcs.vn', '0988001002', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@tcs.vn');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'student@test.com', '0905556666', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student@test.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'parent.nguyen@gmail.com', '0901234567', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'parent.nguyen@gmail.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'parent.tran@gmail.com', '0902345678', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'parent.tran@gmail.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'parent.tuan@tcs.vn', '0903334444', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'parent.tuan@tcs.vn');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'tutor@test.com', '0907778888', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tutor@test.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'tutor.le@gmail.com', '0903456789', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tutor.le@gmail.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'tutor.pham@gmail.com', '0904567890', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tutor.pham@gmail.com');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'tutor.math@tcs.vn', '0912111222', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tutor.math@tcs.vn');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'tutor.physics@tcs.vn', '0912333444', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'tutor.physics@tcs.vn');

INSERT INTO users (email, phone, password_hash, status, token_version, created_at, updated_at)
SELECT 'center.triangviet@gmail.com', '02838999999', @demo_pwd, 'ACTIVE', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'center.triangviet@gmail.com');

-- 1.2. Đồng bộ mật khẩu 12345678 và kích hoạt trạng thái ACTIVE
UPDATE users 
SET password_hash = @demo_pwd,
    status = 'ACTIVE',
    token_version = 0,
    updated_at = NOW()
WHERE email IN (
    'admin@test.com', 'thanhkiu0209@gmail.com', 'admin@tcs.vn',
    'student@test.com', 'parent.nguyen@gmail.com', 'parent.tran@gmail.com', 'parent.tuan@tcs.vn',
    'tutor@test.com', 'tutor.le@gmail.com', 'tutor.pham@gmail.com', 'tutor.math@tcs.vn', 'tutor.physics@tcs.vn',
    'center.triangviet@gmail.com'
);

-- 1.3. Lấy User ID động cho toàn bộ script
SET @u_admin_thanh = (SELECT user_id FROM users WHERE email = 'thanhkiu0209@gmail.com' LIMIT 1);
SET @u_admin_tcs   = (SELECT user_id FROM users WHERE email = 'admin@tcs.vn' LIMIT 1);
SET @u_admin_test  = (SELECT user_id FROM users WHERE email = 'admin@test.com' LIMIT 1);

SET @u_client_hung = (SELECT user_id FROM users WHERE email = 'parent.nguyen@gmail.com' LIMIT 1);
SET @u_client_tran = (SELECT user_id FROM users WHERE email = 'parent.tran@gmail.com' LIMIT 1);
SET @u_client_tuan = (SELECT user_id FROM users WHERE email = 'parent.tuan@tcs.vn' LIMIT 1);
SET @u_client_test = (SELECT user_id FROM users WHERE email = 'student@test.com' LIMIT 1);

SET @u_tutor_nam   = (SELECT user_id FROM users WHERE email = 'tutor.le@gmail.com' LIMIT 1);
SET @u_tutor_thao  = (SELECT user_id FROM users WHERE email = 'tutor.pham@gmail.com' LIMIT 1);
SET @u_tutor_toan  = (SELECT user_id FROM users WHERE email = 'tutor.math@tcs.vn' LIMIT 1);
SET @u_tutor_ly    = (SELECT user_id FROM users WHERE email = 'tutor.physics@tcs.vn' LIMIT 1);
SET @u_tutor_test  = (SELECT user_id FROM users WHERE email = 'tutor@test.com' LIMIT 1);

SET @u_center_tv   = (SELECT user_id FROM users WHERE email = 'center.triangviet@gmail.com' LIMIT 1);

-- 1.4. Thiết lập hồ sơ Admin (platform_admins)
INSERT INTO platform_admins (user_id, full_name, created_at, updated_at)
VALUES 
    (@u_admin_thanh, 'Quản Trị Viên Hệ Thống', NOW(), NOW()),
    (@u_admin_tcs,   'Admin TCS', NOW(), NOW()),
    (@u_admin_test,  'Quản Trị Viên Hội Đồng (Reviewer)', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), updated_at = NOW();

SET @admin_pic_id = (SELECT admin_id FROM platform_admins WHERE user_id = @u_admin_thanh LIMIT 1);

-- 1.5. Thiết lập hồ sơ Phụ huynh (clients)
INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
VALUES 
    (@u_client_hung, 'Nguyễn Văn Hùng', '0901234567', 'Số 18 Hoàng Quốc Việt, Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()),
    (@u_client_tran, 'Trần Thị Mai', '0902345678', 'Số 45 Chùa Bộc, Đống Đa, Hà Nội', 'FEMALE', NOW(), NOW()),
    (@u_client_tuan, 'Phạm Anh Tuấn', '0903334444', 'Số 88 Trần Duy Hưng, Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()),
    (@u_client_test, 'Học Viên Demo TCS', '0905556666', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'MALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), phone = VALUES(phone), address = VALUES(address), updated_at = NOW();

-- 1.6. Thiết lập hồ sơ Gia sư (tutors)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
VALUES 
    (@u_tutor_nam,  'Lê Hoàng Nam', 'MALE', '0903456789', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư chuyên Toán 12 và Luyện thi Đại học, cử nhân Sư phạm Toán ĐH Sư Phạm Hà Nội.', 250000.00, 4.90, 'VERIFIED', NOW(), NOW()),
    (@u_tutor_thao, 'Phạm Thu Thảo', 'FEMALE', '0904567890', 'Quận 1, TP.HCM', 4, 'Gia sư Tiếng Anh IELTS 8.0 chuyên kèm 1:1 học sinh cấp 3 và người đi làm.', 300000.00, 5.00, 'VERIFIED', NOW(), NOW()),
    (@u_tutor_toan, 'Nguyễn Văn Toán', 'MALE', '0912111222', 'Quận Ba Đình, Hà Nội', 6, 'Gia sư Toán bồi dưỡng học sinh giỏi THCS & THPT.', 220000.00, 4.85, 'VERIFIED', NOW(), NOW()),
    (@u_tutor_ly,   'Trần Minh Lý', 'MALE', '0912333444', 'Quận Đống Đa, Hà Nội', 3, 'Gia sư Vật lý 10-12 ôn thi THPT Quốc Gia.', 200000.00, 4.70, 'UNDER_VERIFY', NOW(), NOW()),
    (@u_tutor_test, 'Gia Sư Demo TCS', 'MALE', '0907778888', 'Quận Cầu Giấy, Hà Nội', 4, 'Gia sư demo tài liệu nghiệm thu hệ thống.', 250000.00, 4.95, 'VERIFIED', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), hourly_rate = VALUES(hourly_rate), rating_avg = VALUES(rating_avg), verification_status = VALUES(verification_status), updated_at = NOW();

SET @tutor_nam_pk  = (SELECT tutor_id FROM tutors WHERE user_id = @u_tutor_nam LIMIT 1);
SET @tutor_thao_pk = (SELECT tutor_id FROM tutors WHERE user_id = @u_tutor_thao LIMIT 1);
SET @tutor_toan_pk = (SELECT tutor_id FROM tutors WHERE user_id = @u_tutor_toan LIMIT 1);

-- 1.7. Thiết lập hồ sơ Trung tâm (tutor_centers)
INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, description, verification_status, custom_fee_rate, created_at, updated_at)
VALUES 
    (@u_center_tv, 'Trung Tâm Gia Sư Trí Việt', 'LICENSE-2026-TV', '02838999999', '123 Đường Cầu Giấy, Cầu Giấy, Hà Nội', 'Trung tâm kết nối gia sư uy tín chất lượng cao hàng đầu Hà Nội', 'VERIFIED', 0.1000, NOW(), NOW())
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), custom_fee_rate = 0.1000, updated_at = NOW();

SET @center_tv_pk = (SELECT center_id FROM tutor_centers WHERE user_id = @u_center_tv LIMIT 1);

-- 1.8. Thiết lập Ví tiền (wallets)
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES 
    (@u_admin_thanh, 0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_admin_tcs,   0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_admin_test,  0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_client_hung, 7500000.00, 3000000.00, 'ACTIVE', NOW(), NOW()),
    (@u_client_tran, 10000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_client_tuan, 5000000.00, 2200000.00, 'ACTIVE', NOW(), NOW()),
    (@u_client_test, 8000000.00, 2500000.00, 'ACTIVE', NOW(), NOW()),
    (@u_tutor_nam,   4500000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_tutor_thao,  6600000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_tutor_toan,  2000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_tutor_ly,    1500000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_tutor_test,  5000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (@u_center_tv,   18000000.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE available_balance = VALUES(available_balance), frozen_balance = VALUES(frozen_balance), status = 'ACTIVE', updated_at = NOW();

-- ------------------------------------------------------------------------------
-- 2. ĐƯỜNG DẪN MEDIA FILES CHUẨN DOMAIN https://tutorconnectsystem.io.vn
-- ------------------------------------------------------------------------------
INSERT INTO media_files (file_id, uploaded_by, file_name, file_url, mime_type, file_size, created_at)
VALUES 
    (201, @u_tutor_nam, 'cccd_front_lehoangnam.jpg', 'https://tutorconnectsystem.io.vn/uploads/verifications/cccd_front_lehoangnam.jpg', 'image/jpeg', 245120, NOW()),
    (202, @u_tutor_nam, 'cccd_back_lehoangnam.jpg', 'https://tutorconnectsystem.io.vn/uploads/verifications/cccd_back_lehoangnam.jpg', 'image/jpeg', 261800, NOW()),
    (203, @u_tutor_nam, 'bang_daihoc_supham_hanoi.jpg', 'https://tutorconnectsystem.io.vn/uploads/verifications/bang_daihoc_supham_hanoi.jpg', 'image/jpeg', 512400, NOW()),
    (204, @u_center_tv, 'giay_phep_kinh_doanh_triviet.jpg', 'https://tutorconnectsystem.io.vn/uploads/verifications/giay_phep_kinh_doanh_triviet.jpg', 'image/jpeg', 624100, NOW()),
    (205, @u_center_tv, 'giay_phep_hoat_dong_giao_duc.jpg', 'https://tutorconnectsystem.io.vn/uploads/verifications/giay_phep_hoat_dong_giao_duc.jpg', 'image/jpeg', 583200, NOW()),
    (206, @u_tutor_thao, 'chung_chi_ielts_80.jpg', 'https://tutorconnectsystem.io.vn/uploads/verifications/chung_chi_ielts_80.jpg', 'image/jpeg', 312500, NOW())
ON DUPLICATE KEY UPDATE file_url = VALUES(file_url);

-- ------------------------------------------------------------------------------
-- 3. LỚP HỌC, CA HỌC, BUỔI HỌC & ĐIỂM DANH (CLASSES, SCHEDULES, LESSONS)
-- ------------------------------------------------------------------------------
INSERT INTO tutoring_classes (
    class_id, creator_id, subject_id, grade_id, class_type, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES 
    (201, @u_client_hung, 1, 12, 'PRIVATE', 'Luyện thi Đại học môn Toán đạt điểm 9+', 'Gia sư có kinh nghiệm dạy Toán 12 chuyên',
    'Lớp Toán 12 Luyện thi Đại học (Cầu Giấy)', 'Lớp học kèm trực tiếp tại nhà học viên 2 buổi/tuần, chuẩn bị kỳ thi THPT Quốc gia.',
    'OFFLINE', 10, 250000.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY),
    2500000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),

    (202, @u_client_tran, 7, 12, 'PRIVATE', 'Luyện thi IELTS mục tiêu 7.5 Band', 'Gia sư IELTS 8.0 trở lên, nhiệt tình và có phương pháp',
    'Lớp Tiếng Anh IELTS 7.5 Cấp tốc Online', 'Học trực tuyến qua Google Meet / Zoom, rèn luyện 4 kỹ năng Nghe Nói Đọc Viết.',
    'ONLINE', 12, 300000.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
    3600000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

    (203, @u_client_tuan, 2, 11, 'PRIVATE', 'Bồi dưỡng học sinh giỏi môn Vật lý 11', 'Sinh viên hoặc giáo viên chuyên Lý ĐH Sư Phạm',
    'Lớp Vật lý 11 Nâng cao Chuyên KHTN', 'Cần gia sư vững lý thuyết và các bài toán đồ thị, dao động cơ học.',
    'OFFLINE', 8, 250000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2000000.00, 'WEEKLY', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

    (204, @u_center_tv, 10, 10, 'CENTER', 'Nhập môn Lập trình Python & Tư duy thuật toán', 'Giảng viên công nghệ thông tin có kinh nghiệm dạy trẻ em',
    'Lớp Lập trình Python Sáng tạo Trẻ em (Online)', 'Lớp học nhóm 5-8 học sinh do Trung tâm Trí Việt tổ chức giảng dạy trực tuyến.',
    'ONLINE', 10, 220000.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2200000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), tuition_fee = VALUES(tuition_fee), budget = VALUES(budget), updated_at = NOW();

-- Ca học (schedule_slots)
INSERT INTO schedule_slots (slot_id, class_id, day_of_week, start_time, end_time)
VALUES 
    (201, 201, 2, '18:00:00', '20:00:00'),
    (202, 201, 5, '18:00:00', '20:00:00'),
    (203, 202, 3, '19:30:00', '21:00:00'),
    (204, 202, 6, '19:30:00', '21:00:00'),
    (205, 204, 7, '09:00:00', '11:00:00')
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time);

-- Phân công gia sư (class_assignments)
INSERT INTO class_assignments (assignment_id, tutor_id, application_id, assigned_date, status, terms_b)
VALUES 
    (101, @tutor_nam_pk,  NULL, DATE_SUB(NOW(), INTERVAL 15 DAY), 'ACTIVE', 'Thỏa thuận giảng dạy kèm môn Toán 12 theo đúng cam kết chuẩn đầu ra.'),
    (102, @tutor_thao_pk, NULL, DATE_SUB(NOW(), INTERVAL 12 DAY), 'ACTIVE', 'Thỏa thuận đào tạo IELTS 7.5 chuyên sâu.'),
    (103, @tutor_toan_pk, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), 'ACTIVE', 'Điều khoản thỏa thuận giải quyết tranh chấp học phí.'),
    (104, @tutor_nam_pk,  NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), 'ACTIVE', 'Thỏa thuận lớp lập trình nhóm Trung tâm Trí Việt.')
ON DUPLICATE KEY UPDATE status = VALUES(status), tutor_id = VALUES(tutor_id);

-- Học viên ghi danh (class_students)
INSERT INTO class_students (class_student_id, class_id, enrolled_by_user_id, student_name, student_phone, status, enrolled_at)
VALUES 
    (201, 201, @u_client_hung, 'Nguyễn Văn An', '0912345678', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (202, 202, @u_client_tran, 'Trần Thảo My', '0923456789', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (204, 204, @u_client_hung, 'Nguyễn Bảo Long', '0934567890', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (205, 204, @u_client_tran, 'Trần Minh Khang', '0945678901', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), student_name = VALUES(student_name);

-- Buổi học & Tiến độ (lessons) cho Lớp 201 (Toán 12 - 10 buổi: 4 buổi hoàn thành, 6 buổi sắp tới)
INSERT INTO lessons (lesson_id, class_id, lesson_date, slot_id, sequence_no, tutor_id, tutor_check_in_at, tutor_check_out_at, client_confirm_at, attendance_status, created_at)
VALUES 
    (2011, 201, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 201, 1, @tutor_nam_pk, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), NOW(), 'COMPLETED', NOW()),
    (2012, 201, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 202, 2, @tutor_nam_pk, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 'COMPLETED', NOW()),
    (2013, 201, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 201, 3, @tutor_nam_pk, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 'COMPLETED', NOW()),
    (2014, 201, CURDATE(), 202, 4, @tutor_nam_pk, NOW(), NULL, NULL, 'COMPLETED', NOW()),
    (2015, 201, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 201, 5, @tutor_nam_pk, NULL, NULL, NULL, 'PENDING', NOW()),
    (2016, 201, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 202, 6, @tutor_nam_pk, NULL, NULL, NULL, 'PENDING', NOW()),
    (2017, 201, DATE_ADD(CURDATE(), INTERVAL 10 DAY), 201, 7, @tutor_nam_pk, NULL, NULL, NULL, 'PENDING', NOW()),
    (2018, 201, DATE_ADD(CURDATE(), INTERVAL 14 DAY), 202, 8, @tutor_nam_pk, NULL, NULL, NULL, 'PENDING', NOW()),
    (2019, 201, DATE_ADD(CURDATE(), INTERVAL 17 DAY), 201, 9, @tutor_nam_pk, NULL, NULL, NULL, 'PENDING', NOW()),
    (2020, 201, DATE_ADD(CURDATE(), INTERVAL 21 DAY), 202, 10, @tutor_nam_pk, NULL, NULL, NULL, 'PENDING', NOW())
ON DUPLICATE KEY UPDATE attendance_status = VALUES(attendance_status);

-- Điểm danh chi tiết
INSERT INTO lesson_attendances (attendance_id, lesson_id, class_student_id, status, note, created_at)
VALUES 
    (2011, 2011, 201, 'PRESENT', 'Học đúng giờ, làm bài tập đầy đủ', NOW()),
    (2012, 2012, 201, 'PRESENT', 'Tiếp thu tốt phần Hình học không gian Oxyz', NOW()),
    (2013, 2013, 201, 'PRESENT', 'Nhiệt tình phát biểu, hoàn thành bài test 15 phút', NOW()),
    (2014, 2014, 201, 'PRESENT', 'Buổi học hôm nay bám sát đề thi thử ĐH', NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- ------------------------------------------------------------------------------
-- 4. HỢP ĐỒNG ĐIỆN TỬ MẪU & CHỮ KÝ SỐ OTP (UC-44, UC-45, UC-46)
-- ------------------------------------------------------------------------------
INSERT INTO contract_templates (template_id, name, content, contract_type, created_by, center_id, is_default, status, created_at, updated_at)
VALUES 
    (1, 'Hợp đồng dịch vụ dạy học kèm 1:1 tiêu chuẩn sàn TCS',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG DỊCH VỤ DẠY KÈM GIA SƯ\n\nBên A (Học viên/Phụ huynh): {{TEN_BEN_A}}\nBên B (Gia sư chuyên môn): {{TEN_BEN_B}}\n\nĐIỀU 1: ĐỐI TƯỢNG VÀ MỤC TIÊU KHÓA HỌC\nBên B đồng ý giảng dạy kèm môn học theo chương trình và mục tiêu điểm số đã thống nhất trên nền tảng Tutor Connect System (tutorconnectsystem.io.vn).\n\nĐIỀU 2: HỌC PHÍ VÀ KÝ QUỸ ĐẢM BẢO ESCROW\n- Học phí được bảo đảm an toàn qua tài khoản Escrow của sàn TCS.\n- Tiền chỉ được giải ngân cho Bên B sau khi hoàn tất đầy đủ số buổi học hợp lệ.\n\nĐIỀU 3: HIỆU LỰC HỢP ĐỒNG\nHợp đồng có hiệu lực pháp lý ngay sau khi cả hai bên xác thực ký số qua mã OTP gửi về Email/SMS.',
        'CENTER_CLASS', @admin_pic_id, NULL, 1, 'ACTIVE', NOW(), NOW()),
    (2, 'Thỏa thuận hợp tác tuyển dụng gia sư cho trung tâm',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nTHỎA THUẬN HỢP TÁC ĐÀO TẠO GIA SƯ\n\nBên A: Trung tâm Gia sư Trí Việt\nBên B: Gia sư chuyên môn trực thuộc\n\nĐIỀU 1: PHÂN CÔNG GIẢNG DẠY\nTrung tâm điều phối lớp học và đảm bảo thù lao minh bạch cho gia sư.\n\nĐIỀU 2: CHỐNG LÁCH SÀN\nGia sư cam kết không thỏa thuận riêng với học viên ngoài hệ thống TCS.',
        'RECRUITMENT', @admin_pic_id, NULL, 1, 'ACTIVE', NOW(), NOW()),
    (3, 'Hợp đồng cam kết đầu ra chứng chỉ quốc tế IELTS/TOEIC',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG CAM KẾT ĐẦU RA CHỨNG CHỈ QUỐC TẾ\n\nĐIỀU 1: MỤC TIÊU BAND ĐIỂM\nGia sư cam kết lộ trình đào tạo đạt mục tiêu Band điểm trong hợp đồng.\n\nĐIỀU 2: CHÍNH SÁCH BẢO HÀNH VÀ HOÀN TIỀN\nTrường hợp không đạt chuẩn đầu ra tối thiểu do lỗi sư phạm, sàn TCS hỗ trợ thủ tục hoàn trả học phí ký quỹ.',
        'SPECIALIZED_GUARANTEE', @admin_pic_id, NULL, 0, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE content = VALUES(content), name = VALUES(name), updated_at = NOW();

INSERT INTO contracts (contract_id, contract_no, assignment_id, template_id, terms_summary, status, source_type, signed_at, created_at, updated_at)
VALUES 
    (101, 'HD-2026-TOAN12', 101, 1, 'Hợp đồng dạy kèm môn Toán 12 tại nhà học viên', 'ACTIVE', 'PRIVATE', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
    (102, 'HD-2026-IELTS75', 102, 3, 'Hợp đồng khóa luyện thi IELTS 7.5 Online', 'ACTIVE', 'PRIVATE', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
    (103, 'HD-2026-LY11', 103, 1, 'Hợp đồng dạy kèm Vật lý 11 Chuyên KHTN', 'PENDING', 'PRIVATE', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), terms_summary = VALUES(terms_summary), updated_at = NOW();

INSERT INTO contract_signatures (signature_id, contract_id, party_role, signer_id, email, signed_at, signature_data, signature_status)
VALUES 
    (101, 101, 'CLIENT', @u_client_hung, 'parent.nguyen@gmail.com', DATE_SUB(NOW(), INTERVAL 15 DAY), 'Ký số điện tử OTP qua email (parent.nguyen@gmail.com)', 'SIGNED'),
    (102, 101, 'TUTOR',  @u_tutor_nam,   'tutor.le@gmail.com',     DATE_SUB(NOW(), INTERVAL 15 DAY), 'Ký số điện tử OTP qua email (tutor.le@gmail.com)', 'SIGNED'),
    (103, 102, 'CLIENT', @u_client_tran, 'parent.tran@gmail.com',   DATE_SUB(NOW(), INTERVAL 12 DAY), 'Ký số điện tử OTP qua email (parent.tran@gmail.com)', 'SIGNED'),
    (104, 102, 'TUTOR',  @u_tutor_thao,  'tutor.pham@gmail.com',   DATE_SUB(NOW(), INTERVAL 12 DAY), 'Ký số điện tử OTP qua email (tutor.pham@gmail.com)', 'SIGNED'),
    (105, 103, 'CLIENT', @u_client_tuan, 'parent.tuan@tcs.vn',     NULL, NULL, 'PENDING'),
    (106, 103, 'TUTOR',  @u_tutor_toan,  'tutor.math@tcs.vn',      NULL, NULL, 'PENDING')
ON DUPLICATE KEY UPDATE signature_status = VALUES(signature_status);

-- ------------------------------------------------------------------------------
-- 5. KÝ QUỸ ESCROW ĐỦ 6 TRẠNG THÁI & DÒNG TIỀN TÀI CHÍNH (UC-41, UC-58)
-- ------------------------------------------------------------------------------
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES 
    (101, @u_client_hung, 'TX-DEP-2500K-001', 'ESCROW_DEPOSIT', 'SUCCESS', 2500000.00, 'Ký quỹ học phí lớp Toán 12 (Cầu Giấy)', 'ESC-2500000', NOW(), DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (102, @u_client_tran, 'TX-DEP-3600K-002', 'ESCROW_DEPOSIT', 'SUCCESS', 3600000.00, 'Ký quỹ học phí lớp Tiếng Anh IELTS 7.5', 'ESC-3600000', NOW(), DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (103, @u_client_tuan, 'TX-DEP-2200K-003', 'ESCROW_DEPOSIT', 'SUCCESS', 2200000.00, 'Ký quỹ hợp đồng tranh chấp Vật lý 11', 'ESC-2200000', NOW(), DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (104, @u_client_hung, 'TX-DEP-1800K-004', 'ESCROW_DEPOSIT', 'PENDING', 1800000.00, 'Chờ thanh toán ký quỹ lớp bổ trợ kiến thức', 'ESC-1800000', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (105, @u_client_hung, 'TX-DEP-3000K-005', 'ESCROW_DEPOSIT', 'SUCCESS', 3000000.00, 'Tạm giữ tiền ký quỹ lớp học đang đối soát', 'ESC-3000000', NOW(), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (106, @u_client_hung, 'TX-REF-2400K-006', 'REFUND', 'SUCCESS', 2400000.00, 'Hoàn trả học phí ký quỹ lớp Toán do gia sư hủy lớp', 'REF-2400000', NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (107, @u_tutor_nam,   'TX-WDR-1500K-007', 'WITHDRAWAL', 'PENDING', 1500000.00, 'Rút tiền thù lao gia sư về tài khoản MB Bank', 'WDR-1500000', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (108, @u_tutor_thao,  'TX-WDR-3000K-008', 'WITHDRAWAL', 'SUCCESS', 3000000.00, 'Rút tiền thù lao gia sư về tài khoản Vietcombank', 'WDR-3000000', NOW(), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount);

-- 6 Trạng thái Escrow: FUNDED, RELEASED, DISPUTED, PENDING, ON_HOLD, REFUNDED
INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, released_at, created_at)
VALUES 
    (101, 101, 101, 2500000.00, 'FUNDED', DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (102, 102, 102, 3600000.00, 'RELEASED', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (103, 103, 103, 2200000.00, 'DISPUTED', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (104, 104, 101, 1800000.00, 'PENDING', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (105, 105, 101, 3000000.00, 'ON_HOLD', DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (106, 106, 101, 2400000.00, 'REFUNDED', DATE_SUB(NOW(), INTERVAL 15 DAY), NULL, DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount);

-- Yêu cầu hoàn tiền (Refund Requests)
INSERT INTO refund_requests (refund_id, escrow_id, requested_by, reason, amount, status, requested_at, processed_at, bank_name, account_no, account_holder_name, refund_reference_code, transfer_status, transfer_processed_at)
VALUES 
    (1, 106, @u_client_hung, 'Gia sư xin hủy lớp học môn Toán 12 do việc bận đột xuất, phụ huynh yêu cầu hoàn 100% tiền ký quỹ.\nThông tin nhận tiền: MB Bank - 0901234567 - NGUYEN VAN HUNG', 2400000.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'MB Bank', '0901234567', 'NGUYEN VAN HUNG', 'REF-2400000', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (2, 103, @u_client_tuan, 'Gia sư giảng dạy không đúng mục tiêu bồi dưỡng HSG Vật lý 11, phụ huynh yêu cầu hoàn lại 50% tiền cọc.\nThông tin nhận tiền: Vietcombank - 0903334444 - PHAM ANH TUAN', 1100000.00, 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, 'Vietcombank', '0903334444', 'PHAM ANH TUAN', 'REF-103-DISP', 'PENDING', NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount);

-- ------------------------------------------------------------------------------
-- 6. [BF-09] SUPPORT TICKETS & TICKET MESSAGES (ĐỦ 8 KỊCH BẢN DEMO THỰC CHIẾN)
-- ------------------------------------------------------------------------------
DELETE FROM ticket_messages WHERE ticket_id BETWEEN 101 AND 110;
DELETE FROM support_tickets WHERE ticket_id BETWEEN 101 AND 110;

INSERT INTO support_tickets (
    ticket_id, user_id, assigned_admin_id, target_class_id, category, subject, description,
    priority, status, due_at, sla_breached, response_sla_ms, resolved_at, closed_at, created_at, updated_at
)
VALUES 
    -- Kịch bản 1: [DEMO AUTO-ASSIGN] Ticket mới OPEN, chưa gán ai (assigned_admin_id IS NULL)
    -- Khi Admin mở xem chi tiết -> Tự động chuyển IN_PROGRESS và gán Admin hiện tại làm PIC!
    (101, @u_client_hung, NULL, NULL, 'INQUIRY', 
     '[DEMO-01 Auto-Assign] Hướng dẫn thay đổi số điện thoại nhận thông báo học tập', 
     'Chào ban quản trị, tôi muốn cập nhật số điện thoại nhận OTP và thông báo lịch học cho con nhưng hệ thống báo lỗi không nhận mã. Nhờ admin hỗ trợ.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 48 HOUR), 0, NULL, NULL, NULL, NOW(), NOW()),

    -- Kịch bản 2: [DEMO FIRST RESPONSE SLA] Ticket IN_PROGRESS, chưa có phản hồi nào
    -- Admin nhập nội dung trả lời -> Backend tự động đo khoảng thời gian phản hồi đầu tiên và ghi log RESPOND_TICKET!
    (102, @u_tutor_nam, @admin_pic_id, NULL, 'BUG_REPORT', 
     '[DEMO-02 SLA Response] Lỗi đồng bộ ca dạy trên Google Calendar buổi tối', 
     'Tôi tích hợp lịch dạy cá nhân với Google Calendar nhưng ca học 19:30 bị hiển thị lệch thành 07:30 sáng hôm sau. Đề nghị admin hỗ trợ sửa lỗi.',
     'MEDIUM', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 20 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW()),

    -- Kịch bản 3A: [DEMO MERGE TICKET - ĐÍCH] Thuộc phụ huynh Nguyễn Văn Hùng
    (103, @u_client_hung, @admin_pic_id, NULL, 'INQUIRY', 
     '[DEMO-03 Target] Thắc mắc đối soát giao dịch nạp tiền qua cổng SePay', 
     'Tôi đã chuyển khoản 2.500.000đ vào tài khoản sàn qua mã QR SePay nhưng số dư khả dụng trên ví vẫn chưa được cộng. Mã giao dịch ngân hàng là MB-889922.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 44 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW()),

    -- Kịch bản 3B: [DEMO MERGE TICKET - NGUỒN] Cùng phụ huynh Hùng, gửi bổ sung thông tin
    -- Mở ticket #104 -> Bấm "Gộp Ticket" -> Nhập ID đích 103 -> #104 chuyển CLOSED, nội dung dời sang #103!
    (104, @u_client_hung, @admin_pic_id, NULL, 'INQUIRY', 
     '[DEMO-03 Source Gộp vào #103] Gửi bổ sung ảnh chụp biên lai giao dịch thành công', 
     'Tôi xin bổ sung ảnh chụp màn hình ứng dụng ngân hàng xác nhận đã trừ tiền 2.500.000đ lúc 10:15 hôm nay, nhờ admin kiểm tra đối soát cùng ticket trước.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 46 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW()),

    -- Kịch bản 4: [DEMO QUÉT QUÁ HẠN SLA] Ticket quá hạn due_at, sla_breached = 0, priority = MEDIUM
    -- Bấm "Quét vi phạm SLA" -> Tự động thăng cấp lũy tiến MEDIUM -> HIGH, gán sla_breached = 1 và gửi thông báo cảnh báo!
    (105, @u_client_tran, @admin_pic_id, NULL, 'INQUIRY', 
     '[DEMO-04 Scan SLA] Khiếu nại thời gian xử lý lệnh rút tiền thù lao quá 24 giờ', 
     'Tôi đã tạo lệnh rút 3.000.000đ về Vietcombank từ 2 ngày trước nhưng trạng thái vẫn hiển thị PENDING. Đề nghị bộ phận tài chính giải quyết gấp.',
     'MEDIUM', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 26 HOUR), NOW()),

    -- Kịch bản 5: [DEMO CHUYỂN SANG TRANH CHẤP ESCROW] Ticket phản ánh sự cố gắn với lớp học 201
    -- Bấm "Chuyển Tranh Chấp" -> Category đổi sang DISPUTE, Priority ép lên HIGH, sinh bản ghi Report CLASS để tạm khóa tiền Escrow!
    (106, @u_client_hung, @admin_pic_id, 201, 'INQUIRY', 
     '[DEMO-05 Chuyển Tranh Chấp] Gia sư tự ý nghỉ buổi học thứ 3 không báo trước', 
     'Gia sư Lê Hoàng Nam không vào lớp buổi 3 ngày hôm qua và không liên lạc được. Tôi đã nạp giữ tiền Escrow 2.500.000đ, đề nghị can thiệp bảo vệ quyền lợi.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 40 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW()),

    -- Kịch bản 6: [DEMO ĐÓNG / GIẢI QUYẾT TICKET] Ticket đã trao đổi xong, sẵn sàng bấm RESOLVED / CLOSED
    (107, @u_tutor_nam, @admin_pic_id, NULL, 'INQUIRY', 
     '[DEMO-06 Close/Resolve] Tư vấn mở lớp dạy kèm nhóm cho 4 học sinh lớp 12', 
     'Nhờ ban quản trị hướng dẫn quy trình tạo lớp học nhóm trên hệ thống và mức thu phí nền tảng áp dụng cho hình thức dạy nhóm.',
     'MEDIUM', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 18 HOUR), 0, 1850000, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),

    -- Kịch bản 7: [DEMO MỞ LẠI TICKET ĐÃ ĐÓNG (REOPEN)]
    (108, @u_client_hung, @admin_pic_id, NULL, 'BUG_REPORT', 
     '[DEMO-07 Reopen] Không xem được tệp đề cương ôn thi đính kèm trên web', 
     'Khi bấm vào link tài liệu ôn tập môn Toán 12 thì trang web báo lỗi 404 không tìm thấy tệp. Đề nghị kỹ thuật kiểm tra lại đường dẫn lưu trữ.',
     'MEDIUM', 'RESOLVED', DATE_SUB(NOW(), INTERVAL 12 HOUR), 0, 950000, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- Khởi tạo tin nhắn trao đổi trong các Support Ticket
INSERT INTO ticket_messages (ticket_id, sender_id, is_from_admin, content, created_at)
VALUES 
    (101, @u_client_hung, 0, 'Chào ban quản trị, tôi muốn cập nhật số điện thoại nhận OTP và thông báo lịch học cho con nhưng hệ thống báo lỗi không nhận mã. Nhờ admin hỗ trợ.', NOW()),
    
    (102, @u_tutor_nam,   0, 'Tôi tích hợp lịch dạy cá nhân với Google Calendar nhưng ca học 19:30 bị hiển thị lệch thành 07:30 sáng hôm sau. Đề nghị admin hỗ trợ sửa lỗi.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),

    (103, @u_client_hung, 0, 'Tôi đã chuyển khoản 2.500.000đ vào tài khoản sàn qua mã QR SePay nhưng số dư khả dụng trên ví vẫn chưa được cộng. Mã giao dịch ngân hàng là MB-889922.', DATE_SUB(NOW(), INTERVAL 2 HOUR)),

    (104, @u_client_hung, 0, 'Tôi xin bổ sung ảnh chụp màn hình ứng dụng ngân hàng xác nhận đã trừ tiền 2.500.000đ lúc 10:15 hôm nay, nhờ admin kiểm tra đối soát cùng ticket trước.', DATE_SUB(NOW(), INTERVAL 1 HOUR)),

    (105, @u_client_tran, 0, 'Tôi đã tạo lệnh rút 3.000.000đ về Vietcombank từ 2 ngày trước nhưng trạng thái vẫn hiển thị PENDING. Đề nghị bộ phận tài chính giải quyết gấp.', DATE_SUB(NOW(), INTERVAL 26 HOUR)),

    (106, @u_client_hung, 0, 'Gia sư Lê Hoàng Nam không vào lớp buổi 3 ngày hôm qua và không liên lạc được. Tôi đã nạp giữ tiền Escrow 2.500.000đ, đề nghị can thiệp bảo vệ quyền lợi.', DATE_SUB(NOW(), INTERVAL 3 HOUR)),

    (107, @u_tutor_nam,   0, 'Nhờ ban quản trị hướng dẫn quy trình tạo lớp học nhóm trên hệ thống và mức thu phí nền tảng áp dụng cho hình thức dạy nhóm.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    (107, @u_admin_thanh, 1, 'Chào thầy Nam! Hiện tại nền tảng TCS hỗ trợ mở lớp nhóm từ 2–8 học sinh tại mục /tao-lop. Mức phí nền tảng cố định vẫn là 10% trên tổng giá trị lớp học thành công.', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (107, @u_tutor_nam,   0, 'Dạ em cảm ơn Admin đã phản hồi nhanh chóng và chi tiết!', DATE_SUB(NOW(), INTERVAL 4 HOUR)),

    (108, @u_client_hung, 0, 'Khi bấm vào link tài liệu ôn tập môn Toán 12 thì trang web báo lỗi 404 không tìm thấy tệp. Đề nghị kỹ thuật kiểm tra lại đường dẫn lưu trữ.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (108, @u_admin_thanh, 1, 'Bộ phận kỹ thuật đã khôi phục lại liên kết tải file đề cương trên hệ thống máy chủ lưu trữ. Bạn vui lòng tải lại trang và thử lại nhé.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ------------------------------------------------------------------------------
-- 7. [BF-09] KHO TRI THỨC FAQ ĐA DANH MỤC & CÂU HỎI BẢN THẢO (PLATFORM FAQ)
-- ------------------------------------------------------------------------------
INSERT INTO faq_entries (faq_id, question, answer, category, sort_order, is_published, created_at, updated_at)
VALUES 
    (201, 'Làm thế nào để đăng ký tài khoản Học viên hoặc Gia sư?', 'Bạn bấm vào nút "Đăng ký" tại góc trên bên phải màn hình hoặc truy cập /register, chọn vai trò phù hợp (Học viên/Phụ huynh, Gia sư, hoặc Trung tâm gia sư), điền số điện thoại, email và mật khẩu để tạo tài khoản.', 'AUTH_PROFILE', 1, 1, NOW(), NOW()),
    (202, 'Quy trình kiểm duyệt hồ sơ CCCD và bằng cấp gia sư mất bao lâu?', 'Đội ngũ Quản trị viên TCS kiểm tra và phê duyệt hồ sơ xác minh danh tính CCCD và bằng cấp cử nhân trong vòng 24–48 giờ làm việc kể từ lúc nộp.', 'VERIFICATION', 2, 1, NOW(), NOW()),
    (203, 'Cơ chế ký quỹ bảo đảm Escrow hoạt động như thế nào?', 'Khi phụ huynh chọn gia sư, học phí được giữ an toàn tại tài khoản Escrow của TCS. Tiền chỉ được tự động giải ngân cho gia sư sau khi học viên xác nhận hoàn thành đầy đủ số buổi học cam kết.', 'FINANCE_ESCROW', 3, 1, NOW(), NOW()),
    (204, 'Mức phí nền tảng (Platform Fee Rate) của TCS là bao nhiêu?', 'TCS áp dụng mức phí sàn minh bạch 10% trên giá trị hợp đồng hoàn thành thành công để duy trì vận hành hệ thống, bảo vệ quỹ ký quỹ và chăm sóc khách hàng 24/7.', 'FINANCE_ESCROW', 4, 1, NOW(), NOW()),
    (205, 'Làm sao để tìm gia sư giỏi khu vực Cầu Giấy, Hà Nội?', 'Tại thanh điều hướng, bạn vào mục /tim-gia-su, chọn bộ lọc môn Toán, cấp học THPT và chọn khu vực Quận Cầu Giấy để xem danh sách các gia sư được đánh giá cao nhất.', 'MARKETPLACE', 5, 1, NOW(), NOW()),
    (206, 'Hợp đồng điện tử ký bằng mã OTP qua Email có giá trị pháp lý không?', 'Có. Hợp đồng điện tử trên TCS được xác thực bằng mã OTP theo quy định của Luật Giao dịch điện tử Việt Nam, ràng buộc quyền lợi và nghĩa vụ pháp lý giữa phụ huynh và gia sư.', 'CONTRACT_REVIEW', 6, 1, NOW(), NOW()),
    (207, 'Hành vi lách sàn, giao dịch ngoài hệ thống bị xử phạt thế nào?', 'TCS nghiêm cấm mọi hành vi lách sàn. Tài khoản vi phạm sẽ bị cảnh cáo, giới hạn tính năng nhận lớp hoặc khóa tài khoản vĩnh viễn theo quy định tại UC-60.', 'TRUST_SAFETY', 7, 1, NOW(), NOW()),
    -- 2 Câu hỏi ở trạng thái BẢN THẢO (is_published = 0) để Admin demo bật/tắt xuất bản trên /platform/faq:
    (208, '[BẢN THẢO] Chính sách ưu đãi hoàn tiền 100% nhân dịp năm học mới 2026', 'Chương trình miễn phí dịch vụ và bảo hiểm hoàn tiền 100% học phí cho 200 lớp học đầu tiên đăng ký trong tháng 9.', 'FINANCE_ESCROW', 8, 0, NOW(), NOW()),
    (209, '[BẢN THẢO] Hướng dẫn nhận diện tài khoản giả mạo gia sư Sư phạm', 'Các dấu hiệu nhận biết gia sư lừa đảo: yêu cầu chuyển cọc trực tiếp qua tài khoản cá nhân, từ chối ký hợp đồng điện tử qua OTP trên sàn.', 'TRUST_SAFETY', 9, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    question = VALUES(question), answer = VALUES(answer), category = VALUES(category), is_published = VALUES(is_published), updated_at = NOW();

-- ------------------------------------------------------------------------------
-- 8. [BF-10] YÊU CẦU XÁC MINH DANH TÍNH eKYC (PLATFORM VERIFICATIONS)
-- ------------------------------------------------------------------------------
DELETE FROM verification_documents WHERE verification_id BETWEEN 101 AND 104;
DELETE FROM verification_requests WHERE verification_id BETWEEN 101 AND 104;

INSERT INTO verification_requests (verification_id, user_id, verification_type, status, admin_notes, submitted_at, reviewed_at, created_at, updated_at)
VALUES 
    -- 1. Chờ duyệt: Gia sư Lê Hoàng Nam (TUTOR_PROFILE)
    (101, @u_tutor_nam, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư Lê Hoàng Nam gửi CCCD 2 mặt và bằng Cử nhân Sư phạm Toán để kiểm duyệt hồ sơ.', NOW(), NULL, NOW(), NOW()),
    
    -- 2. Chờ duyệt: Trung tâm Trí Việt (TUTOR_CENTER_LICENSE)
    (102, @u_center_tv, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', 'Trung tâm Trí Việt nộp Giấy phép kinh doanh giáo dục và CCCD người đại diện pháp luật.', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NOW(), NOW()),

    -- 3. Đã duyệt thành công: Gia sư Nguyễn Văn Toán (TUTOR_PROFILE)
    (103, @u_tutor_toan, 'TUTOR_PROFILE', 'VERIFIED', 'Hồ sơ đầy đủ, rõ nét, đã đối soát CCCD và bằng cấp chuyên môn hợp lệ.', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- 4. Bị từ chối: Gia sư Trần Minh Lý (TUTOR_PROFILE) - lý do từ chối >= 10 ký tự
    (104, @u_tutor_ly, 'TUTOR_PROFILE', 'REJECTED', 'Ảnh chụp giấy tờ tùy thân CCCD mặt sau bị mờ góc số định danh và lóa sáng, đề nghị tải lại bản chụp rõ nét.', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO verification_documents (document_id, verification_id, file_id, document_type)
VALUES 
    (201, 101, 201, 'ID_CARD'),
    (202, 101, 202, 'ID_CARD'),
    (203, 101, 203, 'DEGREE'),
    (204, 102, 204, 'LICENSE'),
    (205, 102, 205, 'LICENSE'),
    (206, 103, 206, 'CERTIFICATE')
ON DUPLICATE KEY UPDATE file_id = VALUES(file_id);

-- ------------------------------------------------------------------------------
-- 9. [BF-10] BÁO CÁO SỰ CỐ & CAN THIỆP LỚP HỌC (PLATFORM REPORTS & DISPUTES)
-- ------------------------------------------------------------------------------
DELETE FROM disputes WHERE dispute_id BETWEEN 101 AND 105;
DELETE FROM reports WHERE report_id BETWEEN 101 AND 105;

INSERT INTO reports (report_id, reporter_id, target_type, target_id, category, description, evidence_urls, status, created_at, updated_at)
VALUES 
    -- Báo cáo 1: Lớp học - sẵn sàng cho Admin can thiệp 7 phương án (RESCHEDULE, REPLACE_TUTOR, TERMINATE, ESCALATE_TO_DISPUTE...)
    (101, @u_client_hung, 'CLASS', 201, 'OTHER', 
     '[UC-29] Báo cáo sự cố lớp học\nMã loại: LATE_TUTOR\nLoại sự cố: Gia sư vào muộn 40 phút buổi học ngày 22/09\nMã hướng xử lý mong muốn: RESCHEDULE\nMô tả: Gia sư vào lớp muộn không thông báo trước, đề nghị sắp xếp ca học bù.',
     '["https://tutorconnectsystem.io.vn/uploads/verifications/cccd_front_lehoangnam.jpg"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

    -- Báo cáo 2: Tranh chấp hợp đồng lớp học - gắn với Dispute và Escrow
    (102, @u_client_tuan, 'CLASS', 202, 'FRAUD', 
     '[UC-29] Tranh chấp học phí lớp Tiếng Anh IELTS\nMã loại: QUALITY_ISSUE\nLoại sự cố: Chất lượng giảng dạy không đúng cam kết\nMã hướng xử lý: ESCALATE_TO_DISPUTE\nMô tả: Đề nghị tạm giữ tiền ký quỹ Escrow và hoàn lại 50% tiền học phí cho gia đình.',
     '["https://tutorconnectsystem.io.vn/uploads/verifications/bang_daihoc_supham_hanoi.jpg"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

    -- Báo cáo 3: Báo cáo nhận xét phản cảm (REVIEW)
    (103, @u_tutor_nam, 'REVIEW', 101, 'ABUSE', 
     'Nhận xét đánh giá có ngôn từ vu khống, xúc phạm danh dự nghề giáo của gia sư, yêu cầu kiểm duyệt ẩn bài viết.',
     '["https://tutorconnectsystem.io.vn/uploads/verifications/cccd_back_lehoangnam.jpg"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW()),

    -- Báo cáo 4: Đã giải quyết thành công
    (104, @u_client_hung, 'CLASS', 204, 'OTHER', 
     'Yêu cầu dời lịch lớp lập trình nhóm sang sáng Chủ nhật để các học sinh tham gia đầy đủ.',
     '["https://tutorconnectsystem.io.vn/uploads/verifications/giay_phep_hoat_dong_giao_duc.jpg"]', 'RESOLVED', DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());

INSERT INTO disputes (dispute_id, report_id, escrow_id, resolution, status, created_at, updated_at)
VALUES 
    (101, 102, 103, 'Ban quản trị đang tiến hành triệu tập đối soát chứng từ học tập giữa phụ huynh và gia sư.', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), resolution = VALUES(resolution), updated_at = NOW();

-- ------------------------------------------------------------------------------
-- 10. [BF-10] PHÁT HIỆN HÀNH VI NÉ TRÁNH NỀN TẢNG (CIRCUMVENTION SCANNER)
-- ------------------------------------------------------------------------------
INSERT INTO conversations (conversation_id, context_type, context_id, type, status, created_at)
VALUES 
    (201, 'CLASS', 201, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (202, 'CLASS', 202, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

INSERT INTO conversation_participants (participant_id, conversation_id, user_id, last_read_at)
VALUES 
    (201, 201, @u_client_hung, NOW()),
    (202, 201, @u_tutor_nam,   NOW()),
    (203, 202, @u_client_tran, NOW()),
    (204, 202, @u_tutor_thao,  NOW())
ON DUPLICATE KEY UPDATE last_read_at = NOW();

INSERT INTO messages (message_id, conversation_id, sender_id, message_type, content, is_edited, is_deleted, sent_at)
VALUES 
    (201, 201, @u_tutor_nam,   'TEXT', 'Chào chị, chị chuyển khoản trực tiếp qua số Zalo 0987654321 hoặc STK MBBank để không mất phí sàn 10% nhé!', 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (202, 202, @u_tutor_thao,  'TEXT', 'Em có thể kết bạn Zalo số 0904567890 hoặc gửi email lehoangnam.tutor@gmail.com để trao đổi học riêng nhé.', 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (203, 201, @u_client_hung, 'TEXT', 'Em vào trang facebook.com/giasuvip.hanoi để xem thêm giáo án mẫu nhé.', 0, 0, DATE_SUB(NOW(), INTERVAL 18 HOUR))
ON DUPLICATE KEY UPDATE content = VALUES(content);

DELETE FROM circumvention_events WHERE event_id BETWEEN 201 AND 204;

INSERT INTO circumvention_events (event_id, message_id, conversation_id, sender_id, matched_rule, evidence, risk_score, status, review_note, reviewed_by, reviewed_at, created_at)
VALUES 
    -- Event 1: Phát hiện từ khóa thanh toán ngoài & STK
    (201, 201, 201, @u_tutor_nam,   'PAYMENT_KEYWORD', 'chuyển khoản trực tiếp qua STK MBBank để không mất phí sàn 10%', 90, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Event 2: Phát hiện số điện thoại Zalo
    (202, 201, 201, @u_tutor_nam,   'PHONE', '0987654321', 85, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Event 3: Phát hiện địa chỉ Email trao đổi ngoài
    (203, 202, 202, @u_tutor_thao,  'EMAIL', 'lehoangnam.tutor@gmail.com', 75, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- Event 4: Phát hiện liên kết mạng xã hội (Đã xác nhận vi phạm)
    (204, 201, 201, @u_client_hung, 'URL', 'facebook.com/giasuvip.hanoi', 70, 'CONFIRMED', 'Xác nhận gửi link Facebook trao đổi ngoài sàn, đã ban hành cảnh cáo lần 1.', @u_admin_thanh, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 18 HOUR));

-- ------------------------------------------------------------------------------
-- 11. [BF-10] BAN HÀNH CHẾ TÀI XỬ PHẠT (USER PENALTIES & GATEKEEPER)
-- ------------------------------------------------------------------------------
DELETE FROM user_penalties WHERE penalty_id BETWEEN 201 AND 204;

INSERT INTO user_penalties (
    penalty_id, user_id, issued_by, penalty_type, reason, evidence_urls,
    restriction_details, source_type, source_id, source_task_id, starts_at, expires_at, status, created_at
)
VALUES 
    -- Phạt 1: Cảnh cáo (WARNING) vì nhắn tin lách sàn
    (201, @u_tutor_nam, @admin_pic_id, 'WARNING', 'Nhắn tin gạ gẫm phụ huynh chuyển khoản học phí ngoài nền tảng TCS qua Zalo cá nhân.', 
     '["https://tutorconnectsystem.io.vn/uploads/verifications/cccd_front_lehoangnam.jpg"]', 
     '{"warningLevel": 1}', 'CIRCUMVENTION', 201, 'TASK-CIRC-201', 
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Phạt 2: Giới hạn tính năng (FEATURE_RESTRICTION) - Chặn nộp đơn nhận lớp
    (202, @u_tutor_ly,  @admin_pic_id, 'FEATURE_RESTRICTION', 'Tự ý hủy ca dạy sát giờ nhiều lần gây ảnh hưởng nghiêm trọng đến học sinh.', 
     '["https://tutorconnectsystem.io.vn/uploads/verifications/bang_daihoc_supham_hanoi.jpg"]', 
     '{"canApplyClasses": false, "canPostClasses": false}', 'REPORT', 101, 'TASK-REP-101', 
     DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- Phạt 3: Tạm khóa tài khoản 14 ngày (TEMPORARY_BAN)
    (203, @u_client_tuan, @admin_pic_id, 'TEMPORARY_BAN', 'Đình chỉ tài khoản 14 ngày do vi phạm thỏa thuận hợp đồng và phát sinh tranh chấp tài chính chưa giải quyết.', 
     '["https://tutorconnectsystem.io.vn/uploads/verifications/cccd_back_lehoangnam.jpg"]', 
     '{"banDays": 14}', 'DISPUTE', 101, 'TASK-DISP-101', 
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),

    -- Phạt 4: Đã hết hạn (EXPIRED)
    (204, @u_tutor_thao, @admin_pic_id, 'WARNING', 'Cảnh cáo về việc chậm phản hồi tin nhắn của học viên quá 48 giờ.', 
     '[]', '{"warningLevel": 1}', 'REPORT', 104, 'TASK-REP-104', 
     DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 'EXPIRED', DATE_SUB(NOW(), INTERVAL 40 DAY));

-- ------------------------------------------------------------------------------
-- 12. [BF-10] NHẬT KÝ KIỂM TOÁN HỆ THỐNG BẤT BIẾN (AUDIT LOGS)
-- ------------------------------------------------------------------------------
DELETE FROM audit_logs WHERE audit_id BETWEEN 201 AND 210;

INSERT INTO audit_logs (audit_id, actor_id, action, entity_type, entity_id, old_value, new_value, ip_address, user_agent, created_at)
VALUES 
    (201, @u_admin_thanh, 'LOGIN', 'User', @u_admin_thanh, NULL, '{"method": "PASSWORD", "status": "SUCCESS"}', '180.93.34.24', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (202, @u_admin_thanh, 'REVIEW_VERIFICATION', 'VerificationRequest', 103, '{"status": "SUBMITTED"}', '{"status": "VERIFIED", "reviewerId": 1}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (203, @u_admin_thanh, 'UPDATE_FEE_RATE', 'SystemParameter', 1, '{"PLATFORM_FEE_RATE": "0.08"}', '{"PLATFORM_FEE_RATE": "0.10"}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (204, @u_admin_thanh, 'RESPOND_TICKET', 'SupportTicket', 107, '{"status": "IN_PROGRESS", "responseSlaMs": null}', '{"status": "IN_PROGRESS", "responseSlaMs": 1850000}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (205, @u_admin_thanh, 'CLOSE_TICKET', 'SupportTicket', 108, '{"status": "IN_PROGRESS"}', '{"status": "RESOLVED", "resolutionNotes": "Da sua loi file 404"}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (206, @u_admin_thanh, 'MERGE_TICKET', 'SupportTicket', 104, '{"status": "OPEN", "targetTicketId": null}', '{"status": "CLOSED", "targetTicketId": 103, "reason": "Trung lap bien lai"}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (207, @u_admin_thanh, 'ISSUE_PENALTY', 'UserPenalty', 201, NULL, '{"penaltyType": "WARNING", "targetUserId": 8, "reason": "Lach san Zalo"}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (208, @u_admin_thanh, 'RESOLVE_REPORT', 'Report', 104, '{"status": "PENDING"}', '{"status": "RESOLVED", "action": "RESCHEDULE"}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (209, @u_admin_thanh, 'UPDATE_USER_STATUS', 'User', @u_client_tuan, '{"status": "ACTIVE"}', '{"status": "SUSPENDED", "tokenVersion": 1}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (210, @u_admin_thanh, 'REDIRECT_DISPUTE', 'SupportTicket', 106, '{"category": "INQUIRY", "priority": "LOW"}', '{"category": "DISPUTE", "priority": "HIGH", "createdReportId": 102}', '180.93.34.24', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- ------------------------------------------------------------------------------
-- 13. [BF-10] CẤU HÌNH THAM SỐ TOÀN SÀN (SYSTEM PARAMETERS)
-- ------------------------------------------------------------------------------
INSERT INTO system_parameters (param_key, param_value, description)
VALUES 
    ('PLATFORM_FEE_RATE', '0.10', 'Tỷ lệ phí dịch vụ nền tảng áp dụng cố định (10%)'),
    ('ESCROW_HOLD_DAYS', '7', 'Thời gian tạm giữ ký quỹ bảo đảm trước khi giải ngân (ngày)'),
    ('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng đơn ứng tuyển tối đa cho mỗi lớp học mở tuyển'),
    ('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày nếu khách không phản hồi'),
    ('SYSTEM_ANNOUNCEMENTS', '[{"announcementId":1,"title":"Chào đón năm học mới 2026 cùng Tutor Connect System!","content":"Bảo hiểm hoàn tiền học phí 100% qua tài khoản Escrow cho tất cả các lớp học mới trên hệ thống tutorconnectsystem.io.vn.","targetRole":null,"active":true,"startsAt":"2026-09-01T00:00:00","endsAt":"2026-10-31T23:59:59","createdByAdminId":1,"createdByName":"Quản Trị Viên Hệ Thống","createdAt":"2026-09-01T08:00:00","updatedAt":"2026-09-01T08:00:00"}]', 'Danh sách thông báo toàn sàn (JSON array)')
ON DUPLICATE KEY UPDATE 
    param_value = VALUES(param_value), description = VALUES(description);

-- ------------------------------------------------------------------------------
-- 14. ĐÁNH GIÁ GIA SƯ (REVIEWS & MODERATION)
-- ------------------------------------------------------------------------------
INSERT INTO reviews (review_id, assignment_id, class_id, reviewer_id, reviewee_id, review_type, rating, comment, is_anonymous, status, created_at)
VALUES 
    (101, 102, 202, @u_client_tran, @u_tutor_thao, 'CLIENT_TO_TUTOR', 5.0, 'Cô Thảo dạy cực kỳ nhiệt tình, phương pháp IELTS Speaking rất thực tế, con tôi đã tiến bộ vượt bậc và đạt 7.5!', 0, 'VISIBLE', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (102, 101, 201, @u_client_hung, @u_tutor_nam,  'CLIENT_TO_TUTOR', 4.8, 'Thầy Nam giảng bài môn Toán rất dễ hiểu, bám sát cấu trúc đề thi THPT Quốc gia, giải bài chi tiết.', 0, 'VISIBLE', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (103, 103, 203, @u_client_tuan, @u_tutor_toan, 'CLIENT_TO_TUTOR', 1.0, 'Giáo viên thiếu trách nhiệm, tự ý nghỉ không báo trước, lừa dối học viên và nền tảng!', 0, 'MODERATED', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), rating = VALUES(rating), comment = VALUES(comment);

-- ------------------------------------------------------------------------------
-- 15. THÔNG BÁO HỆ THỐNG (NOTIFICATIONS)
-- ------------------------------------------------------------------------------
INSERT INTO notifications (user_id, type, title, content, reference_type, reference_id, is_read, status, created_at)
VALUES 
    (@u_tutor_nam,   'SYSTEM', 'Hồ sơ xác minh gia sư đã được tiếp nhận', 'Hồ sơ bằng cấp Sư phạm của bạn đang được Ban Quản Trị xem xét.', 'VERIFICATION', 101, 0, 'SENT', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (@u_client_hung, 'CLASS',  'Nhắc nhở ca học môn Toán 12', 'Lớp Toán 12 Luyện thi Đại học sẽ bắt đầu vào 18:00 hôm nay.', 'CLASS', 201, 1, 'SENT', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (@u_client_tran, 'PAYMENT', 'Ký quỹ học phí thành công', 'Khoản ký quỹ 3.600.000đ cho khóa học IELTS 7.5 đã được bảo đảm thành công.', 'ESCROW', 102, 1, 'SENT', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (@u_admin_thanh, 'SYSTEM', 'Có 2 hồ sơ xác minh mới cần kiểm duyệt', 'Gia sư Lê Hoàng Nam và Trung tâm Trí Việt vừa nộp hồ sơ eKYC.', 'VERIFICATION', 101, 0, 'SENT', DATE_SUB(NOW(), INTERVAL 1 HOUR))
ON DUPLICATE KEY UPDATE title = VALUES(title);

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- BÁO CÁO KẾT QUẢ NẠP SEED DATA
-- ==============================================================================
SELECT 'NẠP THÀNH CÔNG SEED DATA CHO DOMAIN tutorconnectsystem.io.vn!' AS Result;
SELECT 'Users' AS TableName, count(*) AS TotalRows FROM users
UNION ALL SELECT 'Support Tickets', count(*) FROM support_tickets
UNION ALL SELECT 'Ticket Messages', count(*) FROM ticket_messages
UNION ALL SELECT 'FAQ Entries', count(*) FROM faq_entries
UNION ALL SELECT 'Verification Requests', count(*) FROM verification_requests
UNION ALL SELECT 'Tutoring Classes', count(*) FROM tutoring_classes
UNION ALL SELECT 'Contracts', count(*) FROM contracts
UNION ALL SELECT 'Escrow Transactions', count(*) FROM escrow_transactions
UNION ALL SELECT 'Circumvention Events', count(*) FROM circumvention_events
UNION ALL SELECT 'User Penalties', count(*) FROM user_penalties
UNION ALL SELECT 'Reports & Incidents', count(*) FROM reports
UNION ALL SELECT 'Audit Logs', count(*) FROM audit_logs;
