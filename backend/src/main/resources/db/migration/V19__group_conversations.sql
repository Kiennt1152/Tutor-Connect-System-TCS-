-- V19: Group conversations with an explicit owner and display name.

ALTER TABLE conversations
    ADD COLUMN name VARCHAR(80) NULL AFTER type,
    ADD COLUMN owner_user_id BIGINT NULL AFTER name;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_owner
        FOREIGN KEY (owner_user_id) REFERENCES users (user_id),
    ADD CONSTRAINT chk_group_conversation_metadata
        CHECK (type <> 'GROUP' OR (name IS NOT NULL AND owner_user_id IS NOT NULL));

CREATE INDEX idx_conversations_type_owner
    ON conversations (type, owner_user_id);
