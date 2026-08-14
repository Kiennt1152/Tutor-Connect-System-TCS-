SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'payment_methods' AND column_name = 'account_holder_name') = 0,
    'ALTER TABLE payment_methods ADD COLUMN account_holder_name VARCHAR(150) NULL AFTER account_no',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'payment_methods' AND column_name = 'verified_at') = 0,
    'ALTER TABLE payment_methods ADD COLUMN verified_at DATETIME NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'payment_methods' AND column_name = 'cooldown_until') = 0,
    'ALTER TABLE payment_methods ADD COLUMN cooldown_until DATETIME NULL AFTER verified_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'payment_methods' AND column_name = 'last_used_at') = 0,
    'ALTER TABLE payment_methods ADD COLUMN last_used_at DATETIME NULL AFTER cooldown_until',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'payment_methods' AND column_name = 'created_at') = 0,
    'ALTER TABLE payment_methods ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER last_used_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'payment_methods' AND column_name = 'updated_at') = 0,
    'ALTER TABLE payment_methods ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'withdrawal_requests' AND column_name = 'bank_name') = 0,
    'ALTER TABLE withdrawal_requests ADD COLUMN bank_name VARCHAR(100) NULL AFTER payment_method_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'withdrawal_requests' AND column_name = 'account_no') = 0,
    'ALTER TABLE withdrawal_requests ADD COLUMN account_no VARCHAR(50) NULL AFTER bank_name',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'withdrawal_requests' AND column_name = 'account_holder_name') = 0,
    'ALTER TABLE withdrawal_requests ADD COLUMN account_holder_name VARCHAR(150) NULL AFTER account_no',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @schema_name AND table_name = 'refund_requests' AND column_name = 'account_holder_name') = 0,
    'ALTER TABLE refund_requests ADD COLUMN account_holder_name VARCHAR(150) NULL AFTER account_no',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
