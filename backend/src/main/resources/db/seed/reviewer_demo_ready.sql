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

-- --------------------------------------------------------------------
-- 1. UC-07: ĐẢM BẢO TẤT CẢ USER TỪ 1 ĐẾN 36 HOẠT ĐỘNG & ĐỒNG BỘ MẬT KHẨU
-- Mật khẩu chung: '12345678' -> $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
-- --------------------------------------------------------------------
UPDATE users 
SET password_hash = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW',
    status = 'ACTIVE'
WHERE user_id BETWEEN 1 AND 36;

-- Đảm bảo tài khoản admin chính xác
INSERT INTO platform_admins (user_id, full_name)
VALUES 
    (1, 'Quản Trị Viên Hệ Thống'),
    (2, 'Admin TCS'),
    (27, 'Quản Trị Viên Đức')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

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

DELETE FROM verification_histories WHERE verification_id IN (1, 2, 3);
DELETE FROM verification_documents WHERE verification_id IN (1, 2, 3);
DELETE FROM verification_requests WHERE verification_id IN (1, 2, 3);

INSERT INTO verification_requests (verification_id, user_id, verification_type, status, admin_notes, submitted_at, created_at, updated_at)
VALUES 
    (1, 8, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư Lê Hoàng Nam gửi hồ sơ bằng cấp Sư phạm & CCCD để kiểm duyệt', NOW(), NOW(), NOW()),
    (2, 12, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', 'Trung tâm Trí Việt nộp giấy phép kinh doanh và cấp phép đào tạo', DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW(), NOW()),
    (3, 9, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư Phạm Thu Thảo gửi chứng chỉ IELTS 8.0 và bằng ĐH Ngoại ngữ', DATE_SUB(NOW(), INTERVAL 5 HOUR), NOW(), NOW());

INSERT INTO verification_documents (document_id, verification_id, file_id, document_type)
VALUES 
    (101, 1, 101, 'ID_CARD'),
    (102, 1, 102, 'DEGREE'),
    (103, 1, 103, 'CERTIFICATE'),
    (104, 2, 104, 'LICENSE'),
    (105, 2, 105, 'LICENSE'),
    (106, 2, 106, 'ID_CARD'),
    (107, 3, 101, 'ID_CARD'),
    (108, 3, 103, 'CERTIFICATE');

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

INSERT INTO class_assignments (assignment_id, tutor_id, application_id, assigned_date, status)
VALUES 
    (1, 2, NULL, DATE_SUB(NOW(), INTERVAL 25 DAY), 'TERMINATED'),
    (101, 2, 201, DATE_SUB(NOW(), INTERVAL 15 DAY), 'ACTIVE'),
    (102, 3, 202, DATE_SUB(NOW(), INTERVAL 12 DAY), 'ACTIVE'),
    (103, 4, 203, DATE_SUB(NOW(), INTERVAL 2 DAY), 'ACTIVE'),
    (104, 2, 204, DATE_SUB(NOW(), INTERVAL 10 DAY), 'ACTIVE')
ON DUPLICATE KEY UPDATE status = VALUES(status), tutor_id = VALUES(tutor_id);

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

-- --------------------------------------------------------------------
-- 5. UC-45: MẪU HỢP ĐỒNG ĐIỆN TỬ HỆ THỐNG MASTER
-- --------------------------------------------------------------------
INSERT INTO contract_templates (template_id, name, content, created_by, center_id, is_default, status, created_at, updated_at)
VALUES 
    (1, 'Hợp đồng dạy học theo lớp (mặc định toàn hệ thống)',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG DỊCH VỤ DẠY KÈM GIA SƯ\n\nBên A (Khách hàng/Phụ huynh): {{TEN_BEN_A}}\nBên B (Gia sư/Trung tâm): {{TEN_BEN_B}}\n\nĐIỀU 1: NỘI DUNG VÀ MỤC TIÊU KHÓA HỌC\nBên B đồng ý cung cấp dịch vụ giảng dạy kèm cho Bên A môn học theo đúng nội dung và mục tiêu đã thỏa thuận trên nền tảng Tutor Connect System.\n\nĐIỀU 2: HỌC PHÍ VÀ CƠ CHẾ KÝ QUỸ BẢO ĐẢM (ESCROW)\n- Mức học phí thỏa thuận: {{HOC_PHI}} VNĐ.\n- Toàn bộ học phí được ký quỹ qua tài khoản ký quỹ trung gian của sàn TCS để đảm bảo quyền lợi đôi bên.\n- Học phí chỉ được tự động giải ngân cho Bên B sau khi Bên A xác nhận hoàn thành đầy đủ số buổi học quy định.\n\nĐIỀU 3: QUYỀN VÀ NGHĨA VỤ CỦA HAI BÊN\n1. Bên B cam kết đúng giờ, không tự ý hủy buổi học mà không thông báo trước 24 giờ.\n2. Bên A có quyền yêu cầu đổi gia sư hoặc chấm dứt hợp đồng nếu chất lượng giảng dạy không đúng cam kết.\n3. Trường hợp phát sinh mâu thuẫn, hai bên đồng ý để Ban Quản Trị TCS phân xử theo quy định tại UC-49.\n\nĐIỀU 4: HIỆU LỰC HỢP ĐỒNG\nHợp đồng có hiệu lực pháp lý kể từ thời điểm cả hai bên ký số xác nhận qua mã OTP trên hệ thống.',
        1, NULL, 1, 'ACTIVE', NOW(), NOW()),
    (2, 'Thỏa thuận hợp tác gia sư - trung tâm (mặc định toàn hệ thống)',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nTHỎA THUẬN HỢP TÁC GIẢNG DẠY\n(Dành cho Gia sư & Trung tâm Đào tạo)\n\nBên A: Trung tâm Gia sư đào tạo\nBên B: Gia sư chuyên môn\n\nĐIỀU 1: PHÂN CÔNG VÀ TIẾP NHẬN LỚP DẠY\nTrung tâm phân công các lớp học phù hợp với năng lực và lịch trình đăng ký của Gia sư. Gia sư có trách nhiệm hoàn thành đầy đủ giáo trình giảng dạy.\n\nĐIỀU 2: THÙ LAO VÀ TỶ LỆ CHIA SẺ DOANH THU\n- Trung tâm cam kết thanh toán thù lao đúng hạn theo chu kỳ hàng tháng hoặc theo thỏa thuận.\n- Tỷ lệ chiết khấu và phí quản lý tuân thủ biểu phí minh bạch của sàn TCS.\n\nĐIỀU 3: CAM KẾT CHẤT LƯỢNG VÀ BẢO MẬT\nGia sư không được tự ý thỏa thuận học ngoài nền tảng hoặc lôi kéo học viên của trung tâm (Chống hành vi luồn lách nền tảng theo UC-43).',
        1, NULL, 1, 'ACTIVE', NOW(), NOW()),
    (3, 'Hợp đồng dạy học kèm 1:1 chuyên biệt (Luyện thi chứng chỉ quốc tế)',
        'CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG DẠY KÈM 1:1 LUYỆN THI CHỨNG CHỈ QUỐC TẾ (IELTS/TOEIC/SAT)\n\nĐIỀU 1: CAM KẾT ĐẦU RA\nGia sư cam kết lộ trình đào tạo rõ ràng, hướng đến mục tiêu điểm số cam kết trong hợp đồng.\n\nĐIỀU 2: ĐIỀU KIỆN HOÀN TIỀN\nNếu học viên tuân thủ 100% chuyên cần và bài tập mà không đạt mục tiêu đầu ra tối thiểu, sàn TCS sẽ hỗ trợ kích hoạt quy trình hoàn tiền bảo đảm theo chính sách bảo hành học tập.',
        1, NULL, 0, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE content = VALUES(content), name = VALUES(name), is_default = VALUES(is_default);

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
-- 7. UC-49, UC-30 & UC-55: BÁO CÁO VI PHẠM & TRANH CHẤP KÝ QUỸ
-- --------------------------------------------------------------------
INSERT INTO reports (report_id, reporter_id, target_type, target_id, category, description, status, created_at, updated_at)
VALUES 
    (1, 5, 'CLASS', 201, 'OTHER', 'Gia sư vào lớp muộn 30 phút ở buổi học ngày 10/09, đề nghị trung tâm xếp lịch dạy bù.', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
    (2, 7, 'CLASS', 202, 'OTHER', 'Tranh chấp hợp đồng và yêu cầu hoàn trả tiền ký quỹ học phí do chất lượng không đạt cam kết ban đầu.', 'PENDING', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
    (3, 8, 'REVIEW', 3, 'ABUSE', 'Nhận xét có lời lẽ bôi nhọ danh dự và xúc phạm nghiêm trọng uy tín giáo viên.', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), description = VALUES(description);

INSERT INTO disputes (dispute_id, report_id, escrow_id, resolution, status, created_at, updated_at)
VALUES 
    (1, 2, 103, 'Hồ sơ đang được Ban Quản Trị xem xét chứng từ đối soát giữa phụ huynh và gia sư.', 'OPEN', DATE_SUB(NOW(), INTERVAL 3 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), resolution = VALUES(resolution);

-- --------------------------------------------------------------------
-- 8. UC-51: YÊU CẦU HỖ TRỢ (SUPPORT TICKETS)
-- Đa dạng danh mục: DISPUTE, INQUIRY, BUG_REPORT, SYSTEM_ERROR
-- --------------------------------------------------------------------
INSERT INTO support_tickets (ticket_id, user_id, category, subject, description, priority, status, assigned_admin_id, created_at, updated_at)
VALUES 
    (1, 5, 'DISPUTE', 'Cần hỗ trợ xử lý tranh chấp khoản ký quỹ #103', 'Tôi đã gửi khiếu nại nhưng chưa thấy admin phản hồi phân xử học phí.', 'HIGH', 'OPEN', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
    (2, 8, 'INQUIRY', 'Tư vấn biểu phí dịch vụ và chu kỳ rút tiền', 'Tôi muốn hỏi thời gian xử lý yêu cầu rút thù lao về tài khoản MB Bank mất bao lâu?', 'MEDIUM', 'IN_PROGRESS', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
    (3, 6, 'BUG_REPORT', 'Lỗi không hiển thị đúng múi giờ ca học buổi tối', 'Lịch học 19:30 bị hiển thị thành 07:30 trên giao diện lịch học cá nhân.', 'LOW', 'OPEN', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
    (4, 9, 'INQUIRY', 'Tiêu chuẩn xét duyệt hồ sơ Gia Sư Uy Tín', 'Hồ sơ của tôi đã đạt mốc 4.9 sao và 10 lớp hoàn thành, nhờ admin hướng dẫn cấp huy hiệu.', 'LOW', 'RESOLVED', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), subject = VALUES(subject);

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
    ('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày nếu người dùng không phản hồi')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value), description = VALUES(description);

-- --------------------------------------------------------------------
-- 11. UC-61: NHẬT KÝ KIỂM TOÁN VẬN HÀNH HỆ THỐNG (AUDIT LOGS)
-- Sẵn sàng cho Admin lọc theo Actor ID, Hành động, Đối tượng, Thời gian
-- --------------------------------------------------------------------
DELETE FROM audit_logs WHERE audit_id BETWEEN 101 AND 108;

INSERT INTO audit_logs (audit_id, actor_id, action, entity_type, entity_id, old_value, new_value, ip_address, created_at)
VALUES 
    (101, 1, 'LOGIN', 'User', 1, NULL, '{"loginMethod": "PASSWORD", "status": "SUCCESS"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (102, 1, 'UPDATE_USER_STATUS', 'User', 8, '{"status": "PENDING"}', '{"status": "ACTIVE"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (103, 1, 'REVIEW_VERIFICATION', 'VerificationRequest', 1, '{"status": "SUBMITTED"}', '{"status": "VERIFIED"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
    (104, 1, 'UPDATE_FEE_RATE', 'SystemParameter', 1, '{"PLATFORM_FEE_RATE": "0.015"}', '{"PLATFORM_FEE_RATE": "0.02"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (105, 2, 'LOGIN', 'User', 2, NULL, '{"loginMethod": "PASSWORD", "status": "SUCCESS"}', '192.168.1.10', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
    (106, 8, 'SUBMIT_VERIFICATION', 'VerificationRequest', 1, NULL, '{"verificationType": "TUTOR_PROFILE"}', '14.232.208.5', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (107, 5, 'CREATE_REPORT', 'Report', 1, NULL, '{"category": "OTHER", "targetType": "CLASS", "targetId": 201}', '118.70.124.9', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (108, 1, 'RESOLVE_REPORT', 'Report', 1, '{"status": "PENDING"}', '{"status": "RESOLVED", "action": "RESCHEDULE"}', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

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

SELECT 'DỮ LIỆU SEED DEMO ĐÃ NẠP THÀNH CÔNG CHO TOÀN BỘ 13 USE CASES!' AS Result;
