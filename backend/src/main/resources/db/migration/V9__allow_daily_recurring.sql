-- =====================================================================
-- UC-14-B: cho phep kieu lap lich DAILY (Hang ngay) cho tutoring_classes.
-- Chi noi long CHECK constraint recurring_type, khong doi du lieu.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE tutoring_classes DROP CHECK chk_tutoring_classes_recurring;
ALTER TABLE tutoring_classes ADD CONSTRAINT chk_tutoring_classes_recurring
    CHECK (recurring_type IN ('ONCE','WEEKLY','DAILY'));
