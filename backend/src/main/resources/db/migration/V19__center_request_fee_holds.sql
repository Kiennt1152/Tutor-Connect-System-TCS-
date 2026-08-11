SET NAMES utf8mb4;

CREATE TABLE center_request_fee_holds (
    fee_hold_id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    client_user_id BIGINT NOT NULL,
    center_user_id BIGINT NOT NULL,
    center_name VARCHAR(150) NULL,
    payment_transaction_id BIGINT NOT NULL,
    projected_escrow_amount DECIMAL(15,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reference_code VARCHAR(100) NOT NULL,
    payout_bank_name VARCHAR(100) NOT NULL,
    payout_account_no VARCHAR(50) NOT NULL,
    payout_account_holder_name VARCHAR(150) NOT NULL,
    class_id BIGINT NULL,
    assignment_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    paid_at DATETIME NULL,
    released_at DATETIME NULL,
    refunded_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_center_request_fee_holds PRIMARY KEY (fee_hold_id),
    CONSTRAINT uq_center_request_fee_holds_request UNIQUE (request_id),
    CONSTRAINT uq_center_request_fee_holds_payment UNIQUE (payment_transaction_id),
    CONSTRAINT uq_center_request_fee_holds_reference UNIQUE (reference_code),
    CONSTRAINT fk_center_request_fee_holds_payment FOREIGN KEY (payment_transaction_id)
        REFERENCES payment_transactions (transaction_id),
    CONSTRAINT fk_center_request_fee_holds_client FOREIGN KEY (client_user_id)
        REFERENCES users (user_id),
    CONSTRAINT fk_center_request_fee_holds_center FOREIGN KEY (center_user_id)
        REFERENCES users (user_id),
    CONSTRAINT fk_center_request_fee_holds_class FOREIGN KEY (class_id)
        REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_center_request_fee_holds_assignment FOREIGN KEY (assignment_id)
        REFERENCES class_assignments (assignment_id),
    CONSTRAINT chk_center_request_fee_holds_status
        CHECK (status IN ('PENDING_PAYMENT','HELD','REFUND_REQUESTED','RELEASED','REFUNDED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE refund_requests
    MODIFY escrow_id BIGINT NULL,
    ADD COLUMN center_request_fee_hold_id BIGINT NULL AFTER escrow_id,
    ADD CONSTRAINT fk_refund_requests_center_request_fee_hold
        FOREIGN KEY (center_request_fee_hold_id) REFERENCES center_request_fee_holds (fee_hold_id);
