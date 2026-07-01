package com.tcs.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Xac thuc Google ID token qua endpoint tokeninfo cua Google (khong can them dependency).
 * Google se kiem tra chu ky + han su dung; ta kiem tra them `aud` khop client id va email da xac thuc.
 */
@Component
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.google.client-id:}")
    private String clientId;

    /** Tra ve thong tin nguoi dung tu Google, hoac nem loi neu token khong hop le. */
    public GooglePayload verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                    "Đăng nhập Google chưa được cấu hình (thiếu app.google.client-id ở backend).");
        }
        JsonNode node = callTokenInfo(idToken);

        String aud = node.path("aud").asText("");
        if (!clientId.equals(aud)) {
            throw new IllegalArgumentException("Google token không dành cho ứng dụng này.");
        }

        String email = node.path("email").asText(null);
        boolean emailVerified = node.path("email_verified").asBoolean(false)
                || "true".equalsIgnoreCase(node.path("email_verified").asText(""));
        if (email == null || email.isBlank() || !emailVerified) {
            throw new IllegalArgumentException("Email Google chưa được xác thực.");
        }

        return new GooglePayload(email, node.path("name").asText(""));
    }

    private JsonNode callTokenInfo(String idToken) {
        try {
            String url = TOKENINFO_URL + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Google token không hợp lệ hoặc đã hết hạn.");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Không kết nối được tới Google để xác thực token.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Xác thực Google token bị gián đoạn.", e);
        }
    }

    /** Thong tin toi thieu lay tu Google ID token. */
    @Getter
    public static class GooglePayload {
        private final String email;
        private final String name;

        public GooglePayload(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }
}
