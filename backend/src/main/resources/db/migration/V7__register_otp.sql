-- =====================================================================
-- UC-01 Register Account - Xac thuc email bang OTP truoc khi tao tai khoan.
--   email_otps                : ma OTP gan theo email (chua co tai khoan)
--   email_verification_tokens : token chung nhan email da xac thuc (BR-UC01-05)
-- Noi license_no cua tutor_centers thanh NULL: dang ky chi thu thap baseline,
--   so giay phep duoc bo sung sau khi dang nhap (BR-UC01-09).
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE email_otps (
    otp_id       BIGINT       NOT NULL AUTO_INCREMENT,
    email        VARCHAR(100) NOT NULL,
    code         VARCHAR(10)  NOT NULL,
    purpose      VARCHAR(30)  NOT NULL DEFAULT 'REGISTRATION',
    expires_at   DATETIME     NOT NULL,
    consumed_at  DATETIME     NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    last_sent_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_email_otps PRIMARY KEY (otp_id),
    CONSTRAINT chk_email_otps_purpose CHECK (purpose IN ('REGISTRATION')),
    INDEX idx_email_otps_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE email_verification_tokens (
    token_id    BIGINT       NOT NULL AUTO_INCREMENT,
    token       VARCHAR(64)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    expires_at  DATETIME     NOT NULL,
    consumed_at DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (token_id),
    CONSTRAINT uq_email_verification_tokens_token UNIQUE (token),
    INDEX idx_email_verification_tokens_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE tutor_centers
    MODIFY COLUMN license_no VARCHAR(50) NULL;
