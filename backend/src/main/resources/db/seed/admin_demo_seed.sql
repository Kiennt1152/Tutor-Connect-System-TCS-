-- =========================================================
-- Comprehensive Platform Admin Demo Seed Script
-- Covers 7 Operational Steps for Platform Admin Portal
-- =========================================================

USE tutorconnectsystem;

-- ---------------------------------------------------------
-- STEP 1: Platform Admin Account & Users Setup
-- ---------------------------------------------------------
-- Admin Credentials: thanhkiu0209@gmail.com / 12345678
INSERT INTO users (email, password_hash, status, created_at, updated_at) 
VALUES (
    'thanhkiu0209@gmail.com', 
    '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 
    'ACTIVE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE 
    password_hash = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW',
    status = 'ACTIVE',
    updated_at = NOW();

INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Quản Trị Viên Hệ Thống' 
FROM users 
WHERE email = 'thanhkiu0209@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Quản Trị Viên Hệ Thống';

-- Sample Clients, Tutors, and Centers
INSERT INTO users (email, password_hash, status, created_at, updated_at) VALUES 
('parent.nguyen@gmail.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('parent.tran@gmail.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('tutor.le@gmail.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('tutor.pham@gmail.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('center.triangviet@gmail.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO clients (user_id, full_name, phone)
SELECT user_id, 'Nguyễn Văn Hùng', '0901234567' FROM users WHERE email = 'parent.nguyen@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Nguyễn Văn Hùng', phone = '0901234567';

INSERT INTO clients (user_id, full_name, phone)
SELECT user_id, 'Trần Thị Mai', '0902345678' FROM users WHERE email = 'parent.tran@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Trần Thị Mai', phone = '0902345678';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg)
SELECT user_id, 'Lê Hoàng Nam', 'MALE', '0903456789', 'Hà Nội', 5, 'Gia sư Toán cấp 3 kinh nghiệm 5 năm', 250000.00, 4.9 FROM users WHERE email = 'tutor.le@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Lê Hoàng Nam';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg)
SELECT user_id, 'Phạm Thu Thảo', 'FEMALE', '0904567890', 'TP.HCM', 4, 'Gia sư Tiếng Anh IELTS 8.0', 300000.00, 5.0 FROM users WHERE email = 'tutor.pham@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Phạm Thu Thảo';

INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, description)
SELECT user_id, 'Trung Tâm Gia Sư Trí Việt', 'LICENSE-2026-TV', '02838999999', 'Hà Nội', 'Trung tâm uy tín chất lượng cao' FROM users WHERE email = 'center.triangviet@gmail.com'
ON DUPLICATE KEY UPDATE company_name = 'Trung Tâm Gia Sư Trí Việt';

-- ---------------------------------------------------------
-- STEP 2: Dashboard KPIs & System Parameters
-- ---------------------------------------------------------
INSERT INTO system_parameters (param_key, param_value, description) VALUES 
('PLATFORM_FEE_RATE', '0.02', 'Phí nền tảng (2%)'),
('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng ứng tuyển tối đa per class'),
('ESCROW_HOLD_DAYS', '7', 'Số ngày tạm giữ tiền ký quỹ'),
('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày không phản hồi')
ON DUPLICATE KEY UPDATE param_key = param_key;

-- ---------------------------------------------------------
-- STEP 3: Pending Operational Tasks (Verifications & Tickets)
-- ---------------------------------------------------------
-- 3.1 Verification Requests (status enum: DRAFT, SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED)
INSERT INTO verification_requests (user_id, verification_type, status, submitted_at)
SELECT user_id, 'TUTOR_PROFILE', 'SUBMITTED', NOW() FROM users WHERE email = 'tutor.le@gmail.com'
ON DUPLICATE KEY UPDATE status = 'SUBMITTED';

INSERT INTO verification_requests (user_id, verification_type, status, submitted_at)
SELECT user_id, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', NOW() FROM users WHERE email = 'center.triangviet@gmail.com'
ON DUPLICATE KEY UPDATE status = 'SUBMITTED';

-- 3.2 Support Tickets (category enum: DISPUTE, SYSTEM_ERROR, REPORT_USER, BUG_REPORT, INQUIRY)
INSERT INTO support_tickets (user_id, category, subject, description, priority, status, created_at) VALUES
((SELECT user_id FROM users WHERE email='parent.nguyen@gmail.com' LIMIT 1), 'DISPUTE', 'Vấn đề hoàn tiền hợp đồng #102', 'Tôi đã nạp tiền nhưng lớp học bị hủy, đề nghị hoàn trả lại tiền vào tài khoản.', 'HIGH', 'OPEN', NOW()),
((SELECT user_id FROM users WHERE email='tutor.le@gmail.com' LIMIT 1), 'INQUIRY', 'Yêu cầu cập nhật chứng chỉ IELTS', 'Tôi đã tải lên bằng IELTS mới 8.5, nhờ Admin kiểm duyệt lại hồ sơ gia sư.', 'MEDIUM', 'IN_PROGRESS', NOW()),
((SELECT user_id FROM users WHERE email='parent.tran@gmail.com' LIMIT 1), 'BUG_REPORT', 'Báo cáo vi phạm thái độ gia sư', 'Gia sư nghỉ học không báo trước 3 buổi liên tiếp.', 'URGENT', 'OPEN', NOW())
ON DUPLICATE KEY UPDATE category = category;

-- ---------------------------------------------------------
-- STEP 4: Resource & Content Management (Categories & FAQs)
-- ---------------------------------------------------------
INSERT INTO categories (name, description, status) VALUES 
('Môn Toán', 'Môn Toán học các cấp từ Cấp 1 đến Cấp 3', 'ACTIVE'),
('Môn Tiếng Anh', 'Môn Tiếng Anh giao tiếp & luyện thi chứng chỉ', 'ACTIVE'),
('Cấp 2 (THCS)', 'Chương trình học THCS Lớp 6 - Lớp 9', 'ACTIVE'),
('Cấp 3 (THPT)', 'Chương trình học THPT Lớp 10 - Lớp 12', 'ACTIVE')
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

INSERT INTO faq_entries (question, answer, category, sort_order, is_published, created_at) VALUES
('Quy trình kiểm duyệt hồ sơ gia sư mất bao lâu?', 'Đội ngũ hỗ trợ sẽ xem xét và phản hồi hồ sơ xác minh gia sư trong vòng 24 - 48 giờ làm việc.', 'GIA_SU', 1, 1, NOW()),
('Phí dịch vụ của hệ thống được tính như thế nào?', 'Hệ thống áp dụng phí nền tảng 2% trên mỗi hợp đồng lớp học được hoàn thành thành công.', 'THANH_TOAN', 2, 1, NOW()),
('Tôi có thể nộp yêu cầu khiếu nại ở đâu?', 'Bạn có thể vào mục Trung tâm hỗ trợ và nhấn "Tạo yêu cầu hỗ trợ" để gửi phiếu cho bộ phận chăm sóc khách hàng.', 'CHUNG', 3, 1, NOW())
ON DUPLICATE KEY UPDATE sort_order = sort_order;

-- ---------------------------------------------------------
-- STEP 5 & 6: System Audit Logs & Operational History
-- ---------------------------------------------------------
INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, old_value, new_value, ip_address, created_at)
SELECT 
    (SELECT user_id FROM users WHERE email = 'thanhkiu0209@gmail.com' LIMIT 1),
    'APPROVE_VERIFICATION',
    'VerificationRequest',
    1,
    '{"status": "SUBMITTED"}',
    '{"status": "VERIFIED"}',
    '127.0.0.1',
    NOW();

INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, old_value, new_value, ip_address, created_at)
SELECT 
    (SELECT user_id FROM users WHERE email = 'thanhkiu0209@gmail.com' LIMIT 1),
    'UPDATE_SYSTEM_PARAMETER',
    'SystemParameter',
    1,
    '{"PLATFORM_FEE_RATE": "0.01"}',
    '{"PLATFORM_FEE_RATE": "0.02"}',
    '127.0.0.1',
    NOW();

-- ---------------------------------------------------------
-- STEP 7: Stakeholder Notifications
-- ---------------------------------------------------------
INSERT INTO notifications (user_id, type, title, content, reference_type, reference_id, is_read, created_at)
SELECT 
    user_id,
    'SYSTEM',
    'Yêu cầu hỗ trợ #1 đang được xử lý',
    'Yêu cầu hỗ trợ của bạn về việc hoàn tiền đã được chuyển sang bộ phận kế toán.',
    'SUPPORT_TICKET',
    1,
    0,
    NOW()
FROM users WHERE email = 'parent.nguyen@gmail.com';

INSERT INTO notifications (user_id, type, title, content, reference_type, reference_id, is_read, created_at)
SELECT 
    user_id,
    'VERIFICATION',
    'Hồ sơ gia sư đang được kiểm duyệt',
    'Tài liệu xác minh bằng cấp của bạn đã được gửi cho Quản trị viên.',
    'VERIFICATION',
    1,
    0,
    NOW()
FROM users WHERE email = 'tutor.le@gmail.com';

-- =========================================================
-- End of Admin Demo Seed Script
-- =========================================================
