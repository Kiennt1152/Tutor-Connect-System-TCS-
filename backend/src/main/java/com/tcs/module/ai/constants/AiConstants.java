package com.tcs.module.ai.constants;

/**
 * Centralized constants for the AI Module across retrieval, scoring, caching, and policies.
 */
public final class AiConstants {
    
    // Visibility / Permission Levels
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_OWNER_PRIVATE = "OWNER_PRIVATE";
    public static final String VISIBILITY_ROLE_RESTRICTED = "ROLE_RESTRICTED";
    public static final String VISIBILITY_ADMIN_ONLY = "ADMIN_ONLY";
    
    // Confidence & Rerank Thresholds
    public static final double MIN_RETRIEVAL_SCORE = 0.60;
    public static final double MIN_REFERENCE_CARD_SCORE = 0.65;
    public static final double SEMANTIC_CACHE_THRESHOLD = 0.92;
    public static final double FEW_SHOT_SIMILARITY_THRESHOLD = 0.50;
    public static final double HIGH_CONFIDENCE_THRESHOLD = 0.70;

    // Cache Settings
    public static final int SEMANTIC_CACHE_TTL_HOURS = 24;
    public static final int MAX_CACHE_ENTRIES = 1000;
    public static final int MAX_EMBEDDING_CACHE_ENTRIES = 1000;

    // Timeouts & Cooldowns
    public static final int LLM_TIMEOUT_SECONDS = 15;
    public static final int PROVIDER_COOLDOWN_SECONDS = 300;
    public static final int MAX_SOURCES_PER_QUERY = 4;
    public static final int MAX_REFERENCE_CARDS = 3;

    private AiConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
