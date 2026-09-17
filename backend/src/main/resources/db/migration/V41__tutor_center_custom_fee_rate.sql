-- V41: Add custom_fee_rate column to tutor_centers table for custom center platform fee overrides
ALTER TABLE tutor_centers 
    ADD COLUMN custom_fee_rate DECIMAL(5, 4) NULL DEFAULT NULL AFTER avatar;
