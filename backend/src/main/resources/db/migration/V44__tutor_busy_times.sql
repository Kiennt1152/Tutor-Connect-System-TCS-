-- V44: Thời gian bận của gia sư, đăng ký theo ngày cụ thể (xem theo tháng).
--
-- Mô hình: gia sư CHỈ khai báo lúc bận. Ngày/giờ nào không có lịch dạy và không nằm trong
-- bảng này thì mặc định là RẢNH — nên không cần bảng "lịch rảnh" riêng.
--
-- start_time / end_time cùng NULL = bận CẢ NGÀY. end_time = '00:00:00' là mốc NỬA ĐÊM
-- (24:00 của chính ngày đó), theo quy ước V42 — so sánh qua com.tcs.common.util.SlotTime.
--
-- Bảng tutor_availabilities (V3) giữ nguyên: nghĩa ngược lại (lịch rảnh lặp theo thứ) và
-- chưa có dữ liệu hay màn hình nào dùng.

CREATE TABLE IF NOT EXISTS tutor_busy_times (
    busy_time_id BIGINT       NOT NULL AUTO_INCREMENT,
    tutor_id     BIGINT       NOT NULL,
    busy_date    DATE         NOT NULL,
    start_time   TIME         NULL,
    end_time     TIME         NULL,
    note         VARCHAR(255) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_tutor_busy_times PRIMARY KEY (busy_time_id),
    CONSTRAINT fk_tutor_busy_times_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (tutor_id),
    -- Giờ bắt đầu/kết thúc phải cùng có hoặc cùng trống (trống = cả ngày).
    CONSTRAINT chk_tutor_busy_times_range CHECK (
        (start_time IS NULL AND end_time IS NULL) OR (start_time IS NOT NULL AND end_time IS NOT NULL)
    ),
    -- Truy vấn chính: một gia sư, một khoảng ngày (một tháng).
    INDEX idx_tutor_busy_times_tutor_date (tutor_id, busy_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
