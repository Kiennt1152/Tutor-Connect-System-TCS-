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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@lombok.extern.slf4j.Slf4j
@Service
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
    private final ConfidenceCalibrator confidenceCalibrator;

    public AiIntentService(IntentClassifier intentClassifier) {
        this(intentClassifier, new ConfidenceCalibrator());
    }

    @Autowired
    public AiIntentService(IntentClassifier intentClassifier, ConfidenceCalibrator confidenceCalibrator) {
        this.intentClassifier = intentClassifier != null ? intentClassifier : new IntentClassifier();
        this.confidenceCalibrator = confidenceCalibrator != null ? confidenceCalibrator : new ConfidenceCalibrator();
    }

    public IntentResultWithEntities classify(String message) {
        DetailedIntentResult detailed = classifyAndExtractDetailed(message, null, null);
        return new IntentResultWithEntities(detailed.legacyIntent(), detailed.confidence(), detailed.entities());
    }

    public IntentResultWithEntities classifyAndExtract(String message) {
        return classify(message);
    }

    public DetailedIntentResult classifyAndExtractDetailed(String message) {
        return classifyAndExtractDetailed(message, null, null);
    }

    public DetailedIntentResult classifyAndExtractDetailed(String message, Long sessionId, Long userId) {
        if (message == null || message.trim().isEmpty()) {
            return new DetailedIntentResult(
                AiDomain.OUT_OF_SCOPE,
                AiSubIntent.OUT_OF_SCOPE,
                AiIntent.OUT_OF_SCOPE,
                0.0,
                Map.of(),
                null
            );
        }

        IntentClassifier.ClassificationDetail detail = intentClassifier.classifyDetailed(message);
        Map<String, String> entities = extractEntities(message);
        double calibratedConfidence = confidenceCalibrator.calibrate(
            detail.confidence(),
            detail.domain(),
            detail.subIntent(),
            entities,
            message
        );

        return new DetailedIntentResult(
            detail.domain(),
            detail.subIntent(),
            detail.legacyIntent(),
            calibratedConfidence,
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

        // 1.5. Extract Price Range ("từ 200k đến 300k", "200k - 300k")
        Pattern rangePattern = Pattern.compile("(?:tu|từ)\\s*(\\d+)\\s*(k|ngan|nghin|nghìn|ngàn|trieu|triệu)?\\s*(?:den|đến|-)\\s*(\\d+)\\s*(k|ngan|nghin|nghìn|ngàn|trieu|triệu)?");
        Matcher rangeMatcher = rangePattern.matcher(lower + " " + normalized);
        if (rangeMatcher.find()) {
            try {
                long minNum = Long.parseLong(rangeMatcher.group(1));
                String minUnit = rangeMatcher.group(2);
                long maxNum = Long.parseLong(rangeMatcher.group(3));
                String maxUnit = rangeMatcher.group(4) != null ? rangeMatcher.group(4) : minUnit;
                minNum = applyFeeUnit(minNum, minUnit);
                maxNum = applyFeeUnit(maxNum, maxUnit);
                entities.put("minFee", String.valueOf(minNum));
                entities.put("maxFee", String.valueOf(maxNum));
            } catch (NumberFormatException ignored) {}
        }

        // 1.6. Extract Min Fee ("trên 300k", "từ 300k trở lên")
        if (!entities.containsKey("minFee")) {
            Pattern minFeePattern = Pattern.compile("(?:tren|trên|tu|từ)\\s*(\\d+)\\s*(k|ngan|nghin|nghìn|ngàn|trieu|triệu)?\\s*(?:tro len|trở lên)?");
            Matcher minMatcher = minFeePattern.matcher(lower + " " + normalized);
            if (minMatcher.find() && !entities.containsKey("maxFee")) {
                try {
                    long num = Long.parseLong(minMatcher.group(1));
                    num = applyFeeUnit(num, minMatcher.group(2));
                    entities.put("minFee", String.valueOf(num));
                } catch (NumberFormatException ignored) {}
            }
        }

        // 1.7. Extract Fee Unit (buổi/giờ/tháng)
        if (containsAny(normalized, "/buoi", "mot buoi", "1 buoi") || lower.contains("/buổi") || lower.contains("một buổi")) {
            entities.put("feeUnit", "buổi");
        } else if (containsAny(normalized, "/h", "/gio", "mot gio", "1 gio") || lower.contains("/giờ") || lower.contains("một giờ")) {
            entities.put("feeUnit", "giờ");
        } else if (containsAny(normalized, "/thang", "mot thang", "1 thang") || lower.contains("/tháng") || lower.contains("một tháng")) {
            entities.put("feeUnit", "tháng");
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

        // 4.5. Extract Schedule / Time Preference
        if (containsAny(normalized, "buoi sang", "sang som")) {
            entities.put("schedule", "Buổi sáng");
        } else if (containsAny(normalized, "buoi chieu")) {
            entities.put("schedule", "Buổi chiều");
        } else if (containsAny(normalized, "buoi toi", "ca toi", "sau 18h", "sau 6h toi")) {
            entities.put("schedule", "Buổi tối");
        } else if (containsAny(normalized, "cuoi tuan", "thu 7", "chu nhat", "t7", "cn")) {
            entities.put("schedule", "Cuối tuần");
        } else if (containsAny(normalized, "trong tuan", "ngay thuong")) {
            entities.put("schedule", "Trong tuần");
        }

        // 4.6. Extract Frequency
        Pattern freqPattern = Pattern.compile("(\\d+)\\s*(?:buoi|lan|buổi|lần)\\s*[/\\\\]?\\s*(?:tuan|thang|tuần|tháng)");
        Matcher freqMatcher = freqPattern.matcher(lower + " " + normalized);
        if (freqMatcher.find()) {
            String unit = freqMatcher.group(0).contains("thang") || freqMatcher.group(0).contains("tháng") ? "tháng" : "tuần";
            entities.put("frequency", freqMatcher.group(1) + " buổi/" + unit);
        }

        // 4.7. Extract Tutor Gender Preference
        if (containsAny(normalized, "co giao", "gia su nu", "co nu", "tim co", "sinh vien nu", "giao vien nu")) {
            entities.put("tutorGender", "NỮ");
        } else if (containsAny(normalized, "thay giao", "gia su nam", "thay nam", "tim thay", "sinh vien nam", "giao vien nam")) {
            entities.put("tutorGender", "NAM");
        }

        // 4.8. Extract Tutor Qualification
        if (containsAny(normalized, "sinh vien") && containsAny(normalized, "gia su", "tim", "can", "day", "thue")) {
            entities.put("tutorType", "Sinh viên");
        } else if (containsAny(normalized, "giao vien dung lop", "giao vien truong cong", "giao vien truong chuyen")) {
            entities.put("tutorType", "Giáo viên");
        } else if (containsAny(normalized, "thac si")) {
            entities.put("tutorType", "Thạc sĩ");
        } else if (containsAny(normalized, "tien si")) {
            entities.put("tutorType", "Tiến sĩ");
        }

        // 4.9. Extract Class Type
        if (containsAny(normalized, "1 kem 1", "1-1", "mot kem mot", "ca nhan", "hoc rieng")) {
            entities.put("classType", "1 kèm 1");
        } else if (containsAny(normalized, "hoc nhom", "lop nhom", "nhom 3", "nhom 4", "nhom 5")) {
            entities.put("classType", "Nhóm");
        }

        // 4.10. Extract Learning Goal
        if (containsAny(normalized, "mat goc", "lay lai goc", "yeu mon", "kem mon")) {
            entities.put("learningGoal", "Lấy lại gốc");
        } else if (containsAny(normalized, "on thi vao 10", "thi vao lop 10", "tuyen sinh 10")) {
            entities.put("learningGoal", "Ôn thi vào 10");
        } else if (containsAny(normalized, "on thi truong chuyen", "truong chuyen")) {
            entities.put("learningGoal", "Ôn thi trường chuyên");
        } else if (containsAny(normalized, "hoc sinh gioi", "hsg", "luyen thi hsg")) {
            entities.put("learningGoal", "Luyện thi HSG");
        } else if (containsAny(normalized, "luyen thi dai hoc", "thi dai hoc", "thi dh", "thptqg")) {
            entities.put("learningGoal", "Luyện thi Đại học");
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
        // TP.HCM Districts
        else if (lower.contains("quận 1") || normalized.contains("quan 1") || normalized.contains("q1")) entities.put("location", "Quận 1");
        else if (lower.contains("quận 3") || normalized.contains("quan 3") || normalized.contains("q3")) entities.put("location", "Quận 3");
        else if (lower.contains("quận 4") || normalized.contains("quan 4")) entities.put("location", "Quận 4");
        else if (lower.contains("quận 5") || normalized.contains("quan 5")) entities.put("location", "Quận 5");
        else if (lower.contains("quận 6") || normalized.contains("quan 6")) entities.put("location", "Quận 6");
        else if (lower.contains("quận 7") || normalized.contains("quan 7") || normalized.contains("q7")) entities.put("location", "Quận 7");
        else if (lower.contains("quận 8") || normalized.contains("quan 8")) entities.put("location", "Quận 8");
        else if (lower.contains("quận 9") || normalized.contains("quan 9")) entities.put("location", "Quận 9");
        else if (lower.contains("quận 10") || normalized.contains("quan 10") || normalized.contains("q10")) entities.put("location", "Quận 10");
        else if (lower.contains("quận 11") || normalized.contains("quan 11")) entities.put("location", "Quận 11");
        else if (lower.contains("quận 12") || normalized.contains("quan 12")) entities.put("location", "Quận 12");
        else if (lower.contains("thủ đức") || normalized.contains("thu duc")) entities.put("location", "TP. Thủ Đức");
        else if (lower.contains("bình thạnh") || normalized.contains("binh thanh")) entities.put("location", "Bình Thạnh");
        else if (lower.contains("gò vấp") || normalized.contains("go vap")) entities.put("location", "Gò Vấp");
        else if (lower.contains("phú nhuận") || normalized.contains("phu nhuan")) entities.put("location", "Phú Nhuận");
        else if (lower.contains("tân bình") || normalized.contains("tan binh")) entities.put("location", "Tân Bình");
        else if (lower.contains("tân phú") || normalized.contains("tan phu")) entities.put("location", "Tân Phú");
        else if (lower.contains("bình tân") || normalized.contains("binh tan")) entities.put("location", "Bình Tân");
        // More Hà Nội Districts
        else if (lower.contains("hoàn kiếm") || normalized.contains("hoan kiem")) entities.put("location", "Hoàn Kiếm");
        else if (lower.contains("hai bà trưng") || normalized.contains("hai ba trung")) entities.put("location", "Hai Bà Trưng");
        else if (lower.contains("long biên") || normalized.contains("long bien")) entities.put("location", "Long Biên");
        else if (lower.contains("đông anh") || normalized.contains("dong anh")) entities.put("location", "Đông Anh");
        else if (lower.contains("gia lâm") || normalized.contains("gia lam")) entities.put("location", "Gia Lâm");
        // More provinces
        else if (lower.contains("quảng ninh") || normalized.contains("quang ninh")) entities.put("location", "Quảng Ninh");
        else if (lower.contains("nghệ an") || normalized.contains("nghe an")) entities.put("location", "Nghệ An");
        else if (lower.contains("thanh hóa") || normalized.contains("thanh hoa")) entities.put("location", "Thanh Hóa");
        else if (lower.contains("huế") || normalized.contains("thua thien hue") || normalized.contains("hue")) entities.put("location", "Thừa Thiên Huế");
        else if (lower.contains("nha trang") || normalized.contains("nha trang") || normalized.contains("khanh hoa")) entities.put("location", "Khánh Hòa");
        else if (lower.contains("vũng tàu") || normalized.contains("vung tau") || normalized.contains("ba ria")) entities.put("location", "Bà Rịa - Vũng Tàu");
        else if (lower.contains("đà lạt") || normalized.contains("da lat") || normalized.contains("lam dong")) entities.put("location", "Lâm Đồng");
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
        } else if (containsAny(normalized, "giao duc cong dan", "gdcd", "kinh te va phap luat")) {
            entities.put("subject", "GDCD");
        } else if (containsAny(normalized, "cong nghe") && !normalized.contains("cong nghe thong tin")) {
            entities.put("subject", "Công nghệ");
        } else if (containsAny(normalized, "am nhac", "piano", "organ", "guitar", "ukulele", "violin", "thanh nhac", "luyen thanh")) {
            entities.put("subject", "Âm nhạc");
            if (lower.contains("piano")) entities.put("instrument", "Piano");
            else if (lower.contains("guitar")) entities.put("instrument", "Guitar");
            else if (lower.contains("violin")) entities.put("instrument", "Violin");
        } else if (containsAny(normalized, "my thuat", "hoi hoa", "ve chi", "ve mau nuoc", "khoi v", "khoi h")) {
            entities.put("subject", "Mỹ thuật");
        } else if (containsAny(normalized, "tieng duc", "german", "goethe")) {
            entities.put("subject", "Tiếng Đức");
        } else if (containsAny(normalized, "tieng nga", "russian")) {
            entities.put("subject", "Tiếng Nga");
        } else if (containsAny(normalized, "tieng tay ban nha", "spanish")) {
            entities.put("subject", "Tiếng Tây Ban Nha");
        } else if (containsAny(normalized, "khoa hoc xa hoi", "khxh")) {
            entities.put("subject", "KHXH");
        } else if (lower.contains("khoa học tự nhiên") || normalized.contains("khoa hoc tu nhien") || normalized.contains("khtn")) {
            entities.put("subject", "KHTN");
        }

        // 7. Extract Certification Level (IELTS, TOEIC, HSK, JLPT, TOPIK)
        Pattern ieltsPattern = Pattern.compile("ielts\\s+(?:band\\s+)?([0-9]+(?:\\.[0-9]+)?)");
        Matcher ieltsMatcher = ieltsPattern.matcher(lower);
        if (ieltsMatcher.find()) {
            entities.put("certLevel", "IELTS " + ieltsMatcher.group(1));
            if (!entities.containsKey("subject")) entities.put("subject", "Anh");
        }

        Pattern toeicPattern = Pattern.compile("toeic\\s+([0-9]{3,4})");
        Matcher toeicMatcher = toeicPattern.matcher(lower);
        if (toeicMatcher.find()) {
            entities.put("certLevel", "TOEIC " + toeicMatcher.group(1));
            if (!entities.containsKey("subject")) entities.put("subject", "Anh");
        }

        if (lower.contains("hsk")) {
            Pattern hskPattern = Pattern.compile("hsk\\s*([1-6])");
            Matcher hskMatcher = hskPattern.matcher(lower);
            if (hskMatcher.find()) {
                entities.put("certLevel", "HSK " + hskMatcher.group(1));
            } else {
                entities.put("certLevel", "HSK");
            }
            if (!entities.containsKey("subject")) entities.put("subject", "Tiếng Trung");
        }

        if (lower.contains("jlpt") || lower.contains("n1") || lower.contains("n2") || lower.contains("n3") || lower.contains("n4") || lower.contains("n5")) {
            Pattern jlptPattern = Pattern.compile("(?:jlpt\\s*)?(n[1-5])");
            Matcher jlptMatcher = jlptPattern.matcher(lower);
            if (jlptMatcher.find()) {
                entities.put("certLevel", "JLPT " + jlptMatcher.group(1).toUpperCase());
                if (!entities.containsKey("subject")) entities.put("subject", "Tiếng Nhật");
            }
        }

        if (lower.contains("topik")) {
            Pattern topikPattern = Pattern.compile("topik\\s*([1-6])");
            Matcher topikMatcher = topikPattern.matcher(lower);
            if (topikMatcher.find()) {
                entities.put("certLevel", "TOPIK " + topikMatcher.group(1));
            } else {
                entities.put("certLevel", "TOPIK");
            }
            if (!entities.containsKey("subject")) entities.put("subject", "Tiếng Hàn");
        }

        // TOEFL
        if (lower.contains("toefl")) {
            Pattern toeflPattern = Pattern.compile("toefl\\s*(?:ibt|junior|primary|itp)?\\s*([0-9]{2,3})?");
            Matcher toeflMatcher = toeflPattern.matcher(lower);
            if (toeflMatcher.find()) {
                String score = toeflMatcher.group(1);
                entities.put("certLevel", "TOEFL" + (score != null ? " " + score : ""));
                if (!entities.containsKey("subject")) entities.put("subject", "Anh");
            }
        }

        // Cambridge (KET, PET, FCE, CAE, CPE)
        if (containsAny(lower, "ket ", "pet ", "fce", "cae", "cpe", "starters", "movers", "flyers")) {
            String cert = lower.contains("fce") ? "FCE" : lower.contains("cae") ? "CAE" :
                          lower.contains("cpe") ? "CPE" : lower.contains("ket") ? "KET" :
                          lower.contains("pet") ? "PET" : "Cambridge YLE";
            entities.put("certLevel", cert);
            if (!entities.containsKey("subject")) entities.put("subject", "Anh");
        }

        // VSTEP
        if (lower.contains("vstep")) {
            entities.put("certLevel", "VSTEP");
            if (!entities.containsKey("subject")) entities.put("subject", "Anh");
        }

        // DELF/DALF/TCF (French)
        if (containsAny(lower, "delf", "dalf", "tcf")) {
            String cert = lower.contains("delf") ? "DELF" : lower.contains("dalf") ? "DALF" : "TCF";
            entities.put("certLevel", cert);
            if (!entities.containsKey("subject")) entities.put("subject", "Tiếng Pháp");
        }

        // Goethe-Zertifikat / TestDaF (German)
        if (containsAny(lower, "goethe", "testdaf", "dsh")) {
            entities.put("certLevel", "Goethe-Zertifikat");
            if (!entities.containsKey("subject")) entities.put("subject", "Tiếng Đức");
        }

        // SAT/ACT
        if (containsAny(lower, "sat ", "sat digital", " act ")) {
            Pattern satPattern = Pattern.compile("sat\\s*(?:digital)?\\s*([0-9]{3,4})?");
            Matcher satMatcher = satPattern.matcher(lower);
            if (satMatcher.find()) {
                String score = satMatcher.group(1);
                entities.put("certLevel", "SAT" + (score != null ? " " + score : ""));
            }
        }

        // 8. Extract Programming Language (for Tin học subject)
        if (lower.contains("python") || lower.contains("py")) {
            entities.put("programmingLang", "Python");
            if (!entities.containsKey("subject")) entities.put("subject", "Tin học");
        } else if (lower.contains("scratch")) {
            entities.put("programmingLang", "Scratch");
            if (!entities.containsKey("subject")) entities.put("subject", "Tin học");
        } else if (lower.contains("c++") || lower.contains("cpp")) {
            entities.put("programmingLang", "C++");
            if (!entities.containsKey("subject")) entities.put("subject", "Tin học");
        } else if (lower.contains("java") && !lower.contains("javascript")) {
            entities.put("programmingLang", "Java");
            if (!entities.containsKey("subject")) entities.put("subject", "Tin học");
        } else if (lower.contains("javascript") || lower.contains("js")) {
            entities.put("programmingLang", "JavaScript");
            if (!entities.containsKey("subject")) entities.put("subject", "Tin học");
        }

        return entities;
    }

    private String removeDiacritics(String text) {
        return VietnameseTextNormalizer.removeDiacritics(text);
    }

    private long applyFeeUnit(long num, String unit) {
        if (unit != null) {
            if (unit.startsWith("k") || unit.startsWith("ng")) {
                return num * 1000;
            } else if (unit.startsWith("tr")) {
                return num * 1000000;
            }
        } else if (num < 1000) {
            return num * 1000;
        }
        return num;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
