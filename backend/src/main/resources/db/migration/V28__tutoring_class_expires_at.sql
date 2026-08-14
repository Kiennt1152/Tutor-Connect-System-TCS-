-- V28: Thời gian hiển thị lớp (30 ngày) cho lớp OPEN chưa ký hợp đồng.
-- Cột expires_at = thời điểm đăng lớp + 30 ngày; job dọn dẹp sẽ hard-delete lớp OPEN quá hạn.

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tutoring_classes'
      AND COLUMN_NAME = 'expires_at'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN expires_at DATETIME NULL AFTER updated_at',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill cho các lớp OPEN đang tồn tại: hạn = ngày tạo + 30 ngày.
UPDATE tutoring_classes
SET expires_at = DATE_ADD(created_at, INTERVAL 30 DAY)
WHERE status = 'OPEN' AND expires_at IS NULL;
