package com.tcs.module.identity.dto.response;

import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VerificationResponse {

    private Long verificationId;
    private Long userId;
    private String userEmail;
    private VerificationType verificationType;
    private VerificationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByAdminEmail;
    private String adminNotes;
    private String rejectionReason;
    private List<DocumentResponse> documents;
    private boolean resubmittable;

    @Getter
    @Setter
    @Builder
    public static class DocumentResponse {
        private Long documentId;
        private Long fileId;
        private String fileName;
        private String fileUrl;
        private String mimeType;
        private Long fileSize;
        private VerificationDocumentType documentType;
        private LocalDateTime uploadedAt;
    }
}
