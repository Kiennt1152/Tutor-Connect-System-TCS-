#!/usr/bin/env python3
"""Generate V1__init_schema.sql from entity definitions."""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/db/migration/V1__init_schema.sql"

HEADER = """-- =====================================================================
-- TutorConnectSystem (TCS) - Initial schema (v2 domain model)
-- Engine: MySQL 8.0+  |  Charset: utf8mb4  |  Migration tool: Flyway
-- PK: BIGINT AUTO_INCREMENT (except wallets.wallet_id = users.user_id)
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
"""

FOOTER = """
SET FOREIGN_KEY_CHECKS = 1;
"""

TABLES = [
    # 1 users
    """
CREATE TABLE users (
    user_id       BIGINT        NOT NULL AUTO_INCREMENT,
    email         VARCHAR(100)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login    DATETIME      NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('PENDING','ACTIVE','BANNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    # 2-6 independent
    """
CREATE TABLE faq_entries (
    faq_id   BIGINT  NOT NULL AUTO_INCREMENT,
    question TEXT    NOT NULL,
    answer   TEXT    NOT NULL,
    category VARCHAR(50) NULL,
    CONSTRAINT pk_faq_entries PRIMARY KEY (faq_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE system_parameters (
    parameter_id BIGINT       NOT NULL AUTO_INCREMENT,
    param_key    VARCHAR(100) NOT NULL,
    param_value  TEXT         NOT NULL,
    description  TEXT         NULL,
    CONSTRAINT pk_system_parameters PRIMARY KEY (parameter_id),
    CONSTRAINT uq_system_parameters_key UNIQUE (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE lead_routing_rules (
    rule_id  BIGINT       NOT NULL AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    criteria TEXT         NULL,
    priority INT          NOT NULL DEFAULT 0,
    active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_lead_routing_rules PRIMARY KEY (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE conversations (
    conversation_id BIGINT       NOT NULL AUTO_INCREMENT,
    context_type    VARCHAR(50)  NULL,
    context_id      BIGINT       NULL,
    type            VARCHAR(30)  NOT NULL,
    last_message_at DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_conversations PRIMARY KEY (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE notification_templates (
    template_id      BIGINT       NOT NULL AUTO_INCREMENT,
    code             VARCHAR(50)  NOT NULL,
    title_template   VARCHAR(200) NOT NULL,
    content_template TEXT         NOT NULL,
    channel          VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_notification_templates PRIMARY KEY (template_id),
    CONSTRAINT uq_notification_templates_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    # 7 categories
    """
CREATE TABLE categories (
    category_id BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    parent_id   BIGINT       NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_categories PRIMARY KEY (category_id),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    # 8-11 profile
    """
CREATE TABLE clients (
    client_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id   BIGINT       NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone     VARCHAR(15)  NOT NULL,
    address   TEXT         NULL,
    avatar    VARCHAR(255) NULL,
    CONSTRAINT pk_clients PRIMARY KEY (client_id),
    CONSTRAINT uq_clients_user UNIQUE (user_id),
    CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutors (
    tutor_id         BIGINT         NOT NULL AUTO_INCREMENT,
    user_id          BIGINT         NOT NULL,
    full_name        VARCHAR(100)   NOT NULL,
    gender           VARCHAR(10)    NOT NULL,
    phone            VARCHAR(15)    NOT NULL,
    address          TEXT           NULL,
    experience_years INT            NOT NULL DEFAULT 0,
    bio              TEXT           NULL,
    hourly_rate      DECIMAL(12,2)  NOT NULL DEFAULT 0,
    rating_avg       DECIMAL(3,2)   NOT NULL DEFAULT 0,
    CONSTRAINT pk_tutors PRIMARY KEY (tutor_id),
    CONSTRAINT uq_tutors_user UNIQUE (user_id),
    CONSTRAINT fk_tutors_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutor_centers (
    center_id    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    license_no   VARCHAR(50)  NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    address      TEXT         NOT NULL,
    description  TEXT         NULL,
    CONSTRAINT pk_tutor_centers PRIMARY KEY (center_id),
    CONSTRAINT uq_tutor_centers_user UNIQUE (user_id),
    CONSTRAINT uq_tutor_centers_license UNIQUE (license_no),
    CONSTRAINT fk_tutor_centers_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE platform_admins (
    admin_id  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id   BIGINT       NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_platform_admins PRIMARY KEY (admin_id),
    CONSTRAINT uq_platform_admins_user UNIQUE (user_id),
    CONSTRAINT fk_platform_admins_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    # 12 wallets shared PK
    """
CREATE TABLE wallets (
    wallet_id          BIGINT         NOT NULL,
    available_balance  DECIMAL(15,2)  NOT NULL DEFAULT 0,
    frozen_balance     DECIMAL(15,2)  NOT NULL DEFAULT 0,
    status             VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wallets PRIMARY KEY (wallet_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (wallet_id) REFERENCES users (user_id),
    CONSTRAINT chk_wallets_status CHECK (status IN ('ACTIVE','FROZEN','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE password_reset_tokens (
    token_id   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at DATETIME     NOT NULL,
    used_at    DATETIME     NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (token_id),
    CONSTRAINT uq_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE verification_requests (
    verification_id   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    verification_type VARCHAR(50)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_at      DATETIME     NULL,
    reviewed_at       DATETIME     NULL,
    CONSTRAINT pk_verification_requests PRIMARY KEY (verification_id),
    CONSTRAINT fk_verification_requests_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_verification_type CHECK (verification_type IN ('TUTOR_PROFILE','TUTOR_CENTER_LICENSE')),
    CONSTRAINT chk_verification_status CHECK (status IN ('DRAFT','PENDING','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE media_files (
    file_id     BIGINT       NOT NULL AUTO_INCREMENT,
    uploaded_by BIGINT       NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    mime_type   VARCHAR(100) NULL,
    file_size   BIGINT       NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_media_files PRIMARY KEY (file_id),
    CONSTRAINT fk_media_files_user FOREIGN KEY (uploaded_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE matching_preferences (
    preference_id      BIGINT         NOT NULL AUTO_INCREMENT,
    user_id            BIGINT         NOT NULL,
    preferred_gender   VARCHAR(10)    NULL,
    min_rating         DECIMAL(3,2)   NULL,
    max_hourly_rate    DECIMAL(12,2)  NULL,
    preferred_location VARCHAR(255)   NULL,
    CONSTRAINT pk_matching_preferences PRIMARY KEY (preference_id),
    CONSTRAINT uq_matching_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_matching_preferences_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE notification_preferences (
    preference_id  BIGINT   NOT NULL AUTO_INCREMENT,
    user_id        BIGINT   NOT NULL,
    email_enabled  BOOLEAN  NOT NULL DEFAULT TRUE,
    sms_enabled    BOOLEAN  NOT NULL DEFAULT FALSE,
    push_enabled   BOOLEAN  NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (preference_id),
    CONSTRAINT uq_notification_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutor_educations (
    education_id   BIGINT       NOT NULL AUTO_INCREMENT,
    tutor_id       BIGINT       NOT NULL,
    institution    VARCHAR(200) NOT NULL,
    degree         VARCHAR(100) NOT NULL,
    field_of_study VARCHAR(100) NULL,
    start_year     INT          NULL,
    end_year       INT          NULL,
    CONSTRAINT pk_tutor_educations PRIMARY KEY (education_id),
    CONSTRAINT fk_tutor_educations_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutor_experiences (
    experience_id BIGINT       NOT NULL AUTO_INCREMENT,
    tutor_id      BIGINT       NOT NULL,
    title         VARCHAR(150) NOT NULL,
    organization  VARCHAR(200) NOT NULL,
    start_date    DATE         NULL,
    end_date      DATE         NULL,
    description   TEXT         NULL,
    CONSTRAINT pk_tutor_experiences PRIMARY KEY (experience_id),
    CONSTRAINT fk_tutor_experiences_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE reputation_histories (
    history_id   BIGINT        NOT NULL AUTO_INCREMENT,
    tutor_id     BIGINT        NOT NULL,
    old_score    DECIMAL(5,2)  NOT NULL,
    new_score    DECIMAL(5,2)  NOT NULL,
    trigger_type VARCHAR(50)   NOT NULL,
    reason       TEXT          NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reputation_histories PRIMARY KEY (history_id),
    CONSTRAINT fk_reputation_histories_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutor_subjects (
    tutor_subject_id  BIGINT      NOT NULL AUTO_INCREMENT,
    tutor_id          BIGINT      NOT NULL,
    category_id       BIGINT      NOT NULL,
    proficiency_level VARCHAR(50) NULL,
    CONSTRAINT pk_tutor_subjects PRIMARY KEY (tutor_subject_id),
    CONSTRAINT uq_tutor_subjects UNIQUE (tutor_id, category_id),
    CONSTRAINT fk_tutor_subjects_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT fk_tutor_subjects_category FOREIGN KEY (category_id) REFERENCES categories (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE favorite_tutors (
    favorite_id BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    tutor_id    BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_favorite_tutors PRIMARY KEY (favorite_id),
    CONSTRAINT uq_favorite_tutors UNIQUE (user_id, tutor_id),
    CONSTRAINT fk_favorite_tutors_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_favorite_tutors_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutor_certificates (
    certificate_id BIGINT       NOT NULL AUTO_INCREMENT,
    tutor_id       BIGINT       NOT NULL,
    name           VARCHAR(200) NOT NULL,
    issuer         VARCHAR(200) NOT NULL,
    issue_date     DATE         NULL,
    file_id        BIGINT       NULL,
    CONSTRAINT pk_tutor_certificates PRIMARY KEY (certificate_id),
    CONSTRAINT fk_tutor_certificates_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT fk_tutor_certificates_file FOREIGN KEY (file_id) REFERENCES media_files (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutoring_classes (
    class_id     BIGINT         NOT NULL AUTO_INCREMENT,
    creator_id   BIGINT         NOT NULL,
    category_id  BIGINT         NOT NULL,
    title        VARCHAR(150)   NOT NULL,
    description  TEXT           NOT NULL,
    max_sessions INT            NOT NULL DEFAULT 1,
    start_date   DATE           NOT NULL,
    end_date     DATE           NOT NULL,
    budget       DECIMAL(12,2)  NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tutoring_classes PRIMARY KEY (class_id),
    CONSTRAINT fk_tutoring_classes_creator FOREIGN KEY (creator_id) REFERENCES users (user_id),
    CONSTRAINT fk_tutoring_classes_category FOREIGN KEY (category_id) REFERENCES categories (category_id),
    CONSTRAINT chk_tutoring_classes_status CHECK (status IN ('DRAFT','OPEN','IN_PROGRESS','COMPLETED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE leads (
    lead_id       BIGINT       NOT NULL AUTO_INCREMENT,
    center_id     BIGINT       NOT NULL,
    contact_name  VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(15)  NOT NULL,
    contact_email VARCHAR(100) NULL,
    source        VARCHAR(50)  NULL,
    notes         TEXT         NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_leads PRIMARY KEY (lead_id),
    CONSTRAINT fk_leads_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id),
    CONSTRAINT chk_leads_status CHECK (status IN ('NEW','CONTACTED','QUALIFIED','CONVERTED','LOST'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE recruitment_posts (
    recruitment_id BIGINT       NOT NULL AUTO_INCREMENT,
    center_id      BIGINT       NOT NULL,
    title          VARCHAR(200) NOT NULL,
    description    TEXT         NOT NULL,
    requirements   TEXT         NULL,
    benefits       TEXT         NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    expired_at     DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recruitment_posts PRIMARY KEY (recruitment_id),
    CONSTRAINT fk_recruitment_posts_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id),
    CONSTRAINT chk_recruitment_posts_status CHECK (status IN ('DRAFT','OPEN','CLOSED','EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE center_tutor_memberships (
    membership_id BIGINT      NOT NULL AUTO_INCREMENT,
    center_id     BIGINT      NOT NULL,
    tutor_id      BIGINT      NOT NULL,
    joined_at     DATETIME    NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_center_tutor_memberships PRIMARY KEY (membership_id),
    CONSTRAINT uq_center_tutor_memberships UNIQUE (center_id, tutor_id),
    CONSTRAINT fk_center_tutor_memberships_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id),
    CONSTRAINT fk_center_tutor_memberships_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_center_tutor_memberships_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE lead_routing_rule_centers (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    rule_id   BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    CONSTRAINT pk_lead_routing_rule_centers PRIMARY KEY (id),
    CONSTRAINT uq_lead_routing_rule_centers UNIQUE (rule_id, center_id),
    CONSTRAINT fk_lead_routing_rule_centers_rule FOREIGN KEY (rule_id) REFERENCES lead_routing_rules (rule_id),
    CONSTRAINT fk_lead_routing_rule_centers_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE audit_logs (
    audit_id    BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id    BIGINT       NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   BIGINT       NOT NULL,
    old_value   JSON         NULL,
    new_value   JSON         NULL,
    ip_address  VARCHAR(45)  NULL,
    user_agent  VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_logs PRIMARY KEY (audit_id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE support_tickets (
    ticket_id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    assigned_admin_id BIGINT       NULL,
    type              VARCHAR(50)  NOT NULL,
    subject           VARCHAR(150) NOT NULL,
    description       TEXT         NOT NULL,
    priority          VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    resolved_at       DATETIME     NULL,
    closed_at         DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_support_tickets PRIMARY KEY (ticket_id),
    CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_support_tickets_admin FOREIGN KEY (assigned_admin_id) REFERENCES platform_admins (admin_id),
    CONSTRAINT chk_support_tickets_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT chk_support_tickets_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE schedule_slots (
    slot_id     BIGINT NOT NULL AUTO_INCREMENT,
    class_id    BIGINT NOT NULL,
    day_of_week INT    NOT NULL,
    start_time  TIME   NOT NULL,
    end_time    TIME   NOT NULL,
    CONSTRAINT pk_schedule_slots PRIMARY KEY (slot_id),
    CONSTRAINT fk_schedule_slots_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT chk_schedule_slots_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE tutor_applications (
    application_id BIGINT      NOT NULL AUTO_INCREMENT,
    class_id       BIGINT      NOT NULL,
    tutor_id       BIGINT      NOT NULL,
    cover_letter   TEXT        NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    applied_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tutor_applications PRIMARY KEY (application_id),
    CONSTRAINT uq_tutor_applications UNIQUE (class_id, tutor_id),
    CONSTRAINT fk_tutor_applications_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_tutor_applications_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_tutor_applications_status CHECK (status IN ('SUBMITTED','UNDER_REVIEW','ACCEPTED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE contracts (
    contract_id  BIGINT         NOT NULL AUTO_INCREMENT,
    class_id     BIGINT         NOT NULL,
    client_id    BIGINT         NOT NULL,
    tutor_id     BIGINT         NOT NULL,
    total_amount DECIMAL(15,2)  NOT NULL,
    terms        TEXT           NULL,
    status       VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    signed_at    DATETIME       NULL,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_contracts PRIMARY KEY (contract_id),
    CONSTRAINT fk_contracts_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_contracts_client FOREIGN KEY (client_id) REFERENCES clients (client_id),
    CONSTRAINT fk_contracts_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_contracts_status CHECK (status IN ('DRAFT','PENDING_SIGNATURE','ACTIVE','COMPLETED','TERMINATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE payment_methods (
    payment_method_id BIGINT       NOT NULL AUTO_INCREMENT,
    wallet_id         BIGINT       NOT NULL,
    type              VARCHAR(50)  NOT NULL,
    account_no        VARCHAR(50)  NOT NULL,
    bank_name         VARCHAR(100) NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_payment_methods PRIMARY KEY (payment_method_id),
    CONSTRAINT fk_payment_methods_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE financial_journals (
    journal_id     BIGINT         NOT NULL AUTO_INCREMENT,
    wallet_id      BIGINT         NOT NULL,
    reference_type VARCHAR(50)    NOT NULL,
    reference_id   BIGINT         NOT NULL,
    entry_type     VARCHAR(10)    NOT NULL,
    amount         DECIMAL(15,2)  NOT NULL,
    balance_before DECIMAL(15,2)  NOT NULL,
    balance_after  DECIMAL(15,2)  NOT NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_financial_journals PRIMARY KEY (journal_id),
    CONSTRAINT fk_financial_journals_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id),
    CONSTRAINT chk_financial_journals_entry_type CHECK (entry_type IN ('CREDIT','DEBIT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE verification_documents (
    document_id   BIGINT      NOT NULL AUTO_INCREMENT,
    verification_id BIGINT    NOT NULL,
    file_id       BIGINT      NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    CONSTRAINT pk_verification_documents PRIMARY KEY (document_id),
    CONSTRAINT fk_verification_documents_request FOREIGN KEY (verification_id) REFERENCES verification_requests (verification_id),
    CONSTRAINT fk_verification_documents_file FOREIGN KEY (file_id) REFERENCES media_files (file_id),
    CONSTRAINT chk_verification_documents_type CHECK (document_type IN ('ID_CARD','DEGREE','CERTIFICATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE notifications (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    template_id     BIGINT       NULL,
    title           VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    read_at         DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notifications PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_notifications_template FOREIGN KEY (template_id) REFERENCES notification_templates (template_id),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING','SENT','READ','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE conversation_participants (
    participant_id  BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    CONSTRAINT pk_conversation_participants PRIMARY KEY (participant_id),
    CONSTRAINT uq_conversation_participants UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_conversation_participants_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id),
    CONSTRAINT fk_conversation_participants_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE lessons (
    lesson_id            BIGINT      NOT NULL AUTO_INCREMENT,
    slot_id              BIGINT      NOT NULL,
    sequence_no          INT         NOT NULL,
    tutor_id             BIGINT      NOT NULL,
    tutor_check_in_at    DATETIME    NULL,
    tutor_check_out_at   DATETIME    NULL,
    client_confirm_at    DATETIME    NULL,
    attendance_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by_user_id  BIGINT      NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_lessons PRIMARY KEY (lesson_id),
    CONSTRAINT fk_lessons_slot FOREIGN KEY (slot_id) REFERENCES schedule_slots (slot_id),
    CONSTRAINT fk_lessons_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT fk_lessons_approver FOREIGN KEY (approved_by_user_id) REFERENCES users (user_id),
    CONSTRAINT chk_lessons_attendance CHECK (attendance_status IN ('PENDING','COMPLETED','ABSENT','DISPUTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE escrows (
    escrow_id  BIGINT         NOT NULL AUTO_INCREMENT,
    contract_id BIGINT        NOT NULL,
    amount     DECIMAL(15,2)  NOT NULL,
    status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_escrows PRIMARY KEY (escrow_id),
    CONSTRAINT fk_escrows_contract FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT chk_escrows_status CHECK (status IN ('PENDING','FUNDED','RELEASED','REFUNDED','ON_HOLD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE contract_signatures (
    signature_id   BIGINT   NOT NULL AUTO_INCREMENT,
    contract_id    BIGINT   NOT NULL,
    signer_id      BIGINT   NOT NULL,
    signed_at      DATETIME NOT NULL,
    signature_data TEXT     NULL,
    CONSTRAINT pk_contract_signatures PRIMARY KEY (signature_id),
    CONSTRAINT fk_contract_signatures_contract FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_contract_signatures_signer FOREIGN KEY (signer_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE reviews (
    review_id   BIGINT      NOT NULL AUTO_INCREMENT,
    contract_id BIGINT      NOT NULL,
    reviewer_id BIGINT      NOT NULL,
    reviewee_id BIGINT      NOT NULL,
    review_type VARCHAR(30) NOT NULL,
    rating      INT         NOT NULL,
    comment     TEXT        NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reviews PRIMARY KEY (review_id),
    CONSTRAINT fk_reviews_contract FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (user_id),
    CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES users (user_id),
    CONSTRAINT chk_reviews_type CHECK (review_type IN ('CLIENT_TO_TUTOR','TUTOR_TO_CLIENT','CENTER_TO_TUTOR')),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE payment_transactions (
    transaction_id          BIGINT         NOT NULL AUTO_INCREMENT,
    wallet_id                 BIGINT         NOT NULL,
    payment_method_id         BIGINT         NULL,
    external_transaction_id VARCHAR(100)   NULL,
    type                    VARCHAR(30)    NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    amount                  DECIMAL(15,2)  NOT NULL,
    description             TEXT           NULL,
    reference_code          VARCHAR(100)   NULL,
    processed_at            DATETIME       NULL,
    failure_reason          TEXT           NULL,
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_payment_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id),
    CONSTRAINT fk_payment_transactions_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (payment_method_id),
    CONSTRAINT chk_payment_transactions_type CHECK (type IN ('DEPOSIT','WITHDRAWAL','REFUND','ESCROW_DEPOSIT','ESCROW_RELEASE')),
    CONSTRAINT chk_payment_transactions_status CHECK (status IN ('PENDING','SUCCESS','FAILED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE withdrawal_requests (
    withdrawal_id     BIGINT         NOT NULL AUTO_INCREMENT,
    wallet_id         BIGINT         NOT NULL,
    payment_method_id BIGINT         NOT NULL,
    amount            DECIMAL(15,2)  NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    requested_at      DATETIME       NOT NULL,
    processed_at      DATETIME       NULL,
    failure_reason    TEXT           NULL,
    CONSTRAINT pk_withdrawal_requests PRIMARY KEY (withdrawal_id),
    CONSTRAINT fk_withdrawal_requests_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id),
    CONSTRAINT fk_withdrawal_requests_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (payment_method_id),
    CONSTRAINT chk_withdrawal_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE notification_queues (
    queue_id        BIGINT      NOT NULL AUTO_INCREMENT,
    notification_id BIGINT      NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_at    DATETIME    NULL,
    sent_at         DATETIME    NULL,
    CONSTRAINT pk_notification_queues PRIMARY KEY (queue_id),
    CONSTRAINT fk_notification_queues_notification FOREIGN KEY (notification_id) REFERENCES notifications (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE messages (
    message_id      BIGINT      NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT      NOT NULL,
    sender_id       BIGINT      NOT NULL,
    message_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content         TEXT        NOT NULL,
    is_edited       BOOLEAN     NOT NULL DEFAULT FALSE,
    edited_at       DATETIME    NULL,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at         DATETIME    NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (message_id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (user_id),
    CONSTRAINT chk_messages_type CHECK (message_type IN ('TEXT','IMAGE','FILE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    # NO UNIQUE on payment_transaction_id — one escrow may have many transactions
    """
CREATE TABLE escrow_transactions (
    escrow_transaction_id  BIGINT         NOT NULL AUTO_INCREMENT,
    escrow_id              BIGINT         NOT NULL,
    payment_transaction_id BIGINT         NOT NULL,
    type                   VARCHAR(20)    NOT NULL,
    amount                 DECIMAL(15,2)  NOT NULL,
    created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_escrow_transactions PRIMARY KEY (escrow_transaction_id),
    CONSTRAINT fk_escrow_transactions_escrow FOREIGN KEY (escrow_id) REFERENCES escrows (escrow_id),
    CONSTRAINT fk_escrow_transactions_payment FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions (transaction_id),
    CONSTRAINT chk_escrow_transactions_type CHECK (type IN ('DEPOSIT','RELEASE','REFUND'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE refund_requests (
    refund_id    BIGINT         NOT NULL AUTO_INCREMENT,
    escrow_id    BIGINT         NOT NULL,
    requested_by BIGINT         NOT NULL,
    reason       TEXT           NOT NULL,
    amount       DECIMAL(15,2)  NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME       NOT NULL,
    processed_at DATETIME       NULL,
    CONSTRAINT pk_refund_requests PRIMARY KEY (refund_id),
    CONSTRAINT fk_refund_requests_escrow FOREIGN KEY (escrow_id) REFERENCES escrows (escrow_id),
    CONSTRAINT fk_refund_requests_user FOREIGN KEY (requested_by) REFERENCES users (user_id),
    CONSTRAINT chk_refund_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE disputes (
    dispute_id BIGINT      NOT NULL AUTO_INCREMENT,
    escrow_id  BIGINT      NOT NULL,
    reason     TEXT        NOT NULL,
    resolution TEXT        NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_disputes PRIMARY KEY (dispute_id),
    CONSTRAINT fk_disputes_escrow FOREIGN KEY (escrow_id) REFERENCES escrows (escrow_id),
    CONSTRAINT chk_disputes_status CHECK (status IN ('OPEN','UNDER_INVESTIGATION','RESOLVED','WAITING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE payment_release_requests (
    request_id   BIGINT         NOT NULL AUTO_INCREMENT,
    escrow_id    BIGINT         NOT NULL,
    amount       DECIMAL(15,2)  NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME       NULL,
    CONSTRAINT pk_payment_release_requests PRIMARY KEY (request_id),
    CONSTRAINT fk_payment_release_requests_escrow FOREIGN KEY (escrow_id) REFERENCES escrows (escrow_id),
    CONSTRAINT chk_payment_release_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','PAID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE message_attachments (
    attachment_id BIGINT   NOT NULL AUTO_INCREMENT,
    message_id    BIGINT   NOT NULL,
    file_id       BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_message_attachments PRIMARY KEY (attachment_id),
    CONSTRAINT fk_message_attachments_message FOREIGN KEY (message_id) REFERENCES messages (message_id),
    CONSTRAINT fk_message_attachments_file FOREIGN KEY (file_id) REFERENCES media_files (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE recruitment_applications (
    recruitment_app_id     BIGINT      NOT NULL AUTO_INCREMENT,
    recruitment_id         BIGINT      NOT NULL,
    tutor_id               BIGINT      NOT NULL,
    cover_letter           TEXT        NULL,
    cv_file_id             BIGINT      NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    interview_scheduled_at DATETIME    NULL,
    internal_notes         TEXT        NULL,
    applied_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recruitment_applications PRIMARY KEY (recruitment_app_id),
    CONSTRAINT uq_recruitment_applications UNIQUE (recruitment_id, tutor_id),
    CONSTRAINT fk_recruitment_applications_post FOREIGN KEY (recruitment_id) REFERENCES recruitment_posts (recruitment_id),
    CONSTRAINT fk_recruitment_applications_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT fk_recruitment_applications_cv FOREIGN KEY (cv_file_id) REFERENCES media_files (file_id),
    CONSTRAINT chk_recruitment_applications_status CHECK (status IN ('SUBMITTED','REVIEWING','INTERVIEW','ACCEPTED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE lead_assignments (
    assignment_id BIGINT   NOT NULL AUTO_INCREMENT,
    lead_id       BIGINT   NOT NULL,
    assigned_to   BIGINT   NOT NULL,
    assigned_at   DATETIME NOT NULL,
    CONSTRAINT pk_lead_assignments PRIMARY KEY (assignment_id),
    CONSTRAINT fk_lead_assignments_lead FOREIGN KEY (lead_id) REFERENCES leads (lead_id),
    CONSTRAINT fk_lead_assignments_user FOREIGN KEY (assigned_to) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
    """
CREATE TABLE recommendation_logs (
    recommendation_id BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    class_id          BIGINT        NOT NULL,
    tutor_id          BIGINT        NOT NULL,
    score             DECIMAL(5,2)  NOT NULL,
    algorithm_version VARCHAR(20)   NULL,
    reason            TEXT          NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recommendation_logs PRIMARY KEY (recommendation_id),
    CONSTRAINT fk_recommendation_logs_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_recommendation_logs_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_recommendation_logs_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""",
]

if __name__ == "__main__":
    content = HEADER + "\n".join(TABLES) + FOOTER
    OUT.write_text(content, encoding="utf-8")
    print(f"Wrote {OUT} ({len(TABLES)} tables)")
