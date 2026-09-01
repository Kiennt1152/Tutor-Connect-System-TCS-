package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.tcs.module.center.dto.request.SubstitutionDecisionBody;
import com.tcs.module.center.repository.CenterTutorMembershipRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.dto.SubstitutionEntry;
import com.tcs.module.marketplace.entity.ClassAssignment;
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
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Unit test module Center — kich hoat lop, xac nhan hoan thanh khoa hoc, duyet yeu cau day thay
 * va dong yeu cau tim gia su cua phu huynh.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: cac sheet activateClass, ceConfirmCompletion,
 * ceDecideSubstitution, giveUpClassRequest va rejectClassRequest.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CenterServiceImplRequestTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long CENTER_ID = 1L;
    private static final Long CLASS_ID = 500L;
    private static final Long TUTOR_ID = 20L;
    private static final Long CLIENT_USER_ID = 300L;
    private static final String REQUEST_ID = "req-001";

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
    @Mock private com.tcs.module.finance.repository.WalletRepository walletRepository;
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

    private User centerUser;
    private TutorCenter center;
    private TutoringClass tutoringClass;

    @BeforeEach
    void setUp() {
        centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        centerUser.setEmail("trungtam@tcs.vn");

        center = new TutorCenter();
        center.setCenterId(CENTER_ID);
        center.setUser(centerUser);
        center.setCompanyName("Trung tam 1");

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setTitle("Toan 9");
        tutoringClass.setCreator(centerUser);
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        tutoringClass.setMinStudents(2);

        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));

        // Danh muc tim-hoac-tao: tra ve ban ghi co san de khong phai chay nhanh tao moi.
        var province = new com.tcs.module.catalog.entity.Province();
        province.setProvinceId(1L);
        province.setProvinceName("Ha Noi");
        when(provinceRepository.findFirstByProvinceNameIgnoreCase(anyString()))
                .thenReturn(Optional.of(province));
        var location = new com.tcs.module.catalog.entity.Location();
        location.setLocationId(1L);
        location.setAddressLine("So 1 Dai Co Viet");
        location.setWardName("Bach Khoa");
        location.setProvince(province);
        when(locationRepository.findFirstByProvince_ProvinceIdAndWardNameIgnoreCaseAndAddressLineIgnoreCase(
                anyLong(), anyString(), anyString())).thenReturn(Optional.of(location));
    }

    /** Nguoi goi khong phai tai khoan trung tam. */
    private void rejectCenterRole() {
        when(authHelper.requireRole(UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Không có quyền truy cập"));
    }

    // ===================================================================
    //  Sheet: activateClass
    // ===================================================================
    @Nested
    @DisplayName("activateClass")
    class ActivateClass {

        @BeforeEach
        void initActivation() {
            Tutor tutor = new Tutor();
            tutor.setTutorId(TUTOR_ID);
            tutor.setFullName("Gia su 1");
            ClassAssignment assignment = new ClassAssignment();
            assignment.setAssignmentId(700L);
            assignment.setTutor(tutor);
            assignment.setStatus(ClassAssignmentStatus.ACTIVE);
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassStudentStatus.ENROLLED)).thenReturn(List.of());
            givenEnrolledCount(3);
        }

        private void givenEnrolledCount(long count) {
            when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassStudentStatus.ENROLLED)).thenReturn(count);
        }

        @Test
        @DisplayName("UTCID01 (N) - Lop MATCHED, co gia su chinh, du si so -> chuyen IN_PROGRESS va bao lop bat dau")
        void utcid01_activateMatchedClass() {
            service.activateClass(CLASS_ID);

            assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID02 (N) - Lop ENROLLMENT_CLOSED va du dieu kien -> cung kich hoat duoc")
        void utcid02_activateEnrollmentClosedClass() {
            tutoringClass.setStatus(TutoringClassStatus.ENROLLMENT_CLOSED);

            service.activateClass(CLASS_ID);

            assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.activateClass(CLASS_ID));
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid04_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.activateClass(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID05 (A) - Lop cua trung tam khac -> ForbiddenException")
        void utcid05_notOwner() {
            User otherCenterUser = new User();
            otherCenterUser.setUserId(999L);
            tutoringClass.setCreator(otherCenterUser);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.activateClass(CLASS_ID));
            assertEquals("Bạn không có quyền chỉnh sửa lớp học này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Lop khong o MATCHED / ENROLLMENT_CLOSED -> 'Chỉ kích hoạt lớp đã đóng ghi danh / đã ghép gia sư.'")
        void utcid06_statusNotActivatable() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.activateClass(CLASS_ID));
            assertEquals("Chỉ kích hoạt lớp đã đóng ghi danh / đã ghép gia sư.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Lop chua co gia su chinh ACTIVE -> 'Cần gán gia sư chính trước khi kích hoạt lớp.'")
        void utcid07_noMainTutor() {
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.activateClass(CLASS_ID));
            assertEquals("Cần gán gia sư chính trước khi kích hoạt lớp.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - So hoc sinh = minStudents - 1 (thieu 1) -> chan kem ti le hien tai")
        void utcid08_oneStudentShort() {
            givenEnrolledCount(1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.activateClass(CLASS_ID));
            assertEquals("Chưa đủ sĩ số tối thiểu để kích hoạt lớp (1/2).", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID09 (B) - So hoc sinh = dung minStudents -> kich hoat duoc")
        void utcid09_exactlyMinStudents() {
            givenEnrolledCount(2);

            service.activateClass(CLASS_ID);

            assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID10 (B) - minStudents = null -> si so toi thieu mac dinh la 1")
        void utcid10_nullMinStudentsDefaultsToOne() {
            tutoringClass.setMinStudents(null);
            givenEnrolledCount(1);

            service.activateClass(CLASS_ID);

            assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus(),
                    "Khong dat si so toi thieu thi chi can 1 hoc sinh");
        }
    }

    // ===================================================================
    //  Sheet: ceConfirmCompletion
    // ===================================================================
    @Nested
    @DisplayName("ceConfirmCompletion")
    class CeConfirmCompletion {

        @Test
        @DisplayName("UTCID01 (N) - Trung tam so huu lop, du dieu kien -> uy quyen cho CenterEscrowAutoSettlementService")
        void utcid01_confirmSuccessfully() {
            service.confirmClassCompletion(CLASS_ID);

            verify(centerEscrowAutoSettlementService).confirmCompletion(CLASS_ID);
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid02_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.confirmClassCompletion(CLASS_ID));
            verify(centerEscrowAutoSettlementService, never()).confirmCompletion(anyLong());
        }

        @Test
        @DisplayName("UTCID03 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.confirmClassCompletion(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID04 (A) - Lop cua trung tam khac -> ForbiddenException")
        void utcid04_notOwner() {
            User otherCenterUser = new User();
            otherCenterUser.setUserId(999L);
            tutoringClass.setCreator(otherCenterUser);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Bạn không có quyền chỉnh sửa lớp học này", ex.getMessage());
            verify(centerEscrowAutoSettlementService, never()).confirmCompletion(anyLong());
        }

        @Test
        @DisplayName("UTCID05 (A) - Gia su chua xac nhan hoan thanh -> loi tu tang tat toan duoc nem ra")
        void utcid05_tutorNotConfirmed() {
            org.mockito.Mockito.doThrow(new IllegalArgumentException(
                            "Gia sư chưa xác nhận hoàn thành khóa học — chưa thể đóng lớp."))
                    .when(centerEscrowAutoSettlementService).confirmCompletion(CLASS_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Gia sư chưa xác nhận hoàn thành khóa học — chưa thể đóng lớp.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Khoa hoc da duoc xac nhan hoan thanh truoc do -> chan")
        void utcid06_alreadyCompleted() {
            org.mockito.Mockito.doThrow(new IllegalArgumentException(
                            "Khóa học đã được xác nhận hoàn thành trước đó."))
                    .when(centerEscrowAutoSettlementService).confirmCompletion(CLASS_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Khóa học đã được xác nhận hoàn thành trước đó.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Chua diem danh du so buoi cua khoa -> chan")
        void utcid07_notEnoughAttendedSessions() {
            org.mockito.Mockito.doThrow(new IllegalArgumentException(
                            "Chưa điểm danh đủ số buổi học của khóa — không thể xác nhận hoàn thành."))
                    .when(centerEscrowAutoSettlementService).confirmCompletion(CLASS_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Chưa điểm danh đủ số buổi học của khóa — không thể xác nhận hoàn thành.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Lop dang co khieu nai / tranh chap -> chan")
        void utcid08_openDispute() {
            org.mockito.Mockito.doThrow(new IllegalArgumentException(
                            "Lớp đang có khiếu nại/tranh chấp đang xử lý — chưa thể xác nhận hoàn thành."))
                    .when(centerEscrowAutoSettlementService).confirmCompletion(CLASS_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Lớp đang có khiếu nại/tranh chấp đang xử lý — chưa thể xác nhận hoàn thành.",
                    ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: ceDecideSubstitution
    // ===================================================================
    @Nested
    @DisplayName("ceDecideSubstitution")
    class CeDecideSubstitution {

        private final LocalDate date = LocalDate.now().plusDays(1);

        private SubstitutionDecisionBody body(Long classId, LocalDate day, boolean approve) {
            SubstitutionDecisionBody b = new SubstitutionDecisionBody();
            b.setClassId(classId);
            b.setDate(day);
            b.setApprove(approve);
            return b;
        }

        private void givenDecision(String status) {
            when(substitutionService.decide(anyLong(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenReturn(new SubstitutionEntry(CLASS_ID, date, TUTOR_ID, status, "Gia su chinh bi om"));
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("UTCID01 (N) - Trung tam duyet yeu cau day thay -> entry APPROVED va ghi audit log")
        void utcid01_approveSubstitution() {
            givenDecision(SubstitutionEntry.APPROVED);

            var response = service.decideSubstitution(body(CLASS_ID, date, true));

            assertEquals(SubstitutionEntry.APPROVED, response.getStatus());
            verify(substitutionService).decide(CLASS_ID, date, true);
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq(CENTER_USER_ID),
                    org.mockito.ArgumentMatchers.eq("DECIDE_SUBSTITUTION"),
                    org.mockito.ArgumentMatchers.eq("TutoringClass"),
                    org.mockito.ArgumentMatchers.eq(CLASS_ID), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Trung tam tu choi yeu cau day thay -> entry REJECTED va ghi audit log")
        void utcid02_rejectSubstitution() {
            givenDecision(SubstitutionEntry.REJECTED);

            var response = service.decideSubstitution(body(CLASS_ID, date, false));

            assertEquals(SubstitutionEntry.REJECTED, response.getStatus());
            verify(substitutionService).decide(CLASS_ID, date, false);
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class,
                    () -> service.decideSubstitution(body(CLASS_ID, date, true)));
            verify(substitutionService, never()).decide(anyLong(), any(), org.mockito.ArgumentMatchers.anyBoolean());
        }

        @Test
        @DisplayName("UTCID04 (A) - classId = null -> 'Thiếu thông tin yêu cầu dạy thay'")
        void utcid04_nullClassId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.decideSubstitution(body(null, date, true)));
            assertEquals("Thiếu thông tin yêu cầu dạy thay", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - date = null -> 'Thiếu thông tin yêu cầu dạy thay'")
        void utcid05_nullDate() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.decideSubstitution(body(CLASS_ID, null, true)));
            assertEquals("Thiếu thông tin yêu cầu dạy thay", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid06_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.decideSubstitution(body(CLASS_ID, date, true)));
        }

        @Test
        @DisplayName("UTCID07 (A) - Lop cua trung tam khac -> ForbiddenException")
        void utcid07_notOwner() {
            User otherCenterUser = new User();
            otherCenterUser.setUserId(999L);
            tutoringClass.setCreator(otherCenterUser);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.decideSubstitution(body(CLASS_ID, date, true)));
            assertEquals("Bạn không có quyền chỉnh sửa lớp học này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Khong co yeu cau day thay vao ngay do -> loi tu substitutionService")
        void utcid08_noSubstitutionRequestOnThatDate() {
            when(substitutionService.decide(anyLong(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                    .thenThrow(new ResourceNotFoundException("Không tìm thấy yêu cầu dạy thay"));

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.decideSubstitution(body(CLASS_ID, date, true)));
            assertEquals("Không tìm thấy yêu cầu dạy thay", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: rejectClassRequest & giveUpClassRequest
    // ===================================================================

    private ClassRequestStore.ClassRequestData requestData(Long ownerCenterId, String status) {
        return new ClassRequestStore.ClassRequestData(
                REQUEST_ID, CLIENT_USER_ID, ownerCenterId, 2L, "Can gia su Toan",
                new BigDecimal("1000000"), status, null,
                java.time.LocalDateTime.now().toString(), null, List.of(), null);
    }

    private CenterRequestFeePaymentResponse feePayment(CenterRequestFeeStatus status) {
        return CenterRequestFeePaymentResponse.builder()
                .requestId(REQUEST_ID)
                .feeHoldId(60L)
                .status(status)
                .amount(new BigDecimal("200000"))
                .build();
    }

    private void givenRequest(ClassRequestStore.ClassRequestData data) {
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
        when(classRequestStore.withStatus(any(), anyString(), any())).thenReturn(data);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(new User()));
    }

    @Nested
    @DisplayName("rejectClassRequest")
    class RejectClassRequest {

        @Test
        @DisplayName("UTCID01 (N) - Trung tam so huu yeu cau, phi da giu -> dong yeu cau, hoan phi va bao phu huynh")
        void utcid01_rejectSuccessfully() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING));
            when(centerRequestFeeService.getPayment(REQUEST_ID))
                    .thenReturn(Optional.of(feePayment(CenterRequestFeeStatus.HELD)));

            service.rejectClassRequest(REQUEST_ID, "Khong co gia su phu hop");

            verify(classRequestStore).save(any());
            verify(centerRequestFeeService).requestRefund(REQUEST_ID, "Khong co gia su phu hop");
            verify(notificationDispatchService).notifyUserFromTemplate(
                    any(), any(), any(), any(),
                    org.mockito.ArgumentMatchers.eq("Yêu cầu tìm gia sư bị từ chối"),
                    anyString(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Phi xu ly chua thanh toan -> chi huy phi va dung lai, khong hoan tien")
        void utcid02_unpaidFeeIsCancelled() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING));
            when(centerRequestFeeService.getPayment(REQUEST_ID))
                    .thenReturn(Optional.of(feePayment(CenterRequestFeeStatus.PENDING_PAYMENT)));

            service.rejectClassRequest(REQUEST_ID, "Khong co gia su phu hop");

            verify(centerRequestFeeService).cancelUnpaid(REQUEST_ID);
            verify(centerRequestFeeService, never()).requestRefund(anyString(), any());
            verify(classRequestStore, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.rejectClassRequest(REQUEST_ID, "ly do"));
        }

        @Test
        @DisplayName("UTCID04 (A) - requestId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu mở lớp'")
        void utcid04_requestNotFound() {
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.rejectClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Không tìm thấy yêu cầu mở lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau thuoc trung tam khac -> 'Không có quyền xử lý yêu cầu này'")
        void utcid05_requestOfAnotherCenter() {
            givenRequest(requestData(999L, ClassRequestStore.STATUS_SEARCHING));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.rejectClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Không có quyền xử lý yêu cầu này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Yeu cau da ACCEPTED -> 'Yêu cầu này đã được xử lý'")
        void utcid06_alreadyAccepted() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_ACCEPTED));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.rejectClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Yêu cầu này đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Yeu cau da REJECTED -> 'Yêu cầu này đã được xử lý'")
        void utcid07_alreadyRejected() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_REJECTED));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.rejectClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Yêu cầu này đã được xử lý", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("giveUpClassRequest")
    class GiveUpClassRequest {

        @Test
        @DisplayName("UTCID01 (N) - Trung tam bo cuoc voi yeu cau da tra phi -> dong yeu cau, hoan phi va bao phu huynh")
        void utcid01_giveUpSuccessfully() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING));
            when(centerRequestFeeService.getPayment(REQUEST_ID))
                    .thenReturn(Optional.of(feePayment(CenterRequestFeeStatus.HELD)));

            service.giveUpClassRequest(REQUEST_ID, "Het gia su ranh");

            verify(classRequestStore).save(any());
            verify(centerRequestFeeService).requestRefund(REQUEST_ID, "Het gia su ranh");
            verify(notificationDispatchService).notifyUserFromTemplate(
                    any(), any(), any(), any(),
                    org.mockito.ArgumentMatchers.eq("Trung tâm chưa tìm được gia sư"),
                    anyString(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Phi xu ly chua thanh toan -> chi huy phi va dung lai")
        void utcid02_unpaidFeeIsCancelled() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING));
            when(centerRequestFeeService.getPayment(REQUEST_ID))
                    .thenReturn(Optional.of(feePayment(CenterRequestFeeStatus.PENDING_PAYMENT)));

            service.giveUpClassRequest(REQUEST_ID, "Het gia su ranh");

            verify(centerRequestFeeService).cancelUnpaid(REQUEST_ID);
            verify(centerRequestFeeService, never()).requestRefund(anyString(), any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.giveUpClassRequest(REQUEST_ID, "ly do"));
        }

        @Test
        @DisplayName("UTCID04 (A) - requestId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu mở lớp'")
        void utcid04_requestNotFound() {
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.giveUpClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Không tìm thấy yêu cầu mở lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau thuoc trung tam khac -> 'Không có quyền xử lý yêu cầu này'")
        void utcid05_requestOfAnotherCenter() {
            givenRequest(requestData(999L, ClassRequestStore.STATUS_SEARCHING));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.giveUpClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Không có quyền xử lý yêu cầu này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Yeu cau da duoc xu ly -> 'Yêu cầu này đã được xử lý'")
        void utcid06_alreadyProcessed() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_ACCEPTED));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.giveUpClassRequest(REQUEST_ID, "ly do"));
            assertEquals("Yêu cầu này đã được xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (B) - reason rong -> dung ghi chu mac dinh 'Trung tâm không tìm được gia sư phù hợp.'")
        void utcid07_defaultReason() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING));
            when(centerRequestFeeService.getPayment(REQUEST_ID))
                    .thenReturn(Optional.of(feePayment(CenterRequestFeeStatus.HELD)));

            service.giveUpClassRequest(REQUEST_ID, "   ");

            verify(centerRequestFeeService)
                    .requestRefund(REQUEST_ID, "Trung tâm không tìm được gia sư phù hợp.");
        }
    }

    // ===================================================================
    //  Sheet: createRecruitmentPost
    // ===================================================================
    @Nested
    @DisplayName("createRecruitmentPost")
    class CreateRecruitmentPost {

        @BeforeEach
        void initCenterState() {
            center.setVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            com.tcs.module.finance.entity.Wallet wallet = new com.tcs.module.finance.entity.Wallet();
            wallet.setWalletId(CENTER_USER_ID);
            wallet.setStatus(com.tcs.module.finance.enums.WalletStatus.ACTIVE);
            when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(wallet));
            when(recruitmentPostRepository.save(any(com.tcs.module.center.entity.RecruitmentPost.class)))
                    .thenAnswer(i -> {
                        var post = (com.tcs.module.center.entity.RecruitmentPost) i.getArgument(0);
                        post.setRecruitmentId(300L);
                        post.setCenter(center);
                        return post;
                    });
        }

        private com.tcs.module.center.dto.request.SaveRecruitmentPostRequest postRequest() {
            var request = new com.tcs.module.center.dto.request.SaveRecruitmentPostRequest();
            request.setTitle("Tuyen gia su Toan 9");
            request.setDescription("Day Toan lop 9 ca toi, 3 buoi mot tuan");
            request.setMaxPositions(2);
            request.setRequiredExperience(1);
            return request;
        }

        /** Lop "theo yeu cau" (EXTERNAL) chua co gia su chinh -> duoc phep gan tin tuyen. */
        private void givenRecruitableClass() {
            var origin = new com.tcs.module.catalog.entity.SystemParameter();
            origin.setParamKey("classorigin:" + CLASS_ID);
            origin.setParamValue("EXTERNAL");
            when(systemParameterRepository.findByParamKey("classorigin:" + CLASS_ID))
                    .thenReturn(Optional.of(origin));
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("UTCID01 (N) - Trung tam da xac minh, khong gan lop -> luu tin o trang thai DRAFT")
        void utcid01_createDraftPost() {
            service.createRecruitmentPost(postRequest());

            org.mockito.ArgumentCaptor<com.tcs.module.center.entity.RecruitmentPost> captor =
                    org.mockito.ArgumentCaptor.forClass(com.tcs.module.center.entity.RecruitmentPost.class);
            verify(recruitmentPostRepository).save(captor.capture());
            assertEquals(com.tcs.module.center.enums.RecruitmentPostStatus.DRAFT,
                    captor.getValue().getStatus(), "Tin moi luon la nhap");
            assertEquals("Tuyen gia su Toan 9", captor.getValue().getTitle());
        }

        @Test
        @DisplayName("UTCID02 (N) - Gan lop theo yeu cau chua co gia su chinh -> luu tin va lien ket voi lop")
        void utcid02_createPostLinkedToRequestClass() {
            givenRecruitableClass();
            var request = postRequest();
            request.setClassId(CLASS_ID);

            service.createRecruitmentPost(request);

            verify(recruitmentPostRepository).save(any(com.tcs.module.center.entity.RecruitmentPost.class));
            verify(systemParameterRepository, org.mockito.Mockito.atLeastOnce())
                    .save(any(com.tcs.module.catalog.entity.SystemParameter.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.createRecruitmentPost(postRequest()));
            verify(recruitmentPostRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - Trung tam chua duoc xac minh -> VerificationRequiredException")
        void utcid04_centerNotVerified() {
            center.setVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.UNDER_VERIFY);

            var ex = assertThrows(com.tcs.exception.VerificationRequiredException.class,
                    () -> service.createRecruitmentPost(postRequest()));
            assertEquals("Trung tâm của bạn cần được xác minh trước khi thực hiện thao tác này.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - title rong -> 'Tiêu đề là bắt buộc'")
        void utcid05_blankTitle() {
            var request = postRequest();
            request.setTitle("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Tiêu đề là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - description rong -> 'Mô tả công việc là bắt buộc'")
        void utcid06_blankDescription() {
            var request = postRequest();
            request.setDescription(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Mô tả công việc là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (B) - maxPositions = 0 (duoi can duoi) -> 'Số lượng cần tuyển phải là số nguyên dương'")
        void utcid07_zeroMaxPositions() {
            var request = postRequest();
            request.setMaxPositions(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Số lượng cần tuyển phải là số nguyên dương", ex.getMessage());
            verify(recruitmentPostRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID08 (B) - requiredExperience = -1 (duoi can duoi) -> 'Số năm kinh nghiệm không được âm'")
        void utcid08_negativeExperience() {
            var request = postRequest();
            request.setRequiredExperience(-1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Số năm kinh nghiệm không được âm", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Co dia chi nhung thieu tinh/thanh -> 'Vui lòng chọn Tỉnh/Thành phố cho địa chỉ đã nhập'")
        void utcid09_addressWithoutProvince() {
            var request = postRequest();
            request.setAddressDetail("So 1 Dai Co Viet");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Vui lòng chọn Tỉnh/Thành phố cho địa chỉ đã nhập", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - Lop gan kem la lop trung tam tu tao -> 'Lớp tự tạo chỉ gán gia sư từ danh sách trung tâm ...'")
        void utcid10_selfCreatedClassCannotBeRecruited() {
            when(systemParameterRepository.findByParamKey("classorigin:" + CLASS_ID))
                    .thenReturn(Optional.empty()); // mac dinh la SELF
            var request = postRequest();
            request.setClassId(CLASS_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Lớp tự tạo chỉ gán gia sư từ danh sách trung tâm, không đăng tin tuyển.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - Lop gan kem da co gia su chinh -> 'Lớp đã có gia sư — không cần đăng tin tuyển.'")
        void utcid11_classAlreadyHasMainTutor() {
            givenRecruitableClass();
            ClassAssignment assignment = new ClassAssignment();
            assignment.setAssignmentId(700L);
            assignment.setStatus(ClassAssignmentStatus.ACTIVE);
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
            var request = postRequest();
            request.setClassId(CLASS_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createRecruitmentPost(request));
            assertEquals("Lớp đã có gia sư — không cần đăng tin tuyển.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID12 (B) - requiredExperience = 0 (dung can duoi) -> chap nhan")
        void utcid12_zeroExperienceAccepted() {
            var request = postRequest();
            request.setRequiredExperience(0);

            service.createRecruitmentPost(request);

            org.mockito.ArgumentCaptor<com.tcs.module.center.entity.RecruitmentPost> captor =
                    org.mockito.ArgumentCaptor.forClass(com.tcs.module.center.entity.RecruitmentPost.class);
            verify(recruitmentPostRepository).save(captor.capture());
            assertEquals(0, captor.getValue().getRequiredExperience());
        }
    }

    // ===================================================================
    //  Sheet: crProposeTutor (trung tam de cu gia su cho yeu cau cua phu huynh)
    // ===================================================================
    @Nested
    @DisplayName("crProposeTutor")
    class CrProposeTutor {

        private void givenMembership() {
            var membership = new com.tcs.module.center.entity.CenterTutorMembership();
            membership.setMembershipId(600L);
            membership.setCenter(center);
            membership.setStatus(com.tcs.module.center.enums.CenterTutorMembershipStatus.ACTIVE);
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(CENTER_ID, TUTOR_ID))
                    .thenReturn(Optional.of(membership));
        }

        private void givenCandidates(ClassRequestStore.ClassRequestData data, List<Long> candidates) {
            when(classRequestStore.candidatesOf(data))
                    .thenReturn(new java.util.ArrayList<>(candidates));
            when(classRequestStore.withCandidates(any(), any())).thenReturn(data);
            when(classRequestStore.withStatus(any(), anyString(), any())).thenReturn(data);
        }

        @Test
        @DisplayName("UTCID01 (N) - Yeu cau dang SEARCHING, gia su thuoc doi -> them vao danh sach de cu")
        void utcid01_proposeOnSearchingRequest() {
            var data = requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING);
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
            givenMembership();
            givenCandidates(data, List.of());

            service.proposeTutor(REQUEST_ID, TUTOR_ID);

            org.mockito.ArgumentCaptor<List<Long>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(classRequestStore).withCandidates(any(), captor.capture());
            assertTrue(captor.getValue().contains(TUTOR_ID));
            verify(classRequestStore).save(any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Yeu cau dang PENDING -> them de cu va chuyen sang SEARCHING")
        void utcid02_pendingBecomesSearching() {
            var data = requestData(CENTER_ID, ClassRequestStore.STATUS_PENDING);
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
            givenMembership();
            givenCandidates(data, List.of());

            service.proposeTutor(REQUEST_ID, TUTOR_ID);

            verify(classRequestStore).withStatus(
                    any(),
                    org.mockito.ArgumentMatchers.eq(ClassRequestStore.STATUS_SEARCHING),
                    org.mockito.ArgumentMatchers.isNull());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            verify(classRequestStore, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - requestId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu mở lớp'")
        void utcid04_requestNotFound() {
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            assertEquals("Không tìm thấy yêu cầu mở lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau thuoc trung tam khac -> 'Không có quyền xử lý yêu cầu này'")
        void utcid05_requestOfAnotherCenter() {
            when(classRequestStore.find(REQUEST_ID))
                    .thenReturn(Optional.of(requestData(999L, ClassRequestStore.STATUS_SEARCHING)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            assertEquals("Không có quyền xử lý yêu cầu này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Yeu cau dang PAYMENT_PENDING -> 'Yêu cầu này chưa sẵn sàng để xử lý'")
        void utcid06_paymentPending() {
            when(classRequestStore.find(REQUEST_ID))
                    .thenReturn(Optional.of(requestData(CENTER_ID, ClassRequestStore.STATUS_PAYMENT_PENDING)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            assertEquals("Yêu cầu này chưa sẵn sàng để xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Yeu cau da CANCELLED -> 'Yêu cầu này chưa sẵn sàng để xử lý'")
        void utcid07_cancelled() {
            when(classRequestStore.find(REQUEST_ID))
                    .thenReturn(Optional.of(requestData(CENTER_ID, ClassRequestStore.STATUS_CANCELLED)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            assertEquals("Yêu cầu này chưa sẵn sàng để xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Yeu cau da ACCEPTED / REJECTED -> 'Yêu cầu này đã kết thúc, không thể đề cử thêm.'")
        void utcid08_alreadyClosed() {
            when(classRequestStore.find(REQUEST_ID))
                    .thenReturn(Optional.of(requestData(CENTER_ID, ClassRequestStore.STATUS_ACCEPTED)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            assertEquals("Yêu cầu này đã kết thúc, không thể đề cử thêm.", ex.getMessage());

            when(classRequestStore.find(REQUEST_ID))
                    .thenReturn(Optional.of(requestData(CENTER_ID, ClassRequestStore.STATUS_REJECTED)));
            assertThrows(IllegalArgumentException.class, () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
        }

        @Test
        @DisplayName("UTCID09 (A) - Gia su khong thuoc doi cua trung tam -> 'Gia sư không thuộc đội của trung tâm.'")
        void utcid09_tutorNotInCenterTeam() {
            when(classRequestStore.find(REQUEST_ID))
                    .thenReturn(Optional.of(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING)));
            when(membershipRepository.findFirstByCenter_CenterIdAndTutor_TutorId(CENTER_ID, TUTOR_ID))
                    .thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.proposeTutor(REQUEST_ID, TUTOR_ID));
            assertEquals("Gia sư không thuộc đội của trung tâm.", ex.getMessage());
            verify(classRequestStore, never()).save(any());
        }

        @Test
        @DisplayName("UTCID10 (B) - Gia su da co trong danh sach de cu -> khong them trung lap")
        void utcid10_tutorAlreadyProposed() {
            var data = requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING);
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
            givenMembership();
            givenCandidates(data, List.of(TUTOR_ID));

            service.proposeTutor(REQUEST_ID, TUTOR_ID);

            org.mockito.ArgumentCaptor<List<Long>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(classRequestStore).withCandidates(any(), captor.capture());
            assertEquals(1, captor.getValue().size(), "Khong duoc them trung gia su da de cu");
        }
    }

    // ===================================================================
    //  Sheet: ceCreateClass & ceUpdateClass (lop cua trung tam - UC-14-B)
    // ===================================================================

    /** Yeu cau tao/sua lop hop le hoan chinh: lich hang tuan Thu 2, 18:00-20:00. */
    private com.tcs.module.center.dto.request.SaveClassRequest classRequest() {
        var request = new com.tcs.module.center.dto.request.SaveClassRequest();
        request.setTitle("Toan 9 - ca toi");
        request.setDescription("Lop Toan 9 on thi vao 10");
        request.setCategoryName("Hoc thuat");
        request.setSubjectName("Toan");
        request.setGradeName("Lop 9");
        request.setProvinceName("Ha Noi");
        request.setWardName("Bach Khoa");
        request.setAddressDetail("So 1 Dai Co Viet");
        request.setLessonMode(com.tcs.module.marketplace.enums.LessonMode.OFFLINE);
        request.setRecurringType(com.tcs.module.marketplace.enums.RecurringType.WEEKLY);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(2));
        request.setTuitionFee(new BigDecimal("1500000"));
        request.setMaxStudents(10);
        request.setMinStudents(2);
        request.setSchedule(new java.util.ArrayList<>(List.of(slot(1, 18, 20))));
        return request;
    }

    private com.tcs.module.center.dto.request.ScheduleSlotRequest slot(
            Integer dayOfWeek, int startHour, int endHour) {
        var s = new com.tcs.module.center.dto.request.ScheduleSlotRequest();
        s.setDayOfWeek(dayOfWeek);
        s.setStartTime(java.time.LocalTime.of(startHour, 0));
        s.setEndTime(java.time.LocalTime.of(endHour, 0));
        return s;
    }

    @Nested
    @DisplayName("ceCreateClass")
    class CeCreateClass {

        @BeforeEach
        void initVerifiedCenter() {
            center.setVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            var wallet = new com.tcs.module.finance.entity.Wallet();
            wallet.setWalletId(CENTER_USER_ID);
            wallet.setStatus(com.tcs.module.finance.enums.WalletStatus.ACTIVE);
            when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(wallet));
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    anyLong(), any())).thenReturn(Optional.empty());
            when(substitutionService.findAssistant(anyLong())).thenReturn(Optional.empty());
            when(scheduleSlotRepository.findByTutoringClass_ClassId(anyLong())).thenReturn(List.of());
        }

        /** Chay createClass va tra ve lop da luu. */
        private TutoringClass create(com.tcs.module.center.dto.request.SaveClassRequest request) {
            service.createClass(request);
            org.mockito.ArgumentCaptor<TutoringClass> captor =
                    org.mockito.ArgumentCaptor.forClass(TutoringClass.class);
            verify(tutoringClassRepository).save(captor.capture());
            return captor.getValue();
        }

        private IllegalArgumentException expectReject(
                com.tcs.module.center.dto.request.SaveClassRequest request) {
            return assertThrows(IllegalArgumentException.class, () -> service.createClass(request));
        }

        @Test
        @DisplayName("UTCID01 (N) - Trung tam da xac minh, du lieu hop le -> luu lop CENTER trang thai DRAFT")
        void utcid01_createSuccessfully() {
            TutoringClass saved = create(classRequest());

            assertEquals(com.tcs.module.marketplace.enums.ClassType.CENTER, saved.getClassType());
            assertEquals(TutoringClassStatus.DRAFT, saved.getStatus());
            assertEquals(CENTER_USER_ID, saved.getCreator().getUserId());
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq(CENTER_USER_ID),
                    org.mockito.ArgumentMatchers.eq("CREATE_CENTER_CLASS"),
                    org.mockito.ArgumentMatchers.eq("TutoringClass"),
                    any(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid02_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.createClass(classRequest()));
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Trung tam chua duoc xac minh -> VerificationRequiredException")
        void utcid03_centerNotVerified() {
            center.setVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.UNDER_VERIFY);

            var ex = assertThrows(com.tcs.exception.VerificationRequiredException.class,
                    () -> service.createClass(classRequest()));
            assertEquals("Trung tâm của bạn cần được xác minh trước khi thực hiện thao tác này.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - title rong -> 'Tiêu đề là bắt buộc'")
        void utcid04_blankTitle() {
            var request = classRequest();
            request.setTitle("  ");
            assertEquals("Tiêu đề là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - categoryName rong -> 'Danh mục là bắt buộc'")
        void utcid05_blankCategory() {
            var request = classRequest();
            request.setCategoryName(null);
            assertEquals("Danh mục là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - subjectName rong -> 'Môn học là bắt buộc'")
        void utcid06_blankSubject() {
            var request = classRequest();
            request.setSubjectName(null);
            assertEquals("Môn học là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - gradeName rong -> 'Khối/lớp là bắt buộc'")
        void utcid07_blankGrade() {
            var request = classRequest();
            request.setGradeName(null);
            assertEquals("Khối/lớp là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - provinceName rong -> 'Vui lòng chọn Tỉnh/Thành phố'")
        void utcid08_blankProvince() {
            var request = classRequest();
            request.setProvinceName(null);
            assertEquals("Vui lòng chọn Tỉnh/Thành phố", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - wardName rong -> 'Vui lòng chọn Phường/Xã'")
        void utcid09_blankWard() {
            var request = classRequest();
            request.setWardName(null);
            assertEquals("Vui lòng chọn Phường/Xã", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - addressDetail rong -> 'Vui lòng nhập địa chỉ cụ thể'")
        void utcid10_blankAddress() {
            var request = classRequest();
            request.setAddressDetail("   ");
            assertEquals("Vui lòng nhập địa chỉ cụ thể", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - lessonMode = null -> 'Hình thức học là bắt buộc'")
        void utcid11_nullLessonMode() {
            var request = classRequest();
            request.setLessonMode(null);
            assertEquals("Hình thức học là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID12 (A) - recurringType = null -> 'Kiểu lặp lịch là bắt buộc'")
        void utcid12_nullRecurringType() {
            var request = classRequest();
            request.setRecurringType(null);
            assertEquals("Kiểu lặp lịch là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID13 (A) - recurringType = ONCE -> 'Kiểu lặp lịch chỉ được là Hằng ngày hoặc Hằng tuần'")
        void utcid13_recurringOnce() {
            var request = classRequest();
            request.setRecurringType(com.tcs.module.marketplace.enums.RecurringType.ONCE);
            assertEquals("Kiểu lặp lịch chỉ được là Hằng ngày hoặc Hằng tuần",
                    expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID14 (B) - tuitionFee = 0 (duoi can duoi) -> 'Học phí phải là số dương'")
        void utcid14_zeroTuitionFee() {
            var request = classRequest();
            request.setTuitionFee(BigDecimal.ZERO);
            assertEquals("Học phí phải là số dương", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID15 (B) - maxStudents = 0 (duoi can duoi) -> 'Số học sinh tối đa phải là số nguyên dương'")
        void utcid15_zeroMaxStudents() {
            var request = classRequest();
            request.setMaxStudents(0);
            assertEquals("Số học sinh tối đa phải là số nguyên dương", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID16 (B) - minStudents = 0 (duoi can duoi) -> 'Số học sinh tối thiểu phải là số nguyên dương'")
        void utcid16_zeroMinStudents() {
            var request = classRequest();
            request.setMinStudents(0);
            assertEquals("Số học sinh tối thiểu phải là số nguyên dương", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID17 (B) - minStudents = maxStudents + 1 (vuot can tren) -> chan")
        void utcid17_minAboveMax() {
            var request = classRequest();
            request.setMaxStudents(10);
            request.setMinStudents(11);
            assertEquals("Số học sinh tối thiểu không được lớn hơn số học sinh tối đa",
                    expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID18 (B) - minStudents = maxStudents (dung can tren) -> chap nhan")
        void utcid18_minEqualsMax() {
            var request = classRequest();
            request.setMaxStudents(10);
            request.setMinStudents(10);

            TutoringClass saved = create(request);

            assertEquals(10, saved.getMinStudents());
        }

        @Test
        @DisplayName("UTCID19 (A) - startDate hoac endDate = null -> 'Ngày bắt đầu và ngày kết thúc là bắt buộc'")
        void utcid19_missingDates() {
            var noStart = classRequest();
            noStart.setStartDate(null);
            assertEquals("Ngày bắt đầu và ngày kết thúc là bắt buộc", expectReject(noStart).getMessage());

            var noEnd = classRequest();
            noEnd.setEndDate(null);
            assertEquals("Ngày bắt đầu và ngày kết thúc là bắt buộc", expectReject(noEnd).getMessage());
        }

        @Test
        @DisplayName("UTCID20 (B) - startDate = hom qua (duoi can duoi) -> 'Ngày bắt đầu phải từ hôm nay trở đi'")
        void utcid20_startDateInThePast() {
            var request = classRequest();
            request.setStartDate(LocalDate.now().minusDays(1));
            assertEquals("Ngày bắt đầu phải từ hôm nay trở đi", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID21 (B) - startDate = hom nay (dung can duoi) -> chap nhan")
        void utcid21_startDateToday() {
            var request = classRequest();
            request.setStartDate(LocalDate.now());

            TutoringClass saved = create(request);

            assertEquals(LocalDate.now(), saved.getStartDate());
        }

        @Test
        @DisplayName("UTCID22 (B) - endDate = startDate -> 'Ngày kết thúc phải sau ngày bắt đầu'")
        void utcid22_endDateEqualsStartDate() {
            var request = classRequest();
            request.setEndDate(request.getStartDate());
            assertEquals("Ngày kết thúc phải sau ngày bắt đầu", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID23 (A) - schedule rong -> 'Cần ít nhất một khung lịch học'")
        void utcid23_emptySchedule() {
            var request = classRequest();
            request.setSchedule(List.of());
            assertEquals("Cần ít nhất một khung lịch học", expectReject(request).getMessage());

            var nullSchedule = classRequest();
            nullSchedule.setSchedule(null);
            assertEquals("Cần ít nhất một khung lịch học", expectReject(nullSchedule).getMessage());
        }

        @Test
        @DisplayName("UTCID24 (A) - Khung lich co gio ket thuc khong sau gio bat dau -> chan")
        void utcid24_invalidSlotTime() {
            var request = classRequest();
            request.setSchedule(List.of(slot(1, 20, 18)));
            assertEquals("Giờ kết thúc của khung lịch phải sau giờ bắt đầu",
                    expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID25 (B) - Lich hang tuan co dayOfWeek = 0 hoac 8 (ngoai can) -> chan")
        void utcid25_dayOfWeekOutOfRange() {
            var below = classRequest();
            below.setSchedule(List.of(slot(0, 18, 20)));
            assertEquals("Thứ trong tuần của khung lịch không hợp lệ (1-7)",
                    expectReject(below).getMessage());

            var above = classRequest();
            above.setSchedule(List.of(slot(8, 18, 20)));
            assertEquals("Thứ trong tuần của khung lịch không hợp lệ (1-7)",
                    expectReject(above).getMessage());
        }

        @Test
        @DisplayName("UTCID26 (A) - Lich hang ngay co hai tiet chong gio -> 'Các tiết trong ngày bị trùng/chồng giờ'")
        void utcid26_dailyOverlappingSlots() {
            var request = classRequest();
            request.setRecurringType(com.tcs.module.marketplace.enums.RecurringType.DAILY);
            request.setSchedule(List.of(slot(null, 18, 20), slot(null, 19, 21)));
            assertEquals("Các tiết trong ngày bị trùng/chồng giờ", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID27 (A) - Lich hang tuan hai khung cung thu bi chong gio -> chan")
        void utcid27_weeklyOverlappingSameDay() {
            var request = classRequest();
            request.setSchedule(List.of(slot(1, 18, 20), slot(1, 19, 21)));
            assertEquals("Lịch học bị trùng/chồng giờ giữa các khung trong cùng một ngày",
                    expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID28 (A) - Thu cua khung lich khong xuat hien trong khoang ngay hoc -> chan")
        void utcid28_weekdayOutsideDateRange() {
            var request = classRequest();
            LocalDate monday = LocalDate.now()
                    .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
            // Khoang hoc chi gom Thu 2 va Thu 3 -> khung Chu Nhat (7) khong bao gio roi vao.
            request.setStartDate(monday);
            request.setEndDate(monday.plusDays(1));
            request.setSchedule(List.of(slot(7, 18, 20)));

            assertEquals("Lịch học phải nằm trong khoảng ngày bắt đầu và ngày kết thúc",
                    expectReject(request).getMessage());
        }
    }

    @Nested
    @DisplayName("ceUpdateClass")
    class CeUpdateClass {

        @BeforeEach
        void initEditableClass() {
            tutoringClass.setStatus(TutoringClassStatus.DRAFT);
            when(classStudentRepository.existsByTutoringClass_ClassId(CLASS_ID)).thenReturn(false);
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    anyLong(), any())).thenReturn(Optional.empty());
            when(substitutionService.findAssistant(anyLong())).thenReturn(Optional.empty());
            when(scheduleSlotRepository.findByTutoringClass_ClassId(anyLong())).thenReturn(List.of());
        }

        private IllegalArgumentException expectReject(
                com.tcs.module.center.dto.request.SaveClassRequest request) {
            return assertThrows(IllegalArgumentException.class,
                    () -> service.updateClass(CLASS_ID, request));
        }

        @Test
        @DisplayName("UTCID01 (N) - Chu lop sua lop DRAFT chua co hoc vien -> ap dung thay doi va ghi audit log")
        void utcid01_updateDraftClass() {
            var request = classRequest();
            request.setTitle("Toan 9 - ca sang");

            service.updateClass(CLASS_ID, request);

            assertEquals("Toan 9 - ca sang", tutoringClass.getTitle());
            verify(tutoringClassRepository).save(tutoringClass);
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq(CENTER_USER_ID),
                    org.mockito.ArgumentMatchers.eq("UPDATE_CENTER_CLASS"),
                    org.mockito.ArgumentMatchers.eq("TutoringClass"),
                    any(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Lop OPEN chua co hoc vien -> van sua duoc")
        void utcid02_updateOpenClass() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);

            service.updateClass(CLASS_ID, classRequest());

            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai tai khoan trung tam -> requireCenter chan lai")
        void utcid03_callerIsNotACenter() {
            rejectCenterRole();

            assertThrows(ForbiddenException.class, () -> service.updateClass(CLASS_ID, classRequest()));
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid04_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateClass(CLASS_ID, classRequest()));
        }

        @Test
        @DisplayName("UTCID05 (A) - Lop cua trung tam khac -> 'Bạn không có quyền chỉnh sửa lớp học này'")
        void utcid05_notOwner() {
            User otherCenterUser = new User();
            otherCenterUser.setUserId(999L);
            tutoringClass.setCreator(otherCenterUser);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.updateClass(CLASS_ID, classRequest()));
            assertEquals("Bạn không có quyền chỉnh sửa lớp học này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Lop khong o DRAFT / OPEN -> 'Lớp học này không thể chỉnh sửa nữa.'")
        void utcid06_statusNotEditable() {
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);

            assertEquals("Lớp học này không thể chỉnh sửa nữa.", expectReject(classRequest()).getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Da co hoc vien dang ky -> 'Đã có học viên đăng ký, không thể chỉnh sửa lớp nữa.'")
        void utcid07_studentAlreadyEnrolled() {
            when(classStudentRepository.existsByTutoringClass_ClassId(CLASS_ID)).thenReturn(true);

            assertEquals("Đã có học viên đăng ký, không thể chỉnh sửa lớp nữa.",
                    expectReject(classRequest()).getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID08 (A) - title rong -> 'Tiêu đề là bắt buộc'")
        void utcid08_blankTitle() {
            var request = classRequest();
            request.setTitle("  ");
            assertEquals("Tiêu đề là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - subjectName rong -> 'Môn học là bắt buộc'")
        void utcid09_blankSubject() {
            var request = classRequest();
            request.setSubjectName(null);
            assertEquals("Môn học là bắt buộc", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID10 (B) - startDate = hom qua -> VAN CHAP NHAN khi sua (luat ngay qua khu chi ap khi tao)")
        void utcid10_pastStartDateAcceptedOnUpdate() {
            var request = classRequest();
            request.setStartDate(LocalDate.now().minusDays(7));
            request.setEndDate(LocalDate.now().plusMonths(1));

            service.updateClass(CLASS_ID, request);

            assertEquals(LocalDate.now().minusDays(7), tutoringClass.getStartDate(),
                    "isCreate = false nen khong chan ngay bat dau trong qua khu");
            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID11 (B) - endDate = startDate -> 'Ngày kết thúc phải sau ngày bắt đầu'")
        void utcid11_endDateEqualsStartDate() {
            var request = classRequest();
            request.setEndDate(request.getStartDate());
            assertEquals("Ngày kết thúc phải sau ngày bắt đầu", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID12 (B) - tuitionFee = 0 -> 'Học phí phải là số dương'")
        void utcid12_zeroTuitionFee() {
            var request = classRequest();
            request.setTuitionFee(BigDecimal.ZERO);
            assertEquals("Học phí phải là số dương", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID13 (A) - schedule rong -> 'Cần ít nhất một khung lịch học'")
        void utcid13_emptySchedule() {
            var request = classRequest();
            request.setSchedule(List.of());
            assertEquals("Cần ít nhất một khung lịch học", expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID14 (A) - recurringType = ONCE -> 'Kiểu lặp lịch chỉ được là Hằng ngày hoặc Hằng tuần'")
        void utcid14_recurringOnce() {
            var request = classRequest();
            request.setRecurringType(com.tcs.module.marketplace.enums.RecurringType.ONCE);
            assertEquals("Kiểu lặp lịch chỉ được là Hằng ngày hoặc Hằng tuần",
                    expectReject(request).getMessage());
        }

        @Test
        @DisplayName("UTCID15 (B) - minStudents = maxStudents + 1 -> chan")
        void utcid15_minAboveMax() {
            var request = classRequest();
            request.setMaxStudents(10);
            request.setMinStudents(11);
            assertEquals("Số học sinh tối thiểu không được lớn hơn số học sinh tối đa",
                    expectReject(request).getMessage());
        }
    }
    // ===================================================================
    //  Sheet: acceptClassRequest
    // ===================================================================
    @Nested
    @DisplayName("acceptClassRequest")
    class AcceptClassRequest {

        @BeforeEach
        void initVerifiedCenter() {
            center.setVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            var wallet = new com.tcs.module.finance.entity.Wallet();
            wallet.setWalletId(CENTER_USER_ID);
            wallet.setStatus(com.tcs.module.finance.enums.WalletStatus.ACTIVE);
            when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(wallet));
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    anyLong(), any())).thenReturn(Optional.empty());
            when(substitutionService.findAssistant(anyLong())).thenReturn(Optional.empty());
            when(scheduleSlotRepository.findByTutoringClass_ClassId(anyLong())).thenReturn(List.of());
        }

        @Test
        @DisplayName("UTCID01 (N) - trung tam so huu yeu cau da san sang -> tao lop va chuyen sang buoc tim gia su")
        void utcid01_acceptSuccessfully() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_PENDING));

            service.acceptClassRequest(REQUEST_ID, classRequest());

            verify(tutoringClassRepository).save(any(TutoringClass.class));
            verify(centerRequestFeeService).linkFulfilledAssignment(
                    org.mockito.ArgumentMatchers.eq(REQUEST_ID), any(), org.mockito.ArgumentMatchers.isNull());
            verify(classRequestStore).save(any());
        }

        @Test
        @DisplayName("UTCID02 (A) - yeu cau thuoc trung tam khac -> ForbiddenException")
        void utcid02_requestOfAnotherCenter() {
            givenRequest(requestData(999L, ClassRequestStore.STATUS_PENDING));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.acceptClassRequest(REQUEST_ID, classRequest()));
            assertEquals("Không có quyền xử lý yêu cầu này", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - yeu cau chua san sang de xu ly (cho thanh toan) -> chan")
        void utcid03_notReadyYet() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_PAYMENT_PENDING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptClassRequest(REQUEST_ID, classRequest()));
            assertEquals("Yêu cầu này chưa sẵn sàng để xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - yeu cau da duoc xu ly -> chan")
        void utcid04_alreadyProcessed() {
            givenRequest(requestData(CENTER_ID, ClassRequestStore.STATUS_SEARCHING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptClassRequest(REQUEST_ID, classRequest()));
            assertEquals("Yêu cầu này đã được xử lý", ex.getMessage());
        }
    }
}
