package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.entity.ClassAssignment;
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
        @DisplayName("UTCID02 (A) - Trung tam chua xac minh -> VerificationRequiredException")
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
        @DisplayName("UTCID03 (A) - Dang tin cua trung tam khac -> ForbiddenException")
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
        @DisplayName("UTCID04 (A) - Tin da ACTIVE, dang lai -> IllegalArgumentException")
        void utcid05_alreadyActive() {
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
            assertEquals("Chỉ tin ở trạng thái nháp mới có thể đăng tải", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Tin da CLOSED -> IllegalArgumentException")
        void utcid06_alreadyClosed() {
            post.setStatus(RecruitmentPostStatus.CLOSED);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class,
                    () -> service.publishRecruitmentPost(POST_ID));
        }

        @Test
        @DisplayName("UTCID06 (A) - Tin khong ton tai -> ResourceNotFoundException")
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
        @DisplayName("UTCID04 (A) - Don da PASSED -> IllegalArgumentException")
        void utcid05_alreadyProcessed() {
            application.setStatus(RecruitmentApplicationStatus.PASSED);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
            assertEquals("Đơn này đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Don da REJECTED -> IllegalArgumentException")
        void utcid06_alreadyRejected() {
            application.setStatus(RecruitmentApplicationStatus.REJECTED);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            assertThrows(IllegalArgumentException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
        }

        @Test
        @DisplayName("UTCID06 (A) - Don da WITHDRAWN -> IllegalArgumentException")
        void utcid07_alreadyWithdrawn() {
            application.setStatus(RecruitmentApplicationStatus.WITHDRAWN);
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(application));

            assertThrows(IllegalArgumentException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
        }

        @Test
        @DisplayName("UTCID07 (A) - Don thuoc tin cua trung tam khac -> ForbiddenException")
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
        @DisplayName("UTCID08 (A) - Don khong ton tai -> ResourceNotFoundException")
        void utcid09_applicationNotFound() {
            loginAsCenter();
            when(recruitmentApplicationRepository.findById(APP_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.decideApplication(APP_ID, true, null, null));
            assertEquals("Không tìm thấy đơn ứng tuyển", ex.getMessage());
        }
    }
    // ===================================================================
    //  Sheet: updateRecruitmentPost
    // ===================================================================
    @Nested
    @DisplayName("updateRecruitmentPost")
    class UpdateRecruitmentPost {

        private static final Long LINKED_CLASS_ID = 4100L;

        @BeforeEach
        void loginAndGivenDraftPost() {
            loginAsCenter();
            post.setStatus(RecruitmentPostStatus.DRAFT);
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(recruitmentApplicationRepository.countByRecruitmentPost_RecruitmentId(POST_ID))
                    .thenReturn(0L);
            when(recruitmentPostRepository.save(any(RecruitmentPost.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.empty());
        }

        private com.tcs.module.center.dto.request.SaveRecruitmentPostRequest body() {
            var body = new com.tcs.module.center.dto.request.SaveRecruitmentPostRequest();
            body.setTitle("Tuyen gia su Toan THCS");
            body.setDescription("Day Toan lop 8-9, 2 buoi/tuan");
            body.setMaxPositions(2);
            body.setRequiredExperience(1);
            return body;
        }

        @Test
        @DisplayName("UTCID01 (N) - dung chu tin, tin chua dong va chua co don, du lieu hop le -> cap nhat noi dung tin")
        void utcid01_updateSuccessfully() {
            service.updateRecruitmentPost(POST_ID, body());

            assertEquals("Tuyen gia su Toan THCS", post.getTitle());
            assertEquals("Day Toan lop 8-9, 2 buoi/tuan", post.getDescription());
            verify(recruitmentPostRepository).save(post);
        }

        @Test
        @DisplayName("UTCID02 (A) - khong phai chu tin -> ForbiddenException")
        void utcid02_notTheOwner() {
            TutorCenter otherCenter = new TutorCenter();
            otherCenter.setCenterId(999L);
            User otherUser = new User();
            otherUser.setUserId(9999L);
            otherCenter.setUser(otherUser);
            post.setCenter(otherCenter);

            assertThrows(ForbiddenException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body()));
            verify(recruitmentPostRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - tin da dong -> khong the chinh sua")
        void utcid03_postClosed() {
            post.setStatus(RecruitmentPostStatus.CLOSED);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body()));
            assertEquals("Tin đã đóng, không thể chỉnh sửa.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - da co gia su ung tuyen -> khoa chinh sua")
        void utcid04_hasApplications() {
            when(recruitmentApplicationRepository.countByRecruitmentPost_RecruitmentId(POST_ID))
                    .thenReturn(3L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body()));
            assertEquals("Đã có gia sư ứng tuyển, không thể chỉnh sửa tin nữa.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - thieu tieu de -> chan")
        void utcid05_missingTitle() {
            var body = body();
            body.setTitle("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals("Tiêu đề là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - thieu mo ta cong viec -> chan")
        void utcid06_missingDescription() {
            var body = body();
            body.setDescription(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals("Mô tả công việc là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (B) - so luong can tuyen = 0 (ngay duoi nguong duong) -> chan")
        void utcid07_nonPositiveMaxPositions() {
            var body = body();
            body.setMaxPositions(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals("Số lượng cần tuyển phải là số nguyên dương", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - so nam kinh nghiem = -1 (ngay duoi nguong 0) -> chan")
        void utcid08_negativeExperience() {
            var body = body();
            body.setRequiredExperience(-1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals("Số năm kinh nghiệm không được âm", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - co dia chi nhung chua chon Tinh/Thanh pho -> chan")
        void utcid09_addressWithoutProvince() {
            var body = body();
            body.setAddressDetail("So 1 Tran Duy Hung");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals("Vui lòng chọn Tỉnh/Thành phố cho địa chỉ đã nhập", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - lop tu tao (khong phai lop theo yeu cau) -> khong dang tin tuyen")
        void utcid10_selfCreatedClass() {
            var body = body();
            body.setClassId(LINKED_CLASS_ID);
            TutoringClass cls = new TutoringClass();
            cls.setClassId(LINKED_CLASS_ID);
            cls.setCenter(center);
            cls.setCreator(center.getUser());
            when(tutoringClassRepository.findById(LINKED_CLASS_ID)).thenReturn(Optional.of(cls));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals(
                    "Lớp tự tạo chỉ gán gia sư từ danh sách trung tâm, không đăng tin tuyển.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - lop da co gia su chinh -> khong can dang tin tuyen")
        void utcid11_classAlreadyHasTutor() {
            var body = body();
            body.setClassId(LINKED_CLASS_ID);
            TutoringClass cls = new TutoringClass();
            cls.setClassId(LINKED_CLASS_ID);
            cls.setCenter(center);
            cls.setCreator(center.getUser());
            when(tutoringClassRepository.findById(LINKED_CLASS_ID)).thenReturn(Optional.of(cls));

            com.tcs.module.catalog.entity.SystemParameter origin =
                    new com.tcs.module.catalog.entity.SystemParameter();
            origin.setParamValue("EXTERNAL");
            when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.of(origin));
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    eq(LINKED_CLASS_ID), any())).thenReturn(Optional.of(new ClassAssignment()));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateRecruitmentPost(POST_ID, body));
            assertEquals("Lớp đã có gia sư — không cần đăng tin tuyển.", ex.getMessage());
        }
    }
    // ===================================================================
    //  Sheet: cePublishClass / ceDecideReschedule / acceptClassRequest
    // ===================================================================
    @Nested
    @DisplayName("cePublishClass")
    class CePublishClass {

        private static final Long DRAFT_CLASS_ID = 4200L;

        private TutoringClass draftClass;

        @BeforeEach
        void givenDraftSelfCreatedClass() {
            loginAsCenter();
            draftClass = new TutoringClass();
            draftClass.setClassId(DRAFT_CLASS_ID);
            draftClass.setTitle("Toan 9");
            draftClass.setCenter(center);
            draftClass.setCreator(center.getUser());
            draftClass.setStatus(TutoringClassStatus.DRAFT);

            when(tutoringClassRepository.findById(DRAFT_CLASS_ID)).thenReturn(Optional.of(draftClass));
            when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));
            when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.empty());
            when(scheduleSlotRepository.findByTutoringClass_ClassId(DRAFT_CLASS_ID))
                    .thenReturn(java.util.List.of());
        }

        private void givenMainTutor() {
            ClassAssignment assignment = new ClassAssignment();
            assignment.setTutor(tutor);
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    eq(DRAFT_CLASS_ID), any())).thenReturn(Optional.of(assignment));
        }

        @Test
        @DisplayName("UTCID01 (N) - lop nhap va da gan du gia su -> chuyen sang trang thai mo ghi danh")
        void utcid01_publishSuccessfully() {
            givenMainTutor();
            when(substitutionService.findAssistant(DRAFT_CLASS_ID)).thenReturn(Optional.of(77L));

            service.publishClass(DRAFT_CLASS_ID);

            assertEquals(TutoringClassStatus.OPEN, draftClass.getStatus());
            assertNotNull(draftClass.getEnrollmentDeadline());
        }

        @Test
        @DisplayName("UTCID02 (A) - lop khong o trang thai nhap -> chan dang tai")
        void utcid02_notDraft() {
            draftClass.setStatus(TutoringClassStatus.OPEN);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publishClass(DRAFT_CLASS_ID));
            assertEquals("Chỉ lớp ở trạng thái nháp mới có thể đăng tải", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - chua gan gia su chinh -> chan mo ghi danh")
        void utcid03_noMainTutor() {
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    eq(DRAFT_CLASS_ID), any())).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publishClass(DRAFT_CLASS_ID));
            assertEquals("Cần gán gia sư chính cho lớp trước khi mở ghi danh (đăng tải).",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - moi gan 1/2 gia su (thieu gia su phu) -> chan mo ghi danh")
        void utcid04_missingAssistantTutor() {
            givenMainTutor();
            when(substitutionService.findAssistant(DRAFT_CLASS_ID)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publishClass(DRAFT_CLASS_ID));
            assertEquals("Cần gán đủ 2 gia sư (gia sư chính + gia sư phụ) trước khi mở ghi danh.",
                    ex.getMessage());
        }
    }

    @Nested
    @DisplayName("ceDecideReschedule")
    class CeDecideReschedule {

        private static final Long RESCHEDULE_CLASS_ID = 4300L;

        @BeforeEach
        void loginAndGivenClass() {
            loginAsCenter();
            TutoringClass cls = new TutoringClass();
            cls.setClassId(RESCHEDULE_CLASS_ID);
            cls.setTitle("Toan 9");
            cls.setCenter(center);
            cls.setCreator(center.getUser());
            when(tutoringClassRepository.findById(RESCHEDULE_CLASS_ID)).thenReturn(Optional.of(cls));
        }

        private com.tcs.module.center.dto.request.RescheduleDecisionBody body(
                Long classId, java.time.LocalDate originalDate) {
            var b = new com.tcs.module.center.dto.request.RescheduleDecisionBody();
            b.setClassId(classId);
            b.setOriginalDate(originalDate);
            b.setApprove(true);
            return b;
        }

        @Test
        @DisplayName("UTCID01 (N) - trung tam so huu lop duyet yeu cau doi lich -> luu quyet dinh")
        void utcid01_decideSuccessfully() {
            java.time.LocalDate original = java.time.LocalDate.now().plusDays(2);
            var entry = new com.tcs.module.marketplace.dto.RescheduleEntry(
                    RESCHEDULE_CLASS_ID, original, original.plusDays(1), null, null,
                    com.tcs.module.marketplace.dto.RescheduleEntry.APPROVED, null, "Doi lich");
            when(rescheduleService.decide(RESCHEDULE_CLASS_ID, original, true)).thenReturn(entry);

            service.decideReschedule(body(RESCHEDULE_CLASS_ID, original));

            verify(rescheduleService).decide(RESCHEDULE_CLASS_ID, original, true);
            verify(auditLogService).record(
                    any(), eq("DECIDE_RESCHEDULE"), eq("TutoringClass"), eq(RESCHEDULE_CLASS_ID),
                    any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - thieu classId hoac originalDate -> chan")
        void utcid02_missingPayload() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.decideReschedule(body(null, java.time.LocalDate.now())));
            assertEquals("Thiếu thông tin yêu cầu dời lịch", ex.getMessage());

            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                    () -> service.decideReschedule(body(RESCHEDULE_CLASS_ID, null)));
            assertEquals("Thiếu thông tin yêu cầu dời lịch", ex2.getMessage());
        }
    }
}
