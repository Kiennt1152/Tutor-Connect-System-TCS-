-- BF-04: học viên phải KÝ hợp đồng xong mới chính thức vào lớp.
-- Thêm trạng thái PENDING_SIGNATURE (chờ ký) cho học viên.
ALTER TABLE class_students DROP CHECK chk_class_students_status;
ALTER TABLE class_students ADD CONSTRAINT chk_class_students_status
    CHECK (status IN ('PENDING_SIGNATURE','ENROLLED','DROPPED','COMPLETED'));
