-- ============================================================
-- TutorConnectSystem (TCS) — Initial schema
-- Source: Data Dictionary v1.0  |  Engine: MySQL 8 / InnoDB
-- UUID stored as CHAR(36). Status/enum fields enforced via CHECK.
-- Tables are created parent-first so foreign keys are valid.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------- Reference / catalog tables ----------
CREATE TABLE roles (
    role_id      CHAR(36)     NOT NULL DEFAULT (UUID()),
    role_name    VARCHAR(50)  NOT NULL,
    description  TEXT         NULL,
    PRIMARY KEY (role_id),
    UNIQUE KEY uq_roles_name (role_name),
    CONSTRAINT chk_roles_name CHECK (role_name IN ('ADMIN', 'CLIENT', 'TUTOR', 'TUTOR_CENTER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE provinces (
    province_id    CHAR(36)     NOT NULL DEFAULT (UUID()),
    province_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (province_id),
    UNIQUE KEY uq_provinces_name (province_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE subjects (
    subject_id    CHAR(36)     NOT NULL DEFAULT (UUID()),
    subject_name  VARCHAR(100) NOT NULL,
    description   TEXT         NULL,
    PRIMARY KEY (subject_id),
    UNIQUE KEY uq_subjects_name (subject_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE grades (
    grade_id    CHAR(36)    NOT NULL DEFAULT (UUID()),
    grade_name  VARCHAR(50) NOT NULL,
    PRIMARY KEY (grade_id),
    UNIQUE KEY uq_grades_name (grade_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE faqs (
    faq_id    CHAR(36)    NOT NULL DEFAULT (UUID()),
    question  TEXT        NOT NULL,
    answer    TEXT        NOT NULL,
    category  VARCHAR(50) NULL,
    PRIMARY KEY (faq_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE conversations (
    conversation_id  CHAR(36)  NOT NULL DEFAULT (UUID()),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE locations (
    location_id      CHAR(36)       NOT NULL DEFAULT (UUID()),
    google_place_id  VARCHAR(255)   NULL,
    address_line     VARCHAR(255)   NOT NULL,
    ward_name        VARCHAR(100)   NULL,
    district_name    VARCHAR(100)   NULL,
    province_id      CHAR(36)       NOT NULL,
    latitude         DECIMAL(10, 8) NULL,
    longitude        DECIMAL(11, 8) NULL,
    PRIMARY KEY (location_id),
    UNIQUE KEY uq_locations_place (google_place_id),
    CONSTRAINT fk_locations_province FOREIGN KEY (province_id) REFERENCES provinces (province_id),
    CONSTRAINT chk_locations_lat CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_locations_lng CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Core account ----------
CREATE TABLE users (
    user_id        CHAR(36)     NOT NULL DEFAULT (UUID()),
    email          VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role_id        CHAR(36)     NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     TIMESTAMP    NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT chk_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'BANNED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Profiles (1:1 with users) ----------
CREATE TABLE clients (
    client_id    CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id      CHAR(36)     NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    address      TEXT         NULL,
    location_id  CHAR(36)     NULL,
    avatar       VARCHAR(255) NULL,
    PRIMARY KEY (client_id),
    UNIQUE KEY uq_clients_user (user_id),
    CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_clients_location FOREIGN KEY (location_id) REFERENCES locations (location_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tutors (
    tutor_id          CHAR(36)       NOT NULL DEFAULT (UUID()),
    user_id           CHAR(36)       NOT NULL,
    full_name         VARCHAR(100)   NOT NULL,
    gender            VARCHAR(10)    NOT NULL,
    phone             VARCHAR(15)    NOT NULL,
    address           TEXT           NULL,
    location_id       CHAR(36)       NULL,
    experience_years  INT            NOT NULL DEFAULT 0,
    bio               TEXT           NULL,
    hourly_rate       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    rating_avg        DECIMAL(3, 2)  NOT NULL DEFAULT 0,
    PRIMARY KEY (tutor_id),
    UNIQUE KEY uq_tutors_user (user_id),
    CONSTRAINT fk_tutors_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_tutors_location FOREIGN KEY (location_id) REFERENCES locations (location_id),
    CONSTRAINT chk_tutors_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_tutors_exp CHECK (experience_years >= 0),
    CONSTRAINT chk_tutors_rate CHECK (hourly_rate >= 0),
    CONSTRAINT chk_tutors_rating CHECK (rating_avg BETWEEN 0 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tutor_centers (
    tutorcenter_id  CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id         CHAR(36)     NOT NULL,
    company_name    VARCHAR(150) NOT NULL,
    license_no      VARCHAR(50)  NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    address         TEXT         NOT NULL,
    location_id     CHAR(36)     NULL,
    description     TEXT         NULL,
    PRIMARY KEY (tutorcenter_id),
    UNIQUE KEY uq_centers_user (user_id),
    UNIQUE KEY uq_centers_license (license_no),
    CONSTRAINT fk_centers_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_centers_location FOREIGN KEY (location_id) REFERENCES locations (location_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE platform_admins (
    admin_id   CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id    CHAR(36)     NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (admin_id),
    UNIQUE KEY uq_admins_user (user_id),
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Wallet & payment method ----------
CREATE TABLE wallets (
    wallet_id  CHAR(36)       NOT NULL DEFAULT (UUID()),
    user_id    CHAR(36)       NOT NULL,
    balance    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (wallet_id),
    UNIQUE KEY uq_wallets_user (user_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payment_methods (
    payment_method_id  CHAR(36)     NOT NULL DEFAULT (UUID()),
    wallet_id          CHAR(36)     NOT NULL,
    type               VARCHAR(50)  NOT NULL,
    account_no         VARCHAR(50)  NOT NULL,
    bank_name          VARCHAR(100) NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (payment_method_id),
    CONSTRAINT fk_pm_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id),
    CONSTRAINT chk_pm_type CHECK (type IN ('BANK_ACCOUNT', 'VIETQR', 'PAYOS')),
    CONSTRAINT chk_pm_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Tutor catalog data ----------
CREATE TABLE tutor_subjects (
    tutor_subject_id   CHAR(36)    NOT NULL DEFAULT (UUID()),
    tutor_id           CHAR(36)    NOT NULL,
    subject_id         CHAR(36)    NOT NULL,
    proficiency_level  VARCHAR(50) NULL,
    PRIMARY KEY (tutor_subject_id),
    CONSTRAINT fk_ts_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT fk_ts_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id),
    UNIQUE KEY uq_tutor_subject (tutor_id, subject_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE qualifications (
    qualification_id  CHAR(36)     NOT NULL DEFAULT (UUID()),
    tutor_id          CHAR(36)     NOT NULL,
    title             VARCHAR(150) NOT NULL,
    issuer            VARCHAR(150) NOT NULL,
    issue_date        DATE         NULL,
    PRIMARY KEY (qualification_id),
    CONSTRAINT fk_qual_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE favorite_tutors (
    favorite_id  CHAR(36)  NOT NULL DEFAULT (UUID()),
    user_id      CHAR(36)  NOT NULL,
    tutor_id     CHAR(36)  NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (favorite_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_fav_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    UNIQUE KEY uq_fav_user_tutor (user_id, tutor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recommendation_logs (
    recommendation_id  CHAR(36)      NOT NULL DEFAULT (UUID()),
    user_id            CHAR(36)      NOT NULL,
    tutor_id           CHAR(36)      NOT NULL,
    score              DECIMAL(5, 2) NOT NULL DEFAULT 0,
    generated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_id),
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_rec_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_rec_score CHECK (score BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Verification ----------
CREATE TABLE verification_requests (
    verification_id    CHAR(36)    NOT NULL DEFAULT (UUID()),
    user_id            CHAR(36)    NOT NULL,
    verification_type  VARCHAR(50) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at       TIMESTAMP   NULL,
    reviewed_at        TIMESTAMP   NULL,
    PRIMARY KEY (verification_id),
    CONSTRAINT fk_vr_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_vr_type CHECK (verification_type IN ('TUTOR_PROFILE', 'TUTORCENTER_LICENSE')),
    CONSTRAINT chk_vr_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_vr_reviewed CHECK (reviewed_at IS NULL OR submitted_at IS NULL OR reviewed_at >= submitted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE verification_documents (
    document_id      CHAR(36)     NOT NULL DEFAULT (UUID()),
    verification_id  CHAR(36)     NOT NULL,
    file_url         VARCHAR(255) NOT NULL,
    document_type    VARCHAR(50)  NOT NULL,
    PRIMARY KEY (document_id),
    CONSTRAINT fk_vd_request FOREIGN KEY (verification_id) REFERENCES verification_requests (verification_id),
    CONSTRAINT chk_vd_type CHECK (document_type IN ('ID_CARD', 'DEGREE', 'CERTIFICATE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE verification_histories (
    verification_history_id  CHAR(36)    NOT NULL DEFAULT (UUID()),
    verification_id          CHAR(36)    NOT NULL,
    old_status               VARCHAR(20) NULL,
    new_status               VARCHAR(20) NOT NULL,
    changed_by_user_id       CHAR(36)    NOT NULL,
    changed_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (verification_history_id),
    CONSTRAINT fk_vh_request FOREIGN KEY (verification_id) REFERENCES verification_requests (verification_id),
    CONSTRAINT fk_vh_user FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Class & lessons ----------
CREATE TABLE classes (
    class_id        CHAR(36)       NOT NULL DEFAULT (UUID()),
    creator_id      CHAR(36)       NOT NULL,
    subject_id      CHAR(36)       NOT NULL,
    title           VARCHAR(150)   NOT NULL,
    description     TEXT           NOT NULL,
    location_id     CHAR(36)       NULL,
    grade_id        CHAR(36)       NOT NULL,
    max_sessions    INT            NOT NULL DEFAULT 1,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    budget          DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    recurring_type  VARCHAR(20)    NOT NULL DEFAULT 'ONCE',
    PRIMARY KEY (class_id),
    CONSTRAINT fk_class_creator FOREIGN KEY (creator_id) REFERENCES users (user_id),
    CONSTRAINT fk_class_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id),
    CONSTRAINT fk_class_location FOREIGN KEY (location_id) REFERENCES locations (location_id),
    CONSTRAINT fk_class_grade FOREIGN KEY (grade_id) REFERENCES grades (grade_id),
    CONSTRAINT chk_class_sessions CHECK (max_sessions > 0),
    CONSTRAINT chk_class_budget CHECK (budget > 0),
    CONSTRAINT chk_class_dates CHECK (end_date > start_date),
    CONSTRAINT chk_class_status CHECK (status IN ('DRAFT', 'OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_class_recurring CHECK (recurring_type IN ('ONCE', 'WEEKLY'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE lessons (
    lesson_id            CHAR(36)    NOT NULL DEFAULT (UUID()),
    class_id             CHAR(36)    NOT NULL,
    lesson_date          DATE        NOT NULL,
    start_time           TIME        NOT NULL,
    end_time             TIME        NOT NULL,
    duration             INT         NOT NULL DEFAULT 0,
    status               VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    approved_by_user_id  CHAR(36)    NULL,
    PRIMARY KEY (lesson_id),
    CONSTRAINT fk_lesson_class FOREIGN KEY (class_id) REFERENCES classes (class_id),
    CONSTRAINT fk_lesson_approver FOREIGN KEY (approved_by_user_id) REFERENCES users (user_id),
    CONSTRAINT chk_lesson_duration CHECK (duration > 0),
    CONSTRAINT chk_lesson_time CHECK (end_time > start_time),
    CONSTRAINT chk_lesson_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Applications & assignment ----------
CREATE TABLE tutor_applications (
    application_id  CHAR(36)    NOT NULL DEFAULT (UUID()),
    class_id        CHAR(36)    NOT NULL,
    tutor_id        CHAR(36)    NOT NULL,
    cover_letter    TEXT        NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    applied_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_id),
    CONSTRAINT fk_app_class FOREIGN KEY (class_id) REFERENCES classes (class_id),
    CONSTRAINT fk_app_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_app_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    UNIQUE KEY uq_app_class_tutor (class_id, tutor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE application_status_histories (
    application_history_id  CHAR(36)    NOT NULL DEFAULT (UUID()),
    application_id          CHAR(36)    NOT NULL,
    old_status              VARCHAR(20) NULL,
    new_status              VARCHAR(20) NOT NULL,
    changed_by_user_id      CHAR(36)    NOT NULL,
    changed_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_history_id),
    CONSTRAINT fk_ash_app FOREIGN KEY (application_id) REFERENCES tutor_applications (application_id),
    CONSTRAINT fk_ash_user FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE class_assignments (
    assignment_id   CHAR(36)    NOT NULL DEFAULT (UUID()),
    application_id  CHAR(36)    NOT NULL,
    assigned_date   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (assignment_id),
    UNIQUE KEY uq_assignment_app (application_id),
    CONSTRAINT fk_assign_app FOREIGN KEY (application_id) REFERENCES tutor_applications (application_id),
    CONSTRAINT chk_assign_status CHECK (status IN ('ACTIVE', 'TERMINATED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Payment / escrow / refund ----------
CREATE TABLE payments (
    payment_id         CHAR(36)       NOT NULL DEFAULT (UUID()),
    payer_id           CHAR(36)       NOT NULL,
    amount             DECIMAL(12, 2) NOT NULL DEFAULT 0,
    payment_method_id  CHAR(36)       NOT NULL,
    payment_status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_pay_payer FOREIGN KEY (payer_id) REFERENCES users (user_id),
    CONSTRAINT fk_pay_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (payment_method_id),
    CONSTRAINT chk_pay_amount CHECK (amount > 0),
    CONSTRAINT chk_pay_status CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payment_histories (
    payment_history_id  CHAR(36)    NOT NULL DEFAULT (UUID()),
    payment_id          CHAR(36)    NOT NULL,
    old_status          VARCHAR(20) NULL,
    new_status          VARCHAR(20) NOT NULL,
    changed_by_user_id  CHAR(36)    NULL,
    changed_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_history_id),
    CONSTRAINT fk_ph_payment FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
    CONSTRAINT fk_ph_user FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE escrow_transactions (
    escrow_id      CHAR(36)       NOT NULL DEFAULT (UUID()),
    payment_id     CHAR(36)       NOT NULL,
    assignment_id  CHAR(36)       NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (escrow_id),
    UNIQUE KEY uq_escrow_payment (payment_id),
    CONSTRAINT fk_escrow_payment FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
    CONSTRAINT fk_escrow_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id),
    CONSTRAINT chk_escrow_amount CHECK (amount > 0),
    CONSTRAINT chk_escrow_status CHECK (status IN ('PENDING', 'FUNDED', 'RELEASED', 'REFUNDED', 'ON_HOLD'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refunds (
    refund_id  CHAR(36)       NOT NULL DEFAULT (UUID()),
    escrow_id  CHAR(36)       NOT NULL,
    amount     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    reason     TEXT           NOT NULL,
    status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (refund_id),
    CONSTRAINT fk_refund_escrow FOREIGN KEY (escrow_id) REFERENCES escrow_transactions (escrow_id),
    CONSTRAINT chk_refund_amount CHECK (amount > 0),
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payment_release_requests (
    request_id     CHAR(36)       NOT NULL DEFAULT (UUID()),
    assignment_id  CHAR(36)       NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    submitted_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (request_id),
    CONSTRAINT fk_prr_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id),
    CONSTRAINT chk_prr_amount CHECK (amount > 0),
    CONSTRAINT chk_prr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PAID'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Reviews ----------
CREATE TABLE reviews (
    review_id      CHAR(36)  NOT NULL DEFAULT (UUID()),
    reviewer_id    CHAR(36)  NOT NULL,
    reviewee_id    CHAR(36)  NOT NULL,
    assignment_id  CHAR(36)  NULL,
    rating         INT       NOT NULL,
    comment        TEXT      NULL,
    PRIMARY KEY (review_id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (user_id),
    CONSTRAINT fk_review_reviewee FOREIGN KEY (reviewee_id) REFERENCES users (user_id),
    CONSTRAINT fk_review_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_self CHECK (reviewer_id <> reviewee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Communication ----------
CREATE TABLE conversation_participants (
    conversation_participant_id  CHAR(36) NOT NULL DEFAULT (UUID()),
    conversation_id              CHAR(36) NOT NULL,
    user_id                      CHAR(36) NOT NULL,
    PRIMARY KEY (conversation_participant_id),
    CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id),
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    UNIQUE KEY uq_cp_conv_user (conversation_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE messages (
    message_id       CHAR(36)  NOT NULL DEFAULT (UUID()),
    conversation_id  CHAR(36)  NOT NULL,
    sender_id        CHAR(36)  NOT NULL,
    content          TEXT      NOT NULL,
    sent_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notifications (
    notification_id  CHAR(36)    NOT NULL DEFAULT (UUID()),
    user_id          CHAR(36)    NOT NULL,
    type             VARCHAR(50) NOT NULL,
    content          TEXT        NOT NULL,
    is_read          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_notif_type CHECK (type IN ('PAYMENT', 'APPLICATION', 'SYSTEM'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------- Moderation, support, dispute, audit ----------
CREATE TABLE reports (
    report_id    CHAR(36)    NOT NULL DEFAULT (UUID()),
    reporter_id  CHAR(36)    NOT NULL,
    target_type  VARCHAR(50) NOT NULL,
    target_id    CHAR(36)    NOT NULL,
    category     VARCHAR(50) NOT NULL,
    description  TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (report_id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users (user_id),
    CONSTRAINT chk_report_target CHECK (target_type IN ('USER', 'CLASS', 'REVIEW')),
    CONSTRAINT chk_report_category CHECK (category IN ('FRAUD', 'ABUSE', 'SPAM')),
    CONSTRAINT chk_report_status CHECK (status IN ('PENDING', 'RESOLVED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE support_tickets (
    ticket_id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id            CHAR(36)     NOT NULL,
    category           VARCHAR(50)  NOT NULL,
    subject            VARCHAR(150) NOT NULL,
    description        TEXT         NOT NULL,
    priority           VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status             VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_admin_id  CHAR(36)     NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ticket_id),
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ticket_admin FOREIGN KEY (assigned_admin_id) REFERENCES platform_admins (admin_id),
    CONSTRAINT chk_ticket_category CHECK (category IN ('BUG_REPORT', 'INQUIRY')),
    CONSTRAINT chk_ticket_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_ticket_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE disputes (
    dispute_id  CHAR(36)    NOT NULL DEFAULT (UUID()),
    report_id   CHAR(36)    NOT NULL,
    escrow_id   CHAR(36)    NOT NULL,
    resolution  TEXT        NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    PRIMARY KEY (dispute_id),
    UNIQUE KEY uq_dispute_report (report_id),
    CONSTRAINT fk_dispute_report FOREIGN KEY (report_id) REFERENCES reports (report_id),
    CONSTRAINT fk_dispute_escrow FOREIGN KEY (escrow_id) REFERENCES escrow_transactions (escrow_id),
    CONSTRAINT chk_dispute_status CHECK (status IN ('OPEN', 'UNDER_INVESTIGATION', 'WAITING_EVIDENCE', 'RESOLVED', 'REJECTED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    log_id       CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id      CHAR(36)     NULL,
    action       VARCHAR(100) NOT NULL,
    entity_name  VARCHAR(100) NOT NULL,
    entity_id    CHAR(36)     NOT NULL,
    timestamp    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
