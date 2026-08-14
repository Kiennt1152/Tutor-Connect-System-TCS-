SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;

-- ====================================================================
-- SEED SCRIPT: TẠO TÀI KHOẢN THEO YÊU CẦU & DỮ LIỆU ĐĂNG NHẬP
-- Mật khẩu cho 2 tài khoản mới: ducminh1011
-- Hash BCrypt: $2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS
-- ====================================================================

-- 1. Tài khoản Phụ huynh / Học sinh: haehuynh35@gmail.com (ducminh1011)
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at)
VALUES (
    'haehuynh35@gmail.com', 
    '0912345678', 
    '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS', 
    'ACTIVE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE 
    password_hash = '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS',
    status = 'ACTIVE',
    updated_at = NOW();

SET @client_user_id = (SELECT user_id FROM users WHERE email = 'haehuynh35@gmail.com' LIMIT 1);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
VALUES (
    @client_user_id, 
    'Huỳnh Đức Minh (Phụ Huynh)', 
    '0912345678', 
    'Quận Cầu Giấy, Hà Nội', 
    'MALE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE 
    full_name = 'Huỳnh Đức Minh (Phụ Huynh)',
    phone = '0912345678',
    updated_at = NOW();

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (
    @client_user_id, 
    5000000.00, 
    0.00, 
    'ACTIVE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();


-- 2. Tài khoản Gia sư: minhduc101dz@gmail.com (ducminh1011)
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at)
VALUES (
    'minhduc101dz@gmail.com', 
    '0987654321', 
    '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS', 
    'ACTIVE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE 
    password_hash = '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS',
    status = 'ACTIVE',
    updated_at = NOW();

SET @tutor_user_id = (SELECT user_id FROM users WHERE email = 'minhduc101dz@gmail.com' LIMIT 1);

INSERT INTO tutors (
    user_id, full_name, gender, phone, address, experience_years, bio,
    hourly_rate, rating_avg, verification_status, created_at, updated_at
)
VALUES (
    @tutor_user_id, 
    'Minh Đức (Gia Sư Toán & Tin Học)', 
    'MALE', 
    '0987654321', 
    'Quận Cầu Giấy, Hà Nội', 
    5, 
    'Gia sư chuyên Toán 12 và Luyện thi Đại học khu vực Cầu Giấy.', 
    250000.00, 
    5.00, 
    'VERIFIED', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE 
    full_name = 'Minh Đức (Gia Sư Toán & Tin Học)',
    verification_status = 'VERIFIED',
    updated_at = NOW();

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
VALUES (
    @tutor_user_id, 
    2000000.00, 
    0.00, 
    'ACTIVE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE status = 'ACTIVE', updated_at = NOW();


-- 3. Tài khoản Quản trị viên (Platform Admin): thanhkiu0209@gmail.com (12345678)
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
