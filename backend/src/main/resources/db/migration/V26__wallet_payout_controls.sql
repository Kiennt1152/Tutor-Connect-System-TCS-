SET NAMES utf8mb4;

ALTER TABLE payment_methods
    ADD COLUMN account_holder_name VARCHAR(150) NULL AFTER account_no,
    ADD COLUMN verified_at DATETIME NULL AFTER status,
    ADD COLUMN cooldown_until DATETIME NULL AFTER verified_at,
    ADD COLUMN last_used_at DATETIME NULL AFTER cooldown_until,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER last_used_at,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE withdrawal_requests
    ADD COLUMN bank_name VARCHAR(100) NULL AFTER payment_method_id,
    ADD COLUMN account_no VARCHAR(50) NULL AFTER bank_name,
    ADD COLUMN account_holder_name VARCHAR(150) NULL AFTER account_no;

ALTER TABLE refund_requests
    ADD COLUMN account_holder_name VARCHAR(150) NULL AFTER account_no;
