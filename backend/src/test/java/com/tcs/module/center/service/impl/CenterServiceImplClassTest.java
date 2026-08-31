package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.entity.CenterTutorMembership;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import com.tcs.module.center.enums.RecruitmentPostStatus;
import com.tcs.module.center.repository.CenterTutorMembershipRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
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
 * Unit test BF-04 (Center Curriculum Class Lifecycle) + phần quản lý tin/thành viên còn lại của BF-03.
 * Bám theo bộ test case trong Report_5.1_UnitTest: các sheet closeRecruitmentPost,
 * updateMembershipStatus, publishClass, closeEnrollment, assignTutor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CenterServiceImplClassTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long OTHER_CENTER_USER_ID = 101L;
    private static final Long CLASS_ID = 500L;
    private static final Long POST_ID = 300L;
    private static final Long TUTOR_ID = 20L;
    private static final Long MEMBERSHIP_ID = 600L;

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
    private TutoringClass tutoringClass;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        User centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);
        center.setCompanyName("Trung tam 1");
        center.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(centerUser);
        tutoringClass.setTitle("Toan 9");
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        tutoringClass.setMinStudents(2);

        User tutorUser = new User();
        tutorUser.setUserId(200L);
        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
    }

    private void loginAsCenter() {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
    }

    private void useOtherCenterOwner() {
        User other = new User();
        other.setUserId(OTHER_CENTER_USER_ID);
        tutoringClass.setCreator(other);
    }

    // ===================================================================
    //  Sheet: closeRecruitmentPost
    // ===================================================================
    @Nested
    @DisplayName("closeRecruitmentPost")
    class CloseRecruitmentPost {

        private RecruitmentPost post;

        @BeforeEach
        void initPost() {
            post = new RecruitmentPost();
            post.setRecruitmentId(POST_ID);
            post.setCenter(center);
            post.setTitle("Tuyen gia su");
            post.setStatus(RecruitmentPostStatus.ACTIVE);
        }

        @Test
        @DisplayName("UTCID01 (N) - Tin ACTIVE -> CLOSED + ghi closedAt")
        void utcid01_closeActivePost() {
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(recruitmentPostRepository.save(any(RecruitmentPost.class))).thenAnswer(i -> i.getArgument(0));

            service.closeRecruitmentPost(POST_ID);

            assertEquals(RecruitmentPostStatus.CLOSED, post.getStatus());
            assertNotNull(post.getClosedAt());
        }

        @Test
        @DisplayName("UTCID02 (A) - Tin DRAFT -> IllegalArgumentException")
        void utcid02_closeDraftPost() {
            post.setStatus(RecruitmentPostStatus.DRAFT);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.closeRecruitmentPost(POST_ID));
            assertEquals("Chỉ tin đang mở mới có thể đóng", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Tin da CLOSED, dong lai -> IllegalArgumentException")
        void utcid03_closeAlreadyClosed() {
            post.setStatus(RecruitmentPostStatus.CLOSED);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class, () -> service.closeRecruitmentPost(POST_ID));
        }

        @Test
        @DisplayName("UTCID04 (A) - Tin cua trung tam khac -> ForbiddenException")
        void utcid04_notOwner() {
            User other = new User();
            other.setUserId(OTHER_CENTER_USER_ID);
            TutorCenter otherCenter = new TutorCenter();
            otherCenter.setCenterId(2L);
            otherCenter.setUser(other);
            post.setCenter(otherCenter);
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            assertThrows(ForbiddenException.class, () -> service.closeRecruitmentPost(POST_ID));
        }

        @Test
        @DisplayName("UTCID05 (A) - Tin khong ton tai -> ResourceNotFoundException")
        void utcid05_postNotFound() {
            loginAsCenter();
            when(recruitmentPostRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.closeRecruitmentPost(POST_ID));
        }
    }

    // ===================================================================
    //  Sheet: updateMembershipStatus
    // ===================================================================
    @Nested
    @DisplayName("updateMembershipStatus")
    class UpdateMembershipStatus {

        private CenterTutorMembership membership;

        @BeforeEach
        void initMembership() {
            membership = new CenterTutorMembership();
            membership.setMembershipId(MEMBERSHIP_ID);
            membership.setCenter(center);
            membership.setTutor(tutor);
            membership.setStatus(CenterTutorMembershipStatus.ACTIVE);
        }

        @Test
        @DisplayName("UTCID01 (N) - Doi ACTIVE -> INACTIVE thanh cong")
        void utcid01_deactivateMember() {
            loginAsCenter();
            when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
            when(membershipRepository.save(any(CenterTutorMembership.class))).thenAnswer(i -> i.getArgument(0));
            when(recruitmentApplicationRepository
                    .findByTutor_TutorIdAndRecruitmentPost_Center_CenterIdOrderByAppliedAtDesc(anyLong(), anyLong()))
                    .thenReturn(List.of());

            service.updateMembershipStatus(MEMBERSHIP_ID, CenterTutorMembershipStatus.INACTIVE);

            assertEquals(CenterTutorMembershipStatus.INACTIVE, membership.getStatus());
        }

        @Test
        @DisplayName("UTCID02 (N) - Doi sang TERMINATED thanh cong")
        void utcid02_terminateMember() {
            loginAsCenter();
            when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
            when(membershipRepository.save(any(CenterTutorMembership.class))).thenAnswer(i -> i.getArgument(0));
            when(recruitmentApplicationRepository
                    .findByTutor_TutorIdAndRecruitmentPost_Center_CenterIdOrderByAppliedAtDesc(anyLong(), anyLong()))
                    .thenReturn(List.of());

            service.updateMembershipStatus(MEMBERSHIP_ID, CenterTutorMembershipStatus.TERMINATED);

            assertEquals(CenterTutorMembershipStatus.TERMINATED, membership.getStatus());
        }

        @Test
        @DisplayName("UTCID03 (A) - status null -> IllegalArgumentException")
        void utcid03_nullStatus() {
            loginAsCenter();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateMembershipStatus(MEMBERSHIP_ID, null));
            assertEquals("Thiếu trạng thái cần cập nhật", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Thanh vien khong ton tai -> ResourceNotFoundException")
        void utcid04_membershipNotFound() {
            loginAsCenter();
            when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.updateMembershipStatus(MEMBERSHIP_ID, CenterTutorMembershipStatus.INACTIVE));
            assertEquals("Không tìm thấy thành viên", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Thanh vien cua trung tam khac -> ForbiddenException")
        void utcid05_notOwner() {
            TutorCenter otherCenter = new TutorCenter();
            otherCenter.setCenterId(999L);
            membership.setCenter(otherCenter);
            loginAsCenter();
            when(membershipRepository.findById(MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.updateMembershipStatus(MEMBERSHIP_ID, CenterTutorMembershipStatus.INACTIVE));
            assertEquals("Bạn không có quyền với thành viên này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Tai khoan goi khong phai trung tam -> requireCenter chan lai")
        void utcid06_callerIsNotACenter() {
            when(authHelper.requireRole(com.tcs.module.profile.enums.UserRole.TUTOR_CENTER))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.updateMembershipStatus(MEMBERSHIP_ID, CenterTutorMembershipStatus.INACTIVE));
            assertEquals("Không có quyền truy cập", ex.getMessage());
            verify(membershipRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: closeEnrollment
    // ===================================================================
    @Nested
    @DisplayName("closeEnrollment")
    class CloseEnrollment {

        @Test
        @DisplayName("UTCID01 (N) - Du hoc sinh + da co gia su -> MATCHED")
        void utcid01_enoughStudentsWithTutor() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(3L);
            com.tcs.module.marketplace.entity.ClassAssignment assignment =
                    new com.tcs.module.marketplace.entity.ClassAssignment();
            assignment.setTutor(tutor);
            when(classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(CLASS_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(Optional.of(assignment));
            when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));

            service.closeEnrollment(CLASS_ID);

            assertEquals(TutoringClassStatus.MATCHED, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID02 (N) - Du hoc sinh nhung chua co gia su -> ENROLLMENT_CLOSED")
        void utcid02_enoughStudentsNoTutor() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(2L);
            when(classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(CLASS_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));

            service.closeEnrollment(CLASS_ID);

            assertEquals(TutoringClassStatus.ENROLLMENT_CLOSED, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID03 (B) - Vua du toi thieu (enrolled == minStudents) -> dong duoc")
        void utcid03_exactlyMinimum() {
            tutoringClass.setMinStudents(2);
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(2L);
            when(classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(CLASS_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));

            service.closeEnrollment(CLASS_ID);

            assertEquals(TutoringClassStatus.ENROLLMENT_CLOSED, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID04 (B) - Thieu 1 hoc sinh so voi toi thieu -> chan")
        void utcid04_oneBelowMinimum() {
            tutoringClass.setMinStudents(3);
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(2L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.closeEnrollment(CLASS_ID));
            assertTrue(ex.getMessage().contains("Chưa đủ học sinh tối thiểu"),
                    "Thông báo phải nêu rõ số hiện có / số cần: " + ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (B) - minStudents null -> mac dinh can it nhat 1 hoc sinh")
        void utcid05_nullMinStudentsDefaultsToOne() {
            tutoringClass.setMinStudents(null);
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(0L);

            assertThrows(IllegalArgumentException.class, () -> service.closeEnrollment(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID06 (A) - Lop khong o trang thai OPEN -> IllegalArgumentException")
        void utcid06_classNotOpen() {
            tutoringClass.setStatus(TutoringClassStatus.DRAFT);
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.closeEnrollment(CLASS_ID));
            assertEquals("Chỉ lớp đang mở tuyển sinh mới có thể đóng ghi danh.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Lop cua trung tam khac -> ForbiddenException")
        void utcid07_notOwner() {
            useOtherCenterOwner();
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.closeEnrollment(CLASS_ID));
            assertEquals("Bạn không có quyền chỉnh sửa lớp học này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Lop khong ton tai -> ResourceNotFoundException")
        void utcid08_classNotFound() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.closeEnrollment(CLASS_ID));
            assertEquals("Không tìm thấy lớp học", ex.getMessage());
        }
    }
    // ===================================================================
    //  Sheet: assignTutor
    // ===================================================================
    @Nested
    @DisplayName("assignTutor")
    class AssignTutor {

        private CenterTutorMembership membership;

        @BeforeEach
        void initMembership() {
            membership = new CenterTutorMembership();
            membership.setMembershipId(MEMBERSHIP_ID);
            membership.setCenter(center);
            membership.setTutor(tutor);
            membership.setStatus(CenterTutorMembershipStatus.ACTIVE);
        }

        /** Trung tam dang nhap, lop thuoc trung tam, gia su la thanh vien ACTIVE. */
        private void givenActiveMember() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(1L, TUTOR_ID))
                    .thenReturn(Optional.of(membership));
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(tutor));
            when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));
            when(tutorApplicationRepository.save(any(com.tcs.module.marketplace.entity.TutorApplication.class)))
                    .thenAnswer(i -> {
                        com.tcs.module.marketplace.entity.TutorApplication app = i.getArgument(0);
                        app.setApplicationId(400L);
                        return app;
                    });
        }

        /** Slot lich hoc dung cho kiem thu trung lich. */
        private com.tcs.module.marketplace.entity.ScheduleSlot slot(
                Long slotId, TutoringClass owner, int dayOfWeek, int startHour, int endHour) {
            com.tcs.module.marketplace.entity.ScheduleSlot s =
                    new com.tcs.module.marketplace.entity.ScheduleSlot();
            s.setSlotId(slotId);
            s.setTutoringClass(owner);
            s.setDayOfWeek(dayOfWeek);
            s.setStartTime(java.time.LocalTime.of(startHour, 0));
            s.setEndTime(java.time.LocalTime.of(endHour, 0));
            return s;
        }

        @Test
        @DisplayName("UTCID01 (N) - Trung tam so huu lop, gia su ACTIVE, khong trung lich -> gan gia su chinh")
        void utcid01_assignSuccessfully() {
            givenActiveMember();

            service.assignTutor(CLASS_ID, TUTOR_ID);

            verify(classAssignmentRepository).save(any());
            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID02 (A) - tutorId null -> 'Vui lòng chọn gia sư'")
        void utcid02_nullTutorId() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.assignTutor(CLASS_ID, null));
            assertEquals("Vui lòng chọn gia sư", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Lop cua trung tam khac -> ForbiddenException")
        void utcid03_notOwner() {
            useOtherCenterOwner();
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            assertThrows(ForbiddenException.class, () -> service.assignTutor(CLASS_ID, TUTOR_ID));
        }

        @Test
        @DisplayName("UTCID04 (A) - Lop da COMPLETED (khong con gan duoc) -> 'Lớp này không thể gán gia sư nữa.'")
        void utcid04_classNotStaffable() {
            tutoringClass.setStatus(TutoringClassStatus.COMPLETED);
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.assignTutor(CLASS_ID, TUTOR_ID));
            assertEquals("Lớp này không thể gán gia sư nữa.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Gia su khong co ban ghi thanh vien voi trung tam -> chan")
        void utcid05_tutorNotInCenter() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(1L, TUTOR_ID))
                    .thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.assignTutor(CLASS_ID, TUTOR_ID));
            assertEquals("Gia sư này không thuộc danh sách gia sư của trung tâm.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Thanh vien khong con ACTIVE -> chan")
        void utcid06_membershipInactive() {
            membership.setStatus(CenterTutorMembershipStatus.INACTIVE);
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(1L, TUTOR_ID))
                    .thenReturn(Optional.of(membership));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.assignTutor(CLASS_ID, TUTOR_ID));
            assertEquals("Gia sư này không còn là thành viên đang hoạt động của trung tâm.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Thanh vien ACTIVE nhung khong co ban ghi Tutor -> 'Không tìm thấy gia sư'")
        void utcid07_tutorRowMissing() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(1L, TUTOR_ID))
                    .thenReturn(Optional.of(membership));
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.assignTutor(CLASS_ID, TUTOR_ID));
            assertEquals("Không tìm thấy gia sư", ex.getMessage());
            verify(classAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID08 (A) - Gia su da co lich day trung khung gio -> chan kem ten lop trung")
        void utcid08_scheduleConflict() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(1L, TUTOR_ID))
                    .thenReturn(Optional.of(membership));
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(tutor));

            // Lop dang gan: thu 2, 18:00-20:00, keo dai 30 ngay toi.
            tutoringClass.setStartDate(java.time.LocalDate.now());
            tutoringClass.setEndDate(java.time.LocalDate.now().plusDays(30));
            when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID))
                    .thenReturn(List.of(slot(1L, tutoringClass, 1, 18, 20)));

            // Lop khac gia su dang day: trung thu, trung khung gio, trung khoang ngay.
            TutoringClass other = new TutoringClass();
            other.setClassId(999L);
            other.setTitle("Ly 9 - Ca toi");
            other.setStartDate(java.time.LocalDate.now());
            other.setEndDate(java.time.LocalDate.now().plusDays(30));
            when(scheduleSlotRepository.findByTutoringClass_ClassId(999L))
                    .thenReturn(List.of(slot(2L, other, 1, 18, 20)));

            com.tcs.module.marketplace.entity.TutorApplication otherApp =
                    new com.tcs.module.marketplace.entity.TutorApplication();
            otherApp.setTutoringClass(other);
            com.tcs.module.marketplace.entity.ClassAssignment otherAssignment =
                    new com.tcs.module.marketplace.entity.ClassAssignment();
            otherAssignment.setApplication(otherApp);
            otherAssignment.setStatus(ClassAssignmentStatus.ACTIVE);
            when(classAssignmentRepository.findByTutor_TutorIdAndStatus(TUTOR_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(List.of(otherAssignment));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.assignTutor(CLASS_ID, TUTOR_ID));
            assertTrue(ex.getMessage().contains("đã có lịch dạy trùng thời gian với lớp này"),
                    "Phai bao trung lich: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("Ly 9 - Ca toi"),
                    "Phai neu ten lop bi trung: " + ex.getMessage());
            verify(classAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID09 (B) - Lop dang ENROLLMENT_CLOSED khi gan gia su -> chuyen sang MATCHED")
        void utcid09_enrollmentClosedBecomesMatched() {
            tutoringClass.setStatus(TutoringClassStatus.ENROLLMENT_CLOSED);
            givenActiveMember();

            service.assignTutor(CLASS_ID, TUTOR_ID);

            assertEquals(TutoringClassStatus.MATCHED, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID10 (B) - Lop dang DRAFT / OPEN khi gan gia su -> giu nguyen trang thai")
        void utcid10_draftAndOpenKeepStatus() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);
            givenActiveMember();

            service.assignTutor(CLASS_ID, TUTOR_ID);

            assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus(),
                    "Con dang tuyen hoc sinh thi chua chuyen MATCHED");
        }

        @Test
        @DisplayName("UTCID11 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid11_callerIsNotACenter() {
            when(authHelper.requireRole(com.tcs.module.profile.enums.UserRole.TUTOR_CENTER))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.assignTutor(CLASS_ID, TUTOR_ID));
            assertEquals("Không có quyền truy cập", ex.getMessage());
            verify(classAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID12 (A) - Lop khong ton tai -> ResourceNotFoundException")
        void utcid12_classNotFound() {
            loginAsCenter();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.assignTutor(CLASS_ID, TUTOR_ID));
        }
    }
}
