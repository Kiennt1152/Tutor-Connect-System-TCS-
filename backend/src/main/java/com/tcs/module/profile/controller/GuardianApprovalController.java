package com.tcs.module.profile.controller;

import com.tcs.module.profile.dto.response.GuardianApprovalResponse;
import com.tcs.module.profile.service.GuardianApprovalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/guardian/approvals")
@RequiredArgsConstructor
public class GuardianApprovalController {

    private final GuardianApprovalService guardianApprovalService;

    @GetMapping("/pending")
    public List<GuardianApprovalResponse> getPendingApprovals() {
        return guardianApprovalService.getPendingApprovalsForParent();
    }

    @GetMapping("/submitted")
    public List<GuardianApprovalResponse> getMySubmittedApprovals() {
        return guardianApprovalService.getMySubmittedApprovals();
    }

    @PostMapping("/{approvalId}/approve")
    public GuardianApprovalResponse approve(@PathVariable Long approvalId) {
        return guardianApprovalService.approve(approvalId);
    }

    @PostMapping("/{approvalId}/reject")
    public GuardianApprovalResponse reject(@PathVariable Long approvalId) {
        return guardianApprovalService.reject(approvalId);
    }
}
