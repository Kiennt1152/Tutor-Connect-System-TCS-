-- Seed initial TCS category taxonomy after schema alignment.
-- This migration runs after category `type` and audit columns are available.

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'SUBJECT', 'SUBJECT', 'Root taxonomy for tutoring subjects and academic topics.', NULL, TRUE, 0, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'SUBJECT');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'EDUCATION_LEVEL', 'EDUCATION_LEVEL', 'Root taxonomy for learning stages and learner levels.', NULL, TRUE, 0, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'EDUCATION_LEVEL');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'LOCATION', 'LOCATION', 'Root taxonomy for supported operating regions and tutoring locations.', NULL, TRUE, 0, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'LOCATION');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'SYSTEM_CONFIG', 'SYSTEM_CONFIG', 'Root taxonomy for reusable system options managed by admins.', NULL, TRUE, 0, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'SYSTEM_CONFIG');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Primary School', 'SUBJECT', 'Subject grouping for primary-school tutoring demand.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Primary School');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Lower Secondary Subjects', 'SUBJECT', 'Subject grouping for lower-secondary tutoring demand.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Lower Secondary Subjects');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Upper Secondary Subjects', 'SUBJECT', 'Subject grouping for upper-secondary tutoring demand.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Upper Secondary Subjects');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Exam Preparation', 'SUBJECT', 'Subjects commonly used for certificate and entrance-exam preparation.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Exam Preparation');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Language & Communication', 'SUBJECT', 'Practical language learning and communication subjects.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Language & Communication');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'STEM & Digital Skills', 'SUBJECT', 'Technology and digital skill subjects for modern tutoring needs.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'STEM & Digital Skills');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Mathematics (Primary)', 'SUBJECT', 'Core math tutoring for primary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Primary School'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mathematics (Primary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Vietnamese (Primary)', 'SUBJECT', 'Vietnamese language tutoring for primary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Primary School'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Vietnamese (Primary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'English (Primary)', 'SUBJECT', 'Foundational English tutoring for primary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Primary School'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English (Primary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Mathematics (Lower Secondary)', 'SUBJECT', 'Math tutoring for lower-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mathematics (Lower Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Literature (Lower Secondary)', 'SUBJECT', 'Literature tutoring for lower-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Literature (Lower Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'English (Lower Secondary)', 'SUBJECT', 'English tutoring for lower-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English (Lower Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Physics (Lower Secondary)', 'SUBJECT', 'Introductory physics tutoring for lower-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Physics (Lower Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Chemistry (Lower Secondary)', 'SUBJECT', 'Introductory chemistry tutoring for lower-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Chemistry (Lower Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Mathematics (Upper Secondary)', 'SUBJECT', 'Math tutoring for upper-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mathematics (Upper Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Literature (Upper Secondary)', 'SUBJECT', 'Literature tutoring for upper-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Literature (Upper Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'English (Upper Secondary)', 'SUBJECT', 'English tutoring for upper-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English (Upper Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Physics (Upper Secondary)', 'SUBJECT', 'Physics tutoring for upper-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Physics (Upper Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Chemistry (Upper Secondary)', 'SUBJECT', 'Chemistry tutoring for upper-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Chemistry (Upper Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Biology (Upper Secondary)', 'SUBJECT', 'Biology tutoring for upper-secondary students.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Biology (Upper Secondary)');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'IELTS', 'SUBJECT', 'Exam preparation subject for IELTS learners.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Exam Preparation'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'IELTS');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'TOEIC', 'SUBJECT', 'Exam preparation subject for TOEIC learners.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Exam Preparation'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'TOEIC');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'National High School Exam', 'SUBJECT', 'Preparation for national high school graduation and university-entry exams.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Exam Preparation'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'National High School Exam');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'English Communication', 'SUBJECT', 'Practical spoken English and daily communication training.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Language & Communication'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English Communication');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Public Speaking', 'SUBJECT', 'Communication confidence and presentation coaching.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Language & Communication'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Public Speaking');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Programming Basics', 'SUBJECT', 'Introductory programming and computational thinking.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'STEM & Digital Skills'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Programming Basics');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Computer Science Fundamentals', 'SUBJECT', 'Core digital literacy and computer science foundations.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'STEM & Digital Skills'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Computer Science Fundamentals');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Primary', 'EDUCATION_LEVEL', 'Learner level for primary education.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Primary');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Lower Secondary', 'EDUCATION_LEVEL', 'Learner level for lower-secondary education.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Lower Secondary');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Upper Secondary', 'EDUCATION_LEVEL', 'Learner level for upper-secondary education.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Upper Secondary');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'University', 'EDUCATION_LEVEL', 'Learner level for university and college tutoring.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'University');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Adult Learner', 'EDUCATION_LEVEL', 'Learner level for working adults and continuing education.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Adult Learner');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Ho Chi Minh City', 'LOCATION', 'Primary operating region in southern Vietnam.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'LOCATION'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ho Chi Minh City');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Hanoi', 'LOCATION', 'Primary operating region in northern Vietnam.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'LOCATION'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hanoi');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Da Nang', 'LOCATION', 'Primary operating region in central Vietnam.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'LOCATION'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Da Nang');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Thu Duc City', 'LOCATION', 'District-level operating area within Ho Chi Minh City.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Ho Chi Minh City'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Thu Duc City');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Binh Thanh District', 'LOCATION', 'District-level operating area within Ho Chi Minh City.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Ho Chi Minh City'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Binh Thanh District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Go Vap District', 'LOCATION', 'District-level operating area within Ho Chi Minh City.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Ho Chi Minh City'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Go Vap District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Cau Giay District', 'LOCATION', 'District-level operating area within Hanoi.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Hanoi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cau Giay District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Dong Da District', 'LOCATION', 'District-level operating area within Hanoi.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Hanoi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Dong Da District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Ha Dong District', 'LOCATION', 'District-level operating area within Hanoi.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Hanoi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ha Dong District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Hai Chau District', 'LOCATION', 'District-level operating area within Da Nang.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Da Nang'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hai Chau District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Thanh Khe District', 'LOCATION', 'District-level operating area within Da Nang.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Da Nang'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Thanh Khe District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'Son Tra District', 'LOCATION', 'District-level operating area within Da Nang.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'Da Nang'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Son Tra District');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'LESSON_MODE', 'SYSTEM_CONFIG', 'System options for lesson delivery mode selection.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'SYSTEM_CONFIG'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'LESSON_MODE');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'ONLINE', 'SYSTEM_CONFIG', 'Lesson delivery happens fully online.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'LESSON_MODE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'ONLINE');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'OFFLINE', 'SYSTEM_CONFIG', 'Lesson delivery happens in person.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'LESSON_MODE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'OFFLINE');

INSERT INTO categories (name, type, description, parent_id, is_active, sort_order, status)
SELECT 'HYBRID', 'SYSTEM_CONFIG', 'Lesson delivery mixes online and in-person formats.', category_id, TRUE, 0, 'ACTIVE'
FROM categories
WHERE name = 'LESSON_MODE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'HYBRID');
