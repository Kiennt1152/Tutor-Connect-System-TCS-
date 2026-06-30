-- =====================================================================
-- TCS - Nhung truong khong con thu thap luc dang ky tro thanh tuy chon:
--   - Gia su: gioi tinh (gender)
--   - Trung tam: so giay phep (license_no), dia chi (address)
-- Cac thong tin nay duoc bo sung sau khi dang nhap.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE tutors
    MODIFY COLUMN gender VARCHAR(10) NULL;

ALTER TABLE tutor_centers
    MODIFY COLUMN license_no VARCHAR(50) NULL,
    MODIFY COLUMN address TEXT NULL;
