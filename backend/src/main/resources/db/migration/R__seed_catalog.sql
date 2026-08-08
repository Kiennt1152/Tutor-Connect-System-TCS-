-- =====================================================================
-- Seed danh mục cơ bản (môn học / khối lớp) cho form.
-- Repeatable migration + INSERT IGNORE => an toàn khi chạy lại nhiều lần.
-- (Tỉnh/Quận-Huyện/Phường-Xã do V11__admin_divisions_new_provinces.sql quản lý.)
-- =====================================================================

SET NAMES utf8mb4;

-- Môn học -------------------------------------------------------------
INSERT IGNORE INTO subjects (subject_name, description) VALUES
    ('Toán', NULL),
    ('Vật lý', NULL),
    ('Hóa học', NULL),
    ('Sinh học', NULL),
    ('Ngữ văn', NULL),
    ('Tiếng Việt', NULL),
    ('Tiếng Anh', NULL),
    ('Lịch sử', NULL),
    ('Địa lý', NULL),
    ('Tin học', NULL),
    ('Thi chứng chỉ (IELTS, TOEIC...)', NULL);

-- Khối lớp ------------------------------------------------------------
INSERT IGNORE INTO grades (grade_name) VALUES
    ('Lớp 1'), ('Lớp 2'), ('Lớp 3'), ('Lớp 4'), ('Lớp 5'), ('Lớp 6'),
    ('Lớp 7'), ('Lớp 8'), ('Lớp 9'), ('Lớp 10'), ('Lớp 11'), ('Lớp 12'),
    -- Đối tượng luyện thi (không theo khối lớp phổ thông)
    ('Luyện thi chứng chỉ (IELTS, TOEIC...)'),
    ('Luyện thi Đại học');

-- Mẫu hợp đồng mặc định (hệ thống: center_id = NULL) -------------------
-- created_by lấy user đầu tiên có trong hệ thống; chỉ chèn khi CHƯA có mẫu cùng tên
-- (idempotent) và khi đã có ít nhất 1 user.
INSERT INTO contract_templates (name, content, created_by, center_id, is_default, status, created_at, updated_at)
SELECT 'Hợp đồng dạy học theo lớp (mặc định)',
       'HỢP ĐỒNG DẠY HỌC\n\nĐiều 1: Gia sư/Trung tâm cam kết giảng dạy đúng nội dung, lịch học đã thống nhất.\nĐiều 2: Học viên/Phụ huynh cam kết tham gia đầy đủ và thanh toán học phí đúng hạn.\nĐiều 3: Hai bên tuân thủ quy định của nền tảng Tutor Connect System.\nĐiều 4: Hợp đồng có hiệu lực kể từ khi các bên ký xác nhận (OTP).',
       u.user_id, NULL, TRUE, 'ACTIVE', NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM contract_templates ct
                  WHERE ct.name = 'Hợp đồng dạy học theo lớp (mặc định)')
ORDER BY u.user_id LIMIT 1;

INSERT INTO contract_templates (name, content, created_by, center_id, is_default, status, created_at, updated_at)
SELECT 'Thỏa thuận hợp tác gia sư - trung tâm (mặc định)',
       'THỎA THUẬN HỢP TÁC\n\nĐiều 1: Gia sư đồng ý gia nhập đội ngũ của trung tâm và nhận phân công giảng dạy.\nĐiều 2: Trung tâm cam kết bố trí lớp, hỗ trợ chuyên môn và thanh toán thù lao.\nĐiều 3: Hai bên tuân thủ quy định của nền tảng Tutor Connect System.\nĐiều 4: Thỏa thuận có hiệu lực kể từ khi gia sư ký xác nhận (OTP).',
       u.user_id, NULL, FALSE, 'ACTIVE', NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM contract_templates ct
                  WHERE ct.name = 'Thỏa thuận hợp tác gia sư - trung tâm (mặc định)')
ORDER BY u.user_id LIMIT 1;
