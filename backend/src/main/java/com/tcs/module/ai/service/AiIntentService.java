package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiIntentService {

    public record IntentResultWithEntities(
        AiIntent intent,
        double confidence,
        Map<String, String> entities
    ) {}

    public record DetailedIntentResult(
        AiDomain domain,
        AiSubIntent subIntent,
        AiIntent legacyIntent,
        double confidence,
        Map<String, String> entities,
        String suggestedRoute
    ) {}

    private final IntentClassifier intentClassifier;

    public IntentResultWithEntities classify(String message) {
        DetailedIntentResult detailed = classifyAndExtractDetailed(message);
        return new IntentResultWithEntities(detailed.legacyIntent(), detailed.confidence(), detailed.entities());
    }

    public IntentResultWithEntities classifyAndExtract(String message) {
        return classify(message);
    }

    public DetailedIntentResult classifyAndExtractDetailed(String message) {
        IntentClassifier.ClassificationDetail detail = intentClassifier.classifyDetailed(message);
        Map<String, String> entities = extractEntities(message);
        return new DetailedIntentResult(
            detail.domain(),
            detail.subIntent(),
            detail.legacyIntent(),
            detail.confidence(),
            entities,
            detail.suggestedRoute()
        );
    }

    private Map<String, String> extractEntities(String message) {
        Map<String, String> entities = new HashMap<>();
        if (message == null) return entities;

        String lower = message.toLowerCase(Locale.ROOT);
        String normalized = removeDiacritics(lower);

        // 1. Extract Max Fee (supports "dưới 250k", "duoi 250k", "<= 300k", "tầm 200 ngàn", "250000đ")
        Pattern feePattern = Pattern.compile("(d\u01b0\u1edbi|duoi|th\u1ea5p h\u01a1n|thap hon|kho\u1ea3ng|khoang|t\u1ea7m|tam|<=)\\s*([0-9]+)\\s*(k|ng\u00e0n|ngan|ngh\u00ecn|nghin|tri\u1ec7u|trieu)?");
        Matcher feeMatcher = feePattern.matcher(lower + " " + normalized);
        if (feeMatcher.find()) {
            try {
                long num = Long.parseLong(feeMatcher.group(2));
                String unit = feeMatcher.group(3);
                if (unit != null) {
                    if (unit.startsWith("k") || unit.startsWith("ng")) {
                        num *= 1000;
                    } else if (unit.startsWith("tr")) {
                        num *= 1000000;
                    }
                } else if (num < 1000) {
                    num *= 1000; // heuristic: 250 -> 250000
                }
                entities.put("maxFee", String.valueOf(num));
            } catch (NumberFormatException ignored) {}
        }

        // 2. Extract Grade (supports "lớp 12", "lop 12", "lớp 10", "lop 6")
        Pattern gradePattern = Pattern.compile("(l\u1edbp|lop)\\s+([0-9]{1,2})");
        Matcher gradeMatcher = gradePattern.matcher(lower + " " + normalized);
        if (gradeMatcher.find()) {
            entities.put("grade", gradeMatcher.group(2));
        }

        // 3. Extract Stage / Educational Level
        if (lower.contains("tiểu học") || normalized.contains("tieu hoc") || lower.contains("cấp 1") || normalized.contains("cap 1")) {
            entities.put("level", "Tiểu học");
        } else if (lower.contains("thcs") || lower.contains("cấp 2") || normalized.contains("cap 2")) {
            entities.put("level", "THCS");
        } else if (lower.contains("thpt") || lower.contains("cấp 3") || normalized.contains("cap 3") || lower.contains("luyện thi đại học") || normalized.contains("luyen thi dai hoc")) {
            entities.put("level", "THPT");
        } else if (lower.contains("đại học") || normalized.contains("dai hoc") || lower.contains("sinh viên") || normalized.contains("sinh vien")) {
            entities.put("level", "Đại học");
        }

        // 4. Extract Mode (Online vs Offline / Tại nhà)
        if (lower.contains("online") || lower.contains("trực tuyến") || normalized.contains("truc tuyen") || normalized.contains("qua mang") || normalized.contains("zoom") || normalized.contains("meet")) {
            entities.put("mode", "ONLINE");
        } else if (lower.contains("tại nhà") || normalized.contains("tai nha") || lower.contains("offline") || lower.contains("trực tiếp") || normalized.contains("truc tiep")) {
            entities.put("mode", "OFFLINE");
        }

        // 5. Extract Location
        if (lower.contains("cầu giấy") || normalized.contains("cau giay")) entities.put("location", "Cầu Giấy");
        else if (lower.contains("đống đa") || normalized.contains("dong da")) entities.put("location", "Đống Đa");
        else if (lower.contains("ba đình") || normalized.contains("ba dinh")) entities.put("location", "Ba Đình");
        else if (lower.contains("thanh xuân") || normalized.contains("thanh xuan")) entities.put("location", "Thanh Xuân");
        else if (lower.contains("hà đông") || normalized.contains("ha dong")) entities.put("location", "Hà Đông");
        else if (lower.contains("hoàng mai") || normalized.contains("hoang mai")) entities.put("location", "Hoàng Mai");
        else if (lower.contains("tây hồ") || normalized.contains("tay ho")) entities.put("location", "Tây Hồ");
        else if (lower.contains("nam từ liêm") || normalized.contains("nam tu liem")) entities.put("location", "Nam Từ Liêm");
        else if (lower.contains("bắc từ liêm") || normalized.contains("bac tu liem")) entities.put("location", "Bắc Từ Liêm");
        else if (lower.contains("hà giang") || normalized.contains("ha giang")) entities.put("location", "Hà Giang");
        else if (lower.contains("bắc ninh") || normalized.contains("bac ninh")) entities.put("location", "Bắc Ninh");
        else if (lower.contains("hải dương") || normalized.contains("hai duong")) entities.put("location", "Hải Dương");
        else if (lower.contains("bình dương") || normalized.contains("binh duong")) entities.put("location", "Bình Dương");
        else if (lower.contains("đồng nai") || normalized.contains("dong nai")) entities.put("location", "Đồng Nai");
        else if (lower.contains("hà nội") || normalized.contains("ha noi")) entities.put("location", "Hà Nội");
        else if (lower.contains("hcm") || lower.contains("hồ chí minh") || normalized.contains("ho chi minh") || normalized.contains("sai gon")) entities.put("location", "TP.HCM");
        else if (lower.contains("đà nẵng") || normalized.contains("da nang")) entities.put("location", "Đà Nẵng");
        else if (lower.contains("hải phòng") || normalized.contains("hai phong")) entities.put("location", "Hải Phòng");
        else if (lower.contains("cần thơ") || normalized.contains("can tho")) entities.put("location", "Cần Thơ");
        else {
            Pattern locationPattern = Pattern.compile("(?<=\\s|^)(khu v\u1ef1c|khu vuc|t\u1ea1i|tai|qu\u1eadn|quan|tp\\.?|th\u00e0nh ph\u1ed1|thanh pho|tinh|t\u1ec9nh)\\s+([^,.;\\s]+(?:\\s+[^,.;\\s]+){0,2})");
            Matcher locationMatcher = locationPattern.matcher(lower);
            if (locationMatcher.find()) {
                String area = locationMatcher.group(2).trim();
                if (area.contains(" dưới")) area = area.substring(0, area.indexOf(" dưới"));
                if (area.contains(" duoi")) area = area.substring(0, area.indexOf(" duoi"));
                if (area.contains(" tầm")) area = area.substring(0, area.indexOf(" tầm"));
                if (area.contains(" khoảng")) area = area.substring(0, area.indexOf(" khoảng"));
                if (area.contains(" lớp")) area = area.substring(0, area.indexOf(" lớp"));
                entities.put("location", area.trim());
            }
        }

        // 6. Extract Subject
        if (lower.contains("tiếng việt") || lower.contains("môn tiếng việt") || normalized.contains("tieng viet") || normalized.contains("tiieng viet") || lower.contains("vietnamese") || lower.contains("luyện chữ") || normalized.contains("luyen chu")) {
            entities.put("subject", "Tiếng Việt");
        } else if (lower.contains("toán") || normalized.contains("toan") || lower.contains("math") || lower.contains("đại số") || lower.contains("hình học")) {
            entities.put("subject", "Toán");
        } else if (lower.contains("tiếng pháp") || lower.contains("môn pháp") || normalized.contains("tieng phap") || normalized.contains("mon phap") || lower.contains("french")) {
            entities.put("subject", "Tiếng Pháp");
        } else if (lower.contains("tiếng trung") || lower.contains("tiếng hoa") || normalized.contains("tieng trung") || normalized.contains("chinese") || lower.contains("hsk")) {
            entities.put("subject", "Tiếng Trung");
        } else if (lower.contains("tiếng nhật") || normalized.contains("tieng nhat") || normalized.contains("japanese") || lower.contains("jlpt")) {
            entities.put("subject", "Tiếng Nhật");
        } else if (lower.contains("tiếng hàn") || normalized.contains("tieng han") || normalized.contains("korean") || lower.contains("topik")) {
            entities.put("subject", "Tiếng Hàn");
        } else if (lower.contains("tiếng anh") || normalized.contains("tieng anh") || normalized.contains("ielts") || normalized.contains("toeic") || lower.contains("english") || lower.contains("môn anh") || normalized.contains("gia su anh") || normalized.contains("lop anh")) {
            entities.put("subject", "Anh");
        } else if (lower.contains("vật lý") || lower.contains("môn lý") || normalized.contains("vat ly") || lower.contains("physics") || normalized.contains("gia su ly") || normalized.contains("lop ly")) {
            entities.put("subject", "Lý");
        } else if (lower.contains("hóa học") || lower.contains("môn hóa") || normalized.contains("hoa hoc") || lower.contains("chemistry") || normalized.contains("gia su hoa") || normalized.contains("lop hoa") || (lower.contains("hóa") && !lower.contains("chuyển hóa") && !lower.contains("tài khóa"))) {
            entities.put("subject", "Hóa");
        } else if (lower.contains("ngữ văn") || lower.contains("môn văn") || normalized.contains("ngu van") || normalized.contains("gia su van") || normalized.contains("lop van") || lower.contains("văn học")) {
            entities.put("subject", "Văn");
        } else if (lower.contains("sinh học") || lower.contains("môn sinh") || normalized.contains("sinh hoc") || normalized.contains("gia su sinh") || normalized.contains("lop sinh")) {
            entities.put("subject", "Sinh");
        } else if (lower.contains("lịch sử") || lower.contains("môn sử") || normalized.contains("lich su") || normalized.contains("gia su su") || normalized.contains("lop su")) {
            entities.put("subject", "Sử");
        } else if (lower.contains("địa lý") || lower.contains("môn địa") || normalized.contains("dia ly") || normalized.contains("gia su dia") || normalized.contains("lop dia")) {
            entities.put("subject", "Địa");
        } else if (lower.contains("tin học") || lower.contains("lập trình") || normalized.contains("tin hoc") || normalized.contains("lap trinh") || normalized.contains("coding") || lower.contains("python") || lower.contains("scratch") || lower.contains("c++")) {
            entities.put("subject", "Tin học");
        } else if (lower.contains("khoa học tự nhiên") || normalized.contains("khoa hoc tu nhien") || normalized.contains("khtn")) {
            entities.put("subject", "KHTN");
        }

        return entities;
    }

    private String removeDiacritics(String text) {
        return VietnameseTextNormalizer.removeDiacritics(text);
    }
}
