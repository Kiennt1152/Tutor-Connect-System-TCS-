package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.mapper.VerificationMapper;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
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
 * Unit test module Identity — luồng duyệt hồ sơ xác minh.
 * Bám bộ test case trong Report_5.1_UnitTest: các sheet startReview, reviewVerification,
 * cancelVerification, getVerificationById.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificationServiceImplReviewTest {

    private static final Long OWNER_USER_ID = 200L;
    private static final Long STRANGER_USER_ID = 999L;
    private static final Long VERIFICATION_ID = 900L;

    @Mock private VerificationRequestRepository verificationRequestRepository;
    @Mock private VerificationDocumentRepository verificationDocumentRepository;
    @Mock private VerificationHistoryRepository verificationHistoryRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private VerificationMapper verificationMapper;
    @Mock private AuthHelper authHelper;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks private VerificationServiceImpl service;

    private User owner;
    private VerificationRequest verification;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUserId(OWNER_USER_ID);
        owner.setEmail("tutor1@tcs.vn");

        verification = new VerificationRequest();
        verification.setVerificationId(VERIFICATION_ID);
        verification.setUser(owner);
        verification.setStatus(VerificationStatus.SUBMITTED);

        when(authHelper.currentUserId()).thenReturn(OWNER_USER_ID);
        when(verificationRequestRepository.findById(VERIFICATION_ID)).thenReturn(Optional.of(verification));
    }

    // ===================================================================
    //  Sheet: cancelVerification
    // ===================================================================
    @Nested
    @DisplayName("cancelVerification")
    class CancelVerification {

        @Test
        @DisplayName("UTCID01 (N) - Chủ hồ sơ hủy khi đang SUBMITTED -> xóa hồ sơ + tài liệu + lịch sử")
        void utcid01_cancelOwnSubmitted() {
            service.cancelVerification(VERIFICATION_ID);

            verify(verificationDocumentRepository).deleteAllByVerificationId(VERIFICATION_ID);
            verify(verificationHistoryRepository).deleteAllByVerificationId(VERIFICATION_ID);
            verify(verificationRequestRepository).delete(verification);
        }

        @Test
        @DisplayName("UTCID02 (A) - Hủy hồ sơ của người khác -> ForbiddenException")
        void utcid02_cancelOfAnotherUser() {
            when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.cancelVerification(VERIFICATION_ID));
            assertEquals("Bạn không có quyền hủy hồ sơ xác minh này", ex.getMessage());
            verify(verificationRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Hồ sơ đã VERIFIED -> không cho hủy")
        void utcid03_cancelVerifiedRecord() {
            verification.setStatus(VerificationStatus.VERIFIED);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelVerification(VERIFICATION_ID));
            assertTrue(ex.getMessage().contains("Chỉ có thể hủy hồ sơ ở trạng thái SUBMITTED"),
                    "Thông báo phải nêu trạng thái hiện tại: " + ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Hồ sơ đã UNDER_REVIEW (khong con SUBMITTED) -> không cho hủy")
        void utcid04_cancelUnderReview() {
            verification.setStatus(VerificationStatus.UNDER_REVIEW);

            assertThrows(BusinessException.class, () -> service.cancelVerification(VERIFICATION_ID));
        }

        @Test
        @DisplayName("Bổ sung ngoài các UTCID của sheet cancelVerification - Hồ sơ không tồn tại -> ResourceNotFoundException")
        void utcid05_notFound() {
            when(verificationRequestRepository.findById(VERIFICATION_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.cancelVerification(VERIFICATION_ID));
            assertTrue(ex.getMessage().startsWith("Verification not found"));
        }
    }

    // ===================================================================
    //  Sheet: getVerificationsByUser
    // ===================================================================
    @Nested
    @DisplayName("getVerificationsByUser")
    class GetVerificationsByUser {

        @Test
        @DisplayName("Ngoài phạm vi Report 5.1 (MethodList không có getVerificationsByUser) - Xem hồ sơ xác minh của người khác -> ForbiddenException")
        void utcid01_viewOthers() {
            com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(
                    owner, com.tcs.module.profile.enums.UserRole.TUTOR);
            when(authHelper.requireAuthenticated()).thenReturn(principal);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.getVerificationsByUser(STRANGER_USER_ID));
            assertEquals("Bạn không có quyền xem hồ sơ xác minh của người khác", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: startReview
    //  Luu y: nhanh "review flow bi tat" khong kiem thu duoc vi
    //  REVIEW_FLOW_ENABLED la hang so bien dich (= true) -> nhanh chet.
    // ===================================================================
    @Nested
    @DisplayName("startReview")
    class StartReview {

        private static final Long ADMIN_USER_ID = 1L;

        private User admin;

        private void loginAsAdmin() {
            admin = new User();
            admin.setUserId(ADMIN_USER_ID);
            admin.setEmail("admin@tcs.vn");
            when(authHelper.requireRole(com.tcs.module.profile.enums.UserRole.PLATFORM_ADMIN))
                    .thenReturn(new com.tcs.security.UserPrincipal(
                            admin, com.tcs.module.profile.enums.UserRole.PLATFORM_ADMIN));
        }

        @Test
        @DisplayName("UTCID01 (N) - Ho so dang SUBMITTED, admin hop le -> chuyen sang UNDER_REVIEW + ghi lich su")
        void utcid01_startReviewSubmitted() {
            loginAsAdmin();
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
            when(verificationRequestRepository.save(verification)).thenReturn(verification);

            service.startReview(VERIFICATION_ID);

            assertEquals(VerificationStatus.UNDER_REVIEW, verification.getStatus());
            verify(verificationRequestRepository).save(verification);
            verify(verificationHistoryRepository).save(any());
        }

        @Test
        @DisplayName("UTCID02 (A) - Ho so khong o trang thai SUBMITTED -> 'Chỉ có thể bắt đầu duyệt khi hồ sơ ở trạng thái SUBMITTED'")
        void utcid02_notSubmitted() {
            loginAsAdmin();
            verification.setStatus(VerificationStatus.UNDER_REVIEW);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.startReview(VERIFICATION_ID));
            assertEquals("Chỉ có thể bắt đầu duyệt khi hồ sơ ở trạng thái SUBMITTED", ex.getMessage());
            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - Khong tim thay tai khoan admin dang dang nhap -> 'Admin not found: <id>'")
        void utcid04_adminNotFound() {
            loginAsAdmin();
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.startReview(VERIFICATION_ID));
            assertEquals("Admin not found: " + ADMIN_USER_ID, ex.getMessage());
            assertEquals(VerificationStatus.SUBMITTED, verification.getStatus(),
                    "Khong duoc doi trang thai khi chua xac dinh duoc admin");
            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - Nguoi dang nhap khong phai quan tri vien nen tang -> 'Không có quyền truy cập'")
        void utcid05_notPlatformAdmin() {
            when(authHelper.requireRole(com.tcs.module.profile.enums.UserRole.PLATFORM_ADMIN))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.startReview(VERIFICATION_ID));
            assertEquals("Không có quyền truy cập", ex.getMessage());
            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Ho so xac minh khong ton tai -> 'Verification not found: <id>'")
        void utcid03_verificationNotFound() {
            loginAsAdmin();
            when(verificationRequestRepository.findById(VERIFICATION_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.startReview(VERIFICATION_ID));
            assertEquals("Verification not found: " + VERIFICATION_ID, ex.getMessage());
        }
    }
}
