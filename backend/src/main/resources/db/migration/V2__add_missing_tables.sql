-- ============================================================
-- V2 — Bổ sung các bảng/cột còn thiếu so với Screen Spec
--   1. otp_tokens        : phục vụ P-03 Reset Password (OTP)
--   2. system_settings   : phục vụ P-13 System Settings Panel
--   3. tutors (ALTER)    : đếm vi phạm / điểm uy tín (BR-15, BR-17)
-- ============================================================

-- 1) OTP cho luồng đặt lại mật khẩu / xác thực email
CREATE TABLE otp_tokens (
    otp_id      CHAR(36)    NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)    NULL,
    email       VARCHAR(100) NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    purpose     VARCHAR(30)  NOT NULL DEFAULT 'PASSWORD_RESET',
    expires_at  TIMESTAMP    NOT NULL,
    is_used     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (otp_id),
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('PASSWORD_RESET', 'EMAIL_VERIFICATION')),
    CONSTRAINT chk_otp_expiry CHECK (expires_at > created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_otp_email_purpose ON otp_tokens (email, purpose);

-- 2) Cấu hình hệ thống dạng key-value (commission rate, session timeout, ...)
CREATE TABLE system_settings (
    setting_id     CHAR(36)     NOT NULL DEFAULT (UUID()),
    setting_key    VARCHAR(100) NOT NULL,
    setting_value  VARCHAR(255) NOT NULL,
    description    VARCHAR(255) NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_id),
    UNIQUE KEY uq_settings_key (setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 3) Bổ sung cột uy tín / vi phạm cho Tutor
ALTER TABLE tutors
    ADD COLUMN infraction_count INT NOT NULL DEFAULT 0,
    ADD COLUMN reputation_score DECIMAL(3, 2) NOT NULL DEFAULT 5.00;

ALTER TABLE tutors
    ADD CONSTRAINT chk_tutors_infraction CHECK (infraction_count >= 0),
    ADD CONSTRAINT chk_tutors_reputation CHECK (reputation_score BETWEEN 0 AND 5);
