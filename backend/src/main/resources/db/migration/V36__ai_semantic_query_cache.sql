-- V36: AI Semantic Query Cache
-- This migration adds semantic caching for AI chatbot responses

CREATE TABLE IF NOT EXISTS ai_query_cache (
    cache_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_text TEXT NOT NULL,
    query_hash VARCHAR(64) NOT NULL,
    normalized_query TEXT,
    embedding_json JSON,
    response_content TEXT NOT NULL,
    
    intent VARCHAR(50),
    domain VARCHAR(50),
    sub_intent VARCHAR(50),
    confidence_score DOUBLE,
    source_count INT,
    
    referenced_tutor_ids TEXT,
    referenced_class_ids TEXT,
    referenced_faq_ids TEXT,
    
    user_role VARCHAR(30),
    hit_count INT NOT NULL DEFAULT 0,
    last_hit_at DATETIME,
    expires_at DATETIME,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_cache_hash (query_hash),
    INDEX idx_cache_created (created_at),
    INDEX idx_cache_hits (hit_count),
    INDEX idx_cache_expires (expires_at),
    INDEX idx_cache_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment
ALTER TABLE ai_query_cache COMMENT = 'Semantic cache for AI chatbot query responses to improve performance';
