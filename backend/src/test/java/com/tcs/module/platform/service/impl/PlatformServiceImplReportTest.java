package com.tcs.module.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.ResolveReportRequest;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformServiceImplReportTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private PlatformServiceImpl platformService;

    @Test
    void resolveReportSupportsUserAndNotifiesReporter() {
        User reporter = new User();
        reporter.setUserId(9L);
        reporter.setEmail("reporter@example.com");
        Report report = new Report();
        report.setReportId(3L);
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.USER);
        report.setTargetId(22L);
        report.setCategory(ReportCategory.ABUSE);
        report.setDescription("Nội dung báo cáo");
        report.setStatus(ReportStatus.PENDING);
        ResolveReportRequest request = new ResolveReportRequest();
        request.setStatus(ReportStatus.RESOLVED);
        request.setAdminNotes("Đã kiểm tra và xử lý tài khoản");

        when(reportRepository.findById(3L)).thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);
        when(disputeRepository.findByReport_ReportId(3L)).thenReturn(Optional.empty());

        var response = platformService.resolveReport(3L, request);

        assertEquals(ReportStatus.RESOLVED, response.getStatus());
        verify(auditLogService).record(
                eq("RESOLVE_REPORT"), eq("Report"), eq(3L),
                eq(Map.of("oldStatus", ReportStatus.PENDING)), any());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(reporter), eq(NotificationType.REPORT), eq("REPORT_RESOLVED"),
                any(), any(), eq("Đã kiểm tra và xử lý tài khoản"), eq("REPORT"), eq(3L));
    }

    @Test
    void resolveReportRejectsClassTarget() {
        Report report = new Report();
        report.setTargetType(ReportTargetType.CLASS);
        when(reportRepository.findById(4L)).thenReturn(Optional.of(report));

        assertThrows(BusinessException.class, () -> platformService.resolveReport(4L, new ResolveReportRequest()));
    }
}
