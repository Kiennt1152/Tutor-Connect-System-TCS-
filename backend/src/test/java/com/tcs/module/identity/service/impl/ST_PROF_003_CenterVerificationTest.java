package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
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
import com.tcs.module.profile.entity.TutorCenter;
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
 * System Test: ST-PROF-003
 * Title: Center submits a business license and is approved.
 *
 * Steps:
 * 1. Center logs in, submits 5 required documents (business license, education permit,
 *    tax certificate, representative ID front/back).
 * 2. Submit the verification request.
 * 3. Admin opens the queue, reviews documents, clicks Approve.
 * 4. Center checks status.
 *
 * Expected:
 * verification_status becomes VERIFIED; center-gated features open (create class, recruit tutors);
 * Center is notified of the result.
 */
@Tag("system-test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_PROF_003_CenterVerificationTest {

    private static final Long CENTER_USER_ID = 30L; // center01@tcs.test
    private static final Long ADMIN_USER_ID = 27L;  // admin01@tcs.test
    private static final Long VERIFICATION_ID = 920L;

    private static final Long FILE_BIZ_LICENSE = 201L;
    private static final Long FILE_EDU_PERMIT = 202L;
    private static final Long FILE_TAX_CODE = 203L;
    private static final Long FILE_REP_ID_FRONT = 204L;
    private static final Long FILE_REP_ID_BACK = 205L;

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

    private User centerUser;
    private User adminUser;
    private TutorCenter centerProfile;
    private PlatformAdmin adminProfile;

    @BeforeEach
    void setUp() {
        centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        centerUser.setEmail("center01@tcs.test");

        adminUser = new User();
        adminUser.setUserId(ADMIN_USER_ID);
        adminUser.setEmail("admin01@tcs.test");

        centerProfile = new TutorCenter();
        centerProfile.setUser(centerUser);
        centerProfile.setCompanyName("Trung Tam Gia Su TCS");
        centerProfile.setVerificationStatus(ProfileVerificationStatus.UNDER_VERIFY);

        adminProfile = new PlatformAdmin();
        adminProfile.setUser(adminUser);

        when(userRepository.findById(CENTER_USER_ID)).thenReturn(Optional.of(centerUser));
        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(centerProfile));
        when(tutorRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.empty());
        when(platformAdminRepository.findAll()).thenReturn(List.of(adminProfile));

        // Mock the 5 media files owned by the center user
        setupMediaFile(FILE_BIZ_LICENSE, "business_license.pdf");
        setupMediaFile(FILE_EDU_PERMIT, "education_permit.pdf");
        setupMediaFile(FILE_TAX_CODE, "tax_code.pdf");
        setupMediaFile(FILE_REP_ID_FRONT, "rep_id_front.jpg");
        setupMediaFile(FILE_REP_ID_BACK, "rep_id_back.jpg");

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

    private void setupMediaFile(Long fileId, String fileName) {
        MediaFile mf = new MediaFile();
        mf.setFileId(fileId);
        mf.setFileName(fileName);
        mf.setUploadedBy(centerUser);
        when(mediaFileRepository.findById(fileId)).thenReturn(Optional.of(mf));
    }

    private VerificationRequestDto createValidCenterRequest() {
        VerificationRequestDto request = new VerificationRequestDto();
        request.setVerificationType(VerificationType.TUTOR_CENTER_LICENSE);

        VerificationRequestDto.DocumentUpload doc1 = new VerificationRequestDto.DocumentUpload();
        doc1.setDocumentType(VerificationDocumentType.LICENSE); // Giấy ĐKKD
        doc1.setFileId(FILE_BIZ_LICENSE);

        VerificationRequestDto.DocumentUpload doc2 = new VerificationRequestDto.DocumentUpload();
        doc2.setDocumentType(VerificationDocumentType.LICENSE); // Giấy phép GD
        doc2.setFileId(FILE_EDU_PERMIT);

        VerificationRequestDto.DocumentUpload doc3 = new VerificationRequestDto.DocumentUpload();
        doc3.setDocumentType(VerificationDocumentType.CERTIFICATE); // MST
        doc3.setFileId(FILE_TAX_CODE);

        VerificationRequestDto.DocumentUpload doc4 = new VerificationRequestDto.DocumentUpload();
        doc4.setDocumentType(VerificationDocumentType.ID_CARD); // CCCD mặt trước
        doc4.setFileId(FILE_REP_ID_FRONT);

        VerificationRequestDto.DocumentUpload doc5 = new VerificationRequestDto.DocumentUpload();
        doc5.setDocumentType(VerificationDocumentType.DEGREE); // CCCD mặt sau
        doc5.setFileId(FILE_REP_ID_BACK);

        request.setDocuments(List.of(doc1, doc2, doc3, doc4, doc5));
        return request;
    }

    @Test
    @DisplayName("ST-PROF-003: Center submits 5 required documents, Admin approves -> Center becomes VERIFIED")
    void testCenterSubmit5DocumentsAndAdminApprove_Success() {
        // --- Step 1: Center logs in & submits valid 5 documents ---
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(centerUser, UserRole.TUTOR_CENTER));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(CENTER_USER_ID), eq(VerificationType.TUTOR_CENTER_LICENSE), any()))
                .thenReturn(false);

        VerificationRequest[] createdHolder = new VerificationRequest[1];
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(inv -> {
            VerificationRequest req = inv.getArgument(0);
            if (req.getVerificationId() == null) {
                req.setVerificationId(VERIFICATION_ID);
            }
            createdHolder[0] = req;
            return req;
        });
        when(verificationRequestRepository.findById(VERIFICATION_ID))
                .thenAnswer(inv -> Optional.of(createdHolder[0]));

        VerificationResponse response = verificationService.submitVerification(createValidCenterRequest());

        assertNotNull(response);
        assertEquals(VERIFICATION_ID, response.getVerificationId());
        assertEquals(VerificationStatus.SUBMITTED, response.getStatus());
        assertEquals(VerificationType.TUTOR_CENTER_LICENSE, response.getVerificationType());
        // Verify exactly 5 documents saved
        verify(verificationDocumentRepository, times(5)).save(any(VerificationDocument.class));
        verify(notificationDispatchService).notifyUser(
                eq(adminUser),
                eq(NotificationType.VERIFICATION),
                eq("Có hồ sơ xác minh mới"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(VERIFICATION_ID));

        // --- Step 2: Admin reviews and approves center license ---
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(CENTER_USER_ID))
                .thenReturn(List.of(createdHolder[0]));

        VerificationDecisionDto decision = new VerificationDecisionDto();
        decision.setDecision("APPROVE");
        decision.setNote("Giấy phép kinh doanh và giấy phép giáo dục hợp lệ.");

        VerificationResponse approveResponse = verificationService.reviewVerification(VERIFICATION_ID, decision);

        assertEquals(VerificationStatus.VERIFIED, approveResponse.getStatus());
        // Assert that TutorCenter entity verificationStatus is synced to VERIFIED!
        assertEquals(ProfileVerificationStatus.VERIFIED, centerProfile.getVerificationStatus());
        verify(tutorCenterRepository).save(centerProfile);

        // Verify Center is notified of the result
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(centerUser),
                eq(NotificationType.VERIFICATION),
                eq("VERIFICATION_APPROVED"),
                any(),
                eq("Hồ sơ xác minh được duyệt"),
                any(),
                eq("VERIFICATION_REQUEST"),
                eq(VERIFICATION_ID));
    }

    @Test
    @DisplayName("ST-PROF-003 Negative: Center submits only 4 documents -> Rejected with 5-doc requirement message")
    void testCenterSubmitMissingDocuments_ThrowsBusinessException() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(centerUser, UserRole.TUTOR_CENTER));
        when(verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                eq(CENTER_USER_ID), eq(VerificationType.TUTOR_CENTER_LICENSE), any()))
                .thenReturn(false);

        VerificationRequestDto invalidRequest = createValidCenterRequest();
        // Remove the 5th document (only 4 docs provided)
        invalidRequest.setDocuments(invalidRequest.getDocuments().subList(0, 4));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> verificationService.submitVerification(invalidRequest));

        assertTrue(ex.getMessage().contains("Hồ sơ trung tâm cần đủ 5 chứng từ bắt buộc"));
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("ST-PROF-003 Negative: Center role attempts to submit TUTOR_PROFILE type -> Rejected")
    void testCenterSubmitWrongVerificationType_ThrowsBusinessException() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(centerUser, UserRole.TUTOR_CENTER));

        VerificationRequestDto wrongTypeRequest = new VerificationRequestDto();
        wrongTypeRequest.setVerificationType(VerificationType.TUTOR_PROFILE);
        wrongTypeRequest.setDocuments(List.of(new VerificationRequestDto.DocumentUpload()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> verificationService.submitVerification(wrongTypeRequest));

        assertEquals("Tài khoản trung tâm chỉ được nộp hồ sơ loại TUTOR_CENTER_LICENSE", ex.getMessage());
        verify(verificationRequestRepository, never()).save(any());
    }
}
