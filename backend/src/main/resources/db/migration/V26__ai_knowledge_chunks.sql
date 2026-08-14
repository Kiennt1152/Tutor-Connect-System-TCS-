-- BF-09/BF-10 consolidated migration for AI knowledge chunks and platform operations indexes.
-- Intended for clean database rebuild before commit.

CREATE TABLE ai_knowledge_chunks (
    chunk_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    title VARCHAR(500),
    content LONGTEXT NOT NULL,
    metadata_json JSON,
    embedding_json JSON,
    embedding_model VARCHAR(100),
    content_hash VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    min_role VARCHAR(30) NULL,
    owner_user_id BIGINT NULL,
    source_status VARCHAR(50) NULL,
    source_updated_at DATETIME NULL,
    last_indexed_at DATETIME NULL,
    quality_score DOUBLE NULL,
    token_count INT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'vi',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_type_id (source_type, source_id),
    INDEX idx_source_type (source_type),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE user_penalties
    ADD COLUMN source_type VARCHAR(50) NULL,
    ADD COLUMN source_id BIGINT NULL,
    ADD COLUMN source_task_id VARCHAR(100) NULL;

CREATE INDEX idx_users_created_status ON users(created_at, status);
CREATE INDEX idx_tutoring_class_status ON tutoring_classes(status, created_at);
CREATE INDEX idx_payment_tx_type_status ON payment_transactions(type, status, created_at);
CREATE INDEX idx_support_ticket_status_pri ON support_tickets(status, priority, due_at);
CREATE INDEX idx_reports_status ON reports(status, created_at);
CREATE INDEX idx_disputes_status ON disputes(status, created_at);
CREATE INDEX idx_withdrawal_status ON withdrawal_requests(status, requested_at);
CREATE INDEX idx_escrow_status ON escrow_transactions(status, created_at);

-- Support PLATFORM_FEE in payment transaction types
ALTER TABLE payment_transactions DROP CHECK chk_payment_transactions_type;
ALTER TABLE payment_transactions ADD CONSTRAINT chk_payment_transactions_type 
    CHECK (type IN ('DEPOSIT','WITHDRAWAL','REFUND','ESCROW_DEPOSIT','ESCROW_RELEASE','PLATFORM_FEE'));
