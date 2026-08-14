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

        String steering = "Nếu bạn có các dạng bài tập khó hơn cần giải thích phương pháp giải chi tiết, hoặc muốn tìm gia sư dạy kèm 1-1, hãy gửi yêu cầu cho tôi nhé!";
        return new OpenDomainResponse(
            answer,
            steering,
            "/tim-gia-su?subject=Toán",
            List.of("Tìm gia sư Toán (/tim-gia-su)", "Xem lớp Toán đang mở (/lop-hoc)", "Tạo lớp tìm gia sư (/tao-lop)")
        );
    }

    public OpenDomainResponse handleTimeDate(String query) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, 'ngày' dd/MM/yyyy, HH:mm", Locale.of("vi", "VN"));
        String answer = "Thời gian hiện tại của hệ thống là: **" + now.format(fmt) + "**.";
        String steering = "TCS có hơn 500+ gia sư sẵn sàng sắp xếp lịch dạy linh hoạt (buổi tối, cuối tuần, học tại nhà hoặc Online). Bạn có cần tìm gia sư hỗ trợ học tập không?";
        return new OpenDomainResponse(
            answer,
            steering,
            "/tim-gia-su",
            List.of("Tìm gia sư linh hoạt lịch (/tim-gia-su)", "Xem danh sách lớp học (/lop-hoc)", "Đăng bài tạo lớp (/tao-lop)")
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

        String steering = "Nếu thời tiết mưa gió bất tiện ra ngoài, bạn hoàn toàn có thể lựa chọn hình thức học Gia sư Online qua Zoom/Google Meet tiện lợi trên TCS!";
        return new OpenDomainResponse(
            answer,
            steering,
            "/tim-gia-su?mode=ONLINE",
            List.of("Tìm gia sư Online (/tim-gia-su)", "Tìm gia sư tại nhà (/tim-gia-su)", "Đăng tin tạo lớp (/tao-lop)")
        );
    }

    public OpenDomainResponse handleDefinition(String term) {
        String answer = "Về khái niệm \"" + term + "\", bạn có thể tham khảo từ điển hoặc đặt câu hỏi chi tiết hơn để tôi giải thích theo từng góc độ học thuật.";
        String steering = "TCS cung cấp trợ giảng AI học tập chuyên sâu và kết nối gia sư các môn học. Bạn có cần tìm gia sư hỗ trợ môn này không?";
        return new OpenDomainResponse(
            answer,
            steering,
            "/tim-gia-su",
            List.of("Tìm gia sư chuyên môn (/tim-gia-su)", "Trung tâm trợ giúp (/help)")
        );
    }

    public OpenDomainResponse handleEntertainment(String topic) {
        String answer = "Chúc bạn có những phút giây thư giãn và học tập tràn đầy năng lượng cùng Tutor Connect System (TCS)!";
        String steering = "Khi bạn sẵn sàng học tập hoặc cần tìm gia sư kèm cặp, đừng ngần ngại nhắn cho tôi nhé.";
        return new OpenDomainResponse(
            answer,
            steering,
            "/tim-gia-su",
            List.of("Tìm gia sư uy tín (/tim-gia-su)", "Khám phá lớp học mới (/lop-hoc)")
        );
    }

    public OpenDomainResponse handleNews(String topic) {
        String answer = "Bạn có thể theo dõi các trang tin tức thời sự chính thống để cập nhật thông tin nhanh nhất trong ngày.";
        String steering = "Tại TCS, các thông báo và tin tức học tập mới nhất luôn được cập nhật liên tục tại bảng tin hệ thống.";
        return new OpenDomainResponse(
            answer,
            steering,
            "/help",
            List.of("Xem tin tức & chính sách (/help)", "Tìm gia sư (/tim-gia-su)")
        );
    }

    public OpenDomainResponse handleGeneralKnowledge(String query) {
        String answer = "Tôi sẵn sàng hỗ trợ giải đáp các thắc mắc học tập và thông tin trên hệ thống.";
        String steering = "TCS là nền tảng kết nối gia sư uy tín hàng đầu. Bạn có muốn tìm gia sư môn học nào hoặc đăng tin tìm lớp không?";
        return new OpenDomainResponse(
            answer,
            steering,
            "/tim-gia-su",
            List.of("Tìm gia sư phù hợp (/tim-gia-su)", "Xem lớp học đang mở (/lop-hoc)")
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
