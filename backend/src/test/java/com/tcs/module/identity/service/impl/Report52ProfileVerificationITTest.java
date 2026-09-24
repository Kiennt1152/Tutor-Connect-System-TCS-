package com.tcs.module.identity.service.impl;

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
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52ProfileVerificationITTest {


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

    
    /**
     * Test Case: IT-VER-001
     * Title: Submit tutor verification documents and notify the platform administrator.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit).
     * Input: TUTOR_PROFILE with file IDs 101 and 102.
     * Steps:
     *   1. Prepare the fixture: The tutor owns media files 101 and 102 and has no active verification request.
     *   2. Use the input: TUTOR_PROFILE with file IDs 101 and 102.
     *   3. Execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_001_SubmitTutorVerificationStoresDocumentsAndNotifiesAdmin.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert request id/status, two VerificationDocument saves and the admin notification.
     * Expected: A SUBMITTED request and document links are saved for the owner and the admin receives a VERIFICATION_REQUEST notification.
     * Pre-conditions: The tutor owns media files 101 and 102 and has no active verification request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-001: Submit tutor verification documents and notify the platform administrator.")
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

    /**
     * Test Case: IT-VER-002
     * Title: List the admin verification queue by the requested status.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.getVerificationsByStatus (GET /api/identity/verification/status/{status}).
     * Input: Status SUBMITTED.
     * Steps:
     *   1. Prepare the fixture: One SUBMITTED verification and its document query are prepared.
     *   2. Use the input: Status SUBMITTED.
     *   3. Execute VerificationServiceImpl.getVerificationsByStatus (GET /api/identity/verification/status/{status}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_002_ListSubmittedVerificationQueueByStatusForAdmin.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response count/id/status and requireRole(PLATFORM_ADMIN).
     * Expected: Only SUBMITTED requests are returned, in repository order, and the caller must have PLATFORM_ADMIN role.
     * Pre-conditions: One SUBMITTED verification and its document query are prepared.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-002: List the admin verification queue by the requested status.")
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

    /**
     * Test Case: IT-VER-003
     * Title: Allow the owner to read a verification detail together with its documents.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.getVerificationById (GET /api/identity/verification/{verificationId}).
     * Input: verificationId=900.
     * Steps:
     *   1. Prepare the fixture: Verification 900 belongs to the current owner.
     *   2. Use the input: verificationId=900.
     *   3. Execute VerificationServiceImpl.getVerificationById (GET /api/identity/verification/{verificationId}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_003_OwnerCanReadVerificationDetailWithDocuments.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Compare response id, owner and status with the fixture.
     * Expected: The response identifies verification 900, owner 200 and status SUBMITTED; document loading is performed for the same request.
     * Pre-conditions: Verification 900 belongs to the current owner.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-003: Allow the owner to read a verification detail together with its documents.")
    void IT_VER_003_OwnerCanReadVerificationDetailWithDocuments() {
        when(verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(VERIFICATION_ID))
                .thenReturn(List.of());

        VerificationResponse response = verificationService.getVerificationById(VERIFICATION_ID);

        assertEquals(VERIFICATION_ID, response.getVerificationId());
        assertEquals(OWNER_USER_ID, response.getUserId());
        assertEquals(VerificationStatus.SUBMITTED, response.getStatus());
    }

    /**
     * Test Case: IT-VER-004
     * Title: Reject verification submission when no document is supplied.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit).
     * Input: TUTOR_PROFILE with an empty documents list.
     * Steps:
     *   1. Prepare the fixture: The applicant is authenticated and eligible to submit.
     *   2. Use the input: TUTOR_PROFILE with an empty documents list.
     *   3. Execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_004_RejectSubmitVerificationWithoutDocuments.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify both save operations are never called.
     * Expected: The service returns “At least one document is required” and creates neither the request nor document rows.
     * Pre-conditions: The applicant is authenticated and eligible to submit.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-004: Reject verification submission when no document is supplied.")
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

    /**
     * Test Case: IT-VER-005
     * Title: Prevent cancellation of a verification request that is already VERIFIED.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.cancelVerification (DELETE /api/identity/verification/{verificationId}).
     * Input: verificationId=900.
     * Steps:
     *   1. Prepare the fixture: Verification 900 has status VERIFIED and belongs to the current user.
     *   2. Use the input: verificationId=900.
     *   3. Execute VerificationServiceImpl.cancelVerification (DELETE /api/identity/verification/{verificationId}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_005_RejectCancelAlreadyVerifiedRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the business message and verify request deletion is never called.
     * Expected: The service rejects the request because only SUBMITTED requests can be cancelled, leaving the row intact.
     * Pre-conditions: Verification 900 has status VERIFIED and belongs to the current user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-005: Prevent cancellation of a verification request that is already VERIFIED.")
    void IT_VER_005_RejectCancelAlreadyVerifiedRequest() {
        verification.setStatus(VerificationStatus.VERIFIED);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationService.cancelVerification(VERIFICATION_ID));

        assertTrue(exception.getMessage().contains("Chỉ có thể hủy hồ sơ ở trạng thái SUBMITTED"));
        verify(verificationRequestRepository, never()).delete(any());
    }

    /**
     * Test Case: IT-VER-006
     * Title: Block an anonymous verification submission before loading users or files.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit).
     * Input: A normal tutor verification request with file IDs.
     * Steps:
     *   1. Prepare the fixture: No authenticated principal is available.
     *   2. Use the input: A normal tutor verification request with file IDs.
     *   3. Execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_006_BlockAnonymousSubmitBeforeUserAndFileRowsAreLoaded.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify all repository writes/lookups are skipped.
     * Expected: The service returns “Yêu cầu đăng nhập” and performs no user, media-file or verification save lookup.
     * Pre-conditions: No authenticated principal is available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-006: Block an anonymous verification submission before loading users or files.")
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

    /**
     * Test Case: IT-VER-007
     * Title: Prevent a PLATFORM_ADMIN account from submitting a user verification request.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit).
     * Input: TUTOR_PROFILE request with file IDs 101 and 102.
     * Steps:
     *   1. Prepare the fixture: The authenticated principal has PLATFORM_ADMIN role.
     *   2. Use the input: TUTOR_PROFILE request with file IDs 101 and 102.
     *   3. Execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_007_BlockPlatformAdminFromSubmittingUserVerification.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exact role message and verify no save calls.
     * Expected: The request is rejected with the role error and no verification/document row is saved.
     * Pre-conditions: The authenticated principal has PLATFORM_ADMIN role.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-007: Prevent a PLATFORM_ADMIN account from submitting a user verification request.")
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

    /**
     * Test Case: IT-VER-008
     * Title: Prevent a user from cancelling another user’s verification request.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.cancelVerification (DELETE /api/identity/verification/{verificationId}).
     * Input: verificationId=900.
     * Steps:
     *   1. Prepare the fixture: Verification 900 belongs to owner 200; current user is stranger 999.
     *   2. Use the input: verificationId=900.
     *   3. Execute VerificationServiceImpl.cancelVerification (DELETE /api/identity/verification/{verificationId}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_008_RejectCancelVerificationOwnedByAnotherUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify no delete.
     * Expected: The service returns “Bạn không có quyền hủy hồ sơ xác minh này” and keeps the request.
     * Pre-conditions: Verification 900 belongs to owner 200; current user is stranger 999.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-008: Prevent a user from cancelling another user’s verification request.")
    void IT_VER_008_RejectCancelVerificationOwnedByAnotherUser() {
        when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.cancelVerification(VERIFICATION_ID));

        assertEquals("Bạn không có quyền hủy hồ sơ xác minh này", exception.getMessage());
        verify(verificationRequestRepository, never()).delete(any());
    }

    /**
     * Test Case: IT-VER-009
     * Title: Reject a second verification request while an earlier request is still active.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit).
     * Input: Another TUTOR_PROFILE request with files 101 and 102.
     * Steps:
     *   1. Prepare the fixture: The same user/type already has an active submitted or approved request.
     *   2. Use the input: Another TUTOR_PROFILE request with files 101 and 102.
     *   3. Execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_009_RejectDuplicateSubmittedVerificationRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert duplicate business message and verify no document save.
     * Expected: The duplicate submission is rejected and no media/document rows are loaded or saved.
     * Pre-conditions: The same user/type already has an active submitted or approved request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-009: Reject a second verification request while an earlier request is still active.")
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

    /**
     * Test Case: IT-VER-010
     * Title: Move a submitted verification request to UNDER_REVIEW and record the status history.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.startReview (POST /api/identity/verification/{verificationId}/start-review).
     * Input: verificationId=900.
     * Steps:
     *   1. Prepare the fixture: Admin 1 can review verification 900 in SUBMITTED state.
     *   2. Use the input: verificationId=900.
     *   3. Execute VerificationServiceImpl.startReview (POST /api/identity/verification/{verificationId}/start-review). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_010_StartReviewMovesSubmittedRequestToUnderReviewAndWritesHistory.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response/entity status and capture VerificationHistory old/new status.
     * Expected: Status changes SUBMITTED -> UNDER_REVIEW and a history row records both statuses.
     * Pre-conditions: Admin 1 can review verification 900 in SUBMITTED state.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-010: Move a submitted verification request to UNDER_REVIEW and record the status history.")
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

    /**
     * Test Case: IT-VER-011
     * Title: Reject a verification request with a reason and notify the requester.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review).
     * Input: Decision REJECT; note “Ảnh CCCD bị mờ”.
     * Steps:
     *   1. Prepare the fixture: Admin is reviewing a SUBMITTED/UNDER_REVIEW tutor request.
     *   2. Use the input: Decision REJECT; note “Ảnh CCCD bị mờ”.
     *   3. Execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_011_RejectVerificationNotifiesRequesterWithReason.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status/reason and capture notification recipient, type, template and text.
     * Expected: Status becomes REJECTED, rejectionReason is “Ảnh CCCD bị mờ” and the requester receives the rejection notification.
     * Pre-conditions: Admin is reviewing a SUBMITTED/UNDER_REVIEW tutor request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-011: Reject a verification request with a reason and notify the requester.")
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

    /**
     * Test Case: IT-VER-012
     * Title: Keep the owner’s submitted verification request visible after a page reload.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.getMyVerifications (GET /api/identity/verification/my).
     * Input: Authenticated owner request.
     * Steps:
     *   1. Prepare the fixture: User 200 owns verification 903 in SUBMITTED state.
     *   2. Use the input: Authenticated owner request.
     *   3. Execute VerificationServiceImpl.getMyVerifications (GET /api/identity/verification/my). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_012_MyVerificationListKeepsSubmittedRequestForBrowserReturn.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert one response with id 903 and status SUBMITTED.
     * Expected: The owner receives the same SUBMITTED request and its current status after reloading the verification page.
     * Pre-conditions: User 200 owns verification 903 in SUBMITTED state.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-012: Keep the owner’s submitted verification request visible after a page reload.")
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

    /**
     * Test Case: IT-VER-013
     * Title: Prevent a user from reading another user’s verification history.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.getVerificationsByUser (GET /api/identity/verification/user/{userId}).
     * Input: userId=999.
     * Steps:
     *   1. Prepare the fixture: Current user is a tutor; target user is 999.
     *   2. Use the input: userId=999.
     *   3. Execute VerificationServiceImpl.getVerificationsByUser (GET /api/identity/verification/user/{userId}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_013_RejectReadingAnotherUsersVerificationHistory.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException before returning any history.
     * Expected: The service returns the Vietnamese permission error for a non-admin reading another user’s history.
     * Pre-conditions: Current user is a tutor; target user is 999.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-013: Prevent a user from reading another user’s verification history.")
    void IT_VER_013_RejectReadingAnotherUsersVerificationHistory() {
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(owner, UserRole.TUTOR));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> verificationService.getVerificationsByUser(STRANGER_USER_ID));

        assertEquals("Bạn không có quyền xem hồ sơ xác minh của người khác", exception.getMessage());
    }

    /**
     * Test Case: IT-VER-014
     * Title: Reject a document file that is owned by a different user.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit).
     * Input: TUTOR_PROFILE request referencing file 101.
     * Steps:
     *   1. Prepare the fixture: File 101 belongs to stranger 999, while the applicant is user 200.
     *   2. Use the input: TUTOR_PROFILE request referencing file 101.
     *   3. Execute VerificationServiceImpl.submitVerification (POST /api/identity/verification/submit). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_014_RejectVerificationDocumentThatBelongsToAnotherUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ownership error and verify document save is never called.
     * Expected: The request returns “File không thuộc sở hữu của bạn” and no VerificationDocument is saved.
     * Pre-conditions: File 101 belongs to stranger 999, while the applicant is user 200.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-014: Reject a document file that is owned by a different user.")
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

    /**
     * Test Case: IT-VER-015
     * Title: Return one admin response per verification and load each request’s documents.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.getVerificationsByStatus (GET /api/identity/verification/status/{status}).
     * Input: Status SUBMITTED.
     * Steps:
     *   1. Prepare the fixture: Submitted requests 904 and 905 exist.
     *   2. Use the input: Status SUBMITTED.
     *   3. Execute VerificationServiceImpl.getVerificationsByStatus (GET /api/identity/verification/status/{status}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_015_ListSubmittedQueueReturnsOneResponsePerVerificationWithDocumentsLoaded.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response count/order and admin role requirement.
     * Expected: Two submitted requests produce two responses in the same order, with document lookups for both IDs.
     * Pre-conditions: Submitted requests 904 and 905 exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-015: Return one admin response per verification and load each request’s documents.")
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
    }

    /**
     * Test Case: IT-VER-016
     * Title: Synchronize a tutor profile to VERIFIED after admin approval.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review).
     * Input: Decision APPROVE; note “Hồ sơ hợp lệ”.
     * Steps:
     *   1. Prepare the fixture: Admin reviews a valid tutor request for owner 200.
     *   2. Use the input: Decision APPROVE; note “Hồ sơ hợp lệ”.
     *   3. Execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_016_ApproveTutorVerificationSyncsTutorProfileToVerified.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert Tutor.verificationStatus=VERIFIED and verify TutorRepository.save.
     * Expected: An APPROVE decision sets the verification request and the linked tutor profile to VERIFIED and saves the tutor.
     * Pre-conditions: Admin reviews a valid tutor request for owner 200.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-016: Synchronize a tutor profile to VERIFIED after admin approval.")
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

    /**
     * Test Case: IT-VER-017
     * Title: Cancel an owner’s submitted request and remove its documents and history.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.cancelVerification (DELETE /api/identity/verification/{verificationId}).
     * Input: verificationId=900.
     * Steps:
     *   1. Prepare the fixture: The current owner has verification 900 in SUBMITTED state.
     *   2. Use the input: verificationId=900.
     *   3. Execute VerificationServiceImpl.cancelVerification (DELETE /api/identity/verification/{verificationId}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_017_CancelOwnSubmittedVerificationDeletesRequestDocumentsAndHistory.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify all three delete operations target verification 900.
     * Expected: Document rows, history rows and the submitted request are deleted for the same verification id.
     * Pre-conditions: The current owner has verification 900 in SUBMITTED state.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-017: Cancel an owner’s submitted request and remove its documents and history.")
    void IT_VER_017_CancelOwnSubmittedVerificationDeletesRequestDocumentsAndHistory() {
        verificationService.cancelVerification(VERIFICATION_ID);

        verify(verificationDocumentRepository).deleteAllByVerificationId(VERIFICATION_ID);
        verify(verificationHistoryRepository).deleteAllByVerificationId(VERIFICATION_ID);
        verify(verificationRequestRepository).delete(verification);
    }

    /**
     * Test Case: IT-VER-018
     * Title: Preserve the admin queue status filter and submitted-at order.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.getVerificationsByStatus (GET /api/identity/verification/status/{status}).
     * Input: Status SUBMITTED.
     * Steps:
     *   1. Prepare the fixture: Request 906 is older than 907 and both are SUBMITTED.
     *   2. Use the input: Status SUBMITTED.
     *   3. Execute VerificationServiceImpl.getVerificationsByStatus (GET /api/identity/verification/status/{status}). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_018_AdminVerificationQueuePreservesStatusFilterAndSubmittedOrder.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert order/status and verify the VERIFIED query is not used.
     * Expected: SUBMITTED requests 906 then 907 are returned in ascending submitted time; VERIFIED rows are not queried.
     * Pre-conditions: Request 906 is older than 907 and both are SUBMITTED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-018: Preserve the admin queue status filter and submitted-at order.")
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

    /**
     * Test Case: IT-VER-019
     * Title: Reject an admin review decision outside APPROVE or REJECT.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review).
     * Input: Decision HOLD; note “Không phải quyết định hợp lệ”.
     * Steps:
     *   1. Prepare the fixture: Admin can access verification 900.
     *   2. Use the input: Decision HOLD; note “Không phải quyết định hợp lệ”.
     *   3. Execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_019_RejectInvalidReviewDecisionWithClearMessage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify the request is not saved.
     * Expected: Decision HOLD is rejected with the validation message and the verification remains unchanged.
     * Pre-conditions: Admin can access verification 900.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-019: Reject an admin review decision outside APPROVE or REJECT.")
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

    /**
     * Test Case: IT-VER-020
     * Title: Invalidate the previous approved request only after a new verification request is approved.
     * Procedure: Prepare the stated fixture and input, then execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review).
     * Input: Decision APPROVE; note “Chứng từ cập nhật hợp lệ”.
     * Steps:
     *   1. Prepare the fixture: The owner has an older VERIFIED request and a newer submitted request 902.
     *   2. Use the input: Decision APPROVE; note “Chứng từ cập nhật hợp lệ”.
     *   3. Execute VerificationServiceImpl.reviewVerification (POST /api/identity/verification/{verificationId}/review). Mapped test: com.tcs.module.identity.service.impl.Report52VerificationServiceITTest#IT_VER_020_AdminApprovalOfNewRequestRejectsPreviousVerifiedRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert new/old statuses, replacement note, old-request save and notification.
     * Expected: Approving request 902 sets it VERIFIED, changes the previous VERIFIED request to REJECTED, stores the replacement note and notifies the owner.
     * Pre-conditions: The owner has an older VERIFIED request and a newer submitted request 902.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-VER-020: Invalidate the previous approved request only after a new verification request is approved.")
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
