-- File nay mot seed file de tao tai khoan test voi firstLogin=true (profile_completed_at NULL).
-- Sau khi backend build voi spring-security-crypto, chay qua MySQL client voi BCryptPasswordEncoder
-- duoc su dung de insert password_hash chinh xac.
--
-- Approach an toan: su dung tool Java nho (xem DevSeed.java) de:
--   1. Lay BCryptPasswordEncoder (cung instance nhu Spring Security trong backend)
--   2. INSERT user + profile row theo role
--   3. INSERT wallets row
-- mac dinh profile_completed_at = NULL => firstLogin=true.
--
-- Password cho ca 3 tai khoan: Test@1234

USE tutorconnectsystem;

-- CLIENT test (chua chinh ho so)
DELETE FROM clients WHERE user_id IN (SELECT user_id FROM users WHERE email='onboarding.client@example.com');
DELETE FROM users WHERE email='onboarding.client@example.com';
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at)
VALUES ('onboarding.client@example.com', '0901234001', '__CLIENT_HASH__', 'ACTIVE', NOW(), NOW(), NULL);
SET @cid = LAST_INSERT_ID();
INSERT INTO clients (user_id, full_name, phone, avatar) VALUES (@cid, 'Client Onboarding', '0901234001', NULL);
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at) VALUES (@cid, 0, 0, 'ACTIVE', NOW());

-- TUTOR test (chua chinh ho so)
DELETE FROM tutors WHERE user_id IN (SELECT user_id FROM users WHERE email='onboarding.tutor@example.com');
DELETE FROM users WHERE email='onboarding.tutor@example.com';
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at)
VALUES ('onboarding.tutor@example.com', '0901234002', '__TUTOR_HASH__', 'ACTIVE', NOW(), NOW(), NULL);
SET @tid = LAST_INSERT_ID();
INSERT INTO tutors (user_id, full_name, gender, phone, experience_years, hourly_rate, rating_avg, bio, avatar)
VALUES (@tid, 'Tutor Onboarding', 'OTHER', '0901234002', 0, 0, 0, NULL, NULL);
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at) VALUES (@tid, 0, 0, 'ACTIVE', NOW());

-- TUTOR_CENTER test (chua chinh ho so)
DELETE FROM tutor_centers WHERE user_id IN (SELECT user_id FROM users WHERE email='onboarding.center@example.com');
DELETE FROM users WHERE email='onboarding.center@example.com';
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at)
VALUES ('onboarding.center@example.com', '0901234003', '__CENTER_HASH__', 'ACTIVE', NOW(), NOW(), NULL);
SET @centerid = LAST_INSERT_ID();
INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, description, avatar)
VALUES (@centerid, 'Center Onboarding', 'TEST-CENTER-001', '0901234003', 'N/A', NULL, NULL);
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at) VALUES (@centerid, 0, 0, 'ACTIVE', NOW());

SELECT email, role_dummy, profile_completed_at IS NULL AS firstLogin_should_be_true FROM users
WHERE email IN ('onboarding.client@example.com','onboarding.tutor@example.com','onboarding.center@example.com');
