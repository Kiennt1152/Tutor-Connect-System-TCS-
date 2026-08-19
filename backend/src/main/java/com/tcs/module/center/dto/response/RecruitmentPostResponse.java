package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.RecruitmentPostStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Một tin tuyển gia sư (FT-33) — dùng cho trung tâm quản lý và gia sư xem tin đang mở. */
@Getter
@Builder
public class RecruitmentPostResponse {

    private Long recruitmentId;
    private Long centerId;
    private String centerName;

    /** Lớp mà tin này tuyển cho (nếu có). Null = tin tuyển chung. */
    private Long classId;
    private String classTitle;

    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private Integer requiredExperience;
    private Integer maxPositions;

    private Long subjectId;
    private String subjectName;

    private Long locationId;
    /** Địa điểm gộp để hiển thị, VD: "12 Trần Phú, Hà Nội". */
    private String locationLabel;
    /** Tách sẵn để đổ lại form khi sửa tin. */
    private String provinceName;
    private String wardName;
    private String addressDetail;

    private RecruitmentPostStatus status;
    private LocalDateTime publishedAt;
    /**
     * Mốc tin hết hạn hiển thị: 30 ngày kể từ {@code publishedAt}. Quá hạn tin tự gỡ
     * về nháp. Null khi tin chưa đăng. Dùng cho đồng hồ đếm ngược ở giao diện.
     */
    private LocalDateTime expiresAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Số đơn đã nộp (trung tâm dùng để biết tin có ứng viên chưa). */
    private long applicationCount;
}
