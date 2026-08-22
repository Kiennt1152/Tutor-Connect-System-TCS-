INSERT IGNORE INTO notification_templates
    (code, title_template, content_template, channel, enabled, description)
VALUES
    ('SUPPORT_TICKET_REOPENED',
     'Yêu cầu hỗ trợ #{{ticketId}} đã được mở lại',
     '{{userEmail}} đã mở lại ticket "{{subject}}".',
     'IN_APP', TRUE, 'Thông báo cho Platform Admin khi người dùng mở lại ticket hỗ trợ đã đóng/giải quyết.'),
    ('SUPPORT_TICKET_REOPENED_USER',
     'Yêu cầu hỗ trợ #{{ticketId}} đã mở lại thành công',
     'Yêu cầu hỗ trợ #{{ticketId}} "{{subject}}" của bạn đã được mở lại. Đội ngũ quản trị viên sẽ sớm phản hồi.',
     'IN_APP', TRUE, 'Thông báo xác nhận cho người dùng khi mở lại ticket hỗ trợ thành công.');
