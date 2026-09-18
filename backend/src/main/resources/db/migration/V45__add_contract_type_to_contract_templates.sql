-- V45: Bổ sung trường contract_type vào bảng contract_templates để chuẩn hóa phân loại mẫu hợp đồng
-- Các loại chuẩn:
--   PRIVATE_TUTORING: Hợp đồng dạy kèm 1:1 cá nhân (Phụ huynh <-> Gia sư)
--   CENTER_CLASS: Hợp đồng đào tạo theo lớp trung tâm (Học viên <-> Trung tâm)
--   RECRUITMENT: Thỏa thuận hợp tác tuyển dụng gia sư (Trung tâm <-> Gia sư)
--   SPECIALIZED_GUARANTEE: Hợp đồng cam kết đầu ra / luyện thi chứng chỉ quốc tế (Học viên <-> Gia sư/Trung tâm)

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contract_templates'
      AND COLUMN_NAME = 'contract_type');
SET @ddl := IF(@col = 0,
    'ALTER TABLE contract_templates ADD COLUMN contract_type VARCHAR(50) NOT NULL DEFAULT \'CENTER_CLASS\' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Cập nhật dữ liệu cho các mẫu hiện có
UPDATE contract_templates
SET contract_type = 'CENTER_CLASS'
WHERE template_id = 1 OR name LIKE '%theo lớp%';

UPDATE contract_templates
SET contract_type = 'RECRUITMENT'
WHERE template_id = 2 OR name LIKE '%hợp tác%' OR name LIKE '%tuyển dụng%';

UPDATE contract_templates
SET contract_type = 'SPECIALIZED_GUARANTEE'
WHERE template_id = 3 OR name LIKE '%chuyên biệt%' OR name LIKE '%chứng chỉ%';

-- Đồng bộ vào system_parameters cho các module cũ nếu có đọc
INSERT INTO system_parameters (param_key, param_value, description)
VALUES 
    ('tpltype:1', 'CLASS', 'Loại mẫu hợp đồng #1'),
    ('tpltype:2', 'RECRUITMENT', 'Loại mẫu hợp đồng #2'),
    ('tpltype:3', 'CLASS', 'Loại mẫu hợp đồng #3'),
    ('CONTRACT_TEMPLATE_TYPE_1', 'CENTER_CLASS', 'Loại mẫu hợp đồng #1'),
    ('CONTRACT_TEMPLATE_TYPE_2', 'RECRUITMENT', 'Loại mẫu hợp đồng #2'),
    ('CONTRACT_TEMPLATE_TYPE_3', 'SPECIALIZED_GUARANTEE', 'Loại mẫu hợp đồng #3')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);
