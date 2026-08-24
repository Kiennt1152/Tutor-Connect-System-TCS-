package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;

/**
 * Strategy interface for domain-specific intent classification rules.
 */
public interface IntentRule {

    /**
     * Try to classify the normalized query.
     * @param normalized query after accented diacritic removal and teencode expansion
     * @param lower raw lowercased query
     * @return ClassificationDetail if matched, or null otherwise
     */
    ClassificationDetail classify(String normalized, String lower);

    /**
     * Priority of rule execution (lower number = higher priority).
     */
    default int priority() {
        return 100;
    }
}
