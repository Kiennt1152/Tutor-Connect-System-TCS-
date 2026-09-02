package com.tcs.module.contract.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test module Contract — ký hợp đồng bằng OTP (dùng chung cho BF-03/04/05).
 * Bám bộ test case trong Report_5.1_UnitTest: các sheet sendOtp1, signWithOtp, sign, getMyContract.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceImplSignTest {

    private static final Long TUTOR_USER_ID = 200L;
    private static final Long CENTER_USER_ID = 100L;
    private static final Long STRANGER_USER_ID = 999L;
    private static final Long CONTRACT_ID = 900L;
    private static final String TUTOR_EMAIL = "tutor@tcs.local";

    @Mock private AuthHelper authHelper;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private EmailService emailService;
    @Mock private EscrowService escrowService;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ReviewRepository reviewRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ReputationHistoryRepository reputationHistoryRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private CccdService cccdService;
    @Mock private EmailOtpRepository emailOtpRepository;

    @InjectMocks private ContractServiceImpl service;

    private Contract contract;
    private ContractSignature tutorSignature;
    /** Mã OTP ký hợp đồng nay nằm ở bảng email_otps chứ không còn trên contract_signatures. */
    private EmailOtp activeOtp;

    @BeforeEach
    void setUp() {
        // OTP dùng chung một service duy nhất; test dựng service thật trên repository giả.
        ReflectionTestUtils.setField(service, "otpService", new OtpService(emailOtpRepository));

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail(TUTOR_EMAIL);
        Tutor tutor = new Tutor();
        tutor.setTutorId(20L);
        tutor.setUser(tutorUser);

        User centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        TutorCenter center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);

        RecruitmentPost post = new RecruitmentPost();
        post.setRecruitmentId(300L);
        post.setCenter(center);

        RecruitmentApplication app = new RecruitmentApplication();
        app.setRecruitmentAppId(400L);
        app.setTutor(tutor);
        app.setRecruitmentPost(post);

        contract = new Contract();
        contract.setContractId(CONTRACT_ID);
        contract.setContractNo("HD-001");
        contract.setStatus(ContractStatus.PENDING);
        contract.setRecruitmentApplication(app);

        tutorSignature = new ContractSignature();
        tutorSignature.setContract(contract);
        tutorSignature.setPartyRole(PartyRole.TUTOR);
        tutorSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        activeOtp = new EmailOtp();
        activeOtp.setEmail(TUTOR_EMAIL);
        activeOtp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        activeOtp.setCode("123456");
        activeOtp.setAttempts(0);
        activeOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        anyString(), any(OtpPurpose.class)))
                .thenReturn(Optional.of(activeOtp));

        // Mặc định: đăng nhập bằng gia sư, CCCD đầy đủ, không phải trẻ vị thành niên.
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated())
                .thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(cccdService.isComplete(anyLong())).thenReturn(true);
        when(clientRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));
    }

    private SignWithOtpRequest otp(String code) {
        SignWithOtpRequest r = new SignWithOtpRequest();
        r.setOtpCode(code);
        return r;
    }

    // ===================================================================
    //  Sheet: signWithOtp
    // ===================================================================
    @Nested
    @DisplayName("signWithOtp")
    class SignWithOtp {

        @Test
        @DisplayName("UTCID02 (A) - OTP rỗng -> 'Mã OTP là bắt buộc'")
        void utcid01_blankOtp() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("")));
            assertEquals("Mã OTP là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID02 (A) - OTP null -> 'Mã OTP là bắt buộc'")
        void utcid02_nullOtp() {
            assertThrows(IllegalArgumentException.class, () -> service.signWithOtp(CONTRACT_ID, otp(null)));
        }

        @Test
        @DisplayName("UTCID05 (A) - Người ký chưa hoàn thành CCCD -> chặn ký")
        void utcid03_cccdIncomplete() {
            when(cccdService.isComplete(TUTOR_USER_ID)).thenReturn(false);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertTrue(ex.getMessage().contains("CCCD"), "Thông báo phải nhắc hoàn thành CCCD: " + ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Tài khoản dưới 18 tuổi -> không được tự ký")
        void utcid04_minorCannotSign() {
            Client minor = new Client();
            minor.setDateOfBirth(LocalDate.now().minusYears(15));
            when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(minor));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertTrue(ex.getMessage().contains("dưới 18 tuổi"),
                    "Phải chặn minor tự ký: " + ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Hợp đồng đã SIGNED -> 'Hợp đồng không ở trạng thái chờ ký'")
        void utcid05_contractNotPending() {
            contract.setStatus(ContractStatus.SIGNED);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertEquals("Hợp đồng không ở trạng thái chờ ký", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Vai trò này đã ký rồi -> 'Bạn đã ký hợp đồng này rồi'")
        void utcid06_alreadySigned() {
            tutorSignature.setSignatureStatus(ContractSignatureStatus.SIGNED);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertEquals("Bạn đã ký hợp đồng này rồi", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - OTP đã hết hạn -> yêu cầu mã mới")
        void utcid07_otpExpired() {
            activeOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertEquals("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - OTP sai -> báo số lần thử còn lại")
        void utcid08_wrongOtp() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("000000")));
            assertTrue(ex.getMessage().startsWith("Mã OTP không đúng"),
                    "Phải báo sai OTP kèm số lần còn lại: " + ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (B) - Da nhap sai vuot qua so lan cho phep (attempts = 5) -> 'Đã vượt quá số lần thử. Vui lòng yêu cầu mã mới.'")
        void utcid09_maxAttemptsExceeded() {
            activeOtp.setAttempts(5); // OTP_MAX_ATTEMPTS = 5

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertEquals("Đã vượt quá số lần thử. Vui lòng yêu cầu mã mới.", ex.getMessage());
            assertEquals(ContractSignatureStatus.EXPIRED, tutorSignature.getSignatureStatus(),
                    "Het luot thu thi o ky phai chuyen sang EXPIRED");
        }

        @Test
        @DisplayName("UTCID10 (A) - Chưa gửi OTP cho vai trò này -> ResourceNotFoundException")
        void utcid10_noSignatureRow() {
            when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                    .thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertEquals("Chưa gửi mã OTP cho vai trò này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - Hợp đồng không tồn tại -> ResourceNotFoundException")
        void utcid11_contractNotFound() {
            when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.signWithOtp(CONTRACT_ID, otp("123456")));
            assertEquals("Không tìm thấy hợp đồng", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: sendOtp1 (ContractService.sendOtp)
    // ===================================================================
    @Nested
    @DisplayName("ctSendOtp")
    class SendOtp {

        @Test
        @DisplayName("UTCID02 (A) - Hợp đồng đã SIGNED -> 'Hợp đồng không ở trạng thái chờ ký'")
        void utcid01_contractNotPending() {
            contract.setStatus(ContractStatus.SIGNED);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.sendOtp(CONTRACT_ID));
            assertEquals("Hợp đồng không ở trạng thái chờ ký", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Vai trò đã ký rồi -> 'Bạn đã ký hợp đồng này rồi'")
        void utcid02_alreadySigned() {
            tutorSignature.setSignatureStatus(ContractSignatureStatus.SIGNED);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.sendOtp(CONTRACT_ID));
            assertEquals("Bạn đã ký hợp đồng này rồi", ex.getMessage());
        }

        @Test
        @DisplayName("Bổ sung ngoài các UTCID của sheet ctSendOtp - Hợp đồng không tồn tại -> ResourceNotFoundException")
        void utcid03_contractNotFound() {
            when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.sendOtp(CONTRACT_ID));
        }
    }

    // ===================================================================
    //  Sheet: sign (ký thay người khác)
    // ===================================================================
    @Nested
    @DisplayName("ctSign")
    class Sign {

        @Test
        @DisplayName("UTCID02 (A) - Ký thay user khác -> 'Không thể ký thay người dùng khác'")
        void utcid01_cannotSignForOthers() {
            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.sign(CONTRACT_ID, "123456", STRANGER_USER_ID));
            assertEquals("Không thể ký thay người dùng khác", ex.getMessage());
            verify(contractSignatureRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: generateStudentContract (BF-04 buoc 7)
    // ===================================================================
    @Nested
    @DisplayName("generateStudentContract")
    class GenerateStudentContract {

        private static final Long CLASS_STUDENT_ID = 700L;
        private static final Long CLASS_ID = 500L;

        private com.tcs.module.marketplace.entity.ClassStudent enrollment() {
            User creator = new User();
            creator.setUserId(CENTER_USER_ID);
            creator.setEmail("center@tcs.local");

            com.tcs.module.marketplace.entity.TutoringClass cls =
                    new com.tcs.module.marketplace.entity.TutoringClass();
            cls.setClassId(CLASS_ID);
            cls.setTitle("Toan 9 - Ca toi");
            cls.setCreator(creator);
            cls.setStartDate(LocalDate.now());
            cls.setEndDate(LocalDate.now().plusMonths(3));

            com.tcs.module.marketplace.entity.ClassStudent cs =
                    new com.tcs.module.marketplace.entity.ClassStudent();
            cs.setClassStudentId(CLASS_STUDENT_ID);
            cs.setTutoringClass(cls);
            cs.setStudentName("Nguyen Van A");
            return cs;
        }

        private void givenNoTemplateConfigured() {
            when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.empty());
            when(contractTemplateRepository.findByStatus(
                    com.tcs.module.contract.enums.ContractTemplateStatus.ACTIVE)).thenReturn(java.util.List.of());
            when(tutorCenterRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
            when(contractRepository.countTodayContracts()).thenReturn(0L);
        }

        @Test
        @DisplayName("UTCID01 (N) - Ghi danh ton tai, chua co hop dong -> tao hop dong PENDING nguon CENTER")
        void utcid01_createsNewContract() {
            com.tcs.module.marketplace.entity.ClassStudent cs = enrollment();
            when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.of(cs));
            when(contractRepository.findByClassStudent_ClassStudentId(CLASS_STUDENT_ID))
                    .thenReturn(Optional.empty());
            givenNoTemplateConfigured();
            when(contractRepository.save(any(Contract.class))).thenAnswer(i -> {
                Contract c = i.getArgument(0);
                c.setContractId(1001L);
                return c;
            });

            service.generateStudentContract(CLASS_STUDENT_ID);

            org.mockito.ArgumentCaptor<Contract> captor =
                    org.mockito.ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(captor.capture());
            Contract saved = captor.getValue();
            assertEquals(ContractStatus.PENDING, saved.getStatus());
            assertEquals(com.tcs.module.contract.enums.ContractSourceType.CENTER, saved.getSourceType());
            assertEquals(CLASS_STUDENT_ID, saved.getClassStudent().getClassStudentId());
            assertTrue(saved.getTermsSummary() != null && !saved.getTermsSummary().isBlank(),
                    "Dieu khoan phai duoc dong bang vao hop dong");
            // Trung tam ky san + o ky cho phia nguoi ghi danh.
            verify(contractSignatureRepository, org.mockito.Mockito.times(2)).save(any(ContractSignature.class));
        }

        @Test
        @DisplayName("UTCID02 (N) - Ghi danh da co hop dong -> tra ve hop dong cu, khong tao trung (idempotent)")
        void utcid02_returnsExistingContract() {
            com.tcs.module.marketplace.entity.ClassStudent cs = enrollment();
            Contract existing = new Contract();
            existing.setContractId(2002L);
            existing.setContractNo("TCS-20260101-0001");
            existing.setStatus(ContractStatus.PENDING);
            existing.setClassStudent(cs);

            when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.of(cs));
            when(contractRepository.findByClassStudent_ClassStudentId(CLASS_STUDENT_ID))
                    .thenReturn(Optional.of(existing));
            when(tutorCenterRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());

            var res = service.generateStudentContract(CLASS_STUDENT_ID);

            assertEquals(2002L, res.getContractId());
            verify(contractRepository, never()).save(any(Contract.class));
            verify(contractSignatureRepository, never()).save(any(ContractSignature.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - classStudentId khong ton tai -> 'Không tìm thấy học viên trong lớp'")
        void utcid03_enrollmentNotFound() {
            when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.generateStudentContract(CLASS_STUDENT_ID));
            assertEquals("Không tìm thấy học viên trong lớp", ex.getMessage());
            verify(contractRepository, never()).save(any(Contract.class));
        }
    }
    // ===================================================================
    //  Cac ca happy-path con thieu cua signWithOtp / ctSendOtp / ctSign
    // ===================================================================

    /**
     * Sheet signWithOtp - UTCID01 (N): nguoi ky du 18 tuoi, co CCCD, hop dong dang cho ky,
     * OTP dung va con han -> ghi nhan chu ky cho vai tro tuong ung.
     */
    @Test
    void signWithOtpRecordsSignatureOnHappyPath() {
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(
                contract.getRecruitmentApplication().getTutor().getUser()));
        when(contractSignatureRepository.save(any(ContractSignature.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(0);
        when(contractSignatureRepository.findByContract_ContractId(CONTRACT_ID))
                .thenReturn(java.util.List.of(tutorSignature));
        when(cccdService.getByUserId(any()))
                .thenReturn(new com.tcs.module.profile.dto.CccdInfoDto());

        service.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
        assertNotNull(tutorSignature.getSignedAt());
        assertTrue(tutorSignature.getSignatureData().startsWith("OTP_VERIFIED:"));
        assertNotNull(activeOtp.getConsumedAt(), "OTP phai duoc danh dau da dung");
    }

    /**
     * Sheet ctSendOtp - UTCID01 (N): hop dong dang cho ky va vai tro nay chua ky
     * -> sinh ma OTP, gui email va tra ve thoi han hieu luc cua ma.
     */
    @Test
    void sendOtpIssuesCodeOnHappyPath() {
        when(contractSignatureRepository.save(any(ContractSignature.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(emailOtpRepository.save(any(EmailOtp.class))).thenAnswer(i -> {
            EmailOtp saved = i.getArgument(0);
            if (saved.getOtpId() == null) {
                saved.setOtpId(3300L);
            }
            return saved;
        });
        when(cccdService.getByUserId(any()))
                .thenReturn(new com.tcs.module.profile.dto.CccdInfoDto());

        var result = service.sendOtp(CONTRACT_ID);

        assertNotNull(result);
        assertEquals(TUTOR_EMAIL, tutorSignature.getEmail());
        verify(emailOtpRepository, org.mockito.Mockito.atLeastOnce()).save(any(EmailOtp.class));
    }

    /**
     * Sheet ctSign - UTCID01 (N): signerUserId trung nguoi dang dang nhap
     * -> chuyen tiep sang luong ky bang OTP cua dung vai tro.
     */
    @Test
    void signDelegatesToOtpFlowForOwnUser() {
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(
                contract.getRecruitmentApplication().getTutor().getUser()));
        when(contractSignatureRepository.save(any(ContractSignature.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(0);
        when(contractSignatureRepository.findByContract_ContractId(CONTRACT_ID))
                .thenReturn(java.util.List.of(tutorSignature));
        when(cccdService.getByUserId(any()))
                .thenReturn(new com.tcs.module.profile.dto.CccdInfoDto());

        service.sign(CONTRACT_ID, "123456", TUTOR_USER_ID);

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
    }
}
