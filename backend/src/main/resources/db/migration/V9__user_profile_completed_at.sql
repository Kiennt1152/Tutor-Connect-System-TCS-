-- =====================================================================
-- V9: track first-login onboarding for new users (UC-08 BR-UC08-01).
-- profile_completed_at NULL = user chua hoan tat ho so lan nao.
-- Khi ProfileService.updateMyProfile luu thanh cong, set = now().
-- Frontend su dung AuthResponse.firstLogin = (profile_completed_at IS NULL)
-- de redirect /profile va hien banner onboarding.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE users
    ADD COLUMN profile_completed_at DATETIME NULL AFTER last_login;

-- Backfill: user cu (da tung login) mac dinh la da hoan tat ho so.
UPDATE users
SET profile_completed_at = COALESCE(last_login, created_at)
WHERE profile_completed_at IS NULL;