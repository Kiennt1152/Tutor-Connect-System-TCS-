package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class AiFallbackMatrixTest {

    private IntentClassifier classifier;
    private AiCapabilityRouter router;
    private AiFallbackService fallbackService;

    public record MatrixRow(
        AiDomain domain,
        AiSubIntent subIntent,
        String exampleQuery,
        AiIntent expectedLegacyIntent,
        String requiredRole,
        String allowedSourceTypes,
        String expectedRoute,
        AiCapabilityRouter.CardPolicy expectedCardPolicy,
        String fallbackType,
        boolean requiresDbSource,
        String negativeCase
    ) {}

    @BeforeEach
    void setUp() {
        classifier = new IntentClassifier();
        router = new AiCapabilityRouter();
        fallbackService = new AiFallbackService();
    }

    static List<MatrixRow> loadCoverageMatrix() throws Exception {
        List<MatrixRow> rows = new ArrayList<>();
        var is = AiFallbackMatrixTest.class.getResourceAsStream("/ai/ai_intent_coverage_matrix.csv");
        assertThat(is).as("ai_intent_coverage_matrix.csv must exist in test resources").isNotNull();

        try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 11) continue;

                rows.add(new MatrixRow(
                    AiDomain.valueOf(parts[0].trim()),
                    AiSubIntent.valueOf(parts[1].trim()),
                    parts[2].trim(),
                    AiIntent.valueOf(parts[3].trim()),
                    parts[4].trim(),
                    parts[5].trim(),
                    parts[6].trim().isEmpty() ? null : parts[6].trim(),
                    AiCapabilityRouter.CardPolicy.valueOf(parts[7].trim()),
                    parts[8].trim(),
                    Boolean.parseBoolean(parts[9].trim()),
                    parts[10].trim()
                ));
            }
        }
        return rows;
    }

    @ParameterizedTest(name = "{index} => Domain: {0}, SubIntent: {1}, Query: {2}")
    @MethodSource("loadCoverageMatrix")
    @DisplayName("Matrix Row: Positive Query Classification & Policy Matching")
    void testPositiveQueryFromMatrix(MatrixRow row) {
        var detail = classifier.classifyDetailed(row.exampleQuery());

        assertThat(detail.domain())
            .as("Domain mismatch for query: '%s'", row.exampleQuery())
            .isEqualTo(row.domain());

        assertThat(detail.subIntent())
            .as("SubIntent mismatch for query: '%s'", row.exampleQuery())
            .isEqualTo(row.subIntent());

        var policy = router.getPolicy(row.domain(), row.subIntent());
        assertThat(policy).isNotNull();

        if (row.expectedRoute() != null) {
            assertThat(policy.deepLinkRoute())
                .as("Route mismatch in policy for %s/%s", row.domain(), row.subIntent())
                .isEqualTo(row.expectedRoute());
        }

        assertThat(policy.cardPolicy())
            .as("CardPolicy mismatch for %s/%s", row.domain(), row.subIntent())
            .isEqualTo(row.expectedCardPolicy());

        assertThat(policy.requireDbSource())
            .as("requireDbSource mismatch for %s/%s", row.domain(), row.subIntent())
            .isEqualTo(row.requiresDbSource());
    }

    @ParameterizedTest(name = "{index} => Domain: {0}, Negative/Unaccented: {10}")
    @MethodSource("loadCoverageMatrix")
    @DisplayName("Matrix Row: Negative / Unaccented Query Classification")
    void testNegativeOrUnaccentedQueryFromMatrix(MatrixRow row) {
        if (row.negativeCase() == null || row.negativeCase().isBlank()) return;

        var detail = classifier.classifyDetailed(row.negativeCase());

        assertThat(detail.domain())
            .as("Domain mismatch for negative/unaccented query: '%s'", row.negativeCase())
            .isEqualTo(row.domain());

        assertThat(detail.subIntent())
            .as("SubIntent mismatch for negative/unaccented query: '%s'", row.negativeCase())
            .isEqualTo(row.subIntent());
    }
}
