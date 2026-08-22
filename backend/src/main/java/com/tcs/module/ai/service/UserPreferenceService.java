package com.tcs.module.ai.service;

import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserPreferenceService {

    public record UserPreferences(
        Long userId,
        Set<String> preferredSubjects,
        Set<String> preferredGrades,
        Set<String> preferredLocations,
        String preferredMode,
        Long maxBudget,
        int interactionCount,
        Instant lastUpdated
    ) {}

    private final Map<Long, UserPreferences> preferencesStore = new ConcurrentHashMap<>();

    public void updateFromInteraction(Long userId, Map<String, String> entities, String query) {
        if (userId == null) return;

        UserPreferences current = preferencesStore.getOrDefault(
            userId,
            new UserPreferences(userId, new HashSet<>(), new HashSet<>(), new HashSet<>(), null, null, 0, Instant.now())
        );

        Set<String> subjects = new HashSet<>(current.preferredSubjects());
        Set<String> grades = new HashSet<>(current.preferredGrades());
        Set<String> locations = new HashSet<>(current.preferredLocations());
        String mode = current.preferredMode();
        Long budget = current.maxBudget();

        if (entities != null) {
            if (entities.containsKey("subject")) {
                subjects.add(entities.get("subject"));
            }
            if (entities.containsKey("grade")) {
                grades.add(entities.get("grade"));
            }
            if (entities.containsKey("location")) {
                locations.add(entities.get("location"));
            }
            if (entities.containsKey("mode")) {
                mode = entities.get("mode");
            }
            if (entities.containsKey("maxFee")) {
                try {
                    budget = Long.parseLong(entities.get("maxFee"));
                } catch (Exception ignored) {}
            }
        }

        UserPreferences updated = new UserPreferences(
            userId,
            subjects,
            grades,
            locations,
            mode,
            budget,
            current.interactionCount() + 1,
            Instant.now()
        );

        preferencesStore.put(userId, updated);
        log.debug("Updated preferences for user {}: subjects={}, grades={}, locations={}, budget={}",
                  userId, subjects, grades, locations, budget);
    }

    public Optional<UserPreferences> getPreferences(Long userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(preferencesStore.get(userId));
    }

    public Map<String, String> enrichWithPreferences(Long userId, Map<String, String> currentEntities) {
        Map<String, String> enriched = currentEntities != null ? new HashMap<>(currentEntities) : new HashMap<>();
        if (userId == null) return enriched;

        Optional<UserPreferences> prefOpt = getPreferences(userId);
        if (prefOpt.isEmpty()) return enriched;

        UserPreferences pref = prefOpt.get();

        // If user didn't specify subject, inject the most frequently preferred subject
        if (!enriched.containsKey("subject") && !pref.preferredSubjects().isEmpty()) {
            enriched.put("preferredSubject", pref.preferredSubjects().iterator().next());
        }

        // If user didn't specify grade, inject preferred grade
        if (!enriched.containsKey("grade") && !pref.preferredGrades().isEmpty()) {
            enriched.put("preferredGrade", pref.preferredGrades().iterator().next());
        }

        // If user didn't specify location, inject preferred location
        if (!enriched.containsKey("location") && !pref.preferredLocations().isEmpty()) {
            enriched.put("preferredLocation", pref.preferredLocations().iterator().next());
        }

        if (!enriched.containsKey("maxFee") && pref.maxBudget() != null) {
            enriched.put("preferredMaxFee", String.valueOf(pref.maxBudget()));
        }

        return enriched;
    }

    public void clear(Long userId) {
        if (userId != null) {
            preferencesStore.remove(userId);
        }
    }

    public void clearAll() {
        preferencesStore.clear();
    }
}
