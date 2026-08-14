-- UC "Xác nhận lớp đã hoàn thành": gia sư yêu cầu hoàn thành -> phụ huynh/học viên phản hồi.
-- Nếu phụ huynh/học viên chọn "chưa hoàn thành" thì bắt buộc kèm lý do gửi cho gia sư.
ALTER TABLE class_assignments
    ADD COLUMN client_reject_reason VARCHAR(500) NULL AFTER client_completed_at;
