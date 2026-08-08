-- Restore tutoring class description fields introduced after V8 was applied.
SET @schema_name := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tutoring_classes' AND column_name = 'learning_goal') = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN learning_goal VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tutoring_classes' AND column_name = 'tutor_requirement') = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN tutor_requirement VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'tutoring_classes' AND column_name = 'address') = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN address VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
