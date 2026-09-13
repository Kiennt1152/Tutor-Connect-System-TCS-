package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.VerificationRequiredException;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
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
 * System Test: ST-PROF-005
 * Title: Unverified tutor tries to apply to a class.
 *
 * Steps:
 * 1. Tutor with unverified status (UNDER_VERIFY, REJECTED, UNVERIFIED) browses marketplace.
 * 2. Tries to apply to an OPEN class.
 *
 * Expected:
 * System blocks action with VerificationRequiredException (error code VERIFICATION_REQUIRED),
 * preventing application submission until verification is complete.
 */
@Tag("system-test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_PROF_005_MarketplaceTrustGateTest {

    private static final Long CLASS_ID = 501L;
    private static final Long TUTOR_USER_ID = 29L; // tutor02@tcs.test
    private static final Long TUTOR_ID = 44L;

    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuthHelper authHelper;
    @Mock private TutorRepository tutorRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private User tutorUser;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor02@tcs.test");

        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư TCS 02");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
    }

    @Test
    @DisplayName("ST-PROF-005: Tutor with UNDER_VERIFY status applying to class -> Blocked by VerificationRequiredException")
    void testUnderVerifyTutor_BlockedFromApplying() {
        tutor.setVerificationStatus(ProfileVerificationStatus.UNDER_VERIFY);

        VerificationRequiredException ex = assertThrows(
                VerificationRequiredException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, new ApplyClassRequest()));

        assertEquals("Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp.", ex.getMessage());
        assertEquals("VERIFICATION_REQUIRED", VerificationRequiredException.CODE);
        verify(authHelper).requireRole(UserRole.TUTOR);
        verify(tutoringClassRepository, never()).findById(any());
        verify(tutorApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("ST-PROF-005: Tutor with REJECTED status applying to class -> Blocked by VerificationRequiredException")
    void testRejectedTutor_BlockedFromApplying() {
        tutor.setVerificationStatus(ProfileVerificationStatus.REJECTED);

        VerificationRequiredException ex = assertThrows(
                VerificationRequiredException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, new ApplyClassRequest()));

        assertEquals("Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp.", ex.getMessage());
        verify(tutorApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("ST-PROF-005: Tutor with null verification status applying to class -> Blocked by VerificationRequiredException")
    void testNullVerificationStatusTutor_BlockedFromApplying() {
        tutor.setVerificationStatus(null);

        VerificationRequiredException ex = assertThrows(
                VerificationRequiredException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, new ApplyClassRequest()));

        assertEquals("Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp.", ex.getMessage());
        verify(tutorApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("ST-PROF-005 Positive: Tutor with VERIFIED status -> Passes trust gate check")
    void testVerifiedTutor_PassesVerificationCheck() {
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        // Wallet active
        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(wallet));

        // Open class
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        tutoringClass.setTitle("Lớp Toán 10");
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, TUTOR_ID))
                .thenReturn(Optional.empty());
        when(tutorApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ApplyClassRequest request = new ApplyClassRequest();
        request.setProposedRate(new BigDecimal("150000.00"));
        request.setCoverLetter("Em có kinh nghiệm dạy Toán cấp 3");

        // Should not throw VerificationRequiredException
        marketplaceService.applyToClass(CLASS_ID, request);

        // Verify application was saved
        verify(tutorApplicationRepository).save(any());
    }
}
