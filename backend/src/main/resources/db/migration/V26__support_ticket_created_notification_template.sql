INSERT IGNORE INTO notification_templates
    (code, title_template, content_template, channel, enabled, description)
VALUES
    ('SUPPORT_TICKET_CREATED',
     'Yêu cầu hỗ trợ mới #{{ticketId}}',
     '{{userEmail}} vừa tạo ticket "{{subject}}". Mức ưu tiên: {{priority}}.',
     'IN_APP', TRUE, 'Thông báo cho Platform Admin khi người dùng tạo yêu cầu hỗ trợ mới.');
