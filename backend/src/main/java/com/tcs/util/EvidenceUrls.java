package com.tcs.util;

import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * Bằng chứng (báo cáo vi phạm, phiếu hỗ trợ, tranh chấp) được lưu chung một cột text,
 * nhiều đường dẫn ngăn nhau bằng xuống dòng / dấu phẩy / chấm phẩy.
 *
 * <p>Tách ở một chỗ duy nhất để mọi màn hình hiển thị cùng một danh sách file.
 */
public final class EvidenceUrls {

    private EvidenceUrls() {
    }

    public static List<String> parse(String evidenceUrls) {
        if (!StringUtils.hasText(evidenceUrls)) {
            return List.of();
        }
        return Arrays.stream(evidenceUrls.split("[\\r\\n,;]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
