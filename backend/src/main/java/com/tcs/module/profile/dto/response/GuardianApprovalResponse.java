package com.tcs.module.profile.dto.response;

import com.tcs.module.profile.enums.GuardianApprovalActionType;
import com.tcs.module.profile.enums.GuardianApprovalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianApprovalResponse {

    private Long approvalId;
    private GuardianApprovalActionType actionType;
    private GuardianApprovalStatus status;
    private BigDecimal amount;
    private String description;
    private String tutorName;
    private String subjectName;
    private String contractReference;
    private Long paymentTransactionId;
    private Long minorUserId;
    private String minorName;
    private Long parentUserId;
    private String parentName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
