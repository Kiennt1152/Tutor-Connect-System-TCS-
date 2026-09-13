package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationDocument;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * System Test: ST-PROF-002
 * Title: Admin rejects documents, tutor resubmits.
 *
 * Steps:
 * 1. Admin opens the request, clicks Reject, writes a reason.
 * 2. Tutor receives the reason.
 * 3. Tutor adds documents and resubmits.
 * 4. Admin approves the new version.
 *
 * Expected:
 * Request REJECTED with a reason; tutor can resubmit; when the new version is approved, the old approval is superseded.
 */
@Tag("system-test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_PROF_002_ResubmitVerificationTest {

    private static final Long TUTOR_USER_ID = 29L; // tutor02@tcs.test
    private static final Long ADMIN_USER_ID = 27L; // admin01@tcs.test
    private static final Long FIRST_VERIFICATION_ID = 910L;
    private static final Long NEW_VERIFICATION_ID = 911L;
    private static final Long FILE_ID_CARD = 101L;
    private static final Long FILE_BLURRY_DEGREE = 102L;
    private static final Long FILE_CLEAR_DEGREE = 103L;
    private static final String REJECTION_REASON = "Blurry degree, resubmit a clear copy";

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

    private User tutorUser;
    private User adminUser;
    private Tutor tutorProfile;
    private MediaFile idFile;
    private MediaFile blurryDegreeFile;
    private MediaFile clearDegreeFile;
    private PlatformAdmin adminProfile;

    @BeforeEach
    void setUp() {
        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor02@tcs.test");

        adminUser = new User();
        adminUser.setUserId(ADMIN_USER_ID);
        adminUser.setEmail("admin01@tcs.test");

        tutorProfile = new Tutor();
        tutorProfile.setUser(tutorUser);
        tutorProfile.setVerificationStatus(ProfileVerificationStatus.UNDER_VERIFY);

        adminProfile = new PlatformAdmin();
        adminProfile.setUser(adminUser);

        idFile = new MediaFile();
        idFile.setFileId(FILE_ID_CARD);
        idFile.setFileName("id.jpg");
        idFile.setUploadedBy(tutorUser);

        blurryDegreeFile = new MediaFile();
        blurryDegreeFile.setFileId(FILE_BLURRY_DEGREE);
        blurryDegreeFile.setFileName("blurry_degree.pdf");
        blurryDegreeFile.setUploadedBy(tutorUser);

        clearDegreeFile = new MediaFile();
        clearDegreeFile.setFileId(FILE_CLEAR_DEGREE);
        clearDegreeFile.setFileName("clear_degree.pdf");
        clearDegreeFile.setUploadedBy(tutorUser);

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutorProfile));
        when(tutorCenterRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(mediaFileRepository.findById(FILE_ID_CARD)).thenReturn(Optional.of(idFile));
        when(mediaFileRepository.findById(FILE_BLURRY_DEGREE)).thenReturn(Optional.of(blurryDegreeFile));
        when(mediaFileRepository.findById(FILE_CLEAR_DEGREE)).thenReturn(Optional.of(clearDegreeFile));
        when(platformAdminRepository.findAll()).thenReturn(List.of(adminProfile));

        when(verificationMapper.toResponse(any(VerificationRequest.class), any()))
                .thenAnswer(invocation -> {
                    VerificationRequest source = invocation.getArgument(0);
                    return VerificationResponse.builder()
                            .verificationId(source.getVerificationId())
                            .userId(source.getUser().getUserId())
                            .userEmail(source.getUser().getEmail())
                            .verificationType(source.getVerificationType())
                            .status(source.getStatus())
                            .rejectionReason(source.getRejectionReason())
                            .adminNotes(source.getAdminNotes())
                            .resubmittable(source.getStatus() == VerificationStatus.REJECTED
                                    || source.getStatus() == VerificationStatus.VERIFIED)
                            .build();
                });
    }

    @Test
    @DisplayName("ST-PROF-002: Admin rejects documents with reason -> Tutor resubmits -> Admin approves new version")
    void testAdminRejects_TutorResubmits_AdminApprovesNewVersion() {
        // --- Step 1: Initial submitted request exists, Admin opens and rejects with reason ---
        VerificationRequest firstRequest = new VerificationRequest();
        firstRequest.setVerificationId(FIRST_VERIFICATION_ID);
        firstRequest.setUser(tutorUser);
        firstRequest.setVerificationType(VerificationType.TUTOR_PROFILE);
        firstRequest.setStatus(VerificationStatus.SUBMITTED);

        when(verificationRequestRepository.findById(FIRST_VERIFICATION_ID)).thenReturn(Optional.of(firstRequest));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));

        VerificationDecisionDto rejectDecision = new VerificationDecisionDto();
        rejectDecision.setDecision("REJECT");
        rejectDecision.setNote(REJECTION_REASON);

        VerificationResponse rejectResponse = verificationService.reviewVerification(FIRST_VERIFICATION_ID, rejectDecision);

        assertEquals(VerificationStatus.REJECTED, rejectResponse.getStatus());
        assertEquals(REJECTION_REASON, firstRequest.getRejectionReason());
        assertEquals(REJECTION_REASON, firstRequest.getAdminNotes());
        assertEquals(ProfileVerificationStatus.REJECTED, tutorProfile.getVerificationStatus());

        // Verify rejection notification sent to tutor with exact reason
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutorUser),
                eq(NotificationType.VERIFICATION),
                eq("VERIFICATION_REJECTED"),
                any(),
                eq("Hồ sơ xác minh bị từ chối"),
                eq("Lý do: " + REJECTION_REASON + ". Bạn có thể nộp lại sau khi bổ sung giấy tờ."),
                eq("VERIFICATION_REQUEST"),
                eq(FIRST_VERIFICATION_ID));

        // --- Step 2: Tutor checks rejection reason ---
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(TUTOR_USER_ID))
                .thenReturn(List.of(firstRequest));

        List<VerificationResponse> myRequests = verificationService.getMyVerifications();
        assertEquals(1, myRequests.size());
        assertEquals(VerificationStatus.REJECTED, myRequests.get(0).getStatus());
        assertTrue(myRequests.get(0).isResubmittable());

        // --- Step 3: Tutor can resubmit and submits new clear copy ---
        // canResubmit check should pass because no SUBMITTED or UNDER_REVIEW exists
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(TUTOR_USER_ID), eq(VerificationType.TUTOR_PROFILE), any()))
                .thenReturn(false);

        VerificationRequest newRequest = new VerificationRequest();
        newRequest.setVerificationId(NEW_VERIFICATION_ID);
        newRequest.setUser(tutorUser);
        newRequest.setVerificationType(VerificationType.TUTOR_PROFILE);
        newRequest.setStatus(VerificationStatus.SUBMITTED);

        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(inv -> {
            VerificationRequest saved = inv.getArgument(0);
            if (saved.getVerificationId() == null) {
                saved.setVerificationId(NEW_VERIFICATION_ID);
            }
            return saved;
        });
        when(verificationRequestRepository.findById(NEW_VERIFICATION_ID)).thenReturn(Optional.of(newRequest));

        VerificationRequestDto resubmitDto = new VerificationRequestDto();
        resubmitDto.setVerificationType(VerificationType.TUTOR_PROFILE);
        VerificationRequestDto.DocumentUpload doc1 = new VerificationRequestDto.DocumentUpload();
        doc1.setDocumentType(VerificationDocumentType.ID_CARD);
        doc1.setFileId(FILE_ID_CARD);
        VerificationRequestDto.DocumentUpload doc2 = new VerificationRequestDto.DocumentUpload();
        doc2.setDocumentType(VerificationDocumentType.DEGREE);
        doc2.setFileId(FILE_CLEAR_DEGREE); // new clear copy!
        resubmitDto.setDocuments(List.of(doc1, doc2));

        VerificationResponse resubmitResponse = verificationService.submitVerification(resubmitDto);
        assertNotNull(resubmitResponse);
        assertEquals(NEW_VERIFICATION_ID, resubmitResponse.getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, resubmitResponse.getStatus());

        // --- Step 4: Admin approves the new version ---
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(TUTOR_USER_ID))
                .thenReturn(List.of(newRequest, firstRequest));

        VerificationDecisionDto approveDecision = new VerificationDecisionDto();
        approveDecision.setDecision("APPROVE");
        approveDecision.setNote("Clear copy verified successfully.");

        VerificationResponse approveResponse = verificationService.reviewVerification(NEW_VERIFICATION_ID, approveDecision);

        assertEquals(VerificationStatus.VERIFIED, approveResponse.getStatus());
        assertEquals(ProfileVerificationStatus.VERIFIED, tutorProfile.getVerificationStatus());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutorUser),
                eq(NotificationType.VERIFICATION),
                eq("VERIFICATION_APPROVED"),
                any(),
                eq("Hồ sơ xác minh được duyệt"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(NEW_VERIFICATION_ID));
    }
}
