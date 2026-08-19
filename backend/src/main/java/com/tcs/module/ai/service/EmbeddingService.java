package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, double[]> embeddingCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.embedding-model:gemini-embedding-001}")
    private String configuredEmbeddingModel;

    private static final List<String> CANDIDATE_MODELS = List.of(
        "gemini-embedding-001",
        "embedding-001",
        "text-embedding-004"
    );

    public Optional<double[]> getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = text.trim();
        if (embeddingCache.containsKey(cacheKey)) {
            return Optional.of(embeddingCache.get(cacheKey));
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Skipping embedding.");
            return Optional.empty();
        }

        List<String> modelsToTry = new java.util.ArrayList<>();
        if (configuredEmbeddingModel != null && !configuredEmbeddingModel.isBlank()) {
            modelsToTry.add(configuredEmbeddingModel.trim());
        }
        for (String m : CANDIDATE_MODELS) {
            if (!modelsToTry.contains(m)) {
                modelsToTry.add(m);
            }
        }

        for (String modelName : modelsToTry) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":embedContent?key=" + geminiApiKey;
                String reqBody = buildEmbeddingPayload(modelName, cacheKey);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode values = root.path("embedding").path("values");
                    if (values.isArray()) {
                        double[] embedding = new double[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            embedding[i] = values.get(i).asDouble();
                        }
                        if (embeddingCache.size() > MAX_CACHE_SIZE) {
                            embeddingCache.clear();
                        }
                        embeddingCache.put(cacheKey, embedding);
                        return Optional.of(embedding);
                    }
                } else if (response.statusCode() == 404) {
                    log.warn("Gemini embedding model '{}' not found (404), trying fallback...", modelName);
                } else {
                    log.error("Gemini embedding API with model '{}' failed with status {}: {}", modelName, response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.error("Failed to generate embedding with model '{}': {}", modelName, e.getMessage());
            }
        }

        return Optional.empty();
    }

    private String buildEmbeddingPayload(String modelName, String text) throws Exception {
        Map<String, Object> payload = Map.of(
            "model", "models/" + modelName,
            "content", Map.of(
                "parts", List.of(
                    Map.of("text", text)
                )
            )
        );
        return objectMapper.writeValueAsString(payload);
    }

    public double cosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    public double[] parseEmbeddingJson(String json) {
        try {
            if (json == null || json.isEmpty()) return new double[0];
            JsonNode root = objectMapper.readTree(json);
            if (root.isArray()) {
                double[] arr = new double[root.size()];
                for (int i = 0; i < root.size(); i++) {
                    arr[i] = root.get(i).asDouble();
                }
                return arr;
            }
        } catch (Exception e) {
            log.error("Failed to parse embedding json", e);
        }
        return new double[0];
    }
}
