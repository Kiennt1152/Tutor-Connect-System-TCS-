package com.tcs.module.platform.service;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.platform.dto.request.CloseTicketRequest;
import com.tcs.module.platform.dto.request.ModerateReviewRequest;
import com.tcs.module.platform.dto.request.RespondTicketRequest;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.ResolveClassIssueRequest;
import com.tcs.module.platform.dto.request.ResolveReportRequest;
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
import com.tcs.module.profile.enums.UserRole;
import java.util.List;

public interface PlatformService {

    PageUserListResponse getUsers(int page, int size, UserStatus status, UserRole role, String keyword);

    UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    DashboardResponse getDashboard();

    List<VerificationRequestResponse> listVerificationRequests();

    VerificationDetailResponse getVerificationDetail(Long verificationId);

    VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);

    List<ReportResponse> listReports();

    ReportResponse resolveClassIssue(Long reportId, ResolveClassIssueRequest request);

    List<ReportResponse> listCenterReports();

    ReportResponse resolveCenterClassIssue(Long reportId, ResolveClassIssueRequest request);

    ReportResponse resolveReport(Long reportId, ResolveReportRequest request);

    List<AdminReviewResponse> listReviews(ReviewStatus status);

    AdminReviewResponse moderateReview(Long reviewId, ModerateReviewRequest request);

    void deleteReview(Long reviewId);

    /** Danh sách ticket hỗ trợ, có filter theo status/category/priority/keyword và phân trang. */
    PageSupportTicketResponse getTickets(
            int page, int size, SupportTicketStatus status,
            SupportTicketCategory category, SupportTicketPriority priority, String keyword);

    /** Mở chi tiết ticket: tự động gán admin hiện tại nếu chưa có, chuyển OPEN -> IN_PROGRESS. */
    SupportTicketDetailResponse getTicketDetail(Long ticketId);

    /** Admin phân loại lại category và/hoặc điều chỉnh priority của ticket. */
    SupportTicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request);

    /** Admin gửi phản hồi vào hội thoại của ticket, chuyển trạng thái sang IN_REVIEW và báo cho người dùng. */
    SupportTicketDetailResponse respondToTicket(Long ticketId, RespondTicketRequest request);

    /** Admin đóng ticket (RESOLVED hoặc CLOSED). */
    SupportTicketDetailResponse closeTicket(Long ticketId, CloseTicketRequest request);
}
