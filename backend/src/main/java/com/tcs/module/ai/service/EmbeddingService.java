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

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    public Optional<double[]> getEmbedding(String text) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Skipping embedding.");
            return Optional.empty();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + geminiApiKey;
            
            String reqBody = buildEmbeddingPayload(text);

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
                    return Optional.of(embedding);
                }
            } else {
                log.error("Gemini embedding API failed with status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
        }
        return Optional.empty();
    }

    private String buildEmbeddingPayload(String text) throws Exception {
        Map<String, Object> payload = Map.of(
            "model", "models/text-embedding-004",
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
