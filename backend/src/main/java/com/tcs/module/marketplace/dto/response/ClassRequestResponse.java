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
    /** PENDING / ACCEPTED / REJECTED. */
    private String status;
    /** Lý do khi trung tâm từ chối (nếu có). */
    private String reason;
    private String createdAt;
}
