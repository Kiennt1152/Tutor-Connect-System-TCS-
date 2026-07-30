
SET @has_assignment_idx := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reviews'
      AND INDEX_NAME = 'idx_reviews_assignment'
);
SET @add_idx_sql := IF(
    @has_assignment_idx = 0,
    'CREATE INDEX idx_reviews_assignment ON reviews (assignment_id)',
    'SELECT 1'
);
PREPARE add_idx_stmt FROM @add_idx_sql;
EXECUTE add_idx_stmt;
DEALLOCATE PREPARE add_idx_stmt;

SET @has_review_uq := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reviews'
      AND CONSTRAINT_NAME = 'uq_reviews_assignment_reviewer_type'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
SET @drop_review_uq_sql := IF(
    @has_review_uq = 1,
    'ALTER TABLE reviews DROP INDEX uq_reviews_assignment_reviewer_type',
    'SELECT 1'
);
PREPARE drop_review_uq_stmt FROM @drop_review_uq_sql;
EXECUTE drop_review_uq_stmt;
DEALLOCATE PREPARE drop_review_uq_stmt;
