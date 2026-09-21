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
import com.tcs.module.platform.dto.request.ResolveReviewReportRequest;
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
import java.time.LocalDate;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.profile.enums.UserRole;
import java.util.List;

/**
 * ============================================================================
 * [BF-10] GIAO DIỆN QUẢN TRỊ NỀN TẢNG TOÀN DIỆN (PLATFORM SERVICE INTERFACE)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-06-23
 * 
 * 1. Mục đích & Chức năng:
 *    - Định nghĩa các phương thức nghiệp vụ quản trị sàn cho Quản trị viên (Platform Admin).
 *    - Quản lý người dùng, tài khoản đa vai trò và phân quyền hệ thống [UC-07].
 *    - Thẩm định danh tính, phê duyệt hồ sơ xác minh KYC CCCD / Bằng cấp [UC-11].
 *    - Quản lý, xử lý báo cáo vi phạm, sự cố lớp học [UC-30, UC-52] và kiểm duyệt đánh giá [UC-55].
 *    - Tiếp nhận, xử lý khiếu nại qua hệ thống Support Ticket, tự động leo thang SLA [UC-63, UC-66].
 *    - Quản lý mẫu hợp đồng điện tử Master [UC-45] và cấu hình biểu phí sàn [UC-46].
 * ============================================================================
 */
public interface PlatformService {

    PageUserListResponse getUsers(int page, int size, UserStatus status, UserRole role, String keyword);

    UserListItemResponse createUser(com.tcs.module.platform.dto.request.CreateUserAdminRequest request);

    UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    UserListItemResponse updateUser(Long userId, com.tcs.module.platform.dto.request.UpdateUserAdminRequest request);

    DashboardResponse getDashboard(LocalDate from, LocalDate to, String granularity);

    List<VerificationRequestResponse> listVerificationRequests();

    VerificationDetailResponse getVerificationDetail(Long verificationId);

    VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);

    List<ReportResponse> listReports();

    ReportResponse resolveClassIssue(Long reportId, ResolveClassIssueRequest request);

    List<ReportResponse> listCenterReports();

    ReportResponse resolveCenterClassIssue(Long reportId, ResolveClassIssueRequest request);

    ReportResponse resolveReport(Long reportId, ResolveReportRequest request);

    /** Xử lý báo cáo vi phạm nhắm vào một đánh giá (targetType = REVIEW). */
    ReportResponse resolveReviewReport(Long reportId, ResolveReviewReportRequest request);

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

    /** Admin gộp ticket trùng vào ticket chính (BF09-TC03). */
    SupportTicketDetailResponse mergeTicket(Long sourceTicketId, com.tcs.module.platform.dto.request.MergeTicketRequest request);

    /** Quét, tự động nâng độ ưu tiên và gửi nhắc nhở cho các ticket quá hạn SLA (BF09-TC02). */
    int scanAndEscalateSlaBreaches();

    /** Admin chuyển tiếp ticket hỗ trợ sang luồng xử lý tranh chấp BF-08 (BF09-TC07). */
    SupportTicketDetailResponse redirectTicketToDispute(Long ticketId, com.tcs.module.platform.dto.request.RedirectDisputeRequest request);

    /** UC-45: Quản lý mẫu hợp đồng điện tử (E-Contract Templates) cho Admin. */
    List<com.tcs.module.center.dto.response.ContractTemplateResponse> listContractTemplates();

    com.tcs.module.center.dto.response.ContractTemplateResponse createContractTemplate(
            com.tcs.module.center.dto.request.SaveContractTemplateRequest request);

    com.tcs.module.center.dto.response.ContractTemplateResponse updateContractTemplate(
            Long templateId, com.tcs.module.center.dto.request.SaveContractTemplateRequest request);

    void deleteContractTemplate(Long templateId);

    /** UC-21: Giám sát lịch học và điểm danh toàn hệ thống theo ngày cho Admin. */
    List<com.tcs.module.center.dto.response.CenterScheduleClassResponse> getPlatformSchedule(java.time.LocalDate date);

    /** UC-46: Lấy danh sách cấu hình phí của các trung tâm gia sư. */
    List<com.tcs.module.platform.dto.response.CenterFeeConfigResponse> listCenterFeeConfigs();

    /** UC-46: Cập nhật tỷ lệ phí riêng cho một trung tâm gia sư. */
    com.tcs.module.platform.dto.response.CenterFeeConfigResponse updateCenterFeeConfig(
            Long centerId, com.tcs.module.platform.dto.request.UpdateCenterFeeRequest request);

    /** UC-46: Xóa cấu hình phí riêng của trung tâm (quay về dùng phí mặc định sàn). */
    com.tcs.module.platform.dto.response.CenterFeeConfigResponse resetCenterFeeConfig(Long centerId);
}

