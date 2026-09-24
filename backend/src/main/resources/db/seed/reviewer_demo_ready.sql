-- ====================================================================
-- REVIEWER DEMO SEED SCRIPT FOR PLATFORM ADMIN (TUTOR CONNECT SYSTEM)
-- Addresses all audit points by Reviewer Đức across 13 Use Cases:
--   1. UC-07: Users 1 to 36 verified & active with standard passwords (12345678)
--   2. UC-11: Verification requests in SUBMITTED status with documents
--   3. UC-21: Active classes with schedules, slots, lessons & attendance progress
--   4. UC-58: Escrow transactions in all 6 statuses (FUNDED, RELEASED, REFUNDED, DISPUTED, ON_HOLD, PENDING)
--   5. UC-41: Financial transactions with 2,400,000 VND refund accurately accounted for (SUCCESS)
--   6. UC-45: System Master E-Contract Templates
--   7. UC-46: Signed E-Contracts for active & completed classes
--   8. UC-49 & UC-30: Class issue reports and Escrow Dispute ticket
--   9. UC-51: Support tickets in OPEN, IN_PROGRESS, RESOLVED across categories
--  10. UC-53 & UC-55: Tutor ratings/reviews & moderation queue (VISIBLE, MODERATED)
--  11. UC-59: System Parameters (platform fee, hold days, max applications)
--  12. UC-61: Operational System Audit Logs with various actors and actions
--  13. UC-67: Published FAQs across all key categories
--  14. UC-37: System notifications for stakeholders
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 1. UC-07: ĐẢM BẢO TẤT CẢ USER TỪ 1 ĐẾN 36 HOẠT ĐỘNG & ĐỒNG BỘ MẬT KHẨU
-- Mật khẩu chung: '12345678' -> $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
-- --------------------------------------------------------------------
UPDATE users 
SET password_hash = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW',
    status = 'ACTIVE'
WHERE user_id BETWEEN 1 AND 36;

-- Đảm bảo tài khoản admin chính xác
INSERT INTO platform_admins (admin_id, user_id, full_name)
VALUES 
    (1, 1, 'Quản Trị Viên Hệ Thống'),
    (2, 2, 'Admin TCS'),
    (3, 27, 'Quản Trị Viên Đức')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- --------------------------------------------------------------------
-- 1.1. HỒ SƠ CLIENTS, TUTORS, TRUNG TÂM & VÍ TIỀN (WALLETS) (UC-07, UC-40)
-- --------------------------------------------------------------------
INSERT INTO clients (client_id, user_id, full_name, phone, address, gender, created_at, updated_at)
VALUES 
    (1, 3, 'Huỳnh Đức Minh', '0912345678', 'Quận Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()),
    (2, 5, 'Nguyễn Văn Hùng', '0901234567', 'Quận Ba Đình, Hà Nội', 'MALE', NOW(), NOW()),
    (3, 6, 'Trần Thị Mai', '0902345678', 'Quận Đống Đa, Hà Nội', 'FEMALE', NOW(), NOW()),
    (4, 7, 'Phạm Anh Tuấn', '0903334444', 'Quận Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO tutors (tutor_id, user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
VALUES 
    (1, 4, 'Minh Đức (Gia Sư Toán & Tin Học)', 'MALE', '0987654321', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư chuyên Toán 12 và Luyện thi Đại học khu vực Cầu Giấy.', 250000.00, 5.00, 'VERIFIED', NOW(), NOW()),
    (2, 8, 'Lê Hoàng Nam', 'MALE', '0903456789', 'Quận Cầu Giấy, Hà Nội', 5, 'Chuyên dạy Toán cấp 3 luyện thi đại học khu vực Cầu Giấy', 250000.00, 4.90, 'VERIFIED', NOW(), NOW()),
    (3, 9, 'Phạm Thu Thảo', 'FEMALE', '0904567890', 'Quận 1, TP.HCM', 4, 'Gia sư Tiếng Anh IELTS 8.0 chuyên lớp 10-12', 300000.00, 5.00, 'VERIFIED', NOW(), NOW()),
    (4, 10, 'Nguyễn Văn Toán', 'MALE', '0912111222', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư Toán luyện thi học sinh giỏi cấp Quốc gia', 220000.00, 4.90, 'VERIFIED', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutor_centers (center_id, user_id, company_name, license_no, phone, address, description, verification_status, custom_fee_rate)
VALUES 
    (1, 12, 'Trung Tâm Gia Sư Trí Việt', 'LICENSE-2026-TV', '02838999999', '123 Đường Cầu Giấy, Hà Nội', 'Trung tâm kết nối gia sư uy tín chất lượng cao hàng đầu Hà Nội', 'VERIFIED', 0.0150)
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), custom_fee_rate = 0.0150, verification_status = 'VERIFIED';

-- Ví điện tử (Wallets) phản ánh đúng số dư thực tế & ký quỹ
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES 
    (1, 0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (2, 0.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (3, 5000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (4, 2500000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (5, 7600000.00, 3000000.00, 'ACTIVE', NOW(), NOW()),
    (6, 10000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (7, 5000000.00, 2200000.00, 'ACTIVE', NOW(), NOW()),
    (8, 3000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (9, 6600000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (10, 2000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (12, 15000000.00, 0.00, 'ACTIVE', NOW(), NOW()),
    (27, 0.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE available_balance = VALUES(available_balance), frozen_balance = VALUES(frozen_balance), status = 'ACTIVE';

-- --------------------------------------------------------------------
-- 2. UC-11: DỮ LIỆU DEMO XÁC MINH DANH TÍNH & BẰNG CẤP (SUBMITTED)
-- Sẵn sàng để Admin click Duyệt / Từ chối trên màn hình /platform/verifications
-- --------------------------------------------------------------------
INSERT INTO media_files (file_id, uploaded_by, file_name, file_url, mime_type, file_size, created_at)
VALUES 
    (101, 8, 'cccd_front_lehoangnam.jpg', 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 245120, NOW()),
    (102, 8, 'cccd_back_lehoangnam.jpg', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 261800, NOW()),
    (103, 8, 'bang_daihoc_supham_hanoi.jpg', 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 512400, NOW()),
    (104, 12, 'giay_phep_kinh_doanh_triviet.jpg', 'https://images.unsplash.com/photo-1450133064473-71024230f91b?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 624100, NOW()),
    (105, 12, 'giay_phep_hoat_dong_giao_duc.jpg', 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 583200, NOW()),
    (106, 12, 'cccd_dai_dien_triviet.jpg', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800&auto=format&fit=crop&q=80', 'image/jpeg', 234100, NOW())
ON DUPLICATE KEY UPDATE file_url = VALUES(file_url), uploaded_by = VALUES(uploaded_by);

DELETE FROM verification_histories WHERE verification_id IN (1, 2, 3, 4, 5);
DELETE FROM verification_documents WHERE verification_id IN (1, 2, 3, 4, 5);
DELETE FROM verification_requests WHERE verification_id IN (1, 2, 3, 4, 5);

INSERT INTO verification_requests (verification_id, user_id, verification_type, status, admin_notes, submitted_at, reviewed_at, created_at, updated_at)
VALUES 
    (1, 8, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư Lê Hoàng Nam gửi hồ sơ bằng cấp Sư phạm & CCCD để kiểm duyệt', NOW(), NULL, NOW(), NOW()),
    (2, 12, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', 'Trung tâm Trí Việt nộp giấy phép kinh doanh và cấp phép đào tạo', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NOW(), NOW()),
    (3, 9, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư Phạm Thu Thảo gửi chứng chỉ IELTS 8.0 và bằng ĐH Ngoại ngữ', DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NOW(), NOW()),
    (4, 10, 'TUTOR_PROFILE', 'VERIFIED', 'Hồ sơ bằng cấp và căn cước công dân đầy đủ, hợp lệ, đã xác thực thông tin đối soát thành công.', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (5, 11, 'TUTOR_PROFILE', 'REJECTED', 'Ảnh chụp giấy tờ tùy thân CCCD bị mờ góc thông tin và chói sáng, đề nghị tải lại bản chụp rõ nét.', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO verification_documents (document_id, verification_id, file_id, document_type)
VALUES 
    (101, 1, 101, 'ID_CARD'),
    (102, 1, 102, 'DEGREE'),
    (103, 1, 103, 'CERTIFICATE'),
    (104, 2, 104, 'LICENSE'),
    (105, 2, 105, 'LICENSE'),
    (106, 2, 106, 'ID_CARD'),
    (107, 3, 101, 'ID_CARD'),
    (108, 3, 103, 'CERTIFICATE'),
    (109, 4, 101, 'ID_CARD'),
    (110, 4, 103, 'CERTIFICATE'),
    (111, 5, 102, 'ID_CARD');

-- --------------------------------------------------------------------
-- 3. UC-21: DỮ LIỆU LỚP HỌC ĐANG HOẠT ĐỘNG, TIẾN ĐỘ & LỊCH HỌC
-- --------------------------------------------------------------------
INSERT INTO tutoring_classes (
    class_id, creator_id, subject_id, grade_id, class_type, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES (
    201, 5, 1, 12, 'PRIVATE', 'Luyện thi Đại học môn Toán đạt điểm 9+', 'Gia sư có kinh nghiệm dạy Toán 12 chuyên',
    'Lớp Toán 12 Luyện thi Đại học (Cầu Giấy)', 'Lớp học kèm trực tiếp tại nhà học viên 2 buổi/tuần, chuẩn bị kỳ thi THPT Quốc gia.',
    'OFFLINE', 10, 240000.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY),
    2400000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()
)
ON DUPLICATE KEY UPDATE 
    status = 'IN_PROGRESS', tuition_fee = 240000.00, budget = 2400000.00, subject_id = 1, grade_id = 12;

INSERT INTO tutoring_classes (
    class_id, creator_id, subject_id, grade_id, class_type, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES (
    202, 6, 7, 12, 'PRIVATE', 'Luyện thi IELTS mục tiêu 7.5 Band', 'Gia sư IELTS 8.0 trở lên, nhiệt tình và có phương pháp',
    'Lớp Tiếng Anh IELTS 7.5 Cấp tốc Online', 'Học trực tuyến qua Google Meet / Zoom, rèn luyện 4 kỹ năng Nghe Nói Đọc Viết.',
    'ONLINE', 12, 300000.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY),
    3600000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()
)
ON DUPLICATE KEY UPDATE 
    status = 'IN_PROGRESS', tuition_fee = 300000.00, budget = 3600000.00, subject_id = 7, grade_id = 12;

INSERT INTO tutoring_classes (
    class_id, creator_id, subject_id, grade_id, class_type, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES (
    203, 7, 2, 11, 'PRIVATE', 'Bồi dưỡng học sinh giỏi môn Vật lý 11', 'Sinh viên hoặc giáo viên chuyên Lý ĐH Sư Phạm',
    'Lớp Vật lý 11 Nâng cao Chuyên KHTN', 'Cần gia sư vững lý thuyết và các bài toán đồ thị, dao động cơ học.',
    'OFFLINE', 8, 250000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2000000.00, 'WEEKLY', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()
)
ON DUPLICATE KEY UPDATE 
    status = 'OPEN', tuition_fee = 250000.00, budget = 2000000.00, subject_id = 2, grade_id = 11;

INSERT INTO tutoring_classes (
    class_id, creator_id, subject_id, grade_id, class_type, center_id, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES (
    204, 12, 10, 10, 'CENTER', 1, 'Nhập môn Lập trình Python & Tư duy thuật toán', 'Giảng viên công nghệ thông tin có kinh nghiệm dạy trẻ em',
    'Lớp Lập trình Python Sáng tạo Trẻ em (Online)', 'Lớp học nhóm 5-8 học sinh do Trung tâm Trí Việt tổ chức giảng dạy trực tuyến.',
    'ONLINE', 10, 220000.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2200000.00, 'WEEKLY', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()
)
ON DUPLICATE KEY UPDATE 
    status = 'IN_PROGRESS', tuition_fee = 220000.00, budget = 2200000.00, subject_id = 10, grade_id = 10;

-- Ca học (schedule_slots) cho các lớp
INSERT INTO schedule_slots (slot_id, class_id, day_of_week, start_time, end_time)
VALUES 
    (201, 201, 2, '18:00:00', '20:00:00'),
    (202, 201, 5, '18:00:00', '20:00:00'),
    (203, 202, 3, '19:30:00', '21:00:00'),
    (204, 202, 6, '19:30:00', '21:00:00'),
    (205, 204, 7, '09:00:00', '11:00:00')
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time);

-- Ứng tuyển & Phân công gia sư
INSERT INTO tutor_applications (application_id, class_id, tutor_id, proposed_rate, status, applied_at)
VALUES 
    (201, 201, 2, 240000.00, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 16 DAY)),
    (202, 202, 3, 300000.00, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 13 DAY)),
    (203, 203, 4, 250000.00, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (204, 204, 2, 220000.00, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 11 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO class_assignments (assignment_id, tutor_id, application_id, assigned_date, status, terms_b)
VALUES 
    (1, 2, 1, DATE_SUB(NOW(), INTERVAL 25 DAY), 'TERMINATED', 'Hợp đồng đã kết thúc do phụ huynh xin hủy lớp.'),
    (101, 2, 201, DATE_SUB(NOW(), INTERVAL 15 DAY), 'ACTIVE', 'Thỏa thuận giảng dạy kèm môn Toán 12 theo đúng cam kết chuẩn đầu ra.'),
    (102, 3, 202, DATE_SUB(NOW(), INTERVAL 12 DAY), 'ACTIVE', 'Thỏa thuận đào tạo IELTS 7.5 chuyên sâu.'),
    (103, 4, 203, DATE_SUB(NOW(), INTERVAL 2 DAY), 'ACTIVE', 'Điều khoản thỏa thuận giải quyết tranh chấp học phí.\n\nThông tin nhận hoàn tiền:\n- Tên chủ tài khoản: PHAM ANH TUAN\n- Ngân hàng: Vietcombank\n- Số tài khoản: 0903334444'),
    (104, 2, 204, DATE_SUB(NOW(), INTERVAL 10 DAY), 'ACTIVE', 'Thỏa thuận giảng dạy lớp lập trình thiếu nhi.')
ON DUPLICATE KEY UPDATE status = VALUES(status), tutor_id = VALUES(tutor_id), terms_b = VALUES(terms_b), application_id = VALUES(application_id);

-- Học viên ghi danh (class_students) cho lớp 201 và lớp trung tâm 204
INSERT INTO class_students (class_student_id, class_id, enrolled_by_user_id, student_name, student_phone, status, enrolled_at)
VALUES 
    (201, 201, 5, 'Nguyễn Văn An', '0912345678', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (202, 202, 6, 'Trần Thảo My', '0923456789', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (204, 204, 5, 'Nguyễn Bảo Long', '0934567890', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (205, 204, 6, 'Trần Minh Khang', '0945678901', 'ENROLLED', DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), student_name = VALUES(student_name);

-- Buổi học & Tiến độ (lessons) cho Lớp 201 (Toán 12 - 10 buổi: 5 buổi đã hoàn thành, 5 buổi sắp tới)
INSERT INTO lessons (lesson_id, class_id, lesson_date, slot_id, sequence_no, tutor_id, tutor_check_in_at, tutor_check_out_at, client_confirm_at, attendance_status, created_at)
VALUES 
    (2011, 201, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 201, 1, 2, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), NOW(), 'COMPLETED', NOW()),
    (2012, 201, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 202, 2, 2, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 'COMPLETED', NOW()),
    (2013, 201, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 201, 3, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 'COMPLETED', NOW()),
    (2014, 201, CURDATE(), 202, 4, 2, NOW(), NULL, NULL, 'COMPLETED', NOW()),
    (2015, 201, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 201, 5, 2, NULL, NULL, NULL, 'PENDING', NOW()),
    (2016, 201, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 202, 6, 2, NULL, NULL, NULL, 'PENDING', NOW()),
    (2017, 201, DATE_ADD(CURDATE(), INTERVAL 10 DAY), 201, 7, 2, NULL, NULL, NULL, 'PENDING', NOW()),
    (2018, 201, DATE_ADD(CURDATE(), INTERVAL 14 DAY), 202, 8, 2, NULL, NULL, NULL, 'PENDING', NOW()),
    (2019, 201, DATE_ADD(CURDATE(), INTERVAL 17 DAY), 201, 9, 2, NULL, NULL, NULL, 'PENDING', NOW()),
    (2020, 201, DATE_ADD(CURDATE(), INTERVAL 21 DAY), 202, 10, 2, NULL, NULL, NULL, 'PENDING', NOW())
ON DUPLICATE KEY UPDATE attendance_status = VALUES(attendance_status);

-- Điểm danh chi tiết cho các buổi đã học
INSERT INTO lesson_attendances (attendance_id, lesson_id, class_student_id, status, note, created_at)
VALUES 
    (2011, 2011, 201, 'PRESENT', 'Học đúng giờ, làm bài tập đầy đủ', NOW()),
    (2012, 2012, 201, 'PRESENT', 'Tiếp thu tốt phần Hình học không gian', NOW()),
    (2013, 2013, 201, 'PRESENT', 'Nhiệt tình phát biểu xây dựng bài', NOW()),
    (2014, 2014, 201, 'PRESENT', 'Buổi học hôm nay', NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- --------------------------------------------------------------------
-- 4. UC-58 & UC-41: KÝ QUỸ ĐẢM BẢO ESCROW ĐỦ 6 TRẠNG THÁI & KHOẢN HOÀN TIỀN
-- Bao gồm đúng khoản hoàn tiền 2.400.000đ (REFUND SUCCESS)
-- --------------------------------------------------------------------
-- Giao dịch hoàn tiền 2.400.000đ
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (1, 5, 'REF-TX-2400K-001', 'REFUND', 'SUCCESS', 2400000.00, 'Hoàn trả tiền ký quỹ học phí lớp Toán 12 do gia sư hủy lớp', 'REF-2400000', NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS', amount = 2400000.00;

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, created_at)
VALUES (1, 1, 1, 2400000.00, 'REFUNDED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY))
ON DUPLICATE KEY UPDATE status = 'REFUNDED', amount = 2400000.00;

-- Giao dịch Đã khóa tiền (FUNDED - 3.000.000đ)
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (101, 5, 'ESC-DEP-3000K', 'ESCROW_DEPOSIT', 'SUCCESS', 3000000.00, 'Đặt cọc học phí lớp Toán 12 nâng cao', 'ESC-3000000', NOW(), DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS', amount = 3000000.00;

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, created_at)
VALUES (101, 101, 101, 3000000.00, 'FUNDED', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE status = 'FUNDED', amount = 3000000.00;

-- Giao dịch Đã giải ngân (RELEASED - 3.600.000đ)
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (102, 6, 'ESC-REL-3600K', 'ESCROW_RELEASE', 'SUCCESS', 3600000.00, 'Giải ngân thù lao khóa Tiếng Anh IELTS hoàn thành', 'ESC-3600000', NOW(), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS', amount = 3600000.00;

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, released_at, created_at)
VALUES (102, 102, 102, 3600000.00, 'RELEASED', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY))
ON DUPLICATE KEY UPDATE status = 'RELEASED', amount = 3600000.00;

-- Giao dịch Đang tranh chấp (DISPUTED - 2.200.000đ)
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (103, 7, 'ESC-DISP-2200K', 'ESCROW_DEPOSIT', 'SUCCESS', 2200000.00, 'Ký quỹ hợp đồng tranh chấp', 'ESC-2200000', NOW(), DATE_SUB(NOW(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS', amount = 2200000.00;

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, created_at)
VALUES (103, 103, 103, 2200000.00, 'DISPUTED', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE status = 'DISPUTED', amount = 2200000.00;

-- Giao dịch Chờ nạp (PENDING - 1.800.000đ)
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (104, 5, 'ESC-PEND-1800K', 'ESCROW_DEPOSIT', 'PENDING', 1800000.00, 'Chờ phụ huynh nạp tiền ký quỹ', 'ESC-1800000', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = 'PENDING', amount = 1800000.00;

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, created_at)
VALUES (104, 104, 102, 1800000.00, 'PENDING', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = 'PENDING', amount = 1800000.00;

-- Giao dịch Tạm giữ (ON_HOLD - 2.500.000đ)
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (105, 5, 'ESC-HOLD-2500K', 'ESCROW_DEPOSIT', 'SUCCESS', 2500000.00, 'Tạm giữ tiền ký quỹ điều tra khiếu nại', 'ESC-2500000', NOW(), DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS', amount = 2500000.00;

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, amount, status, deposited_at, created_at)
VALUES (105, 105, 101, 2500000.00, 'ON_HOLD', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE status = 'ON_HOLD', amount = 2500000.00;

-- Giao dịch Ký quỹ lớp trung tâm Trí Việt (CENTER_CLASS_ESCROW - 2.200.000đ)
INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (108, 5, 'ESC-CTR-2200K', 'ESCROW_DEPOSIT', 'SUCCESS', 2200000.00, 'Ký quỹ học phí lớp Lập trình Python (Trung tâm Trí Việt)', 'ESC-2200000-CTR', NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS', amount = 2200000.00, description = VALUES(description);

INSERT INTO escrow_transactions (escrow_id, payment_id, assignment_id, class_student_id, amount, status, deposited_at, created_at)
VALUES (106, 108, NULL, 204, 2200000.00, 'FUNDED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE status = 'FUNDED', amount = 2200000.00;

-- --------------------------------------------------------------------
-- 4.1. UC-43: PHƯƠNG THỨC THANH TOÁN (PAYMENT METHODS) & LỆNH RÚT TIỀN (WITHDRAWALS)
-- --------------------------------------------------------------------
INSERT INTO payment_methods (payment_method_id, wallet_id, type, account_no, account_holder_name, bank_name, status, created_at)
VALUES 
    (1, 8, 'BANK_TRANSFER', '0903456789', 'LE HOANG NAM', 'MB Bank', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (2, 9, 'BANK_TRANSFER', '0904567890', 'PHAM THU THAO', 'Vietcombank', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (3, 12, 'BANK_TRANSFER', '1903999888777', 'TRUNG TAM GIA SU TRI VIET', 'Techcombank', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 20 DAY))
ON DUPLICATE KEY UPDATE account_no = VALUES(account_no), bank_name = VALUES(bank_name), account_holder_name = VALUES(account_holder_name), status = 'ACTIVE';

INSERT INTO payment_transactions (transaction_id, wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES 
    (106, 9, 'WITHDRAW-3000K-001', 'WITHDRAWAL', 'SUCCESS', 3000000.00, 'Rút tiền thù lao về tài khoản ngân hàng Vietcombank', 'WDR-3000000', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (107, 8, 'WITHDRAW-1500K-001', 'WITHDRAWAL', 'PENDING', 1500000.00, 'Rút tiền thù lao về tài khoản ngân hàng MB Bank', 'WDR-1500000', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount), description = VALUES(description);

INSERT INTO withdrawal_requests (withdrawal_id, wallet_id, payment_method_id, bank_name, account_no, account_holder_name, amount, status, requested_at, processed_at)
VALUES 
    (1, 8, 1, 'MB Bank', '0903456789', 'LE HOANG NAM', 1500000.00, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
    (2, 9, 2, 'Vietcombank', '0904567890', 'PHAM THU THAO', 3000000.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount), bank_name = VALUES(bank_name), account_no = VALUES(account_no), account_holder_name = VALUES(account_holder_name);

-- --------------------------------------------------------------------
-- 4.2. UC-51: YÊU CẦU HOÀN TIỀN (REFUND REQUESTS)
-- Gồm: 1 khoản 2.4tr COMPLETED (khớp UC-41) và 1 khoản 1.1tr PENDING (sẵn sàng demo duyệt/từ chối)
-- --------------------------------------------------------------------
INSERT INTO refund_requests (refund_id, escrow_id, requested_by, reason, amount, status, requested_at, processed_at, bank_name, account_no, account_holder_name, refund_reference_code, transfer_status, transfer_processed_at)
VALUES 
    (1, 1, 5, 'Gia sư vào lớp muộn và xin hủy lớp học môn Toán 12, phụ huynh yêu cầu hoàn trả 100% học phí ký quỹ.\n\nThông tin nhận hoàn tiền:\n- Tên chủ tài khoản: NGUYEN VAN HUNG\n- Ngân hàng: MB Bank\n- Số tài khoản: 0901234567', 2400000.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'MB Bank', '0901234567', 'NGUYEN VAN HUNG', 'REF-2400000', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (2, 103, 7, 'Nội dung giảng dạy chưa sát với đề cương bồi dưỡng HSG Vật lý 11, phụ huynh xin rút lại 50% tiền học phí ký quỹ.\n\nThông tin nhận hoàn tiền:\n- Tên chủ tài khoản: PHAM ANH TUAN\n- Ngân hàng: Vietcombank\n- Số tài khoản: 0903334444', 1100000.00, 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, 'Vietcombank', '0903334444', 'PHAM ANH TUAN', 'REF-103-DISP', 'PENDING', NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount), reason = VALUES(reason), bank_name = VALUES(bank_name), account_no = VALUES(account_no), account_holder_name = VALUES(account_holder_name), refund_reference_code = VALUES(refund_reference_code), transfer_status = VALUES(transfer_status);

-- --------------------------------------------------------------------
-- 5. UC-45: MẪU HỢP ĐỒNG ĐIỆN TỬ HỆ THỐNG MASTER
-- --------------------------------------------------------------------
INSERT INTO contract_templates (template_id, name, content, contract_type, created_by, center_id, is_default, status, created_at, updated_at)
VALUES 
    (1, 'Hợp đồng dạy học theo lớp (mặc định toàn hệ thống)',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG DỊCH VỤ DẠY KÈM GIA SƯ\n\nBên A (Khách hàng/Phụ huynh): {{TEN_BEN_A}}\nBên B (Gia sư/Trung tâm): {{TEN_BEN_B}}\n\nĐIỀU 1: NỘI DUNG VÀ MỤC TIÊU KHÓA HỌC\nBên B đồng ý cung cấp dịch vụ giảng dạy kèm cho Bên A môn học theo đúng nội dung và mục tiêu đã thỏa thuận trên nền tảng Tutor Connect System.\n\nĐIỀU 2: HỌC PHÍ VÀ CƠ CHẾ KÝ QUỸ BẢO ĐẢM (ESCROW)\n- Mức học phí thỏa thuận: {{HOC_PHI}} VNĐ.\n- Toàn bộ học phí được ký quỹ qua tài khoản ký quỹ trung gian của sàn TCS để đảm bảo quyền lợi đôi bên.\n- Học phí chỉ được tự động giải ngân cho Bên B sau khi Bên A xác nhận hoàn thành đầy đủ số buổi học quy định.\n\nĐIỀU 3: QUYỀN VÀ NGHĨA VỤ CỦA HAI BÊN\n1. Bên B cam kết đúng giờ, không tự ý hủy buổi học mà không thông báo trước 24 giờ.\n2. Bên A có quyền yêu cầu đổi gia sư hoặc chấm dứt hợp đồng nếu chất lượng giảng dạy không đúng cam kết.\n3. Trường hợp phát sinh mâu thuẫn, hai bên đồng ý để Ban Quản Trị TCS phân xử theo quy định tại UC-49.\n\nĐIỀU 4: HIỆU LỰC HỢP ĐỒNG\nHợp đồng có hiệu lực pháp lý kể từ thời điểm cả hai bên ký số xác nhận qua mã OTP trên hệ thống.',
        'CENTER_CLASS',
        1, NULL, 1, 'ACTIVE', NOW(), NOW()),
    (2, 'Thỏa thuận hợp tác gia sư - trung tâm (mặc định toàn hệ thống)',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nTHỎA THUẬN HỢP TÁC GIẢNG DẠY\n(Dành cho Gia sư & Trung tâm Đào tạo)\n\nBên A: Trung tâm Gia sư đào tạo\nBên B: Gia sư chuyên môn\n\nĐIỀU 1: PHÂN CÔNG VÀ TIẾP NHẬN LỚP DẠY\nTrung tâm phân công các lớp học phù hợp với năng lực và lịch trình đăng ký của Gia sư. Gia sư có trách nhiệm hoàn thành đầy đủ giáo trình giảng dạy.\n\nĐIỀU 2: THÙ LAO VÀ TỶ LỆ CHIA SẺ DOANH THU\n- Trung tâm cam kết thanh toán thù lao đúng hạn theo chu kỳ hàng tháng hoặc theo thỏa thuận.\n- Tỷ lệ chiết khấu và phí quản lý tuân thủ biểu phí minh bạch của sàn TCS.\n\nĐIỀU 3: CAM KẾT CHẤT LƯỢNG VÀ BẢO MẬT\nGia sư không được tự ý thỏa thuận học ngoài nền tảng hoặc lôi kéo học viên của trung tâm (Chống hành vi luồn lách nền tảng theo UC-43).',
        'RECRUITMENT',
        1, NULL, 1, 'ACTIVE', NOW(), NOW()),
    (3, 'Hợp đồng dạy học kèm 1:1 chuyên biệt (Luyện thi chứng chỉ quốc tế)',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG DẠY KÈM 1:1 LUYỆN THI CHỨNG CHỈ QUỐC TẾ (IELTS/TOEIC/SAT)\n\nĐIỀU 1: CAM KẾT ĐẦU RA\nGia sư cam kết lộ trình đào tạo rõ ràng, hướng đến mục tiêu điểm số cam kết trong hợp đồng.\n\nĐIỀU 2: ĐIỀU KIỆN HOÀN TIỀN\nNếu học viên tuân thủ 100% chuyên cần và bài tập mà không đạt mục tiêu đầu ra tối thiểu, sàn TCS sẽ hỗ trợ kích hoạt quy trình hoàn tiền bảo đảm theo chính sách bảo hành học tập.',
        'SPECIALIZED_GUARANTEE',
        1, NULL, 0, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE content = VALUES(content), name = VALUES(name), contract_type = VALUES(contract_type), is_default = VALUES(is_default);

INSERT INTO system_parameters (param_key, param_value, description)
VALUES 
    ('tpltype:1', 'CLASS', 'Loại mẫu hợp đồng #1'),
    ('tpltype:2', 'RECRUITMENT', 'Loại mẫu hợp đồng #2'),
    ('tpltype:3', 'CLASS', 'Loại mẫu hợp đồng #3'),
    ('CONTRACT_TEMPLATE_TYPE_1', 'CENTER_CLASS', 'Loại mẫu hợp đồng #1'),
    ('CONTRACT_TEMPLATE_TYPE_2', 'RECRUITMENT', 'Loại mẫu hợp đồng #2'),
    ('CONTRACT_TEMPLATE_TYPE_3', 'SPECIALIZED_GUARANTEE', 'Loại mẫu hợp đồng #3')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);

-- --------------------------------------------------------------------
-- 6. UC-46: HỢP ĐỒNG ĐIỆN TỬ ĐÃ KÝ (CONTRACTS)
-- --------------------------------------------------------------------
INSERT INTO contracts (contract_id, contract_no, assignment_id, template_id, terms_summary, status, source_type, signed_at, created_at, updated_at)
VALUES 
    (101, 'HD-2026-TOAN12', 101, 1, 'Hợp đồng dạy kèm môn Toán 12 tại nhà học viên', 'ACTIVE', 'PRIVATE', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
    (102, 'HD-2026-IELTS75', 102, 3, 'Hợp đồng khóa luyện thi IELTS 7.5 Online', 'ACTIVE', 'PRIVATE', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
    (103, 'HD-2026-LY11', 103, 1, 'Hợp đồng dạy kèm Vật lý 11 Chuyên KHTN', 'PENDING', 'PRIVATE', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), terms_summary = VALUES(terms_summary);

-- --------------------------------------------------------------------
-- 6.1. UC-44: LỊCH SỬ KÝ HỢP ĐỒNG ĐIỆN TỬ (CONTRACT SIGNATURES)
-- --------------------------------------------------------------------
INSERT INTO contract_signatures (signature_id, contract_id, party_role, signer_id, email, signed_at, signature_data, signature_status)
VALUES 
    (101, 101, 'CLIENT', 5, 'parent.nguyen@gmail.com', DATE_SUB(NOW(), INTERVAL 15 DAY), 'Ký số điện tử OTP qua email (parent.nguyen@gmail.com)', 'SIGNED'),
    (102, 101, 'TUTOR', 8, 'tutor.le@gmail.com', DATE_SUB(NOW(), INTERVAL 15 DAY), 'Ký số điện tử OTP qua email (tutor.le@gmail.com)', 'SIGNED'),
    (103, 102, 'CLIENT', 6, 'parent.tran@gmail.com', DATE_SUB(NOW(), INTERVAL 12 DAY), 'Ký số điện tử OTP qua email (parent.tran@gmail.com)', 'SIGNED'),
    (104, 102, 'TUTOR', 9, 'tutor.pham@gmail.com', DATE_SUB(NOW(), INTERVAL 12 DAY), 'Ký số điện tử OTP qua email (tutor.pham@gmail.com)', 'SIGNED'),
    (105, 103, 'CLIENT', 7, 'parent.tuan@tcs.vn', NULL, NULL, 'PENDING'),
    (106, 103, 'TUTOR', 10, 'tutor.math@tcs.vn', NULL, NULL, 'PENDING')
ON DUPLICATE KEY UPDATE signature_status = VALUES(signature_status), signed_at = VALUES(signed_at);

-- --------------------------------------------------------------------
-- 7. UC-49, UC-30 & UC-55: BÁO CÁO VI PHẠM & TRANH CHẤP KÝ QUỸ
-- --------------------------------------------------------------------
INSERT INTO reports (report_id, reporter_id, target_type, target_id, category, description, evidence_urls, status, created_at, updated_at)
VALUES 
    (1, 5, 'CLASS', 201, 'OTHER', '[UC-29] Báo cáo sự cố lớp học\nMã loại sự cố: LATE_TUTOR\nLoại sự cố: Gia sư vào muộn / Vắng mặt\nBuổi/ngày liên quan: Buổi 3 (10/09/2026)\nNgày xảy ra: 2026-09-10\nMã hướng xử lý: RESCHEDULE\nHướng xử lý mong muốn: Đề nghị sắp xếp buổi học bù\nMô tả: Gia sư vào lớp muộn 30 phút ở buổi học ngày 10/09, đề nghị trung tâm xếp lịch dạy bù.', '["https://images.unsplash.com/photo-1544717305-2782549b5136"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
    (2, 7, 'CLASS', 202, 'OTHER', '[UC-29] Báo cáo sự cố lớp học\nMã loại sự cố: QUALITY_ISSUE\nLoại sự cố: Chất lượng giảng dạy không đạt cam kết\nBuổi/ngày liên quan: Buổi 2 (12/09/2026)\nNgày xảy ra: 2026-09-12\nMã hướng xử lý: REFUND_REQUEST\nHướng xử lý mong muốn: Yêu cầu hoàn trả học phí ký quỹ\nMô tả: Tranh chấp hợp đồng và yêu cầu hoàn trả tiền ký quỹ học phí do chất lượng không đạt cam kết ban đầu.\n\nThông tin nhận hoàn tiền:\n- Tên chủ tài khoản: PHAM ANH TUAN\n- Ngân hàng: Vietcombank\n- Số tài khoản: 0903334444', '["https://images.unsplash.com/photo-1523240795612-9a054b0db644"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
    (3, 8, 'REVIEW', 3, 'ABUSE', 'Nhận xét có lời lẽ bôi nhọ danh dự và xúc phạm nghiêm trọng uy tín giáo viên.', '["https://images.unsplash.com/photo-1573496359142-b8d87734a5a2"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    (4, 6, 'REVIEW', 1, 'INAPPROPRIATE', 'Yêu cầu kiểm duyệt nhận xét có nội dung sai lệch về lịch học.', '["https://images.unsplash.com/photo-1450133064473-71024230f91b"]', 'PENDING', DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW()),
    (5, 5, 'CLASS', 204, 'OTHER', '[UC-29] Báo cáo sự cố lớp học\nMã loại sự cố: SCHEDULE_CONFLICT\nLoại sự cố: Trùng lịch học đột xuất\nBuổi/ngày liên quan: Buổi 1 (11/09/2026)\nNgày xảy ra: 2026-09-11\nMã hướng xử lý: RESCHEDULE\nHướng xử lý mong muốn: Đổi ca học sang buổi tối cuối tuần\nMô tả: Lớp học nhóm cần điều chỉnh ca học sang sáng Chủ nhật để các em học sinh có thể tham gia đầy đủ.', '["https://images.unsplash.com/photo-1507679799987-c73779587ccf"]', 'RESOLVED', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), description = VALUES(description), evidence_urls = VALUES(evidence_urls);

INSERT INTO disputes (dispute_id, report_id, escrow_id, resolution, status, created_at, updated_at)
VALUES 
    (1, 2, 103, 'Hồ sơ đang được Ban Quản Trị xem xét chứng từ đối soát giữa phụ huynh và gia sư.', 'OPEN', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), resolution = VALUES(resolution);

INSERT INTO class_termination_requests (termination_id, assignment_id, class_student_id, requested_by, reason, effective_date, status, created_at, processed_at)
VALUES (1, 103, NULL, 7, 'Phụ huynh xin chấm dứt hợp đồng sớm môn Vật lý 11 do gia sư không đáp ứng chuẩn đầu ra kỳ thi học sinh giỏi.\n\nThông tin nhận hoàn tiền:\n- Tên chủ tài khoản: PHAM ANH TUAN\n- Ngân hàng: Vietcombank\n- Số tài khoản: 0903334444', CURDATE(), 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status), reason = VALUES(reason);

-- --------------------------------------------------------------------
-- 8. UC-51 & UC-66: YÊU CẦU HỖ TRỢ (SUPPORT TICKETS)
-- Đa dạng danh mục: DISPUTE, INQUIRY, BUG_REPORT, SYSTEM_ERROR
-- --------------------------------------------------------------------
INSERT INTO support_tickets (ticket_id, user_id, category, subject, description, priority, status, assigned_admin_id, due_at, created_at, updated_at)
VALUES 
    (1, 5, 'DISPUTE', 'Cần hỗ trợ xử lý tranh chấp khoản ký quỹ #103', 'Tôi đã gửi khiếu nại nhưng chưa thấy admin phản hồi phân xử học phí.', 'HIGH', 'OPEN', 3, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
    (2, 8, 'INQUIRY', 'Tư vấn biểu phí dịch vụ và chu kỳ rút tiền', 'Tôi muốn hỏi thời gian xử lý yêu cầu rút thù lao về tài khoản MB Bank mất bao lâu?', 'MEDIUM', 'IN_PROGRESS', 1, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
    (3, 6, 'BUG_REPORT', 'Lỗi không hiển thị đúng múi giờ ca học buổi tối', 'Lịch học 19:30 bị hiển thị thành 07:30 trên giao diện lịch học cá nhân.', 'LOW', 'OPEN', 1, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    (4, 9, 'INQUIRY', 'Tiêu chuẩn xét duyệt hồ sơ Gia Sư Uy Tín', 'Hồ sơ của tôi đã đạt mốc 4.9 sao và 10 lớp hoàn thành, nhờ admin hướng dẫn cấp huy hiệu.', 'LOW', 'RESOLVED', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), subject = VALUES(subject), assigned_admin_id = VALUES(assigned_admin_id), due_at = VALUES(due_at);

-- --------------------------------------------------------------------
-- 8.1. UC-65 & UC-66: TIN NHẮN PHẢN HỒI YÊU CẦU HỖ TRỢ (TICKET MESSAGES)
-- --------------------------------------------------------------------
INSERT INTO ticket_messages (message_id, ticket_id, sender_id, is_from_admin, content, created_at)
VALUES 
    (1, 1, 5, 0, 'Tôi đã gửi khiếu nại về khoản ký quỹ #103 nhưng chưa thấy admin phản hồi phân xử học phí. Nhờ ban quản trị kiểm tra gấp giúp tôi.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (2, 2, 8, 0, 'Tôi muốn hỏi thời gian xử lý yêu cầu rút thù lao về tài khoản MB Bank mất bao lâu? Phí chuyển khoản do bên nào chịu?', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (3, 2, 1, 1, 'Chào thầy Nam! Lệnh rút tiền về MB Bank thông thường được xử lý tự động trong vòng 24 giờ làm việc. Phí chuyển khoản liên ngân hàng được nền tảng TCS miễn phí hoàn toàn.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (4, 2, 8, 0, 'Cảm ơn admin đã giải đáp nhanh và tận tình!', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (5, 3, 6, 0, 'Lịch học 19:30 bị hiển thị thành 07:30 trên giao diện lịch học cá nhân của tôi.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (6, 4, 9, 0, 'Hồ sơ của tôi đã đạt mốc 4.9 sao và 10 lớp hoàn thành, nhờ admin hướng dẫn cấp huy hiệu Gia Sư Uy Tín.', DATE_SUB(NOW(), INTERVAL 7 DAY)),
    (7, 4, 1, 1, 'Chào cô Thảo! Hệ thống đã tự động kiểm tra đủ điều kiện và kích hoạt huy hiệu Gia Sư Uy Tín trên hồ sơ công khai của bạn.', DATE_SUB(NOW(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- --------------------------------------------------------------------
-- 9. UC-53 & UC-55: ĐÁNH GIÁ GIA SƯ & KIỂM DUYỆT ĐÁNH GIÁ (REVIEWS)
-- --------------------------------------------------------------------
INSERT INTO reviews (review_id, assignment_id, class_id, reviewer_id, reviewee_id, review_type, rating, comment, is_anonymous, status, created_at)
VALUES 
    (1, 102, 202, 6, 9, 'CLIENT_TO_TUTOR', 5.0, 'Cô Thảo dạy cực kỳ nhiệt tình, phương pháp IELTS Speaking rất thực tế, con tôi đã tiến bộ vượt bậc và đạt 7.5!', 0, 'VISIBLE', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (2, 101, 201, 5, 8, 'CLIENT_TO_TUTOR', 4.8, 'Thầy Nam giảng bài môn Toán rất dễ hiểu, bám sát cấu trúc đề thi THPT Quốc gia, giải bài chi tiết.', 0, 'VISIBLE', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (3, 1, 201, 7, 8, 'CLIENT_TO_TUTOR', 1.0, 'Giáo viên thiếu trách nhiệm, tự ý nghỉ không báo trước, lừa dối học viên và nền tảng!', 0, 'MODERATED', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status), rating = VALUES(rating), comment = VALUES(comment);

-- --------------------------------------------------------------------
-- 10. UC-59: THAM SỐ CẤU HÌNH HỆ THỐNG (SYSTEM PARAMETERS)
-- --------------------------------------------------------------------
INSERT INTO system_parameters (param_key, param_value, description)
VALUES 
    ('PLATFORM_FEE_RATE', '0.02', 'Tỷ lệ phí dịch vụ nền tảng trích từ giao dịch hoàn tất (2%)'),
    ('ESCROW_HOLD_DAYS', '7', 'Thời gian tạm giữ ký quỹ bảo đảm trước khi giải ngân tự động (ngày)'),
    ('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng đơn ứng tuyển tối đa cho mỗi lớp học mở tuyển'),
    ('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày nếu người dùng không phản hồi'),
    ('SYSTEM_ANNOUNCEMENTS', '[{"announcementId":1,"title":"Chào mừng năm học mới 2026 cùng Tutor Connect System!","content":"Hệ thống hỗ trợ 100% chi phí ký quỹ cho 500 lớp học đầu tiên trong tháng 9.","targetRole":null,"active":true,"startsAt":"2026-09-01T00:00:00","endsAt":"2026-09-30T23:59:59","createdByAdminId":1,"createdByName":"Quản Trị Viên Hệ Thống","createdAt":"2026-09-01T08:00:00","updatedAt":"2026-09-01T08:00:00"},{"announcementId":2,"title":"Quy định cập nhật biểu phí dịch vụ trung tâm gia sư","content":"Ban Quản Trị thông báo áp dụng chính sách ưu đãi phí sàn riêng biệt cho các trung tâm đào tạo đối tác.","targetRole":"TUTOR_CENTER","active":true,"startsAt":"2026-09-05T00:00:00","endsAt":"2026-10-05T23:59:59","createdByAdminId":1,"createdByName":"Quản Trị Viên Hệ Thống","createdAt":"2026-09-05T09:00:00","updatedAt":"2026-09-05T09:00:00"}]', 'Danh sách thông báo toàn sàn (JSON array)')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value), description = VALUES(description);

-- --------------------------------------------------------------------
-- 11. UC-61: NHẬT KÝ KIỂM TOÁN VẬN HÀNH HỆ THỐNG (AUDIT LOGS)
-- Sẵn sàng cho Admin lọc theo Actor ID, Hành động, Đối tượng, Thời gian
-- --------------------------------------------------------------------
DELETE FROM audit_logs WHERE audit_id BETWEEN 101 AND 112;

INSERT INTO audit_logs (audit_id, actor_id, action, entity_type, entity_id, old_value, new_value, ip_address, created_at)
VALUES 
    (101, 1, 'LOGIN', 'User', 1, NULL, '{"loginMethod": "PASSWORD", "status": "SUCCESS"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (102, 1, 'UPDATE_USER_STATUS', 'User', 8, '{"status": "PENDING"}', '{"status": "ACTIVE"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (103, 1, 'REVIEW_VERIFICATION', 'VerificationRequest', 1, '{"status": "SUBMITTED"}', '{"status": "VERIFIED"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (104, 1, 'UPDATE_FEE_RATE', 'SystemParameter', 1, '{"PLATFORM_FEE_RATE": "0.015"}', '{"PLATFORM_FEE_RATE": "0.02"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (105, 2, 'LOGIN', 'User', 2, NULL, '{"loginMethod": "PASSWORD", "status": "SUCCESS"}', '192.168.1.10', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
    (106, 8, 'SUBMIT_VERIFICATION', 'VerificationRequest', 1, NULL, '{"verificationType": "TUTOR_PROFILE"}', '14.232.208.5', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (107, 5, 'CREATE_REPORT', 'Report', 1, NULL, '{"category": "OTHER", "targetType": "CLASS", "targetId": 201}', '118.70.124.9', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (108, 1, 'RESOLVE_REPORT', 'Report', 5, '{"status": "PENDING"}', '{"status": "RESOLVED", "action": "RESCHEDULE"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (109, 1, 'ISSUE_PENALTY', 'UserPenalty', 1, NULL, '{"penaltyType": "WARNING", "targetUserId": 8, "reason": "Nhắn tin gạ gẫm giao dịch ngoài sàn"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (110, 1, 'REVIEW_CIRCUMVENTION', 'CircumventionEvent', 2, '{"status": "PENDING"}', '{"status": "CONFIRMED", "note": "Xác nhận vi phạm gửi SĐT"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (111, 1, 'APPROVE_REFUND', 'RefundRequest', 1, '{"status": "PENDING"}', '{"status": "COMPLETED", "amount": 2400000, "transferStatus": "SUCCESS"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (112, 1, 'CREATE_DISPUTE', 'Dispute', 1, NULL, '{"reportId": 2, "escrowId": 103, "status": "OPEN"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- --------------------------------------------------------------------
-- 12. UC-67: CÂU HỎI THƯỜNG GẶP (FAQ ENTRIES)
-- --------------------------------------------------------------------
INSERT INTO faq_entries (faq_id, question, answer, category, sort_order, is_published, created_at, updated_at)
VALUES 
    (1, 'Thời gian xét duyệt hồ sơ xác minh gia sư mất bao lâu?', 'Hồ sơ bằng cấp và CCCD của gia sư được Ban Quản Trị TCS thẩm định trong vòng 24 - 48 giờ làm việc kể từ lúc nộp.', 'GIA_SU', 1, 1, NOW(), NOW()),
    (2, 'Cơ chế ký quỹ bảo đảm Escrow hoạt động như thế nào?', 'Học phí của phụ huynh được tạm khóa an toàn trên hệ thống và chỉ giải ngân cho gia sư sau khi học viên xác nhận hoàn thành khóa học.', 'THANH_TOAN', 2, 1, NOW(), NOW()),
    (3, 'Tôi có quyền yêu cầu đổi gia sư hoặc hoàn tiền không?', 'Có. Phụ huynh có quyền gửi yêu cầu đổi gia sư hoặc mở khiếu nại hoàn tiền trong 2 buổi đầu tiên nếu chất lượng không đúng cam kết.', 'LOP_HOC', 3, 1, NOW(), NOW()),
    (4, 'Phí dịch vụ nền tảng TCS là bao nhiêu?', 'TCS áp dụng mức phí dịch vụ minh bạch 2% trên mỗi giao dịch lớp học thành công.', 'CHUNG', 4, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE question = VALUES(question), answer = VALUES(answer);

-- --------------------------------------------------------------------
-- 13. UC-37: THÔNG BÁO HỆ THỐNG CHO NGƯỜI DÙNG (NOTIFICATIONS)
-- --------------------------------------------------------------------
INSERT INTO notifications (user_id, type, title, content, reference_type, reference_id, is_read, status, created_at)
VALUES 
    (8, 'SYSTEM', 'Hồ sơ xác minh gia sư đã được tiếp nhận', 'Hồ sơ bằng cấp Sư phạm của bạn đang được Ban Quản Trị xem xét.', 'VERIFICATION', 1, 0, 'SENT', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (5, 'CLASS', 'Nhắc nhở ca học môn Toán 12', 'Lớp Toán 12 Luyện thi Đại học sẽ bắt đầu vào 18:00 hôm nay.', 'CLASS', 201, 1, 'SENT', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (6, 'PAYMENT', 'Ký quỹ học phí thành công', 'Khoản ký quỹ 3.600.000đ cho khóa học IELTS 7.5 đã được bảo đảm thành công.', 'ESCROW', 102, 1, 'SENT', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (1, 'SYSTEM', 'Có 3 hồ sơ xác minh mới cần kiểm duyệt', 'Gia sư Lê Hoàng Nam, Phạm Thu Thảo và Trung tâm Trí Việt vừa nộp hồ sơ.', 'VERIFICATION', 1, 0, 'SENT', DATE_SUB(NOW(), INTERVAL 1 HOUR))
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- --------------------------------------------------------------------
-- 14. UC-35: MẪU THÔNG BÁO HỆ THỐNG (NOTIFICATION TEMPLATES)
-- --------------------------------------------------------------------
INSERT INTO notification_templates (template_id, code, title_template, content_template, channel, enabled, description, updated_at)
VALUES 
    (1, 'VERIFICATION_APPROVED', 'Hồ sơ xác minh đã được duyệt', 'Chúc mừng {{user_name}}! Hồ sơ xác minh danh tính và bằng cấp của bạn đã được phê duyệt thành công.', 'SYSTEM', 1, 'Thông báo khi admin duyệt xác minh hồ sơ', NOW()),
    (2, 'VERIFICATION_REJECTED', 'Hồ sơ xác minh chưa đạt yêu cầu', 'Rất tiếc, hồ sơ của bạn bị từ chối với lý do: {{reason}}. Vui lòng cập nhật lại giấy tờ.', 'SYSTEM', 1, 'Thông báo khi admin từ chối hồ sơ xác minh', NOW()),
    (3, 'SUPPORT_TICKET_RESPONSE', 'Phản hồi yêu cầu hỗ trợ #{{ticket_id}}', 'Ban quản trị đã phản hồi yêu cầu của bạn: "{{response}}".', 'SYSTEM', 1, 'Thông báo khi admin phản hồi ticket', NOW()),
    (4, 'REPORT_RESOLVED', 'Báo cáo vi phạm đã được xử lý', 'Báo cáo #{{report_id}} đã được xử lý với quyết định: {{decision}}.', 'SYSTEM', 1, 'Thông báo khi xử lý xong báo cáo vi phạm', NOW()),
    (5, 'PENALTY_ISSUED', 'Thông báo quyết định xử lý vi phạm', 'Tài khoản của bạn đã bị áp dụng hình thức xử lý: {{penalty_type}} do {{reason}}.', 'SYSTEM', 1, 'Thông báo khi admin ban hành quyết định xử phạt', NOW()),
    (6, 'CENTER_APPLICATION_RESULT', 'Kết quả ứng tuyển: {{title}}', '{{content}}', 'SYSTEM', 1, 'Thông báo kết quả ứng tuyển gia sư cho trung tâm', NOW())
ON DUPLICATE KEY UPDATE title_template = VALUES(title_template), content_template = VALUES(content_template);

-- --------------------------------------------------------------------
-- 15. UC-59: PHÁT HIỆN HÀNH VI NÉ TRÁNH NỀN TẢNG (CIRCUMVENTION EVENTS)
-- --------------------------------------------------------------------
INSERT INTO conversations (conversation_id, context_type, context_id, type, status, created_at)
VALUES 
    (1, 'CLASS', 201, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (2, 'CLASS', 202, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (3, 'CLASS', 202, 'DIRECT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO conversation_participants (participant_id, conversation_id, user_id, last_read_at)
VALUES 
    (1, 1, 5, NOW()),
    (2, 1, 8, NOW()),
    (3, 2, 6, NOW()),
    (4, 2, 9, NOW()),
    (5, 3, 6, NOW()),
    (6, 3, 9, NOW())
ON DUPLICATE KEY UPDATE last_read_at = VALUES(last_read_at);

INSERT INTO messages (message_id, conversation_id, sender_id, message_type, content, is_edited, is_deleted, sent_at)
VALUES 
    (1, 1, 8, 'TEXT', 'Chào bạn, bạn có thể chuyển tiền trực tiếp qua Zalo 0987654321 hoặc STK MBBank để không mất phí sàn 2% nhé!', 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 2, 9, 'TEXT', 'Em có thể add Zalo chị 0912345678 để trao đổi học ngoài không cần qua sàn nhé.', 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (3, 3, 6, 'TEXT', 'Chào cô Thảo, buổi học IELTS hôm qua con tôi tiếp thu rất tốt, cảm ơn cô nhé!', 0, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (4, 3, 9, 'TEXT', 'Dạ em cảm ơn chị! Bé My rất chăm chỉ và phát âm chuẩn, tuần tới em sẽ tăng cường thêm bài tập Reading ạ.', 0, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (5, 3, 6, 'TEXT', 'Vâng cô, nhờ cô rèn thêm cho cháu phần Writing Task 2 nữa nhé.', 0, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (6, 3, 9, 'TEXT', 'Dạ vâng chị, em đã chuẩn bị sẵn giáo trình mẫu cho phần Writing rồi ạ.', 0, 0, DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO circumvention_events (event_id, message_id, conversation_id, sender_id, matched_rule, evidence, risk_score, status, review_note, reviewed_by, reviewed_at, created_at)
VALUES 
    (1, 1, 1, 8, 'PAYMENT_KEYWORD', 'Chuyển tiền trực tiếp qua Zalo 0987654321 hoặc STK MBBank để không mất phí', 85, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 2, 2, 9, 'PHONE_EXCHANGE', 'add Zalo chị 0912345678 để trao đổi học ngoài', 75, 'CONFIRMED', 'Xác nhận gia sư gửi số điện thoại lôi kéo học viên ra ngoài sàn. Đã ban hành cảnh cáo.', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (3, 3, 3, 6, 'PROFANITY', 'trao đổi học ngoài không qua sàn', 40, 'DISMISSED', 'Tin nhắn bình thường giữa phụ huynh và gia sư, không phát hiện hành vi né tránh nền tảng.', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE evidence = VALUES(evidence), status = VALUES(status), review_note = VALUES(review_note), reviewed_by = VALUES(reviewed_by), reviewed_at = VALUES(reviewed_at);

-- --------------------------------------------------------------------
-- 16. UC-60: XỬ PHẠT TÀI KHOẢN VI PHẠM (USER PENALTIES)
-- --------------------------------------------------------------------
INSERT INTO user_penalties (penalty_id, user_id, issued_by, penalty_type, reason, evidence_urls, restriction_details, source_type, source_id, source_task_id, starts_at, expires_at, status, created_at)
VALUES 
    (1, 8, 1, 'WARNING', 'Có hành vi nhắn tin gạ gẫm phụ huynh thanh toán học phí ngoài nền tảng TCS.', '["https://images.unsplash.com/photo-1544717305-2782549b5136"]', '{"maxClasses": 1}', 'CIRCUMVENTION', 2, 'TASK-CIRC-01', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 7, 1, 'FEATURE_RESTRICTION', 'Hủy lớp học sát giờ nhiều lần mà không có lý do chính đáng.', '["https://images.unsplash.com/photo-1523240795612-9a054b0db644"]', '{"canApplyClasses": false}', 'REPORT', 1, 'TASK-REPORT-01', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (3, 4, 1, 'TEMPORARY_BAN', 'Đình chỉ tài khoản 14 ngày do có hành vi vi phạm thỏa thuận giảng dạy và phát sinh tranh chấp học phí chưa giải quyết.', '["https://images.unsplash.com/photo-1573496359142-b8d87734a5a2"]', '{"banDays": 14}', 'DISPUTE', 1, 'TASK-DISP-01', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 12 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE reason = VALUES(reason), status = VALUES(status), evidence_urls = VALUES(evidence_urls), restriction_details = VALUES(restriction_details);

-- --------------------------------------------------------------------
-- 17. UC-08: HỒ SƠ CHUYÊN MÔN GIA SƯ (HỌC VẤN, KINH NGHIỆM, CHỨNG CHỈ)
-- --------------------------------------------------------------------
INSERT INTO tutor_educations (education_id, tutor_id, institution, degree, field_of_study, start_year, end_year)
VALUES 
    (101, 1, 'Đại học Bách Khoa Hà Nội', 'Kỹ sư Công nghệ Thông tin & Toán ứng dụng', 'Khoa học Máy tính', 2016, 2020),
    (102, 2, 'Đại học Sư phạm Hà Nội', 'Cử nhân Sư phạm Toán học', 'Toán học & Phương pháp giảng dạy', 2017, 2021),
    (103, 3, 'Đại học Ngoại ngữ - ĐHQGHN', 'Cử nhân Sư phạm Tiếng Anh', 'Ngôn ngữ Anh & Giảng dạy TESOL', 2018, 2022),
    (104, 4, 'Đại học Khoa học Tự nhiên - ĐHQGHN', 'Cử nhân Toán học Tài chính', 'Toán giải tích và xác suất thống kê', 2017, 2021)
ON DUPLICATE KEY UPDATE institution = VALUES(institution), degree = VALUES(degree), field_of_study = VALUES(field_of_study);

INSERT INTO tutor_experiences (experience_id, tutor_id, role, organization, start_date, end_date, description)
VALUES 
    (100, 1, 'Gia sư Toán & Tin học nâng cao', 'Nền tảng Tutor Connect System', '2021-01-01', NULL, 'Kèm hơn 20 học sinh ôn thi chuyên Tin và đạt giải học sinh giỏi cấp quận/thành phố.'),
    (101, 2, 'Giáo viên bộ môn Toán', 'THPT Chuyên Hà Nội - Amsterdam', '2021-09-01', '2023-06-30', 'Giảng dạy Toán nâng cao khối 11, 12 và bồi dưỡng đội tuyển HSG cấp thành phố.'),
    (102, 2, 'Gia sư Toán luyện thi Đại học', 'Nền tảng Tutor Connect System', '2023-07-01', NULL, 'Kèm 1:1 hơn 15 học sinh thi đỗ nguyện vọng 1 các trường Đại học Bách Khoa, KTQD.'),
    (103, 3, 'Giảng viên IELTS', 'Học viện Anh ngữ Quốc tế', '2022-08-01', NULL, 'Đào tạo học viên đạt Target IELTS 7.0 - 8.0 cho mục đích du học và xét tuyển đại học.'),
    (104, 4, 'Trợ giảng bộ môn Toán giải tích', 'Đại học Khoa học Tự nhiên', '2021-08-01', '2023-12-31', 'Hỗ trợ giảng dạy chuyên đề giải tích và bồi dưỡng đội tuyển Olympic Toán sinh viên.')
ON DUPLICATE KEY UPDATE role = VALUES(role), organization = VALUES(organization), description = VALUES(description);

INSERT INTO tutor_certificates (certificate_id, tutor_id, name, issuer, issue_date, file_id)
VALUES 
    (100, 1, 'Chứng chỉ Lập trình Quốc tế OCA Java SE', 'Oracle Corporation', '2020-11-15', 103),
    (101, 2, 'Chứng chỉ Nghiệp vụ Sư phạm Quốc gia', 'Đại học Sư phạm Hà Nội', '2021-06-25', 103),
    (102, 3, 'Chứng chỉ IELTS 8.0 Overall', 'British Council Vietnam', '2023-03-15', 103),
    (103, 4, 'Chứng chỉ Bồi dưỡng Giáo viên Toán THPT', 'Viện Nghiên cứu Sư phạm', '2022-05-20', 103)
ON DUPLICATE KEY UPDATE name = VALUES(name), issuer = VALUES(issuer);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'DỮ LIỆU SEED DEMO ĐÃ NẠP THÀNH CÔNG CHO TOÀN BỘ 30 USE CASES!' AS Result;


