package com.tcs.module.profile.service;

import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.profile.dto.response.GuardianApprovalResponse;
import com.tcs.module.profile.service.ClientLegalAccountService.LegalAccountContext;
import java.math.BigDecimal;
import java.util.List;

public interface GuardianApprovalService {

    GuardianApprovalResponse submitDepositApproval(LegalAccountContext legalContext, PaymentTransaction transaction);

    GuardianApprovalResponse submitContractApproval(
            LegalAccountContext legalContext, String tutorName, String subjectName, String contractReference);

    List<GuardianApprovalResponse> getPendingApprovalsForParent();

    List<GuardianApprovalResponse> getMySubmittedApprovals();

    GuardianApprovalResponse approve(Long approvalId);

    GuardianApprovalResponse reject(Long approvalId);
}
