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

/**
 * ====================================================================================================
 * [UC-06] XÁC NHẬN ĐỒNG THUẬN NGƯỜI GIÁM HỘ (GUARDIAN APPROVAL CONTROLLER)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Thu thập thông tin liên lạc và cam kết của phụ huynh/người giám hộ cho học viên vị thành niên.
 * 2. Gửi yêu cầu xác nhận bảo trợ qua email và ghi nhận biên bản chấp thuận tham gia lớp học.
 * 3. Đảm bảo tính pháp lý và an toàn cho học sinh dưới 18 tuổi khi tham gia học kèm trực tuyến.
 * * @author Nguyễn Trung Kiên (Kiennt1152)
 * @author Nguyễn Tiến Anh (tienanh6677)
 */
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
