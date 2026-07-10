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
    ('Giáo dục công dân', NULL);

-- Khối lớp ------------------------------------------------------------
INSERT IGNORE INTO grades (grade_name) VALUES
    ('Lớp 1'), ('Lớp 2'), ('Lớp 3'), ('Lớp 4'), ('Lớp 5'), ('Lớp 6'),
    ('Lớp 7'), ('Lớp 8'), ('Lớp 9'), ('Lớp 10'), ('Lớp 11'), ('Lớp 12'),
    -- Đối tượng luyện thi (không theo khối lớp phổ thông)
    ('Luyện thi chứng chỉ (IELTS, TOEIC...)'),
    ('Luyện thi Đại học');
