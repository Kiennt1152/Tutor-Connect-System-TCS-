ALTER TABLE reviews
    ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE AFTER criteria_json,
    ADD COLUMN display_name VARCHAR(100) NULL AFTER is_anonymous;
