package com.tcs.module.contract.dto.response;

import com.tcs.module.contract.enums.ContractSourceType;
import com.tcs.module.contract.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractResponse {

    private Long contractId;
    private String contractNo;
    private ContractStatus status;
    private ContractSourceType sourceType;

    private Long assignmentId;
    private Long classId;
    private Long classStudentId;

    private Long clientId;
    private String clientName;
    private String clientEmail;

    private Long tutorId;
    private String tutorName;
    private String tutorEmail;

    private Long centerId;
    private String centerName;
    private String centerEmail;

    private Long templateId;
    private String templateName;
    private String termsSummary;
    private String contractFileUrl;

    private boolean hasAllSignatures;
    private int signedCount;
    private int requiredSignatures;

    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Legacy contract pages still read these denormalized class fields.
    private String classTitle;
    private String classType;
    private BigDecimal tuitionFee;
    private String lessonMode;
    private Integer numberOfSessions;

    private PartyInfo tutor;
    private PartyInfo client;
    private PartyInfo center;

    @Getter
    @Builder
    public static class PartyInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String phone;
    }
}
