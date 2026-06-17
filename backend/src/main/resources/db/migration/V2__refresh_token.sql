-- =====================================================================
-- Refresh token store (auth) - not part of the original data dictionary.
-- Needed for the identity domain's RefreshToken entity.
-- =====================================================================
CREATE TABLE refresh_token (
    refresh_token_id CHAR(36)      NOT NULL,
    user_id          CHAR(36)      NOT NULL,
    token            VARCHAR(255)  NOT NULL,
    issued_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       DATETIME      NOT NULL,
    revoked          BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_refresh_token PRIMARY KEY (refresh_token_id),
    CONSTRAINT uq_refresh_token_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES `user` (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
