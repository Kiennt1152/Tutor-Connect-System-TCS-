package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.mapper.VerificationMapper;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Unit test module Identity - luong nop / duyet / xem ho so xac minh.
 * Bam bo test case trong Report_5.1_UnitTest: cac sheet vrfSubmit, vrfReview,
 * getVerificationById, canResubmit.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificationServiceImplSubmitTest {

    private static final Long USER_ID = 300L;
    private static final Long ADMIN_ID = 1L;
    private static final Long STRANGER_ID = 777L;
    private static final Long VERIFICATION_ID = 950L;
    private static final Long FILE_FRONT = 11L;
    private static final Long FILE_BACK = 12L;

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
    @Mock private PlatformAdminRepository platformAdminRepository;

    @InjectMocks private VerificationServiceImpl service;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setEmail("tutor@tcs.vn");

        admin = new User();
        admin.setUserId(ADMIN_ID);
        admin.setEmail("admin@tcs.vn");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(platformAdminRepository.findAll()).thenReturn(List.of());
        when(verificationMapper.toResponse(any(), anyList()))
                .thenReturn(org.mockito.Mockito.mock(VerificationResponse.class));
        when(verificationDocumentRepository
                .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(any()))
                .thenReturn(List.of());
    }

    // --- helpers ------------------------------------------------------------

    private void givenRole(UserRole role) {
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(user, role));
    }

    private void givenNoPendingRequest() {
        when(verificationRequestRepository
                .existsByUser_UserIdAndVerificationTypeAndStatusIn(eq(USER_ID), any(), anyList()))
                .thenReturn(false);
    }

    private void givenOwnedFiles(Long... fileIds) {
        for (Long id : fileIds) {
            MediaFile file = new MediaFile();
            file.setUploadedBy(user);
            when(mediaFileRepository.findById(id)).thenReturn(Optional.of(file));
        }
    }

    private void givenSaveAssignsId() {
        when(verificationRequestRepository.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> {
                    VerificationRequest v = inv.getArgument(0);
                    v.setVerificationId(VERIFICATION_ID);
                    return v;
                });
        when(verificationRequestRepository.findById(VERIFICATION_ID))
                .thenAnswer(inv -> {
                    VerificationRequest v = new VerificationRequest();
                    v.setVerificationId(VERIFICATION_ID);
                    v.setUser(user);
                    v.setStatus(VerificationStatus.SUBMITTED);
                    return Optional.of(v);
                });
    }

    private static VerificationRequestDto.DocumentUpload doc(
            VerificationDocumentType type, Long fileId) {
        VerificationRequestDto.DocumentUpload d = new VerificationRequestDto.DocumentUpload();
        d.setDocumentType(type);
        d.setFileId(fileId);
        return d;
    }

    private static VerificationRequestDto request(
            VerificationType type, VerificationRequestDto.DocumentUpload... docs) {
        VerificationRequestDto dto = new VerificationRequestDto();
        dto.setVerificationType(type);
        dto.setDocuments(new ArrayList<>(Arrays.asList(docs)));
        return dto;
    }

    /** Bo 2 anh CCCD hop le dung cho phu huynh va gia su. */
    private static VerificationRequestDto tutorRequest() {
        return request(VerificationType.TUTOR_PROFILE,
                doc(VerificationDocumentType.ID_CARD, FILE_FRONT),
                doc(VerificationDocumentType.DEGREE, FILE_BACK));
    }

    // ========================================================================
    //  Sheet: canResubmit
    // ========================================================================

    @Nested
    @DisplayName("canResubmit")
    class CanResubmit {

        private void givenExisting(boolean pending) {
            when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                    eq(USER_ID), eq(VerificationType.TUTOR_PROFILE), anyList()))
                    .thenReturn(pending);
        }

        @Test
        @DisplayName("UTCID01 (N) - chưa từng nộp hồ sơ nào -> cho phép nộp mới")
        void utcid01_neverSubmitted() {
            givenExisting(false);
            assertTrue(service.canResubmit(USER_ID, VerificationType.TUTOR_PROFILE));
        }

        @Test
        @DisplayName("UTCID02 (N) - đã có hồ sơ SUBMITTED -> không cho nộp mới")
        void utcid02_submittedExists() {
            givenExisting(true);
            assertFalse(service.canResubmit(USER_ID, VerificationType.TUTOR_PROFILE));
        }

        @Test
        @DisplayName("UTCID03 (N) - đã có hồ sơ UNDER_REVIEW -> không cho nộp mới")
        void utcid03_underReviewExists() {
            givenExisting(true);
            assertFalse(service.canResubmit(USER_ID, VerificationType.TUTOR_PROFILE));
        }

        @Test
        @DisplayName("UTCID04 (N) - đã có hồ sơ VERIFIED -> vẫn cho nộp mới để cập nhật giấy tờ")
        void utcid04_verifiedExists() {
            // canResubmit chi chan SUBMITTED / UNDER_REVIEW; ho so VERIFIED cu se bi
            // rejectPreviousVerifiedRequests() thu hoi khi ho so moi duoc duyet.
            givenExisting(false);
            assertTrue(service.canResubmit(USER_ID, VerificationType.TUTOR_PROFILE));
        }

        @Test
        @DisplayName("UTCID05 (N) - chỉ còn hồ sơ REJECTED -> cho phép nộp lại")
        void utcid05_onlyRejectedExists() {
            givenExisting(false);
            assertTrue(service.canResubmit(USER_ID, VerificationType.TUTOR_PROFILE));
        }
    }

    // ========================================================================
    //  Sheet: vrfSubmit
    // ========================================================================

    @Nested
    @DisplayName("vrfSubmit")
    class Submit {

        @Test
        @DisplayName("UTCID01 (N) - gia sư nộp đủ 2 mặt CCCD -> tạo hồ sơ SUBMITTED kèm danh sách tài liệu")
        void utcid01_submitSuccessfully() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();
            givenOwnedFiles(FILE_FRONT, FILE_BACK);
            givenSaveAssignsId();

            VerificationResponse res = service.submitVerification(tutorRequest());

            assertNotNull(res);
            verify(verificationRequestRepository).save(any(VerificationRequest.class));
            verify(verificationDocumentRepository, times(2)).save(any());
            verify(verificationHistoryRepository).save(any());
            verify(auditLogService).record(
                    eq(USER_ID), eq("SUBMIT_VERIFICATION"), eq("VerificationRequest"),
                    eq(VERIFICATION_ID), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - vai trò không được phép nộp xác minh -> ForbiddenException")
        void utcid02_roleNotAllowed() {
            givenRole(UserRole.PLATFORM_ADMIN);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.submitVerification(tutorRequest()));
            assertEquals("Chỉ phụ huynh, gia sư hoặc trung tâm mới được nộp xác minh", ex.getMessage());
            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - phụ huynh/gia sư nộp sai loại hồ sơ -> chỉ được nộp xác minh danh tính")
        void utcid03_wrongTypeForIndividual() {
            givenRole(UserRole.CLIENT);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_CENTER_LICENSE,
                            doc(VerificationDocumentType.ID_CARD, FILE_FRONT))));
            assertEquals("Tài khoản này chỉ được nộp hồ sơ xác minh danh tính", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - trung tâm nộp sai loại hồ sơ -> chỉ được nộp TUTOR_CENTER_LICENSE")
        void utcid04_wrongTypeForCenter() {
            givenRole(UserRole.TUTOR_CENTER);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(tutorRequest()));
            assertEquals("Tài khoản trung tâm chỉ được nộp hồ sơ loại TUTOR_CENTER_LICENSE",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - đã có hồ sơ đang xử lý -> chặn nộp mới")
        void utcid05_alreadyPending() {
            givenRole(UserRole.TUTOR);
            when(verificationRequestRepository
                    .existsByUser_UserIdAndVerificationTypeAndStatusIn(eq(USER_ID), any(), anyList()))
                    .thenReturn(true);
            VerificationRequest pending = new VerificationRequest();
            pending.setVerificationId(VERIFICATION_ID);
            pending.setVerificationType(VerificationType.TUTOR_PROFILE);
            pending.setStatus(VerificationStatus.SUBMITTED);
            when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(USER_ID))
                    .thenReturn(List.of(pending));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(tutorRequest()));
            assertTrue(ex.getMessage()
                    .startsWith("Bạn đã có hồ sơ xác minh đang xử lý hoặc đã được duyệt"));
            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID06 (B) - danh sách tài liệu rỗng (ngay dưới ngưỡng tối thiểu 1) -> chặn")
        void utcid06_noDocument() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_PROFILE)));
            assertEquals("At least one document is required", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - tài liệu chưa gắn file đã tải lên -> chặn")
        void utcid07_documentWithoutFile() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();
            givenSaveAssignsId();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_PROFILE,
                            doc(VerificationDocumentType.ID_CARD, null),
                            doc(VerificationDocumentType.DEGREE, FILE_BACK))));
            assertEquals("Mỗi tài liệu xác minh phải có file đã tải lên", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - file không tồn tại -> ResourceNotFoundException")
        void utcid08_fileNotFound() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();
            givenSaveAssignsId();
            when(mediaFileRepository.findById(FILE_FRONT)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.submitVerification(tutorRequest()));
            assertEquals("File not found: " + FILE_FRONT, ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - file không thuộc sở hữu người nộp -> ForbiddenException")
        void utcid09_fileNotOwned() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();
            givenSaveAssignsId();
            User stranger = new User();
            stranger.setUserId(STRANGER_ID);
            MediaFile foreignFile = new MediaFile();
            foreignFile.setUploadedBy(stranger);
            when(mediaFileRepository.findById(FILE_FRONT)).thenReturn(Optional.of(foreignFile));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.submitVerification(tutorRequest()));
            assertEquals("File không thuộc sở hữu của bạn", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - tài liệu thiếu loại tài liệu -> chặn")
        void utcid10_documentWithoutType() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_PROFILE,
                            doc(null, FILE_FRONT),
                            doc(VerificationDocumentType.DEGREE, FILE_BACK))));
            assertEquals("Mỗi tài liệu xác minh phải có loại tài liệu", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (B) - hồ sơ trung tâm chỉ có 4/5 chứng từ bắt buộc -> chặn")
        void utcid11_centerMissingCertificate() {
            givenRole(UserRole.TUTOR_CENTER);
            givenNoPendingRequest();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_CENTER_LICENSE,
                            doc(VerificationDocumentType.LICENSE, 1L),
                            doc(VerificationDocumentType.LICENSE, 2L),
                            doc(VerificationDocumentType.ID_CARD, 3L),
                            doc(VerificationDocumentType.DEGREE, 4L))));
            assertTrue(ex.getMessage().startsWith("Hồ sơ trung tâm cần đủ 5 chứng từ bắt buộc"));
        }

        @Test
        @DisplayName("UTCID12 (B) - hồ sơ phụ huynh chỉ có 1 ảnh CCCD (ngay dưới ngưỡng 2) -> chặn")
        void utcid12_clientWrongPhotoCount() {
            givenRole(UserRole.CLIENT);
            givenNoPendingRequest();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_PROFILE,
                            doc(VerificationDocumentType.ID_CARD, FILE_FRONT))));
            assertEquals("Hồ sơ xác minh phụ huynh cần đúng 2 ảnh: CCCD/CMND mặt trước và mặt sau.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID13 (A) - hồ sơ gia sư thiếu ảnh mặt sau CCCD -> chặn")
        void utcid13_tutorMissingBackSide() {
            givenRole(UserRole.TUTOR);
            givenNoPendingRequest();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitVerification(request(VerificationType.TUTOR_PROFILE,
                            doc(VerificationDocumentType.ID_CARD, FILE_FRONT),
                            doc(VerificationDocumentType.CERTIFICATE, 9L))));
            assertEquals(
                    "Hồ sơ xác minh gia sư cần CCCD/CMND mặt trước, mặt sau; "
                            + "bằng cấp/chứng chỉ là không bắt buộc.",
                    ex.getMessage());
        }
    }

    // ========================================================================
    //  Sheet: vrfReview
    // ========================================================================

    @Nested
    @DisplayName("vrfReview")
    class Review {

        private VerificationRequest verification;

        private void givenVerification(VerificationStatus status) {
            verification = new VerificationRequest();
            verification.setVerificationId(VERIFICATION_ID);
            verification.setUser(user);
            verification.setVerificationType(VerificationType.TUTOR_PROFILE);
            verification.setStatus(status);
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                    .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
            when(verificationRequestRepository.findById(VERIFICATION_ID))
                    .thenReturn(Optional.of(verification));
            when(verificationRequestRepository.save(any(VerificationRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(USER_ID))
                    .thenReturn(List.of());
            when(tutorRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
            when(tutorCenterRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
        }

        private VerificationDecisionDto decision(String value) {
            VerificationDecisionDto dto = new VerificationDecisionDto();
            dto.setDecision(value);
            dto.setNote("Giấy tờ rõ ràng, đầy đủ hai mặt.");
            return dto;
        }

        @Test
        @DisplayName("UTCID01 (N) - hồ sơ SUBMITTED + quyết định APPROVE -> chuyển VERIFIED và ghi lịch sử")
        void utcid01_approveSubmitted() {
            givenVerification(VerificationStatus.SUBMITTED);

            service.reviewVerification(VERIFICATION_ID, decision("APPROVE"));

            assertEquals(VerificationStatus.VERIFIED, verification.getStatus());
            assertEquals(ADMIN_ID, verification.getReviewedBy());
            assertNotNull(verification.getReviewedAt());
            verify(verificationHistoryRepository).save(any());
        }

        @Test
        @DisplayName("UTCID02 (A) - hồ sơ không ở trạng thái duyệt được -> chặn")
        void utcid02_notReviewableStatus() {
            givenVerification(VerificationStatus.VERIFIED);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.reviewVerification(VERIFICATION_ID, decision("APPROVE")));
            assertEquals("Chỉ có thể duyệt hồ sơ ở trạng thái SUBMITTED hoặc UNDER_REVIEW",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - quyết định không phải APPROVE/REJECT -> chặn")
        void utcid03_invalidDecision() {
            givenVerification(VerificationStatus.UNDER_REVIEW);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reviewVerification(VERIFICATION_ID, decision("MAYBE")));
            assertEquals("Decision must be APPROVE or REJECT", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - cờ REVIEW_FLOW_ENABLED đang bật nên guard không chặn luồng duyệt")
        void utcid04_reviewFlowFlagIsEnabled() throws Exception {
            // REVIEW_FLOW_ENABLED la hang so bien dich; nhanh 'Review flow is temporarily
            // disabled.' chi dat toi khi co duoc dat false luc build. Test nay khoa gia tri
            // hien tai de neu ai do tat co thi test do ngay.
            java.lang.reflect.Field flag =
                    VerificationServiceImpl.class.getDeclaredField("REVIEW_FLOW_ENABLED");
            flag.setAccessible(true);
            assertTrue((boolean) flag.get(null),
                    "Khi cờ này false, reviewVerification phải ném 'Review flow is temporarily disabled.'");

            givenVerification(VerificationStatus.SUBMITTED);
            service.reviewVerification(VERIFICATION_ID, decision("REJECT"));
            assertEquals(VerificationStatus.REJECTED, verification.getStatus());
        }
    }

    // ========================================================================
    //  Sheet: getVerificationById
    // ========================================================================

    @Nested
    @DisplayName("getVerificationById")
    class GetById {

        private void givenVerificationOwnedByUser() {
            VerificationRequest verification = new VerificationRequest();
            verification.setVerificationId(VERIFICATION_ID);
            verification.setUser(user);
            verification.setStatus(VerificationStatus.SUBMITTED);
            when(verificationRequestRepository.findById(VERIFICATION_ID))
                    .thenReturn(Optional.of(verification));
        }

        @Test
        @DisplayName("UTCID01 (N) - người xem là chủ hồ sơ -> trả chi tiết kèm danh sách tài liệu")
        void utcid01_ownerCanView() {
            givenVerificationOwnedByUser();
            when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(user, UserRole.TUTOR));

            assertNotNull(service.getVerificationById(VERIFICATION_ID));
            verify(verificationDocumentRepository)
                    .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID);
        }

        @Test
        @DisplayName("UTCID02 (N) - người xem là quản trị viên -> xem được mọi hồ sơ")
        void utcid02_adminCanView() {
            givenVerificationOwnedByUser();
            when(authHelper.requireAuthenticated())
                    .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));

            assertNotNull(service.getVerificationById(VERIFICATION_ID));
        }

        @Test
        @DisplayName("UTCID03 (A) - không phải chủ hồ sơ cũng không phải admin -> ForbiddenException")
        void utcid03_strangerRejected() {
            givenVerificationOwnedByUser();
            User stranger = new User();
            stranger.setUserId(STRANGER_ID);
            when(authHelper.requireAuthenticated())
                    .thenReturn(new UserPrincipal(stranger, UserRole.TUTOR));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.getVerificationById(VERIFICATION_ID));
            assertEquals("Bạn không có quyền xem hồ sơ xác minh này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - hồ sơ không tồn tại -> ResourceNotFoundException")
        void utcid04_notFound() {
            when(verificationRequestRepository.findById(VERIFICATION_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.getVerificationById(VERIFICATION_ID));
            assertEquals("Verification not found: " + VERIFICATION_ID, ex.getMessage());
        }
    }
}
