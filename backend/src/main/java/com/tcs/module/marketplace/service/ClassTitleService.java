package com.tcs.module.marketplace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.SubjectRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sinh tiêu đề mặc định của lớp ("Cần tìm gia sư môn Toán, Vật lý") từ danh sách môn trong detailsJson.
 *
 * <p>Tách thành lớp riêng vì tiêu đề phải chạy theo danh sách môn ở hai nơi cách xa nhau:</p>
 *
 * <ul>
 *   <li>lúc khách chọn gia sư — lớp thu hẹp về đúng môn gia sư nhận dạy
 *       ({@code MarketplaceServiceImpl#chooseApplicant});</li>
 *   <li>lúc hết hạn 48 giờ — lớp mở lại với đủ môn ban đầu
 *       ({@code MatchContractDeadlineScheduler}).</li>
 * </ul>
 *
 * <p>Hai nơi bắt buộc dùng CHUNG một quy tắc. Mỗi nơi tự ghép chuỗi theo cách riêng thì tiêu đề và
 * danh sách môn sẽ lệch nhau, mà lệch kiểu này rất khó thấy: lớp vẫn chạy đúng, chỉ có cái tên nói
 * sai với gia sư đang đọc tin.</p>
 */
@Service
@RequiredArgsConstructor
public class ClassTitleService {

    /** Độ dài tối đa của cột title trong bảng tutoring_classes. */
    private static final int TITLE_MAX_LENGTH = 150;

    /** Mã môn "Khác" — tên do người dùng tự gõ nên không tra được trong bảng subjects. */
    private static final String OTHER_SUBJECT_KEY = "other";

    private final SubjectRepository subjectRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Sinh tiêu đề "Cần tìm gia sư môn A, B", cắt ngắn nếu vượt độ dài tối đa của cột. */
    public String autoTitle(String detailsJson, Subject fallbackSubject) {
        List<String> names = subjectNames(detailsJson);
        if (names.isEmpty() && fallbackSubject != null) {
            names = List.of(fallbackSubject.getSubjectName());
        }
        StringBuilder sb = new StringBuilder("Cần tìm gia sư");
        if (!names.isEmpty()) {
            sb.append(" môn ").append(String.join(", ", names));
        }
        String title = sb.toString();
        return title.length() > TITLE_MAX_LENGTH
                ? title.substring(0, TITLE_MAX_LENGTH - 1) + "…"
                : title;
    }

    /**
     * Tiêu đề hiện tại có phải do hệ thống tự sinh không — tức là có khớp đúng chuỗi mà
     * {@link #autoTitle} sinh ra từ danh sách môn hiện tại hay không.
     *
     * <p>Đây là điều kiện để được phép viết đè tiêu đề. Khách tự đặt tên lớp thì tên đó là ý của
     * họ, hệ thống không có quyền sửa dù danh sách môn có đổi.</p>
     */
    public boolean isAutoTitle(String currentTitle, String detailsJson, Subject fallbackSubject) {
        return StringUtils.hasText(currentTitle)
                && currentTitle.trim().equals(autoTitle(detailsJson, fallbackSubject));
    }

    /** Đọc danh sách mã môn (subjectIds) trong detailsJson; JSON lỗi hoặc không có thì trả danh sách rỗng. */
    public List<String> subjectKeys(String detailsJson) {
        if (!StringUtils.hasText(detailsJson)) {
            return List.of();
        }
        try {
            JsonNode ids = objectMapper.readTree(detailsJson).path("subjectIds");
            if (!ids.isArray()) {
                return List.of();
            }
            List<String> keys = new ArrayList<>();
            for (JsonNode id : ids) {
                keys.add(id.asText());
            }
            return keys;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** Đổi mã môn trong detailsJson thành tên môn (môn "Khác" lấy tên người dùng tự gõ). */
    public List<String> subjectNames(String detailsJson) {
        List<String> keys = subjectKeys(detailsJson);
        if (keys.isEmpty()) {
            return List.of();
        }
        JsonNode root = null;
        try {
            root = objectMapper.readTree(detailsJson);
        } catch (JsonProcessingException ignored) {
        }
        String legacyOther = root != null ? root.path("subjectOther").asText("").trim() : "";
        JsonNode subjectOthers = root != null ? root.path("subjectOthers") : null;
        List<String> names = new ArrayList<>();
        for (String key : keys) {
            if (isOtherSubjectKey(key)) {
                String name = "";
                if (subjectOthers != null && subjectOthers.isObject()) {
                    name = subjectOthers.path(key).asText("").trim();
                }
                if (!StringUtils.hasText(name)) {
                    name = legacyOther;
                }
                names.add(StringUtils.hasText(name) ? name : "Môn học khác");
                continue;
            }
            try {
                subjectRepository.findById(Long.valueOf(key))
                        .map(Subject::getSubjectName)
                        .ifPresent(names::add);
            } catch (NumberFormatException ignored) {
            }
        }
        return names;
    }

    /** Mã môn có phải môn "Khác" (người dùng tự nhập tên) hay không. */
    public boolean isOtherSubjectKey(String key) {
        return OTHER_SUBJECT_KEY.equals(key)
                || (key != null && key.startsWith(OTHER_SUBJECT_KEY + ":"));
    }
}
