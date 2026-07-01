-- V3: Verification System Schema Update
-- Aligns verification_requests table with UCS spec:
--   SUBMITTED -> UNDER_REVIEW -> VERIFIED | REJECTED

-- 1. Add missing columns (idempotent)
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'verification_requests'
       AND COLUMN_NAME = 'reviewed_by') = 0,
    'ALTER TABLE verification_requests ADD COLUMN reviewed_by BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'verification_requests'
       AND COLUMN_NAME = 'admin_notes') = 0,
    'ALTER TABLE verification_requests ADD COLUMN admin_notes TEXT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'verification_requests'
       AND COLUMN_NAME = 'rejection_reason') = 0,
    'ALTER TABLE verification_requests ADD COLUMN rejection_reason TEXT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Drop old CHECK constraints (idempotent via conditional)
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_verification_status') > 0,
    'ALTER TABLE verification_requests DROP CHECK chk_verification_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_verification_type') > 0,
    'ALTER TABLE verification_requests DROP CHECK chk_verification_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Set default status to SUBMITTED (matches UCS postcondition)
ALTER TABLE verification_requests MODIFY status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED';

-- 4. Add new CHECK constraint matching UCS lifecycle (idempotent)
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_verification_status_v3') = 0,
    'ALTER TABLE verification_requests ADD CONSTRAINT chk_verification_status_v3 CHECK (status IN (''SUBMITTED'', ''UNDER_REVIEW'', ''VERIFIED'', ''REJECTED''))',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Add verification type CHECK (idempotent)
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_verification_type_v3') = 0,
    'ALTER TABLE verification_requests ADD CONSTRAINT chk_verification_type_v3 CHECK (verification_type IN (''TUTOR_PROFILE'', ''TUTOR_CENTER_LICENSE''))',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
