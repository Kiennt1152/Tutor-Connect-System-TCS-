-- ============================================================
-- V3 — Dữ liệu nền (reference data) chạy một lần qua Flyway.
--   PK (UUID) tự sinh nhờ DEFAULT (UUID()) nên không cần khai báo.
-- ============================================================

-- 1) Roles (khớp CHECK của bảng roles)
INSERT INTO roles (role_name, description) VALUES
    ('ADMIN',        'Quản trị viên nền tảng'),
    ('CLIENT',       'Học viên / Phụ huynh'),
    ('TUTOR',        'Gia sư cá nhân'),
    ('TUTOR_CENTER', 'Trung tâm gia sư');

-- 2) Grades (khối lớp)
INSERT INTO grades (grade_name) VALUES
    ('Grade 1'),  ('Grade 2'),  ('Grade 3'),  ('Grade 4'),
    ('Grade 5'),  ('Grade 6'),  ('Grade 7'),  ('Grade 8'),
    ('Grade 9'),  ('Grade 10'), ('Grade 11'), ('Grade 12'),
    ('University');

-- 3) Provinces (một số tỉnh/thành lớn — bổ sung thêm khi cần)
INSERT INTO provinces (province_name) VALUES
    ('Hà Nội'),
    ('TP. Hồ Chí Minh'),
    ('Đà Nẵng'),
    ('Hải Phòng'),
    ('Cần Thơ'),
    ('Thừa Thiên Huế'),
    ('Khánh Hòa'),
    ('Lâm Đồng'),
    ('Quảng Ninh'),
    ('Bình Dương'),
    ('Đồng Nai'),
    ('Bà Rịa - Vũng Tàu');

-- 4) System settings mặc định (tham chiếu Business Rules trong SRS)
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('platform_name',              'Tutor Connect System', 'Tên hiển thị của nền tảng'),
    ('support_email',              'support@tcs.local',    'Email hỗ trợ người dùng'),
    ('commission_rate_percent',    '10',                   'Phí hoa hồng nền tảng (%) — BR-12'),
    ('escrow_auto_release_hours',  '4',                    'Tự giải ngân sau X giờ nếu không tranh chấp — BR-11'),
    ('dispute_window_hours',       '48',                   'Thời hạn mở tranh chấp sau buổi học — BR-18'),
    ('max_pending_applications',   '5',                    'Số đơn ứng tuyển chờ tối đa của 1 gia sư — BR-08'),
    ('request_expiry_days',        '30',                   'Số ngày trước khi yêu cầu/lớp hết hạn — BR-07'),
    ('verification_sla_hours',     '48',                   'Thời hạn admin duyệt hồ sơ — BR-06'),
    ('session_timeout_minutes',    '30',                   'Thời gian hết hạn phiên đăng nhập');
