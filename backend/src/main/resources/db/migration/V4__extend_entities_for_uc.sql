-- Extend entities to align with Entity Definitions 3.1 / 71 UC.

-- users
ALTER TABLE users ADD COLUMN phone VARCHAR(15) NULL AFTER email;
ALTER TABLE users ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
UPDATE users SET status = 'ACTIVE' WHERE status = 'PENDING';
ALTER TABLE users MODIFY status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users DROP CHECK chk_users_status;
ALTER TABLE users ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','SUSPENDED','BANNED'));

-- clients
ALTER TABLE clients ADD COLUMN date_of_birth DATE NULL AFTER phone;
ALTER TABLE clients ADD COLUMN gender VARCHAR(10) NULL AFTER date_of_birth;
ALTER TABLE clients ADD COLUMN location_id BIGINT NULL AFTER address;
ALTER TABLE clients CHANGE COLUMN avatar avatar_url VARCHAR(255) NULL;
ALTER TABLE clients ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE clients ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE clients ADD CONSTRAINT fk_clients_location FOREIGN KEY (location_id) REFERENCES locations (location_id);
ALTER TABLE clients ADD CONSTRAINT chk_clients_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','OTHER'));

-- tutors
ALTER TABLE tutors ADD COLUMN date_of_birth DATE NULL AFTER gender;
ALTER TABLE tutors ADD COLUMN location_id BIGINT NULL AFTER address;
ALTER TABLE tutors ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'UNDER_VERIFY' AFTER rating_avg;
ALTER TABLE tutors ADD COLUMN avatar VARCHAR(255) NULL AFTER verification_status;
ALTER TABLE tutors ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tutors ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE tutors ADD CONSTRAINT fk_tutors_location FOREIGN KEY (location_id) REFERENCES locations (location_id);
ALTER TABLE tutors ADD CONSTRAINT chk_tutors_verification CHECK (verification_status IN ('UNDER_VERIFY','VERIFIED','REJECTED'));

-- tutor_centers
ALTER TABLE tutor_centers ADD COLUMN location_id BIGINT NULL AFTER address;
ALTER TABLE tutor_centers ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'UNDER_VERIFY' AFTER description;
ALTER TABLE tutor_centers ADD COLUMN avatar VARCHAR(255) NULL AFTER verification_status;
ALTER TABLE tutor_centers ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tutor_centers ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE tutor_centers ADD CONSTRAINT fk_tutor_centers_location FOREIGN KEY (location_id) REFERENCES locations (location_id);
ALTER TABLE tutor_centers ADD CONSTRAINT chk_tutor_centers_verification CHECK (verification_status IN ('UNDER_VERIFY','VERIFIED','REJECTED'));

-- platform_admins
ALTER TABLE platform_admins ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE platform_admins ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- verification_requests
ALTER TABLE verification_requests ADD COLUMN admin_notes TEXT NULL AFTER status;
ALTER TABLE verification_requests ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE verification_requests ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
UPDATE verification_requests SET status = 'SUBMITTED' WHERE status = 'PENDING';
UPDATE verification_requests SET status = 'VERIFIED' WHERE status = 'APPROVED';
ALTER TABLE verification_requests DROP CHECK chk_verification_status;
ALTER TABLE verification_requests ADD CONSTRAINT chk_verification_status CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','VERIFIED','REJECTED'));

-- verification_documents
ALTER TABLE verification_documents DROP CHECK chk_verification_documents_type;
ALTER TABLE verification_documents ADD CONSTRAINT chk_verification_documents_type CHECK (document_type IN ('ID_CARD','DEGREE','CERTIFICATE','LICENSE'));

-- tutoring_classes
ALTER TABLE tutoring_classes MODIFY category_id BIGINT NULL;
ALTER TABLE tutoring_classes ADD COLUMN subject_id BIGINT NULL AFTER category_id;
ALTER TABLE tutoring_classes ADD COLUMN grade_id BIGINT NULL AFTER subject_id;
ALTER TABLE tutoring_classes ADD COLUMN location_id BIGINT NULL AFTER description;
ALTER TABLE tutoring_classes ADD COLUMN lesson_mode VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' AFTER location_id;
ALTER TABLE tutoring_classes ADD COLUMN number_of_sessions INT NOT NULL DEFAULT 1 AFTER lesson_mode;
ALTER TABLE tutoring_classes ADD COLUMN tuition_fee DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER max_sessions;
ALTER TABLE tutoring_classes ADD COLUMN recurring_type VARCHAR(20) NOT NULL DEFAULT 'ONCE' AFTER budget;
ALTER TABLE tutoring_classes ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE tutoring_classes ADD CONSTRAINT fk_tutoring_classes_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id);
ALTER TABLE tutoring_classes ADD CONSTRAINT fk_tutoring_classes_grade FOREIGN KEY (grade_id) REFERENCES grades (grade_id);
ALTER TABLE tutoring_classes ADD CONSTRAINT fk_tutoring_classes_location FOREIGN KEY (location_id) REFERENCES locations (location_id);
ALTER TABLE tutoring_classes DROP CHECK chk_tutoring_classes_status;
ALTER TABLE tutoring_classes ADD CONSTRAINT chk_tutoring_classes_status CHECK (status IN ('DRAFT','OPEN','MATCHED','IN_PROGRESS','COMPLETED','CANCELLED','DISPUTED'));
ALTER TABLE tutoring_classes ADD CONSTRAINT chk_tutoring_classes_lesson_mode CHECK (lesson_mode IN ('ONLINE','OFFLINE','HYBRID'));
ALTER TABLE tutoring_classes ADD CONSTRAINT chk_tutoring_classes_recurring CHECK (recurring_type IN ('ONCE','WEEKLY'));

-- tutor_applications
ALTER TABLE tutor_applications ADD COLUMN proposed_rate DECIMAL(12,2) NULL AFTER tutor_id;
ALTER TABLE tutor_applications ADD COLUMN reviewed_at DATETIME NULL AFTER applied_at;
ALTER TABLE tutor_applications DROP CHECK chk_tutor_applications_status;
ALTER TABLE tutor_applications ADD CONSTRAINT chk_tutor_applications_status CHECK (status IN ('SUBMITTED','UNDER_REVIEW','ACCEPTED','REJECTED','WITHDRAWN'));

-- contracts (restructure to assignment-based)
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE contracts DROP FOREIGN KEY fk_contracts_class;
ALTER TABLE contracts DROP FOREIGN KEY fk_contracts_client;
ALTER TABLE contracts DROP FOREIGN KEY fk_contracts_tutor;
ALTER TABLE contracts DROP COLUMN class_id;
ALTER TABLE contracts DROP COLUMN client_id;
ALTER TABLE contracts DROP COLUMN tutor_id;
ALTER TABLE contracts DROP COLUMN total_amount;
ALTER TABLE contracts DROP COLUMN terms;
ALTER TABLE contracts ADD COLUMN contract_no VARCHAR(50) NOT NULL DEFAULT 'TCS-TEMP' AFTER contract_id;
ALTER TABLE contracts ADD COLUMN assignment_id BIGINT NOT NULL AFTER contract_no;
ALTER TABLE contracts ADD COLUMN template_id BIGINT NULL AFTER assignment_id;
ALTER TABLE contracts ADD COLUMN contract_file_url VARCHAR(500) NULL AFTER template_id;
ALTER TABLE contracts ADD COLUMN terms_summary TEXT NULL AFTER contract_file_url;
ALTER TABLE contracts ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
UPDATE contracts SET status = 'SIGNED' WHERE status = 'PENDING_SIGNATURE';
ALTER TABLE contracts DROP CHECK chk_contracts_status;
ALTER TABLE contracts ADD CONSTRAINT uq_contracts_no UNIQUE (contract_no);
ALTER TABLE contracts ADD CONSTRAINT uq_contracts_assignment UNIQUE (assignment_id);
ALTER TABLE contracts ADD CONSTRAINT fk_contracts_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id);
ALTER TABLE contracts ADD CONSTRAINT fk_contracts_template FOREIGN KEY (template_id) REFERENCES contract_templates (template_id);
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_status CHECK (status IN ('DRAFT','SIGNED','ACTIVE','COMPLETED','TERMINATED'));
SET FOREIGN_KEY_CHECKS = 1;

-- reviews
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE reviews DROP FOREIGN KEY fk_reviews_contract;
ALTER TABLE reviews DROP COLUMN contract_id;
ALTER TABLE reviews ADD COLUMN assignment_id BIGINT NOT NULL AFTER review_id;
ALTER TABLE reviews ADD COLUMN class_id BIGINT NULL AFTER assignment_id;
ALTER TABLE reviews ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE' AFTER comment;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id);
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id);
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_status CHECK (status IN ('VISIBLE','HIDDEN','MODERATED'));
SET FOREIGN_KEY_CHECKS = 1;

-- disputes
ALTER TABLE disputes DROP COLUMN reason;
ALTER TABLE disputes ADD COLUMN report_id BIGINT NOT NULL AFTER dispute_id;
ALTER TABLE disputes ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE disputes ADD CONSTRAINT uq_disputes_report UNIQUE (report_id);
ALTER TABLE disputes ADD CONSTRAINT fk_disputes_report FOREIGN KEY (report_id) REFERENCES reports (report_id);

-- wallets
ALTER TABLE wallets ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- notifications
ALTER TABLE notifications ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' AFTER user_id;
ALTER TABLE notifications MODIFY title VARCHAR(200) NULL;
ALTER TABLE notifications ADD COLUMN reference_type VARCHAR(50) NULL AFTER content;
ALTER TABLE notifications ADD COLUMN reference_id BIGINT NULL AFTER reference_type;
ALTER TABLE notifications ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE AFTER reference_id;

-- conversations
ALTER TABLE conversations ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER type;
ALTER TABLE conversations ADD CONSTRAINT chk_conversations_status CHECK (status IN ('ACTIVE','ARCHIVED'));

-- support_tickets
ALTER TABLE support_tickets ADD COLUMN target_class_id BIGINT NULL AFTER user_id;
ALTER TABLE support_tickets ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'INQUIRY' AFTER assigned_admin_id;
ALTER TABLE support_tickets ADD COLUMN evidence_urls TEXT NULL AFTER description;
ALTER TABLE support_tickets ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE support_tickets DROP COLUMN type;
ALTER TABLE support_tickets ADD CONSTRAINT fk_support_tickets_class FOREIGN KEY (target_class_id) REFERENCES tutoring_classes (class_id);
ALTER TABLE support_tickets DROP CHECK chk_support_tickets_status;
ALTER TABLE support_tickets ADD CONSTRAINT chk_support_tickets_status CHECK (status IN ('OPEN','IN_PROGRESS','IN_REVIEW','RESOLVED','CLOSED'));
ALTER TABLE support_tickets ADD CONSTRAINT chk_support_tickets_category CHECK (category IN ('DISPUTE','SYSTEM_ERROR','REPORT_USER','BUG_REPORT','INQUIRY'));

-- categories
ALTER TABLE categories MODIFY name VARCHAR(150) NOT NULL;
ALTER TABLE categories ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM_CONFIG' AFTER parent_id;
ALTER TABLE categories ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE AFTER description;
ALTER TABLE categories ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER is_active;
ALTER TABLE categories ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE categories ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE categories ADD CONSTRAINT chk_categories_type CHECK (type IN ('SUBJECT','EDUCATION_LEVEL','LOCATION','SYSTEM_CONFIG'));

-- faq_entries
ALTER TABLE faq_entries ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER category;
ALTER TABLE faq_entries ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT TRUE AFTER sort_order;
ALTER TABLE faq_entries ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE faq_entries ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- tutor_subjects
ALTER TABLE tutor_subjects MODIFY category_id BIGINT NULL;
ALTER TABLE tutor_subjects ADD COLUMN subject_id BIGINT NULL AFTER category_id;
ALTER TABLE tutor_subjects ADD CONSTRAINT fk_tutor_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id);

-- recruitment_posts
UPDATE recruitment_posts SET status = 'ACTIVE' WHERE status = 'OPEN';
UPDATE recruitment_posts SET status = 'CLOSED' WHERE status = 'EXPIRED';
ALTER TABLE recruitment_posts ADD COLUMN required_experience INT NULL DEFAULT 0 AFTER benefits;
ALTER TABLE recruitment_posts ADD COLUMN subject_id BIGINT NULL AFTER required_experience;
ALTER TABLE recruitment_posts ADD COLUMN location_id BIGINT NULL AFTER subject_id;
ALTER TABLE recruitment_posts ADD COLUMN max_positions INT NOT NULL DEFAULT 1 AFTER location_id;
ALTER TABLE recruitment_posts ADD COLUMN published_at DATETIME NULL AFTER status;
ALTER TABLE recruitment_posts ADD COLUMN closed_at DATETIME NULL AFTER published_at;
ALTER TABLE recruitment_posts ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE recruitment_posts DROP COLUMN expired_at;
ALTER TABLE recruitment_posts DROP CHECK chk_recruitment_posts_status;
ALTER TABLE recruitment_posts ADD CONSTRAINT fk_recruitment_posts_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id);
ALTER TABLE recruitment_posts ADD CONSTRAINT fk_recruitment_posts_location FOREIGN KEY (location_id) REFERENCES locations (location_id);
ALTER TABLE recruitment_posts ADD CONSTRAINT chk_recruitment_posts_status CHECK (status IN ('DRAFT','ACTIVE','CLOSED'));

-- recruitment_applications
UPDATE recruitment_applications SET status = 'APPLIED' WHERE status = 'SUBMITTED';
UPDATE recruitment_applications SET status = 'SCREENING' WHERE status = 'REVIEWING';
UPDATE recruitment_applications SET status = 'HIRED' WHERE status = 'ACCEPTED';
ALTER TABLE recruitment_applications ADD COLUMN resume_url VARCHAR(500) NULL AFTER cover_letter;
ALTER TABLE recruitment_applications CHANGE COLUMN interview_scheduled_at interview_date DATETIME NULL;
ALTER TABLE recruitment_applications CHANGE COLUMN internal_notes interview_notes TEXT NULL;
ALTER TABLE recruitment_applications ADD COLUMN reviewed_at DATETIME NULL AFTER applied_at;
ALTER TABLE recruitment_applications ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE recruitment_applications DROP FOREIGN KEY fk_recruitment_applications_cv;
ALTER TABLE recruitment_applications DROP COLUMN cv_file_id;
ALTER TABLE recruitment_applications DROP CHECK chk_recruitment_applications_status;
ALTER TABLE recruitment_applications ADD CONSTRAINT chk_recruitment_applications_status CHECK (status IN ('APPLIED','SCREENING','INTERVIEW','PASSED','HIRED','REJECTED','WITHDRAWN'));

-- center_tutor_memberships
ALTER TABLE center_tutor_memberships ADD COLUMN recruitment_app_id BIGINT NULL AFTER tutor_id;
UPDATE center_tutor_memberships SET status = 'INACTIVE' WHERE status = 'SUSPENDED';
ALTER TABLE center_tutor_memberships DROP CHECK chk_center_tutor_memberships_status;
ALTER TABLE center_tutor_memberships ADD CONSTRAINT fk_center_tutor_recruitment_app FOREIGN KEY (recruitment_app_id) REFERENCES recruitment_applications (recruitment_app_id);
ALTER TABLE center_tutor_memberships ADD CONSTRAINT chk_center_tutor_memberships_status CHECK (status IN ('ACTIVE','INACTIVE','TERMINATED'));

-- recommendation_logs
ALTER TABLE recommendation_logs MODIFY class_id BIGINT NULL;
ALTER TABLE recommendation_logs MODIFY tutor_id BIGINT NULL;
ALTER TABLE recommendation_logs ADD COLUMN recruitment_id BIGINT NULL AFTER tutor_id;
ALTER TABLE recommendation_logs CHANGE COLUMN created_at generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE recommendation_logs ADD CONSTRAINT fk_recommendation_logs_recruitment FOREIGN KEY (recruitment_id) REFERENCES recruitment_posts (recruitment_id);

-- UC-26 / UC-28 request entities
CREATE TABLE tutor_replacement_requests (
    replacement_id     BIGINT      NOT NULL AUTO_INCREMENT,
    class_id           BIGINT      NOT NULL,
    assignment_id      BIGINT      NOT NULL,
    requested_by       BIGINT      NOT NULL,
    preferred_tutor_id BIGINT      NULL,
    reason             TEXT        NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at        DATETIME    NULL,
    CONSTRAINT pk_tutor_replacement_requests PRIMARY KEY (replacement_id),
    CONSTRAINT fk_tutor_replacement_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_tutor_replacement_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id),
    CONSTRAINT fk_tutor_replacement_requester FOREIGN KEY (requested_by) REFERENCES users (user_id),
    CONSTRAINT fk_tutor_replacement_preferred FOREIGN KEY (preferred_tutor_id) REFERENCES tutors (tutor_id),
    CONSTRAINT chk_tutor_replacement_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE class_termination_requests (
    termination_id BIGINT      NOT NULL AUTO_INCREMENT,
    assignment_id  BIGINT      NOT NULL,
    requested_by   BIGINT      NOT NULL,
    reason         TEXT        NOT NULL,
    effective_date DATE        NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at   DATETIME    NULL,
    CONSTRAINT pk_class_termination_requests PRIMARY KEY (termination_id),
    CONSTRAINT fk_class_termination_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id),
    CONSTRAINT fk_class_termination_requester FOREIGN KEY (requested_by) REFERENCES users (user_id),
    CONSTRAINT chk_class_termination_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
