package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
 * System Test: ST-PROF-001
 * Title: Tutor submits documents and Admin approves.
 *
 * Steps:
 * 1. Tutor logs in, submits degree + national ID.
 * 2. Submit the verification request.
 * 3. Admin opens the queue, reviews documents, clicks Approve.
 * 4. Tutor checks status.
 *
 * Expected:
 * verification_status becomes VERIFIED; trust-gated features open; Tutor is notified of the result.
 */
@Tag("system-test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_PROF_001_TutorVerificationTest {

    private static final Long TUTOR_USER_ID = 29L; // tutor02@tcs.test
    private static final Long ADMIN_USER_ID = 27L; // admin01@tcs.test
    private static final Long VERIFICATION_ID = 901L;
    private static final Long FILE_ID_CARD = 101L; // id.jpg
    private static final Long FILE_DEGREE = 102L;  // degree.pdf

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
    private MediaFile degreeFile;
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

        degreeFile = new MediaFile();
        degreeFile.setFileId(FILE_DEGREE);
        degreeFile.setFileName("degree.pdf");
        degreeFile.setUploadedBy(tutorUser);

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutorProfile));
        when(tutorCenterRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(mediaFileRepository.findById(FILE_ID_CARD)).thenReturn(Optional.of(idFile));
        when(mediaFileRepository.findById(FILE_DEGREE)).thenReturn(Optional.of(degreeFile));
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
                            .build();
                });
    }

    @Test
    @DisplayName("ST-PROF-001: Full flow - Tutor submits ID + Degree, Admin approves, Profile synced to VERIFIED")
    void testTutorSubmitAndAdminApprove_FullFlow() {
        // --- Step 1 & 2: Tutor logs in and submits verification request ---
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(TUTOR_USER_ID), eq(VerificationType.TUTOR_PROFILE), any()))
                .thenReturn(false);

        VerificationRequest[] createdHolder = new VerificationRequest[1];
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> {
            VerificationRequest req = invocation.getArgument(0);
            if (req.getVerificationId() == null) {
                req.setVerificationId(VERIFICATION_ID);
            }
            createdHolder[0] = req;
            return req;
        });
        when(verificationRequestRepository.findById(VERIFICATION_ID))
                .thenAnswer(inv -> Optional.of(createdHolder[0]));

        VerificationRequestDto submitDto = new VerificationRequestDto();
        submitDto.setVerificationType(VerificationType.TUTOR_PROFILE);
        VerificationRequestDto.DocumentUpload doc1 = new VerificationRequestDto.DocumentUpload();
        doc1.setDocumentType(VerificationDocumentType.ID_CARD);
        doc1.setFileId(FILE_ID_CARD);
        VerificationRequestDto.DocumentUpload doc2 = new VerificationRequestDto.DocumentUpload();
        doc2.setDocumentType(VerificationDocumentType.DEGREE);
        doc2.setFileId(FILE_DEGREE);
        submitDto.setDocuments(List.of(doc1, doc2));

        VerificationResponse submitResponse = verificationService.submitVerification(submitDto);

        assertNotNull(submitResponse);
        assertEquals(VERIFICATION_ID, submitResponse.getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, submitResponse.getStatus());
        verify(verificationDocumentRepository, times(2)).save(any(VerificationDocument.class));
        verify(notificationDispatchService).notifyUser(
                eq(adminUser),
                eq(NotificationType.VERIFICATION),
                eq("Có hồ sơ xác minh mới"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(VERIFICATION_ID));

        // --- Step 3: Admin reviews and approves ---
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(TUTOR_USER_ID))
                .thenReturn(List.of(createdHolder[0]));

        VerificationDecisionDto approveDecision = new VerificationDecisionDto();
        approveDecision.setDecision("APPROVE");
        approveDecision.setNote("Documents are clear and valid.");

        VerificationResponse approveResponse = verificationService.reviewVerification(VERIFICATION_ID, approveDecision);

        assertEquals(VerificationStatus.VERIFIED, approveResponse.getStatus());
        assertEquals(ProfileVerificationStatus.VERIFIED, tutorProfile.getVerificationStatus());
        verify(tutorRepository).save(tutorProfile);
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutorUser),
                eq(NotificationType.VERIFICATION),
                eq("VERIFICATION_APPROVED"),
                any(),
                eq("Hồ sơ xác minh được duyệt"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(VERIFICATION_ID));

        // --- Step 4: Tutor checks status ---
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(TUTOR_USER_ID))
                .thenReturn(List.of(createdHolder[0]));

        List<VerificationResponse> myRequests = verificationService.getMyVerifications();
        assertEquals(1, myRequests.size());
        assertEquals(VerificationStatus.VERIFIED, myRequests.get(0).getStatus());
    }
}
