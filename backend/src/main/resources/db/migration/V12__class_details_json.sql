-- =====================================================================
-- Lưu toàn bộ dữ liệu form đăng yêu cầu (nhiều môn, lịch từng thứ, tỉnh/
-- quận/phường, ...) dưới dạng JSON snapshot để màn Sửa khôi phục nguyên vẹn.
-- Các cột có sẵn (subject_id, grade_id, address, ...) vẫn dùng để hiển thị
-- thẻ; details_json là nguồn để nạp lại form khi chỉnh sửa.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE tutoring_classes
    ADD COLUMN details_json TEXT NULL AFTER description;
