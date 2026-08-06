-- V12: Allow early termination requests to target either PRIVATE assignments
--      or CENTER enrollments.

SET NAMES utf8mb4;

ALTER TABLE class_termination_requests MODIFY assignment_id BIGINT NULL;

SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'class_termination_requests'
       AND COLUMN_NAME = 'class_student_id') = 0,
  'ALTER TABLE class_termination_requests ADD COLUMN class_student_id BIGINT NULL AFTER assignment_id',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND TABLE_NAME = 'class_termination_requests'
       AND CONSTRAINT_NAME = 'fk_class_termination_class_student') = 0,
  'ALTER TABLE class_termination_requests ADD CONSTRAINT fk_class_termination_class_student FOREIGN KEY (class_student_id) REFERENCES class_students (class_student_id)',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := IF(
  (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND CONSTRAINT_NAME = 'chk_class_termination_target') > 0,
  'ALTER TABLE class_termination_requests DROP CHECK chk_class_termination_target',
  'DO 0'
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE class_termination_requests ADD CONSTRAINT chk_class_termination_target
    CHECK ((assignment_id IS NULL) <> (class_student_id IS NULL));
