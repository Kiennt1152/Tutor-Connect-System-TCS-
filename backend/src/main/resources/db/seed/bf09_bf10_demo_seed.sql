-- ==============================================================================
-- KỊCH BẢN DỮ LIỆU MẪU CHUẨN DEMO: BF-09 (SUPPORT HELP & AI) & BF-10 (ADMIN OPERATIONS)
-- Hệ thống: Tutor Connect System (TCS)
-- Mật khẩu chung cho tất cả tài khoản: 12345678
-- Hash: $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
-- ==============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. DANH SÁCH TÀI KHOẢN NGƯỜI DÙNG & QUẢN TRỊ VIÊN (USERS & PLATFORM ADMINS)
-- ------------------------------------------------------------------------------
-- Cập nhật mật khẩu chuẩn 12345678 và kích hoạt trạng thái cho các user
UPDATE users 
SET password_hash = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW',
    status = 'ACTIVE',
    token_version = COALESCE(token_version, 0)
WHERE user_id BETWEEN 1 AND 36;

-- Tài khoản Quản trị viên (Platform Admins)
INSERT INTO users (user_id, email, phone, password_hash, status, token_version, created_at, updated_at)
VALUES 
    (1, 'thanhkiu0209@gmail.com', '0988001001', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW()),
    (2, 'admin@tcs.vn', '0988001002', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    password_hash = VALUES(password_hash), status = 'ACTIVE', updated_at = NOW();

INSERT INTO platform_admins (admin_id, user_id, full_name)
VALUES 
    (1, 1, 'Quản Trị Viên Hệ Thống'),
    (2, 2, 'Admin TCS')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Tài khoản Học viên / Phụ huynh (Clients)
INSERT INTO users (user_id, email, phone, password_hash, status, token_version, created_at, updated_at)
VALUES 
    (5, 'parent.nguyen@gmail.com', '0901234567', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW()),
    (6, 'parent.tran@gmail.com', '0902345678', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW()),
    (7, 'parent.tuan@tcs.vn', '0903334444', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    password_hash = VALUES(password_hash), status = 'ACTIVE', updated_at = NOW();

INSERT INTO clients (client_id, user_id, full_name, phone, address, gender, created_at, updated_at)
VALUES 
    (1, 5, 'Nguyễn Văn Hùng', '0901234567', 'Số 18 Hoàng Quốc Việt, Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()),
    (2, 6, 'Trần Thị Mai', '0902345678', 'Số 45 Chùa Bộc, Đống Đa, Hà Nội', 'FEMALE', NOW(), NOW()),
    (3, 7, 'Phạm Anh Tuấn', '0903334444', 'Số 88 Trần Duy Hưng, Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), phone = VALUES(phone), address = VALUES(address);

-- Tài khoản Gia sư (Tutors)
INSERT INTO users (user_id, email, phone, password_hash, status, token_version, created_at, updated_at)
VALUES 
    (8, 'tutor.le@gmail.com', '0903456789', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW()),
    (9, 'tutor.pham@gmail.com', '0904567890', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW()),
    (10, 'tutor.math@tcs.vn', '0912111222', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW()),
    (11, 'tutor.physics@tcs.vn', '0912333444', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    password_hash = VALUES(password_hash), status = 'ACTIVE', updated_at = NOW();

INSERT INTO tutors (tutor_id, user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
VALUES 
    (1, 8, 'Lê Hoàng Nam', 'MALE', '0903456789', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư chuyên Toán 12 và Luyện thi Đại học, cử nhân Sư phạm Toán ĐH Sư Phạm Hà Nội.', 250000.00, 4.90, 'VERIFIED', NOW(), NOW()),
    (2, 9, 'Phạm Thu Thảo', 'FEMALE', '0904567890', 'Quận 1, TP.HCM', 4, 'Gia sư Tiếng Anh IELTS 8.0 chuyên kèm 1:1 học sinh cấp 3 và người đi làm.', 300000.00, 5.00, 'VERIFIED', NOW(), NOW()),
    (3, 10, 'Nguyễn Văn Toán', 'MALE', '0912111222', 'Quận Ba Đình, Hà Nội', 6, 'Gia sư Toán bồi dưỡng học sinh giỏi THCS & THPT.', 220000.00, 4.85, 'VERIFIED', NOW(), NOW()),
    (4, 11, 'Trần Minh Lý', 'MALE', '0912333444', 'Quận Đống Đa, Hà Nội', 3, 'Gia sư Vật lý 10-12 ôn thi THPT Quốc Gia.', 200000.00, 4.70, 'UNDER_VERIFY', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), hourly_rate = VALUES(hourly_rate), rating_avg = VALUES(rating_avg);

-- Tài khoản Trung tâm Gia sư (Tutor Center)
INSERT INTO users (user_id, email, phone, password_hash, status, token_version, created_at, updated_at)
VALUES 
    (12, 'center.triangviet@gmail.com', '02838999999', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    password_hash = VALUES(password_hash), status = 'ACTIVE', updated_at = NOW();

INSERT INTO tutor_centers (center_id, user_id, company_name, license_no, phone, address, description, verification_status, custom_fee_rate)
VALUES 
    (1, 12, 'Trung Tâm Gia Sư Trí Việt', 'LICENSE-2026-TV', '02838999999', '123 Đường Cầu Giấy, Cầu Giấy, Hà Nội', 'Trung tâm kết nối gia sư uy tín chất lượng cao hàng đầu Hà Nội', 'VERIFIED', 0.1000)
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), custom_fee_rate = 0.1000;

-- Ví tiền người dùng (Wallets)
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES 
    (1, 0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (2, 0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (5, 7500000.00, 3000000.00, 'ACTIVE', NOW(), NOW()),
    (6, 10000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (7, 5000000.00, 2200000.00, 'ACTIVE', NOW(), NOW()),
    (8, 4500000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (9, 6600000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (10, 2000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (11, 1500000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (12, 18000000.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE available_balance = VALUES(available_balance), frozen_balance = VALUES(frozen_balance);

-- ------------------------------------------------------------------------------
-- 2. DỮ LIỆU LỚP HỌC & HỢP ĐỒNG (CLASSES & ASSIGNMENTS)
-- ------------------------------------------------------------------------------
INSERT INTO tutoring_classes (
    class_id, creator_id, subject_id, grade_id, class_type, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES 
    (201, 5, 1, 12, 'PRIVATE', 'Luyện thi Đại học môn Toán đạt điểm 9+', 'Gia sư có kinh nghiệm dạy Toán 12 chuyên',
    'Lớp Toán 12 Luyện thi Đại học (Cầu Giấy)', 'Lớp học kèm trực tiếp tại nhà học viên 2 buổi/tuần, chuẩn bị kỳ thi THPT Quốc gia.',
    'OFFLINE', 10, 250000.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY),
    2500000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),

    (202, 6, 7, 12, 'PRIVATE', 'Luyện thi IELTS mục tiêu 7.5 Band', 'Gia sư IELTS 8.0 trở lên, nhiệt tình và có phương pháp',
    'Lớp Tiếng Anh IELTS 7.5 Cấp tốc Online', 'Học trực tuyến qua Google Meet / Zoom, rèn luyện 4 kỹ năng Nghe Nói Đọc Viết.',
    'ONLINE', 12, 300000.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
    3600000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

    (203, 7, 2, 11, 'PRIVATE', 'Bồi dưỡng học sinh giỏi môn Vật lý 11', 'Sinh viên hoặc giáo viên chuyên Lý ĐH Sư Phạm',
    'Lớp Vật lý 11 Nâng cao Chuyên KHTN', 'Cần gia sư vững lý thuyết và các bài toán đồ thị, dao động cơ học.',
    'OFFLINE', 8, 250000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2000000.00, 'WEEKLY', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

    (204, 12, 10, 10, 'CENTER', 'Nhập môn Lập trình Python & Tư duy thuật toán', 'Giảng viên công nghệ thông tin có kinh nghiệm dạy trẻ em',
    'Lớp Lập trình Python Sáng tạo Trẻ em (Online)', 'Lớp học nhóm 5-8 học sinh do Trung tâm Trí Việt tổ chức giảng dạy trực tuyến.',
    'ONLINE', 10, 220000.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2200000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), tuition_fee = VALUES(tuition_fee), budget = VALUES(budget);

-- Phân công gia sư (class_assignments)
INSERT INTO class_assignments (assignment_id, tutor_id, application_id, assigned_date, status, terms_b)
VALUES 
    (101, 1, NULL, DATE_SUB(NOW(), INTERVAL 15 DAY), 'ACTIVE', 'Thỏa thuận giảng dạy kèm môn Toán 12 theo đúng cam kết chuẩn đầu ra.'),
    (102, 2, NULL, DATE_SUB(NOW(), INTERVAL 12 DAY), 'ACTIVE', 'Thỏa thuận đào tạo IELTS 7.5 chuyên sâu.'),
    (103, 3, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), 'ACTIVE', 'Điều khoản thỏa thuận giải quyết tranh chấp học phí.')
ON DUPLICATE KEY UPDATE status = VALUES(status), tutor_id = VALUES(tutor_id);

-- ------------------------------------------------------------------------------
-- 3. KÝ QUỸ ESCROW ĐỦ 6 TRẠNG THÁI & GIAO DỊCH TÀI CHÍNH (UC-58, UC-41)
-- ------------------------------------------------------------------------------
-- Giao dịch nạp / ký quỹ
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES 
    (101, 5, 'TX-DEP-2500K-001', 'ESCROW_DEPOSIT', 'SUCCESS', 2500000.00, 'Ký quỹ học phí lớp Toán 12 (Cầu Giấy)', 'ESC-2500000', NOW(), DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (102, 6, 'TX-DEP-3600K-002', 'ESCROW_DEPOSIT', 'SUCCESS', 3600000.00, 'Ký quỹ học phí lớp Tiếng Anh IELTS 7.5', 'ESC-3600000', NOW(), DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (103, 7, 'TX-DEP-2200K-003', 'ESCROW_DEPOSIT', 'SUCCESS', 2200000.00, 'Ký quỹ hợp đồng tranh chấp Vật lý 11', 'ESC-2200000', NOW(), DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (104, 5, 'TX-DEP-1800K-004', 'ESCROW_DEPOSIT', 'PENDING', 1800000.00, 'Chờ thanh toán ký quỹ lớp bổ trợ kiến thức', 'ESC-1800000', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (105, 5, 'TX-DEP-3000K-005', 'ESCROW_DEPOSIT', 'SUCCESS', 3000000.00, 'Tạm giữ tiền ký quỹ lớp học đang đối soát', 'ESC-3000000', NOW(), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (106, 5, 'TX-REF-2400K-006', 'REFUND', 'SUCCESS', 2400000.00, 'Hoàn trả học phí ký quỹ lớp Toán do gia sư hủy lớp', 'REF-2400000', NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (107, 8, 'TX-WDR-1500K-007', 'WITHDRAWAL', 'PENDING', 1500000.00, 'Rút tiền thù lao gia sư về tài khoản MB Bank', 'WDR-1500000', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (108, 9, 'TX-WDR-3000K-008', 'WITHDRAWAL', 'SUCCESS', 3000000.00, 'Rút tiền thù lao gia sư về tài khoản Vietcombank', 'WDR-3000000', NOW(), DATE_SUB(NOW(), INTERVAL 3 DAY))
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

-- ------------------------------------------------------------------------------
-- 4. [BF-09] SUPPORT TICKETS & TICKET MESSAGES - CHUẨN ĐỦ 6 KỊCH BẢN DEMO
-- ------------------------------------------------------------------------------
DELETE FROM ticket_messages WHERE ticket_id BETWEEN 101 AND 110;
DELETE FROM support_tickets WHERE ticket_id BETWEEN 101 AND 110;

INSERT INTO support_tickets (
    ticket_id, user_id, assigned_admin_id, target_class_id, category, subject, description,
    priority, status, due_at, sla_breached, response_sla_ms, resolved_at, closed_at, created_at, updated_at
)
VALUES 
    -- Kịch bản 1: [DEMO AUTO-ASSIGN] Ticket mới OPEN, chưa gán admin (assigned_admin_id IS NULL).
    -- Admin click vào xem chi tiết -> Backend tự động gán admin đó làm PIC và đổi status thành IN_PROGRESS!
    (101, 5, NULL, NULL, 'INQUIRY', 
     '[DEMO-01 Auto-Assign] Hướng dẫn thay đổi số điện thoại nhận thông báo học tập', 
     'Chào ban quản trị, tôi muốn cập nhật số điện thoại nhận OTP và thông báo lịch học cho con nhưng hệ thống báo lỗi không nhận mã. Nhờ admin hỗ trợ.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 48 HOUR), 0, NULL, NULL, NULL, NOW(), NOW()),

    -- Kịch bản 2: [DEMO FIRST RESPONSE SLA] Ticket IN_PROGRESS, chưa có phản hồi nào từ admin (response_sla_ms IS NULL).
    -- Admin nhập nội dung phản hồi -> Backend đo Duration từ lúc tạo đến lúc phản hồi và ghi nhận First Response SLA ms!
    (102, 8, 1, NULL, 'BUG_REPORT', 
     '[DEMO-02 SLA Response] Lỗi đồng bộ ca dạy trên Google Calendar buổi tối', 
     'Tôi tích hợp lịch dạy cá nhân với Google Calendar nhưng ca học 19:30 bị hiển thị lệch thành 07:30 sáng hôm sau. Đề nghị admin hỗ trợ sửa lỗi.',
     'MEDIUM', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 20 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW()),

    -- Kịch bản 3A: [DEMO MERGE TICKET - ĐÍCH] Ticket của phụ huynh Nguyễn Văn Hùng (user_id = 5)
    (103, 5, 1, NULL, 'INQUIRY', 
     '[DEMO-03 Target] Thắc mắc đối soát giao dịch nạp tiền qua cổng SePay', 
     'Tôi đã chuyển khoản 2.500.000đ vào tài khoản sàn qua mã QR SePay nhưng số dư khả dụng trên ví vẫn chưa được cộng. Mã giao dịch ngân hàng là MB-889922.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 44 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW()),

    -- Kịch bản 3B: [DEMO MERGE TICKET - NGUỒN CẦN GỘP VÀO #103] Cùng người dùng user_id = 5, nội dung bổ sung!
    -- Admin mở ticket #104 -> Bấm "Gộp Ticket" -> Nhập ID đích 103 -> #104 chuyển CLOSED, nội dung dời sang #103!
    (104, 5, 1, NULL, 'INQUIRY', 
     '[DEMO-03 Source Gộp vào #103] Gửi bổ sung ảnh chụp biên lai giao dịch thành công', 
     'Tôi xin bổ sung ảnh chụp màn hình ứng dụng ngân hàng xác nhận đã trừ tiền 2.500.000đ lúc 10:15 hôm nay, nhờ admin kiểm tra đối soát cùng ticket trước.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 46 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW()),

    -- Kịch bản 4: [DEMO QUÉT QUÁ HẠN SLA] Ticket quá hạn SLA (due_at đã trôi qua), sla_breached = 0, priority = MEDIUM.
    -- Khi Admin bấm "Quét vi phạm SLA" hoặc Scheduler chạy -> Tự động thăng cấp lũy tiến MEDIUM -> HIGH, sla_breached = 1, bắn cảnh báo!
    (105, 6, 1, NULL, 'INQUIRY', 
     '[DEMO-04 Scan SLA] Khiếu nại thời gian xử lý lệnh rút tiền thù lao quá 24 giờ', 
     'Tôi đã tạo lệnh rút 3.000.000đ về Vietcombank từ 2 ngày trước nhưng trạng thái vẫn hiển thị PENDING. Đề nghị bộ phận tài chính giải quyết gấp.',
     'MEDIUM', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 26 HOUR), NOW()),

    -- Kịch bản 5: [DEMO CHUYỂN SANG TRANH CHẤP ESCROW] Ticket phản ánh sự cố lớp học 201.
    -- Admin click "Chuyển Tranh Chấp" -> Category đổi DISPUTE, Priority ép lên HIGH, sinh bản ghi Report CLASS để tạm khóa tiền Escrow!
    (106, 5, 1, 201, 'INQUIRY', 
     '[DEMO-05 Chuyển Tranh Chấp] Gia sư tự ý nghỉ buổi học thứ 3 không báo trước', 
     'Gia sư Lê Hoàng Nam không vào lớp buổi 3 ngày hôm qua và không liên lạc được. Tôi đã nạp giữ tiền Escrow 2.500.000đ, đề nghị can thiệp bảo vệ quyền lợi.',
     'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 40 HOUR), 0, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW()),

    -- Kịch bản 6: [DEMO ĐÓNG / GIẢI QUYẾT TICKET] Ticket đã được trao đổi hoàn tất, sẵn sàng bấm RESOLVED / CLOSED.
    (107, 8, 1, NULL, 'INQUIRY', 
     '[DEMO-06 Close/Resolve] Tư vấn mở lớp dạy kèm nhóm cho 4 học sinh lớp 12', 
     'Nhờ ban quản trị hướng dẫn quy trình tạo lớp học nhóm trên hệ thống và mức thu phí nền tảng áp dụng cho hình thức dạy nhóm.',
     'MEDIUM', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 18 HOUR), 0, 1850000, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),

    -- Kịch bản 7: [DEMO MỞ LẠI TICKET ĐÃ ĐÓNG (REOPEN)]
    (108, 5, 1, NULL, 'BUG_REPORT', 
     '[DEMO-07 Reopen] Không xem được tệp đề cương ôn thi đính kèm trên web', 
     'Khi bấm vào link tài liệu ôn tập môn Toán 12 thì trang web báo lỗi 404 không tìm thấy tệp. Đề nghị kỹ thuật kiểm tra lại đường dẫn lưu trữ.',
     'MEDIUM', 'RESOLVED', DATE_SUB(NOW(), INTERVAL 12 HOUR), 0, 950000, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- Khởi tạo tin nhắn trao đổi trong các Support Ticket (ticket_messages)
INSERT INTO ticket_messages (ticket_id, sender_id, is_from_admin, content, created_at)
VALUES 
    (101, 5, 0, 'Chào ban quản trị, tôi muốn cập nhật số điện thoại nhận OTP và thông báo lịch học cho con nhưng hệ thống báo lỗi không nhận mã. Nhờ admin hỗ trợ.', NOW()),
    
    (102, 8, 0, 'Tôi tích hợp lịch dạy cá nhân với Google Calendar nhưng ca học 19:30 bị hiển thị lệch thành 07:30 sáng hôm sau. Đề nghị admin hỗ trợ sửa lỗi.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),

    (103, 5, 0, 'Tôi đã chuyển khoản 2.500.000đ vào tài khoản sàn qua mã QR SePay nhưng số dư khả dụng trên ví vẫn chưa được cộng. Mã giao dịch ngân hàng là MB-889922.', DATE_SUB(NOW(), INTERVAL 2 HOUR)),

    (104, 5, 0, 'Tôi xin bổ sung ảnh chụp màn hình ứng dụng ngân hàng xác nhận đã trừ tiền 2.500.000đ lúc 10:15 hôm nay, nhờ admin kiểm tra đối soát cùng ticket trước.', DATE_SUB(NOW(), INTERVAL 1 HOUR)),

    (105, 6, 0, 'Tôi đã tạo lệnh rút 3.000.000đ về Vietcombank từ 2 ngày trước nhưng trạng thái vẫn hiển thị PENDING. Đề nghị bộ phận tài chính giải quyết gấp.', DATE_SUB(NOW(), INTERVAL 26 HOUR)),

    (106, 5, 0, 'Gia sư Lê Hoàng Nam không vào lớp buổi 3 ngày hôm qua và không liên lạc được. Tôi đã nạp giữ tiền Escrow 2.500.000đ, đề nghị can thiệp bảo vệ quyền lợi.', DATE_SUB(NOW(), INTERVAL 3 HOUR)),

    (107, 8, 0, 'Nhờ ban quản trị hướng dẫn quy trình tạo lớp học nhóm trên hệ thống và mức thu phí nền tảng áp dụng cho hình thức dạy nhóm.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    (107, 1, 1, 'Chào thầy Nam! Hiện tại nền tảng TCS hỗ trợ mở lớp nhóm từ 2–8 học sinh tại mục /tao-lop. Mức phí nền tảng cố định vẫn là 10% trên tổng giá trị lớp học thành công.', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (107, 8, 0, 'Dạ em cảm ơn Admin đã phản hồi nhanh chóng và chi tiết!', DATE_SUB(NOW(), INTERVAL 4 HOUR)),

    (108, 5, 0, 'Khi bấm vào link tài liệu ôn tập môn Toán 12 thì trang web báo lỗi 404 không tìm thấy tệp. Đề nghị kỹ thuật kiểm tra lại đường dẫn lưu trữ.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (108, 1, 1, 'Bộ phận kỹ thuật đã khôi phục lại liên kết tải file đề cương trên hệ thống máy chủ lưu trữ. Bạn vui lòng tải lại trang và thử lại nhé.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ------------------------------------------------------------------------------
-- 5. [BF-09] KHO TRI THỨC FAQ ĐA DANH MỤC & CÂU HỎI BẢN THẢO (PLATFORM FAQ)
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
    question = VALUES(question), answer = VALUES(answer), category = VALUES(category), is_published = VALUES(is_published);

-- ------------------------------------------------------------------------------
-- 6. [BF-10] YÊU CẦU XÁC MINH DANH TÍNH eKYC (PLATFORM VERIFICATIONS)
-- ------------------------------------------------------------------------------
-- Tạo media files tài liệu xác minh mẫu
INSERT INTO media_files (file_id, uploaded_by, file_name, file_url, mime_type, file_size, created_at)
VALUES 
    (201, 8, 'cccd_front_lehoangnam.jpg', 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 245120, NOW()),
    (202, 8, 'cccd_back_lehoangnam.jpg', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 261800, NOW()),
    (203, 8, 'bang_daihoc_supham_hanoi.jpg', 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 512400, NOW()),
    (204, 12, 'giay_phep_kinh_doanh_triviet.jpg', 'https://images.unsplash.com/photo-1450133064473-71024230f91b?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 624100, NOW()),
    (205, 12, 'giay_phep_hoat_dong_giao_duc.jpg', 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 583200, NOW()),
    (206, 9, 'chung_chi_ielts_80.jpg', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 312500, NOW())
ON DUPLICATE KEY UPDATE file_url = VALUES(file_url);

DELETE FROM verification_documents WHERE verification_id BETWEEN 101 AND 104;
DELETE FROM verification_requests WHERE verification_id BETWEEN 101 AND 104;

INSERT INTO verification_requests (verification_id, user_id, verification_type, status, admin_notes, submitted_at, reviewed_at, created_at, updated_at)
VALUES 
    -- 1. Chờ duyệt: Gia sư Lê Hoàng Nam (TUTOR_PROFILE)
    (101, 8, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư Lê Hoàng Nam gửi CCCD 2 mặt và bằng Cử nhân Sư phạm Toán để kiểm duyệt hồ sơ.', NOW(), NULL, NOW(), NOW()),
    
    -- 2. Chờ duyệt: Trung tâm Trí Việt (TUTOR_CENTER_LICENSE)
    (102, 12, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', 'Trung tâm Trí Việt nộp Giấy phép kinh doanh giáo dục và CCCD người đại diện pháp luật.', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NOW(), NOW()),

    -- 3. Đã duyệt thành công: Gia sư Nguyễn Văn Toán (TUTOR_PROFILE)
    (103, 10, 'TUTOR_PROFILE', 'VERIFIED', 'Hồ sơ đầy đủ, rõ nét, đã đối soát CCCD và bằng cấp chuyên môn hợp lệ.', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- 4. Bị từ chối: Gia sư Trần Minh Lý (TUTOR_PROFILE) - lý do từ chối >= 10 ký tự
    (104, 11, 'TUTOR_PROFILE', 'REJECTED', 'Ảnh chụp giấy tờ tùy thân CCCD mặt sau bị mờ góc số định danh và lóa sáng, đề nghị tải lại bản chụp rõ nét.', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

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
-- 7. [BF-10] BÁO CÁO SỰ CỐ & CAN THIỆP LỚP HỌC (PLATFORM REPORTS & DISPUTES)
-- ------------------------------------------------------------------------------
DELETE FROM disputes WHERE dispute_id BETWEEN 101 AND 105;
DELETE FROM reports WHERE report_id BETWEEN 101 AND 105;

INSERT INTO reports (report_id, reporter_id, target_type, target_id, category, description, evidence_urls, status, created_at, updated_at)
VALUES 
    -- Báo cáo 1: Lớp học - sẵn sàng cho Admin can thiệp 7 phương án (RESCHEDULE, REPLACE_TUTOR, TERMINATE, ESCALATE_TO_DISPUTE...)
    (101, 5, 'CLASS', 201, 'OTHER', 
     '[UC-29] Báo cáo sự cố lớp học\nMã loại: LATE_TUTOR\nLoại sự cố: Gia sư vào muộn 40 phút buổi học ngày 22/09\nMã hướng xử lý mong muốn: RESCHEDULE\nMô tả: Gia sư vào lớp muộn không thông báo trước, đề nghị sắp xếp ca học bù.',
     '["https://images.unsplash.com/photo-1544717305-2782549b5136"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

    -- Báo cáo 2: Tranh chấp hợp đồng lớp học - gắn với Dispute và Escrow
    (102, 7, 'CLASS', 202, 'FRAUD', 
     '[UC-29] Tranh chấp học phí lớp Tiếng Anh IELTS\nMã loại: QUALITY_ISSUE\nLoại sự cố: Chất lượng giảng dạy không đúng cam kết\nMã hướng xử lý: ESCALATE_TO_DISPUTE\nMô tả: Đề nghị tạm giữ tiền ký quỹ Escrow và hoàn lại 50% tiền học phí cho gia đình.',
     '["https://images.unsplash.com/photo-1523240795612-9a054b0db644"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

    -- Báo cáo 3: Báo cáo nhận xét phản cảm (REVIEW)
    (103, 8, 'REVIEW', 101, 'ABUSE', 
     'Nhận xét đánh giá có ngôn từ vu khống, xúc phạm danh dự nghề giáo của gia sư, yêu cầu kiểm duyệt ẩn bài viết.',
     '["https://images.unsplash.com/photo-1573496359142-b8d87734a5a2"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW()),

    -- Báo cáo 4: Đã giải quyết thành công
    (104, 5, 'CLASS', 204, 'OTHER', 
     'Yêu cầu dời lịch lớp lập trình nhóm sang sáng Chủ nhật để các học sinh tham gia đầy đủ.',
     '["https://images.unsplash.com/photo-1507679799987-c73779587ccf"]', 'RESOLVED', DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());

INSERT INTO disputes (dispute_id, report_id, escrow_id, resolution, status, created_at, updated_at)
VALUES 
    (101, 102, 103, 'Ban quản trị đang tiến hành triệu tập đối soát chứng từ học tập giữa phụ huynh và gia sư.', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), resolution = VALUES(resolution);

-- ------------------------------------------------------------------------------
-- 8. [BF-10] PHÁT HIỆN HÀNH VI NÉ TRÁNH NỀN TẢNG (CIRCUMVENTION SCANNER)
-- ------------------------------------------------------------------------------
INSERT INTO conversations (conversation_id, context_type, context_id, type, status, created_at)
VALUES 
    (201, 'CLASS', 201, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (202, 'CLASS', 202, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

INSERT INTO conversation_participants (participant_id, conversation_id, user_id, last_read_at)
VALUES 
    (201, 201, 5, NOW()),
    (202, 201, 8, NOW()),
    (203, 202, 6, NOW()),
    (204, 202, 9, NOW())
ON DUPLICATE KEY UPDATE last_read_at = NOW();

INSERT INTO messages (message_id, conversation_id, sender_id, message_type, content, is_edited, is_deleted, sent_at)
VALUES 
    (201, 201, 8, 'TEXT', 'Chào chị, chị chuyển khoản trực tiếp qua số Zalo 0987654321 hoặc STK MBBank để không mất phí sàn 10% nhé!', 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (202, 202, 9, 'TEXT', 'Em có thể kết bạn Zalo số 0904567890 hoặc gửi email lehoangnam.tutor@gmail.com để trao đổi học riêng nhé.', 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (203, 201, 5, 'TEXT', 'Em vào trang facebook.com/giasuvip.hanoi để xem thêm giáo án mẫu nhé.', 0, 0, DATE_SUB(NOW(), INTERVAL 18 HOUR))
ON DUPLICATE KEY UPDATE content = VALUES(content);

DELETE FROM circumvention_events WHERE event_id BETWEEN 201 AND 204;

INSERT INTO circumvention_events (event_id, message_id, conversation_id, sender_id, matched_rule, evidence, risk_score, status, review_note, reviewed_by, reviewed_at, created_at)
VALUES 
    -- Event 1: Phát hiện từ khóa thanh toán ngoài & STK
    (201, 201, 201, 8, 'PAYMENT_KEYWORD', 'chuyển khoản trực tiếp qua STK MBBank để không mất phí sàn 10%', 90, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Event 2: Phát hiện số điện thoại Zalo
    (202, 201, 201, 8, 'PHONE', '0987654321', 85, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Event 3: Phát hiện địa chỉ Email trao đổi ngoài
    (203, 202, 202, 9, 'EMAIL', 'lehoangnam.tutor@gmail.com', 75, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- Event 4: Phát hiện liên kết mạng xã hội (Đã xác nhận vi phạm)
    (204, 201, 201, 5, 'URL', 'facebook.com/giasuvip.hanoi', 70, 'CONFIRMED', 'Xác nhận gửi link Facebook trao đổi ngoài sàn, đã ban hành cảnh cáo lần 1.', 1, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 18 HOUR));

-- ------------------------------------------------------------------------------
-- 9. [BF-10] BAN HÀNH CHẾ TÀI XỬ PHẠT (USER PENALTIES & GATEKEEPER)
-- ------------------------------------------------------------------------------
DELETE FROM user_penalties WHERE penalty_id BETWEEN 201 AND 204;

INSERT INTO user_penalties (
    penalty_id, user_id, issued_by, penalty_type, reason, evidence_urls,
    restriction_details, source_type, source_id, source_task_id, starts_at, expires_at, status, created_at
)
VALUES 
    -- Phạt 1: Cảnh cáo (WARNING) vì nhắn tin lách sàn
    (201, 8, 1, 'WARNING', 'Nhắn tin gạ gẫm phụ huynh chuyển khoản học phí ngoài nền tảng TCS qua Zalo cá nhân.', 
     '["https://images.unsplash.com/photo-1544717305-2782549b5136"]', 
     '{"warningLevel": 1}', 'CIRCUMVENTION', 201, 'TASK-CIRC-201', 
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),

    -- Phạt 2: Giới hạn tính năng (FEATURE_RESTRICTION) - Chặn nộp đơn nhận lớp
    (202, 11, 1, 'FEATURE_RESTRICTION', 'Tự ý hủy ca dạy sát giờ nhiều lần gây ảnh hưởng nghiêm trọng đến học sinh.', 
     '["https://images.unsplash.com/photo-1523240795612-9a054b0db644"]', 
     '{"canApplyClasses": false, "canPostClasses": false}', 'REPORT', 101, 'TASK-REP-101', 
     DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- Phạt 3: Tạm khóa tài khoản 14 ngày (TEMPORARY_BAN)
    (203, 7, 1, 'TEMPORARY_BAN', 'Đình chỉ tài khoản 14 ngày do vi phạm thỏa thuận hợp đồng và phát sinh tranh chấp tài chính chưa giải quyết.', 
     '["https://images.unsplash.com/photo-1573496359142-b8d87734a5a2"]', 
     '{"banDays": 14}', 'DISPUTE', 101, 'TASK-DISP-101', 
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),

    -- Phạt 4: Đã hết hạn (EXPIRED)
    (204, 9, 1, 'WARNING', 'Cảnh cáo về việc chậm phản hồi tin nhắn của học viên quá 48 giờ.', 
     '[]', '{"warningLevel": 1}', 'REPORT', 104, 'TASK-REP-104', 
     DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 'EXPIRED', DATE_SUB(NOW(), INTERVAL 40 DAY));

-- ------------------------------------------------------------------------------
-- 10. [BF-10] NHẬT KÝ KIỂM TOÁN HỆ THỐNG BẤT BIẾN (AUDIT LOGS)
-- ------------------------------------------------------------------------------
DELETE FROM audit_logs WHERE audit_id BETWEEN 201 AND 210;

INSERT INTO audit_logs (audit_id, actor_id, action, entity_type, entity_id, old_value, new_value, ip_address, user_agent, created_at)
VALUES 
    (201, 1, 'LOGIN', 'User', 1, NULL, '{"method": "PASSWORD", "status": "SUCCESS"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (202, 1, 'REVIEW_VERIFICATION', 'VerificationRequest', 103, '{"status": "SUBMITTED"}', '{"status": "VERIFIED", "reviewerId": 1}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (203, 1, 'UPDATE_FEE_RATE', 'SystemParameter', 1, '{"PLATFORM_FEE_RATE": "0.08"}', '{"PLATFORM_FEE_RATE": "0.10"}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (204, 1, 'RESPOND_TICKET', 'SupportTicket', 107, '{"status": "IN_PROGRESS", "responseSlaMs": null}', '{"status": "IN_PROGRESS", "responseSlaMs": 1850000}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (205, 1, 'CLOSE_TICKET', 'SupportTicket', 108, '{"status": "IN_PROGRESS"}', '{"status": "RESOLVED", "resolutionNotes": "Da sua loi file 404"}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (206, 1, 'MERGE_TICKET', 'SupportTicket', 104, '{"status": "OPEN", "targetTicketId": null}', '{"status": "CLOSED", "targetTicketId": 103, "reason": "Trung lap bien lai"}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (207, 1, 'ISSUE_PENALTY', 'UserPenalty', 201, NULL, '{"penaltyType": "WARNING", "targetUserId": 8, "reason": "Lach san Zalo"}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (208, 1, 'RESOLVE_REPORT', 'Report', 104, '{"status": "PENDING"}', '{"status": "RESOLVED", "action": "RESCHEDULE"}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (209, 1, 'UPDATE_USER_STATUS', 'User', 7, '{"status": "ACTIVE"}', '{"status": "SUSPENDED", "tokenVersion": 1}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (210, 1, 'REDIRECT_DISPUTE', 'SupportTicket', 106, '{"category": "INQUIRY", "priority": "LOW"}', '{"category": "DISPUTE", "priority": "HIGH", "createdReportId": 102}', '127.0.0.1', 'Admin-Portal-Client/1.0', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- ------------------------------------------------------------------------------
-- 11. [BF-10] CẤU HÌNH THAM SỐ TOÀN SÀN (SYSTEM PARAMETERS)
-- ------------------------------------------------------------------------------
INSERT INTO system_parameters (param_key, param_value, description)
VALUES 
    ('PLATFORM_FEE_RATE', '0.10', 'Tỷ lệ phí dịch vụ nền tảng áp dụng cố định (10%)'),
    ('ESCROW_HOLD_DAYS', '7', 'Thời gian tạm giữ ký quỹ bảo đảm trước khi giải ngân (ngày)'),
    ('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng đơn ứng tuyển tối đa cho mỗi lớp học mở tuyển'),
    ('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày nếu khách không phản hồi'),
    ('SYSTEM_ANNOUNCEMENTS', '[{"announcementId":1,"title":"Chào đón năm học mới 2026 cùng Tutor Connect System!","content":"Bảo hiểm hoàn tiền học phí 100% qua tài khoản Escrow cho tất cả các lớp học mới.","targetRole":null,"active":true,"startsAt":"2026-09-01T00:00:00","endsAt":"2026-10-31T23:59:59","createdByAdminId":1,"createdByName":"Quản Trị Viên Hệ Thống","createdAt":"2026-09-01T08:00:00","updatedAt":"2026-09-01T08:00:00"}]', 'Danh sách thông báo toàn sàn (JSON array)')
ON DUPLICATE KEY UPDATE 
    param_value = VALUES(param_value), description = VALUES(description);

SET FOREIGN_KEY_CHECKS = 1;

-- Kiểm tra kết quả nạp dữ liệu
SELECT 'BF-09 & BF-10 DEMO SEED NẠP THÀNH CÔNG!' AS status;
SELECT count(*) AS total_tickets FROM support_tickets;
SELECT count(*) AS total_ticket_messages FROM ticket_messages;
SELECT count(*) AS total_faqs FROM faq_entries;
SELECT count(*) AS total_verifications FROM verification_requests;
SELECT count(*) AS total_reports FROM reports;
SELECT count(*) AS total_circumventions FROM circumvention_events;
SELECT count(*) AS total_penalties FROM user_penalties;
SELECT count(*) AS total_audit_logs FROM audit_logs;
