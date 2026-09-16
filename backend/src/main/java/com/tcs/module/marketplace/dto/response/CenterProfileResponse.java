package com.tcs.module.marketplace.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Hồ sơ công khai của một trung tâm đã xác minh: thông tin chung, đội ngũ gia sư đang
 * hoạt động và các lớp đang mở. Gia sư và lớp dùng lại DTO của trang tìm gia sư / tìm lớp
 * để giao diện hiển thị bằng cùng một loại thẻ.
 */
@Getter
@Builder
public class CenterProfileResponse {
    private Long centerId;
    private String companyName;
    private String description;
    private String address;
    private String phone;
    private String avatar;
    private LocalDateTime joinedAt;
    private List<TutorSearchResponse> tutors;
    private List<ClassResponse> openClasses;
}
