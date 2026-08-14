-- UC "Xác nhận lớp đã hoàn thành": mốc thời gian gia sư yêu cầu và client xác nhận hoàn thành.
-- Idempotent: một số DB dev đã có sẵn 2 cột này (do va chạm version migration khi merge main),
-- nên chỉ thêm cột khi chưa tồn tại để tránh lỗi "Duplicate column".

SET @exists_tutor := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
      AND COLUMN_NAME = 'tutor_completed_at');
SET @sql := IF(@exists_tutor = 0,
    'ALTER TABLE class_assignments ADD COLUMN tutor_completed_at DATETIME NULL AFTER client_signed_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists_client := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_assignments'
      AND COLUMN_NAME = 'client_completed_at');
SET @sql := IF(@exists_client = 0,
    'ALTER TABLE class_assignments ADD COLUMN client_completed_at DATETIME NULL AFTER tutor_completed_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
