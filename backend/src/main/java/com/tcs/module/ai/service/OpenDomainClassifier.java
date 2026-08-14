package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OpenDomainClassifier {

    public record OpenDomainResult(
        AiSubIntent subIntent,
        double confidence,
        Map<String, String> extractedData
    ) {}

    private static final Pattern BASIC_MATH_PATTERN = Pattern.compile("(?i)(\\d+\\s*[+\\-*/÷×^%x]\\s*\\d+|\\b(\\d+)\\s*(cộng|trừ|nhân|chia|mu|luy thua)\\s*(\\d+)|bằng mấy|bang may|=\\s*\\?|equals|giải phương trình|giai phuong trinh|solve equation)");

    public OpenDomainResult classifyOpen(String query) {
        if (query == null || query.isBlank()) {
            return new OpenDomainResult(AiSubIntent.OUT_OF_SCOPE, 0.0, Map.of());
        }

        String lower = query.toLowerCase(Locale.ROOT).trim();
        String normalized = VietnameseTextNormalizer.removeDiacritics(lower);

        // 1. MATH_CALCULATION (highest priority for arithmetic & equations)
        if (isMathExpression(lower, normalized)) {
            Map<String, String> data = new HashMap<>();
            data.put("expression", query);
            return new OpenDomainResult(AiSubIntent.MATH_CALCULATION, 0.95, data);
        }

        // 2. TIME_DATE_QUERY
        if (containsAny(normalized,
                "hom nay la ngay", "hom nay ngay may", "hom nay thu may", "hom nay ngay bao nhieu", "ngay bao nhieu",
                "bay gio la may gio", "may gio roi", "may gio", "gio hien tai", "ngay hien tai", "thoi gian hien tai",
                "what time", "what date", "what day is today", "current time", "today's date", "time now")) {
            Map<String, String> data = new HashMap<>();
            data.put("queryType", "TIME_DATE");
            return new OpenDomainResult(AiSubIntent.TIME_DATE_QUERY, 0.95, data);
        }

        // 3. WEATHER_QUERY
        if (containsAny(normalized,
                "thoi tiet", "du bao thoi tiet", "troi co mua khong", "troi mua", "troi nang", "nhiet do",
                "weather", "weather forecast", "is it raining", "temperature today")) {
            Map<String, String> data = new HashMap<>();
            data.put("location", extractWeatherLocation(lower, normalized));
            return new OpenDomainResult(AiSubIntent.WEATHER_QUERY, 0.9, data);
        }

        // 4. DEFINITION_LOOKUP
        if (containsAny(normalized, "nghia la gi", "dinh nghia", "khai niem", "la gi the", "nghia cua tu", "what is the meaning of", "definition of")) {
            Map<String, String> data = new HashMap<>();
            data.put("term", query);
            return new OpenDomainResult(AiSubIntent.DEFINITION_LOOKUP, 0.9, data);
        }

        // 5. ENTERTAINMENT
        if (containsAny(normalized, "ke chuyen cuoi", "ke chuyen", "do vui", "lam tho", "hat mot bai", "tell me a joke", "tell a story", "riddle")) {
            Map<String, String> data = new HashMap<>();
            data.put("topic", query);
            return new OpenDomainResult(AiSubIntent.ENTERTAINMENT, 0.85, data);
        }

        // 6. NEWS_CURRENT_EVENTS
        if (containsAny(normalized, "tin tuc hom nay", "thoi su", "gia vang", "gia xang", "chung khoan", "tin tuc moi nhat", "latest news")) {
            Map<String, String> data = new HashMap<>();
            data.put("topic", query);
            return new OpenDomainResult(AiSubIntent.NEWS_CURRENT_EVENTS, 0.85, data);
        }

        // 7. GENERAL_KNOWLEDGE
        if (containsAny(normalized,
                "thu do cua", "thu do nuoc", "dan so", "dien tich", "ai la nguoi", "ai phat minh",
                "vi sao", "tai sao lai", "nuoc nao", "o dau tren the gioi", "capital of", "who invented", "why is")) {
            Map<String, String> data = new HashMap<>();
            data.put("question", query);
            return new OpenDomainResult(AiSubIntent.GENERAL_KNOWLEDGE, 0.85, data);
        }

        // Default: generic out of scope
        return new OpenDomainResult(AiSubIntent.OUT_OF_SCOPE, 0.3, Map.of());
    }

    private boolean isMathExpression(String lower, String normalized) {
        if (BASIC_MATH_PATTERN.matcher(lower).find() || BASIC_MATH_PATTERN.matcher(normalized).find()) {
            return true;
        }
        return containsAny(normalized, "tinh giup", "tinh ho", "giai toan", "can bac hai", "sqrt");
    }

    private String extractWeatherLocation(String lower, String normalized) {
        if (normalized.contains("ha noi")) return "Hà Nội";
        if (normalized.contains("ho chi minh") || normalized.contains("hcm") || normalized.contains("sai gon")) return "TP.HCM";
        if (normalized.contains("da nang")) return "Đà Nẵng";
        if (normalized.contains("hai phong")) return "Hải Phòng";
        if (normalized.contains("can tho")) return "Cần Thơ";
        return "khu vực của bạn";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
