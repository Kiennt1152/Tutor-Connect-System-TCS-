-- BF-10 Platform Administration & Analytics demo data.
-- Run manually against the local tutorconnectsystem database.
-- Idempotent: rerunning restores the demo records without duplicating them.

SET NAMES utf8mb4;
START TRANSACTION;

-- Password for both demo accounts: 12345678
SET @demo_password_hash = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW';
SET @client_email = 'demo.bf10.client@tcs.local';
SET @tutor_email = 'demo.bf10.tutor@tcs.local';

INSERT INTO users (email, phone, password_hash, status, created_at, updated_at)
VALUES (@client_email, '0901001010', @demo_password_hash, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

INSERT INTO users (email, phone, password_hash, status, created_at, updated_at)
VALUES (@tutor_email, '0901002020', @demo_password_hash, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

SET @client_user_id = (SELECT user_id FROM users WHERE email = @client_email LIMIT 1);
SET @tutor_user_id = (SELECT user_id FROM users WHERE email = @tutor_email LIMIT 1);

INSERT INTO clients (user_id, full_name, phone, address, created_at, updated_at)
VALUES (@client_user_id, 'Khách hàng Demo BF10', '0901001010', 'Hà Nội', NOW(), NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), phone = VALUES(phone), updated_at = NOW();

INSERT INTO tutors (
    user_id, full_name, gender, phone, address, experience_years, bio,
    hourly_rate, rating_avg, verification_status, created_at, updated_at
)
VALUES (
    @tutor_user_id, 'Gia sư Demo BF10', 'MALE', '0901002020', 'Hà Nội', 4,
    'Gia sư demo cho luồng quản trị và phân tích BF-10.',
    250000.00, 4.80, 'UNDER_VERIFY', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name), bio = VALUES(bio), hourly_rate = VALUES(hourly_rate),
    verification_status = 'UNDER_VERIFY', updated_at = NOW();

-- Safe catalog item whose description can be edited during the demo.
INSERT INTO categories (name, description, is_active, sort_order, type, status, created_at, updated_at)
VALUES (
    'Tin học văn phòng',
    '[DEMO BF10] Danh mục thực hành Word, Excel và PowerPoint. Có thể sửa mô tả khi demo.',
    1, 90, 'SUBJECT', 'ACTIVE', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description), is_active = 1, status = 'ACTIVE', updated_at = NOW();

SET @demo_category_id = (SELECT category_id FROM categories WHERE name = 'Tin học văn phòng' LIMIT 1);
SET @demo_subject_id = (SELECT subject_id FROM subjects WHERE subject_name LIKE 'Tin h%' ORDER BY subject_id LIMIT 1);
SET @demo_grade_id = (SELECT grade_id FROM grades ORDER BY grade_id LIMIT 1);

-- Analytics: at least one class. The title is the idempotency key.
INSERT INTO tutoring_classes (
    creator_id, class_type, category_id, subject_id, grade_id, learning_goal,
    tutor_requirement, title, description, lesson_mode, number_of_sessions,
    tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
SELECT
    @client_user_id, 'PRIVATE', @demo_category_id, @demo_subject_id, @demo_grade_id,
    'Thành thạo kỹ năng tin học văn phòng', 'Gia sư có kinh nghiệm Word và Excel',
    '[DEMO BF10] Lớp Tin học văn phòng',
    'Dữ liệu lớp học dùng để minh họa KPI và báo cáo Analytics BF-10.',
    'ONLINE', 8, 250000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2000000.00, 'WEEKLY', 'OPEN', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tutoring_classes WHERE title = '[DEMO BF10] Lớp Tin học văn phòng'
);

UPDATE tutoring_classes
SET category_id = @demo_category_id,
    subject_id = @demo_subject_id,
    status = 'OPEN',
    updated_at = NOW()
WHERE title = '[DEMO BF10] Lớp Tin học văn phòng';

SET @demo_class_id = (
    SELECT class_id FROM tutoring_classes WHERE title = '[DEMO BF10] Lớp Tin học văn phòng' ORDER BY class_id LIMIT 1
);

-- Analytics: wallet and a successful deposit transaction.
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (@client_user_id, 1500000.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

INSERT INTO payment_transactions (
    wallet_id, external_transaction_id, type, status, amount, description,
    reference_code, processed_at, created_at
)
SELECT
    @client_user_id, 'DEMO-BF10-PAYMENT-001', 'DEPOSIT', 'SUCCESS', 1500000.00,
    '[DEMO BF10] Giao dịch thành công dùng cho Analytics.',
    'DEMO-BF10-PAYMENT-001', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM payment_transactions WHERE reference_code = 'DEMO-BF10-PAYMENT-001'
);

UPDATE payment_transactions
SET status = 'SUCCESS', amount = 1500000.00, processed_at = NOW()
WHERE reference_code = 'DEMO-BF10-PAYMENT-001';

-- Aggregate task queue: a submitted tutor verification.
INSERT INTO verification_requests (
    user_id, verification_type, status, admin_notes, submitted_at, created_at, updated_at
)
SELECT
    @tutor_user_id, 'TUTOR_PROFILE', 'SUBMITTED',
    '[DEMO BF10] Hồ sơ cho hàng đợi tác vụ tổng hợp.', NOW(), NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM verification_requests
    WHERE user_id = @tutor_user_id AND verification_type = 'TUTOR_PROFILE'
);

UPDATE verification_requests
SET status = 'SUBMITTED',
    admin_notes = '[DEMO BF10] Hồ sơ cho hàng đợi tác vụ tổng hợp.',
    submitted_at = NOW(), reviewed_at = NULL, updated_at = NOW()
WHERE user_id = @tutor_user_id AND verification_type = 'TUTOR_PROFILE';

-- Required client support ticket. INQUIRY permits the requested LOW priority.
INSERT INTO support_tickets (
    user_id, target_class_id, category, subject, description, priority, status,
    due_at, sla_breached, response_sla_ms, created_at, updated_at
)
SELECT
    @client_user_id, @demo_class_id, 'INQUIRY',
    '[DEMO BF10] Không nhận được xác nhận thanh toán',
    'Tôi đã thanh toán nhưng chưa thấy trạng thái lớp học được cập nhật. Nhờ bộ phận hỗ trợ kiểm tra.',
    'LOW', 'OPEN', DATE_ADD(NOW(), INTERVAL 48 HOUR), 0, 172800000, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM support_tickets
    WHERE subject = '[DEMO BF10] Không nhận được xác nhận thanh toán'
);

UPDATE support_tickets
SET user_id = @client_user_id,
    target_class_id = @demo_class_id,
    category = 'INQUIRY',
    description = 'Tôi đã thanh toán nhưng chưa thấy trạng thái lớp học được cập nhật. Nhờ bộ phận hỗ trợ kiểm tra.',
    priority = 'LOW', status = 'OPEN', assigned_admin_id = NULL,
    due_at = DATE_ADD(NOW(), INTERVAL 48 HOUR), sla_breached = 0,
    response_sla_ms = 172800000, resolved_at = NULL, closed_at = NULL, updated_at = NOW()
WHERE subject = '[DEMO BF10] Không nhận được xác nhận thanh toán';

SET @demo_ticket_id = (
    SELECT ticket_id FROM support_tickets
    WHERE subject = '[DEMO BF10] Không nhận được xác nhận thanh toán'
    ORDER BY ticket_id LIMIT 1
);

INSERT INTO ticket_messages (ticket_id, sender_id, is_from_admin, content, created_at)
SELECT
    @demo_ticket_id, @client_user_id, 0,
    'Tôi đã thanh toán nhưng chưa thấy trạng thái lớp học được cập nhật. Nhờ bộ phận hỗ trợ kiểm tra.',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_messages
    WHERE ticket_id = @demo_ticket_id AND sender_id = @client_user_id AND is_from_admin = 0
);

COMMIT;

-- Verification output and the value to restore after changing configuration in the demo.
SELECT param_key, param_value AS value_before_demo
FROM system_parameters
WHERE param_key = 'PLATFORM_FEE_RATE';

SELECT 'users' AS metric, COUNT(*) AS value FROM users
UNION ALL SELECT 'tutors', COUNT(*) FROM tutors
UNION ALL SELECT 'classes', COUNT(*) FROM tutoring_classes
UNION ALL SELECT 'successful_transactions', COUNT(*) FROM payment_transactions WHERE status = 'SUCCESS'
UNION ALL SELECT 'pending_verifications', COUNT(*) FROM verification_requests WHERE status IN ('SUBMITTED', 'UNDER_REVIEW')
UNION ALL SELECT 'open_support_tickets', COUNT(*) FROM support_tickets WHERE status IN ('OPEN', 'IN_PROGRESS', 'IN_REVIEW');

SELECT ticket_id, subject, priority, status, due_at
FROM support_tickets
WHERE subject = '[DEMO BF10] Không nhận được xác nhận thanh toán';

SELECT category_id, name, description, status
FROM categories
WHERE name = 'Tin học văn phòng';
