-- ====================================================================
-- SEED SCRIPT FOR MANUAL TESTING: ST-PROF-001 TO ST-PROF-006
-- Accounts created:
-- 1. admin01@tcs.test   / 12345678 (PLATFORM_ADMIN)
-- 2. tutor01@tcs.test   / 12345678 (TUTOR, VERIFIED - for ST-PROF-006)
-- 3. tutor02@tcs.test   / 12345678 (TUTOR, UNDER_VERIFY - for ST-PROF-001, 002, 004, 005)
-- 4. center01@tcs.test  / 12345678 (TUTOR_CENTER, UNDER_VERIFY - for ST-PROF-003)
-- BCrypt hash for '12345678': $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;

-- 1. USERS
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
('admin01@tcs.test', '0901110001', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('tutor01@tcs.test', '0901110002', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('tutor02@tcs.test', '0901110003', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('center01@tcs.test', '0901110004', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    status = 'ACTIVE',
    updated_at = NOW();

-- 2. ADMIN
SET @admin_id = (SELECT user_id FROM users WHERE email = 'admin01@tcs.test' LIMIT 1);
INSERT INTO platform_admins (user_id, full_name)
VALUES (@admin_id, 'Admin Verification Officer 01')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 3. TUTOR 01 (VERIFIED - ST-PROF-006)
SET @tutor01_id = (SELECT user_id FROM users WHERE email = 'tutor01@tcs.test' LIMIT 1);
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
VALUES (@tutor01_id, 'Gia Sư Kiểm Thử 01', 'MALE', '0901110002', 'Quận Cầu Giấy, Hà Nội', 3, 'Gia sư Toán Tin kiểm thử ST-PROF-006', 200000.00, 5.00, 'VERIFIED', NOW(), NOW())
ON DUPLICATE KEY UPDATE verification_status = 'VERIFIED', updated_at = NOW();

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (@tutor01_id, 1000000.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

-- 4. TUTOR 02 (UNDER_VERIFY - ST-PROF-001, ST-PROF-002, ST-PROF-004, ST-PROF-005)
SET @tutor02_id = (SELECT user_id FROM users WHERE email = 'tutor02@tcs.test' LIMIT 1);
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
VALUES (@tutor02_id, 'Gia Sư Kiểm Thử 02', 'FEMALE', '0901110003', 'Quận Đống Đa, Hà Nội', 2, 'Gia sư chưa xác minh hồ sơ', 180000.00, 0.00, 'UNDER_VERIFY', NOW(), NOW())
ON DUPLICATE KEY UPDATE verification_status = 'UNDER_VERIFY', updated_at = NOW();

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (@tutor02_id, 500000.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

-- Clean any existing verification requests for tutor02 so it starts clean
DELETE FROM verification_documents WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = @tutor02_id);
DELETE FROM verification_histories WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = @tutor02_id);
DELETE FROM verification_requests WHERE user_id = @tutor02_id;

-- Pre-seed CCCD for tutor02 in system_parameters so CccdSection check passes in UI
INSERT INTO system_parameters (param_key, param_value, description)
VALUES (
    CONCAT('cccd:', @tutor02_id),
    '{"cccdNumber":"001200000002","fullName":"Gia Sư Kiểm Thử 02","dateOfBirth":"15/08/1998","gender":"Nữ","permanentAddress":"Quận Đống Đa, Hà Nội","issueDate":"10/05/2021","issuePlace":"Cục Cảnh sát QLHC về TTXH","phone":"0901110003","workplace":"","tempResidence":""}',
    'Thông tin CCCD của user (cho hợp đồng)'
)
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);

-- 5. CENTER 01 (UNDER_VERIFY - ST-PROF-003)
SET @center01_id = (SELECT user_id FROM users WHERE email = 'center01@tcs.test' LIMIT 1);
INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, verification_status, created_at, updated_at)
VALUES (@center01_id, 'Trung Tâm Giáo Dục TCS Test 01', 'MSDN-0109999888', '0901110004', 'Số 123 Đường Cầu Giấy, Hà Nội', 'UNDER_VERIFY', NOW(), NOW())
ON DUPLICATE KEY UPDATE verification_status = 'UNDER_VERIFY', updated_at = NOW();

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (@center01_id, 2000000.00, 0.00, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();

-- Clean any existing verification requests for center01 so it starts clean
DELETE FROM verification_documents WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = @center01_id);
DELETE FROM verification_histories WHERE verification_id IN (SELECT verification_id FROM verification_requests WHERE user_id = @center01_id);
DELETE FROM verification_requests WHERE user_id = @center01_id;

-- Pre-seed CCCD for center01 legal rep in system_parameters
INSERT INTO system_parameters (param_key, param_value, description)
VALUES (
    CONCAT('cccd:', @center01_id),
    '{"cccdNumber":"001200000003","fullName":"Nguyễn Văn Đại Diện","dateOfBirth":"20/11/1985","gender":"Nam","permanentAddress":"Số 123 Cầu Giấy, Hà Nội","issueDate":"15/09/2021","issuePlace":"Cục Cảnh sát QLHC về TTXH","phone":"0901110004","workplace":"Trung Tâm TCS Test 01","tempResidence":""}',
    'Thông tin CCCD của user (cho hợp đồng)'
)
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);
