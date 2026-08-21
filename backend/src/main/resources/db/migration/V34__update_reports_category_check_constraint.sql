-- Cập nhật check constraint cho bảng reports để hỗ trợ đầy đủ các giá trị enum ReportCategory (FRAUD, ABUSE, SPAM, INAPPROPRIATE, PLATFORM_CIRCUMVENTION, OTHER).
-- Script idempotent: Kiểm tra và xóa check constraint cũ nếu tồn tại, sau đó thêm lại constraint mới.

SET @constraint_exists := (
    SELECT COUNT(*)
    FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'chk_reports_category'
);

SET @sql_drop := IF(@constraint_exists > 0,
    'ALTER TABLE reports DROP CHECK chk_reports_category',
    'SELECT 1');

PREPARE stmt_drop FROM @sql_drop;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;

ALTER TABLE reports
    ADD CONSTRAINT chk_reports_category
    CHECK (category IN ('FRAUD', 'ABUSE', 'SPAM', 'INAPPROPRIATE', 'PLATFORM_CIRCUMVENTION', 'OTHER'));
