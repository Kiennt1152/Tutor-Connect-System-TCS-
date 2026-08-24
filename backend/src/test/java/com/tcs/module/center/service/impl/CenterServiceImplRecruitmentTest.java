package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.exception.VerificationRequiredException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import com.tcs.module.center.enums.RecruitmentPostStatus;
import com.tcs.module.center.repository.CenterTutorMembershipRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.RescheduleService;
import com.tcs.module.marketplace.service.SubstitutionService;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
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
 * Unit test BF-03 (Tutor Recruitment for Center Workforce) — bám theo bộ test case đã thiết kế
 * trong Report_5.1_UnitTest: các sheet applyToRecruitment, publishRecruitmentPost, decideApplication.
 *
 * <p>Mỗi @Test tương ứng một UTCID trong sheet; tên test ghi rõ UTCID để đối chiếu khi điền kết quả.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CenterServiceImplRecruitmentTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long OTHER_CENTER_USER_ID = 101L;
    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long POST_ID = 300L;
    private static final Long APP_ID = 400L;

    @Mock private AuthHelper authHelper;
    @Mock private RecruitmentPostRepository recruitmentPostRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private CenterTutorMembershipRepository membershipRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private VerificationRequestRepository verificationRequestRepository;
    @Mock private VerificationDocumentRepository verificationDocumentRepository;
    @Mock private CenterEscrowAutoSettlementService centerEscrowAutoSettlementService;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private EscrowService escrowService;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private RescheduleService rescheduleService;
    @Mock private SubstitutionService substitutionService;
    @Mock private AuditLogService auditLogService;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private ContractService contractService;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private CccdService cccdService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks private CenterServiceImpl service;

    private TutorCenter center;
    private Tutor tutor;
    private RecruitmentPost post;

    @BeforeEach
    void setUp() {
        User centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);
        center.setCompanyName("Trung tam 1");
        center.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        post = new RecruitmentPost();
        post.setRecruitmentId(POST_ID);
        post.setCenter(center);
        post.setTitle("Tuyen gia su Toan THCS");
        post.setStatus(RecruitmentPostStatus.ACTIVE);
        post.setMaxPositions(2);
    }

    private void loginAsTutor() {
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(activeWallet()));
    }

    private void loginAsCenter() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
        when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(activeWallet()));
    }

    private Wallet activeWallet() {
        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private ApplyRecruitmentRequest applyBody(String coverLetter) {
        ApplyRecruitmentRequest body = new ApplyRecruitmentRequest();
        body.setCoverLetter(coverLetter);
        return body;
    }

    // ===================================================================
    //  Sheet: applyToRecruitment
    // ===================================================================
    @Nested
    @DisplayName("applyToRecruitment")
    class ApplyToRecruitment {

        @Test
        @DisplayName("UTCID01 (N) - Gia su VERIFIED, tin ACTIVE, chua nop -> luu don APPLIED")
        void utcid01_applySuccessfully() {
            loginAsTutor();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(recruitmentApplicationRepository
                    .findFirstByRecruitmentPost_RecruitmentIdAndTutor_TutorId(POST_ID, TUTOR_ID))
                    .thenReturn(Optional.empty());

            service.applyToRecruitment(POST_ID, applyBody("Em co kinh nghiem day Toan THCS"));

            ArgumentCaptor<RecruitmentApplication> captor =
                    ArgumentCaptor.forClass(RecruitmentApplication.class);
            verify(recruitmentApplicationRepository).save(captor.capture());
            RecruitmentApplication saved = captor.getValue();
            assertEquals(RecruitmentApplicationStatus.APPLIED, saved.getStatus());
            assertEquals(TUTOR_ID, saved.getTutor().getTutorId());
            assertEquals(POST_ID, saved.getRecruitmentPost().getRecruitmentId());
        }

        @Test
        @DisplayName("UTCID02 (B) - coverLetter rong van luu duoc (service khong validate)")
        void utcid02_emptyCoverLetterStillSaved() {
            loginAsTutor();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(recruitmentApplicationRepository
                    .findFirstByRecruitmentPost_RecruitmentIdAndTutor_TutorId(POST_ID, TUTOR_ID))
                    .thenReturn(Optional.empty());

            service.applyToRecruitment(POST_ID, applyBody(""));

            verify(recruitmentApplicationRepository).save(any(RecruitmentApplication.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - Gia su UNDER_VERIFY -> VerificationRequiredException")
        void utcid03_tutorNotVerified() {
            tutor.setVerificationStatus(ProfileVerificationStatus.UNDER_VERIFY);
            loginAsTutor();

            VerificationRequiredException ex = assertThrows(VerificationRequiredException.class,
                    () -> service.applyToRecruitment(POST_ID, applyBody("abc")));
            assertEquals("Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển.", ex.getMessage());
            verify(recruitmentApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - Gia su REJECTED -> VerificationRequiredException")
        void utcid04_tutorRejected() {
            tutor.setVerificationStatus(ProfileVerificationStatus.REJECTED);
            loginAsTutor();

            assertThrows(VerificationRequiredException.class,
                    () -> service.applyToRecruitment(POST_ID, applyBody("abc")));
        }

        @Test
        @DisplayName("UTCID05 (A) - Tai khoan khong phai gia su -> ResourceNotFoundException")
        void utcid05_notATutor() {
            when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.applyToRecruitment(POST_ID, applyBody("abc")));
            assertEquals("Không tìm thấy hồ sơ gia sư", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Tin khong ton tai -> ResourceNotFoundException")
        void utcid06_postNotFound() {
            loginAsTutor();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.applyToRecruitment(POST_ID, applyBody("abc")));
            assertEquals("Không tìm thấy tin tuyển dụng", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Tin DRAFT -> IllegalArgumentException")
        void utcid07_postNotActive() {
            post.setStatus(RecruitmentPostStatus.DRAFT);
            loginAsTutor();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.applyToRecruitment(POST_ID, applyBody("abc")));
            assertEquals("Tin tuyển dụng chưa mở hoặc đã đóng", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Nop lan 2 cho cung tin -> IllegalArgumentException")
        void utcid08_duplicateApplication() {
            loginAsTutor();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(recruitmentApplicationRepository
                    .findFirstByRecruitmentPost_RecruitmentIdAndTutor_TutorId(POST_ID, TUTOR_ID))
                    .thenReturn(Optional.of(new RecruitmentApplication()));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.applyToRecruitment(POST_ID, applyBody("abc")));
            assertEquals("Bạn đã ứng tuyển tin này rồi", ex.getMessage());
            verify(recruitmentApplicationRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: publishRecruitmentPost
    // ===================================================================
    @Nested
    @DisplayName("publishRecruitmentPost")
    class PublishRecruitmentPost {

        @Test
        @DisplayName("UTCID01 (N) - Tin DRAFT cua chinh minh -> ACTIVE + ghi publishedAt")
        void utcid01_publishSuccessfully() {
            post.setStatus(RecruitmentPostStatus.DRAFT);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.empty());
            when(recruitmentPostRepository.save(any(RecruitmentPost.class))).thenAnswer(i -> i.getArgument(0));

            service.publishRecruitmentPost(POST_ID);

            assertEquals(RecruitmentPostStatus.ACTIVE, post.getStatus());
            org.junit.jupiter.api.Assertions.assertNotNull(post.getPublishedAt(),
                    "publishedAt phai duoc ghi vi moc 30 ngay tinh tu truong nay");
            verify(auditLogService).record(anyLong(), anyString(), anyString(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Trung tam chua xac minh -> VerificationRequiredException")
        void utcid03_centerNotVerified() {
            center.setVerificationStatus(ProfileVerificationStatus.UNDER_VERIFY);
            post.setStatus(RecruitmentPostStatus.DRAFT);
            loginAsCenter();

            VerificationRequiredException ex = assertThrows(VerificationRequiredException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
            assertEquals("Trung tâm của bạn cần được xác minh trước khi thực hiện thao tác này.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Dang tin cua trung tam khac -> ForbiddenException")
        void utcid04_notOwner() {
            User otherUser = new User();
            otherUser.setUserId(OTHER_CENTER_USER_ID);
            TutorCenter otherCenter = new TutorCenter();
            otherCenter.setCenterId(2L);
            otherCenter.setUser(otherUser);
            post.setCenter(otherCenter);
            post.setStatus(RecruitmentPostStatus.DRAFT);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
            assertEquals("Bạn không có quyền với tin tuyển dụng này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Tin da ACTIVE, dang lai -> IllegalArgumentException")
        void utcid05_alreadyActive() {
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
            assertEquals("Chỉ tin ở trạng thái nháp mới có thể đăng tải", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Tin da CLOSED -> IllegalArgumentException")
        void utcid06_alreadyClosed() {
            post.setStatus(RecruitmentPostStatus.CLOSED);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
        }

        @Test
        @DisplayName("UTCID07 (A) - Tin khong ton tai -> ResourceNotFoundException")
        void utcid07_postNotFound() {
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
            assertEquals("Không tìm thấy tin tuyển dụng", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: decideApplication
    // ===================================================================
    @Nested
    @DisplayName("decideApplication")
    class DecideApplication {

        private RecruitmentApplication application;

        @BeforeEach
        void initApplication() {
            application = new RecruitmentApplication();
            application.setRecruitmentAppId(APP_ID);
            application.setRecruitmentPost(post);
            application.setTutor(tutor);
            application.setStatus(RecruitmentApplicationStatus.APPLIED);
        }

        @Test
        @DisplayName("UTCID01 (N) - Duyet don APPLIED -> PASSED + sinh hop dong, CHUA la thanh vien")
        void utcid01_approveCreatesContractButNotMember() {
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));
            when(recruitmentApplicationRepository.save(any(RecruitmentApplication.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(classRequestStore.findByRecruitmentPostId(POST_ID)).thenReturn(Optional.empty());

            service.decideApplication(APP_ID, true, 9L, "Noi dung dieu khoan");

            assertEquals(RecruitmentApplicationStatus.PASSED, application.getStatus(),
                    "Duyet chi dat PASSED, KHONG duoc nhay thang HIRED");
            verify(contractService).generateCooperationContract(APP_ID, 9L, "Noi dung dieu khoan");
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID02 (B) - Duyet voi templateId null -> van sinh hop dong")
        void utcid02_approveWithNullTemplate() {
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));
            when(recruitmentApplicationRepository.save(any(RecruitmentApplication.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(classRequestStore.findByRecruitmentPostId(POST_ID)).thenReturn(Optional.empty());

            service.decideApplication(APP_ID, true, null, null);

            verify(contractService).generateCooperationContract(APP_ID, null, null);
        }

        @Test
        @DisplayName("UTCID03 (N) - Tu choi don -> REJECTED, khong sinh hop dong")
        void utcid03_rejectApplication() {
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));
            when(recruitmentApplicationRepository.save(any(RecruitmentApplication.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(classRequestStore.findByRecruitmentPostId(POST_ID)).thenReturn(Optional.empty());

            service.decideApplication(APP_ID, false, null, null);

            assertEquals(RecruitmentApplicationStatus.REJECTED, application.getStatus());
            verify(contractService, never()).generateCooperationContract(anyLong(), any(), any());
        }

        @Test
        @DisplayName("UTCID05 (A) - Don da PASSED -> IllegalArgumentException")
        void utcid05_alreadyProcessed() {
            application.setStatus(RecruitmentApplicationStatus.PASSED);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
            assertEquals("Đơn này đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Don da REJECTED -> IllegalArgumentException")
        void utcid06_alreadyRejected() {
            application.setStatus(RecruitmentApplicationStatus.REJECTED);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            assertThrows(IllegalArgumentException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
        }

        @Test
        @DisplayName("UTCID07 (A) - Don da WITHDRAWN -> IllegalArgumentException")
        void utcid07_alreadyWithdrawn() {
            application.setStatus(RecruitmentApplicationStatus.WITHDRAWN);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            assertThrows(IllegalArgumentException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
        }

        @Test
        @DisplayName("UTCID08 (A) - Don thuoc tin cua trung tam khac -> ForbiddenException")
        void utcid08_notOwner() {
            User otherUser = new User();
            otherUser.setUserId(OTHER_CENTER_USER_ID);
            TutorCenter otherCenter = new TutorCenter();
            otherCenter.setCenterId(2L);
            otherCenter.setUser(otherUser);
            post.setCenter(otherCenter);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
            assertEquals("Bạn không có quyền với tin tuyển dụng này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Don khong ton tai -> ResourceNotFoundException")
        void utcid09_applicationNotFound() {
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
            assertEquals("Không tìm thấy đơn ứng tuyển", ex.getMessage());
        }
    }
}
