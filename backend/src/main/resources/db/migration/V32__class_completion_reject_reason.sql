-- UC "Xác nhận lớp đã hoàn thành": khi client báo "chưa hoàn thành" thì bắt buộc kèm lý do gửi cho gia sư.
-- Idempotent: chỉ thêm cột khi chưa tồn tại.

SET @exists_reason := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
      AND COLUMN_NAME = 'client_reject_reason');
SET @sql := IF(@exists_reason = 0,
    'ALTER TABLE class_assignments ADD COLUMN client_reject_reason VARCHAR(500) NULL AFTER client_completed_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
