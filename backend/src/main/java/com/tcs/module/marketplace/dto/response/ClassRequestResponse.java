package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Yêu cầu mở lớp của phụ huynh — dùng cho cả phía phụ huynh và phía trung tâm. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
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
    /** PAYMENT_PENDING / PENDING / SEARCHING / ACCEPTED / REJECTED / CANCELLED. */
    private String status;
    /** Lý do khi trung tâm từ chối (nếu có). */
    private String reason;
    private String createdAt;
    /** Nguyên payload form "tìm gia sư" (JSON) để trung tâm xem chi tiết. */
    private String detailsJson;
    /** Danh sách gia sư trung tâm đề cử (shortlist) để phụ huynh chọn. */
    private List<CandidateTutorResponse> candidates;
    /** Tin tuyển dụng trung tâm đã đăng cho yêu cầu này (null = chưa đăng). */
    private Long recruitmentPostId;
    /** Phí xử lý yêu cầu của trung tâm (QR / trạng thái thanh toán). */
    private CenterRequestFeePaymentResponse centerRequestFeePayment;

    public String getRequestId() { return requestId; }
    public Long getCenterId() { return centerId; }
    public String getCenterName() { return centerName; }
    public Long getClientUserId() { return clientUserId; }
    public String getClientName() { return clientName; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getNote() { return note; }
    public BigDecimal getDesiredBudget() { return desiredBudget; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getCreatedAt() { return createdAt; }
    public String getDetailsJson() { return detailsJson; }
    public List<CandidateTutorResponse> getCandidates() { return candidates; }
    public Long getRecruitmentPostId() { return recruitmentPostId; }
    public CenterRequestFeePaymentResponse getCenterRequestFeePayment() { return centerRequestFeePayment; }

    public ClassRequestResponseBuilder toBuilder() {
        return new ClassRequestResponseBuilder()
            .requestId(requestId)
            .centerId(centerId)
            .centerName(centerName)
            .clientUserId(clientUserId)
            .clientName(clientName)
            .categoryId(categoryId)
            .categoryName(categoryName)
            .note(note)
            .desiredBudget(desiredBudget)
            .status(status)
            .reason(reason)
            .createdAt(createdAt)
            .detailsJson(detailsJson)
            .candidates(candidates)
            .recruitmentPostId(recruitmentPostId)
            .centerRequestFeePayment(centerRequestFeePayment);
    }

    public static ClassRequestResponseBuilder builder() {
        return new ClassRequestResponseBuilder();
    }

    public static class ClassRequestResponseBuilder {
        private String requestId;
        private Long centerId;
        private String centerName;
        private Long clientUserId;
        private String clientName;
        private Long categoryId;
        private String categoryName;
        private String note;
        private BigDecimal desiredBudget;
        private String status;
        private String reason;
        private String createdAt;
        private String detailsJson;
        private List<CandidateTutorResponse> candidates;
        private Long recruitmentPostId;
        private CenterRequestFeePaymentResponse centerRequestFeePayment;

        public ClassRequestResponseBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        public ClassRequestResponseBuilder centerId(Long centerId) { this.centerId = centerId; return this; }
        public ClassRequestResponseBuilder centerName(String centerName) { this.centerName = centerName; return this; }
        public ClassRequestResponseBuilder clientUserId(Long clientUserId) { this.clientUserId = clientUserId; return this; }
        public ClassRequestResponseBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public ClassRequestResponseBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public ClassRequestResponseBuilder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public ClassRequestResponseBuilder note(String note) { this.note = note; return this; }
        public ClassRequestResponseBuilder desiredBudget(BigDecimal desiredBudget) { this.desiredBudget = desiredBudget; return this; }
        public ClassRequestResponseBuilder status(String status) { this.status = status; return this; }
        public ClassRequestResponseBuilder reason(String reason) { this.reason = reason; return this; }
        public ClassRequestResponseBuilder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public ClassRequestResponseBuilder detailsJson(String detailsJson) { this.detailsJson = detailsJson; return this; }
        public ClassRequestResponseBuilder candidates(List<CandidateTutorResponse> candidates) { this.candidates = candidates; return this; }
        public ClassRequestResponseBuilder recruitmentPostId(Long recruitmentPostId) { this.recruitmentPostId = recruitmentPostId; return this; }
        public ClassRequestResponseBuilder centerRequestFeePayment(CenterRequestFeePaymentResponse centerRequestFeePayment) { this.centerRequestFeePayment = centerRequestFeePayment; return this; }

        public ClassRequestResponse build() {
            return new ClassRequestResponse(requestId, centerId, centerName, clientUserId, clientName, categoryId, categoryName, note, desiredBudget, status, reason, createdAt, detailsJson, candidates, recruitmentPostId, centerRequestFeePayment);
        }
    }
}
