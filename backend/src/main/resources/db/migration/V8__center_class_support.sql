-- =====================================================================
-- BF-04 / BF-06 Center Class support (data model, khong gom service).
-- Phan tach 2 truc doc lap:
--   - class_type : PRIVATE (lop gia su) / CENTER (lop day them)
--   - tutor source: INTERNAL (gan truc tiep) / EXTERNAL (qua application)
-- Bo sung suc chua & han ghi danh cho lop CENTER, gan tutor noi bo,
-- escrow/contract theo tung ghi danh, va diem danh theo tung hoc sinh.
-- Tat ca la migration cong them + nullable + backfill an toan,
-- khong pha du lieu nhanh PRIVATE hien co.
-- =====================================================================

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

-- budget chi co nghia voi PRIVATE (client de xuat ngan sach) -> cho phep NULL
ALTER TABLE tutoring_classes MODIFY budget DECIMAL(12,2) NULL;

ALTER TABLE tutoring_classes
    ADD CONSTRAINT fk_tutoring_classes_center FOREIGN KEY (center_id) REFERENCES tutor_centers (center_id);
ALTER TABLE tutoring_classes
    ADD CONSTRAINT chk_tutoring_classes_type CHECK (class_type IN ('PRIVATE','CENTER'));

-- status: them ENROLLMENT_CLOSED (auto-close khi du max_students hoac het han ghi danh)
ALTER TABLE tutoring_classes DROP CHECK chk_tutoring_classes_status;
ALTER TABLE tutoring_classes ADD CONSTRAINT chk_tutoring_classes_status
    CHECK (status IN ('DRAFT','OPEN','MATCHED','ENROLLMENT_CLOSED','IN_PROGRESS','COMPLETED','CANCELLED','DISPUTED'));

-- ---------------------------------------------------------------------
-- 2. class_assignments: cho phep gan tutor noi bo (khong qua application)
--    - tutor_id: nguon su that cua tutor (thay vi lay gian tiep qua application)
--    - application_id: chi set khi tutor den tu marketplace (EXTERNAL)
-- ---------------------------------------------------------------------
ALTER TABLE class_assignments ADD COLUMN tutor_id BIGINT NULL AFTER assignment_id;

-- backfill tutor cho cac assignment hien co (deu la EXTERNAL, co application)
UPDATE class_assignments ca
    JOIN tutor_applications ta ON ca.application_id = ta.application_id
    SET ca.tutor_id = ta.tutor_id
    WHERE ca.tutor_id IS NULL;

ALTER TABLE class_assignments MODIFY tutor_id BIGINT NOT NULL;
ALTER TABLE class_assignments
    ADD CONSTRAINT fk_class_assignments_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id);

-- application_id nullable (unique van cho phep nhieu NULL)
ALTER TABLE class_assignments MODIFY application_id BIGINT NULL;

-- ---------------------------------------------------------------------
-- 3. class_students: nguoi ghi danh / tra hoc phi (khong them trang thai duyet)
--    Ghi danh khong duyet: du max hoac het han thi lop tu dong dong.
-- ---------------------------------------------------------------------
ALTER TABLE class_students ADD COLUMN enrolled_by_user_id BIGINT NULL AFTER child_profile_id;
ALTER TABLE class_students
    ADD CONSTRAINT fk_class_students_enrolled_by FOREIGN KEY (enrolled_by_user_id) REFERENCES users (user_id);

-- ---------------------------------------------------------------------
-- 4. escrow_transactions: gan vao MOT trong hai nguon
--    - assignment_id : PRIVATE (giai ngan ve tutor)
--    - class_student_id : CENTER (escrow theo tung ghi danh, giai ngan ve center)
-- ---------------------------------------------------------------------
ALTER TABLE escrow_transactions MODIFY assignment_id BIGINT NULL;
ALTER TABLE escrow_transactions ADD COLUMN class_student_id BIGINT NULL AFTER assignment_id;
ALTER TABLE escrow_transactions
    ADD CONSTRAINT fk_escrow_transactions_class_student FOREIGN KEY (class_student_id) REFERENCES class_students (class_student_id);
ALTER TABLE escrow_transactions
    ADD CONSTRAINT chk_escrow_transactions_target CHECK ((assignment_id IS NULL) <> (class_student_id IS NULL));

-- ---------------------------------------------------------------------
-- 5. contracts: gan vao MOT trong hai nguon (giong escrow)
--    - assignment_id : hop dong PRIVATE (client/center <-> tutor)
--    - class_student_id : hop dong CENTER theo tung ghi danh (client <-> center)
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
--    Lop CENTER dung bang nay; lop PRIVATE van dung lessons.attendance_status.
-- ---------------------------------------------------------------------
CREATE TABLE lesson_attendances (
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
