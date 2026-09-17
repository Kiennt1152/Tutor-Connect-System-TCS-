-- V41: Hạn 48 giờ ký hợp đồng + chuyển tiền sau khi client chọn gia sư.
--
-- Chọn gia sư KHÔNG còn loại các ứng viên khác: họ nằm ở "danh sách chờ" (giữ nguyên
-- SUBMITTED). Hết 48 giờ mà chưa ký đủ hai bên + chưa có tiền vào escrow thì lớp tự mở
-- lại (OPEN) cho đúng nhóm ứng viên đã nộp đơn từ trước.
--
-- pre_match_* là ảnh chụp học phí/details của lớp TRƯỚC khi áp giá của gia sư được chọn,
-- để khi hủy ghép còn trả lớp về đúng nội dung mà các ứng viên kia đã ứng tuyển.

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tutoring_classes'
      AND COLUMN_NAME = 'match_deadline_at');
SET @ddl := IF(@col = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN match_deadline_at DATETIME NULL AFTER expires_at',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tutoring_classes'
      AND COLUMN_NAME = 'pre_match_details_json');
SET @ddl := IF(@col = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN pre_match_details_json TEXT NULL AFTER match_deadline_at',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tutoring_classes'
      AND COLUMN_NAME = 'pre_match_tuition_fee');
SET @ddl := IF(@col = 0,
    'ALTER TABLE tutoring_classes ADD COLUMN pre_match_tuition_fee DECIMAL(12,2) NULL AFTER pre_match_details_json',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tutoring_classes'
      AND INDEX_NAME = 'idx_class_match_deadline');
SET @ddl := IF(@idx = 0,
    'CREATE INDEX idx_class_match_deadline ON tutoring_classes(status, match_deadline_at)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Lớp đang MATCHED sẵn có: tính hạn từ lúc phân công được tạo.
UPDATE tutoring_classes c
JOIN (
    SELECT t.class_id, MAX(a.assigned_date) AS assigned_date
    FROM class_assignments a
    JOIN tutor_applications t ON t.application_id = a.application_id
    WHERE a.status = 'PENDING'
    GROUP BY t.class_id
) x ON x.class_id = c.class_id
SET c.match_deadline_at = DATE_ADD(x.assigned_date, INTERVAL 48 HOUR)
WHERE c.status = 'MATCHED' AND c.match_deadline_at IS NULL;
