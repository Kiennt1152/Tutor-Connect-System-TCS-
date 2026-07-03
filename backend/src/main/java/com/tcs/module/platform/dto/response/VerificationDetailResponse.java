package com.tcs.module.platform.dto.response;

import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Chi tiet mot yeu cau xac minh: thong tin ho so nguoi nop + danh sach tai lieu,
 * de Platform Admin xem khi duyet.
 */
@Getter
@Builder
public class VerificationDetailResponse {

    private Long verificationId;
    private Long userId;
    private String userEmail;
    private VerificationType verificationType;
    private VerificationStatus status;
    private String adminNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String submitterName;
    private String submitterPhone;
    /** Thong tin ho so rieng theo vai tro (nhan -> gia tri). */
    private Map<String, String> submitterDetails;

    private List<VerificationDocumentResponse> documents;
    /** true neu co it nhat mot tai lieu bi thieu/hong. */
    private boolean hasUnreadableDocument;
}
