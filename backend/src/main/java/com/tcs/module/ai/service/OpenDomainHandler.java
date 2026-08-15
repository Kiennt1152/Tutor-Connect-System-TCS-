package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiSubIntent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenDomainHandler {

    private final WeatherService weatherService;
    private final ContentSafetyFilter contentSafetyFilter;

    public record OpenDomainResponse(
        String answer,
        String steeringMessage,
        String suggestedRoute,
        List<String> ctaButtons
    ) {
        public String formatFullResponse() {
            if (steeringMessage == null || steeringMessage.isBlank()) {
                return answer;
            }
            return answer + "\n\n💡 " + steeringMessage;
        }
    }

    private static final Pattern SIMPLE_ARITHMETIC = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([+\\-*/÷×^%])\\s*([0-9]+(?:\\.[0-9]+)?)");
    private final Map<String, String> responseCache = new ConcurrentHashMap<>();

    public OpenDomainHandler() {
        this.weatherService = new WeatherService();
        this.contentSafetyFilter = new ContentSafetyFilter();
    }

    public OpenDomainResponse handle(AiSubIntent subIntent, String query, Map<String, String> extractedData) {
        if (subIntent == null) {
            subIntent = AiSubIntent.GENERAL_KNOWLEDGE;
        }

        // 1. Content Safety Check First
        ContentSafetyFilter.SafetyCheckResult safety = contentSafetyFilter.checkQuery(query);
        if (!safety.isSafe()) {
            return new OpenDomainResponse(
                safety.suggestedResponse(),
                null,
                safety.isCrisis() ? "/help" : "/tim-gia-su",
                safety.isCrisis()
                    ? List.of("Tổng đài Trẻ em (111)", "Đường dây nóng Sức khỏe Tâm thần (1800 599 920)", "Trung tâm trợ giúp (/help)")
                    : List.of("Tìm gia sư uy tín (/tim-gia-su)", "Quy tắc cộng đồng (/help)")
            );
        }

        return switch (subIntent) {
            case MATH_CALCULATION -> handleMath(extractedData.getOrDefault("expression", query));
            case TIME_DATE_QUERY -> handleTimeDate(query);
            case WEATHER_QUERY -> handleWeather(extractedData.getOrDefault("location", "Hà Nội"));
            case DEFINITION_LOOKUP -> handleDefinition(extractedData.getOrDefault("term", query));
            case ENTERTAINMENT -> handleEntertainment(extractedData.getOrDefault("topic", query));
            case NEWS_CURRENT_EVENTS -> handleNews(extractedData.getOrDefault("topic", query));
            default -> handleGeneralKnowledge(query);
        };
    }

    public OpenDomainResponse handleMath(String expression) {
        String cacheKey = "math:" + (expression != null ? expression.trim().toLowerCase() : "");
        String cachedAnswer = responseCache.get(cacheKey);

        String answer = (cachedAnswer != null) ? cachedAnswer : computeSimpleMath(expression);
        if (cachedAnswer == null && expression != null) {
            responseCache.put(cacheKey, answer);
        }

        // Smart Steering: Only steer if it is complex math/algebra/calculus
        boolean isComplexMath = expression != null && (
            expression.contains("phương trình") || 
            expression.contains("đạo hàm") ||
            expression.contains("tích phân") ||
            expression.contains("hệ phương trình") ||
            expression.matches(".*[x|y|z].*[²³⁴].*") ||
            expression.length() > 20
        );

        if (isComplexMath) {
            return new OpenDomainResponse(
                answer,
                "Nếu bạn cần giải thích chi tiết phương pháp giải, tôi sẵn sàng hướng dẫn từng bước. Hoặc bạn có thể tìm gia sư Toán để học sâu hơn.",
                "/tim-gia-su?subject=Toán",
                List.of("Tìm gia sư Toán (/tim-gia-su)", "Xem lớp Toán đang mở (/lop-hoc)", "Tạo lớp tìm gia sư (/tao-lop)")
            );
        } else {
            // Simple arithmetic: NO STEERING, clean concise answer
            return new OpenDomainResponse(
                answer,
                null,
                null,
                List.of()
            );
        }
    }

    public OpenDomainResponse handleTimeDate(String query) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, 'ngày' dd/MM/yyyy, HH:mm", Locale.of("vi", "VN"));
        String answer = "Thời gian hiện tại: **" + now.format(fmt) + "**.";
        
        // Pure time query: No unsolicited marketing
        return new OpenDomainResponse(
            answer,
            null,
            null,
            List.of()
        );
    }

    public OpenDomainResponse handleWeather(String location) {
        Optional<WeatherService.WeatherInfo> weatherOpt = weatherService.getWeather(location);

        String answer;
        if (weatherOpt.isPresent()) {
            WeatherService.WeatherInfo w = weatherOpt.get();
            answer = String.format(
                Locale.of("vi", "VN"),
                "Thời tiết tại **%s** hiện tại:\n" +
                "• 🌡️ **Nhiệt độ**: %.1f°C\n" +
                "• ☁️ **Tình trạng**: %s\n" +
                "• 💧 **Độ ẩm**: %d%%",
                w.location(), w.tempC(), w.condition(), w.humidity()
            );
        } else {
            answer = "Để theo dõi thông tin thời tiết chính xác và mới nhất tại " + location + ", bạn có thể kiểm tra ứng dụng thời tiết trên điện thoại hoặc trang dự báo khí tượng thủy văn.";
        }

        // Smart Steering: Only suggest online tutor when rainy or stormy
        boolean isRainyOrBad = weatherOpt.isPresent() && 
            (weatherOpt.get().condition().toLowerCase().contains("mưa") ||
             weatherOpt.get().condition().toLowerCase().contains("rain") ||
             weatherOpt.get().condition().toLowerCase().contains("bão"));

        if (isRainyOrBad) {
            return new OpenDomainResponse(
                answer,
                "Trời mưa có thể bạn muốn học online? TCS có gia sư dạy qua Zoom/Google Meet tiện lợi.",
                "/tim-gia-su?mode=ONLINE",
                List.of("Tìm gia sư Online (/tim-gia-su)")
            );
        } else {
            // Fair weather: No steering
            return new OpenDomainResponse(
                answer,
                null,
                null,
                List.of()
            );
        }
    }

    public OpenDomainResponse handleDefinition(String term) {
        String answer = "Về khái niệm \"" + term + "\", bạn có thể tham khảo từ điển học thuật hoặc gửi câu hỏi chi tiết hơn để tôi giải thích cụ thể.";
        return new OpenDomainResponse(
            answer,
            null,
            null,
            List.of()
        );
    }

    public OpenDomainResponse handleEntertainment(String topic) {
        String answer = "Chúc bạn có những phút giây thư giãn và học tập tràn đầy năng lượng cùng Tutor Connect System (TCS)!";
        return new OpenDomainResponse(
            answer,
            null,
            null,
            List.of()
        );
    }

    public OpenDomainResponse handleNews(String topic) {
        String answer = "Bạn có thể theo dõi các trang tin tức thời sự chính thống để cập nhật thông tin nhanh nhất trong ngày.";
        return new OpenDomainResponse(
            answer,
            null,
            null,
            List.of()
        );
    }

    public OpenDomainResponse handleGeneralKnowledge(String query) {
        String answer = "Tôi sẵn sàng hỗ trợ giải đáp các thắc mắc học tập và thông tin kiến thức tự nhiên/xã hội.";
        return new OpenDomainResponse(
            answer,
            null,
            null,
            List.of()
        );
    }

    private String computeSimpleMath(String expression) {
        if (expression == null) return "Kết quả phép tính của bạn đã được tiếp nhận.";
        Matcher m = SIMPLE_ARITHMETIC.matcher(expression);
        if (m.find()) {
            try {
                double a = Double.parseDouble(m.group(1));
                String op = m.group(2);
                double b = Double.parseDouble(m.group(3));
                double result;
                switch (op) {
                    case "+": result = a + b; break;
                    case "-": result = a - b; break;
                    case "*":
                    case "×":
                    case "x": result = a * b; break;
                    case "/":
                    case "÷":
                        if (b == 0) return "Phép chia cho 0 không xác định trong tập số thực.";
                        result = a / b;
                        break;
                    case "^": result = Math.pow(a, b); break;
                    case "%": result = a % b; break;
                    default: return "Kết quả tính toán biểu thức: " + expression;
                }
                String resStr = (result == Math.floor(result) && !Double.isInfinite(result)) 
                        ? String.format(Locale.ROOT, "%.0f", result) 
                        : String.format(Locale.ROOT, "%.4f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
                return "Kết quả phép tính `" + m.group(1) + " " + op + " " + m.group(3) + "` = **" + resStr + "**";
            } catch (Exception ignored) {}
        }
        return "Tôi đã ghi nhận biểu thức toán học: `" + expression + "`. Nếu đây là phương trình hoặc bài toán cần giải từng bước, bạn hãy gửi đầy đủ đề bài nhé!";
    }
}
