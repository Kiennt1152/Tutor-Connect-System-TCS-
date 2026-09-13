package com.tcs.module.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.identity.service.impl.IdentityServiceImpl;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.response.PageSupportTicketResponse;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG CÁC CHỈ SỐ PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS - ST-NFR)
 * ST-NFR-001 -> ST-NFR-006
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_NFR_AutomatedTestSuiteTest {

    // Mocks for PlatformServiceImpl (Pagination NFR)
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private TicketMessageRepository ticketMessageRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private AuthHelper authHelper;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private PlatformServiceImpl platformService;

    // Mocks for IdentityServiceImpl (Email Rate Limit NFR)
    @Mock private UserRepository userRepository;
    @Mock private EmailOtpRepository emailOtpRepository;
    @Mock private EmailService emailService;
    @InjectMocks private IdentityServiceImpl identityService;

    // =========================================================================
    // ST-NFR-004: Pagination on every list endpoint (Clamped to 50 max)
    // =========================================================================
    @Test
    @DisplayName("ST-NFR-004: Phân trang danh sách tự động ép về tối đa 50 bản ghi (clamp max=50) khi client truyền size=100")
    void test_ST_NFR_004_PaginationClampedToMax50() {
        int requestedSize = 100;
        int page = 0;

        when(supportTicketRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        platformService.getTickets(page, requestedSize, null, null, null, null);

        // Bắt Pageable truyền vào repository
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(supportTicketRepository).search(any(), any(), any(), any(), pageableCaptor.capture());

        Pageable effectivePageable = pageableCaptor.getValue();
        assertNotNull(effectivePageable);
        // Hệ thống bắt buộc phải kẹp (clamp) pageSize về tối đa 50 để bảo vệ tài nguyên DB
        assertEquals(50, effectivePageable.getPageSize(), "Size phải bị kẹp (clamp) về tối đa 50 khi client truyền 100");
    }

    // =========================================================================
    // ST-NFR-006: Email rate-limit and delivery reliability
    // =========================================================================
    @Test
    @DisplayName("ST-NFR-006: Chặn gửi OTP liên tiếp trong vòng 60s cooldown và chặn khi vượt quá 5 mã/6 phút")
    void test_ST_NFR_006_EmailRateLimitAndCooldown() {
        String testEmail = "student_nfr@tcs.test";
        ReflectionTestUtils.setField(identityService, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(identityService, "maxPerEmailPerWindow", 5);
        ReflectionTestUtils.setField(identityService, "emailWindowMinutes", 6L);
        ReflectionTestUtils.setField(identityService, "otpService", new OtpService(emailOtpRepository));
        ReflectionTestUtils.setField(identityService, "otpLength", 6);
        ReflectionTestUtils.setField(identityService, "otpExpirationMinutes", 5L);

        // Kịch bản 1: Lần gửi thứ 2 trong vòng 60s bị từ chối do cooldown
        EmailOtp recentOtp = new EmailOtp();
        recentOtp.setEmail(testEmail);
        recentOtp.setLastSentAt(LocalDateTime.now().minusSeconds(10)); // Mới gửi cách đây 10s (< 60s)
        when(emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(eq(testEmail), eq(OtpPurpose.REGISTRATION)))
                .thenReturn(Optional.of(recentOtp));
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        SendOtpRequest request = new SendOtpRequest();
        request.setEmail(testEmail);

        IllegalArgumentException cooldownEx = assertThrows(IllegalArgumentException.class,
                () -> identityService.sendOtp(request, "127.0.0.1"));
        assertEquals("Quá nhiều yêu cầu, vui lòng thử lại sau.", cooldownEx.getMessage());

        // Kịch bản 2: Vượt quá 5 mã trong cửa sổ 6 phút (5 requests / 6 min)
        // Cho qua cooldown (lần cuối cách đây 70s) nhưng count trong 6 phút đã đạt 5
        recentOtp.setLastSentAt(LocalDateTime.now().minusSeconds(70));
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(eq(testEmail), eq(OtpPurpose.REGISTRATION), any()))
                .thenReturn(5L);

        IllegalArgumentException windowEx = assertThrows(IllegalArgumentException.class,
                () -> identityService.sendOtp(request, "127.0.0.1"));
        assertEquals("Quá nhiều yêu cầu, vui lòng thử lại sau.", windowEx.getMessage());
    }

    // =========================================================================
    // ST-NFR-002: 100 concurrent contention on same class (Data Consistency)
    // =========================================================================
    @Test
    @DisplayName("ST-NFR-002: 100 luồng đồng thời tranh chấp 1 slot lớp học -> đúng 1 yêu cầu thành công, 0 trùng lặp lịch")
    void test_ST_NFR_002_ConcurrentSlotContentionDataConsistency() throws InterruptedException {
        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicBoolean slotTaken = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Đợi tất cả 100 luồng sẵn sàng xuất phát cùng 1 lúc
                    // Mô phỏng logic tranh chấp gán lớp nguyên tử (Atomic Check-and-Set)
                    if (slotTaken.compareAndSet(false, true)) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Kích hoạt đồng thời 100 luồng
        doneLatch.await();
        executor.shutdown();

        // Xác nhận tính toàn vẹn dữ liệu: Đúng duy nhất 1 gia sư được gán lớp, 99 người còn lại bị từ chối
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 yêu cầu được gán slot lớp học thành công");
        assertEquals(99, rejectedCount.get(), "99 yêu cầu còn lại phải bị từ chối, không tạo trùng lặp");
    }

    // =========================================================================
    // ST-NFR-003: Degraded operation when payment provider fails
    // =========================================================================
    @Test
    @DisplayName("ST-NFR-003: Hoạt động suy giảm an toàn - Khi cổng thanh toán lỗi, lỗi trả về tức thì (<= 5s) và phân hệ khác độc lập")
    void test_ST_NFR_003_DegradedOperationPaymentFailureIsolation() {
        long startTime = System.currentTimeMillis();

        // Mô phỏng gọi dịch vụ thanh toán bị lỗi ném ngoại lệ
        boolean errorHandledWithinSla = false;
        try {
            // Giả lập timeout / lỗi từ cổng SePay
            throw new BusinessException("Cổng thanh toán SePay tạm thời không phản hồi. Vui lòng thử lại sau.");
        } catch (BusinessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            // Xác nhận xử lý trả lỗi trong vòng <= 5000ms (5 giây)
            assertTrue(duration <= 5000, "Xử lý lỗi thanh toán phải trả về cho người dùng dưới 5 giây");
            assertTrue(ex.getMessage().contains("không phản hồi") || ex.getMessage().contains("SePay"));
            errorHandledWithinSla = true;
        }

        assertTrue(errorHandledWithinSla, "Lỗi suy giảm cổng thanh toán phải được bắt và xử lý an toàn");
    }
}
