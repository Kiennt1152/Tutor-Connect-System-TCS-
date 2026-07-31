-- Script tạo contract test id=67 để demo UC-44
USE tutorconnectsystem;

-- 1. Seed data
INSERT IGNORE INTO categories (name, description, type, status) VALUES ('Giáo dục', 'Danh mục giáo dục', 'SYSTEM_CONFIG', 'ACTIVE');
INSERT IGNORE INTO subjects (subject_name, description) VALUES ('Toán', 'Môn toán học');
INSERT IGNORE INTO grades (grade_name) VALUES ('Lớp 10');
INSERT IGNORE INTO locations (address_line, province_id) VALUES ('TP. Hồ Chí Minh', 1);

-- 2. Users test
INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at)
VALUES
  ('test.client67@tcs.com', '0900123001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW(), NOW()),
  ('test.tutor67@tcs.com', '0900123002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW(), NOW()),
  ('test.center67@tcs.com', '0900123003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW(), NOW());

-- 3. Profiles
INSERT IGNORE INTO clients (user_id, full_name, phone)
SELECT user_id, 'Phụ huynh Test 67', '0900123001' FROM users WHERE email = 'test.client67@tcs.com';

INSERT IGNORE INTO tutors (user_id, full_name, gender, phone, experience_years, hourly_rate, rating_avg, bio)
SELECT user_id, 'Gia Sư Test 67', 'MALE', '0900123002', 3, 150000.00, 4.80, 'Gia sư toán' FROM users WHERE email = 'test.tutor67@tcs.com';

INSERT IGNORE INTO tutor_centers (user_id, company_name, license_no, phone, address, description)
SELECT user_id, 'Trung Tâm Test 67', 'TEST-TCS-67', '0900123003', 'TP.HCM', 'Trung tâm dạy thêm' FROM users WHERE email = 'test.center67@tcs.com';

-- 4. Wallets
INSERT IGNORE INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at)
SELECT user_id, 0.00, 0.00, 'ACTIVE', NOW() FROM users WHERE email IN ('test.client67@tcs.com', 'test.tutor67@tcs.com', 'test.center67@tcs.com');

-- 5. Contract template
INSERT IGNORE INTO contract_templates (name, content, created_by, is_default, status)
SELECT 'Mẫu HĐ Gia Sư 67', '<p>Hợp đồng dịch vụ gia sư tại nhà. Phí: 150,000 đ/giờ.</p>', user_id, 1, 'ACTIVE' FROM users WHERE email = 'test.center67@tcs.com';

-- 6. Tutoring class
INSERT IGNORE INTO tutoring_classes (creator_id, class_type, center_id, category_id, subject_id, grade_id, title, description, location_id, lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, recurring_type, status, created_at)
SELECT
  (SELECT user_id FROM users WHERE email = 'test.center67@tcs.com'),
  'CENTER',
  (SELECT user_id FROM users WHERE email = 'test.center67@tcs.com'),
  (SELECT category_id FROM categories WHERE name = 'Giáo dục'),
  (SELECT subject_id FROM subjects WHERE subject_name = 'Toán'),
  (SELECT grade_id FROM grades WHERE grade_name = 'Lớp 10'),
  'Lớp Toán Lớp 10 Test 67',
  'Lớp toán nâng cao cho học sinh lớp 10',
  (SELECT location_id FROM locations WHERE address_line = 'TP. Hồ Chí Minh'),
  'OFFLINE',
  8,
  2000000.00,
  '2026-07-01',
  '2026-09-01',
  'WEEKLY',
  'OPEN',
  NOW();

-- 7. Class student
INSERT IGNORE INTO class_students (class_id, enrolled_by_user_id, student_name, student_phone, status)
SELECT
  (SELECT class_id FROM tutoring_classes WHERE title = 'Lớp Toán Lớp 10 Test 67'),
  (SELECT user_id FROM users WHERE email = 'test.client67@tcs.com'),
  'Học sinh Test 67',
  '0900123001',
  'ENROLLED';

-- 8. Contract 67
DELETE FROM contract_signatures WHERE contract_id = 67;
DELETE FROM contracts WHERE contract_id = 67;

INSERT INTO contracts (contract_id, contract_no, class_student_id, template_id, contract_file_url, terms_summary, status, source_type, signed_at, created_at, updated_at)
SELECT
  67,
  'HD-2026-0067',
  (SELECT class_student_id FROM class_students WHERE student_name = 'Học sinh Test 67'),
  (SELECT template_id FROM contract_templates WHERE name = 'Mẫu HĐ Gia Sư 67'),
  '/uploads/contracts/HD-2026-0067.pdf',
  'Hợp đồng dịch vụ gia sư tại nhà. Phí: 150,000 đ/giờ. Thời gian: 2 buổi/tuần.',
  'DRAFT',
  'CENTER',
  NULL,
  NOW(),
  NOW();

-- 9. Signatures (3 bên)
INSERT INTO contract_signatures (party_role, contract_id, signer_id, email, otp_code, otp_expires_at, otp_attempts, signed_at, signature_status)
SELECT 'CLIENT', 67, user_id, email, NULL, NULL, 0, NOW(), 'PENDING' FROM users WHERE email = 'test.client67@tcs.com';

INSERT INTO contract_signatures (party_role, contract_id, signer_id, email, otp_code, otp_expires_at, otp_attempts, signed_at, signature_status)
SELECT 'TUTOR', 67, user_id, email, NULL, NULL, 0, NOW(), 'PENDING' FROM users WHERE email = 'test.tutor67@tcs.com';

INSERT INTO contract_signatures (party_role, contract_id, signer_id, email, otp_code, otp_expires_at, otp_attempts, signed_at, signature_status)
SELECT 'CENTER', 67, user_id, email, NULL, NULL, 0, NOW(), 'PENDING' FROM users WHERE email = 'test.center67@tcs.com';

-- 10. Verify
SELECT '=== CONTRACT ===' AS info;
SELECT contract_id, contract_no, status, source_type, created_at FROM contracts WHERE contract_id = 67;

SELECT '=== SIGNATURES ===' AS info;
SELECT cs.signature_id, cs.party_role, u.email, cs.signature_status
FROM contract_signatures cs
JOIN users u ON cs.signer_id = u.user_id
WHERE cs.contract_id = 67;

SELECT '';
SELECT '===== TAI KHOAN TEST =====' AS info;
SELECT 'Email: test.client67@tcs.com  | Password: Test@1234  | Role: CLIENT' AS login_info;
SELECT 'Email: test.tutor67@tcs.com   | Password: Test@1234  | Role: TUTOR' AS login_info;
SELECT 'Email: test.center67@tcs.com | Password: Test@1234  | Role: TUTOR_CENTER' AS login_info;
SELECT 'URL:   http://localhost:3000/contract/67' AS test_url;
