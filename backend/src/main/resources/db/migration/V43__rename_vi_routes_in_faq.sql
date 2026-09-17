-- V43: Đổi đường dẫn tiếng Việt thành tiếng Anh trong nội dung FAQ đã nằm sẵn trong DB.
--
-- R__seed_catalog.sql dùng INSERT IGNORE nên chạy lại KHÔNG cập nhật được các dòng faq_entries
-- đã tồn tại — phải UPDATE tay ở đây, nếu không trợ lý AI sẽ trả về link chết.
--
-- /tim-gia-su  -> /find-tutor    (APP_ROUTES.findTutor)
-- /lop-hoc     -> /class-finder  (APP_ROUTES.classFinder)

UPDATE faq_entries
SET answer = REPLACE(answer, '/tim-gia-su', '/find-tutor')
WHERE answer LIKE '%/tim-gia-su%';

UPDATE faq_entries
SET answer = REPLACE(answer, '/lop-hoc', '/class-finder')
WHERE answer LIKE '%/lop-hoc%';

UPDATE faq_entries
SET question = REPLACE(question, '/tim-gia-su', '/find-tutor')
WHERE question LIKE '%/tim-gia-su%';

UPDATE faq_entries
SET question = REPLACE(question, '/lop-hoc', '/class-finder')
WHERE question LIKE '%/lop-hoc%';

-- Chỉ mục tri thức của trợ lý AI là bản sao đã dựng sẵn từ FAQ + văn bản hệ thống; nó chỉ tự
-- dựng lại khi bảng RỖNG lúc khởi động, nên phải sửa tại chỗ. content_hash cố tình để nguyên:
-- lần Reindex thủ công tiếp theo sẽ thấy khác và tự cập nhật lại kèm embedding mới.
UPDATE ai_knowledge_chunks
SET content = REPLACE(REPLACE(content, '/tim-gia-su', '/find-tutor'), '/lop-hoc', '/class-finder')
WHERE content LIKE '%/tim-gia-su%' OR content LIKE '%/lop-hoc%';

UPDATE ai_knowledge_chunks
SET title = REPLACE(REPLACE(title, '/tim-gia-su', '/find-tutor'), '/lop-hoc', '/class-finder')
WHERE title LIKE '%/tim-gia-su%' OR title LIKE '%/lop-hoc%';
