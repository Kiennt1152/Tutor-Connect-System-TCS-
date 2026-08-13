SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification_templates'
      AND column_name = 'enabled'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE notification_templates ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification_templates'
      AND column_name = 'description'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE notification_templates ADD COLUMN description VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'notification_templates'
      AND column_name = 'updated_at'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE notification_templates ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO notification_templates
    (code, title_template, content_template, channel, enabled, description)
VALUES
    ('PENALTY_ISSUED',
     'Tài khoản nhận hình thức xử lý',
     'Hình thức: {{penaltyType}}. Lý do: {{reason}}. Hiệu lực đến: {{expiresAt}}.',
     'IN_APP', TRUE, 'Thông báo khi Platform Admin áp dụng penalty.'),
    ('REPORT_CREATED',
     'Báo cáo mới cần kiểm duyệt',
     'Có báo cáo mới về {{targetType}} (lý do: {{category}}). Mở hàng đợi báo cáo để kiểm tra.',
     'IN_APP', TRUE, 'Thông báo cho Admin khi có báo cáo USER, REVIEW hoặc CLASS mới.'),
    ('REPORT_RESOLVED',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo cho người báo cáo khi Admin hoàn tất xử lý.'),
    ('SUPPORT_TICKET_RESPONSE',
     'Phản hồi yêu cầu hỗ trợ #{{ticketId}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo khi Admin phản hồi ticket hỗ trợ.'),
    ('VERIFICATION_APPROVED',
     'Hồ sơ xác minh được duyệt',
     'Hồ sơ xác minh của bạn đã được duyệt. Các quyền yêu cầu xác minh đã được mở.',
     'IN_APP', TRUE, 'Thông báo kết quả xác minh thành công.'),
    ('VERIFICATION_REJECTED',
     'Hồ sơ xác minh bị từ chối',
     'Lý do: {{reason}}. Bạn có thể nộp lại sau khi bổ sung giấy tờ.',
     'IN_APP', TRUE, 'Thông báo kết quả xác minh bị từ chối.'),
    ('MARKETPLACE_NEW_APPLICATION',
     'Có gia sư ứng tuyển',
     '{{tutorName}} vừa ứng tuyển vào lớp "{{classTitle}}". Xem chi tiết để chọn gia sư.',
     'IN_APP', TRUE, 'Thông báo cho người đăng lớp khi có gia sư ứng tuyển.'),
    ('MARKETPLACE_TUTOR_INVITED',
     'Bạn được mời nhận lớp',
     'Bạn được chọn cho lớp "{{classTitle}}". Mở lịch dạy để nhận lớp.',
     'IN_APP', TRUE, 'Thông báo cho gia sư được chọn.'),
    ('MARKETPLACE_APPLICATION_REJECTED',
     'Đơn ứng tuyển không được chọn',
     'Đơn ứng tuyển lớp "{{classTitle}}" không được chọn. Lý do: {{reason}}.',
     'IN_APP', TRUE, 'Thông báo kết quả đơn ứng tuyển.'),
    ('MARKETPLACE_CONTRACT_TUTOR_SIGN',
     'Bên A đã ký hợp đồng - mời bạn ký',
     'Hợp đồng lớp "{{classTitle}}" đã được phía phụ huynh/học sinh ký. Vui lòng ký xác nhận.',
     'IN_APP', TRUE, 'Thông báo mời gia sư ký hợp đồng.'),
    ('MARKETPLACE_ESCROW_PAYMENT_READY',
     'Hợp đồng đã hoàn tất - vui lòng thanh toán escrow',
     'Hợp đồng lớp "{{classTitle}}" đã ký xong. Vui lòng mở hợp đồng để thanh toán escrow.',
     'IN_APP', TRUE, 'Thông báo yêu cầu thanh toán escrow.'),
    ('MARKETPLACE_CLASS_EVENT',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Mẫu chung cho thay đổi trạng thái lớp học.'),
    ('CENTER_APPLICATION_RESULT',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo kết quả ứng tuyển vào trung tâm.'),
    ('CENTER_CLASS_REQUEST_CLOSED',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo khi trung tâm đóng yêu cầu tìm gia sư.'),
    ('CHAT_GROUP_MEMBER_ADDED',
     'Bạn đã được thêm vào nhóm {{groupName}}',
     '{{ownerName}} đã thêm bạn vào nhóm chat.',
     'IN_APP', TRUE, 'Thông báo khi người dùng được thêm vào nhóm chat.'),
    ('PAYMENT_ESCROW',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo giải ngân hoặc hoàn tiền escrow.'),
    ('PAYMENT_REFUND_REQUEST',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo hàng đợi chuyển khoản hoàn tiền cho Admin.'),
    ('DISPUTE_EVENT',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo cho các bên khi trạng thái tranh chấp thay đổi.'),
    ('REPORT_EVENT',
     '{{title}}', '{{content}}',
     'IN_APP', TRUE, 'Thông báo cho các bên trong luồng báo cáo lớp học.');
