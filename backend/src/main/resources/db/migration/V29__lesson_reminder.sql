-- V29: Nhắc nhở buổi học trong ngày.
-- Thêm cột reminder_sent_at (chống gửi trùng) + template thông báo LESSON_REMINDER.

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lessons'
      AND COLUMN_NAME = 'reminder_sent_at'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE lessons ADD COLUMN reminder_sent_at DATETIME NULL AFTER created_at',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO notification_templates
    (code, title_template, content_template, channel, enabled, description)
VALUES
    ('LESSON_REMINDER',
     'Nhắc nhở buổi học hôm nay',
     'Hôm nay bạn có buổi học môn {{subjectName}} lúc {{startTime}} - {{endTime}} (lớp "{{classTitle}}"). Vui lòng chuẩn bị và tham gia đúng giờ.',
     'IN_APP', TRUE, 'Nhắc nhở tự động cho gia sư và người học vào ngày có buổi học.');
