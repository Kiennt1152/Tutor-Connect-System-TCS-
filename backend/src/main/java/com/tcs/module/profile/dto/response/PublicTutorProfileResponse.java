package com.tcs.module.profile.dto.response;

import com.tcs.module.profile.enums.Gender;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Hồ sơ gia sư ở dạng CÔNG KHAI (dùng cho trang /gia-su/:tutorId).
 * Chỉ chứa thông tin an toàn để hiển thị cho người xem lạ — KHÔNG kèm email,
 * số điện thoại, địa chỉ, ngày sinh.
 */
@Getter
@Builder
public class PublicTutorProfileResponse {

    private Long tutorId;
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private Gender gender;
    private String bio;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal ratingAvg;
    private ProfileVerificationStatus verificationStatus;

    private List<TutorEducationResponse> educations;
    private List<TutorCertificateResponse> certificates;
    private List<TutorExperienceResponse> experiences;
}
