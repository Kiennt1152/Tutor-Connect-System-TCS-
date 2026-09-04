package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationHistory;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.mapper.VerificationMapper;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52VerificationServiceITTest {

    private static final Long OWNER_USER_ID = 200L;
    private static final Long STRANGER_USER_ID = 999L;
    private static final Long ADMIN_USER_ID = 1L;
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
    @Mock private PlatformAdminRepository platformAdminRepository;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    private User owner;
    private VerificationRequest verification;

    @BeforeEach
    void setUpVerificationItFixture() {
        owner = user(OWNER_USER_ID, "tutor.it@tcs.test");
        verification = verification(VERIFICATION_ID, owner, VerificationType.TUTOR_PROFILE, VerificationStatus.SUBMITTED);

        when(authHelper.currentUserId()).thenReturn(OWNER_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(owner, UserRole.TUTOR));
        when(verificationRequestRepository.findById(VERIFICATION_ID)).thenReturn(Optional.of(verification));
        when(verificationMapper.toResponse(any(VerificationRequest.class), any()))
                .thenAnswer(invocation -> {
                    VerificationRequest source = invocation.getArgument(0);
                    return VerificationResponse.builder()
                            .verificationId(source.getVerificationId())
                            .userId(source.getUser().getUserId())
                            .userEmail(source.getUser().getEmail())
                            .verificationType(source.getVerificationType())
                            .status(source.getStatus())
                            .build();
                });
    }

    @Test
    @Tag("report52-it")
    void IT_VER_001_SubmitTutorVerificationStoresDocumentsAndNotifiesAdmin() {
        VerificationRequestDto request = tutorVerificationRequest(101L, 102L);
        MediaFile front = mediaFile(101L, owner);
        MediaFile back = mediaFile(102L, owner);
        User adminUser = user(ADMIN_USER_ID, "admin.it@tcs.test");
        PlatformAdmin admin = new PlatformAdmin();
        admin.setUser(adminUser);
        VerificationRequest[] savedHolder = new VerificationRequest[1];

        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(OWNER_USER_ID),
                eq(VerificationType.TUTOR_PROFILE),
                any()))
                .thenReturn(false);
        when(mediaFileRepository.findById(101L)).thenReturn(Optional.of(front));
        when(mediaFileRepository.findById(102L)).thenReturn(Optional.of(back));
        when(platformAdminRepository.findAll()).thenReturn(List.of(admin));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> {
            VerificationRequest saved = invocation.getArgument(0);
            saved.setVerificationId(VERIFICATION_ID);
            savedHolder[0] = saved;
            return saved;
        });
        when(verificationRequestRepository.findById(VERIFICATION_ID))
                .thenAnswer(invocation -> Optional.of(savedHolder[0]));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                .thenReturn(List.of());

        VerificationResponse response = verificationService.submitVerification(request);

        assertEquals(VERIFICATION_ID, response.getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, response.getStatus());
        verify(verificationDocumentRepository, org.mockito.Mockito.times(2)).save(any(VerificationDocument.class));
        verify(notificationDispatchService).notifyUser(
                eq(adminUser),
                eq(NotificationType.VERIFICATION),
                eq("Có hồ sơ xác minh mới"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(VERIFICATION_ID));
    }

    @Test
    @Tag("report52-it")
    void IT_VER_002_ListSubmittedVerificationQueueByStatusForAdmin() {
        VerificationRequest submitted = verification(
                901L,
                owner,
                VerificationType.TUTOR_PROFILE,
                VerificationStatus.SUBMITTED);

        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED))
                .thenReturn(List.of(submitted));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(901L))
                .thenReturn(List.of());

        List<VerificationResponse> responses = verificationService.getVerificationsByStatus(VerificationStatus.SUBMITTED);

        assertEquals(1, responses.size());
        assertEquals(901L, responses.get(0).getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, responses.get(0).getStatus());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
    }

    @Test
    @Tag("report52-it")
    void IT_VER_003_OwnerCanReadVerificationDetailWithDocuments() {
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                .thenReturn(List.of());

        VerificationResponse response = verificationService.getVerificationById(VERIFICATION_ID);

        assertEquals(VERIFICATION_ID, response.getVerificationId());
        assertEquals(OWNER_USER_ID, response.getUserId());
        assertEquals(VerificationStatus.SUBMITTED, response.getStatus());
        verify(verificationDocumentRepository)
                .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID);
    }

    @Test
    @Tag("report52-it")
    void IT_VER_004_RejectSubmitVerificationWithoutDocuments() {
        VerificationRequestDto request = new VerificationRequestDto();
        request.setVerificationType(VerificationType.TUTOR_PROFILE);
        request.setDocuments(List.of());

        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(OWNER_USER_ID),
                eq(VerificationType.TUTOR_PROFILE),
                any()))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> verificationService.submitVerification(request));

        assertEquals("At least one document is required", exception.getMessage());
        verify(verificationRequestRepository, never()).save(any());
        verify(verificationDocumentRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_009_RejectDuplicateSubmittedVerificationRequest() {
        VerificationRequestDto request = tutorVerificationRequest(101L, 102L);

        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(OWNER_USER_ID),
                eq(VerificationType.TUTOR_PROFILE),
                any()))
                .thenReturn(true);
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(OWNER_USER_ID))
                .thenReturn(List.of(verification));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationService.submitVerification(request));

        assertTrue(exception.getMessage().contains("Bạn đã có hồ sơ xác minh đang xử lý hoặc đã được duyệt"));
        verify(mediaFileRepository, never()).findById(any());
        verify(verificationDocumentRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_017_CancelOwnSubmittedVerificationDeletesRequestDocumentsAndHistory() {
        verificationService.cancelVerification(VERIFICATION_ID);

        verify(verificationDocumentRepository).deleteAllByVerificationId(VERIFICATION_ID);
        verify(verificationHistoryRepository).deleteAllByVerificationId(VERIFICATION_ID);
        verify(verificationRequestRepository).delete(verification);
    }

    @Test
    @Tag("report52-it")
    void IT_VER_008_RejectCancelVerificationOwnedByAnotherUser() {
        when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.cancelVerification(VERIFICATION_ID));

        assertEquals("Bạn không có quyền hủy hồ sơ xác minh này", exception.getMessage());
        verify(verificationRequestRepository, never()).delete(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_005_RejectCancelAlreadyVerifiedRequest() {
        verification.setStatus(VerificationStatus.VERIFIED);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationService.cancelVerification(VERIFICATION_ID));

        assertTrue(exception.getMessage().contains("Chỉ có thể hủy hồ sơ ở trạng thái SUBMITTED"));
        verify(verificationRequestRepository, never()).delete(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_006_BlockAnonymousSubmitBeforeUserAndFileRowsAreLoaded() {
        VerificationRequestDto request = tutorVerificationRequest(101L, 102L);

        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.submitVerification(request));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(mediaFileRepository, never()).findById(any());
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_007_BlockPlatformAdminFromSubmittingUserVerification() {
        VerificationRequestDto request = tutorVerificationRequest(101L, 102L);
        User admin = user(ADMIN_USER_ID, "admin.it@tcs.test");

        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.submitVerification(request));

        assertEquals("Chỉ phụ huynh, gia sư hoặc trung tâm mới được nộp xác minh", exception.getMessage());
        verify(verificationRequestRepository, never()).save(any());
        verify(verificationDocumentRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_013_RejectReadingAnotherUsersVerificationHistory() {
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(owner, UserRole.TUTOR));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.getVerificationsByUser(STRANGER_USER_ID));

        assertEquals("Bạn không có quyền xem hồ sơ xác minh của người khác", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_010_StartReviewMovesSubmittedRequestToUnderReviewAndWritesHistory() {
        User admin = user(ADMIN_USER_ID, "admin.it@tcs.test");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                .thenReturn(List.of());

        VerificationResponse response = verificationService.startReview(VERIFICATION_ID);

        assertEquals(VerificationStatus.UNDER_REVIEW, response.getStatus());
        assertEquals(VerificationStatus.UNDER_REVIEW, verification.getStatus());
        ArgumentCaptor<VerificationHistory> historyCaptor = ArgumentCaptor.forClass(VerificationHistory.class);
        verify(verificationHistoryRepository).save(historyCaptor.capture());
        assertEquals(VerificationStatus.SUBMITTED.name(), historyCaptor.getValue().getOldStatus());
        assertEquals(VerificationStatus.UNDER_REVIEW.name(), historyCaptor.getValue().getNewStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_011_RejectVerificationNotifiesRequesterWithReason() {
        User admin = user(ADMIN_USER_ID, "admin.it@tcs.test");
        VerificationDecisionDto decision = new VerificationDecisionDto();
        decision.setDecision("REJECT");
        decision.setNote("Ảnh CCCD bị mờ");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tutorRepository.findByUser_UserId(OWNER_USER_ID)).thenReturn(Optional.empty());
        when(tutorCenterRepository.findByUser_UserId(OWNER_USER_ID)).thenReturn(Optional.empty());
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                .thenReturn(List.of());

        VerificationResponse response = verificationService.reviewVerification(VERIFICATION_ID, decision);

        assertEquals(VerificationStatus.REJECTED, response.getStatus());
        assertEquals("Ảnh CCCD bị mờ", verification.getRejectionReason());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(owner),
                eq(NotificationType.VERIFICATION),
                eq("VERIFICATION_REJECTED"),
                any(),
                eq("Hồ sơ xác minh bị từ chối"),
                eq("Lý do: Ảnh CCCD bị mờ. Bạn có thể nộp lại sau khi bổ sung giấy tờ."),
                eq("VERIFICATION_REQUEST"),
                eq(VERIFICATION_ID));
    }

    @Test
    @Tag("report52-it")
    void IT_VER_012_MyVerificationListKeepsSubmittedRequestForBrowserReturn() {
        VerificationRequest submitted = verification(
                903L,
                owner,
                VerificationType.TUTOR_PROFILE,
                VerificationStatus.SUBMITTED);

        when(authHelper.currentUserId()).thenReturn(OWNER_USER_ID);
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(OWNER_USER_ID))
                .thenReturn(List.of(submitted));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(903L))
                .thenReturn(List.of());

        List<VerificationResponse> responses = verificationService.getMyVerifications();

        assertEquals(1, responses.size());
        assertEquals(903L, responses.get(0).getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, responses.get(0).getStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_014_RejectVerificationDocumentThatBelongsToAnotherUser() {
        VerificationRequestDto request = tutorVerificationRequest(101L, 102L);
        MediaFile front = mediaFile(101L, user(STRANGER_USER_ID, "other.it@tcs.test"));

        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(OWNER_USER_ID),
                eq(VerificationType.TUTOR_PROFILE),
                any()))
                .thenReturn(false);
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> {
            VerificationRequest saved = invocation.getArgument(0);
            saved.setVerificationId(VERIFICATION_ID);
            return saved;
        });
        when(mediaFileRepository.findById(101L)).thenReturn(Optional.of(front));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.submitVerification(request));

        assertEquals("File không thuộc sở hữu của bạn", exception.getMessage());
        verify(verificationDocumentRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_015_ListSubmittedQueueReturnsOneResponsePerVerificationWithDocumentsLoaded() {
        VerificationRequest first = verification(904L, owner, VerificationType.TUTOR_PROFILE, VerificationStatus.SUBMITTED);
        VerificationRequest second = verification(905L, owner, VerificationType.TUTOR_PROFILE, VerificationStatus.SUBMITTED);

        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED))
                .thenReturn(List.of(first, second));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(904L))
                .thenReturn(List.of(new VerificationDocument()));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(905L))
                .thenReturn(List.of(new VerificationDocument(), new VerificationDocument()));

        List<VerificationResponse> responses = verificationService.getVerificationsByStatus(VerificationStatus.SUBMITTED);

        assertEquals(2, responses.size());
        assertEquals(904L, responses.get(0).getVerificationId());
        assertEquals(905L, responses.get(1).getVerificationId());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(verificationDocumentRepository)
                .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(904L);
        verify(verificationDocumentRepository)
                .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(905L);
    }

    @Test
    @Tag("report52-it")
    void IT_VER_016_ApproveTutorVerificationSyncsTutorProfileToVerified() {
        User admin = user(ADMIN_USER_ID, "admin.it@tcs.test");
        Tutor tutor = new Tutor();
        tutor.setUser(owner);

        VerificationDecisionDto decision = new VerificationDecisionDto();
        decision.setDecision("APPROVE");
        decision.setNote("Hồ sơ hợp lệ");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(OWNER_USER_ID))
                .thenReturn(List.of(verification));
        when(tutorRepository.findByUser_UserId(OWNER_USER_ID)).thenReturn(Optional.of(tutor));
        when(tutorCenterRepository.findByUser_UserId(OWNER_USER_ID)).thenReturn(Optional.empty());
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                .thenReturn(List.of());

        verificationService.reviewVerification(VERIFICATION_ID, decision);

        assertEquals(ProfileVerificationStatus.VERIFIED, tutor.getVerificationStatus());
        verify(tutorRepository).save(tutor);
    }

    @Test
    @Tag("report52-it")
    void IT_VER_018_AdminVerificationQueuePreservesStatusFilterAndSubmittedOrder() {
        VerificationRequest older = verification(
                906L,
                owner,
                VerificationType.TUTOR_PROFILE,
                VerificationStatus.SUBMITTED);
        older.setSubmittedAt(LocalDateTime.of(2026, 8, 30, 8, 0));
        VerificationRequest newer = verification(
                907L,
                owner,
                VerificationType.TUTOR_PROFILE,
                VerificationStatus.SUBMITTED);
        newer.setSubmittedAt(LocalDateTime.of(2026, 8, 31, 8, 0));

        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED))
                .thenReturn(List.of(older, newer));
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(906L))
                .thenReturn(List.of());
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(907L))
                .thenReturn(List.of());

        List<VerificationResponse> responses = verificationService.getVerificationsByStatus(VerificationStatus.SUBMITTED);

        assertEquals(2, responses.size());
        assertEquals(906L, responses.get(0).getVerificationId());
        assertEquals(907L, responses.get(1).getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, responses.get(0).getStatus());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(verificationRequestRepository, never()).findByStatusOrderBySubmittedAtAsc(VerificationStatus.VERIFIED);
    }

    @Test
    @Tag("report52-it")
    void IT_VER_019_RejectInvalidReviewDecisionWithClearMessage() {
        User admin = user(ADMIN_USER_ID, "admin.it@tcs.test");
        VerificationDecisionDto decision = new VerificationDecisionDto();
        decision.setDecision("HOLD");
        decision.setNote("Không phải quyết định hợp lệ");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> verificationService.reviewVerification(VERIFICATION_ID, decision));

        assertEquals("Decision must be APPROVE or REJECT", exception.getMessage());
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_VER_020_AdminApprovalOfNewRequestRejectsPreviousVerifiedRequest() {
        User admin = user(ADMIN_USER_ID, "admin.it@tcs.test");
        VerificationRequest newRequest = verification(902L, owner, VerificationType.TUTOR_PROFILE, VerificationStatus.SUBMITTED);
        VerificationRequest previousVerified = verification(901L, owner, VerificationType.TUTOR_PROFILE, VerificationStatus.VERIFIED);
        VerificationDecisionDto decision = new VerificationDecisionDto();
        decision.setDecision("APPROVE");
        decision.setNote("Chứng từ cập nhật hợp lệ");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.findById(902L)).thenReturn(Optional.of(newRequest));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(OWNER_USER_ID))
                .thenReturn(List.of(newRequest, previousVerified));
        when(tutorRepository.findByUser_UserId(OWNER_USER_ID)).thenReturn(Optional.empty());
        when(tutorCenterRepository.findByUser_UserId(OWNER_USER_ID)).thenReturn(Optional.empty());
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(902L))
                .thenReturn(List.of());

        VerificationResponse response = verificationService.reviewVerification(902L, decision);

        assertEquals(VerificationStatus.VERIFIED, response.getStatus());
        assertEquals(VerificationStatus.REJECTED, previousVerified.getStatus());
        assertTrue(previousVerified.getAdminNotes().contains("thay thế bởi hồ sơ xác minh mới #902"));
        verify(verificationRequestRepository).save(previousVerified);
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(owner),
                eq(NotificationType.VERIFICATION),
                eq("VERIFICATION_APPROVED"),
                any(),
                eq("Hồ sơ xác minh được duyệt"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(902L));
    }

    private VerificationRequestDto tutorVerificationRequest(Long frontFileId, Long backFileId) {
        VerificationRequestDto request = new VerificationRequestDto();
        request.setVerificationType(VerificationType.TUTOR_PROFILE);
        VerificationRequestDto.DocumentUpload front = new VerificationRequestDto.DocumentUpload();
        front.setDocumentType(VerificationDocumentType.ID_CARD);
        front.setFileId(frontFileId);
        VerificationRequestDto.DocumentUpload back = new VerificationRequestDto.DocumentUpload();
        back.setDocumentType(VerificationDocumentType.DEGREE);
        back.setFileId(backFileId);
        request.setDocuments(List.of(front, back));
        return request;
    }

    private VerificationRequest verification(
            Long verificationId,
            User owner,
            VerificationType type,
            VerificationStatus status) {

        VerificationRequest request = new VerificationRequest();
        request.setVerificationId(verificationId);
        request.setUser(owner);
        request.setVerificationType(type);
        request.setStatus(status);
        return request;
    }

    private MediaFile mediaFile(Long fileId, User uploadedBy) {
        MediaFile file = new MediaFile();
        file.setFileId(fileId);
        file.setUploadedBy(uploadedBy);
        file.setFileName("cccd-" + fileId + ".jpg");
        file.setFileUrl("/uploads/cccd-" + fileId + ".jpg");
        file.setMimeType("image/jpeg");
        file.setFileSize(1000L);
        return file;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        return user;
    }
}
