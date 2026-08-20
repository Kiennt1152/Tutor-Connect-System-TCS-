-- =====================================================================
-- Mọi OTP (đăng ký / quên mật khẩu / ký hợp đồng) đã gộp về bảng email_otps.
-- Ba cột OTP còn sót trên contract_signatures không còn được ghi nữa -> gỡ bỏ.
-- Dùng kiểm tra information_schema để chạy được cả trên DB đã không còn cột.
-- =====================================================================

SET NAMES utf8mb4;

SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE contract_signatures DROP COLUMN otp_code',
              'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contract_signatures'
      AND COLUMN_NAME = 'otp_code');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE contract_signatures DROP COLUMN otp_expires_at',
              'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contract_signatures'
      AND COLUMN_NAME = 'otp_expires_at');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE contract_signatures DROP COLUMN otp_attempts',
              'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contract_signatures'
      AND COLUMN_NAME = 'otp_attempts');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
