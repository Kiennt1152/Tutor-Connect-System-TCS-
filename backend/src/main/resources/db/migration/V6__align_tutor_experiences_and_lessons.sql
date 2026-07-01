-- UC-09: tutor_experiences.title -> role
-- UC-18/25: lessons.class_id (denormalized from schedule_slots)

SET @has_experience_title := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tutor_experiences'
      AND COLUMN_NAME = 'title'
);

SET @rename_experience_role_sql := IF(
    @has_experience_title > 0,
    'ALTER TABLE tutor_experiences CHANGE COLUMN title role VARCHAR(150) NOT NULL',
    'SELECT 1'
);
PREPARE rename_experience_role_stmt FROM @rename_experience_role_sql;
EXECUTE rename_experience_role_stmt;
DEALLOCATE PREPARE rename_experience_role_stmt;

SET @has_lesson_class_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lessons'
      AND COLUMN_NAME = 'class_id'
);

SET @add_lesson_class_id_sql := IF(
    @has_lesson_class_id = 0,
    'ALTER TABLE lessons ADD COLUMN class_id BIGINT NULL AFTER lesson_id',
    'SELECT 1'
);
PREPARE add_lesson_class_id_stmt FROM @add_lesson_class_id_sql;
EXECUTE add_lesson_class_id_stmt;
DEALLOCATE PREPARE add_lesson_class_id_stmt;

SET @populate_lesson_class_id_sql := IF(
    @has_lesson_class_id = 0,
    'UPDATE lessons l INNER JOIN schedule_slots ss ON l.slot_id = ss.slot_id SET l.class_id = ss.class_id WHERE l.class_id IS NULL',
    'SELECT 1'
);
PREPARE populate_lesson_class_id_stmt FROM @populate_lesson_class_id_sql;
EXECUTE populate_lesson_class_id_stmt;
DEALLOCATE PREPARE populate_lesson_class_id_stmt;

SET @modify_lesson_class_id_sql := IF(
    @has_lesson_class_id = 0,
    'ALTER TABLE lessons MODIFY class_id BIGINT NOT NULL',
    'SELECT 1'
);
PREPARE modify_lesson_class_id_stmt FROM @modify_lesson_class_id_sql;
EXECUTE modify_lesson_class_id_stmt;
DEALLOCATE PREPARE modify_lesson_class_id_stmt;

SET @has_lesson_class_fk := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lessons'
      AND CONSTRAINT_NAME = 'fk_lessons_class'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @add_lesson_class_fk_sql := IF(
    @has_lesson_class_fk = 0,
    'ALTER TABLE lessons ADD CONSTRAINT fk_lessons_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id)',
    'SELECT 1'
);
PREPARE add_lesson_class_fk_stmt FROM @add_lesson_class_fk_sql;
EXECUTE add_lesson_class_fk_stmt;
DEALLOCATE PREPARE add_lesson_class_fk_stmt;
