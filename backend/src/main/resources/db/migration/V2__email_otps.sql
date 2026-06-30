-- =====================================================================
-- TutorConnectSystem (TCS) - Bang ma OTP xac thuc email khi dang ky
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE email_otps (
    otp_id      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    purpose     VARCHAR(30)  NOT NULL DEFAULT 'REGISTRATION',
    expires_at  DATETIME     NOT NULL,
    consumed_at DATETIME     NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    last_sent_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_email_otps PRIMARY KEY (otp_id),
    CONSTRAINT fk_email_otps_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_email_otps_purpose CHECK (purpose IN ('REGISTRATION')),
    INDEX idx_email_otps_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
