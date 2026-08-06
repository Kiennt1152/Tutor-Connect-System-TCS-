-- V11: E-Contract signatures + OTP + PENDING status + expiry
-- Required for UC-44 (M4 - DucHM)
-- Prerequisite: V9__user_profile_completed_at.sql and V8__center_class_support.sql must have run
--
-- Idempotent: uses IF [NOT] EXISTS so this script can be re-run safely on
-- a partially-applied database (DDL in MySQL auto-commits and is not rolled back
-- automatically when the migration fails).

-- ---------------------------------------------------------------------
-- 1. contracts: PENDING status + expires_at + source_type + confirmed_at
-- ---------------------------------------------------------------------

-- status: widen to VARCHAR(20) and re-add the check with PENDING included
ALTER TABLE contracts MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Drop check (ignore if missing) and recreate with PENDING allowed
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME   = 'chk_contracts_status') > 0,
  'ALTER TABLE contracts DROP CHECK chk_contracts_status',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE contracts ADD CONSTRAINT chk_contracts_status
    CHECK (status IN ('PENDING','DRAFT','SIGNED','ACTIVE','COMPLETED','TERMINATED'));

-- expires_at
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contracts'
       AND COLUMN_NAME   = 'expires_at') = 0,
  'ALTER TABLE contracts ADD COLUMN expires_at DATETIME NULL AFTER signed_at',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- source_type
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contracts'
       AND COLUMN_NAME   = 'source_type') = 0,
  'ALTER TABLE contracts ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT ''PRIVATE'' AFTER status',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- confirmed_at
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contracts'
       AND COLUMN_NAME   = 'confirmed_at') = 0,
  'ALTER TABLE contracts ADD COLUMN confirmed_at DATETIME NULL AFTER expires_at',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- Drop & recreate chk_contracts_target
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME   = 'chk_contracts_target') > 0,
  'ALTER TABLE contracts DROP CHECK chk_contracts_target',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE contracts ADD CONSTRAINT chk_contracts_target
    CHECK ((assignment_id IS NULL) <> (class_student_id IS NULL));

-- ---------------------------------------------------------------------
-- 2. contract_signatures: party_role + OTP fields + email + status
-- ---------------------------------------------------------------------

-- party_role
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contract_signatures'
       AND COLUMN_NAME   = 'party_role') = 0,
  'ALTER TABLE contract_signatures ADD COLUMN party_role VARCHAR(20) NOT NULL COMMENT ''CLIENT TUTOR CENTER'' AFTER signature_id',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- email
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contract_signatures'
       AND COLUMN_NAME   = 'email') = 0,
  'ALTER TABLE contract_signatures ADD COLUMN email VARCHAR(255) NULL AFTER signer_id',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- otp_code
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contract_signatures'
       AND COLUMN_NAME   = 'otp_code') = 0,
  'ALTER TABLE contract_signatures ADD COLUMN otp_code VARCHAR(6) NULL AFTER email',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- otp_expires_at
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contract_signatures'
       AND COLUMN_NAME   = 'otp_expires_at') = 0,
  'ALTER TABLE contract_signatures ADD COLUMN otp_expires_at DATETIME NULL AFTER otp_code',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- otp_attempts
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contract_signatures'
       AND COLUMN_NAME   = 'otp_attempts') = 0,
  'ALTER TABLE contract_signatures ADD COLUMN otp_attempts INT NOT NULL DEFAULT 0 AFTER otp_expires_at',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- signature_status
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'contract_signatures'
       AND COLUMN_NAME   = 'signature_status') = 0,
  'ALTER TABLE contract_signatures ADD COLUMN signature_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING SIGNED EXPIRED'' AFTER signature_data',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- chk_signatures_status
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME   = 'chk_signatures_status') > 0,
  'ALTER TABLE contract_signatures DROP CHECK chk_signatures_status',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE contract_signatures ADD CONSTRAINT chk_signatures_status
    CHECK (signature_status IN ('PENDING','SIGNED','EXPIRED'));

-- chk_otp_attempts
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME   = 'chk_otp_attempts') > 0,
  'ALTER TABLE contract_signatures DROP CHECK chk_otp_attempts',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE contract_signatures ADD CONSTRAINT chk_otp_attempts
    CHECK (otp_attempts >= 0 AND otp_attempts <= 5);

-- chk_party_role
SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME   = 'chk_party_role') > 0,
  'ALTER TABLE contract_signatures DROP CHECK chk_party_role',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE contract_signatures ADD CONSTRAINT chk_party_role
    CHECK (party_role IN ('CLIENT','TUTOR','CENTER'));
