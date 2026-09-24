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

/**
 * ============================================================================
 * [BF-10] PHÂN HỆ ĐIỀU HÀNH & QUẢN TRỊ NỀN TẢNG TRUNG TÂM (PLATFORM CORE CONTROLLER)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-06-23
 * * 1. Danh mục các Use Case phụ trách chính:
 *   - [UC-07] Quản lý & Phân quyền tài khoản người dùng
 *   - [UC-11] Thẩm định danh tính & Phê duyệt hồ sơ xác minh (KYC / CCCD / Bằng cấp)
 *   - [UC-21] Giám sát lịch học & Điểm danh toàn sàn theo ngày
 *   - [UC-49 / UC-52] Can thiệp sự cố lớp học với 7 phương án nghiệp vụ & Chuyển tiếp tranh chấp Escrow
 *   - [UC-45] Quản lý mẫu hợp đồng điện tử Master (E-Contract Templates)
 *   - [UC-46] Cấu hình biểu phí sàn & Tỷ lệ phí riêng cho từng trung tâm gia sư
 *   - [UC-53] Xem & Thống kê đánh giá, phản hồi
 *   - [UC-55] Kiểm duyệt & Xóa đánh giá tiêu cực / vi phạm quy chuẩn
 *   - [UC-56] Bảng điều khiển quản trị Admin Dashboard & Giám sát sức khỏe sàn
 *   - [UC-66] Tiếp nhận, giải quyết Ticket hỗ trợ & Đo lường vi phạm SLA CSKH
 * * 2. Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu từ Admin Console, kiểm tra quyền PLATFORM_ADMIN qua Spring Security.
 *   - Bước 2: Gọi tầng nghiệp vụ PlatformService / PlatformAnalyticsService để thực thi các tác vụ quản trị.
 *   - Bước 3: Ghi vết kiểm toán (Audit Log) đối với các hành động can thiệp dữ liệu nhạy cảm.
 *   - Bước 4: Trả về dữ liệu chuẩn hóa DTO cho giao diện Admin.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    // =========================================================================
    // [UC-07]: XÁC THỰC & QUẢN TRỊ TÀI KHOẢN NGƯỜI DÙNG (USER GOVERNANCE)
    // =========================================================================

    /**
     * [UC-07]: Tìm kiếm, lọc và phân trang danh sách tài khoản người dùng trên sàn.
     *     * Nghiệp vụ:
     *   - Cho phép Admin tra cứu thông tin của tất cả 4 vai trò: CLIENT, TUTOR, TUTOR_CENTER, PLATFORM_ADMIN.
     *   - Hỗ trợ lọc theo Trạng thái tài khoản (ACTIVE, PENDING_APPROVAL, SUSPENDED, BANNED).
     *   - Tìm kiếm linh hoạt theo từ khóa: Email, Họ tên, Số điện thoại.
     *     * @param page Trang hiện tại (0-indexed)
     * @param size Số lượng bản ghi mỗi trang (mặc định 10, tối đa 50)
     * @param status Lọc theo trạng thái tài khoản
     * @param role Lọc theo vai trò người dùng
     * @param keyword Từ khóa tìm kiếm
     * @return {@link PageUserListResponse} Danh sách người dùng phân trang kèm thông tin hồ sơ rút gọn
     */
    @GetMapping("/users")
    public PageUserListResponse getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String keyword) {
        return platformService.getUsers(page, size, status, role, keyword);
    }

    /**
     * [UC-07]: Tạo tài khoản người dùng trực tiếp bởi Quản trị viên (Create User Admin).
     *     * Nghiệp vụ:
     *   - Áp dụng khi cần tạo tài khoản quản trị viên mới, hoặc cấp tài khoản đặc biệt cho đối tác.
     *   - Mã hóa mật khẩu bảo mật BCrypt, khởi tạo ví điện tử và bản ghi hồ sơ tương ứng.
     *     * @param request Dữ liệu tạo tài khoản {@link CreateUserAdminRequest}
     * @return {@link UserListItemResponse} Thông tin tài khoản vừa được tạo
     */
    @PostMapping("/users")
    public UserListItemResponse createUser(@Valid @RequestBody CreateUserAdminRequest request) {
        return platformService.createUser(request);
    }

    /**
     * [UC-07]: Cập nhật trạng thái tài khoản người dùng (Khóa / Mở khóa / Đình chỉ).
     *     * Nghiệp vụ:
     *   - Chuyển trạng thái giữa ACTIVE, SUSPENDED, BANNED.
     *   - Khi khóa tài khoản: Buộc vô hiệu hóa các JWT Token đang lưu hành (tăng tokenVersion).
     *   - Ghi vết vào Audit Log hệ thống [UC-61].
     *     * @param userId ID người dùng cần cập nhật trạng thái
     * @param request Trạng thái mới và lý do giải trình {@link UpdateUserStatusRequest}
     * @return {@link UserListItemResponse} Thông tin người dùng sau khi đổi trạng thái
     */
    @PatchMapping("/users/{userId}/status")
    public UserListItemResponse updateUserStatus(
            @PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return platformService.updateUserStatus(userId, request);
    }

    /**
     * [UC-07]: Cập nhật thông tin định danh và hồ sơ người dùng bởi Admin.
     *     * @param userId ID người dùng cần chỉnh sửa
     * @param request Họ tên mới, số điện thoại, trạng thái
     * @return {@link UserListItemResponse} Dữ liệu người dùng đã cập nhật
     */
    @org.springframework.web.bind.annotation.PutMapping("/users/{userId}")
    public UserListItemResponse updateUser(
            @PathVariable Long userId, @RequestBody com.tcs.module.platform.dto.request.UpdateUserAdminRequest request) {
        return platformService.updateUser(userId, request);
    }

    // =========================================================================
    // [UC-56]: BẢNG ĐIỀU KHIỂN QUẢN TRỊ & GIÁM SÁT CHỈ SỐ DASHBOARD
    // =========================================================================

    /**
     * [UC-56]: Thống kê chỉ số KPI tổng quan và biểu đồ sức khỏe vận hành sàn.
     *     * Dữ liệu bao gồm:
     *   - Thẻ thống kê (Metric Cards): Tổng người dùng, Lớp học đang chạy, Tranh chấp chờ xử lý, Ticket SLA.
     *   - Biểu đồ biến thiên dòng tiền theo ngày/tuần/tháng (Granularity: DAY, WEEK, MONTH).
     *   - Cảnh báo nhanh các trường hợp khẩn cấp (Hồ sơ KYC quá hạn, Ticket vi phạm SLA).
     *     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @param granularity Độ mịn biểu đồ ('DAY', 'WEEK', 'MONTH')
     * @return {@link DashboardResponse} Toàn bộ chỉ số KPI quản trị sàn
     */
    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return platformService.getDashboard(from, to, granularity);
    }

    // =========================================================================
    // [UC-11]: THẨM ĐỊNH DANH TÍNH & PHÊ DUYỆT HỒ SƠ XÁC MINH (KYC WORKFLOW)
    // =========================================================================

    /**
     * [UC-11]: Lấy danh sách các hồ sơ xác minh danh tính và năng lực chuyên môn đang chờ duyệt.
     *     * @return Danh sách các yêu cầu xác minh {@link VerificationRequestResponse}
     */
    @GetMapping("/verifications")
    public List<VerificationRequestResponse> listVerifications() {
        return platformService.listVerificationRequests();
    }

    /**
     * [UC-11]: Xem chi tiết hồ sơ xác minh kèm các tệp bằng chứng đính kèm (CCCD, Bằng ĐH, Giấy phép).
     *     * @param verificationId ID yêu cầu xác minh
     * @return {@link VerificationDetailResponse} Chi tiết thông tin cá nhân và liên kết tài liệu số hóa
     */
    @GetMapping("/verifications/{verificationId}")
    public VerificationDetailResponse getVerificationDetail(@PathVariable Long verificationId) {
        return platformService.getVerificationDetail(verificationId);
    }

    /**
     * [UC-11]: Phê duyệt (APPROVED) hoặc Từ chối (REJECTED) hồ sơ xác minh danh tính.
     *     * Nghiệp vụ:
     *   - Khi DUYỆT: Tự động gắn tích xanh xác minh (ProfileVerificationStatus = VERIFIED), cấp huy hiệu gia sư uy tín.
     *   - Khi TỪ CHỐI: Bắt buộc cung cấp lý do từ chối rõ ràng (tối thiểu 10 ký tự) để người dùng bổ sung giấy tờ.
     *   - Gửi thông báo tức thời đến người dùng và ghi nhận lịch sử thẩm định {@code verification_histories}.
     *     * @param verificationId ID yêu cầu xác minh
     * @param request Quyết định duyệt và ghi chú của Admin {@link ReviewVerificationRequest}
     * @return {@link VerificationRequestResponse} Trạng thái cập nhật của yêu cầu xác minh
     */
    @PatchMapping("/verifications/{verificationId}")
    public VerificationRequestResponse reviewVerification(
            @PathVariable Long verificationId, @Valid @RequestBody ReviewVerificationRequest request) {
        return platformService.reviewVerification(verificationId, request);
    }

    // =========================================================================
    // [UC-52]: TIẾP NHẬN & XỬ LÝ BÁO CÁO VI PHẠM (REPORTS)
    // =========================================================================

    /**
     * [UC-52]: Danh sách các báo cáo vi phạm cộng đồng do Phụ huynh hoặc Gia sư gửi lên.
     *     * @return Danh sách các báo cáo {@link ReportResponse}
     */
    @GetMapping("/reports")
    public List<ReportResponse> listReports() {
        return platformService.listReports();
    }

    // =========================================================================
    // [UC-53 & UC-55]: QUẢN TRỊ & KIỂM DUYỆT ĐÁNH GIÁ (REVIEWS & MODERATION)
    // =========================================================================

    /**
     * [UC-53]: Tra cứu danh sách đánh giá và phản hồi của người dùng trên toàn hệ thống.
     *     * @param status Lọc theo trạng thái (VISIBLE: Hiển thị, HIDDEN: Bị ẩn, PENDING: Chờ duyệt)
     * @return Danh sách đánh giá {@link AdminReviewResponse} kèm số sao và nội dung
     */
    @GetMapping("/reviews")
    public List<AdminReviewResponse> listReviews(
            @RequestParam(required = false) ReviewStatus status) {
        return platformService.listReviews(status);
    }

    /**
     * [UC-55]: Kiểm duyệt nội dung đánh giá (Ẩn đánh giá vi phạm thuần phong mỹ tục hoặc tiêu cực sai sự thật).
     *     * @param reviewId ID của đánh giá cần kiểm duyệt
     * @param request Trạng thái kiểm duyệt mới và lý do giải trình {@link ModerateReviewRequest}
     * @return {@link AdminReviewResponse} Đánh giá sau khi cập nhật trạng thái hiển thị
     */
    @PatchMapping("/reviews/{reviewId}")
    public AdminReviewResponse moderateReview(
            @PathVariable Long reviewId, @Valid @RequestBody ModerateReviewRequest request) {
        return platformService.moderateReview(reviewId, request);
    }

    /**
     * [UC-55]: Xóa vĩnh viễn đánh giá sai sự thật, xúc phạm hoặc vu khống khỏi cơ sở dữ liệu.
     *     * @param reviewId ID đánh giá cần xóa
     */
    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        platformService.deleteReview(reviewId);
    }

    // =========================================================================
    // [UC-49 / UC-52]: CAN THIỆP SỰ CỐ LỚP HỌC (CLASS ISSUES INTERVENTION)
    // =========================================================================

    /**
     * [UC-49 / UC-52]: Can thiệp và xử lý sự cố lớp học với 7 phương án nghiệp vụ toàn diện.
     *     * Các phương án xử lý (ClassIssueResolutionAction):
     *   1. CANCEL_CLASS: Hủy lớp học và thu hồi các quyền liên quan.
     *   2. CHANGE_TUTOR: Thay đổi gia sư phụ trách lớp mà không làm gián đoạn lịch học.
     *   3. RESCHEDULE_SLOT: Điều chỉnh lại khung giờ/buổi học phát sinh mâu thuẫn.
     *   4. REFUND_COMPENSATION: Hoàn tiền một phần hoặc toàn phần cho học viên từ quỹ Escrow.
     *   5. ISSUE_WARNING: Ban hành cảnh cáo vi phạm cho bên vi phạm cam kết giảng dạy.
     *   6. MEDIATE: Hòa giải giữa phụ huynh và trung tâm/gia sư với biên bản cam kết.
     *   7. DISMISS: Bác bỏ sự cố nếu không có căn cứ xác thực.
     *     * @param reportId ID bản ghi sự cố cần giải quyết
     * @param request Phương án lựa chọn và biên bản giải quyết {@link ResolveClassIssueRequest}
     * @return {@link ReportResponse} Trạng thái sau khi giải quyết
     */
    @PatchMapping("/reports/{reportId}/resolve")
    public ReportResponse resolveClassIssue(
            @PathVariable Long reportId,
            @RequestBody ResolveClassIssueRequest request) {
        return platformService.resolveClassIssue(reportId, request);
    }

    /**
     * [UC-52]: Xử lý báo cáo vi phạm nội quy chung giữa các người dùng trên sàn.
     *     * @param reportId ID báo cáo vi phạm
     * @param request Hành động xử lý và ghi chú {@link ResolveReportRequest}
     * @return {@link ReportResponse} Báo cáo sau khi xử lý
     */
    @PatchMapping("/reports/{reportId}")
    public ReportResponse resolveReport(
            @PathVariable Long reportId,
            @RequestBody ResolveReportRequest request) {
        return platformService.resolveReport(reportId, request);
    }

    /**
     * [UC-55]: Xử lý khiếu nại đối với một đánh giá nhận xét bị phản ánh sai sự thật.
     *     * @param reportId ID báo cáo đánh giá
     * @param request Quyết định giữ lại hoặc ẩn đánh giá {@link ResolveReviewReportRequest}
     * @return {@link ReportResponse} Kết quả xử lý khiếu nại đánh giá
     */
    @PatchMapping("/reports/{reportId}/resolve-review")
    public ReportResponse resolveReviewReport(
            @PathVariable Long reportId,
            @RequestBody ResolveReviewReportRequest request) {
        return platformService.resolveReviewReport(reportId, request);
    }

    // =========================================================================
    // [UC-66]: QUẢN LÝ TIẾP NHẬN YÊU CẦU HỖ TRỢ & ĐO LƯỜNG SLA (TICKET CSKH)
    // =========================================================================

    /**
     * [UC-66]: Tra cứu, lọc đa chiều và phân trang danh sách Ticket hỗ trợ khách hàng.
     *     * Bộ lọc nghiệp vụ:
     *   - {@code status}: OPEN (Mới mở), IN_PROGRESS (Đang xử lý), IN_REVIEW (Chờ phản hồi), RESOLVED (Đã giải quyết), CLOSED (Đã đóng).
     *   - {@code category}: ACCOUNT (Tài khoản), PAYMENT (Thanh toán), CLASS (Lớp học), TECHNICAL (Lỗi hệ thống), DISPUTE (Tranh chấp).
     *   - {@code priority}: LOW, MEDIUM, HIGH, URGENT (Khẩn cấp).
     *   - {@code keyword}: Tìm kiếm theo tiêu đề ticket hoặc nội dung trao đổi.
     *     * @return {@link PageSupportTicketResponse} Danh sách Ticket phân trang kèm chỉ số SLA quá hạn
     */
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

    /**
     * [UC-66]: Mở xem chi tiết Ticket hỗ trợ và toàn bộ lịch sử trao đổi qua lại giữa CSKH và người dùng.
     *     * Nghiệp vụ tự động:
     *   - Nếu Ticket đang ở trạng thái OPEN, hệ thống tự động gán Admin hiện tại phụ trách và chuyển sang IN_PROGRESS.
     *     * @param ticketId ID ticket hỗ trợ
     * @return {@link SupportTicketDetailResponse} Chi tiết ticket và hội thoại
     */
    @GetMapping("/tickets/{ticketId}")
    public SupportTicketDetailResponse getTicketDetail(@PathVariable Long ticketId) {
        return platformService.getTicketDetail(ticketId);
    }

    /**
     * [UC-66]: Điều chỉnh phân loại Category và/hoặc Nâng cấp độ ưu tiên Priority của Ticket.
     *     * @param ticketId ID ticket cần cập nhật
     * @param request Phân loại và độ ưu tiên mới {@link UpdateTicketRequest}
     * @return {@link SupportTicketDetailResponse} Ticket sau khi cập nhật
     */
    @PatchMapping("/tickets/{ticketId}")
    public SupportTicketDetailResponse updateTicket(
            @PathVariable Long ticketId, @RequestBody UpdateTicketRequest request) {
        return platformService.updateTicket(ticketId, request);
    }

    /**
     * [UC-66]: Admin gửi phản hồi giải quyết chính thức vào luồng trao đổi của Ticket.
     *     * Nghiệp vụ SLA:
     *   - Nếu đây là phản hồi đầu tiên của Admin, hệ thống tự động chốt thời gian phản hồi đầu (First Response Time)
     *     và tính toán chỉ số SLA (đạt chuẩn hoặc vi phạm SLA thời hạn 2h / 4h / 8h).
     *   - Chuyển trạng thái sang IN_REVIEW và gửi thông báo tức thời đến người yêu cầu.
     *     * @param ticketId ID ticket cần trả lời
     * @param request Nội dung tin nhắn phản hồi {@link RespondTicketRequest}
     * @return {@link SupportTicketDetailResponse} Ticket sau khi gửi tin
     */
    @PostMapping("/tickets/{ticketId}/messages")
    public SupportTicketDetailResponse respondToTicket(
            @PathVariable Long ticketId, @Valid @RequestBody RespondTicketRequest request) {
        return platformService.respondToTicket(ticketId, request);
    }

    /**
     * [UC-66]: Đóng / Hoàn tất xử lý Ticket hỗ trợ (Chuyển sang RESOLVED hoặc CLOSED).
     *     * @param ticketId ID ticket cần đóng
     * @param request Trạng thái đóng và đánh giá kết quả {@link CloseTicketRequest}
     * @return {@link SupportTicketDetailResponse} Ticket đã hoàn tất
     */
    @PatchMapping("/tickets/{ticketId}/status")
    public SupportTicketDetailResponse closeTicket(
            @PathVariable Long ticketId, @Valid @RequestBody CloseTicketRequest request) {
        return platformService.closeTicket(ticketId, request);
    }

    /**
     * [UC-66]: Gộp Ticket trùng lặp (Merge Ticket) vào Ticket chính.
     *     * Nghiệp vụ:
     *   - Khi người dùng gửi nhiều yêu cầu cho cùng một vấn đề, Admin gộp các ticket con vào ticket chính.
     *   - Đóng ticket con và chuyển toàn bộ tin nhắn liên quan sang ticket chính.
     *     * @param ticketId ID ticket nguồn cần gộp
     * @param request ID ticket đích chính {@link com.tcs.module.platform.dto.request.MergeTicketRequest}
     * @return {@link SupportTicketDetailResponse} Ticket chính sau khi sáp nhập
     */
    @PostMapping("/tickets/{ticketId}/merge")
    public SupportTicketDetailResponse mergeTicket(
            @PathVariable Long ticketId, @Valid @RequestBody com.tcs.module.platform.dto.request.MergeTicketRequest request) {
        return platformService.mergeTicket(ticketId, request);
    }

    /**
     * [UC-66]: Chuyển tiếp Ticket sang luồng Tranh chấp Khiếu nại chính thức (BF-08).
     *     * Nghiệp vụ:
     *   - Khi sự việc hỗ trợ vượt quá thẩm quyền CSKH thông thường (liên quan đến đòi tiền, hợp đồng vi phạm):
     *     Admin chuyển tiếp ticket sang phân hệ Tranh chấp Escrow [UC-49] để lập hội đồng phán xử.
     *     * @param ticketId ID ticket cần chuyển tiếp
     * @param request Lý do và hồ sơ chuyển tiếp {@link com.tcs.module.platform.dto.request.RedirectDisputeRequest}
     * @return {@link SupportTicketDetailResponse} Ticket đã chuyển trạng thái
     */
    @PostMapping("/tickets/{ticketId}/redirect-dispute")
    public SupportTicketDetailResponse redirectTicketToDispute(
            @PathVariable Long ticketId, @RequestBody com.tcs.module.platform.dto.request.RedirectDisputeRequest request) {
        return platformService.redirectTicketToDispute(ticketId, request);
    }

    /**
     * [UC-66]: Kích hoạt quét tự động và leo thang các Ticket quá hạn SLA (Job-11 SLA Escalation Scanner).
     *     * Nghiệp vụ:
     *   - Quét toàn bộ ticket chưa có phản hồi hoặc chưa giải quyết vượt quá SLA thời hạn cam kết.
     *   - Tự động nâng Priority lên HIGH hoặc URGENT, gửi email/thông báo cảnh báo khẩn đến Quản lý CSKH.
     *     * @return Thống kê số lượng ticket bị leo thang vi phạm SLA
     */
    @PostMapping("/tickets/sla/scan")
    public java.util.Map<String, Object> triggerSlaScan() {
        int count = platformService.scanAndEscalateSlaBreaches();
        return java.util.Map.of("message", "Quét SLA hoàn tất", "escalatedCount", count);
    }

    // =========================================================================
    // [UC-45]: QUẢN LÝ MẪU HỢP ĐỒNG ĐIỆN TỬ MASTER (E-CONTRACT TEMPLATES)
    // =========================================================================

    /**
     * [UC-45]: Danh sách các mẫu hợp đồng giảng dạy chuẩn hóa của sàn và trung tâm đối tác.
     *     * @return Danh sách mẫu hợp đồng {@link com.tcs.module.center.dto.response.ContractTemplateResponse}
     */
    @GetMapping("/contract-templates")
    public List<com.tcs.module.center.dto.response.ContractTemplateResponse> listContractTemplates() {
        return platformService.listContractTemplates();
    }

    /**
     * [UC-45]: Tạo mới mẫu hợp đồng điện tử với nội dung pháp lý và các biến giữ chỗ {{placeholder}}.
     *     * @param request Tên mẫu, nội dung điều khoản, loại hợp đồng
     * @return Mẫu hợp đồng vừa được khởi tạo
     */
    @PostMapping("/contract-templates")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public com.tcs.module.center.dto.response.ContractTemplateResponse createContractTemplate(
            @RequestBody com.tcs.module.center.dto.request.SaveContractTemplateRequest request) {
        return platformService.createContractTemplate(request);
    }

    /**
     * [UC-45]: Chỉnh sửa và cập nhật nội dung điều khoản mẫu hợp đồng.
     *     * @param templateId ID mẫu hợp đồng
     * @param request Nội dung cập nhật
     * @return Mẫu hợp đồng sau khi sửa đổi
     */
    @org.springframework.web.bind.annotation.PutMapping("/contract-templates/{templateId}")
    public com.tcs.module.center.dto.response.ContractTemplateResponse updateContractTemplate(
            @PathVariable Long templateId,
            @RequestBody com.tcs.module.center.dto.request.SaveContractTemplateRequest request) {
        return platformService.updateContractTemplate(templateId, request);
    }

    /**
     * [UC-45]: Xóa / Vô hiệu hóa mẫu hợp đồng điện tử khỏi danh mục.
     *     * @param templateId ID mẫu hợp đồng cần xóa
     */
    @DeleteMapping("/contract-templates/{templateId}")
    public void deleteContractTemplate(@PathVariable Long templateId) {
        platformService.deleteContractTemplate(templateId);
    }

    // =========================================================================
    // [UC-21]: GIÁM SÁT LỊCH HỌC & ĐIỂM DANH TOÀN SÀN THEO NGÀY (SCHEDULE MONITOR)
    // =========================================================================

    /**
     * [UC-21]: Giám sát toàn bộ các ca học, lịch dạy và trạng thái điểm danh toàn sàn theo ngày được chọn.
     *     * Tối ưu hiệu năng:
     *   - Lọc sẵn các lớp ACTIVE tại tầng Database, gom nhóm ca học và điểm danh học viên hiệu quả.
     *     * @param date Ngày cần giám sát lịch học (mặc định hôm nay nếu để trống)
     * @return Danh sách các ca học {@link com.tcs.module.center.dto.response.CenterScheduleClassResponse}
     */
    @GetMapping("/classes/schedule")
    public List<com.tcs.module.center.dto.response.CenterScheduleClassResponse> getPlatformSchedule(
            @RequestParam(required = false) java.time.LocalDate date) {
        return platformService.getPlatformSchedule(date);
    }

    // =========================================================================
    // [UC-46]: CẤU HÌNH BIỂU PHÍ RIÊNG BIỆT CHO TRUNG TÂM GIA SƯ (CUSTOM FEES)
    // =========================================================================

    /**
     * [UC-46]: Lấy danh sách toàn bộ các trung tâm gia sư và tỷ lệ phí chiết khấu đang áp dụng.
     *     * @return Danh sách cấu hình phí {@link com.tcs.module.platform.dto.response.CenterFeeConfigResponse}
     */
    @GetMapping("/fees/centers")
    public List<com.tcs.module.platform.dto.response.CenterFeeConfigResponse> listCenterFeeConfigs() {
        return platformService.listCenterFeeConfigs();
    }

    /**
     * [UC-46]: Thiết lập tỷ lệ phí chiết khấu riêng biệt cho một Trung tâm Gia sư cụ thể.
     *     * Nghiệp vụ:
     *   - Cho phép đặt mức phí từ 0% đến 50% (ví dụ: ưu đãi 1.5% hoặc 1%).
     *   - Khi giải ngân các lớp học thuộc trung tâm này, hệ thống sẽ ưu tiên áp dụng mức phí riêng này.
     *   - Tự động ghi vết vào Nhật ký kiểm toán [UC-61] với action {@code UPDATE_CENTER_FEE}.
     *     * @param centerId ID của trung tâm gia sư
     * @param request Tỷ lệ phí tùy chỉnh và lý do ưu đãi {@link com.tcs.module.platform.dto.request.UpdateCenterFeeRequest}
     * @return Thông tin cấu hình phí trung tâm đã cập nhật
     */
    @org.springframework.web.bind.annotation.PutMapping("/fees/centers/{centerId}")
    public com.tcs.module.platform.dto.response.CenterFeeConfigResponse updateCenterFeeConfig(
            @PathVariable Long centerId,
            @RequestBody @Valid com.tcs.module.platform.dto.request.UpdateCenterFeeRequest request) {
        return platformService.updateCenterFeeConfig(centerId, request);
    }

    /**
     * [UC-46]: Khôi phục tỷ lệ phí của trung tâm gia sư về mức mặc định toàn sàn (2%).
     *     * @param centerId ID của trung tâm gia sư
     * @return Cấu hình phí của trung tâm sau khi xóa ghi đè
     */
    @DeleteMapping("/fees/centers/{centerId}")
    public com.tcs.module.platform.dto.response.CenterFeeConfigResponse resetCenterFeeConfig(
            @PathVariable Long centerId) {
        return platformService.resetCenterFeeConfig(centerId);
    }
}

