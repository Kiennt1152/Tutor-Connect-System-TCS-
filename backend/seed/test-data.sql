-- ============================================================================
-- Tutor-Connect-System (TCS) – Seed data phục vụ test thủ công
-- Module tập trung: marketplace (tutor_applications / tutoring_classes)
-- Cách dùng:
--   1. Đăng ký 1 user qua app để lấy password_hash thật (BCrypt 10 rounds)
--      Hoặc dùng hash có sẵn dưới (đã gen bằng Spring BCryptPasswordEncoder).
--   2. Mở MySQL Workbench / CLI, chọn schema `tcs`, chạy file này.
--   3. Nếu schema khác tên, sửa dòng `USE tcs;` phía dưới.
-- Lưu ý:
--   - Script idempotent trên các bảng reference (xóa & insert lại).
--   - Bảng core (users / clients / tutors / tutoring_classes /
--     tutor_applications) dùng INSERT IGNORE theo id cố định để không đè dữ liệu thật.
--   - Password chung cho mọi user test: 123456aA@
--   - BCrypt hash của password trên:
--     $2a$10$UtYSrMyN3B65oDQfS7uLAeAoz6roV9TBLK7LUEwpWc3J19pvD0biu
-- ============================================================================

USE tcs;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. Reference data: provinces, locations, grades, subjects, categories
-- ----------------------------------------------------------------------------
TRUNCATE TABLE provinces;
INSERT INTO provinces (province_id, province_name, code) VALUES
  (1, 'Hà Nội',          'HN'),
  (2, 'TP. Hồ Chí Minh', 'HCM');

TRUNCATE TABLE locations;
INSERT INTO locations
  (location_id, google_place_id, address_line, ward, district, city, province_id, latitude, longitude)
VALUES
  (1, 'PLACE_HN_CAU_GI', '12 Chùa Bộc',     'Quang Trung',   'Đống Đa',     'Hà Nội',  1, 21.0078, 105.8289),
  (2, 'PLACE_HCM_Q1',    '1 Lê Duẩn',       'Bến Nghé',     'Quận 1',      'TP.HCM',  2, 10.7821, 106.7000);

TRUNCATE TABLE grades;
INSERT INTO grades (grade_id, grade_name) VALUES
  (1, 'Lớp 1'), (2, 'Lớp 2'), (3, 'Lớp 3'), (4, 'Lớp 4'), (5, 'Lớp 5'),
  (6, 'Lớp 6'), (7, 'Lớp 7'), (8, 'Lớp 8'), (9, 'Lớp 9'),
  (10,'Lớp 10'),(11,'Lớp 11'),(12,'Lớp 12');

TRUNCATE TABLE subjects;
INSERT INTO subjects (subject_id, subject_name, description) VALUES
  (1, 'Toán',  'Toán học'),
  (2, 'Ngữ văn', 'Ngữ văn Việt Nam'),
  (3, 'Tiếng Anh', 'English'),
  (4, 'Vật lý', 'Vật lý'),
  (5, 'Hóa học', 'Hóa học');

TRUNCATE TABLE categories;
INSERT INTO categories (category_id, name, description, parent_id, status) VALUES
  (1, 'Toán',     'Toán học', NULL, 'ACTIVE'),
  (2, 'Ngữ văn',  'Ngữ văn',  NULL, 'ACTIVE'),
  (3, 'Ngoại ngữ','Ngoại ngữ',NULL, 'ACTIVE');

-- ----------------------------------------------------------------------------
-- 2. Users + profile (client / tutor)
--    Password chung: 123456aA@
-- ----------------------------------------------------------------------------
SET @PWD := '$2a$10$UtYSrMyN3B65oDQfS7uLAeAoz6roV9TBLK7LUEwpWc3J19pvD0biu';

DELETE FROM tutor_subjects        WHERE tutor_id IN (1,2,3);
DELETE FROM class_assignments      WHERE application_id IN (1,2,3,4);
DELETE FROM tutor_applications     WHERE tutor_id    IN (1,2,3);
DELETE FROM tutoring_classes       WHERE creator_id  IN (1,2);
DELETE FROM tutor_educations       WHERE tutor_id    IN (1,2,3);
DELETE FROM tutor_experiences      WHERE tutor_id    IN (1,2,3);
DELETE FROM tutors                 WHERE tutor_id    IN (1,2,3);
DELETE FROM clients                WHERE client_id   IN (1,2);
DELETE FROM users                  WHERE user_id     IN (1,2,3,4,5);

INSERT INTO users (user_id, email, password_hash, status, created_at) VALUES
  (1, '[email protected]', @PWD, 'ACTIVE', '2026-06-01 09:00:00'),
  (2, '[email protected]',    @PWD, 'ACTIVE', '2026-06-01 09:00:00'),
  (3, '[email protected]',   @PWD, 'ACTIVE', '2026-06-01 09:00:00'),
  (4, '[email protected]', @PWD, 'ACTIVE', '2026-06-01 09:00:00'),
  (5, '[email protected]', @PWD, 'ACTIVE', '2026-06-01 09:00:00');

INSERT INTO clients (client_id, user_id, full_name, phone, address, avatar, gender, location_id) VALUES
  (1, 1, 'Nguyễn Văn A',  '0901000001', '12 Chùa Bộc, Đống Đa, Hà Nội',  NULL, 'MALE',   1),
  (2, 2, 'Trần Thị B',    '0901000002', '1 Lê Duẩn, Quận 1, TP.HCM',     NULL, 'FEMALE', 2);

INSERT INTO tutors
  (tutor_id, user_id, full_name, gender, phone, address,
   experience_years, bio, hourly_rate, rating_avg,
   date_of_birth, location_id, verification_status, avatar)
VALUES
  (1, 3, 'Lê Minh C',    'MALE',   '0901000003', 'Hà Nội',
       5, 'GV Toán trường THPT chuyên, 5 năm kinh nghiệm', 250000.00, 4.80,
       '1995-03-15', 1, 'VERIFIED', NULL),
  (2, 4, 'Phạm Thu D',   'FEMALE', '0901000004', 'Hà Nội',
       3, 'GV Ngữ văn, từng dạy gia sư nhiều năm', 220000.00, 4.60,
       '1997-07-22', 1, 'VERIFIED', NULL),
  (3, 5, 'Hoàng Anh E',  'MALE',   '0901000005', 'TP.HCM',
       7, 'GV Tiếng Anh, IELTS 8.5', 300000.00, 4.90,
       '1992-11-05', 2, 'VERIFIED', NULL);

-- tutor_subjects (subject-based taxonomy từ V5)
INSERT INTO tutor_subjects (tutor_id, subject_id, proficiency_level) VALUES
  (1, 1, 'EXPERT'),     -- Tutor 1: Toán
  (2, 2, 'ADVANCED'),   -- Tutor 2: Ngữ văn
  (2, 3, 'INTERMEDIATE'),
  (3, 3, 'EXPERT');     -- Tutor 3: Tiếng Anh

-- ----------------------------------------------------------------------------
-- 3. Wallets cho các user (để test endpoint wallet sau này)
-- ----------------------------------------------------------------------------
DELETE FROM wallets WHERE wallet_id IN (1,2,3,4,5);
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status) VALUES
  (1, 5000000.00, 0.00, 'ACTIVE'),
  (2, 3000000.00, 0.00, 'ACTIVE'),
  (3,  200000.00, 0.00, 'ACTIVE'),
  (4,  150000.00, 0.00, 'ACTIVE'),
  (5,  800000.00, 0.00, 'ACTIVE');

-- ----------------------------------------------------------------------------
-- 4. tutoring_classes – 4 lớp test các trạng thái khác nhau
--    Cột V8: class_type PRIVATE/CENTER, budget NULL-able
-- ----------------------------------------------------------------------------
DELETE FROM tutoring_classes WHERE class_id IN (1,2,3,4);

-- Class 1: OPEN – 1 tutor đã apply (status SUBMITTED)
INSERT INTO tutoring_classes
  (class_id, creator_id, class_type, category_id, subject_id, grade_id, location_id,
   title, description, lesson_mode, number_of_sessions, max_sessions,
   tuition_fee, budget, recurring_type, status,
   start_date, end_date, max_students, min_students, enrollment_deadline)
VALUES
  (1, 1, 'PRIVATE', 1, 1, 9, 1,
   'Gia sư Toán 9 – học kỳ 1', 'Cần gia sư Toán lớp 9, 2 buổi/tuần, học tại nhà',
   'OFFLINE', 24, 24,
   200000.00, 4800000.00, 'WEEKLY', 'OPEN',
   '2026-07-15', '2026-09-15', 1, 1, '2026-07-10');

-- Class 2: OPEN – 3 tutor đã apply (SUBMITTED, UNDER_REVIEW, REJECTED)
INSERT INTO tutoring_classes
  (class_id, creator_id, class_type, category_id, subject_id, grade_id, location_id,
   title, description, lesson_mode, number_of_sessions, max_sessions,
   tuition_fee, budget, recurring_type, status,
   start_date, end_date, max_students, min_students, enrollment_deadline)
VALUES
  (2, 1, 'PRIVATE', 3, 3, 11, 2,
   'Gia sư Tiếng Anh 11 – luyện thi', 'Luyện IELTS + speaking, 3 buổi/tuần',
   'ONLINE', 30, 30,
   250000.00, 7500000.00, 'WEEKLY', 'OPEN',
   '2026-07-20', '2026-10-20', 1, 1, '2026-07-18');

-- Class 3: IN_PROGRESS – đã accept tutor, tạo assignment
INSERT INTO tutoring_classes
  (class_id, creator_id, class_type, category_id, subject_id, grade_id, location_id,
   title, description, lesson_mode, number_of_sessions, max_sessions,
   tuition_fee, budget, recurring_type, status,
   start_date, end_date, max_students, min_students, enrollment_deadline)
VALUES
  (3, 2, 'PRIVATE', 2, 2, 8, 1,
   'Gia sư Ngữ văn 8 – viết luận', 'Tập trung kỹ năng viết luận văn',
   'HYBRID', 16, 16,
   200000.00, 3200000.00, 'WEEKLY', 'IN_PROGRESS',
   '2026-06-01', '2026-08-30', 1, 1, '2026-05-30');

-- Class 4: DRAFT – lớp nháp, chưa mở
INSERT INTO tutoring_classes
  (class_id, creator_id, class_type, category_id, subject_id, grade_id, location_id,
   title, description, lesson_mode, number_of_sessions, max_sessions,
   tuition_fee, budget, recurring_type, status,
   start_date, end_date, max_students, min_students, enrollment_deadline)
VALUES
  (4, 1, 'PRIVATE', 1, 1, 7, 1,
   'Gia sư Toán 7 – hè', 'Ôn hè, 2 buổi/tuần',
   'OFFLINE', 10, 10,
   180000.00, 1800000.00, 'WEEKLY', 'DRAFT',
   '2026-07-01', '2026-08-01', 1, 1, '2026-06-25');

-- ----------------------------------------------------------------------------
-- 5. tutor_applications – đủ trạng thái phục vụ test marketplace
--    V4 CHECK: SUBMITTED, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN
-- ----------------------------------------------------------------------------
DELETE FROM tutor_applications WHERE application_id IN (1,2,3,4);

-- App 1: SUBMITTED – tutor 3 apply class 1
INSERT INTO tutor_applications
  (application_id, class_id, tutor_id, proposed_rate, cover_letter,
   status, applied_at, reviewed_at)
VALUES
  (1, 1, 3, 220000.00,
   'Em có 7 năm kinh nghiệm dạy Toán THCS, sẵn sàng cam kết đầu ra.',
   'SUBMITTED', '2026-06-25 10:30:00', NULL);

-- App 2: UNDER_REVIEW – tutor 1 apply class 2 (client đang xét)
INSERT INTO tutor_applications
  (application_id, class_id, tutor_id, proposed_rate, cover_letter,
   status, applied_at, reviewed_at)
VALUES
  (2, 2, 1, 230000.00,
   'Em dạy Toán nhưng rất tự tin tiếng Anh, IELTS 7.0.',
   'UNDER_REVIEW', '2026-06-24 14:15:00', NULL);

-- App 3: REJECTED – tutor 2 apply class 2
INSERT INTO tutor_applications
  (application_id, class_id, tutor_id, proposed_rate, cover_letter,
   status, applied_at, reviewed_at)
VALUES
  (3, 2, 2, 240000.00,
   'Em có bằng cử nhân Sư phạm Ngữ văn, có thể hỗ trợ Tiếng Anh cơ bản.',
   'REJECTED', '2026-06-24 09:00:00', '2026-06-25 11:00:00');

-- App 4: ACCEPTED – tutor 2 apply class 3 (kèm assignment)
INSERT INTO tutor_applications
  (application_id, class_id, tutor_id, proposed_rate, cover_letter,
   status, applied_at, reviewed_at)
VALUES
  (4, 3, 2, 200000.00,
   'Em chuyên Ngữ văn, có kinh nghiệm luyện viết luận cho học sinh lớp 8.',
   'ACCEPTED', '2026-05-25 08:30:00', '2026-05-27 16:00:00');

-- ----------------------------------------------------------------------------
-- 6. class_assignments – cho app 4 (ACCEPTED)
-- ----------------------------------------------------------------------------
DELETE FROM class_assignments WHERE assignment_id = 1;
INSERT INTO class_assignments (assignment_id, application_id, tutor_id, assigned_date, status) VALUES
  (1, 4, 2, '2026-05-27 16:00:00', 'ACTIVE');

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 7. Bảng test nhanh sau khi chạy
-- ----------------------------------------------------------------------------
SELECT 'users' AS tbl, COUNT(*) AS cnt FROM users WHERE user_id <= 5
UNION ALL SELECT 'clients',          COUNT(*) FROM clients          WHERE client_id <= 2
UNION ALL SELECT 'tutors',           COUNT(*) FROM tutors           WHERE tutor_id <= 3
UNION ALL SELECT 'tutoring_classes', COUNT(*) FROM tutoring_classes WHERE class_id <= 4
UNION ALL SELECT 'tutor_applications', COUNT(*) FROM tutor_applications WHERE application_id <= 4
UNION ALL SELECT 'class_assignments', COUNT(*) FROM class_assignments WHERE assignment_id <= 1;

-- ----------------------------------------------------------------------------
-- 8. Truy vấn nhanh cho từng flow test
-- ----------------------------------------------------------------------------

-- Lớp của client 1, status OPEN (test marketplace search):
--   SELECT * FROM tutoring_classes WHERE creator_id = 1 AND status = 'OPEN';

-- Application của tutor 1 (xem trên TutorView):
--   SELECT a.*, c.title FROM tutor_applications a
--   JOIN tutoring_classes c ON c.class_id = a.class_id
--   WHERE a.tutor_id = 1;

-- Application của class 2 (xem trên ClientView, có đủ 3 status):
--   SELECT * FROM tutor_applications WHERE class_id = 2;