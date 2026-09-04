package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.ScheduleSlotRequest;
import com.tcs.module.center.dto.response.CenterClassResponse;
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
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
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
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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
class Report52CenterClassEnrollmentITTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long CLASS_ID = 500L;
    private static final Long TUTOR_ID = 20L;

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

    @InjectMocks
    private CenterServiceImpl centerService;

    @Test
    @Tag("report52-it")
    void IT_CCE_002_ListCenterClassesReturnsOwnedRowsWithEnrollmentCounts() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.OPEN);
        ClassStudent student = enrolledStudent(tutoringClass, user(301L));

        loginAsCenter(center);
        stubClassResponseDependencies(tutoringClass, List.of(student));
        when(tutoringClassRepository.findByCreator_UserId(CENTER_USER_ID)).thenReturn(List.of(tutoringClass));

        List<CenterClassResponse> response = centerService.listMyClasses();

        assertEquals(1, response.size());
        assertEquals(CLASS_ID, response.get(0).getClassId());
        assertEquals(TutoringClassStatus.OPEN, response.get(0).getStatus());
        assertEquals(1, response.get(0).getEnrolledCount());
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_003_GetCenterClassDetailReturnsJoinedScheduleAndStudentData() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.OPEN);
        ClassStudent student = enrolledStudent(tutoringClass, user(301L));
        ScheduleSlot slot = scheduleSlot(tutoringClass);

        loginAsCenter(center);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        stubClassResponseDependencies(tutoringClass, List.of(student));
        when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(slot));

        CenterClassResponse response = centerService.getMyClass(CLASS_ID);

        assertEquals(CLASS_ID, response.getClassId());
        assertEquals("Lớp Toán trung tâm", response.getTitle());
        assertEquals(1, response.getSchedule().size());
        assertEquals("Nguyễn Minh Anh", response.getStudents().get(0).getStudentName());
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_004_RejectCenterClassCreationWhenRequiredFieldsAreMissing() {
        TutorCenter center = verifiedCenter();
        SaveClassRequest request = validCenterClassRequest();
        request.setTitle("");

        loginAsCenter(center);
        when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(activeWallet()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.createClass(request));

        assertEquals("Tiêu đề là bắt buộc", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_005_RejectPublishingCenterClassInIllegalState() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.IN_PROGRESS);

        loginAsCenter(center);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.publishClass(CLASS_ID));

        assertEquals("Chỉ lớp ở trạng thái nháp mới có thể đăng tải", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    @Test
    void SUPPORT_CCE_CreateCenterClassStoresDraftAndAuditHistory() {
        TutorCenter center = verifiedCenter();
        SaveClassRequest request = validCenterClassRequest();

        loginAsCenter(center);
        when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(activeWallet()));
        stubCatalogLookups();
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> {
            TutoringClass saved = invocation.getArgument(0);
            saved.setClassId(CLASS_ID);
            return saved;
        });
        when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of());
        when(scheduleSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(centerClass(center, TutoringClassStatus.DRAFT)));
        stubClassResponseDependencies(centerClass(center, TutoringClassStatus.DRAFT), List.of());

        CenterClassResponse response = centerService.createClass(request);

        assertEquals(CLASS_ID, response.getClassId());
        ArgumentCaptor<TutoringClass> classCaptor = ArgumentCaptor.forClass(TutoringClass.class);
        verify(tutoringClassRepository).save(classCaptor.capture());
        assertEquals(ClassType.CENTER, classCaptor.getValue().getClassType());
        assertEquals(TutoringClassStatus.DRAFT, classCaptor.getValue().getStatus());
        assertEquals(4, classCaptor.getValue().getNumberOfSessions());
        verify(auditLogService).record(eq(CENTER_USER_ID), eq("CREATE_CENTER_CLASS"),
                eq("TutoringClass"), eq(CLASS_ID), eq(null), eq(request));
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_015_ListCenterClassesKeepsRepositoryRowCountForCurrentCenter() {
        TutorCenter center = verifiedCenter();
        TutoringClass first = centerClass(center, TutoringClassStatus.OPEN);
        TutoringClass second = centerClass(center, TutoringClassStatus.MATCHED);
        second.setClassId(CLASS_ID + 1);

        loginAsCenter(center);
        stubClassResponseDependencies(first, List.of());
        stubClassResponseDependencies(second, List.of());
        when(tutoringClassRepository.findByCreator_UserId(CENTER_USER_ID)).thenReturn(List.of(first, second));

        List<CenterClassResponse> response = centerService.listMyClasses();

        assertEquals(2, response.size());
        verify(tutoringClassRepository, org.mockito.Mockito.times(2)).findByCreator_UserId(CENTER_USER_ID);
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_018_ExpiredOpenEnrollmentAutoCancelsClassWhenMinimumStudentsNotReached() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.OPEN);
        tutoringClass.setMinStudents(2);
        tutoringClass.setEnrollmentDeadline(LocalDate.now().minusDays(1));

        loginAsCenter(center);
        stubClassResponseDependencies(tutoringClass, List.of());
        when(tutoringClassRepository.findByCreator_UserId(CENTER_USER_ID)).thenReturn(List.of(tutoringClass));
        when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(1L);

        centerService.listMyClasses();

        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
        verify(tutoringClassRepository).save(tutoringClass);
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_020_ActivateCenterClassUpdatesClassStatusAndNotifiesTutorAndClient() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.MATCHED);
        TutoringClass activeClass = centerClass(center, TutoringClassStatus.IN_PROGRESS);
        Tutor tutor = tutor(user(201L));
        ClassAssignment assignment = assignment(tutoringClass, tutor);
        ClassStudent student = enrolledStudent(tutoringClass, user(301L));

        loginAsCenter(center);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(1L);
        when(tutoringClassRepository.save(tutoringClass)).thenAnswer(invocation -> {
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
            return activeClass;
        });
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of(student));
        stubClassResponseDependencies(activeClass, List.of(student));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));

        CenterClassResponse response = centerService.activateClass(CLASS_ID);

        assertEquals(TutoringClassStatus.IN_PROGRESS, response.getStatus());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutor.getUser()),
                eq(NotificationType.CLASS),
                eq("CENTER_CLASS_STARTED"),
                any(),
                eq("Lớp học đã bắt đầu"),
                any(),
                eq("CENTER_CLASS"),
                eq(CLASS_ID));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(student.getEnrolledByUser()),
                eq(NotificationType.CLASS),
                eq("CENTER_CLASS_STARTED"),
                any(),
                eq("Lớp học đã bắt đầu"),
                any(),
                eq("CENTER_CLASS"),
                eq(CLASS_ID));
    }

    private void loginAsCenter(TutorCenter center) {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
    }

    private void stubClassResponseDependencies(TutoringClass tutoringClass, List<ClassStudent> students) {
        when(classStudentRepository.existsByTutoringClass_ClassId(tutoringClass.getClassId())).thenReturn(!students.isEmpty());
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                tutoringClass.getClassId(), ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
        when(substitutionService.findAssistant(tutoringClass.getClassId())).thenReturn(Optional.empty());
        when(scheduleSlotRepository.findByTutoringClass_ClassId(tutoringClass.getClassId())).thenReturn(List.of());
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                tutoringClass.getClassId(), ClassStudentStatus.ENROLLED)).thenReturn(students);
        when(systemParameterRepository.findByParamKey("classorigin:" + tutoringClass.getClassId()))
                .thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("classtpl:" + tutoringClass.getClassId()))
                .thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("classterms:" + tutoringClass.getClassId()))
                .thenReturn(Optional.empty());
        when(centerEscrowAutoSettlementService.isTutorConfirmed(tutoringClass.getClassId())).thenReturn(false);
    }

    private void stubCatalogLookups() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setName("Lớp phổ thông");
        Subject subject = new Subject();
        subject.setSubjectId(2L);
        subject.setSubjectName("Toán");
        Grade grade = new Grade();
        grade.setGradeId(3L);
        grade.setGradeName("Lớp 9");
        Province province = new Province();
        province.setProvinceId(4L);
        province.setProvinceName("Hà Nội");
        Location location = new Location();
        location.setLocationId(5L);
        location.setProvince(province);
        location.setWardName("Cầu Giấy");
        location.setAddressLine("Số 15 Trần Duy Hưng");

        when(categoryRepository.findByNameIgnoreCase("Lớp phổ thông")).thenReturn(Optional.of(category));
        when(subjectRepository.findFirstBySubjectNameIgnoreCase("Toán")).thenReturn(Optional.of(subject));
        when(gradeRepository.findFirstByGradeNameIgnoreCase("Lớp 9")).thenReturn(Optional.of(grade));
        when(provinceRepository.findFirstByProvinceNameIgnoreCase("Hà Nội")).thenReturn(Optional.of(province));
        when(locationRepository.findFirstByProvince_ProvinceIdAndWardNameIgnoreCaseAndAddressLineIgnoreCase(
                4L, "Cầu Giấy", "Số 15 Trần Duy Hưng")).thenReturn(Optional.of(location));
    }

    private SaveClassRequest validCenterClassRequest() {
        SaveClassRequest request = new SaveClassRequest();
        request.setTitle("Lớp Toán trung tâm");
        request.setDescription("Ôn tập kiến thức Toán lớp 9");
        request.setCategoryName("Lớp phổ thông");
        request.setSubjectName("Toán");
        request.setGradeName("Lớp 9");
        request.setProvinceName("Hà Nội");
        request.setWardName("Cầu Giấy");
        request.setAddressDetail("Số 15 Trần Duy Hưng");
        request.setLessonMode(LessonMode.OFFLINE);
        request.setRecurringType(RecurringType.WEEKLY);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(15));
        request.setTuitionFee(new BigDecimal("1200000.00"));
        request.setMaxStudents(10);
        request.setMinStudents(1);
        request.setSchedule(List.of(scheduleRequest(2), scheduleRequest(4)));
        return request;
    }

    private ScheduleSlotRequest scheduleRequest(int dayOfWeek) {
        ScheduleSlotRequest request = new ScheduleSlotRequest();
        request.setDayOfWeek(dayOfWeek);
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(19, 30));
        return request;
    }

    private ScheduleSlot scheduleSlot(TutoringClass tutoringClass) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(30L);
        slot.setTutoringClass(tutoringClass);
        slot.setDayOfWeek(2);
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(19, 30));
        return slot;
    }

    private TutorCenter verifiedCenter() {
        User user = user(CENTER_USER_ID);
        TutorCenter center = new TutorCenter();
        center.setCenterId(10L);
        center.setUser(user);
        center.setCompanyName("Trung tâm Minh Tâm");
        center.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        return center;
    }

    private TutoringClass centerClass(TutorCenter center, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(center.getUser());
        tutoringClass.setCenter(center);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setTitle("Lớp Toán trung tâm");
        tutoringClass.setDescription("Ôn tập Toán lớp 9");
        tutoringClass.setStatus(status);
        tutoringClass.setLessonMode(LessonMode.OFFLINE);
        tutoringClass.setRecurringType(RecurringType.WEEKLY);
        tutoringClass.setStartDate(LocalDate.now().plusDays(1));
        tutoringClass.setEndDate(LocalDate.now().plusDays(15));
        tutoringClass.setTuitionFee(new BigDecimal("1200000.00"));
        tutoringClass.setMaxStudents(10);
        tutoringClass.setMinStudents(1);
        return tutoringClass;
    }

    private ClassStudent enrolledStudent(TutoringClass tutoringClass, User enrolledBy) {
        ClassStudent student = new ClassStudent();
        student.setClassStudentId(700L);
        student.setTutoringClass(tutoringClass);
        student.setEnrolledByUser(enrolledBy);
        student.setStudentName("Nguyễn Minh Anh");
        student.setStudentPhone("0900000001");
        student.setStatus(ClassStudentStatus.ENROLLED);
        return student;
    }

    private ClassAssignment assignment(TutoringClass tutoringClass, Tutor tutor) {
        TutorApplication application = new TutorApplication();
        application.setApplicationId(90L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setStatus(TutorApplicationStatus.ACCEPTED);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(91L);
        assignment.setApplication(application);
        assignment.setTutor(tutor);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
    }

    private Tutor tutor(User user) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(user);
        tutor.setFullName("Lê Hoàng Nam");
        return tutor;
    }

    private Wallet activeWallet() {
        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
    }
}
