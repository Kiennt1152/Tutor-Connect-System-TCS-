-- =====================================================================
-- V4: Bảng refresh_tokens phục vụ cơ chế JWT refresh token
-- =====================================================================
CREATE TABLE refresh_tokens (
    refresh_token_id CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id          CHAR(36)     NOT NULL,
    token            VARCHAR(255) NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    revoked          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (refresh_token_id),
    UNIQUE KEY uq_refresh_tokens_token (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
