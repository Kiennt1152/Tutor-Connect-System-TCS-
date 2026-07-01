-- V10: Verification history notes + DRAFT status support
-- 1. Add note column to verification_histories (admin comment captured in audit log)
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'verification_histories'
       AND COLUMN_NAME = 'note') = 0,
    'ALTER TABLE verification_histories ADD COLUMN note TEXT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Drop old status check and recreate to include DRAFT
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_verification_status_v3') > 0,
    'ALTER TABLE verification_requests DROP CHECK chk_verification_status_v3',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_verification_status_v10') = 0,
    'ALTER TABLE verification_requests ADD CONSTRAINT chk_verification_status_v10 CHECK (status IN (''DRAFT'', ''SUBMITTED'', ''UNDER_REVIEW'', ''VERIFIED'', ''REJECTED''))',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;