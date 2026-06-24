-- Seed initial TCS category taxonomy inferred from PRD Report 3.
-- Because the current Category schema has no `type` column, the PRD types
-- are represented as root categories: SUBJECT, EDUCATION_LEVEL, LOCATION, SYSTEM_CONFIG.

INSERT INTO categories (name, description, parent_id, status)
SELECT 'SUBJECT', 'Root taxonomy for tutoring subjects and academic topics.', NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'SUBJECT');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'EDUCATION_LEVEL', 'Root taxonomy for learning stages and learner levels.', NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'EDUCATION_LEVEL');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'LOCATION', 'Root taxonomy for supported operating regions and tutoring locations.', NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'LOCATION');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'SYSTEM_CONFIG', 'Root taxonomy for reusable system options managed by admins.', NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'SYSTEM_CONFIG');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Primary School', 'Subject grouping for primary-school tutoring demand.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Primary School');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Lower Secondary Subjects', 'Subject grouping for lower-secondary tutoring demand.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Lower Secondary Subjects');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Upper Secondary Subjects', 'Subject grouping for upper-secondary tutoring demand.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Upper Secondary Subjects');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Exam Preparation', 'Subjects commonly used for certificate and entrance-exam preparation.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Exam Preparation');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Language & Communication', 'Practical language learning and communication subjects.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Language & Communication');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'STEM & Digital Skills', 'Technology and digital skill subjects for modern tutoring needs.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SUBJECT'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'STEM & Digital Skills');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Mathematics (Primary)', 'Core math tutoring for primary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Primary School'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mathematics (Primary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Vietnamese (Primary)', 'Vietnamese language tutoring for primary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Primary School'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Vietnamese (Primary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'English (Primary)', 'Foundational English tutoring for primary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Primary School'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English (Primary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Mathematics (Lower Secondary)', 'Math tutoring for lower-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mathematics (Lower Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Literature (Lower Secondary)', 'Literature tutoring for lower-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Literature (Lower Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'English (Lower Secondary)', 'English tutoring for lower-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English (Lower Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Physics (Lower Secondary)', 'Introductory physics tutoring for lower-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Physics (Lower Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Chemistry (Lower Secondary)', 'Introductory chemistry tutoring for lower-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Lower Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Chemistry (Lower Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Mathematics (Upper Secondary)', 'Math tutoring for upper-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Mathematics (Upper Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Literature (Upper Secondary)', 'Literature tutoring for upper-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Literature (Upper Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'English (Upper Secondary)', 'English tutoring for upper-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English (Upper Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Physics (Upper Secondary)', 'Physics tutoring for upper-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Physics (Upper Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Chemistry (Upper Secondary)', 'Chemistry tutoring for upper-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Chemistry (Upper Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Biology (Upper Secondary)', 'Biology tutoring for upper-secondary students.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Upper Secondary Subjects'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Biology (Upper Secondary)');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'IELTS', 'Exam preparation subject for IELTS learners.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Exam Preparation'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'IELTS');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'TOEIC', 'Exam preparation subject for TOEIC learners.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Exam Preparation'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'TOEIC');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'National High School Exam', 'Preparation for national high school graduation and university-entry exams.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Exam Preparation'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'National High School Exam');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'English Communication', 'Practical spoken English and daily communication training.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Language & Communication'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'English Communication');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Public Speaking', 'Communication confidence and presentation coaching.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Language & Communication'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Public Speaking');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Programming Basics', 'Introductory programming and computational thinking.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'STEM & Digital Skills'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Programming Basics');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Computer Science Fundamentals', 'Core digital literacy and computer science foundations.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'STEM & Digital Skills'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Computer Science Fundamentals');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Primary', 'Learner level for primary education.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Primary');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Lower Secondary', 'Learner level for lower-secondary education.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Lower Secondary');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Upper Secondary', 'Learner level for upper-secondary education.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Upper Secondary');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'University', 'Learner level for university and college tutoring.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'University');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Adult Learner', 'Learner level for working adults and continuing education.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'EDUCATION_LEVEL'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Adult Learner');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Ho Chi Minh City', 'Primary operating region in southern Vietnam.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LOCATION'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ho Chi Minh City');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Hanoi', 'Primary operating region in northern Vietnam.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LOCATION'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hanoi');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Da Nang', 'Primary operating region in central Vietnam.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LOCATION'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Da Nang');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Thu Duc City', 'District-level operating area within Ho Chi Minh City.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Ho Chi Minh City'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Thu Duc City');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Binh Thanh District', 'District-level operating area within Ho Chi Minh City.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Ho Chi Minh City'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Binh Thanh District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Go Vap District', 'District-level operating area within Ho Chi Minh City.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Ho Chi Minh City'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Go Vap District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Cau Giay District', 'District-level operating area within Hanoi.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Hanoi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cau Giay District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Dong Da District', 'District-level operating area within Hanoi.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Hanoi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Dong Da District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Ha Dong District', 'District-level operating area within Hanoi.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Hanoi'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ha Dong District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Hai Chau District', 'District-level operating area within Da Nang.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Da Nang'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hai Chau District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Thanh Khe District', 'District-level operating area within Da Nang.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Da Nang'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Thanh Khe District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'Son Tra District', 'District-level operating area within Da Nang.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'Da Nang'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Son Tra District');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'LESSON_MODE', 'System options for lesson delivery mode selection.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SYSTEM_CONFIG'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'LESSON_MODE');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'LEAD_SOURCE', 'System options for inbound lead source origin.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'SYSTEM_CONFIG'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'LEAD_SOURCE');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'ONLINE', 'Lesson delivery happens fully online.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LESSON_MODE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'ONLINE');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'OFFLINE', 'Lesson delivery happens in person.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LESSON_MODE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'OFFLINE');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'HYBRID', 'Lesson delivery mixes online and in-person formats.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LESSON_MODE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'HYBRID');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'FACEBOOK_GROUP', 'Lead collected from public Facebook groups.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LEAD_SOURCE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'FACEBOOK_GROUP');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'FACEBOOK_ADS', 'Lead collected from Facebook lead ads integrations.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LEAD_SOURCE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'FACEBOOK_ADS');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'MANUAL', 'Lead inserted manually by center staff or administrators.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LEAD_SOURCE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'MANUAL');

INSERT INTO categories (name, description, parent_id, status)
SELECT 'MESSENGER', 'Lead collected from page or chatbot messenger flows.', category_id, 'ACTIVE'
FROM categories
WHERE name = 'LEAD_SOURCE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'MESSENGER');
