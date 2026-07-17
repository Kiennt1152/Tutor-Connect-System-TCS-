-- Gia su ung tuyen: hoc phi de xuat theo TUNG MON thay vi mot muc chung.
-- Luu snapshot JSON {subjectId -> rate} giong cach tutoring_classes.details_json luu subjectFees.
-- Cot proposed_rate cu duoc giu lai (= muc cao nhat trong cac mon) cho don cu va cham diem AI.
ALTER TABLE tutor_applications
    ADD COLUMN proposed_rates_json TEXT NULL AFTER proposed_rate;
