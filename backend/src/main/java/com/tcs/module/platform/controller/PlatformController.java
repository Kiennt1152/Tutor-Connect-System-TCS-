package com.tcs.module.platform.controller;

import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.platform.dto.request.CreateUserAdminRequest;
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
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.service.PlatformService;
import java.time.LocalDate;
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

    // =========================================================================
    // LUỒNG 1: XÁC THỰC & QUẢN TRỊ TÀI KHOẢN NGƯỜI DÙNG (UC-07)
    // - Tìm kiếm & phân trang danh sách tài khoản theo vai trò/trạng thái
    // - Tạo tài khoản trực tiếp bởi Quản trị viên (Create User)
    // - Khóa / Mở khóa tài khoản (Cập nhật UserStatus)
    // - Cập nhật thông tin profile: Họ tên, Số điện thoại, Trạng thái (Update User)
    // =========================================================================

    @GetMapping("/users")
    public PageUserListResponse getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String keyword) {
        return platformService.getUsers(page, size, status, role, keyword);
    }

    @PostMapping("/users")
    public UserListItemResponse createUser(@Valid @RequestBody CreateUserAdminRequest request) {
        return platformService.createUser(request);
    }

    @PatchMapping("/users/{userId}/status")
    public UserListItemResponse updateUserStatus(
            @PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return platformService.updateUserStatus(userId, request);
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{userId}")
    public UserListItemResponse updateUser(
            @PathVariable Long userId, @RequestBody com.tcs.module.platform.dto.request.UpdateUserAdminRequest request) {
        return platformService.updateUser(userId, request);
    }

    // =========================================================================
    // LUỒNG 8: BẢNG ĐIỀU KHIỂN QUẢN TRỊ & GIÁM SÁT SỨC KHỎE DASHBOARD (UC-56)
    // - Thống kê thẻ KPI tổng quan: Người dùng, Lớp học, Tranh chấp, Hàng đợi SLA
    // =========================================================================
    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return platformService.getDashboard(from, to, granularity);
    }

    // =========================================================================
    // LUỒNG 2: THẨM ĐỊNH DANH TÍNH & DUYỆT HỒ SƠ XÁC MINH (UC-11)
    // - Xem danh sách yêu cầu xác minh CCCD, bằng cấp, giấy phép kinh doanh
    // - Kiểm tra hồ sơ tài liệu đính kèm (Ảnh 2 mặt CCCD, Bằng ĐH Sư Phạm)
    // - Phê duyệt (APPROVED) hoặc Từ chối (REJECTED) kèm lý do
    // =========================================================================

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

    // =========================================================================
    // LUỒNG 6: KHIẾU NẠI, BÁO CÁO VI PHẠM CHUNG (UC-52)
    // - Danh sách các báo cáo vi phạm do phụ huynh, gia sư hoặc trung tâm gửi lên
    // =========================================================================

    @GetMapping("/reports")
    public List<ReportResponse> listReports() {
        return platformService.listReports();
    }

    // =========================================================================
    // LUỒNG 8: ĐÁNH GIÁ, FAQ, DASHBOARD & KIỂM TOÁN (UC-53 & UC-55)
    // - Xem danh sách nhận xét công khai và nhận xét bị ẩn
    // - Kiểm duyệt đánh giá (Ẩn review vi phạm chuẩn mực cộng đồng)
    // - Xóa vĩnh viễn đánh giá sai sự thật hoặc bôi nhọ danh dự
    // =========================================================================

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

    // =========================================================================
    // LUỒNG 3 & LUỒNG 8: XỬ LÝ SỰ CỐ LỚP HỌC (UC-30) & BÁO CÁO ĐÁNH GIÁ (UC-55)
    // - Xử lý sự cố lớp học với 7 phương án can thiệp (Dạy bù, Đổi gia sư, Hoàn tiền...)
    // - Giải quyết báo cáo vi phạm chung
    // - Xử lý báo cáo vi phạm đối với đánh giá nhận xét gia sư
    // =========================================================================

    @PatchMapping("/reports/{reportId}/resolve")
    public ReportResponse resolveClassIssue(
            @PathVariable Long reportId,
            @RequestBody ResolveClassIssueRequest request) {
        return platformService.resolveClassIssue(reportId, request);
    }

    @PatchMapping("/reports/{reportId}")
    public ReportResponse resolveReport(
            @PathVariable Long reportId,
            @RequestBody ResolveReportRequest request) {
        return platformService.resolveReport(reportId, request);
    }

    @PatchMapping("/reports/{reportId}/resolve-review")
    public ReportResponse resolveReviewReport(
            @PathVariable Long reportId,
            @RequestBody ResolveReviewReportRequest request) {
        return platformService.resolveReviewReport(reportId, request);
    }

    // =========================================================================
    // LUỒNG 7: HỖ TRỢ KHÁCH HÀNG CSKH, XỬ LÝ TICKET & ĐO LƯỜNG SLA (UC-66)
    // - Danh sách Ticket hỗ trợ, lọc đa chiều (trạng thái, ưu tiên, phân loại)
    // - Xem chi tiết Ticket và lịch sử trao đổi giữa người dùng và Admin
    // - Gửi phản hồi chính thức (Respond) & Kích hoạt tính First Response Time SLA
    // - Đóng ticket / Giải quyết (Resolved/Closed)
    // - Gộp ticket trùng lặp (Merge Ticket) & Chuyển tiếp sang Tranh chấp (Redirect Dispute)
    // - Kích hoạt quét tự động và leo thang vi phạm thời hạn SLA (Job-11 Scan SLA)
    // =========================================================================

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

    @PostMapping("/tickets/{ticketId}/merge")
    public SupportTicketDetailResponse mergeTicket(
            @PathVariable Long ticketId, @Valid @RequestBody com.tcs.module.platform.dto.request.MergeTicketRequest request) {
        return platformService.mergeTicket(ticketId, request);
    }

    @PostMapping("/tickets/{ticketId}/redirect-dispute")
    public SupportTicketDetailResponse redirectTicketToDispute(
            @PathVariable Long ticketId, @RequestBody com.tcs.module.platform.dto.request.RedirectDisputeRequest request) {
        return platformService.redirectTicketToDispute(ticketId, request);
    }

    @PostMapping("/tickets/sla/scan")
    public java.util.Map<String, Object> triggerSlaScan() {
        int count = platformService.scanAndEscalateSlaBreaches();
        return java.util.Map.of("message", "Quét SLA hoàn tất", "escalatedCount", count);
    }

    // =========================================================================
    // LUỒNG 4: HỢP ĐỒNG ĐIỆN TỬ & QUẢN LÝ MẪU HỢP ĐỒNG MASTER (UC-45)
    // - Danh sách các mẫu hợp đồng mẫu toàn sàn và trung tâm
    // - Tạo mới mẫu hợp đồng với nội dung pháp lý và các biến giữ chỗ {{placeholder}}
    // - Cập nhật / Tùy chỉnh điều khoản mẫu hợp đồng
    // - Xóa / Vô hiệu hóa mẫu hợp đồng
    // =========================================================================

    @GetMapping("/contract-templates")
    public List<com.tcs.module.center.dto.response.ContractTemplateResponse> listContractTemplates() {
        return platformService.listContractTemplates();
    }

    @PostMapping("/contract-templates")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public com.tcs.module.center.dto.response.ContractTemplateResponse createContractTemplate(
            @RequestBody com.tcs.module.center.dto.request.SaveContractTemplateRequest request) {
        return platformService.createContractTemplate(request);
    }

    @org.springframework.web.bind.annotation.PutMapping("/contract-templates/{templateId}")
    public com.tcs.module.center.dto.response.ContractTemplateResponse updateContractTemplate(
            @PathVariable Long templateId,
            @RequestBody com.tcs.module.center.dto.request.SaveContractTemplateRequest request) {
        return platformService.updateContractTemplate(templateId, request);
    }

    @DeleteMapping("/contract-templates/{templateId}")
    public void deleteContractTemplate(@PathVariable Long templateId) {
        platformService.deleteContractTemplate(templateId);
    }

    // =========================================================================
    // LUỒNG 3: GIÁM SÁT LỚP HỌC & LỊCH HỌC TOÀN HỆ THỐNG THEO NGÀY (UC-21)
    // - Theo dõi các ca học, tiến độ buổi học theo ngày được chọn trên toàn sàn
    // - Đã tối ưu hiệu năng: Lọc trước trạng thái lớp đang hoạt động tại Database
    // =========================================================================

    @GetMapping("/classes/schedule")
    public List<com.tcs.module.center.dto.response.CenterScheduleClassResponse> getPlatformSchedule(
            @RequestParam(required = false) java.time.LocalDate date) {
        return platformService.getPlatformSchedule(date);
    }

    // =========================================================================
    // LUỒNG 5: KÝ QUỸ ESCROW, TÀI CHÍNH & CẤU HÌNH PHÍ RIÊNG TRUNG TÂM (UC-46)
    // - Xem danh sách các trung tâm gia sư và tỷ lệ phí nền tảng áp dụng
    // - Thiết lập tỷ lệ phí chiết khấu riêng biệt cho trung tâm đối tác (customFeeRate)
    // - Khôi phục biểu phí về mức phí mặc định toàn sàn (2%)
    // =========================================================================

    @GetMapping("/fees/centers")
    public List<com.tcs.module.platform.dto.response.CenterFeeConfigResponse> listCenterFeeConfigs() {
        return platformService.listCenterFeeConfigs();
    }

    @org.springframework.web.bind.annotation.PutMapping("/fees/centers/{centerId}")
    public com.tcs.module.platform.dto.response.CenterFeeConfigResponse updateCenterFeeConfig(
            @PathVariable Long centerId,
            @RequestBody @Valid com.tcs.module.platform.dto.request.UpdateCenterFeeRequest request) {
        return platformService.updateCenterFeeConfig(centerId, request);
    }

    @DeleteMapping("/fees/centers/{centerId}")
    public com.tcs.module.platform.dto.response.CenterFeeConfigResponse resetCenterFeeConfig(
            @PathVariable Long centerId) {
        return platformService.resetCenterFeeConfig(centerId);
    }
}

