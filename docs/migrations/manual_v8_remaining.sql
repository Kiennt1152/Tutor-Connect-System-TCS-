-- =====================================================================
-- MANUAL MIGRATION: V8__center_class_support.sql (sections 1, 3, 4, 5, 6)
-- Section 2 (class_assignments.tutor_id) da chay o buoc truoc.
-- Chon DB truoc trong Workbench (double-click SCHEMAS), hoac chay lenh USE.
-- =====================================================================

USE tutorconnectsystem;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. tutoring_classes: phan loai lop + suc chua + han ghi danh
-- ---------------------------------------------------------------------
ALTER TABLE tutoring_classes
    ADD COLUMN class_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' AFTER creator_id;
ALTER TABLE tutoring_classes
    ADD COLUMN center_id BIGINT NULL AFTER class_type;
ALTER TABLE tutoring_classes
    ADD COLUMN max_students INT NULL AFTER budget;
ALTER TABLE tutoring_classes
    ADD COLUMN min_students INT NULL AFTER max_students;
ALTER TABLE tutoring_classes
    ADD COLUMN enrollment_deadline DATE NULL AFTER min_students;

ALTER TABLE tutoring_classes MODIFY budget DECIMAL(12,2) NULL;

ALTER TABLE tutoring_classes
    ADD CONSTRAINT fk_tutoring_classes_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id);
ALTER TABLE tutoring_classes
    ADD CONSTRAINT chk_tutoring_classes_type CHECK (class_type IN ('PRIVATE','CENTER'));

ALTER TABLE tutoring_classes DROP CHECK chk_tutoring_classes_status;
ALTER TABLE tutoring_classes ADD CONSTRAINT chk_tutoring_classes_status
    CHECK (status IN ('DRAFT','OPEN','MATCHED','ENROLLMENT_CLOSED','IN_PROGRESS','COMPLETED','CANCELLED','DISPUTED'));

-- ---------------------------------------------------------------------
-- 3. class_students: nguoi ghi danh / tra hoc phi
-- ---------------------------------------------------------------------
ALTER TABLE class_students ADD COLUMN enrolled_by_user_id BIGINT NULL AFTER child_profile_id;
ALTER TABLE class_students
    ADD CONSTRAINT fk_class_students_enrolled_by FOREIGN KEY (enrolled_by_user_id) REFERENCES users (user_id);

-- ---------------------------------------------------------------------
-- 4. escrow_transactions: gan vao MOT trong hai nguon
-- ---------------------------------------------------------------------
ALTER TABLE escrow_transactions MODIFY assignment_id BIGINT NULL;
ALTER TABLE escrow_transactions ADD COLUMN class_student_id BIGINT NULL AFTER assignment_id;
ALTER TABLE escrow_transactions
    ADD CONSTRAINT fk_escrow_transactions_class_student FOREIGN KEY (class_student_id) REFERENCES class_students (class_student_id);
ALTER TABLE escrow_transactions
    ADD CONSTRAINT chk_escrow_transactions_target CHECK ((assignment_id IS NULL) <> (class_student_id IS NULL));

-- ---------------------------------------------------------------------
-- 5. contracts: gan vao MOT trong hai nguon (giong escrow)
-- ---------------------------------------------------------------------
ALTER TABLE contracts MODIFY assignment_id BIGINT NULL;
ALTER TABLE contracts ADD COLUMN class_student_id BIGINT NULL AFTER assignment_id;
ALTER TABLE contracts
    ADD CONSTRAINT uq_contracts_class_student UNIQUE (class_student_id);
ALTER TABLE contracts
    ADD CONSTRAINT fk_contracts_class_student FOREIGN KEY (class_student_id) REFERENCES class_students (class_student_id);
ALTER TABLE contracts
    ADD CONSTRAINT chk_contracts_target CHECK ((assignment_id IS NULL) <> (class_student_id IS NULL));

-- ---------------------------------------------------------------------
-- 6. lesson_attendances: diem danh theo tung hoc sinh (BF-06)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_attendances (
    attendance_id     BIGINT      NOT NULL AUTO_INCREMENT,
    lesson_id         BIGINT      NOT NULL,
    class_student_id  BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    note              TEXT        NULL,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_lesson_attendances PRIMARY KEY (attendance_id),
    CONSTRAINT uq_lesson_attendances UNIQUE (lesson_id, class_student_id),
    CONSTRAINT fk_lesson_attendances_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (lesson_id),
    CONSTRAINT fk_lesson_attendances_student FOREIGN KEY (class_student_id) REFERENCES class_students (class_student_id),
    CONSTRAINT chk_lesson_attendances_status CHECK (status IN ('PRESENT','ABSENT','EXCUSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;