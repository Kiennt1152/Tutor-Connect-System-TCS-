package com.tcs.module.platform.controller;

import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.platform.dto.request.CloseTicketRequest;
import com.tcs.module.platform.dto.request.ModerateReviewRequest;
import com.tcs.module.platform.dto.request.RespondTicketRequest;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.ResolveClassIssueRequest;
import com.tcs.module.platform.dto.request.UpdateTicketRequest;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.AdminReviewResponse;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageSupportTicketResponse;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.dto.response.VerificationDetailResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.service.PlatformService;
import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
            @PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
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

    @GetMapping("/verifications/{verificationId}")
    public VerificationDetailResponse getVerificationDetail(@PathVariable Long verificationId) {
        return platformService.getVerificationDetail(verificationId);
    }

    @PatchMapping("/verifications/{verificationId}")
    public VerificationRequestResponse reviewVerification(
            @PathVariable Long verificationId, @Valid @RequestBody ReviewVerificationRequest request) {
        return platformService.reviewVerification(verificationId, request);
    }

    @GetMapping("/reports")
    public List<ReportResponse> listReports() {
        return platformService.listReports();
    }

    @GetMapping("/reviews")
    public List<AdminReviewResponse> listReviews(
            @RequestParam(required = false) ReviewStatus status) {
        return platformService.listReviews(status);
    }

    @PatchMapping("/reviews/{reviewId}")
    public AdminReviewResponse moderateReview(
            @PathVariable Long reviewId, @Valid @RequestBody ModerateReviewRequest request) {
        return platformService.moderateReview(reviewId, request);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        platformService.deleteReview(reviewId);
    }

    @PatchMapping("/reports/{reportId}/resolve")
    public ReportResponse resolveClassIssue(
            @PathVariable Long reportId,
            @RequestBody ResolveClassIssueRequest request) {
        return platformService.resolveClassIssue(reportId, request);
    }

    @GetMapping("/tickets")
    public PageSupportTicketResponse getTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketCategory category,
            @RequestParam(required = false) SupportTicketPriority priority,
            @RequestParam(required = false) String keyword) {
        return platformService.getTickets(page, size, status, category, priority, keyword);
    }

    @GetMapping("/tickets/{ticketId}")
    public SupportTicketDetailResponse getTicketDetail(@PathVariable Long ticketId) {
        return platformService.getTicketDetail(ticketId);
    }

    @PatchMapping("/tickets/{ticketId}")
    public SupportTicketDetailResponse updateTicket(
            @PathVariable Long ticketId, @RequestBody UpdateTicketRequest request) {
        return platformService.updateTicket(ticketId, request);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public SupportTicketDetailResponse respondToTicket(
            @PathVariable Long ticketId, @Valid @RequestBody RespondTicketRequest request) {
        return platformService.respondToTicket(ticketId, request);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public SupportTicketDetailResponse closeTicket(
            @PathVariable Long ticketId, @Valid @RequestBody CloseTicketRequest request) {
        return platformService.closeTicket(ticketId, request);
    }
}
