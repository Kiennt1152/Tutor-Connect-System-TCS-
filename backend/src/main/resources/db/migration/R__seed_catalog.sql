-- =====================================================================
-- Seed danh mục cơ bản (môn học / khối lớp / tỉnh thành / địa điểm) để
-- các form tìm gia sư & đăng lớp có dữ liệu dropdown.
-- Repeatable migration + INSERT IGNORE => an toàn khi chạy lại nhiều lần,
-- không trùng lặp (dựa trên các khóa UNIQUE: subject_name, grade_name,
-- province_name, google_place_id).
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

-- Tỉnh / thành phố (34 đơn vị hành chính sau sáp nhập, hiệu lực 1/7/2025) ---
--   6 thành phố trực thuộc TW + 28 tỉnh.
INSERT IGNORE INTO provinces (province_name) VALUES
    -- 6 thành phố trực thuộc trung ương
    ('Hà Nội'),
    ('Hải Phòng'),
    ('Huế'),
    ('Đà Nẵng'),
    ('TP Hồ Chí Minh'),
    ('Cần Thơ'),
    -- 28 tỉnh
    ('Tuyên Quang'),
    ('Lào Cai'),
    ('Thái Nguyên'),
    ('Phú Thọ'),
    ('Bắc Ninh'),
    ('Hưng Yên'),
    ('Ninh Bình'),
    ('Quảng Ninh'),
    ('Lạng Sơn'),
    ('Cao Bằng'),
    ('Lai Châu'),
    ('Điện Biên'),
    ('Sơn La'),
    ('Thanh Hóa'),
    ('Nghệ An'),
    ('Hà Tĩnh'),
    ('Quảng Trị'),
    ('Quảng Ngãi'),
    ('Gia Lai'),
    ('Đắk Lắk'),
    ('Khánh Hòa'),
    ('Lâm Đồng'),
    ('Đồng Nai'),
    ('Tây Ninh'),
    ('Vĩnh Long'),
    ('Đồng Tháp'),
    ('An Giang'),
    ('Cà Mau');

-- Địa điểm (quận/huyện tiêu biểu) -------------------------------------
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hn-hoankiem', 'Quận Hoàn Kiếm, Hà Nội', 'Quận Hoàn Kiếm', p.province_id
FROM provinces p WHERE p.province_name = 'Hà Nội';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hn-caugiay', 'Quận Cầu Giấy, Hà Nội', 'Quận Cầu Giấy', p.province_id
FROM provinces p WHERE p.province_name = 'Hà Nội';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hn-dongda', 'Quận Đống Đa, Hà Nội', 'Quận Đống Đa', p.province_id
FROM provinces p WHERE p.province_name = 'Hà Nội';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hn-thanhxuan', 'Quận Thanh Xuân, Hà Nội', 'Quận Thanh Xuân', p.province_id
FROM provinces p WHERE p.province_name = 'Hà Nội';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hcm-q1', 'Quận 1, TP Hồ Chí Minh', 'Quận 1', p.province_id
FROM provinces p WHERE p.province_name = 'TP Hồ Chí Minh';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hcm-q3', 'Quận 3, TP Hồ Chí Minh', 'Quận 3', p.province_id
FROM provinces p WHERE p.province_name = 'TP Hồ Chí Minh';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-hcm-binhthanh', 'Quận Bình Thạnh, TP Hồ Chí Minh', 'Quận Bình Thạnh', p.province_id
FROM provinces p WHERE p.province_name = 'TP Hồ Chí Minh';
INSERT IGNORE INTO locations (google_place_id, address_line, district_name, province_id)
SELECT 'seed-dn-haichau', 'Quận Hải Châu, Đà Nẵng', 'Quận Hải Châu', p.province_id
FROM provinces p WHERE p.province_name = 'Đà Nẵng';
