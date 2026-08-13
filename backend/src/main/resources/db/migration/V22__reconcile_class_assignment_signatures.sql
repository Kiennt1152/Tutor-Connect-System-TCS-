-- V8 was expanded after some local databases had already recorded it as applied.
-- Reconcile the contract-signing columns without failing on fresh databases.
SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
       AND COLUMN_NAME = 'tutor_signed_at') = 0,
    'ALTER TABLE class_assignments ADD COLUMN tutor_signed_at DATETIME NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
       AND COLUMN_NAME = 'client_signed_at') = 0,
    'ALTER TABLE class_assignments ADD COLUMN client_signed_at DATETIME NULL AFTER tutor_signed_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
       AND COLUMN_NAME = 'payment_method') = 0,
    'ALTER TABLE class_assignments ADD COLUMN payment_method VARCHAR(20) NULL AFTER client_signed_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
       AND COLUMN_NAME = 'terms_b') = 0,
    'ALTER TABLE class_assignments ADD COLUMN terms_b TEXT NULL AFTER payment_method',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
