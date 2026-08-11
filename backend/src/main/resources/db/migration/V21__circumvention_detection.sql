CREATE TABLE circumvention_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    matched_rule VARCHAR(30) NOT NULL,
    evidence VARCHAR(500) NOT NULL,
    risk_score INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    review_note VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_circumvention_message FOREIGN KEY (message_id) REFERENCES messages(message_id),
    CONSTRAINT fk_circumvention_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id),
    CONSTRAINT fk_circumvention_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT fk_circumvention_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CONSTRAINT uq_circumvention_message_rule UNIQUE (message_id, matched_rule)
);

CREATE INDEX idx_circumvention_status_created ON circumvention_events(status, created_at);
