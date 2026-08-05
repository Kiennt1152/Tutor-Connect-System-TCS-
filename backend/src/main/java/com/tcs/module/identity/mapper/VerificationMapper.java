package com.tcs.module.identity.mapper;

import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.dto.response.VerificationResponse.DocumentResponse;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VerificationMapper {

    public VerificationResponse toResponse(VerificationRequest request, List<VerificationDocument> documents) {
        return VerificationResponse.builder()
                .verificationId(request.getVerificationId())
                .userId(request.getUser().getUserId())
                .userEmail(request.getUser().getEmail())
                .verificationType(request.getVerificationType())
                .status(request.getStatus())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewedByAdminEmail(null)
                .adminNotes(request.getAdminNotes())
                .rejectionReason(request.getRejectionReason())
                .documents(toDocumentResponses(documents))
                .resubmittable(isResubmittable(request.getStatus()))
                .build();
    }

    public VerificationResponse toResponse(VerificationRequest request, List<VerificationDocument> documents, String adminEmail) {
        VerificationResponse response = toResponse(request, documents);
        response.setReviewedByAdminEmail(adminEmail);
        return response;
    }

    private List<DocumentResponse> toDocumentResponses(List<VerificationDocument> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    private DocumentResponse toDocumentResponse(VerificationDocument doc) {
        return DocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .fileId(doc.getFile().getFileId())
                .fileName(doc.getFile().getFileName())
                .fileUrl(doc.getFile().getFileUrl())
                .mimeType(doc.getFile().getMimeType())
                .fileSize(doc.getFile().getFileSize())
                .documentType(doc.getDocumentType())
                .uploadedAt(doc.getFile().getCreatedAt())
                .build();
    }

    private boolean isResubmittable(VerificationStatus status) {
        return status == VerificationStatus.REJECTED;
    }
}
