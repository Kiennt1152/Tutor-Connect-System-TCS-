-- UC "Xác nhận lớp đã hoàn thành": tutor & client mỗi bên xác nhận riêng.
-- Khi cả hai đã xác nhận -> lớp COMPLETED + giải ngân escrow cho gia sư.
ALTER TABLE class_assignments
    ADD COLUMN tutor_completed_at DATETIME NULL AFTER client_signed_at,
    ADD COLUMN client_completed_at DATETIME NULL AFTER tutor_completed_at;
