-- =====================================================================
-- TCS - Tai cau truc luong dang ky (UC FT-01):
--   OTP gan theo EMAIL (chua co tai khoan) + token email-da-xac-thuc.
-- Tai khoan chi duoc tao sau khi email da xac thuc va Submit thanh cong.
-- =====================================================================

SET NAMES utf8mb4;

-- Bang OTP cu (V2) gan user_id khong con phu hop -> tao lai theo email.
DROP TABLE IF EXISTS email_otps;

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

-- Token chung nhan email da xac thuc (ngan han, dung mot lan), cap sau khi Verify OTP.
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
