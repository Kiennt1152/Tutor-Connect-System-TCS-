package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** Yêu cầu mở lớp của phụ huynh — dùng cho cả phía phụ huynh và phía trung tâm. */
@Getter
@Builder
public class ClassRequestResponse {

    private String requestId;
    private Long centerId;
    private String centerName;
    private Long clientUserId;
    private String clientName;
    private Long categoryId;
    private String categoryName;
    private String note;
    private BigDecimal desiredBudget;
    /** PENDING / SEARCHING / ACCEPTED / REJECTED. */
    private String status;
    /** Lý do khi trung tâm từ chối (nếu có). */
    private String reason;
    private String createdAt;
    /** Nguyên payload form "tìm gia sư" (JSON) để trung tâm xem chi tiết. */
    private String detailsJson;
    /** Danh sách gia sư trung tâm đề cử (shortlist) để phụ huynh chọn. */
    private java.util.List<CandidateTutorResponse> candidates;
    /** Tin tuyển dụng trung tâm đã đăng cho yêu cầu này (null = chưa đăng). */
    private Long recruitmentPostId;
}
