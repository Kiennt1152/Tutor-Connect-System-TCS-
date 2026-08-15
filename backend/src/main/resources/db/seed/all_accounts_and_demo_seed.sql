-- ====================================================================
-- MASTER SEED SCRIPT: TOÀN BỘ TÀI KHOẢN & DỮ LIỆU DEMO HỆ THỐNG TCS
-- Chứa đầy đủ: Admin, Phụ huynh, Gia sư, Trung tâm, Lớp học, Ví tiền,
-- Giao dịch Escrow, Báo cáo vi phạm, Tranh chấp, Khiếu nại SLA, FAQs.
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;

-- --------------------------------------------------------------------
-- 1. BẢNG USERS (Tất cả tài khoản hệ thống)
-- Hash BCrypt cho 'ducminh1011': $2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS
-- Hash BCrypt cho '12345678':   $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
-- --------------------------------------------------------------------
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
-- 1.1 Quản trị viên (Platform Admin) - MK: 12345678
('thanhkiu0209@gmail.com', '0909999999', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
('admin@tcs.vn', '0908888888', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),

-- 1.2 Phụ huynh / Học viên tùy chọn mới - MK: ducminh1011
('haehuynh35@gmail.com', '0912345678', '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),

-- 1.3 Gia sư tùy chọn mới - MK: ducminh1011
('minhduc101dz@gmail.com', '0987654321', '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 45 DAY), NOW()),

-- 1.4 Phụ huynh mẫu - MK: 12345678
('parent.nguyen@gmail.com', '0901234567', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 40 DAY), NOW()),
('parent.tran@gmail.com', '0902345678', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 35 DAY), NOW()),
('parent.tuan@tcs.vn', '0903334444', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),

-- 1.5 Gia sư mẫu - MK: 12345678
('tutor.le@gmail.com', '0903456789', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 50 DAY), NOW()),
('tutor.pham@gmail.com', '0904567890', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 45 DAY), NOW()),


ON DUPLICATE KEY UPDATE 
    password_hash = VALUES(password_hash),
    status = 'ACTIVE',
    updated_at = NOW();


-- --------------------------------------------------------------------
-- 2. PHÂN QUYỀN PLATFORM ADMINS
-- --------------------------------------------------------------------
INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Quản Trị Viên Hệ Thống' FROM users WHERE email = 'thanhkiu0209@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Quản Trị Viên Hệ Thống';

INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Admin TCS' FROM users WHERE email = 'admin@tcs.vn'
ON DUPLICATE KEY UPDATE full_name = 'Admin TCS';


-- --------------------------------------------------------------------
-- 3. HỒ SƠ CLIENTS (Phụ huynh / Học viên)
-- --------------------------------------------------------------------
INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Huỳnh Đức Minh (Phụ Huynh)', '0912345678', 'Quận Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()
FROM users WHERE email = 'haehuynh35@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Nguyễn Văn Hùng', '0901234567', 'Quận Ba Đình, Hà Nội', 'MALE', NOW(), NOW()
FROM users WHERE email = 'parent.nguyen@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Trần Thị Mai', '0902345678', 'Quận Đống Đa, Hà Nội', 'FEMALE', NOW(), NOW()
FROM users WHERE email = 'parent.tran@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Phạm Anh Tuấn', '0903334444', 'Quận Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()
FROM users WHERE email = 'parent.tuan@tcs.vn'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);


-- --------------------------------------------------------------------
-- 4. HỒ SƠ TUTORS (Gia sư)
-- --------------------------------------------------------------------
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Minh Đức (Gia Sư Toán & Tin Học)', 'MALE', '0987654321', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư chuyên Toán 12 và Luyện thi Đại học khu vực Cầu Giấy.', 250000.00, 5.00, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'minhduc101dz@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Lê Hoàng Nam', 'MALE', '0903456789', 'Quận Cầu Giấy, Hà Nội', 5, 'Chuyên dạy Toán cấp 3 luyện thi đại học khu vực Cầu Giấy', 250000.00, 4.90, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'tutor.le@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Phạm Thu Thảo', 'FEMALE', '0904567890', 'Quận 1, TP.HCM', 4, 'Gia sư Tiếng Anh IELTS 8.0 chuyên lớp 10-12', 300000.00, 5.00, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'tutor.pham@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Nguyễn Văn Toán', 'MALE', '0912111222', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư Toán luyện thi học sinh giỏi cấp Quốc gia', 220000.00, 4.90, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'tutor.math@tcs.vn'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';




-- --------------------------------------------------------------------
-- 5. HỒ SƠ TRUNG TÂM GIA SƯ
-- --------------------------------------------------------------------
INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, description)
SELECT user_id, 'Trung Tâm Gia Sư Trí Việt', 'LICENSE-2026-TV', '02838999999', '123 Đường Cầu Giấy, Hà Nội', 'Trung tâm kết nối gia sư uy tín chất lượng cao hàng đầu Hà Nội'
FROM users WHERE email = 'center.triangviet@gmail.com'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);


-- --------------------------------------------------------------------
-- 6. VÍ TIỀN WALLETS CHO CÁC TÀI KHOẢN
-- --------------------------------------------------------------------
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
SELECT user_id, 5000000.00, 0.00, 'ACTIVE', NOW(), NOW() FROM users WHERE email IN ('haehuynh35@gmail.com', 'parent.nguyen@gmail.com', 'parent.tuan@tcs.vn')
ON DUPLICATE KEY UPDATE available_balance = 5000000.00, status = 'ACTIVE';

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
SELECT user_id, 3000000.00, 0.00, 'ACTIVE', NOW(), NOW() FROM users WHERE email IN ('minhduc101dz@gmail.com', 'tutor.le@gmail.com', 'tutor.pham@gmail.com', 'tutor.math@tcs.vn')
ON DUPLICATE KEY UPDATE available_balance = 3000000.00, status = 'ACTIVE';


-- --------------------------------------------------------------------
-- 7. THIẾT LẬP THAM SỐ HỆ THỐNG & FAQS
-- --------------------------------------------------------------------
INSERT INTO system_parameters (param_key, param_value, description) VALUES 
('PLATFORM_FEE_RATE', '0.10', 'Phí nền tảng (10%)'),
('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng ứng tuyển tối đa per class'),
('ESCROW_HOLD_DAYS', '7', 'Số ngày tạm giữ tiền ký quỹ'),
('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày không phản hồi')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);

INSERT IGNORE INTO faq_entries (faq_id, question, answer, category, sort_order, is_published) VALUES
(1, 'Làm sao để tìm gia sư phù hợp?', 'Bạn có thể sử dụng bộ lọc theo môn học, cấp độ, khu vực và mức giá. Hệ thống AI sẽ đề xuất các gia sư phù hợp nhất với yêu cầu của bạn.', 'GENERAL', 1, 1),
(2, 'Chính sách hoàn tiền như thế nào?', 'Hoàn 100% nếu hủy trước 24h, 50% nếu hủy trước 12h. Sau khi lớp bắt đầu không được hoàn tiền trừ trường hợp tranh chấp có cơ sở.', 'PAYMENT', 2, 1),
(3, 'Phí nền tảng là bao nhiêu?', 'Nền tảng thu 10% phí trên mỗi giao dịch thành công. Phí này được tự động trừ khi giải ngân từ escrow cho gia sư.', 'PAYMENT', 3, 1);


-- --------------------------------------------------------------------
-- 8. TICKET HỖ TRỢ, YÊU CẦU XÁC MINH & SLA DEMO
-- --------------------------------------------------------------------
INSERT INTO verification_requests (user_id, verification_type, status, submitted_at)
SELECT user_id, 'TUTOR_PROFILE', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 HOUR) FROM users WHERE email = 'tutor.le@gmail.com'
ON DUPLICATE KEY UPDATE status = 'SUBMITTED';

INSERT INTO verification_requests (user_id, verification_type, status, submitted_at)
SELECT user_id, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 4 HOUR) FROM users WHERE email = 'center.triangviet@gmail.com'
ON DUPLICATE KEY UPDATE status = 'SUBMITTED';

INSERT INTO support_tickets (user_id, category, subject, description, priority, status, due_at, sla_breached, created_at)
SELECT user_id, 'DISPUTE', 'Lỗi thanh toán VNPay qua cổng quét QR', 'Sau khi quét mã QR thanh toán thành công, hệ thống chưa cộng tiền vào ví ký quỹ.', 'URGENT', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 HOUR), 1, DATE_SUB(NOW(), INTERVAL 14 HOUR)
FROM users WHERE email = 'haehuynh35@gmail.com' LIMIT 1
ON DUPLICATE KEY UPDATE status = 'OPEN';

INSERT INTO support_tickets (user_id, category, subject, description, priority, status, due_at, sla_breached, created_at)
SELECT user_id, 'INQUIRY', 'Yêu cầu kiểm duyệt bổ sung chứng chỉ IELTS', 'Tôi đã tải lên bằng IELTS mới 8.5, nhờ Admin kiểm duyệt lại hồ sơ gia sư.', 'HIGH', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 8 HOUR), 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM users WHERE email = 'minhduc101dz@gmail.com' LIMIT 1
ON DUPLICATE KEY UPDATE status = 'IN_PROGRESS';

INSERT INTO support_tickets (user_id, category, subject, description, priority, status, created_at)
SELECT user_id, 'BUG_REPORT', 'Báo cáo vi phạm thái độ gia sư', 'Gia sư nghỉ học không báo trước 3 buổi liên tiếp.', 'URGENT', 'OPEN', NOW()
FROM users WHERE email = 'parent.tran@gmail.com' LIMIT 1
ON DUPLICATE KEY UPDATE status = 'OPEN';


-- --------------------------------------------------------------------
-- 9. BÁO CÁO VI PHẠM (REPORTS)
-- --------------------------------------------------------------------
INSERT INTO reports (reporter_id, target_type, target_id, category, description, status, created_at)
SELECT (SELECT user_id FROM users WHERE email = 'haehuynh35@gmail.com' LIMIT 1), 'USER', (SELECT user_id FROM users WHERE email = 'minhduc101dz@gmail.com' LIMIT 1), 'FRAUD', 'Gia sư đề nghị chuyển khoản ngoài để né tránh phí dịch vụ nền tảng 10%', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY)
ON DUPLICATE KEY UPDATE status = 'PENDING';

INSERT INTO reports (reporter_id, target_type, target_id, category, description, status, created_at)
SELECT (SELECT user_id FROM users WHERE email = 'parent.nguyen@gmail.com' LIMIT 1), 'USER', (SELECT user_id FROM users WHERE email = 'tutor.le@gmail.com' LIMIT 1), 'ABUSE', 'Gia sư thường xuyên đến muộn quá 30 phút và không dạy đủ thời lượng cam kết', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE status = 'PENDING';
