-- ======================================================================
-- V10: UC-44 contract e-signature OTP + contract_otps table
-- ======================================================================

CREATE TABLE IF NOT EXISTS contract_otps (
    otp_id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    contract_id     BIGINT       NOT NULL,
    signer_user_id  BIGINT       NOT NULL,
    otp_code        VARCHAR(10)  NOT NULL,
    expires_at      DATETIME     NOT NULL,
    consumed_at     DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_contract_signer (contract_id, signer_user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
