package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Registry and orchestrator for all domain-specific IntentRule strategy components.
 */
@Slf4j
@Component
public class IntentRuleRegistry {

    private final List<IntentRule> rules;

    public IntentRuleRegistry(List<IntentRule> rules) {
        if (rules != null) {
            this.rules = rules.stream()
                .sorted(Comparator.comparingInt(IntentRule::priority))
                .toList();
        } else {
            this.rules = Collections.emptyList();
        }
        log.info("Registered {} intent classification rules in strategy registry", this.rules.size());
    }

    /**
     * Evaluate rules in priority order until the first matching rule produces a result.
     * @return ClassificationDetail or null if no rule matched
     */
    public ClassificationDetail evaluate(String normalized, String lower) {
        for (IntentRule rule : rules) {
            ClassificationDetail result = rule.classify(normalized, lower);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public List<IntentRule> getRules() {
        return rules;
    }
}
