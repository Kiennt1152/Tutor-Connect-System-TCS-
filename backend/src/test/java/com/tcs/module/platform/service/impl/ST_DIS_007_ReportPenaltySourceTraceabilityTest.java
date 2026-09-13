package com.tcs.module.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.UserPenalty;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.repository.CircumventionEventRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * System Test ST-DIS-007: Report a user or class, creating a penalty source.
 * Procedure:
 * 1. A user reports a user/class with a reason.
 * 2. Admin processes it, creates a penalty.
 * 3. Check source traceability.
 * Expected:
 * Report created; penalty stores sourceType/sourceId/sourceTaskId;
 * in the penalties list a source badge & correct back-link appear.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ST_DIS_007_ReportPenaltySourceTraceabilityTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long TARGET_TUTOR_USER_ID = 5L;
    private static final Long REPORTER_CLIENT_USER_ID = 10L;
    private static final Long REPORT_ID = 105L;
    private static final Long PENALTY_ID = 701L;

    @Mock private UserPenaltyRepository userPenaltyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private CircumventionEventRepository circumventionEventRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private AuthHelper authHelper;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks private PenaltyServiceImpl penaltyService;

    private User adminUser;
    private PlatformAdmin adminProfile;
    private User targetTutor;
    private User reporterClient;
    private Report report;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(ADMIN_USER_ID);
        adminUser.setEmail("admin01@tcs.test");

        adminProfile = new PlatformAdmin();
        adminProfile.setAdminId(1L);
        adminProfile.setUser(adminUser);

        targetTutor = new User();
        targetTutor.setUserId(TARGET_TUTOR_USER_ID);
        targetTutor.setEmail("tutor05@tcs.test");
        targetTutor.setStatus(UserStatus.ACTIVE);

        reporterClient = new User();
        reporterClient.setUserId(REPORTER_CLIENT_USER_ID);
        reporterClient.setEmail("client01@tcs.test");

        // Step 1: User reported tutor05 for 'FRAUD'
        report = new Report();
        report.setReportId(REPORT_ID);
        report.setReporter(reporterClient);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(TARGET_TUTOR_USER_ID);
        report.setCategory(ReportCategory.FRAUD);
        report.setDescription("Gia sư có hành vi gian lận buổi học và thu học phí ngoài luồng trái phép");
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(LocalDateTime.now());

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminProfile));
        when(userRepository.findById(TARGET_TUTOR_USER_ID)).thenReturn(Optional.of(targetTutor));
        when(reportRepository.existsById(REPORT_ID)).thenReturn(true);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
    }

    @Test
    @DisplayName("ST-DIS-007: Bước 1 & 2 - Admin xử lý báo cáo, ban hành án phạt lưu đúng sourceType, sourceId, sourceTaskId")
    void testAdminCreatesPenaltyWithReportSourceTraceability() {
        IssuePenaltyRequest request = new IssuePenaltyRequest();
        request.setUserId(TARGET_TUTOR_USER_ID);
        request.setPenaltyType("WARNING");
        request.setReason("Xử phạt cảnh cáo do có hành vi gian lận từ báo cáo vi phạm #" + REPORT_ID);
        request.setSourceType("REPORT");
        request.setSourceId(REPORT_ID);
        request.setSourceTaskId("REPORT-" + REPORT_ID);

        when(userPenaltyRepository.save(any(UserPenalty.class))).thenAnswer(invocation -> {
            UserPenalty p = invocation.getArgument(0);
            p.setPenaltyId(PENALTY_ID);
            return p;
        });

        PenaltyResponse response = penaltyService.issuePenalty(request);

        // Verify response
        assertNotNull(response);
        assertEquals(PENALTY_ID, response.getPenaltyId());
        assertEquals("WARNING", response.getPenaltyType());
        assertEquals("REPORT", response.getSourceType());
        assertEquals(REPORT_ID, response.getSourceId());
        assertEquals("REPORT-" + REPORT_ID, response.getSourceTaskId());

        // Verify Entity saved with correct source traceability fields
        ArgumentCaptor<UserPenalty> penaltyCaptor = ArgumentCaptor.forClass(UserPenalty.class);
        verify(userPenaltyRepository).save(penaltyCaptor.capture());
        UserPenalty savedPenalty = penaltyCaptor.getValue();

        assertEquals("REPORT", savedPenalty.getSourceType(), "sourceType phải là REPORT");
        assertEquals(REPORT_ID, savedPenalty.getSourceId(), "sourceId phải khớp với REPORT_ID");
        assertEquals("REPORT-" + REPORT_ID, savedPenalty.getSourceTaskId(), "sourceTaskId phải lưu dạng REPORT-<id>");
        assertEquals(UserPenaltyStatus.ACTIVE, savedPenalty.getStatus());

        // Verify Audit Log and Notification dispatched
        verify(auditLogService).record(eq("ISSUE_PENALTY"), eq("UserPenalty"), eq(PENALTY_ID), any(), any());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(targetTutor), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("ST-DIS-007: Bước 3 - Truy vấn danh sách án phạt lọc theo sourceType = REPORT để hiển thị badge và link nguồn")
    void testListPenaltiesFiltersBySourceTypeReport() {
        UserPenalty savedPenalty = new UserPenalty();
        savedPenalty.setPenaltyId(PENALTY_ID);
        savedPenalty.setUser(targetTutor);
        savedPenalty.setIssuedBy(adminProfile);
        savedPenalty.setPenaltyType(UserPenaltyType.WARNING);
        savedPenalty.setReason("Xử phạt cảnh cáo vi phạm gian lận");
        savedPenalty.setStatus(UserPenaltyStatus.ACTIVE);
        savedPenalty.setSourceType("REPORT");
        savedPenalty.setSourceId(REPORT_ID);
        savedPenalty.setSourceTaskId("REPORT-" + REPORT_ID);
        savedPenalty.setCreatedAt(LocalDateTime.now());

        when(userPenaltyRepository.search(eq(null), eq(null), eq(null), eq("REPORT"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(savedPenalty)));

        PagePenaltyResponse pageResult = penaltyService.listPenalties(null, null, null, "REPORT", 0, 10);

        assertNotNull(pageResult);
        assertEquals(1, pageResult.getContent().size());
        PenaltyResponse item = pageResult.getContent().get(0);

        assertEquals("REPORT", item.getSourceType());
        assertEquals(REPORT_ID, item.getSourceId());
        assertEquals("REPORT-" + REPORT_ID, item.getSourceTaskId());
    }

    @Test
    @DisplayName("ST-DIS-007: Xác thực nguồn - Thất bại nếu sourceId báo cáo không tồn tại trong hệ thống")
    void testPenaltyCreationFailsWhenSourceReportNotFound() {
        Long nonExistentReportId = 9999L;
        when(reportRepository.existsById(nonExistentReportId)).thenReturn(false);

        IssuePenaltyRequest request = new IssuePenaltyRequest();
        request.setUserId(TARGET_TUTOR_USER_ID);
        request.setPenaltyType("WARNING");
        request.setReason("Xử phạt vi phạm báo cáo không tồn tại");
        request.setSourceType("REPORT");
        request.setSourceId(nonExistentReportId);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> penaltyService.issuePenalty(request));

        assertEquals("Không tìm thấy báo cáo #" + nonExistentReportId, ex.getMessage());
    }
}
