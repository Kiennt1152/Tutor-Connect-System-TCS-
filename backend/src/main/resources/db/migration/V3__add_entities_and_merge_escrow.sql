-- New entities from Entity Definitions 3.1 + merge Escrow into EscrowTransaction.

CREATE TABLE IF NOT EXISTS provinces (
    province_id   BIGINT       NOT NULL AUTO_INCREMENT,
    province_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_provinces PRIMARY KEY (province_id),
    CONSTRAINT uq_provinces_name UNIQUE (province_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grades (
    grade_id   BIGINT      NOT NULL AUTO_INCREMENT,
    grade_name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_grades PRIMARY KEY (grade_id),
    CONSTRAINT uq_grades_name UNIQUE (grade_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS locations (
    location_id      BIGINT         NOT NULL AUTO_INCREMENT,
    google_place_id  VARCHAR(255)   NULL,
    address_line     VARCHAR(255)   NOT NULL,
    ward_name        VARCHAR(100)   NULL,
    district_name    VARCHAR(100)   NULL,
    province_id      BIGINT         NOT NULL,
    latitude         DECIMAL(10,8)  NULL,
    longitude        DECIMAL(11,8)  NULL,
    CONSTRAINT pk_locations PRIMARY KEY (location_id),
    CONSTRAINT uq_locations_google_place_id UNIQUE (google_place_id),
    CONSTRAINT fk_locations_province FOREIGN KEY (province_id) REFERENCES provinces (province_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subjects (
    subject_id   BIGINT       NOT NULL AUTO_INCREMENT,
    subject_name VARCHAR(100) NOT NULL,
    description  TEXT         NULL,
    CONSTRAINT pk_subjects PRIMARY KEY (subject_id),
    CONSTRAINT uq_subjects_name UNIQUE (subject_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qualifications (
    qualification_id BIGINT       NOT NULL AUTO_INCREMENT,
    tutor_id         BIGINT       NOT NULL,
    title            VARCHAR(150) NOT NULL,
    issuer           VARCHAR(150) NOT NULL,
    issue_date       DATE         NULL,
    CONSTRAINT pk_qualifications PRIMARY KEY (qualification_id),
    CONSTRAINT fk_qualifications_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS child_profiles (
    child_profile_id BIGINT       NOT NULL AUTO_INCREMENT,
    full_name        VARCHAR(100) NOT NULL,
    date_of_birth    DATE         NULL,
    gender           VARCHAR(10)  NULL,
    grade_id         BIGINT       NULL,
    school_name      VARCHAR(200) NULL,
    notes            TEXT         NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_child_profiles PRIMARY KEY (child_profile_id),
    CONSTRAINT fk_child_profiles_grade FOREIGN KEY (grade_id) REFERENCES grades (grade_id),
    CONSTRAINT chk_child_profiles_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parent_child_links (
    link_id           BIGINT      NOT NULL AUTO_INCREMENT,
    parent_user_id    BIGINT      NOT NULL,
    child_profile_id  BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_parent_child_links PRIMARY KEY (link_id),
    CONSTRAINT uq_parent_child_links UNIQUE (parent_user_id, child_profile_id),
    CONSTRAINT fk_parent_child_links_parent FOREIGN KEY (parent_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_parent_child_links_child FOREIGN KEY (child_profile_id) REFERENCES child_profiles (child_profile_id),
    CONSTRAINT chk_parent_child_links_status CHECK (status IN ('ACTIVE','REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tutor_availabilities (
    availability_id          BIGINT   NOT NULL AUTO_INCREMENT,
    tutor_id                 BIGINT   NOT NULL,
    day_of_week              INT      NOT NULL,
    start_time               TIME     NOT NULL,
    end_time                 TIME     NOT NULL,
    is_recurring             BOOLEAN  NOT NULL DEFAULT TRUE,
    specific_date            DATE     NULL,
    google_calendar_event_id VARCHAR(255) NULL,
    created_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_tutor_availabilities PRIMARY KEY (availability_id),
    CONSTRAINT fk_tutor_availabilities_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_tutor_availabilities_day CHECK (day_of_week BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_assignments (
    assignment_id  BIGINT      NOT NULL AUTO_INCREMENT,
    application_id BIGINT      NOT NULL,
    assigned_date  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_class_assignments PRIMARY KEY (assignment_id),
    CONSTRAINT uq_class_assignments_application UNIQUE (application_id),
    CONSTRAINT fk_class_assignments_application FOREIGN KEY (application_id) REFERENCES tutor_applications (application_id),
    CONSTRAINT chk_class_assignments_status CHECK (status IN ('ACTIVE','TERMINATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS application_status_histories (
    application_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    application_id         BIGINT      NOT NULL,
    old_status             VARCHAR(20) NULL,
    new_status             VARCHAR(20) NOT NULL,
    changed_by_user_id     BIGINT      NOT NULL,
    changed_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_application_status_histories PRIMARY KEY (application_history_id),
    CONSTRAINT fk_application_status_histories_app FOREIGN KEY (application_id) REFERENCES tutor_applications (application_id),
    CONSTRAINT fk_application_status_histories_user FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_students (
    class_student_id  BIGINT       NOT NULL AUTO_INCREMENT,
    class_id          BIGINT       NOT NULL,
    child_profile_id  BIGINT       NULL,
    student_name      VARCHAR(100) NOT NULL,
    student_phone     VARCHAR(15)  NULL,
    student_email     VARCHAR(100) NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ENROLLED',
    enrolled_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes             TEXT         NULL,
    CONSTRAINT pk_class_students PRIMARY KEY (class_student_id),
    CONSTRAINT uq_class_students UNIQUE (class_id, student_name, student_phone),
    CONSTRAINT fk_class_students_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_class_students_child FOREIGN KEY (child_profile_id) REFERENCES child_profiles (child_profile_id),
    CONSTRAINT chk_class_students_status CHECK (status IN ('ENROLLED','DROPPED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS verification_histories (
    verification_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    verification_id         BIGINT      NOT NULL,
    old_status              VARCHAR(20) NULL,
    new_status              VARCHAR(20) NOT NULL,
    changed_by_user_id      BIGINT      NOT NULL,
    changed_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_verification_histories PRIMARY KEY (verification_history_id),
    CONSTRAINT fk_verification_histories_request FOREIGN KEY (verification_id) REFERENCES verification_requests (verification_id),
    CONSTRAINT fk_verification_histories_user FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_histories (
    payment_history_id BIGINT      NOT NULL AUTO_INCREMENT,
    payment_id         BIGINT      NOT NULL,
    old_status         VARCHAR(20) NULL,
    new_status         VARCHAR(20) NOT NULL,
    changed_by_user_id BIGINT      NULL,
    changed_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payment_histories PRIMARY KEY (payment_history_id),
    CONSTRAINT fk_payment_histories_payment FOREIGN KEY (payment_id) REFERENCES payment_transactions (transaction_id),
    CONSTRAINT fk_payment_histories_user FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reports (
    report_id     BIGINT       NOT NULL AUTO_INCREMENT,
    reporter_id   BIGINT       NOT NULL,
    target_type   VARCHAR(50)  NOT NULL,
    target_id     BIGINT       NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    description   TEXT         NOT NULL,
    evidence_urls TEXT         NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_reports PRIMARY KEY (report_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (user_id),
    CONSTRAINT chk_reports_target_type CHECK (target_type IN ('USER','CLASS','REVIEW')),
    CONSTRAINT chk_reports_category CHECK (category IN ('FRAUD','ABUSE','SPAM')),
    CONSTRAINT chk_reports_status CHECK (status IN ('PENDING','RESOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_keys (
    api_key_id        BIGINT       NOT NULL AUTO_INCREMENT,
    key_hash          VARCHAR(64)  NOT NULL,
    allowed_endpoints TEXT         NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at      DATETIME     NULL,
    CONSTRAINT pk_api_keys PRIMARY KEY (api_key_id),
    CONSTRAINT uq_api_keys_hash UNIQUE (key_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contract_templates (
    template_id BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    created_by  BIGINT       NOT NULL,
    center_id   BIGINT       NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_contract_templates PRIMARY KEY (template_id),
    CONSTRAINT fk_contract_templates_creator FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT fk_contract_templates_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id),
    CONSTRAINT chk_contract_templates_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_penalties (
    penalty_id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    issued_by           BIGINT      NOT NULL,
    penalty_type        VARCHAR(30) NOT NULL,
    reason              TEXT        NOT NULL,
    evidence_urls       TEXT        NULL,
    restriction_details JSON        NULL,
    starts_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          DATETIME    NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    revoked_at          DATETIME    NULL,
    revoked_reason      TEXT        NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_penalties PRIMARY KEY (penalty_id),
    CONSTRAINT fk_user_penalties_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_penalties_admin FOREIGN KEY (issued_by) REFERENCES platform_admins (admin_id),
    CONSTRAINT chk_user_penalties_type CHECK (penalty_type IN ('WARNING','FEATURE_RESTRICTION','TEMPORARY_BAN','PERMANENT_BAN')),
    CONSTRAINT chk_user_penalties_status CHECK (status IN ('ACTIVE','EXPIRED','REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Merge escrows + legacy escrow_transactions into spec EscrowTransaction.

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE refund_requests DROP FOREIGN KEY fk_refund_requests_escrow;
ALTER TABLE disputes DROP FOREIGN KEY fk_disputes_escrow;
ALTER TABLE payment_release_requests DROP FOREIGN KEY fk_payment_release_requests_escrow;

DROP TABLE IF EXISTS escrow_transactions;
DROP TABLE IF EXISTS escrows;

CREATE TABLE escrow_transactions (
    escrow_id      BIGINT         NOT NULL AUTO_INCREMENT,
    payment_id     BIGINT         NOT NULL,
    assignment_id  BIGINT         NOT NULL,
    amount         DECIMAL(15,2)  NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    deposited_at   DATETIME       NULL,
    released_at    DATETIME       NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_escrow_transactions PRIMARY KEY (escrow_id),
    CONSTRAINT uq_escrow_transactions_payment UNIQUE (payment_id),
    CONSTRAINT fk_escrow_transactions_payment FOREIGN KEY (payment_id) REFERENCES payment_transactions (transaction_id),
    CONSTRAINT fk_escrow_transactions_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id),
    CONSTRAINT chk_escrow_transactions_status CHECK (status IN ('PENDING','FUNDED','RELEASED','REFUNDED','ON_HOLD','DISPUTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE refund_requests
    ADD CONSTRAINT fk_refund_requests_escrow FOREIGN KEY (escrow_id) REFERENCES escrow_transactions (escrow_id);

ALTER TABLE disputes
    ADD CONSTRAINT fk_disputes_escrow FOREIGN KEY (escrow_id) REFERENCES escrow_transactions (escrow_id);

ALTER TABLE payment_release_requests
    ADD CONSTRAINT fk_payment_release_requests_escrow FOREIGN KEY (escrow_id) REFERENCES escrow_transactions (escrow_id);

SET FOREIGN_KEY_CHECKS = 1;
