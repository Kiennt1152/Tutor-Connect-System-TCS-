package com.tcs.module.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.ModerateReviewRequest;
import com.tcs.module.platform.dto.request.ResolveClassIssueRequest;
import com.tcs.module.platform.dto.request.ResolveReportRequest;
import com.tcs.module.platform.dto.request.ResolveReviewReportRequest;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.ClassIssueResolutionAction;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.enums.ReviewReportAction;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module Platform — kiem duyet danh gia, xu ly bao cao, duyet ho so xac minh
 * va quet ticket qua han SLA.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: cac sheet moderateReview, resolveReport,
 * resolveReviewReport, resolveCenterClassIssue, plReviewVerification va scanSlaBreaches.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformServiceImplModerationTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long CENTER_USER_ID = 100L;
    private static final Long TUTOR_USER_ID = 200L;
    private static final Long REPORT_ID = 300L;
    private static final Long REVIEW_ID = 400L;
    private static final Long CLASS_ID = 500L;
    private static final Long VERIFICATION_ID = 600L;

    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private PlatformMapper platformMapper;
    @Mock private VerificationRequestRepository verificationRequestRepository;
    @Mock private VerificationDocumentRepository verificationDocumentRepository;
    @Mock private VerificationHistoryRepository verificationHistoryRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private EscrowService escrowService;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ContractService contractService;
    @Mock private AuthHelper authHelper;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private TicketMessageRepository ticketMessageRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private com.tcs.module.platform.service.PlatformTaskQueueService taskQueueService;
    @Mock private com.tcs.module.platform.service.PlatformAnalyticsService analyticsService;
    @Mock private CccdService cccdService;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks private PlatformServiceImpl service;

    private User adminUser;
    private User reporterUser;
    private User tutorUser;

    @BeforeEach
    void setUp() {
        adminUser = user(ADMIN_USER_ID, "admin@tcs.vn");
        reporterUser = user(700L, "nguoibaocao@tcs.vn");
        tutorUser = user(TUTOR_USER_ID, "giasu@tcs.vn");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(authHelper.currentUserId()).thenReturn(ADMIN_USER_ID);
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
    }

    private User user(Long id, String email) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        return u;
    }

    private Review review(Long id, ReviewStatus status) {
        Review r = new Review();
        r.setReviewId(id);
        r.setReviewee(tutorUser);
        r.setReviewer(reporterUser);
        r.setStatus(status);
        r.setRating(new java.math.BigDecimal("4.0"));
        r.setCreatedAt(LocalDateTime.now().minusDays(1));
        return r;
    }

    private Report report(ReportTargetType targetType, Long targetId, ReportStatus status) {
        Report r = new Report();
        r.setReportId(REPORT_ID);
        r.setReporter(reporterUser);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setStatus(status);
        r.setDescription("Noi dung bao cao ban dau");
        r.setCreatedAt(LocalDateTime.now().minusDays(1));
        return r;
    }

    // ===================================================================
    //  Sheet: moderateReview
    // ===================================================================
    @Nested
    @DisplayName("moderateReview")
    class ModerateReview {

        private ModerateReviewRequest request(ReviewStatus status) {
            ModerateReviewRequest r = new ModerateReviewRequest();
            r.setStatus(status);
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - reviewId hop le + trang thai kiem duyet -> luu va tinh lai diem uy tin")
        void utcid01_moderateSuccessfully() {
            Review target = review(REVIEW_ID, ReviewStatus.VISIBLE);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(target));

            service.moderateReview(REVIEW_ID, request(ReviewStatus.MODERATED));

            assertEquals(ReviewStatus.MODERATED, target.getStatus());
            verify(reviewRepository).save(target);
            verify(contractService).recomputeReputationByTutorUser(TUTOR_USER_ID);
        }

        @Test
        @DisplayName("UTCID02 (A) - reviewId khong khop danh gia nao -> 'Không tìm thấy đánh giá'")
        void utcid02_reviewNotFound() {
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.moderateReview(REVIEW_ID, request(ReviewStatus.HIDDEN)));
            assertEquals("Không tìm thấy đánh giá", ex.getMessage());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (B) - Trang thai an danh gia -> danh gia chuyen HIDDEN va tinh lai diem")
        void utcid03_hideReview() {
            Review target = review(REVIEW_ID, ReviewStatus.VISIBLE);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(target));

            service.moderateReview(REVIEW_ID, request(ReviewStatus.HIDDEN));

            assertEquals(ReviewStatus.HIDDEN, target.getStatus());
            verify(contractService).recomputeReputationByTutorUser(TUTOR_USER_ID);
        }

        @Test
        @DisplayName("UTCID04 (B) - Trang thai khoi phuc hien thi -> danh gia tro lai VISIBLE")
        void utcid04_restoreReview() {
            Review target = review(REVIEW_ID, ReviewStatus.HIDDEN);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(target));

            service.moderateReview(REVIEW_ID, request(ReviewStatus.VISIBLE));

            assertEquals(ReviewStatus.VISIBLE, target.getStatus());
            verify(contractService).recomputeReputationByTutorUser(TUTOR_USER_ID);
        }
    }

    // ===================================================================
    //  Sheet: resolveReport
    // ===================================================================
    @Nested
    @DisplayName("resolveReport")
    class ResolveReport {

        private ResolveReportRequest request(ReportStatus status, String notes) {
            ResolveReportRequest r = new ResolveReportRequest();
            r.setStatus(status);
            r.setAdminNotes(notes);
            return r;
        }

        private void givenReport(Report report) {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        }

        @Test
        @DisplayName("UTCID01 (N) - Bao cao khong phai CLASS, chua RESOLVED, co adminNotes -> chuyen RESOLVED")
        void utcid01_resolveSuccessfully() {
            Report target = report(ReportTargetType.USER, 888L, ReportStatus.PENDING);
            givenReport(target);

            service.resolveReport(REPORT_ID, request(ReportStatus.RESOLVED, "Da canh cao nguoi bi to cao"));

            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(reportRepository).save(target);
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq("RESOLVE_REPORT"),
                    org.mockito.ArgumentMatchers.eq("Report"),
                    org.mockito.ArgumentMatchers.eq(REPORT_ID),
                    any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - reportId khong khop bao cao nao -> 'Không tìm thấy báo cáo'")
        void utcid02_reportNotFound() {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.resolveReport(REPORT_ID, request(ReportStatus.RESOLVED, "note")));
            assertEquals("Không tìm thấy báo cáo", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Bao cao targetType = CLASS -> phai dung luong su co lop hoc")
        void utcid03_classReportRejected() {
            givenReport(report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveReport(REPORT_ID, request(ReportStatus.RESOLVED, "note")));
            assertEquals("Báo cáo lớp phải được xử lý bằng luồng sự cố lớp học", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - request = null -> 'Trạng thái xử lý báo cáo phải là RESOLVED'")
        void utcid04_nullRequest() {
            givenReport(report(ReportTargetType.USER, 888L, ReportStatus.PENDING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveReport(REPORT_ID, null));
            assertEquals("Trạng thái xử lý báo cáo phải là RESOLVED", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - request.status khac RESOLVED -> chan")
        void utcid05_statusNotResolved() {
            givenReport(report(ReportTargetType.USER, 888L, ReportStatus.PENDING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveReport(REPORT_ID, request(ReportStatus.PENDING, "note")));
            assertEquals("Trạng thái xử lý báo cáo phải là RESOLVED", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID06 (A) - Bao cao da RESOLVED -> 'Báo cáo đã được xử lý'")
        void utcid06_alreadyResolved() {
            givenReport(report(ReportTargetType.USER, 888L, ReportStatus.RESOLVED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveReport(REPORT_ID, request(ReportStatus.RESOLVED, "note")));
            assertEquals("Báo cáo đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (B) - adminNotes rong -> gui thong bao voi noi dung mac dinh")
        void utcid07_blankAdminNotes() {
            Report target = report(ReportTargetType.USER, 888L, ReportStatus.PENDING);
            givenReport(target);

            service.resolveReport(REPORT_ID, request(ReportStatus.RESOLVED, "   "));

            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(notificationDispatchService).notifyUserFromTemplate(
                    org.mockito.ArgumentMatchers.eq(reporterUser),
                    any(), any(), any(),
                    org.mockito.ArgumentMatchers.eq("Báo cáo của bạn đã được xử lý"),
                    org.mockito.ArgumentMatchers.eq(
                            "Quản trị viên đã hoàn tất xử lý báo cáo #" + REPORT_ID + "."),
                    any(), any());
        }

        @Test
        @DisplayName("UTCID08 (N) - adminNotes co noi dung -> gui thong bao dung noi dung admin nhap")
        void utcid08_withAdminNotes() {
            Report target = report(ReportTargetType.USER, 888L, ReportStatus.PENDING);
            givenReport(target);

            service.resolveReport(REPORT_ID, request(ReportStatus.RESOLVED, "  Da xu ly xong  "));

            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(notificationDispatchService).notifyUserFromTemplate(
                    org.mockito.ArgumentMatchers.eq(reporterUser),
                    any(), any(), any(),
                    org.mockito.ArgumentMatchers.eq("Báo cáo của bạn đã được xử lý"),
                    org.mockito.ArgumentMatchers.eq("Da xu ly xong"),
                    any(), any());
        }
    }

    // ===================================================================
    //  Sheet: resolveReviewReport
    // ===================================================================
    @Nested
    @DisplayName("resolveReviewReport")
    class ResolveReviewReport {

        private ResolveReviewReportRequest request(ReviewReportAction action) {
            ResolveReviewReportRequest r = new ResolveReviewReportRequest();
            r.setAction(action);
            r.setNotes("Danh gia vi pham quy dinh cong dong");
            return r;
        }

        private void givenReport(Report report) {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        }

        @Test
        @DisplayName("UTCID01 (N) - Admin xu ly bao cao REVIEW dang PENDING, danh gia con ton tai -> an danh gia va dong bao cao")
        void utcid01_hideReviewAndResolve() {
            Report target = report(ReportTargetType.REVIEW, REVIEW_ID, ReportStatus.PENDING);
            givenReport(target);
            Review reported = review(REVIEW_ID, ReviewStatus.VISIBLE);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reported));

            service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.HIDE_REVIEW));

            assertEquals(ReviewStatus.HIDDEN, reported.getStatus());
            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(contractService).recomputeReputationByTutorUser(TUTOR_USER_ID);
        }

        @Test
        @DisplayName("UTCID02 (N) - Danh gia da bi xoa nhung hanh dong la KEEP_REVIEW -> chi dong bao cao")
        void utcid02_keepReviewWhenReviewGone() {
            Report target = report(ReportTargetType.REVIEW, REVIEW_ID, ReportStatus.PENDING);
            givenReport(target);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

            service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.KEEP_REVIEW));

            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(reviewRepository, never()).save(any());
            verify(reviewRepository, never()).delete(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai PLATFORM_ADMIN -> ForbiddenException")
        void utcid03_notAdmin() {
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class,
                    () -> service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.HIDE_REVIEW)));
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - reportId = null -> 'reportId là bắt buộc'")
        void utcid04_nullReportId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveReviewReport(null, request(ReviewReportAction.HIDE_REVIEW)));
            assertEquals("reportId là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - request = null -> 'Hành động xử lý là bắt buộc'")
        void utcid05_nullRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveReviewReport(REPORT_ID, null));
            assertEquals("Hành động xử lý là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - request.action = null -> 'Hành động xử lý là bắt buộc'")
        void utcid06_nullAction() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveReviewReport(REPORT_ID, new ResolveReviewReportRequest()));
            assertEquals("Hành động xử lý là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - reportId khong khop bao cao nao -> 'Không tìm thấy báo cáo'")
        void utcid07_reportNotFound() {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.HIDE_REVIEW)));
            assertEquals("Không tìm thấy báo cáo", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Bao cao khong nham vao danh gia -> 'Luồng này chỉ xử lý báo cáo nhắm vào đánh giá'")
        void utcid08_reportIsNotAboutAReview() {
            givenReport(report(ReportTargetType.USER, 888L, ReportStatus.PENDING));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.HIDE_REVIEW)));
            assertEquals("Luồng này chỉ xử lý báo cáo nhắm vào đánh giá", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Bao cao da RESOLVED -> 'Báo cáo đã được xử lý'")
        void utcid09_alreadyResolved() {
            givenReport(report(ReportTargetType.REVIEW, REVIEW_ID, ReportStatus.RESOLVED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.HIDE_REVIEW)));
            assertEquals("Báo cáo đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - Danh gia khong con ton tai va hanh dong khac KEEP_REVIEW -> chan")
        void utcid10_reviewGoneWithNonKeepAction() {
            givenReport(report(ReportTargetType.REVIEW, REVIEW_ID, ReportStatus.PENDING));
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveReviewReport(REPORT_ID, request(ReviewReportAction.DELETE_REVIEW)));
            assertEquals("Đánh giá bị báo cáo không còn tồn tại", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: resolveCenterClassIssue
    // ===================================================================
    @Nested
    @DisplayName("resolveCenterClassIssue")
    class ResolveCenterClassIssue {

        private ResolveClassIssueRequest request(ClassIssueResolutionAction action) {
            ResolveClassIssueRequest r = new ResolveClassIssueRequest();
            r.setAction(action);
            r.setNotes("Trung tam da lam viec voi gia su va hoc vien");
            return r;
        }

        private TutoringClass centerClassOwnedBy(Long centerUserId) {
            User centerUser = user(centerUserId, "trungtam@tcs.vn");
            TutorCenter center = new TutorCenter();
            center.setCenterId(10L);
            center.setUser(centerUser);

            TutoringClass cls = new TutoringClass();
            cls.setClassId(CLASS_ID);
            cls.setTitle("Toan 9");
            cls.setClassType(ClassType.CENTER);
            cls.setCenter(center);
            cls.setCreator(centerUser);
            return cls;
        }

        @BeforeEach
        void loginAsCenter() {
            User centerUser = user(CENTER_USER_ID, "trungtam@tcs.vn");
            when(authHelper.requireRole(UserRole.TUTOR_CENTER))
                    .thenReturn(new UserPrincipal(centerUser, UserRole.TUTOR_CENTER));
            when(tutoringClassRepository.findById(CLASS_ID))
                    .thenReturn(Optional.of(centerClassOwnedBy(CENTER_USER_ID)));
        }

        private void givenReport(Report report) {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        }

        @Test
        @DisplayName("UTCID01 (N) - Trung tam so huu lop, bao cao con PENDING -> xu ly va dong bao cao")
        void utcid01_resolveSuccessfully() {
            Report target = report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING);
            givenReport(target);

            service.resolveCenterClassIssue(REPORT_ID,
                    request(ClassIssueResolutionAction.CONTINUE_CLASS));

            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(reportRepository).save(target);
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai TUTOR_CENTER -> ForbiddenException")
        void utcid02_notACenter() {
            when(authHelper.requireRole(UserRole.TUTOR_CENTER))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class, () -> service.resolveCenterClassIssue(
                    REPORT_ID, request(ClassIssueResolutionAction.CONTINUE_CLASS)));
        }

        @Test
        @DisplayName("UTCID03 (A) - reportId = null -> 'reportId là bắt buộc'")
        void utcid03_nullReportId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveCenterClassIssue(null,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("reportId là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - reportId khong khop bao cao nao -> 'Không tìm thấy báo cáo'")
        void utcid04_reportNotFound() {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.resolveCenterClassIssue(REPORT_ID,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("Không tìm thấy báo cáo", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Bao cao thuoc lop cua trung tam khac -> ForbiddenException")
        void utcid05_classOfAnotherCenter() {
            givenReport(report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING));
            when(tutoringClassRepository.findById(CLASS_ID))
                    .thenReturn(Optional.of(centerClassOwnedBy(999L)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.resolveCenterClassIssue(REPORT_ID,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("Bạn chỉ có quyền xử lý báo cáo của lớp trung tâm do mình quản lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - request = null -> 'Hành động xử lý là bắt buộc'")
        void utcid06_nullRequest() {
            givenReport(report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveCenterClassIssue(REPORT_ID, null));
            assertEquals("Hành động xử lý là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - request.action = null -> 'Hành động xử lý là bắt buộc'")
        void utcid07_nullAction() {
            givenReport(report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveCenterClassIssue(REPORT_ID, new ResolveClassIssueRequest()));
            assertEquals("Hành động xử lý là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Bao cao da RESOLVED -> 'Báo cáo đã được xử lý'")
        void utcid08_alreadyResolved() {
            givenReport(report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.RESOLVED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveCenterClassIssue(REPORT_ID,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("Báo cáo đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Bao cao khong phai loai CLASS -> khong xu ly qua luong su co lop")
        void utcid09_reportIsNotAClassReport() {
            givenReport(report(ReportTargetType.USER, 888L, ReportStatus.PENDING));

            // Bao cao khong phai CLASS thi cung khong thuoc lop trung tam nao -> chan ngay o quyen.
            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.resolveCenterClassIssue(REPORT_ID,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("Bạn chỉ có quyền xử lý báo cáo của lớp trung tâm do mình quản lý", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: plReviewVerification (PlatformService.reviewVerification)
    // ===================================================================
    @Nested
    @DisplayName("plReviewVerification")
    class PlReviewVerification {

        private VerificationRequest verification;

        @BeforeEach
        void initVerification() {
            verification = new VerificationRequest();
            verification.setVerificationId(VERIFICATION_ID);
            verification.setUser(tutorUser);
            verification.setStatus(VerificationStatus.UNDER_REVIEW);
            verification.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 10, 0, 0));

            when(verificationRequestRepository.findById(VERIFICATION_ID))
                    .thenReturn(Optional.of(verification));
            when(verificationRequestRepository.save(any(VerificationRequest.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
            when(tutorCenterRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
            when(verificationDocumentRepository
                    .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                    .thenReturn(List.of());
        }

        private ReviewVerificationRequest request(
                VerificationStatus status, String notes, LocalDateTime expectedUpdatedAt) {
            ReviewVerificationRequest r = new ReviewVerificationRequest();
            r.setStatus(status);
            r.setAdminNotes(notes);
            r.setExpectedUpdatedAt(expectedUpdatedAt);
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - Admin duyet ho so UNDER_REVIEW -> chuyen VERIFIED va dong bo ho so gia su")
        void utcid01_approve() {
            Tutor tutor = new Tutor();
            tutor.setTutorId(20L);
            tutor.setUser(tutorUser);
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));

            service.reviewVerification(VERIFICATION_ID,
                    request(VerificationStatus.VERIFIED, "Ho so day du", null));

            assertEquals(VerificationStatus.VERIFIED, verification.getStatus());
            assertEquals(ProfileVerificationStatus.VERIFIED, tutor.getVerificationStatus());
            verify(tutorRepository).save(tutor);
        }

        @Test
        @DisplayName("UTCID02 (N) - Admin tu choi ho so kem ly do -> chuyen REJECTED va luu ly do")
        void utcid02_reject() {
            service.reviewVerification(VERIFICATION_ID,
                    request(VerificationStatus.REJECTED, "Anh CCCD bi mo, khong doc duoc", null));

            assertEquals(VerificationStatus.REJECTED, verification.getStatus());
            assertEquals("Anh CCCD bi mo, khong doc duoc", verification.getRejectionReason());
            assertEquals(ADMIN_USER_ID, verification.getReviewedBy());
        }

        @Test
        @DisplayName("UTCID03 (A) - request.status = null -> 'Trạng thái xác minh không được để trống'")
        void utcid03_nullStatus() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reviewVerification(VERIFICATION_ID, request(null, "note", null)));
            assertEquals("Trạng thái xác minh không được để trống", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Nguoi goi khong phai PLATFORM_ADMIN -> ForbiddenException")
        void utcid04_notAdmin() {
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class, () -> service.reviewVerification(
                    VERIFICATION_ID, request(VerificationStatus.VERIFIED, "note", null)));
        }

        @Test
        @DisplayName("UTCID05 (A) - verificationId khong khop ho so nao -> 'Không tìm thấy yêu cầu xác minh'")
        void utcid05_verificationNotFound() {
            when(verificationRequestRepository.findById(VERIFICATION_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.reviewVerification(VERIFICATION_ID,
                            request(VerificationStatus.VERIFIED, "note", null)));
            assertEquals("Không tìm thấy yêu cầu xác minh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Quyet dinh khong phai VERIFIED / REJECTED -> chan")
        void utcid06_invalidDecision() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reviewVerification(VERIFICATION_ID,
                            request(VerificationStatus.SUBMITTED, "note", null)));
            assertEquals("Quyết định không hợp lệ. Chỉ chấp nhận Duyệt hoặc Từ chối.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - expectedUpdatedAt lech tu 1 giay tro len -> chan ghi de")
        void utcid07_staleExpectedUpdatedAt() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reviewVerification(VERIFICATION_ID, request(
                            VerificationStatus.VERIFIED, "note",
                            LocalDateTime.of(2026, 8, 1, 10, 0, 5))));
            assertEquals("Hồ sơ vừa được cập nhật bởi người khác, vui lòng tải lại trước khi sửa.",
                    ex.getMessage());
            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID08 (A) - Trang thai hien tai khong phai UNDER_REVIEW / VERIFIED / REJECTED -> chan")
        void utcid08_statusNotReadyForReview() {
            verification.setStatus(VerificationStatus.SUBMITTED);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reviewVerification(VERIFICATION_ID,
                            request(VerificationStatus.VERIFIED, "note", null)));
            assertEquals("Hồ sơ chưa sẵn sàng để duyệt. Vui lòng mở hồ sơ để xem xét trước.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (B) - expectedUpdatedAt = null -> bo qua kiem tra dong thoi")
        void utcid09_expectedUpdatedAtIsNull() {
            service.reviewVerification(VERIFICATION_ID,
                    request(VerificationStatus.VERIFIED, "Ho so day du", null));

            assertEquals(VerificationStatus.VERIFIED, verification.getStatus());
        }

        @Test
        @DisplayName("UTCID10 (B) - expectedUpdatedAt chi lech phan mili giay -> van khop (so sanh theo giay)")
        void utcid10_subSecondDriftStillMatches() {
            verification.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 10, 0, 0, 500_000_000));

            service.reviewVerification(VERIFICATION_ID, request(
                    VerificationStatus.VERIFIED, "Ho so day du",
                    LocalDateTime.of(2026, 8, 1, 10, 0, 0, 900_000_000)));

            assertEquals(VerificationStatus.VERIFIED, verification.getStatus(),
                    "Lech duoi 1 giay khong duoc coi la xung dot");
        }

        @Test
        @DisplayName("UTCID11 (B) - Tu choi nhung ly do ngan hon 10 ky tu -> 'Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).'")
        void utcid11_rejectReasonTooShort() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reviewVerification(VERIFICATION_ID,
                            request(VerificationStatus.REJECTED, "Anh mo", null)));
            assertEquals("Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).", ex.getMessage());
            verify(verificationRequestRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: scanSlaBreaches (scanAndEscalateSlaBreaches)
    // ===================================================================
    @Nested
    @DisplayName("scanSlaBreaches")
    class ScanSlaBreaches {

        private SupportTicket breachedTicket(Long id, SupportTicketPriority priority) {
            SupportTicket t = new SupportTicket();
            t.setTicketId(id);
            t.setSubject("Khong dang nhap duoc");
            t.setUser(reporterUser);
            t.setPriority(priority);
            t.setDueAt(LocalDateTime.now().minusHours(2));
            t.setSlaBreached(false);
            return t;
        }

        private void givenBreachedTickets(SupportTicket... tickets) {
            when(supportTicketRepository.findBreachedCandidateTickets(any(), any()))
                    .thenReturn(List.of(tickets));
            when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));
            when(platformAdminRepository.findAll()).thenReturn(List.of());
        }

        @Test
        @DisplayName("UTCID01 (N) - Co ticket qua han chua danh dau -> danh dau slaBreached va tra so luong nang cap")
        void utcid01_escalateBreachedTickets() {
            SupportTicket ticket = breachedTicket(10L, SupportTicketPriority.MEDIUM);
            givenBreachedTickets(ticket);

            int escalated = service.scanAndEscalateSlaBreaches();

            assertEquals(1, escalated);
            assertTrue(ticket.getSlaBreached());
            assertEquals(SupportTicketPriority.HIGH, ticket.getPriority());
            verify(supportTicketRepository).save(ticket);
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq("SLA_BREACH_ESCALATION"),
                    org.mockito.ArgumentMatchers.eq("SupportTicket"),
                    org.mockito.ArgumentMatchers.eq(10L),
                    any(), any());
        }

        @Test
        @DisplayName("UTCID02 (B) - Khong ticket nao qua han -> tra 0 va khong ghi gi")
        void utcid02_noBreachedTicket() {
            when(supportTicketRepository.findBreachedCandidateTickets(any(), any())).thenReturn(List.of());

            assertEquals(0, service.scanAndEscalateSlaBreaches());
            verify(supportTicketRepository, never()).save(any());
            verify(auditLogService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("UTCID03 (B) - Ticket LOW qua han -> nang len MEDIUM")
        void utcid03_lowBecomesMedium() {
            SupportTicket ticket = breachedTicket(11L, SupportTicketPriority.LOW);
            givenBreachedTickets(ticket);

            service.scanAndEscalateSlaBreaches();

            assertEquals(SupportTicketPriority.MEDIUM, ticket.getPriority());
        }

        @Test
        @DisplayName("UTCID04 (B) - Ticket MEDIUM qua han -> nang len HIGH")
        void utcid04_mediumBecomesHigh() {
            SupportTicket ticket = breachedTicket(12L, SupportTicketPriority.MEDIUM);
            givenBreachedTickets(ticket);

            service.scanAndEscalateSlaBreaches();

            assertEquals(SupportTicketPriority.HIGH, ticket.getPriority());
        }

        @Test
        @DisplayName("UTCID05 (B) - Ticket HIGH qua han -> nang len URGENT")
        void utcid05_highBecomesUrgent() {
            SupportTicket ticket = breachedTicket(13L, SupportTicketPriority.HIGH);
            givenBreachedTickets(ticket);

            service.scanAndEscalateSlaBreaches();

            assertEquals(SupportTicketPriority.URGENT, ticket.getPriority());
        }

        @Test
        @DisplayName("UTCID06 (B) - Ticket URGENT qua han -> giu nguyen URGENT (khong vuot tran)")
        void utcid06_urgentStaysUrgent() {
            SupportTicket ticket = breachedTicket(14L, SupportTicketPriority.URGENT);
            givenBreachedTickets(ticket);

            service.scanAndEscalateSlaBreaches();

            assertEquals(SupportTicketPriority.URGENT, ticket.getPriority());
        }

        /**
         * UTCID07 (B) — ticket quá hạn nhưng chưa có mức ưu tiên.
         *
         * <p><b>DEF-11.</b> {@code escalatePriority()} đã xử lý đúng trường hợp null (mặc định HIGH),
         * nhưng ngay sau đó dòng ghi audit dùng
         * {@code Map.of("oldPriority", oldPriority, "slaBreached", false)} — {@code Map.of} KHÔNG
         * chấp nhận giá trị null nên cả vòng quét bị ném {@code NullPointerException}. Hậu quả:
         * chỉ cần một ticket quá hạn có priority = null là job SLA dừng hẳn, không ticket nào
         * được nâng cấp.</p>
         *
         * <p>Cách sửa: thay {@code Map.of} bằng map cho phép null (ví dụ {@code java.util.HashMap})
         * ở {@code PlatformServiceImpl#scanAndEscalateSlaBreaches}.</p>
         */
        @Test
        @DisplayName("UTCID07 (B) - Ticket khong co muc uu tien -> phai nang len HIGH [DEF-11]")
        void utcid07_nullPriorityBecomesHigh() {
            SupportTicket ticket = breachedTicket(15L, null);
            givenBreachedTickets(ticket);

            service.scanAndEscalateSlaBreaches();

            assertEquals(SupportTicketPriority.HIGH, ticket.getPriority());
        }

        @Test
        @DisplayName("UTCID08 (A) - Ticket da RESOLVED / CLOSED bi loai khoi truy vet")
        void utcid08_resolvedAndClosedAreExcluded() {
            when(supportTicketRepository.findBreachedCandidateTickets(any(), any())).thenReturn(List.of());

            service.scanAndEscalateSlaBreaches();

            org.mockito.ArgumentCaptor<List<com.tcs.module.platform.enums.SupportTicketStatus>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            verify(supportTicketRepository).findBreachedCandidateTickets(captor.capture(), any());
            assertTrue(captor.getValue().contains(
                    com.tcs.module.platform.enums.SupportTicketStatus.RESOLVED));
            assertTrue(captor.getValue().contains(
                    com.tcs.module.platform.enums.SupportTicketStatus.CLOSED));
        }
    }
    // ===================================================================
    //  Sheet: resolveClassIssue
    // ===================================================================
    @Nested
    @DisplayName("resolveClassIssue")
    class ResolveClassIssue {

        private ResolveClassIssueRequest request(ClassIssueResolutionAction action) {
            ResolveClassIssueRequest r = new ResolveClassIssueRequest();
            r.setAction(action);
            r.setNotes("Admin da lam viec voi hai ben va thong nhat huong xu ly");
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - bao cao su co lop con PENDING -> ghi huong xu ly va dong bao cao")
        void utcid01_resolveSuccessfully() {
            Report target = report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(target));

            service.resolveClassIssue(REPORT_ID, request(ClassIssueResolutionAction.CONTINUE_CLASS));

            assertEquals(ReportStatus.RESOLVED, target.getStatus());
            verify(reportRepository).save(target);
        }

        @Test
        @DisplayName("UTCID02 (A) - reportId = null -> 'reportId là bắt buộc'")
        void utcid02_nullReportId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveClassIssue(null,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("reportId là bắt buộc", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - thieu hanh dong xu ly -> 'Hành động xử lý là bắt buộc'")
        void utcid03_missingAction() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.resolveClassIssue(REPORT_ID, new ResolveClassIssueRequest()));
            assertEquals("Hành động xử lý là bắt buộc", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - bao cao khong phai loai su co lop hoc -> chan")
        void utcid04_notAClassReport() {
            Report target = report(ReportTargetType.REVIEW, REVIEW_ID, ReportStatus.PENDING);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(target));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveClassIssue(REPORT_ID,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("Chỉ hỗ trợ xử lý báo cáo sự cố lớp học trong luồng này", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - bao cao da duoc xu ly -> 'Báo cáo đã được xử lý'")
        void utcid05_alreadyResolved() {
            Report target = report(ReportTargetType.CLASS, CLASS_ID, ReportStatus.RESOLVED);
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(target));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resolveClassIssue(REPORT_ID,
                            request(ClassIssueResolutionAction.CONTINUE_CLASS)));
            assertEquals("Báo cáo đã được xử lý", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: updateUserStatus
    // ===================================================================
    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatus {

        private static final Long TARGET_USER_ID = 820L;

        private User target;

        private com.tcs.module.platform.dto.request.UpdateUserStatusRequest request(
                com.tcs.module.identity.enums.UserStatus status) {
            var r = new com.tcs.module.platform.dto.request.UpdateUserStatusRequest();
            r.setStatus(status);
            return r;
        }

        @BeforeEach
        void givenOrdinaryTarget() {
            target = user(TARGET_USER_ID, "hocvien@tcs.vn");
            target.setStatus(com.tcs.module.identity.enums.UserStatus.ACTIVE);
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(target));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        }

        @Test
        @DisplayName("UTCID01 (N) - tai khoan thuong + trang thai moi hop le -> cap nhat va ghi audit")
        void utcid01_updateSuccessfully() {
            service.updateUserStatus(TARGET_USER_ID,
                    request(com.tcs.module.identity.enums.UserStatus.SUSPENDED));

            assertEquals(com.tcs.module.identity.enums.UserStatus.SUSPENDED, target.getStatus());
            verify(userRepository).save(target);
            verify(auditLogService).record(
                    eq("UPDATE_USER_STATUS"), eq("User"), eq(TARGET_USER_ID), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - khong truyen trang thai -> 'Trạng thái không được để trống'")
        void utcid02_nullStatus() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateUserStatus(TARGET_USER_ID, request(null)));
            assertEquals("Trạng thái không được để trống", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - doi tuong la tai khoan quan tri vien -> chan")
        void utcid03_targetIsAdmin() {
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.PLATFORM_ADMIN);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateUserStatus(TARGET_USER_ID,
                            request(com.tcs.module.identity.enums.UserStatus.BANNED)));
            assertEquals("Không thể thay đổi trạng thái tài khoản quản trị viên", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - enum UserStatus chi co 3 gia tri nen nhanh 'Trạng thái không hợp lệ' khong the cham toi")
        void utcid04_everyEnumValueIsAccepted() {
            // Guard trong code chan cac gia tri ngoai ACTIVE / SUSPENDED / BANNED. Kieu tham so
            // la enum UserStatus nen moi gia tri non-null deu hop le; test nay khoa lai dieu do:
            // neu ai do them hang so moi vao enum thi test do ngay va phai bo sung ca guard.
            assertEquals(3, com.tcs.module.identity.enums.UserStatus.values().length);
            for (com.tcs.module.identity.enums.UserStatus status
                    : com.tcs.module.identity.enums.UserStatus.values()) {
                target.setStatus(com.tcs.module.identity.enums.UserStatus.ACTIVE);
                service.updateUserStatus(TARGET_USER_ID, request(status));
                assertEquals(status, target.getStatus());
            }
        }
    }
}
