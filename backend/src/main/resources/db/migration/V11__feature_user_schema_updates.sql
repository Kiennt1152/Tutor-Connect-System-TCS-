-- V11: Feature User Schema Updates (UC-36 Chat, AI Assistant, Support Tickets SLA/Thread Reply)

-- =========================================================
-- 1. UC-36: Chat with Users (Messenger-style)
-- =========================================================
ALTER TABLE conversations
    ADD COLUMN last_message_preview VARCHAR(200) NULL AFTER last_message_at,
    ADD COLUMN last_message_sender_id BIGINT NULL AFTER last_message_preview;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_last_sender
        FOREIGN KEY (last_message_sender_id) REFERENCES users (user_id);

ALTER TABLE conversation_participants
    ADD COLUMN last_read_at DATETIME NULL AFTER user_id;

-- =========================================================
-- 2. AI Assistant (Groq) Sessions & Messages
-- =========================================================
CREATE TABLE ai_chat_sessions (
    session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    title VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_sessions_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE ai_chat_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    referenced_tutor_ids VARCHAR(255) NULL,
    referenced_class_ids VARCHAR(255) NULL,
    referenced_faq_ids VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_messages_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions (session_id) ON DELETE CASCADE
);

-- =========================================================
-- 3. Support Tickets (BF-09) SLA & Thread Reply
-- =========================================================
ALTER TABLE support_tickets
    ADD COLUMN due_at DATETIME NULL AFTER status,
    ADD COLUMN sla_breached BOOLEAN NULL AFTER due_at,
    ADD COLUMN response_sla_ms BIGINT NULL AFTER sla_breached;

CREATE TABLE ticket_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    is_from_admin BOOLEAN NOT NULL DEFAULT FALSE,
    content TEXT NOT NULL,
    evidence_urls TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (ticket_id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_messages_sender FOREIGN KEY (sender_id) REFERENCES users (user_id)
);
