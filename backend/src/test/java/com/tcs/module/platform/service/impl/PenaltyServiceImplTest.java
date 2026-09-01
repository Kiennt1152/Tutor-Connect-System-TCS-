package com.tcs.module.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.entity.UserPenalty;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module Platform — ban hanh va thu hoi quyet dinh xu phat (UC-60).
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet issuePenalty va revokePenalty.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PenaltyServiceImplTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 200L;
    private static final Long PENALTY_ID = 900L;
    private static final String VALID_REASON = "Nguoi dung lien tuc gui tin nhan quang cao ngoai nen tang";

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

    @InjectMocks private PenaltyServiceImpl service;

    private User adminUser;
    private PlatformAdmin admin;
    private User target;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(ADMIN_USER_ID);
        adminUser.setEmail("admin@tcs.vn");

        admin = new PlatformAdmin();
        admin.setUser(adminUser);
        admin.setFullName("Quan tri vien 1");

        target = new User();
        target.setUserId(TARGET_USER_ID);
        target.setEmail("hocvien@tcs.vn");
        target.setStatus(UserStatus.ACTIVE);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        // Mac dinh: nguoi bi phat KHONG phai admin.
        when(platformAdminRepository.findByUser_UserId(TARGET_USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(target));
        when(userPenaltyRepository.save(any(UserPenalty.class))).thenAnswer(i -> {
            UserPenalty p = i.getArgument(0);
            p.setPenaltyId(PENALTY_ID);
            return p;
        });
    }

    /** Yeu cau xu phat hop le mac dinh: canh cao, ly do du dai, nguon DIRECT. */
    private IssuePenaltyRequest issueRequest(String penaltyType) {
        IssuePenaltyRequest r = new IssuePenaltyRequest();
        r.setUserId(TARGET_USER_ID);
        r.setPenaltyType(penaltyType);
        r.setReason(VALID_REASON);
        return r;
    }

    private UserPenalty penalty(User owner, UserPenaltyStatus status) {
        UserPenalty p = new UserPenalty();
        p.setPenaltyId(PENALTY_ID);
        p.setUser(owner);
        p.setIssuedBy(admin);
        p.setPenaltyType(UserPenaltyType.WARNING);
        p.setReason(VALID_REASON);
        p.setStatus(status);
        p.setStartsAt(LocalDateTime.now().minusDays(1));
        p.setCreatedAt(LocalDateTime.now().minusDays(1));
        return p;
    }

    // ===================================================================
    //  Sheet: issuePenalty
    // ===================================================================
    @Nested
    @DisplayName("issuePenalty")
    class IssuePenalty {

        @Test
        @DisplayName("UTCID01 (N) - Nguoi dung thuong, loai/nguon hop le, ly do du dai -> tao UserPenalty ACTIVE")
        void utcid01_issueWarningSuccessfully() {
            service.issuePenalty(issueRequest("WARNING"));

            ArgumentCaptor<UserPenalty> captor = ArgumentCaptor.forClass(UserPenalty.class);
            verify(userPenaltyRepository).save(captor.capture());
            UserPenalty saved = captor.getValue();
            assertEquals(UserPenaltyStatus.ACTIVE, saved.getStatus());
            assertEquals(UserPenaltyType.WARNING, saved.getPenaltyType());
            assertEquals(TARGET_USER_ID, saved.getUser().getUserId());
            // Canh cao khong khoa tai khoan.
            assertEquals(UserStatus.ACTIVE, target.getStatus());
            verify(notificationDispatchService).notifyUserFromTemplate(
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - Admin tu ap dung hinh phat cho chinh minh -> 'Quản trị viên không thể tự áp dụng hình phạt.'")
        void utcid02_cannotPenalizeSelf() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setUserId(ADMIN_USER_ID);
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Quản trị viên không thể tự áp dụng hình phạt.", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Doi tuong la quan tri vien khac -> 'Không thể áp dụng hình phạt cho tài khoản quản trị viên khác.'")
        void utcid03_cannotPenalizeAnotherAdmin() {
            PlatformAdmin otherAdmin = new PlatformAdmin();
            otherAdmin.setUser(target);
            when(platformAdminRepository.findByUser_UserId(TARGET_USER_ID)).thenReturn(Optional.of(otherAdmin));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(issueRequest("WARNING")));
            assertEquals("Không thể áp dụng hình phạt cho tài khoản quản trị viên khác.", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (B) - Ly do ngan hon 20 ky tu -> 'Lý do xử phạt phải có ít nhất 20 ký tự.'")
        void utcid04_reasonTooShort() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setReason("Spam qua nhieu."); // 15 ky tu

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Lý do xử phạt phải có ít nhất 20 ký tự.", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - Loai hinh phat khong hop le -> 'Loại hình phạt không hợp lệ: <giá trị>'")
        void utcid05_invalidPenaltyType() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(issueRequest("KHONG_TON_TAI")));
            assertEquals("Loại hình phạt không hợp lệ: KHONG_TON_TAI", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID06 (A) - Cam tam thoi nhung thoi han khong hop le -> 'Thời gian hết hạn không hợp lệ cho cấm tạm thời'")
        void utcid06_temporaryBanWithoutValidExpiry() {
            IssuePenaltyRequest missingExpiry = issueRequest("TEMPORARY_BAN");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(missingExpiry));
            assertEquals("Thời gian hết hạn không hợp lệ cho cấm tạm thời", ex.getMessage());

            IssuePenaltyRequest pastExpiry = issueRequest("TEMPORARY_BAN");
            pastExpiry.setExpiresAt(LocalDateTime.now().minusDays(1));
            assertThrows(IllegalArgumentException.class, () -> service.issuePenalty(pastExpiry));
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID07 (A) - Loai nguon xu phat khong hop le -> 'Loại nguồn xử phạt không hợp lệ: ...'")
        void utcid07_invalidSourceType() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceType("EMAIL");
            request.setSourceId(5L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertTrue(ex.getMessage().startsWith("Loại nguồn xử phạt không hợp lệ: EMAIL"),
                    "Phai neu ro nguon khong hop le: " + ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID08 (A) - Co sourceType nhung thieu sourceId -> 'sourceId là bắt buộc khi sourceType là REPORT'")
        void utcid08_sourceIdMissing() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceType("REPORT");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertEquals("sourceId là bắt buộc khi sourceType là REPORT", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID09 (A) - Co sourceId nhung thieu sourceType -> 'sourceType là bắt buộc khi có sourceId.'")
        void utcid09_sourceTypeMissing() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceId(5L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertEquals("sourceType là bắt buộc khi có sourceId.", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID10 (A) - sourceType = REPORT nhung khong co bao cao -> 'Không tìm thấy báo cáo #<id>'")
        void utcid10_reportSourceNotFound() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceType("REPORT");
            request.setSourceId(77L);
            when(reportRepository.existsById(77L)).thenReturn(false);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Không tìm thấy báo cáo #77", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID11 (A) - sourceType = CIRCUMVENTION nhung khong co su kien lan bao cao -> ResourceNotFoundException")
        void utcid11_circumventionSourceNotFound() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceType("CIRCUMVENTION");
            request.setSourceId(78L);
            when(circumventionEventRepository.existsById(78L)).thenReturn(false);
            when(reportRepository.existsById(78L)).thenReturn(false);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Không tìm thấy sự kiện lách sàn hoặc báo cáo #78", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID12 (A) - sourceType = DISPUTE nhung khong co tranh chap -> 'Không tìm thấy tranh chấp #<id>'")
        void utcid12_disputeSourceNotFound() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceType("DISPUTE");
            request.setSourceId(79L);
            when(disputeRepository.existsById(79L)).thenReturn(false);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Không tìm thấy tranh chấp #79", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID13 (A) - sourceType = TICKET nhung khong co ticket -> 'Không tìm thấy ticket #<id>'")
        void utcid13_ticketSourceNotFound() {
            IssuePenaltyRequest request = issueRequest("WARNING");
            request.setSourceType("TICKET");
            request.setSourceId(80L);
            when(supportTicketRepository.existsById(80L)).thenReturn(false);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Không tìm thấy ticket #80", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID14 (A) - FEATURE_RESTRICTION voi restrictionDetails rong -> 'Hạn chế tính năng phải có mã tính năng.'")
        void utcid14_featureRestrictionWithoutDetails() {
            IssuePenaltyRequest request = issueRequest("FEATURE_RESTRICTION");
            request.setRestrictionDetails("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertEquals("Hạn chế tính năng phải có mã tính năng.", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID15 (A) - restrictionDetails khong phai JSON / khong co ma hop le -> chan")
        void utcid15_featureRestrictionWithBadDetails() {
            IssuePenaltyRequest request = issueRequest("FEATURE_RESTRICTION");
            request.setRestrictionDetails("WITHDRAWAL"); // dung ma nhung khong phai JSON

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.issuePenalty(request));
            assertTrue(ex.getMessage().startsWith("restrictionDetails phải là JSON chứa mã tính năng hợp lệ"),
                    "Phai neu ro yeu cau JSON + ma tinh nang: " + ex.getMessage());

            IssuePenaltyRequest jsonWithoutCode = issueRequest("FEATURE_RESTRICTION");
            jsonWithoutCode.setRestrictionDetails("{\"features\":[\"UNKNOWN_CODE\"]}");
            assertThrows(IllegalArgumentException.class, () -> service.issuePenalty(jsonWithoutCode));
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID16 (B) - restrictionDetails la JSON chua ma hop le (WITHDRAWAL) -> tao hinh phat")
        void utcid16_featureRestrictionAtBoundary() {
            IssuePenaltyRequest request = issueRequest("FEATURE_RESTRICTION");
            request.setRestrictionDetails("{\"features\":[\"WITHDRAWAL\"]}");

            service.issuePenalty(request);

            ArgumentCaptor<UserPenalty> captor = ArgumentCaptor.forClass(UserPenalty.class);
            verify(userPenaltyRepository).save(captor.capture());
            UserPenalty saved = captor.getValue();
            assertEquals(UserPenaltyType.FEATURE_RESTRICTION, saved.getPenaltyType());
            assertEquals(UserPenaltyStatus.ACTIVE, saved.getStatus());
            assertEquals("{\"features\":[\"WITHDRAWAL\"]}", saved.getRestrictionDetails());
            // Han che tinh nang khong khoa tai khoan.
            assertEquals(UserStatus.ACTIVE, target.getStatus());
        }
    }

    // ===================================================================
    //  Sheet: revokePenalty
    // ===================================================================
    @Nested
    @DisplayName("revokePenalty")
    class RevokePenalty {

        private RevokePenaltyRequest revokeRequest() {
            RevokePenaltyRequest r = new RevokePenaltyRequest();
            r.setRevokedReason("Nguoi dung da khieu nai thanh cong, go bo hinh phat");
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - Hinh phat cua nguoi khac dang ACTIVE -> chuyen REVOKED va khoi phuc quyen")
        void utcid01_revokeActivePenalty() {
            target.setStatus(UserStatus.BANNED);
            UserPenalty active = penalty(target, UserPenaltyStatus.ACTIVE);
            when(userPenaltyRepository.findById(PENALTY_ID)).thenReturn(Optional.of(active));
            when(userPenaltyRepository.existsByUser_UserIdAndStatusAndPenaltyTypeIn(
                    org.mockito.ArgumentMatchers.eq(TARGET_USER_ID),
                    org.mockito.ArgumentMatchers.eq(UserPenaltyStatus.ACTIVE),
                    org.mockito.ArgumentMatchers.any())).thenReturn(false);

            service.revokePenalty(PENALTY_ID, revokeRequest());

            assertEquals(UserPenaltyStatus.REVOKED, active.getStatus());
            org.junit.jupiter.api.Assertions.assertNotNull(active.getRevokedAt());
            assertEquals(UserStatus.ACTIVE, target.getStatus(),
                    "Khong con lenh cam nao con hieu luc thi tai khoan phai duoc mo lai");
            verify(userRepository).save(target);
        }

        @Test
        @DisplayName("UTCID02 (A) - Thu hoi hinh phat cua chinh minh -> 'Quản trị viên không thể tự thu hồi hình phạt của chính mình.'")
        void utcid02_cannotRevokeOwnPenalty() {
            UserPenalty ownPenalty = penalty(adminUser, UserPenaltyStatus.ACTIVE);
            when(userPenaltyRepository.findById(PENALTY_ID)).thenReturn(Optional.of(ownPenalty));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.revokePenalty(PENALTY_ID, revokeRequest()));
            assertEquals("Quản trị viên không thể tự thu hồi hình phạt của chính mình.", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Hinh phat khong con ACTIVE -> 'Chỉ có thể thu hồi hình phạt đang hoạt động'")
        void utcid03_penaltyNoLongerActive() {
            UserPenalty expired = penalty(target, UserPenaltyStatus.EXPIRED);
            when(userPenaltyRepository.findById(PENALTY_ID)).thenReturn(Optional.of(expired));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.revokePenalty(PENALTY_ID, revokeRequest()));
            assertEquals("Chỉ có thể thu hồi hình phạt đang hoạt động", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - penaltyId khong khop hinh phat nao -> 'Không tìm thấy hình phạt'")
        void utcid04_penaltyNotFound() {
            when(userPenaltyRepository.findById(PENALTY_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.revokePenalty(PENALTY_ID, revokeRequest()));
            assertEquals("Không tìm thấy hình phạt", ex.getMessage());
            verify(userPenaltyRepository, never()).save(any());
        }
    }

    /** PERMANENT_BAN xoa expiresAt va khoa tai khoan — kiem tra rieng de bao ve hai hanh vi nay. */
    /** Sheet issuePenalty - UTCID01 (N): cấm vĩnh viễn người dùng thường -> khoá tài khoản và xoá thời hạn hết hiệu lực */
    @Test
    @DisplayName("issuePenalty - PERMANENT_BAN khoa tai khoan va bo expiresAt")
    void permanentBanLocksAccountAndClearsExpiry() {
        IssuePenaltyRequest request = issueRequest("PERMANENT_BAN");
        request.setExpiresAt(LocalDateTime.now().plusDays(30));

        service.issuePenalty(request);

        ArgumentCaptor<UserPenalty> captor = ArgumentCaptor.forClass(UserPenalty.class);
        verify(userPenaltyRepository).save(captor.capture());
        assertNull(captor.getValue().getExpiresAt(), "Cam vinh vien thi khong co thoi han");
        assertEquals(UserStatus.BANNED, target.getStatus());
        verify(userRepository).save(target);
    }
}
