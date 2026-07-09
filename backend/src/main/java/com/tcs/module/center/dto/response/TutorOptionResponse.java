package com.tcs.module.center.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** Một gia sư trong danh sách để trung tâm chọn gán vào lớp. */
@Getter
@Builder
public class TutorOptionResponse {

    private Long tutorId;
    private String fullName;
    private Integer experienceYears;
    private BigDecimal ratingAvg;
    private String verificationStatus;
    private String phone;
    private String avatar;
    private String bio;
}
