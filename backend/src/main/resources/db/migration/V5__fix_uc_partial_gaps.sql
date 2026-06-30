-- Fix remaining UC partial gaps: TutorSubject, Wallet status, PaymentReleaseRequest FK,
-- Review uniqueness, drop redundant max_sessions.
-- Idempotent where possible for retry after partial failure.

-- tutoring_classes: keep number_of_sessions only (UC-14)
SET @has_max_sessions := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tutoring_classes'
      AND COLUMN_NAME = 'max_sessions'
);

SET @sync_sessions_sql := IF(
    @has_max_sessions > 0,
    'UPDATE tutoring_classes SET number_of_sessions = GREATEST(number_of_sessions, max_sessions)',
    'SELECT 1'
);
PREPARE sync_sessions_stmt FROM @sync_sessions_sql;
EXECUTE sync_sessions_stmt;
DEALLOCATE PREPARE sync_sessions_stmt;

SET @drop_max_sessions_only_sql := IF(
    @has_max_sessions > 0,
    'ALTER TABLE tutoring_classes DROP COLUMN max_sessions',
    'SELECT 1'
);
PREPARE drop_max_sessions_only_stmt FROM @drop_max_sessions_only_sql;
EXECUTE drop_max_sessions_only_stmt;
DEALLOCATE PREPARE drop_max_sessions_only_stmt;

-- tutor_subjects: Subject-only taxonomy (UC-31) — recreate to avoid FK/index coupling
SET @needs_tutor_subject_migration := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tutor_subjects'
      AND COLUMN_NAME = 'category_id'
);

SET @cleanup_tutor_subjects_new_sql := IF(
    (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tutor_subjects_new') > 0,
    'DROP TABLE tutor_subjects_new',
    'SELECT 1'
);
PREPARE cleanup_tutor_subjects_new_stmt FROM @cleanup_tutor_subjects_new_sql;
EXECUTE cleanup_tutor_subjects_new_stmt;
DEALLOCATE PREPARE cleanup_tutor_subjects_new_stmt;

SET @recreate_tutor_subjects_sql := IF(
    @needs_tutor_subject_migration > 0,
    'CREATE TABLE tutor_subjects_new (
        tutor_subject_id  BIGINT       NOT NULL AUTO_INCREMENT,
        tutor_id          BIGINT       NOT NULL,
        subject_id        BIGINT       NOT NULL,
        proficiency_level VARCHAR(50)  NULL,
        CONSTRAINT pk_tutor_subjects_new PRIMARY KEY (tutor_subject_id),
        CONSTRAINT uq_tutor_subjects_new UNIQUE (tutor_id, subject_id),
        CONSTRAINT fk_tutor_subjects_new_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
        CONSTRAINT fk_tutor_subjects_new_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT 1'
);
PREPARE recreate_tutor_subjects_stmt FROM @recreate_tutor_subjects_sql;
EXECUTE recreate_tutor_subjects_stmt;
DEALLOCATE PREPARE recreate_tutor_subjects_stmt;

SET @copy_tutor_subjects_sql := IF(
    @needs_tutor_subject_migration > 0,
    'INSERT INTO tutor_subjects_new (tutor_subject_id, tutor_id, subject_id, proficiency_level)
     SELECT tutor_subject_id, tutor_id, subject_id, proficiency_level
     FROM tutor_subjects
     WHERE subject_id IS NOT NULL',
    'SELECT 1'
);
PREPARE copy_tutor_subjects_stmt FROM @copy_tutor_subjects_sql;
EXECUTE copy_tutor_subjects_stmt;
DEALLOCATE PREPARE copy_tutor_subjects_stmt;

SET @swap_tutor_subjects_sql := IF(
    @needs_tutor_subject_migration > 0,
    'SET FOREIGN_KEY_CHECKS = 0; DROP TABLE tutor_subjects; ALTER TABLE tutor_subjects_new RENAME TO tutor_subjects; SET FOREIGN_KEY_CHECKS = 1',
    'SELECT 1'
);
-- MySQL prepared statements cannot run multiple statements in one PREPARE; run swap steps when needed
SET @drop_old_tutor_subjects_sql := IF(
    @needs_tutor_subject_migration > 0,
    'DROP TABLE tutor_subjects',
    'SELECT 1'
);
PREPARE drop_old_tutor_subjects_stmt FROM @drop_old_tutor_subjects_sql;
SET FOREIGN_KEY_CHECKS = 0;
EXECUTE drop_old_tutor_subjects_stmt;
DEALLOCATE PREPARE drop_old_tutor_subjects_stmt;

SET @rename_tutor_subjects_sql := IF(
    @needs_tutor_subject_migration > 0,
    'ALTER TABLE tutor_subjects_new RENAME TO tutor_subjects',
    'SELECT 1'
);
PREPARE rename_tutor_subjects_stmt FROM @rename_tutor_subjects_sql;
EXECUTE rename_tutor_subjects_stmt;
DEALLOCATE PREPARE rename_tutor_subjects_stmt;
SET FOREIGN_KEY_CHECKS = 1;

-- wallets: align status with spec (UC-39)
UPDATE wallets SET status = 'SUSPENDED' WHERE status = 'FROZEN';

SET @wallet_check_has_suspended := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS tc
    INNER JOIN information_schema.CHECK_CONSTRAINTS cc
        ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
       AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'wallets'
      AND cc.CHECK_CLAUSE LIKE '%SUSPENDED%'
);

SET @wallet_check_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wallets'
      AND CONSTRAINT_NAME = 'chk_wallets_status'
);

SET @wallet_check_drop_sql := IF(
    @wallet_check_has_suspended = 0 AND @wallet_check_exists > 0,
    'ALTER TABLE wallets DROP CHECK chk_wallets_status',
    'SELECT 1'
);
PREPARE wallet_check_drop_stmt FROM @wallet_check_drop_sql;
EXECUTE wallet_check_drop_stmt;
DEALLOCATE PREPARE wallet_check_drop_stmt;

SET @wallet_check_add_sql := IF(
    @wallet_check_has_suspended = 0,
    'ALTER TABLE wallets ADD CONSTRAINT chk_wallets_status CHECK (status IN (''ACTIVE'',''SUSPENDED'',''CLOSED''))',
    'SELECT 1'
);
PREPARE wallet_check_add_stmt FROM @wallet_check_add_sql;
EXECUTE wallet_check_add_stmt;
DEALLOCATE PREPARE wallet_check_add_stmt;

-- payment_release_requests: FK assignment_id per spec (UC-47/58)
SET @has_assignment_col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment_release_requests'
      AND COLUMN_NAME = 'assignment_id'
);

SET @add_assignment_col_sql := IF(
    @has_assignment_col = 0,
    'ALTER TABLE payment_release_requests ADD COLUMN assignment_id BIGINT NULL AFTER request_id',
    'SELECT 1'
);
PREPARE add_assignment_col_stmt FROM @add_assignment_col_sql;
EXECUTE add_assignment_col_stmt;
DEALLOCATE PREPARE add_assignment_col_stmt;

SET @has_escrow_col_for_update := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment_release_requests'
      AND COLUMN_NAME = 'escrow_id'
);
SET @populate_assignment_sql := IF(
    @has_escrow_col_for_update > 0,
    'UPDATE payment_release_requests prr INNER JOIN escrow_transactions et ON prr.escrow_id = et.escrow_id SET prr.assignment_id = et.assignment_id WHERE prr.assignment_id IS NULL',
    'SELECT 1'
);
PREPARE populate_assignment_stmt FROM @populate_assignment_sql;
EXECUTE populate_assignment_stmt;
DEALLOCATE PREPARE populate_assignment_stmt;

SET @modify_assignment_not_null_sql := IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'payment_release_requests'
       AND COLUMN_NAME = 'assignment_id') > 0,
    'ALTER TABLE payment_release_requests MODIFY assignment_id BIGINT NOT NULL',
    'SELECT 1'
);
PREPARE modify_assignment_not_null_stmt FROM @modify_assignment_not_null_sql;
EXECUTE modify_assignment_not_null_stmt;
DEALLOCATE PREPARE modify_assignment_not_null_stmt;

SET @has_escrow_fk := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment_release_requests'
      AND CONSTRAINT_NAME = 'fk_payment_release_requests_escrow'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @drop_escrow_fk_sql := IF(
    @has_escrow_fk > 0,
    'ALTER TABLE payment_release_requests DROP FOREIGN KEY fk_payment_release_requests_escrow',
    'SELECT 1'
);
PREPARE drop_escrow_fk_stmt FROM @drop_escrow_fk_sql;
EXECUTE drop_escrow_fk_stmt;
DEALLOCATE PREPARE drop_escrow_fk_stmt;

SET @has_escrow_col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment_release_requests'
      AND COLUMN_NAME = 'escrow_id'
);
SET @drop_escrow_col_sql := IF(
    @has_escrow_col > 0,
    'ALTER TABLE payment_release_requests DROP COLUMN escrow_id',
    'SELECT 1'
);
PREPARE drop_escrow_col_stmt FROM @drop_escrow_col_sql;
EXECUTE drop_escrow_col_stmt;
DEALLOCATE PREPARE drop_escrow_col_stmt;

SET @has_assignment_fk := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment_release_requests'
      AND CONSTRAINT_NAME = 'fk_payment_release_requests_assignment'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @add_assignment_fk_sql := IF(
    @has_assignment_fk = 0,
    'ALTER TABLE payment_release_requests ADD CONSTRAINT fk_payment_release_requests_assignment FOREIGN KEY (assignment_id) REFERENCES class_assignments (assignment_id)',
    'SELECT 1'
);
PREPARE add_assignment_fk_stmt FROM @add_assignment_fk_sql;
EXECUTE add_assignment_fk_stmt;
DEALLOCATE PREPARE add_assignment_fk_stmt;

-- reviews: one review per reviewer/type per assignment (UC-54)
SET @has_review_uq := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reviews'
      AND CONSTRAINT_NAME = 'uq_reviews_assignment_reviewer_type'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
SET @add_review_uq_sql := IF(
    @has_review_uq = 0,
    'ALTER TABLE reviews ADD CONSTRAINT uq_reviews_assignment_reviewer_type UNIQUE (assignment_id, reviewer_id, review_type)',
    'SELECT 1'
);
PREPARE add_review_uq_stmt FROM @add_review_uq_sql;
EXECUTE add_review_uq_stmt;
DEALLOCATE PREPARE add_review_uq_stmt;
