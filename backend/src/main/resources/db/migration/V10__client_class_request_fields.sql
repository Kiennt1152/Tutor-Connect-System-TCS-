-- =====================================================================
-- UC: Client tao/sua yeu cau tim gia su (lop PRIVATE).
-- Bo sung 3 truong mo ta nhu form dang tin tim gia su:
--   - learning_goal     : Muc tieu hoc tap (Lay lai goc / On thi hoc ky / ...)
--   - tutor_requirement : Yeu cau doi voi gia su (sinh vien / giao vien / co chung chi ...)
--   - address           : Dia chi hoc chi tiet (bo sung cho location_id cap quan/huyen)
-- Tat ca nullable + cong them, khong pha du lieu hien co.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE tutoring_classes
    ADD COLUMN learning_goal VARCHAR(100) NULL AFTER grade_id;
ALTER TABLE tutoring_classes
    ADD COLUMN tutor_requirement VARCHAR(255) NULL AFTER learning_goal;
ALTER TABLE tutoring_classes
    ADD COLUMN address VARCHAR(255) NULL AFTER location_id;
