-- =====================================================================
-- TCS - Trang thai xac minh ho so chuyen mon (BR-09 / BF-02).
-- Tai khoan tao ra o trang thai ACTIVE nhung ho so Gia su / Trung tam
-- giu verification_status = PENDING cho toi khi nop va duyet ho so.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE tutors
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER rating_avg,
    ADD CONSTRAINT chk_tutors_verification_status
        CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED'));

ALTER TABLE tutor_centers
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER description,
    ADD CONSTRAINT chk_tutor_centers_verification_status
        CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED'));
