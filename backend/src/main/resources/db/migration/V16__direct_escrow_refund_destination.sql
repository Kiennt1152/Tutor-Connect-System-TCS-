ALTER TABLE refund_requests
    ADD COLUMN bank_name VARCHAR(100) NULL AFTER amount,
    ADD COLUMN account_no VARCHAR(50) NULL AFTER bank_name,
    ADD COLUMN refund_reference_code VARCHAR(100) NULL AFTER account_no,
    ADD COLUMN transfer_status VARCHAR(20) NULL AFTER refund_reference_code,
    ADD COLUMN transfer_processed_at DATETIME NULL AFTER transfer_status;
