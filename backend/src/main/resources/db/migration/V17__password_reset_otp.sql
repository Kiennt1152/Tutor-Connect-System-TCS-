-- Password reset uses the existing email OTP table.
-- Existing databases need the purpose constraint widened before Hibernate starts.
SET NAMES utf8mb4;

SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_email_otps_purpose') > 0,
  'ALTER TABLE email_otps DROP CHECK chk_email_otps_purpose',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE email_otps
    ADD CONSTRAINT chk_email_otps_purpose
    CHECK (purpose IN ('REGISTRATION', 'CONTRACT_SIGNING', 'PASSWORD_RESET'));
