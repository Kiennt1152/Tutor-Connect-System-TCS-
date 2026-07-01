package com.tcs.module.platform.controller;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.platform.service.PlatformService;
import com.tcs.module.profile.enums.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping("/users")
    public PageUserListResponse getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String keyword) {
        return platformService.getUsers(page, size, status, role, keyword);
    }

    @PatchMapping("/users/{userId}/status")
    public UserListItemResponse updateUserStatus(
            @PathVariable Long userId, @RequestBody UpdateUserStatusRequest request) {
        return platformService.updateUserStatus(userId, request);
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return platformService.getDashboard();
    }

    @GetMapping("/verifications")
    public List<VerificationRequestResponse> listVerifications() {
        return platformService.listVerificationRequests();
    }

    @PatchMapping("/verifications/{verificationId}")
    public VerificationRequestResponse reviewVerification(
            @PathVariable Long verificationId, @RequestBody ReviewVerificationRequest request) {
        return platformService.reviewVerification(verificationId, request);
    }

    @GetMapping("/reports")
    public List<ReportResponse> listReports() {
        return platformService.listReports();
    }
}
