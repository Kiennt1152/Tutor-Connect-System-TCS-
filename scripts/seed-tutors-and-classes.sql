-- =====================================================================
-- SEED SCRIPT: 20 Hồ sơ Gia sư (Đầy đủ môn) & 30 Lớp học mở (Tutor Connect System)
-- Sử dụng: Chạy trực tiếp trong MySQL Workbench / DBeaver / CLI MySQL:
--   mysql -u root -p tutorconnectsystem < scripts/seed-tutors-and-classes.sql
-- =====================================================================

USE tutorconnectsystem;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Đảm bảo có đầy đủ các môn học trong bảng subjects
INSERT IGNORE INTO subjects (subject_name, description) VALUES
    ('Toán', 'Môn Toán các cấp, luyện thi vào 10 và THPT Quốc gia'),
    ('Vật lý', 'Môn Vật lý THCS, THPT và luyện thi Đại học'),
    ('Hóa học', 'Môn Hóa học THCS, THPT và bồi dưỡng HSG'),
    ('Sinh học', 'Môn Sinh học ôn thi THPTQG và khối B Y Dược'),
    ('Ngữ văn', 'Môn Ngữ văn, rèn kỹ năng viết và cảm thụ văn học'),
    ('Tiếng Việt', 'Môn Tiếng Việt tiểu học, rèn chữ đẹp, tập đọc'),
    ('Tiếng Anh', 'Tiếng Anh phổ thông, giao tiếp và chứng chỉ quốc tế'),
    ('Tiếng Hàn', 'Tiếng Hàn giao tiếp, du học và luyện thi TOPIK'),
    ('Tiếng Trung', 'Tiếng Trung giao tiếp thương mại và luyện thi HSK/TOCFL'),
    ('Tin học', 'Tin học văn phòng, lập trình Scratch, Python, C++, Web'),
    ('Lịch sử', 'Môn Lịch sử ôn thi vào 10 và THPTQG'),
    ('Địa lý', 'Môn Địa lý ôn thi vào 10 và THPTQG'),
    ('Thi chứng chỉ (IELTS, TOEIC...)', 'Luyện thi IELTS, TOEIC, TOEFL quốc tế');

-- 2. Tạo 20 Tài khoản User cho Gia sư
-- Mật khẩu mặc định: 123@123a (BCrypt: $2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u)
INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
    ('tutor.toan1@tcs.com', '0981000001', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.toan2@tcs.com', '0981000002', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.ly1@tcs.com', '0981000003', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.ly2@tcs.com', '0981000004', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.hoa1@tcs.com', '0981000005', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.hoa2@tcs.com', '0981000006', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.van1@tcs.com', '0981000007', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.van2@tcs.com', '0981000008', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.anh1@tcs.com', '0981000009', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.anh2@tcs.com', '0981000010', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.anh3@tcs.com', '0981000011', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.han1@tcs.com', '0981000012', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.han2@tcs.com', '0981000013', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.trung1@tcs.com', '0981000014', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.trung2@tcs.com', '0981000015', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.tin1@tcs.com', '0981000016', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.tin2@tcs.com', '0981000017', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.tieuhoc1@tcs.com', '0981000018', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.sinh1@tcs.com', '0981000019', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('tutor.sudia1@tcs.com', '0981000020', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW());

-- 3. Tạo 5 Tài khoản Phụ Huynh / Học Viên (Người đăng lớp)
INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
    ('client.hanoi1@tcs.com', '0971000001', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.hanoi2@tcs.com', '0971000002', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.hcm1@tcs.com', '0971000003', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.hcm2@tcs.com', '0971000004', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.danang1@tcs.com', '0971000005', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW());

-- Tạo Profile trong bảng clients
INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Lê Thu Trang', u.phone, 'Số 25 Hoàng Quốc Việt, Cầu Giấy, Hà Nội'
FROM users u WHERE u.email = 'client.hanoi1@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Nguyễn Văn Đức', u.phone, 'Ngõ 119 Tây Sơn, Đống Đa, Hà Nội'
FROM users u WHERE u.email = 'client.hanoi2@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Trần Minh Tâm', u.phone, '120 Nguyễn Thị Minh Khai, Quận 3, TP.HCM'
FROM users u WHERE u.email = 'client.hcm1@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Hoàng Kim Oanh', u.phone, 'Khu đô thị Phú Mỹ Hưng, Quận 7, TP.HCM'
FROM users u WHERE u.email = 'client.hcm2@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Đặng Quốc Bảo', u.phone, '54 Nguyễn Văn Linh, Hải Châu, Đà Nẵng'
FROM users u WHERE u.email = 'client.danang1@tcs.com';

-- 4. Tạo Hồ Sơ Chi Tiết Cho 20 Gia Sư (Bảng tutors)
-- 4.1 Toán 1 (Hà Nội - ĐH Sư Phạm - Cấp 3)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Nguyễn Thành Long', 'MALE', u.phone, 'Cầu Giấy, Hà Nội', 6,
       'Cử nhân Sư phạm Toán ĐH Sư Phạm Hà Nội, 6 năm kinh nghiệm luyện thi vào 10 và THPT Quốc gia điểm 8+, 9+. Chuyên dạy Toán hình, Đại số nâng cao cho học sinh mất gốc và bồi dưỡng HSG cấp tỉnh. Nhận dạy khu vực Cầu Giấy, Nam Từ Liêm và Online.',
       250000.00, 4.95, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.toan1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.2 Toán 2 (TP.HCM - Bách Khoa - Cấp 2 & 3)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Lê Hoàng Nam', 'MALE', u.phone, 'Quận 10, TP.HCM', 4,
       'Tốt nghiệp ĐH Bách Khoa TP.HCM, thủ khoa khối A tỉnh Lâm Đồng. 4 năm kinh nghiệm gia sư Toán 9, Toán 10, 11, 12. Phương pháp giải nhanh trắc nghiệm máy tính Casio, tư duy logic bản chất. Dạy tại Quận 10, Quận 3, Quận 1 và Online.',
       200000.00, 4.85, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.toan2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.3 Vật lý 1 (Hà Nội - KHTN - Luyện thi ĐH)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Vũ Minh Tuấn', 'MALE', u.phone, 'Thanh Xuân, Hà Nội', 5,
       'Cử nhân Vật lý ĐH Khoa học Tự nhiên - ĐHQGHN, Giải Nhì Vật lý Quốc gia. 5 năm dạy Vật lý lớp 10, 11, 12 và luyện thi Đại học khối A. Chuyên đề Dao động cơ, Sóng cơ, Dòng điện xoay chiều, Vật lý hạt nhân. Khu vực Thanh Xuân, Đống Đa, Hà Đông.',
       220000.00, 4.90, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.ly1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.4 Vật lý 2 (TP.HCM - Sư Phạm Lý)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Đặng Thu Thảo', 'FEMALE', u.phone, 'Bình Thạnh, TP.HCM', 3,
       'Cử nhân Sư phạm Vật lý ĐH Sư Phạm TP.HCM, giáo viên nhiệt huyết, kiên nhẫn. 3 năm kèm Vật lý lớp 8, 9, 10, 11 cho học sinh mất căn bản lấy lại điểm 8+. Nhận dạy tại Bình Thạnh, Phú Nhuận, Gò Vấp.',
       180000.00, 4.80, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.ly2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.5 Hóa học 1 (Hà Nội - ĐH Dược)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Phạm Đức Minh', 'MALE', u.phone, 'Hoàn Kiếm, Hà Nội', 7,
       'Thạc sĩ ĐH Dược Hà Nội, cựu chuyên Hóa Amsterdam. 7 năm chuyên luyện thi Hóa THPTQG điểm 9+ xét tuyển Y Đa khoa, Dược. Nắm vững phương pháp bảo toàn electron, bảo toàn khối lượng, bài toán este - peptit nâng cao. Khu vực Hoàn Kiếm, Hai Bà Trưng.',
       300000.00, 4.98, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.hoa1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.6 Hóa học 2 (TP.HCM - KHTN)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Bùi Bích Ngọc', 'FEMALE', u.phone, 'Quận 7, TP.HCM', 4,
       'Cử nhân Hóa học ĐH Khoa học Tự nhiên TP.HCM. Chuyên dạy Hóa THCS lớp 8, 9 ôn thi vào 10 và Hóa 10, 11 cơ bản. Hướng dẫn viết phương trình, giải bài toán vô cơ chi tiết dễ hiểu. Khu vực Quận 7, Quận 4, Nhà Bè.',
       200000.00, 4.75, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.hoa2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.7 Ngữ văn 1 (Hà Nội - Sư Phạm Văn)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Trần Thị Thu Hà', 'FEMALE', u.phone, 'Đống Đa, Hà Nội', 8,
       'Thạc sĩ Văn học ĐH Sư Phạm Hà Nội, 8 năm kinh nghiệm dạy Ngữ văn lớp 9 thi vào 10 và lớp 12 luyện thi tốt nghiệp THPT/Đại học. Rèn kỹ năng viết Nghị luận xã hội sắc bén, phân tích tác phẩm văn học đạt 8.5+. Khu vực Đống Đa, Ba Đình, Cầu Giấy.',
       280000.00, 4.95, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.van1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.8 Ngữ văn 2 (TP.HCM - KHXH&NV)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Nguyễn Mai Phương', 'FEMALE', u.phone, 'Tân Bình, TP.HCM', 5,
       'Cử nhân Văn học ĐH KHXH&NV TP.HCM. 5 năm kèm Văn cấp 2 (lớp 6, 7, 8, 9) và luyện thi vào lớp 10 trường công lập. Phương pháp học Văn qua sơ đồ tư duy, rèn chữ và chính tả. Khu vực Tân Bình, Phú Nhuận, Quận 3.',
       200000.00, 4.80, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.van2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.9 Tiếng Anh 1 - IELTS (Hà Nội - FTU - IELTS 8.0)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Phạm Quỳnh Anh', 'FEMALE', u.phone, 'Cầu Giấy, Hà Nội', 5,
       'Cử nhân Kinh tế đối ngoại ĐH Ngoại Thương, chứng chỉ IELTS 8.0 (Speaking 8.5, Writing 8.0). 5 năm kinh nghiệm luyện thi IELTS Academic mục tiêu 6.5 - 7.5+, củng cố ngữ pháp và phát âm chuẩn bản ngữ. Dạy tại Cầu Giấy, Nam Từ Liêm hoặc Online qua Zoom.',
       350000.00, 5.00, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.anh1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.10 Tiếng Anh 2 - Phổ thông & Chuyển cấp (TP.HCM - Sư Phạm Anh)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Hoàng Trọng Nghĩa', 'MALE', u.phone, 'Quận 1, TP.HCM', 6,
       'Cử nhân Sư phạm Tiếng Anh ĐH Sư Phạm TP.HCM, chứng chỉ C1 CEFR. 6 năm dạy kèm Tiếng Anh tăng cường lớp 6-12, ôn thi vào lớp 10 chuyên Anh và luyện thi THPTQG. Khu vực Quận 1, Quận 3, Quận 4.',
       250000.00, 4.88, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.anh2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.11 Tiếng Anh 3 - Trẻ em Cambridge & Giao tiếp (TP.HCM - TOEIC 950)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Đỗ Ngọc Hân', 'FEMALE', u.phone, 'Quận 7, TP.HCM', 3,
       'Tốt nghiệp ĐH RMIT Việt Nam, TOEIC 950, chứng chỉ giảng dạy quốc tế TESOL. Chuyên dạy Tiếng Anh tiểu học Cambridge (Starters, Movers, Flyers) và tiếng Anh giao tiếp phản xạ cho người mất gốc. Khu vực Quận 7, TP. Thủ Đức và Online.',
       220000.00, 4.82, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.anh3@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.12 Tiếng Hàn 1 - TOPIK II (TP.HCM - KHXH&NV - TOPIK 6)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Nguyễn Minh Thư', 'FEMALE', u.phone, 'Quận 7, TP.HCM', 5,
       'Tốt nghiệp khoa Hàn Quốc học ĐH KHXH&NV TP.HCM, chứng chỉ TOPIK 6 (cấp cao nhất). 5 năm kinh nghiệm dạy Tiếng Hàn sơ cấp, trung cấp, luyện thi TOPIK I & TOPIK II và tiếng Hàn thương mại cho người đi làm. Nhận dạy tại Quận 7, Quận 1 và Online.',
       300000.00, 4.92, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.han1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.13 Tiếng Hàn 2 - Giao tiếp & Du học (Hà Nội - ĐHQGHN - TOPIK 5)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Park Sung-Min', 'MALE', u.phone, 'Cầu Giấy, Hà Nội', 4,
       'Cử nhân Ngôn ngữ Hàn ĐH Ngoại Ngữ - ĐHQGHN, 2 năm trao đổi tại ĐH Yonsei Seoul. 4 năm kinh nghiệm dạy phát âm chuẩn Seoul, giao tiếp thực tế cho người đi làm và phỏng vấn visa du học Hàn Quốc. Khu vực Cầu Giấy, Thanh Xuân.',
       260000.00, 4.85, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.han2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.14 Tiếng Trung 1 - HSK 6 & TOCFL (TP.HCM - ĐH Sư Phạm - HSK 6)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Trần Quốc Huy', 'MALE', u.phone, 'Quận 5, TP.HCM', 6,
       'Cử nhân Sư phạm tiếng Trung ĐH Sư Phạm TP.HCM, chứng chỉ HSK 6 (285/300) và HSKK Cao cấp. 6 năm dạy Tiếng Trung giao tiếp, chữ Hán phồn thể/giản thể, luyện thi HSK 3, 4, 5, 6 và chứng chỉ TOCFL. Dạy tại Quận 5, Quận 10, Quận 11 và Online.',
       300000.00, 4.95, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.trung1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.15 Tiếng Trung 2 - Sơ cấp & Giao tiếp (Hà Nội - ĐH Hà Nội - HSK 5)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Lý Ngọc Diệp', 'FEMALE', u.phone, 'Ba Đình, Hà Nội', 3,
       'Tốt nghiệp khoa Tiếng Trung ĐH Hà Nội (HANU), chứng chỉ HSK 5. 3 năm kèm tiếng Trung cho người mới bắt đầu từ con số 0, tiếng Trung bán hàng, du lịch và order hàng Taobao. Dạy tại Ba Đình, Đống Đa, Hoàn Kiếm.',
       200000.00, 4.78, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.trung2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.16 Tin học 1 - Python & Ôn thi HSG Tin (Hà Nội - ĐH Công Nghệ - UET)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Hoàng Anh Đức', 'MALE', u.phone, 'Cầu Giấy, Hà Nội', 4,
       'Kỹ sư Khoa học máy tính ĐH Công nghệ - ĐHQGHN, Giải Nhì Olympic Tin học Sinh viên. 4 năm kinh nghiệm dạy lập trình Python, C++, Pascal cho học sinh THCS, THPT ôn thi Học sinh giỏi Tin và thuật toán cơ bản. Khu vực Cầu Giấy, Nam Từ Liêm hoặc Online 1-1.',
       300000.00, 4.94, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.tin1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.17 Tin học 2 - Scratch & Lập trình Web cho trẻ em (TP.HCM - Bách Khoa)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Lê Quốc Tuấn', 'MALE', u.phone, 'Thủ Đức, TP.HCM', 3,
       'Kỹ sư Phần mềm ĐH Bách Khoa TP.HCM. 3 năm dạy lập trình Scratch, Game 2D, lập trình Web frontend (HTML, CSS, JavaScript) cho học sinh tiểu học và cấp 2. Phương pháp học qua dự án vui vẻ, sáng tạo. Khu vực TP. Thủ Đức, Bình Thạnh.',
       220000.00, 4.80, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.tin2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.18 Tiếng Việt & Rèn chữ Tiểu học (Hà Nội - Sư Phạm Giáo Dục Tiểu Học)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Nguyễn Thị Lan Anh', 'FEMALE', u.phone, 'Hà Đông, Hà Nội', 6,
       'Cử nhân Giáo dục Tiểu học ĐH Sư Phạm Hà Nội, chứng nhận Giáo viên dạy giỏi cấp Quận. 6 năm chuyên rèn chữ đẹp, tập đọc, chính tả và môn Tiếng Việt cho học sinh chuẩn bị vào lớp 1, lớp 2, 3, 4, 5. Phương pháp sư phạm chuẩn, kiên nhẫn, yêu trẻ. Khu vực Hà Đông, Thanh Xuân.',
       180000.00, 4.96, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.tieuhoc1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.19 Sinh học (TP.HCM - Sư Phạm Sinh - Luyện thi Y Dược)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Võ Trọng Nhân', 'MALE', u.phone, 'Quận 5, TP.HCM', 5,
       'Thạc sĩ Di truyền học ĐH Sư Phạm TP.HCM. 5 năm chuyên luyện thi môn Sinh học THPTQG xét tuyển khối B (Y Đa khoa, Răng Hàm Mặt, Dược). Chuyên sâu Di truyền học phân tử, Di truyền quần thể, Phả hệ, Sinh thái học. Khu vực Quận 5, Quận 10, Tân Bình.',
       250000.00, 4.89, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.sinh1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.20 Lịch sử & Địa lý (Hà Nội - ĐH Sư Phạm)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Đặng Thùy Dung', 'FEMALE', u.phone, 'Hai Bà Trưng, Hà Nội', 4,
       'Cử nhân Sư phạm Lịch sử & Địa lý ĐH Sư Phạm Hà Nội. 4 năm kèm môn Lịch sử và Địa lý lớp 9 thi vào 10, lớp 12 ôn thi khối C (C00) đạt điểm 9+. Phương pháp ghi nhớ mốc lịch sử qua sơ đồ thời gian, đọc Atlat Địa lý thành thạo. Khu vực Hai Bà Trưng, Hoàn Kiếm.',
       180000.00, 4.85, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.sudia1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 5. Tạo 30 Lớp Học Mở (tutoring_classes, status = 'OPEN')

-- 5.1 Lớp Toán 12 Luyện thi THPTQG (Hà Nội - Cầu Giấy)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Toán 12 kèm 1-1 luyện thi Đại học điểm 9+',
       'Cần tìm thầy/cô kèm môn Toán cho học sinh lớp 12 trường THPT Cầu Giấy. Mục tiêu xét tuyển ĐH Ngoại Thương khối A00. Yêu cầu nắm chắc chuyên đề Hàm số, Oxyz, Số phức và Tích phân nâng cao.',
       'Đạt điểm 9+ môn Toán THPTQG', 'Gia sư Sư phạm hoặc Bách Khoa có kinh nghiệm ôn thi ĐH',
       'Số 18 Duy Tân, Cầu Giấy, Hà Nội', 'OFFLINE', 15, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 60 DAY,
       3750000.00, 4000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Toán 12 kèm 1-1 luyện thi Đại học điểm 9+');

-- 5.2 Lớp Toán 9 Ôn thi vào lớp 10 công lập (TP.HCM - Quận 3)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần gia sư Toán lớp 9 ôn thi tuyển sinh vào lớp 10',
       'Bé hiện học lực khá nhưng yếu phần Hình học không gian và bài toán thực tế. Cần gia sư dạy 2 buổi/tuần vào thứ 3 và thứ 6 lúc 18h30.',
       'Đỗ nguyện vọng 1 THPT Nguyễn Thị Minh Khai', 'Sinh viên Bách Khoa/Sư phạm hoặc Giáo viên kiên nhẫn',
       '120 Nguyễn Thị Minh Khai, Quận 3, TP.HCM', 'OFFLINE', 12, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 50 DAY,
       2400000.00, 2600000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 9'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần gia sư Toán lớp 9 ôn thi tuyển sinh vào lớp 10');

-- 5.3 Lớp Tiếng Anh IELTS Academic 6.5 -> 7.5 (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Lớp kèm 1-1 IELTS Academic nâng band Speaking & Writing lên 7.5',
       'Học viên là sinh viên năm 3 cần bằng IELTS để apply học bổng du học. Hiện band 6.5, cần sửa bài Writing Task 1 & 2 chuyên sâu và luyện phản xạ Speaking các chủ đề khó.',
       'IELTS Overall 7.5 (Speaking 7.5, Writing 7.0)', 'Gia sư IELTS 8.0+ có kinh nghiệm sửa bài chi tiết',
       'Học trực tuyến qua Zoom/Google Meet', 'ONLINE', 16, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 45 DAY,
       5600000.00, 6000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Tiếng Anh' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Lớp kèm 1-1 IELTS Academic nâng band Speaking & Writing lên 7.5');

-- 5.4 Lớp Tiếng Hàn TOPIK II cấp 4 (TP.HCM - Quận 7)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần tìm gia sư dạy Tiếng Hàn ôn thi TOPIK II cấp 4',
       'Đã hoàn thành sơ cấp TOPIK 2, cần học lên trung cấp để thi TOPIK 4 phục vụ công việc tại công ty Hàn Quốc. Cần gia sư dạy chắc ngữ pháp trung cấp và kỹ năng viết câu 53, 54.',
       'Đạt chứng chỉ TOPIK cấp 4 trong 4 tháng', 'Gia sư tốt nghiệp khoa Tiếng Hàn hoặc chứng chỉ TOPIK 5, 6',
       'Chung cư Sunrise City, Nguyễn Hữu Thọ, Quận 7, TP.HCM', 'OFFLINE', 20, CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 70 DAY,
       6000000.00, 6500000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm2@tcs.com' AND s.subject_name = 'Tiếng Hàn' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần tìm gia sư dạy Tiếng Hàn ôn thi TOPIK II cấp 4');

-- 5.5 Lớp Tiếng Trung HSK 4 cấp tốc cho người đi làm (Hà Nội - Đống Đa)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Tiếng Trung giao tiếp và luyện thi HSK 4',
       'Học viên đi làm ngành xuất nhập khẩu cần nâng cao vốn từ vựng thương mại, giao tiếp tự tin với đối tác Trung Quốc và thi chứng chỉ HSK 4.',
       'Giao tiếp lưu loát và đỗ HSK 4', 'Gia sư HSK 5-6 phát âm chuẩn Bắc Kinh',
       'Ngõ 119 Tây Sơn, Đống Đa, Hà Nội', 'HYBRID', 18, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 60 DAY,
       4500000.00, 5000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Tiếng Trung' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Tiếng Trung giao tiếp và luyện thi HSK 4');

-- 5.6 Lớp Lập trình Python & C++ ôn thi HSG Tin (Hà Nội - Nam Từ Liêm)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư kèm Lập trình Python & Thuật toán C++ ôn thi HSG',
       'Học sinh lớp 8 trường Chuyên Amsterdam đam mê Tin học, cần học cấu trúc dữ liệu và giải thuật (Quy hoạch động, Đồ thị, Cây) để thi Học sinh giỏi Tin cấp Thành phố.',
       'Đạt giải HSG môn Tin học', 'Sinh viên ngành CNTT ĐH Công nghệ hoặc Bách Khoa có giải Quốc gia',
       'KĐT Mỹ Đình 2, Nam Từ Liêm, Hà Nội', 'OFFLINE', 16, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 50 DAY,
       4800000.00, 5000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Tin học' AND g.grade_name = 'Lớp 8'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư kèm Lập trình Python & Thuật toán C++ ôn thi HSG');

-- 5.7 Lớp Lập trình Scratch cho trẻ em (TP.HCM - Thủ Đức - Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Lớp Lập trình Scratch sáng tạo Game 2D cho học sinh lớp 4',
       'Bé thích chơi game và máy tính, phụ huynh muốn định hướng tư duy logic lập trình từ nhỏ thông qua ngôn ngữ kéo thả Scratch 3.0.',
       'Tự lập trình được 3 game mini', 'Gia sư trẻ trung, nhiệt tình, có kỹ năng sư phạm dạy trẻ em',
       'Học trực tuyến qua Google Meet', 'ONLINE', 10, CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 30 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Tin học' AND g.grade_name = 'Lớp 4'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Lớp Lập trình Scratch sáng tạo Game 2D cho học sinh lớp 4');

-- 5.8 Lớp Ngữ văn 12 Luyện viết Nghị luận văn học (Hà Nội - Ba Đình)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm cô giáo kèm Ngữ văn 12 ôn thi Tốt nghiệp THPT và Đại học',
       'Học sinh học khối D, hiện môn Văn điểm trung bình 6.5. Cần cô giáo rèn phương pháp mở bài, kết bài sáng tạo và luận điểm sâu sắc cho các tác phẩm trọng tâm (Vợ chồng A Phủ, Người lái đò sông Đà, Đất Nước).',
       'Đạt 8.5+ môn Văn thi Đại học', 'Cô giáo hoặc Thạc sĩ Sư phạm Ngữ văn',
       'Số 45 Kim Mã, Ba Đình, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 45 DAY,
       3000000.00, 3200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Ngữ văn' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm cô giáo kèm Ngữ văn 12 ôn thi Tốt nghiệp THPT và Đại học');

-- 5.9 Lớp Vật lý 11 - Điện từ học & Quang hình (TP.HCM - Bình Thạnh)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần gia sư Vật lý lớp 11 kèm tại nhà 2 buổi/tuần',
       'Học sinh cần củng cố kiến thức học kỳ 1 môn Vật lý 11 chương Điện từ học và Quang hình học để chuẩn bị cho kỳ thi học kỳ và định hướng thi khối A1.',
       'Nắm chắc lý thuyết và giải thành thạo bài tập 8+', 'Sinh viên giỏi Sư phạm Lý hoặc Bách Khoa',
       'Đường D2 (Nguyễn Gia Trí), Bình Thạnh, TP.HCM', 'OFFLINE', 14, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 50 DAY,
       2800000.00, 3000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Vật lý' AND g.grade_name = 'Lớp 11'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần gia sư Vật lý lớp 11 kèm tại nhà 2 buổi/tuần');

-- 5.10 Lớp Hóa học 10 - Bảng tuần hoàn & Oxi hóa khử (Hà Nội - Thanh Xuân)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Hóa học 10 cho học sinh mới vào cấp 3',
       'Em chuyển cấp vào lớp 10 học chương trình GDPT mới còn bỡ ngỡ, cần gia sư dạy chắc lý thuyết cấu tạo nguyên tử, bảng tuần hoàn và phản ứng oxi hóa khử.',
       'Điểm trung bình môn Hóa đạt 8.0 trở lên', 'Gia sư chuyên Hóa có phương pháp giảng giải trực quan',
       'Khu tập thể Thanh Xuân Bắc, Thanh Xuân, Hà Nội', 'OFFLINE', 10, CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 40 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Hóa học' AND g.grade_name = 'Lớp 10'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Hóa học 10 cho học sinh mới vào cấp 3');

-- 5.11 Lớp Tiếng Việt & Rèn chữ Lớp 1 (Hà Nội - Hà Đông)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm cô giáo rèn chữ đẹp và đánh vần Tiếng Việt cho bé 6 tuổi vào lớp 1',
       'Bé năm nay vào lớp 1 trường Tiểu học Lê Quý Đôn. Cần cô giáo kiên nhẫn dạy bé cầm bút đúng tư thế, rèn nét chữ và ghép vần tiếng Việt cơ bản.',
       'Biết đọc trôi chảy và viết đúng ô ly', 'Ưu tiên cô giáo khoa Giáo dục Tiểu học ĐH Sư Phạm',
       'KĐT Văn Phú, Hà Đông, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 40 DAY,
       2160000.00, 2400000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Tiếng Việt' AND g.grade_name = 'Lớp 1'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm cô giáo rèn chữ đẹp và đánh vần Tiếng Việt cho bé 6 tuổi vào lớp 1');

-- 5.12 Lớp Sinh học 12 Ôn thi Y Dược (TP.HCM - Quận 5)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Sinh học 12 chuyên sâu ôn thi Y Dược TP.HCM điểm 9+',
       'Học sinh đặt mục tiêu thi ĐH Y Dược TP.HCM ngành Y Đa khoa. Đã có nền tảng cơ bản, cần thầy/cô kèm bài tập Di truyền học nâng cao, phả hệ phức tạp và sinh thái học.',
       'Đạt 9.2+ môn Sinh học THPTQG', 'Thầy/Cô Sư phạm Sinh hoặc Bác sĩ nội trú',
       'Gần ĐH Sư Phạm, Quận 5, TP.HCM', 'OFFLINE', 15, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 50 DAY,
       3750000.00, 4000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm2@tcs.com' AND s.subject_name = 'Sinh học' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Sinh học 12 chuyên sâu ôn thi Y Dược TP.HCM điểm 9+');

-- 5.13 Lớp Tiếng Anh Giao Tiếp Người Đi Làm (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Lớp Tiếng Anh giao tiếp phản xạ cho người đi làm (1 kèm 1 Online)',
       'Học viên đi làm ngành Marketing cần cải thiện phát âm, tự tin thuyết trình tiếng Anh và trao đổi email với sếp nước ngoài. Học vào các buổi tối 20h - 21h30.',
       'Phản xạ tiếng Anh trôi chảy sau 2 tháng', 'Gia sư phát âm chuẩn Anh/Mỹ, nhiệt tình sửa lỗi',
       'Học qua Zoom', 'ONLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 40 DAY,
       2880000.00, 3000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.danang1@tcs.com' AND s.subject_name = 'Tiếng Anh' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Lớp Tiếng Anh giao tiếp phản xạ cho người đi làm (1 kèm 1 Online)');

-- 5.14 Lớp Toán 6 Chuyển cấp THCS (Đà Nẵng - Hải Châu)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Toán lớp 6 kèm tại nhà khu vực Hải Châu Đà Nẵng',
       'Bé vừa vào lớp 6 chương trình Cánh Diều, cần gia sư hướng dẫn phương pháp tự học Số học (Tập hợp, Số nguyên tố, Ước chung) và Hình học trực quan.',
       'Nắm chắc kiến thức đạt điểm 8+', 'Sinh viên Sư phạm Toán hoặc Bách Khoa Đà Nẵng',
       '54 Nguyễn Văn Linh, Hải Châu, Đà Nẵng', 'OFFLINE', 10, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 35 DAY,
       1800000.00, 2000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.danang1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 6'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Toán lớp 6 kèm tại nhà khu vực Hải Châu Đà Nẵng');

-- 5.15 Lớp Lập trình Web Fullstack Java & React (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Kèm 1-1 Lập trình Web fullstack Java Spring Boot và ReactJS',
       'Sinh viên năm 2 ngành CNTT cần gia sư hướng dẫn thực chiến xây dựng hệ thống REST API với Spring Boot, MySQL, Spring Security JWT và giao diện React/Vite.',
       'Hoàn thành 1 project thực tế để đi xin việc thực tập', 'Senior Software Engineer hoặc Mentor có kinh nghiệm',
       'Học Online qua Discord / Google Meet', 'ONLINE', 15, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 45 DAY,
       5250000.00, 6000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Tin học' AND g.grade_name = 'Luyện thi Đại học'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Kèm 1-1 Lập trình Web fullstack Java Spring Boot và ReactJS');

-- 5.16 Lớp Lịch sử 9 Ôn thi Chuyên (Hà Nội - Hoàn Kiếm)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư môn Lịch sử lớp 9 kèm ôn thi vào trường Chuyên',
       'Học sinh có nguyện vọng thi chuyên Sử THPT Chuyên Hà Nội - Amsterdam. Cần gia sư giỏi hướng dẫn cách phân tích sự kiện Lịch sử Việt Nam giai đoạn 1919 - 2000 và Lịch sử Thế giới.',
       'Đỗ Chuyên Sử Amsterdam', 'Gia sư từng đạt giải Quốc gia môn Sử hoặc Sư phạm Sử',
       'Hàng Bạc, Hoàn Kiếm, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 45 DAY,
       2400000.00, 2600000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Lịch sử' AND g.grade_name = 'Lớp 9'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư môn Lịch sử lớp 9 kèm ôn thi vào trường Chuyên');

-- 5.17 Lớp Địa lý 12 Khối C (TP.HCM - Quận 1)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần gia sư môn Địa lý 12 luyện thi tốt nghiệp THPT khối C',
       'Học sinh ôn thi khối C00 cần rèn luyện kỹ năng khai thác triệt để Atlat Địa lý Việt Nam, kỹ năng vẽ biểu đồ và nhận xét biểu đồ để đạt trọn vẹn điểm phần thực hành.',
       'Đạt 8.5+ môn Địa lý', 'Sinh viên hoặc Giáo viên Sư phạm Địa lý',
       'Đinh Tiên Hoàng, Quận 1, TP.HCM', 'OFFLINE', 10, CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 35 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Địa lý' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần gia sư môn Địa lý 12 luyện thi tốt nghiệp THPT khối C');

-- 5.18 Lớp Toán 5 Ôn thi CLC (Hà Nội - Cầu Giấy)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Toán 5 ôn thi vào lớp 6 THCS Cầu Giấy, Archimedes, Cầu Giấy',
       'Bé chuẩn bị thi vào lớp 6 các trường CLC tại Hà Nội. Cần gia sư giỏi Toán tư duy, toán chuyển động, toán tỉ số phần trăm và hình học tiểu học nâng cao.',
       'Đỗ vào lớp 6 trường CLC', 'Gia sư chuyên luyện thi Toán tiểu học chất lượng cao',
       'Trần Thái Tông, Cầu Giấy, Hà Nội', 'OFFLINE', 15, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 50 DAY,
       3300000.00, 3600000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 5'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Toán 5 ôn thi vào lớp 6 THCS Cầu Giấy, Archimedes, Cầu Giấy');

-- 5.19 Lớp Hóa 9 Chuyên (TP.HCM - Quận 10)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Hóa 9 ôn thi Chuyên Lê Hồng Phong / Năng Khiếu',
       'Học sinh có tố chất môn Hóa, cần học sâu chuyên đề Kim loại tác dụng với Axit, Muối, bài toán dung dịch và đồ thị phản ứng kết tủa.',
       'Đỗ Chuyên Hóa trường Top đầu TP.HCM', 'Thủ khoa/Á khoa Chuyên Hóa hoặc Sinh viên ĐH Y Dược',
       'Lý Thường Kiệt, Quận 10, TP.HCM', 'OFFLINE', 16, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 55 DAY,
       4000000.00, 4500000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm2@tcs.com' AND s.subject_name = 'Hóa học' AND g.grade_name = 'Lớp 9'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Hóa 9 ôn thi Chuyên Lê Hồng Phong / Năng Khiếu');

-- 5.20 Lớp Vật lý 10 Động lực học (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Lớp kèm Online Vật lý lớp 10 chương Động lực học chất điểm',
       'Học sinh lớp 10 cần nắm vững 3 định luật Newton, các lực cơ học, định luật bảo toàn động lượng và bảo toàn cơ năng.',
       'Điểm bài thi giữa kỳ đạt 9.0+', 'Gia sư nhiệt tình, có bảng vẽ đồ họa dạy Online sinh động',
       'Học qua Zoom', 'ONLINE', 10, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 30 DAY,
       2200000.00, 2500000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Vật lý' AND g.grade_name = 'Lớp 10'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Lớp kèm Online Vật lý lớp 10 chương Động lực học chất điểm');

-- 5.21 Lớp Tiếng Hàn Sơ Cấp (Hà Nội - Cầu Giấy)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Tiếng Hàn bắt đầu từ con số 0 cho người đi làm',
       'Học viên là nhân viên công ty Hàn Quốc muốn học thuộc bảng chữ cái Hangeul, cách phát âm, từ vựng thông dụng và giao tiếp cơ bản trong văn phòng.',
       'Giao tiếp cơ bản và đọc viết thành thạo', 'Gia sư phát âm chuẩn, kiên nhẫn',
       'Trung Kính, Cầu Giấy, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 40 DAY,
       3000000.00, 3200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Tiếng Hàn' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Tiếng Hàn bắt đầu từ con số 0 cho người đi làm');

-- 5.22 Lớp Tiếng Trung HSKK Cao Cấp (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Kèm 1-1 Luyện khẩu ngữ HSKK Cao cấp thi học bổng CSC',
       'Đã đỗ HSK 5, cần gia sư người Việt phát âm chuẩn hoặc du học sinh Trung Quốc luyện đề HSKK Cao cấp để nộp hồ sơ xin học bổng chính phủ Trung Quốc.',
       'Đạt 75+ điểm HSKK Cao cấp', 'Gia sư du học sinh Trung Quốc hoặc tốt nghiệp khoa Trung',
       'Học Online qua VooV Meeting / Zoom', 'ONLINE', 14, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 40 DAY,
       3920000.00, 4200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Tiếng Trung' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Kèm 1-1 Luyện khẩu ngữ HSKK Cao cấp thi học bổng CSC');

-- 5.23 Lớp Toán Đại Học (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Toán Cao Cấp & Giải tích 1 cho sinh viên năm nhất',
       'Sinh viên ĐH Bách Khoa/Kinh tế Quốc dân cần gia sư hướng dẫn các phần: Giới hạn hàm số, Đạo hàm, Tích phân suy rộng, Ma trận và Hệ phương trình tuyến tính.',
       'Qua môn điểm A/B+', 'Gia sư thủ khoa Toán hoặc Thạc sĩ Toán ứng dụng',
       'Học qua Google Meet', 'ONLINE', 10, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 30 DAY,
       2500000.00, 2800000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Luyện thi Đại học'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Toán Cao Cấp & Giải tích 1 cho sinh viên năm nhất');

-- 5.24 Lớp Văn 9 Chuyên (TP.HCM - Quận 1)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm cô giáo kèm môn Văn 9 ôn thi vào lớp 10 Chuyên',
       'Học sinh có năng khiếu văn chương, cần cô giáo hướng dẫn kỹ năng làm bài Lý luận văn học, tư duy mở rộng và liên hệ văn học đa chiều.',
       'Đỗ Chuyên Văn THPT Chuyên Lê Hồng Phong', 'Cô giáo chuyên Văn hoặc đạt giải Quốc gia môn Văn',
       'Nguyễn Du, Quận 1, TP.HCM', 'OFFLINE', 14, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 45 DAY,
       3500000.00, 3800000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Ngữ văn' AND g.grade_name = 'Lớp 9'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm cô giáo kèm môn Văn 9 ôn thi vào lớp 10 Chuyên');

-- 5.25 Lớp Tiếng Anh Cambridge Lớp 2 (Hà Nội - Nam Từ Liêm)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Tiếng Anh trẻ em Cambridge Starters cho bé lớp 2',
       'Bé 7 tuổi học trường tiểu học Vinschool, cần gia sư dạy ngữ âm Phonics, từ vựng và phản xạ giao tiếp tiếng Anh tự nhiên.',
       'Đạt 15 khiên chứng chỉ Starters', 'Cô giáo trẻ trung, phát âm chuẩn, có kinh nghiệm dạy trẻ em',
       'Vinhomes Smart City, Nam Từ Liêm, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 40 DAY,
       2400000.00, 2600000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Tiếng Anh' AND g.grade_name = 'Lớp 2'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Tiếng Anh trẻ em Cambridge Starters cho bé lớp 2');

-- 5.26 Lớp Hóa 11 Hữu cơ (TP.HCM - Tân Bình)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần gia sư Hóa 11 chuyên đề Hóa học hữu cơ (Hiđrocacbon, Ancol, Andehit)',
       'Học sinh bị hổng kiến thức phần Hóa hữu cơ lớp 11, cần thầy/cô kèm lại từ đầu để lấy lại gốc và chuẩn bị thi học kỳ 2.',
       'Lấy lại căn bản và đạt điểm 8+', 'Gia sư Sư phạm Hóa hoặc Bách Khoa có phương pháp dạy dễ hiểu',
       'Cộng Hòa, Tân Bình, TP.HCM', 'OFFLINE', 10, CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 35 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Hóa học' AND g.grade_name = 'Lớp 11'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần gia sư Hóa 11 chuyên đề Hóa học hữu cơ (Hiđrocacbon, Ancol, Andehit)');

-- 5.27 Lớp Toán 11 Lượng giác & Xác suất (Hà Nội - Cầu Giấy)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm thầy kèm Toán 11 chuyên đề Phương trình lượng giác và Tổ hợp - Xác suất',
       'Học sinh cần nắm chắc các công thức biến đổi lượng giác, phương trình lượng giác thường gặp và các bài toán đếm, xác suất nâng cao.',
       'Điểm kiểm tra định kỳ trên 8.5', 'Gia sư Sư phạm Toán nhiệt tình',
       'Khu tập thể ĐH Quốc Gia, Cầu Giấy, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 40 DAY,
       2700000.00, 3000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 11'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm thầy kèm Toán 11 chuyên đề Phương trình lượng giác và Tổ hợp - Xác suất');

-- 5.28 Lớp Tiếng Anh 9 Vào 10 (TP.HCM - Gò Vấp)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Tiếng Anh lớp 9 luyện thi vào 10 trường công lập tại Gò Vấp',
       'Học sinh cần củng cố ngữ pháp, rèn dạng bài tìm lỗi sai, biến đổi câu và đọc hiểu để chuẩn bị cho kỳ thi tuyển sinh lớp 10 tại TP.HCM.',
       'Đạt 9.0 môn Tiếng Anh vào 10', 'Sinh viên Sư phạm Anh hoặc Ngoại Thương',
       'Quang Trung, Gò Vấp, TP.HCM', 'OFFLINE', 12, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 40 DAY,
       2400000.00, 2600000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Tiếng Anh' AND g.grade_name = 'Lớp 9'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Tiếng Anh lớp 9 luyện thi vào 10 trường công lập tại Gò Vấp');

-- 5.29 Lớp Lập trình Frontend ReactJS (Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Kèm 1-1 Lập trình ReactJS, TailwindCSS và JavaScript ES6+',
       'Học viên trái ngành muốn học lập trình Web Frontend từ cơ bản đến nâng cao để chuyển nghề thành công.',
       'Tự xây dựng được 2 website thực tế (E-commerce / Dashboard)', 'Senior Frontend Developer có kinh nghiệm giảng dạy',
       'Học Online qua Zoom/Meet', 'ONLINE', 15, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 45 DAY,
       4500000.00, 5000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Tin học' AND g.grade_name = 'Luyện thi Đại học'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Kèm 1-1 Lập trình ReactJS, TailwindCSS và JavaScript ES6+');

-- 5.30 Lớp Sinh học 10 Khối B (Đà Nẵng - Hải Châu - Online)
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Sinh học 10 chương trình mới Kết nối tri thức',
       'Cần gia sư kèm Sinh học 10 cho học sinh định hướng thi khối B. Giảng giải chi tiết cơ chế phân bào Nguyên phân, Giảm phân và chuyển hóa vật chất trong tế bào.',
       'Nắm chắc lý thuyết đạt điểm 8.5+', 'Sinh viên Sư phạm Sinh hoặc Y Dược',
       'Học trực tuyến qua Zoom', 'ONLINE', 10, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 30 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.danang1@tcs.com' AND s.subject_name = 'Sinh học' AND g.grade_name = 'Lớp 10'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Sinh học 10 chương trình mới Kết nối tri thức');

SET FOREIGN_KEY_CHECKS = 1;
