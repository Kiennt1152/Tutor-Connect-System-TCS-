-- =====================================================================
-- 4P.7: Announcement (thong bao he thong) cho Platform Admin.
-- Admin tao thong bao gui toi tat ca user hoac theo tung role,
-- hien thi tren banner/khu vuc thong bao cua frontend.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL,
    audience        VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    published       TINYINT(1)   NOT NULL DEFAULT 1,
    starts_at       DATETIME     NULL,
    ends_at         DATETIME     NULL,
    created_by      BIGINT       NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_announcements PRIMARY KEY (announcement_id),
    CONSTRAINT chk_announcements_audience
        CHECK (audience IN ('ALL','TUTOR','TUTOR_CENTER','CLIENT')),
    CONSTRAINT fk_announcements_created_by FOREIGN KEY (created_by) REFERENCES users (user_id),
    INDEX idx_announcements_published (published),
    INDEX idx_announcements_audience (audience)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
