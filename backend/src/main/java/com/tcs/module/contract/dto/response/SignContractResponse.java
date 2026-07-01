package com.tcs.module.contract.dto.response;

import com.tcs.module.profile.enums.GuardianApprovalStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignContractResponse {

    private String contractReference;
    private String signerName;
    private String beneficiaryMinorName;
    private boolean signedByParentOnBehalf;
    private LocalDateTime signedAt;
    private String message;
    private boolean pendingGuardianApproval;
    private Long guardianApprovalId;
    private GuardianApprovalStatus guardianApprovalStatus;
}
