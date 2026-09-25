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
 *    - Quản lý, xử lý báo cáo vi phạm, sự cố lớp học [UC-49, UC-52] và kiểm duyệt đánh giá [UC-55].
 *    - Tiếp nhận, xử lý khiếu nại qua hệ thống Support Ticket, tự động leo thang SLA [UC-63, UC-66].
 *    - Quản lý mẫu hợp đồng điện tử Master [UC-45] và cấu hình biểu phí sàn [UC-46].
 * ============================================================================
 */
public interface PlatformService {

    /**
     * [UC-07] Tra cứu danh sách người dùng phân trang theo bộ lọc vai trò, trạng thái và từ khóa.
     * 
     * @param page Số trang hiện tại (0-indexed)
     * @param size Kích thước trang
     * @param status Bộ lọc trạng thái tài khoản {@link UserStatus}
     * @param role Bộ lọc vai trò {@link UserRole}
     * @param keyword Từ khóa tìm kiếm (email, tên, sđt)
     * @return Kết quả phân trang {@link PageUserListResponse}
     */
    PageUserListResponse getUsers(int page, int size, UserStatus status, UserRole role, String keyword);

    /**
     * [UC-07] Tạo tài khoản người dùng mới trực tiếp bởi Quản trị viên sàn.
     * 
     * @param request Dữ liệu tạo tài khoản {@link com.tcs.module.platform.dto.request.CreateUserAdminRequest}
     * @return Thông tin người dùng vừa được tạo {@link UserListItemResponse}
     */
    UserListItemResponse createUser(com.tcs.module.platform.dto.request.CreateUserAdminRequest request);

    /**
     * [UC-07] Cập nhật trạng thái tài khoản người dùng (Khóa/Mở/Đình chỉ) và thu hồi phiên đăng nhập.
     * 
     * @param userId ID người dùng cần cập nhật
     * @param request Trạng thái mới và lý do {@link UpdateUserStatusRequest}
     * @return Dữ liệu người dùng sau khi cập nhật {@link UserListItemResponse}
     */
    UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    /**
     * [UC-07] Cập nhật thông tin định danh và hồ sơ người dùng bởi Quản trị viên.
     * 
     * @param userId ID người dùng cần chỉnh sửa
     * @param request Dữ liệu cập nhật {@link com.tcs.module.platform.dto.request.UpdateUserAdminRequest}
     * @return Dữ liệu người dùng sau khi chỉnh sửa {@link UserListItemResponse}
     */
    UserListItemResponse updateUser(Long userId, com.tcs.module.platform.dto.request.UpdateUserAdminRequest request);

    /**
     * [UC-56] Tổng hợp chỉ số điều hành nền tảng (Dashboard KPI, Doanh thu, Escrow, Người dùng).
     * 
     * @param from Ngày bắt đầu chu kỳ thống kê
     * @param to Ngày kết thúc chu kỳ thống kê
     * @param granularity Độ chi tiết nhóm thời gian (DAY, WEEK, MONTH)
     * @return Dữ liệu tổng hợp Dashboard {@link DashboardResponse}
     */
    DashboardResponse getDashboard(LocalDate from, LocalDate to, String granularity);

    /**
     * [UC-11] Lấy danh sách các yêu cầu xác minh danh tính và hồ sơ KYC đang chờ duyệt.
     * 
     * @return Danh sách yêu cầu KYC {@link VerificationRequestResponse}
     */
    List<VerificationRequestResponse> listVerificationRequests();

    /**
     * [UC-11] Xem chi tiết hồ sơ xác minh KYC kèm tài liệu CCCD/bằng cấp và lịch sử thẩm định.
     * 
     * @param verificationId ID yêu cầu xác minh KYC
     * @return Chi tiết hồ sơ xác minh {@link VerificationDetailResponse}
     */
    VerificationDetailResponse getVerificationDetail(Long verificationId);

    /**
     * [UC-11] Phê duyệt hoặc từ chối hồ sơ xác minh KYC và đồng bộ trạng thái hồ sơ người dùng.
     * 
     * @param verificationId ID yêu cầu xác minh
     * @param request Kết quả thẩm định {@link ReviewVerificationRequest}
     * @return Bản ghi yêu cầu xác minh sau khi xử lý {@link VerificationRequestResponse}
     */
    VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);

    /**
     * [UC-49] Lấy danh sách tất cả các báo cáo vi phạm trên sàn, sắp xếp mới nhất trước.
     * 
     * @return Danh sách báo cáo vi phạm {@link ReportResponse}
     */
    List<ReportResponse> listReports();

    /**
     * [UC-49, UC-52] Xử lý báo cáo sự cố lớp học với 7 phương án can thiệp nghiệp vụ hoặc chuyển tiếp tranh chấp.
     * 
     * @param reportId ID báo cáo sự cố
     * @param request Phương án xử lý {@link ResolveClassIssueRequest}
     * @return Kết quả xử lý báo cáo {@link ReportResponse}
     */
    ReportResponse resolveClassIssue(Long reportId, ResolveClassIssueRequest request);

    /**
     * [UC-49] Lấy danh sách các báo cáo vi phạm liên quan đến các lớp học thuộc quyền quản lý của Trung tâm.
     * 
     * @return Danh sách báo cáo của Trung tâm {@link ReportResponse}
     */
    List<ReportResponse> listCenterReports();

    /**
     * [UC-49] Trung tâm gia sư xử lý sự cố lớp học nội bộ thuộc quyền quản lý của đơn vị mình.
     * 
     * @param reportId ID báo cáo sự cố
     * @param request Phương án giải quyết {@link ResolveClassIssueRequest}
     * @return Báo cáo sau khi xử lý {@link ReportResponse}
     */
    ReportResponse resolveCenterClassIssue(Long reportId, ResolveClassIssueRequest request);

    /**
     * [UC-49] Quản trị viên xử lý báo cáo vi phạm chung (Người dùng, Đánh giá, Nội dung xấu).
     * 
     * @param reportId ID báo cáo vi phạm
     * @param request Hành động xử lý {@link ResolveReportRequest}
     * @return Kết quả giải quyết báo cáo {@link ReportResponse}
     */
    ReportResponse resolveReport(Long reportId, ResolveReportRequest request);

    /**
     * [UC-49, UC-55] Xử lý báo cáo vi phạm nhắm vào một đánh giá (targetType = REVIEW).
     * 
     * @param reportId ID báo cáo đánh giá
     * @param request Hành động xử lý {@link ResolveReviewReportRequest}
     * @return Kết quả giải quyết {@link ReportResponse}
     */
    ReportResponse resolveReviewReport(Long reportId, ResolveReviewReportRequest request);

    /**
     * [UC-53, UC-55] Lấy danh sách các đánh giá trên sàn kèm bộ lọc trạng thái kiểm duyệt.
     * 
     * @param status Bộ lọc trạng thái {@link ReviewStatus} (null lấy tất cả)
     * @return Danh sách đánh giá {@link AdminReviewResponse}
     */
    List<AdminReviewResponse> listReviews(ReviewStatus status);

    /**
     * [UC-55] Kiểm duyệt đánh giá (Phê duyệt công khai hoặc Ẩn khỏi hệ thống).
     * 
     * @param reviewId ID đánh giá
     * @param request Quyết định kiểm duyệt {@link ModerateReviewRequest}
     * @return Đánh giá sau kiểm duyệt {@link AdminReviewResponse}
     */
    AdminReviewResponse moderateReview(Long reviewId, ModerateReviewRequest request);

    /**
     * [UC-55] Xóa vĩnh viễn một đánh giá vi phạm quy chuẩn khỏi hệ thống.
     * 
     * @param reviewId ID đánh giá cần xóa
     */
    void deleteReview(Long reviewId);

    /**
     * [UC-66] Lấy danh sách ticket hỗ trợ phân trang kèm bộ lọc đa tiêu chí (trạng thái, danh mục, ưu tiên).
     * 
     * @param page Số trang hiện tại
     * @param size Kích thước trang
     * @param status Bộ lọc trạng thái ticket
     * @param category Bộ lọc danh mục
     * @param priority Bộ lọc độ ưu tiên
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách ticket phân trang {@link PageSupportTicketResponse}
     */
    PageSupportTicketResponse getTickets(
            int page, int size, SupportTicketStatus status,
            SupportTicketCategory category, SupportTicketPriority priority, String keyword);

    /**
     * [UC-66] Mở chi tiết ticket: Tự động gán admin hiện tại và chuyển trạng thái OPEN -> IN_PROGRESS.
     * 
     * @param ticketId ID ticket hỗ trợ
     * @return Chi tiết ticket {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse getTicketDetail(Long ticketId);

    /**
     * [UC-66] Điều chỉnh phân loại danh mục hoặc mức độ ưu tiên của ticket hỗ trợ.
     * 
     * @param ticketId ID ticket
     * @param request Cập nhật danh mục và mức ưu tiên {@link UpdateTicketRequest}
     * @return Chi tiết ticket sau khi cập nhật {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request);

    /**
     * [UC-66] Quản trị viên phản hồi vào hội thoại ticket, chuyển sang IN_REVIEW và gửi thông báo cho người dùng.
     * 
     * @param ticketId ID ticket
     * @param request Nội dung phản hồi và bằng chứng {@link RespondTicketRequest}
     * @return Chi tiết ticket sau phản hồi {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse respondToTicket(Long ticketId, RespondTicketRequest request);

    /**
     * [UC-66] Đóng ticket hỗ trợ khi vấn đề đã được giải quyết hoặc không còn hiệu lực.
     * 
     * @param ticketId ID ticket
     * @param request Quyết định đóng ticket {@link CloseTicketRequest}
     * @return Chi tiết ticket sau khi đóng {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse closeTicket(Long ticketId, CloseTicketRequest request);

    /**
     * [UC-66] Gộp các ticket trùng lặp vào ticket chính và chuyển trạng thái ticket phụ sang MERGED.
     * 
     * @param sourceTicketId ID ticket nguồn (ticket trùng lặp cần gộp)
     * @param request Thông tin ticket đích {@link com.tcs.module.platform.dto.request.MergeTicketRequest}
     * @return Chi tiết ticket chính sau khi hợp nhất {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse mergeTicket(Long sourceTicketId, com.tcs.module.platform.dto.request.MergeTicketRequest request);

    /**
     * [UC-66] Tác vụ tự động quét và nâng độ ưu tiên cho các ticket bị quá hạn cam kết SLA.
     * 
     * @return Số lượng ticket đã được tự động leo thang ưu tiên
     */
    int scanAndEscalateSlaBreaches();

    /**
     * [UC-66, UC-49] Chuyển tiếp ticket hỗ trợ sang phân hệ tranh chấp tài chính Escrow (Dispute Redirect).
     * 
     * @param ticketId ID ticket hỗ trợ
     * @param request Thông tin chuyển tiếp tranh chấp {@link com.tcs.module.platform.dto.request.RedirectDisputeRequest}
     * @return Chi tiết ticket sau khi chuyển tiếp sang tranh chấp {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse redirectTicketToDispute(Long ticketId, com.tcs.module.platform.dto.request.RedirectDisputeRequest request);

    /**
     * [UC-45] Lấy danh sách toàn bộ các mẫu hợp đồng điện tử Master của hệ thống.
     * 
     * @return Danh sách mẫu hợp đồng {@link com.tcs.module.center.dto.response.ContractTemplateResponse}
     */
    List<com.tcs.module.center.dto.response.ContractTemplateResponse> listContractTemplates();

    /**
     * [UC-45] Tạo mới một mẫu hợp đồng điện tử Master phục vụ sinh hợp đồng gia sư.
     * 
     * @param request Dữ liệu mẫu hợp đồng {@link com.tcs.module.center.dto.request.SaveContractTemplateRequest}
     * @return Thông tin mẫu hợp đồng vừa tạo {@link com.tcs.module.center.dto.response.ContractTemplateResponse}
     */
    com.tcs.module.center.dto.response.ContractTemplateResponse createContractTemplate(
            com.tcs.module.center.dto.request.SaveContractTemplateRequest request);

    /**
     * [UC-45] Cập nhật nội dung, điều khoản và định dạng của mẫu hợp đồng điện tử.
     * 
     * @param templateId ID mẫu hợp đồng cần chỉnh sửa
     * @param request Dữ liệu cập nhật {@link com.tcs.module.center.dto.request.SaveContractTemplateRequest}
     * @return Thông tin mẫu hợp đồng sau chỉnh sửa {@link com.tcs.module.center.dto.response.ContractTemplateResponse}
     */
    com.tcs.module.center.dto.response.ContractTemplateResponse updateContractTemplate(
            Long templateId, com.tcs.module.center.dto.request.SaveContractTemplateRequest request);

    /**
     * [UC-45] Xóa bỏ một mẫu hợp đồng điện tử khỏi hệ thống.
     * 
     * @param templateId ID mẫu hợp đồng cần xóa
     */
    void deleteContractTemplate(Long templateId);

    /**
     * [UC-21] Giám sát lịch học và tình hình điểm danh trên toàn sàn theo ngày được chọn.
     * 
     * @param date Ngày cần giám sát lịch học
     * @return Danh sách lớp học và tình trạng điểm danh trong ngày
     */
    List<com.tcs.module.center.dto.response.CenterScheduleClassResponse> getPlatformSchedule(java.time.LocalDate date);

    /**
     * [UC-46] Lấy danh sách cấu hình tỷ lệ phí sàn của tất cả các trung tâm gia sư đối tác.
     * 
     * @return Danh sách cấu hình phí sàn {@link com.tcs.module.platform.dto.response.CenterFeeConfigResponse}
     */
    List<com.tcs.module.platform.dto.response.CenterFeeConfigResponse> listCenterFeeConfigs();

    /**
     * [UC-46] Cập nhật tỷ lệ phí dịch vụ sàn thỏa thuận riêng cho một trung tâm gia sư.
     * 
     * @param centerId ID trung tâm gia sư
     * @param request Tỷ lệ phí mới {@link com.tcs.module.platform.dto.request.UpdateCenterFeeRequest}
     * @return Cấu hình phí sau khi cập nhật {@link com.tcs.module.platform.dto.response.CenterFeeConfigResponse}
     */
    com.tcs.module.platform.dto.response.CenterFeeConfigResponse updateCenterFeeConfig(
            Long centerId, com.tcs.module.platform.dto.request.UpdateCenterFeeRequest request);

    /**
     * [UC-46] Xóa cấu hình phí riêng của trung tâm (khôi phục về tỷ lệ phí mặc định sàn).
     * 
     * @param centerId ID trung tâm gia sư
     * @return Cấu hình phí mặc định {@link com.tcs.module.platform.dto.response.CenterFeeConfigResponse}
     */
    com.tcs.module.platform.dto.response.CenterFeeConfigResponse resetCenterFeeConfig(Long centerId);
}

