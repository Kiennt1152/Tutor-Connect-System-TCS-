ALTER TABLE reviews
    ADD COLUMN tutor_reply TEXT NULL AFTER comment,
    ADD COLUMN tutor_reply_at DATETIME NULL AFTER tutor_reply;
