-- Chon gia su -> gia su nhan lop -> sinh lich day -> diem danh tung buoi.
-- Ba bang class_assignments / schedule_slots / lessons da co san tu V1 nhung chua bao gio
-- duoc ghi; ba cot duoi day la nhung gi con thieu de chay duoc luong tren.

-- 1) Gia su can trang thai "cho nhan lop" truoc khi ACTIVE, va "tu choi" de lop quay ve OPEN.
ALTER TABLE class_assignments DROP CHECK chk_class_assignments_status;
ALTER TABLE class_assignments
    ADD CONSTRAINT chk_class_assignments_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'DECLINED', 'TERMINATED'));

-- 2) Lop nhieu mon: moi slot thuoc mot mon cu the (detailsJson.slots[].subjectId).
--    Nullable vi lop cu / lop CENTER co the khong nêu mon.
ALTER TABLE schedule_slots
    ADD COLUMN subject_id BIGINT NULL AFTER class_id,
    ADD CONSTRAINT fk_schedule_slots_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id);

-- 3) Buoi hoc phai biet dien ra NGAY NAO thi moi hien duoc lich day va chan diem danh
--    ngoai ngay. Bang dang rong nen dat NOT NULL duoc ngay.
ALTER TABLE lessons
    ADD COLUMN lesson_date DATE NOT NULL AFTER class_id;

CREATE INDEX idx_lessons_tutor_date ON lessons (tutor_id, lesson_date);
