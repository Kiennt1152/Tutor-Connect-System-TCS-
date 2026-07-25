package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.service.GeminiService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private static final String SYSTEM_PROMPT =
            "Ban la tro ly ho tro cua he thong Tutor Connect System (ket noi hoc vien va gia su). "
            + "Hay tra loi ngan gon, thien thien, chinh xac bang tieng Viet. "
            + "Neu cau hoi khong lien quan den hoc tap/gia su/he thong, hay tra loi lich su rang ban "
            + "chi ho tro cac van de lien quan den Tutor Connect System.";

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${app.gemini.timeout-ms:8000}")
    private int timeoutMs;

    private RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
            requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
            restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(requestFactory)
                    .build();
        }
        return restClient;
    }

    @Override
    public Optional<String> askQuestion(String question) {
        if (!StringUtils.hasText(apiKey)) {
            log.debug("Gemini API key chua duoc cau hinh, bo qua fallback AI.");
            return Optional.empty();
        }
        if (!StringUtils.hasText(question)) {
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ),
                    "contents", List.of(
                            Map.of("role", "user", "parts", List.of(Map.of("text", question)))
                    )
            );

            GeminiResponse response = client().post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractAnswer(response);
        } catch (RestClientException ex) {
            log.warn("Goi Gemini API loi: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractAnswer(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }
        Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return Optional.empty();
        }
        String text = candidate.content().parts().get(0).text();
        return StringUtils.hasText(text) ? Optional.of(text.trim()) : Optional.empty();
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}
