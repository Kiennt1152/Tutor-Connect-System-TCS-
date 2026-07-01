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
 * Xac thuc Google OAuth2 access token (do luong initTokenClient tra ve tren frontend).
 * - tokeninfo: kiem tra `aud` khop client id cua ta + email da xac thuc.
 * - userinfo: lay ten hien thi (best-effort).
 * Khong can them dependency (dung java.net.http + Jackson co san).
 */
@Component
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?access_token=";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.google.client-id:}")
    private String clientId;

    /** Tra ve thong tin nguoi dung tu Google, hoac nem loi neu token khong hop le. */
    public GooglePayload verify(String accessToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                    "Đăng nhập Google chưa được cấu hình (thiếu app.google.client-id ở backend).");
        }

        // 1) tokeninfo: token phai duoc cap cho dung client id cua ta.
        JsonNode info = getJson(TOKENINFO_URL + URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
        String aud = info.path("aud").asText("");
        if (!clientId.equals(aud)) {
            throw new IllegalArgumentException("Google token không dành cho ứng dụng này.");
        }
        String email = info.path("email").asText(null);
        boolean emailVerified = info.path("email_verified").asBoolean(false)
                || "true".equalsIgnoreCase(info.path("email_verified").asText(""));
        if (email == null || email.isBlank() || !emailVerified) {
            throw new IllegalArgumentException("Email Google chưa được xác thực.");
        }

        // 2) userinfo: lay ten hien thi (neu that bai van cho dang nhap, ten se lay tu email).
        String name = "";
        try {
            JsonNode userInfo = getJsonWithBearer(USERINFO_URL, accessToken);
            name = userInfo.path("name").asText("");
        } catch (RuntimeException ignored) {
            // best-effort: bo qua neu khong lay duoc ten.
        }

        return new GooglePayload(email, name);
    }

    private JsonNode getJson(String url) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET().build());
    }

    private JsonNode getJsonWithBearer(String url, String accessToken) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build());
    }

    private JsonNode send(HttpRequest request) {
        try {
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

    /** Thong tin toi thieu lay tu Google. */
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
