-- V45: Sửa mẫu nhắc buổi học (LESSON_REMINDER).
--  - Gia sư nhận "buổi dạy", người học nhận "buổi học" ({{activity}}); buổi không có giờ thì bỏ phần "lúc …" ({{timePart}}).
--  - Thông báo cũ của lớp trung tâm bị lộ nguyên {{subjectName}} (bộ nhắc "ngày mai" gọi nhầm mẫu này) -> thay bằng câu chung.
UPDATE notification_templates
SET title_template   = 'Nhắc nhở buổi {{activity}} hôm nay',
    content_template = 'Hôm nay bạn có buổi {{activity}} môn {{subjectName}}{{timePart}} (lớp "{{classTitle}}"). Vui lòng chuẩn bị và tham gia đúng giờ.'
WHERE code = 'LESSON_REMINDER';

UPDATE notifications
SET title   = 'Nhắc buổi học ngày mai',
    content = 'Lớp của bạn có buổi học vào ngày mai. Vui lòng chuẩn bị và tham gia đúng giờ.'
WHERE content LIKE '%{{subjectName}}%';
