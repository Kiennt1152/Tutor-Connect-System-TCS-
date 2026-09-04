package com.tcs.module.contract.service.impl;

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

import com.tcs.common.event.CooperationContractSigned;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52ContractOtpServiceITTest {

    private static final Long TUTOR_USER_ID = 200L;
    private static final Long STRANGER_USER_ID = 999L;
    private static final Long CONTRACT_ID = 900L;
    private static final String TUTOR_EMAIL = "tutor.it@tcs.test";

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

    @InjectMocks private ContractServiceImpl contractService;

    private Contract contract;
    private ContractSignature tutorSignature;
    private EmailOtp activeOtp;

    @BeforeEach
    void setUpContractOtpItFixture() {
        ReflectionTestUtils.setField(contractService, "otpService", new OtpService(emailOtpRepository));

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail(TUTOR_EMAIL);
        Tutor tutor = new Tutor();
        tutor.setTutorId(20L);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư IT");

        User centerUser = new User();
        centerUser.setUserId(100L);
        centerUser.setEmail("center.it@tcs.test");
        TutorCenter center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);
        center.setCompanyName("Trung tâm IT");
        center.setAddress("Hà Nội");
        center.setPhone("0900000000");
        RecruitmentPost post = new RecruitmentPost();
        post.setRecruitmentId(300L);
        post.setCenter(center);
        RecruitmentApplication application = new RecruitmentApplication();
        application.setRecruitmentAppId(400L);
        application.setTutor(tutor);
        application.setRecruitmentPost(post);

        contract = new Contract();
        contract.setContractId(CONTRACT_ID);
        contract.setContractNo("HD-IT-001");
        contract.setStatus(ContractStatus.PENDING);
        contract.setRecruitmentApplication(application);

        tutorSignature = new ContractSignature();
        tutorSignature.setSignatureId(1L);
        tutorSignature.setContract(contract);
        tutorSignature.setPartyRole(PartyRole.TUTOR);
        tutorSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        tutorSignature.setEmail(TUTOR_EMAIL);

        activeOtp = new EmailOtp();
        activeOtp.setEmail(TUTOR_EMAIL);
        activeOtp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        activeOtp.setCode("123456");
        activeOtp.setAttempts(0);
        activeOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(cccdService.isComplete(TUTOR_USER_ID)).thenReturn(true);
        when(cccdService.getByUserId(anyLong())).thenReturn(CccdInfoDto.builder()
                .fullName("Nguyễn Văn IT")
                .cccdNumber("012345678901")
                .dateOfBirth("01/01/1999")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build());
        when(clientRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                anyString(),
                any(OtpPurpose.class)))
                .thenReturn(Optional.of(activeOtp));
    }

    @Test
    @Tag("report52-it")
    void IT_CT_001_SuccessfulOtpSignatureStoresSignerAndSignatureData() {
        User tutorUser = tutorUser();
        ContractSignature centerSignature = signedCenterSignature();

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(1);

        contractService.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
        assertEquals(tutorUser, tutorSignature.getSigner());
        assertNotNull(tutorSignature.getSignedAt());
        assertTrue(tutorSignature.getSignatureData().startsWith("OTP_VERIFIED:" + TUTOR_EMAIL));
        verify(contractSignatureRepository).save(tutorSignature);
    }

    @Test
    @Tag("report52-it")
    void IT_CT_002_GetSignatureListReturnsSignedAndPendingSlotsForContractPage() {
        ContractSignature centerSignature = signedCenterSignature();
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, centerSignature));

        var response = contractService.getSignatures(CONTRACT_ID);

        assertEquals(CONTRACT_ID, response.getContractId());
        assertEquals(2, response.getRequiredSignatures());
        assertEquals(1, response.getSignedCount());
        assertEquals(false, response.isFullySigned());
        assertEquals(true, response.getSignatures().get(0).isCurrentUser());
        assertEquals("Gia sư", response.getSignatures().get(0).getPartyLabel());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_003_GetSignatureStatusReturnsOnlySignedRowsWithCurrentUserFlag() {
        ContractSignature centerSignature = signedCenterSignature();
        ContractSignature signedTutor = signedTutorSignature(tutorUser());

        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(signedTutor, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID))
                .thenReturn(Optional.of(contract.getRecruitmentApplication().getTutor()));

        var response = contractService.getSignatureStatus(CONTRACT_ID);

        assertEquals(true, response.isFullySigned());
        assertEquals(2, response.getSignedCount());
        assertEquals(2, response.getSignatures().size());
        assertEquals(true, response.getSignatures().stream()
                .anyMatch(signature -> signature.isCurrentUser()
                        && "Gia sư IT".equals(signature.getSignerName())));
    }

    @Test
    @Tag("report52-it")
    void IT_CT_004_RejectBlankOtpBeforeChangingSignatureState() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("")));

        assertEquals("Mã OTP là bắt buộc", exception.getMessage());
        verify(contractSignatureRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_005_RejectSigningWhenSignerCccdIsIncomplete() {
        when(cccdService.isComplete(TUTOR_USER_ID)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertTrue(exception.getMessage().contains("CCCD"));
        verify(contractSignatureRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_006_BlockAnonymousContractReadBeforeReturningDetail() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContract(CONTRACT_ID));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_007_BlockNonSignerFromReadingContractDetail() {
        User stranger = new User();
        stranger.setUserId(STRANGER_USER_ID);
        stranger.setEmail("stranger.it@tcs.test");

        when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(stranger, UserRole.CLIENT));
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, signedCenterSignature()));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContract(CONTRACT_ID));

        assertEquals("Bạn không có quyền xem hợp đồng này", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_002_GetMyContractsReturnsDeduplicatedPrivateContractRowsForSigner() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        privateContract.setStatus(ContractStatus.SIGNED);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser(), UserRole.TUTOR));
        stubContractListForUser(TUTOR_USER_ID, TUTOR_EMAIL, privateContract);
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(2);

        var responses = contractService.getMyContracts();

        assertEquals(1, responses.size());
        assertEquals(901L, responses.get(0).getContractId());
        assertEquals(800L, responses.get(0).getAssignmentId());
        assertEquals(TUTOR_USER_ID, responses.get(0).getTutorId());
        assertEquals("PRIVATE", responses.get(0).getClassType());
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_003_GetPrivateContractDetailReturnsClassPartiesAndTuition() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        User clientUser = privateContract.getAssignment().getApplication().getTutoringClass().getCreator();

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractRepository.findContractsByUserId(clientUser.getUserId())).thenReturn(List.of());
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(1);

        var response = contractService.getMyContract(901L);

        assertEquals("HD-PRIVATE-IT", response.getContractNo());
        assertEquals("Lớp private Toán 12", response.getClassTitle());
        assertEquals("client.it@tcs.test", response.getClientEmail());
        assertEquals(TUTOR_EMAIL, response.getTutorEmail());
        assertEquals(new BigDecimal("400000.00"), response.getEscrowAmount());
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_006_BlockAnonymousContractListBeforeReturningPrivateContracts() {
        when(authHelper.requireAuthenticated()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContracts());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(contractRepository, never()).findContractsByUserId(anyLong());
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_007_BlockUnrelatedUserFromPrivateContractDetail() {
        Contract privateContract = privateAssignmentContract();

        when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContract(901L));

        assertEquals("Bạn không có quyền xem hợp đồng này", exception.getMessage());
        verify(contractSignatureRepository, never()).countSignedByContractId(901L);
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_012_ReloadSignedContractReturnsExistingPendingEscrowQrPayment() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        User clientUser = privateContract.getAssignment().getApplication().getTutoringClass().getCreator();
        PaymentTransaction pendingPayment = pendingEscrowPayment("ESCROW-A800", new BigDecimal("400000.00"));

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractRepository.findContractsByUserId(clientUser.getUserId())).thenReturn(List.of());
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(2);
        when(paymentTransactionRepository.findByReferenceCode("ESCROW-A800"))
                .thenReturn(Optional.of(pendingPayment));

        var response = contractService.getMyContract(901L);

        assertNotNull(response.getEscrowPayment());
        assertEquals(PaymentTransactionStatus.PENDING, response.getEscrowPayment().getPaymentStatus());
        assertEquals("ESCROW-A800", response.getEscrowPayment().getTransferContent());
        assertTrue(response.getEscrowPayment().getQrUrl().contains("amount=400000"));
    }

    @Test
    @Tag("report52-it")
    void IT_CT_009_RejectOtpSigningForAlreadySignedContract() {
        contract.setStatus(ContractStatus.SIGNED);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertEquals("Hợp đồng không ở trạng thái chờ ký", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_010_OtpSignatureUpdatesTutorSignedAtOnPrivateAssignment() {
        Contract privateContract = privateAssignmentContract();
        ContractSignature privateTutorSignature = new ContractSignature();
        privateTutorSignature.setSignatureId(8L);
        privateTutorSignature.setContract(privateContract);
        privateTutorSignature.setPartyRole(PartyRole.TUTOR);
        privateTutorSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        privateTutorSignature.setEmail(TUTOR_EMAIL);

        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(901L, PartyRole.TUTOR))
                .thenReturn(Optional.of(privateTutorSignature));
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser()));
        when(contractSignatureRepository.findByContractId(901L)).thenReturn(List.of(privateTutorSignature));
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(1);

        contractService.signWithOtp(901L, otp("123456"));

        assertNotNull(privateContract.getAssignment().getTutorSignedAt());
        verify(classAssignmentRepository).save(privateContract.getAssignment());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_011_SendContractOtpEmailsSignerAndStoresPendingSignatureSlot() {
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));

        var response = contractService.sendOtp(CONTRACT_ID);

        assertEquals("Mã OTP đã được gửi đến email của bạn", response.get("message"));
        assertEquals("tu***@tcs.test", response.get("maskedEmail"));
        assertEquals(5, response.get("expiresInMinutes"));
        assertEquals(TUTOR_EMAIL, tutorSignature.getEmail());
        verify(emailOtpRepository, org.mockito.Mockito.atLeastOnce()).save(any(EmailOtp.class));
        verify(emailService).sendContractOtp(eq(TUTOR_EMAIL), anyString(), eq(contract.getContractNo()));
        verify(contractSignatureRepository).save(tutorSignature);
    }

    @Test
    @Tag("report52-it")
    void IT_CT_012_SentOtpKeepsSignaturePendingUntilUserSubmitsCode() {
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));

        contractService.sendOtp(CONTRACT_ID);

        assertEquals(ContractSignatureStatus.PENDING, tutorSignature.getSignatureStatus());
        assertEquals(TUTOR_EMAIL, tutorSignature.getEmail());
        verify(contractSignatureRepository).save(tutorSignature);
        verify(emailOtpRepository, org.mockito.Mockito.atLeastOnce()).save(any(EmailOtp.class));
        verify(contractRepository, never()).save(contract);
    }

    @Test
    @Tag("report52-it")
    void IT_CT_018_MarkSignatureExpiredWhenContractOtpExpires() {
        activeOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertEquals("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.", exception.getMessage());
        assertEquals(ContractSignatureStatus.EXPIRED, tutorSignature.getSignatureStatus());
        verify(contractSignatureRepository).save(tutorSignature);
    }

    @Test
    @Tag("report52-it")
    void IT_CT_013_FullySignedStudentContractPublishesStudentSignedEvent() {
        Contract studentContract = studentEnrollmentContract();
        ContractSignature clientSignature = pendingClientSignature(studentContract);
        ContractSignature centerSignature = signedCenterSignature(studentContract);
        User clientUser = studentContract.getClassStudent().getEnrolledByUser();
        activeOtp.setEmail(clientUser.getEmail());

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(cccdService.isComplete(clientUser.getUserId())).thenReturn(true);
        when(contractRepository.findById(902L)).thenReturn(Optional.of(studentContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(902L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractId(902L)).thenReturn(List.of(clientSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(902L)).thenReturn(2);
        when(userRepository.findById(clientUser.getUserId())).thenReturn(Optional.of(clientUser));
        when(tutorCenterRepository.findByUser_UserId(100L)).thenReturn(Optional.of(studentCenter()));
        when(clientRepository.findByUser_UserId(clientUser.getUserId())).thenReturn(Optional.empty());

        contractService.signWithOtp(902L, otp("123456"));

        assertEquals(ContractStatus.SIGNED, studentContract.getStatus());
        verify(eventPublisher).publishEvent(any(StudentContractSigned.class));
    }

    @Test
    @Tag("report52-it")
    void IT_CT_016_FullySignedStudentContractPreparesCenterEscrowPayment() {
        Contract studentContract = studentEnrollmentContract();
        ContractSignature clientSignature = pendingClientSignature(studentContract);
        ContractSignature centerSignature = signedCenterSignature(studentContract);
        User clientUser = studentContract.getClassStudent().getEnrolledByUser();
        activeOtp.setEmail(clientUser.getEmail());

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(cccdService.isComplete(clientUser.getUserId())).thenReturn(true);
        when(contractRepository.findById(902L)).thenReturn(Optional.of(studentContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(902L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractId(902L)).thenReturn(List.of(clientSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(902L)).thenReturn(2);
        when(userRepository.findById(clientUser.getUserId())).thenReturn(Optional.of(clientUser));
        when(tutorCenterRepository.findByUser_UserId(100L)).thenReturn(Optional.of(studentCenter()));
        when(clientRepository.findByUser_UserId(clientUser.getUserId())).thenReturn(Optional.empty());

        contractService.signWithOtp(902L, otp("123456"));

        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(300L, commandCaptor.getValue().payerUserId());
        assertEquals(88L, commandCaptor.getValue().classStudentId());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
    }

    @Test
    @Tag("report52-it")
    void IT_CCE_014_StudentEnrollmentContractUsesPerStudentTuitionForEscrowCommand() {
        Contract studentContract = studentEnrollmentContract();
        ContractSignature clientSignature = pendingClientSignature(studentContract);
        ContractSignature centerSignature = signedCenterSignature(studentContract);
        User clientUser = studentContract.getClassStudent().getEnrolledByUser();
        activeOtp.setEmail(clientUser.getEmail());

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(cccdService.isComplete(clientUser.getUserId())).thenReturn(true);
        when(contractRepository.findById(902L)).thenReturn(Optional.of(studentContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(902L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractId(902L)).thenReturn(List.of(clientSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(902L)).thenReturn(2);
        when(userRepository.findById(clientUser.getUserId())).thenReturn(Optional.of(clientUser));
        when(tutorCenterRepository.findByUser_UserId(100L)).thenReturn(Optional.of(studentCenter()));
        when(clientRepository.findByUser_UserId(clientUser.getUserId())).thenReturn(Optional.empty());

        contractService.signWithOtp(902L, otp("123456"));

        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(clientUser.getUserId(), commandCaptor.getValue().payerUserId());
        assertEquals(88L, commandCaptor.getValue().classStudentId());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_014_StudentContractAmountRemainsStableAfterOtpSignature() {
        Contract studentContract = studentEnrollmentContract();
        ContractSignature clientSignature = pendingClientSignature(studentContract);
        ContractSignature centerSignature = signedCenterSignature(studentContract);
        User clientUser = studentContract.getClassStudent().getEnrolledByUser();
        activeOtp.setEmail(clientUser.getEmail());

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(cccdService.isComplete(clientUser.getUserId())).thenReturn(true);
        when(contractRepository.findById(902L)).thenReturn(Optional.of(studentContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(902L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractId(902L)).thenReturn(List.of(clientSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(902L)).thenReturn(2);
        when(userRepository.findById(clientUser.getUserId())).thenReturn(Optional.of(clientUser));
        when(tutorCenterRepository.findByUser_UserId(100L)).thenReturn(Optional.of(studentCenter()));
        when(clientRepository.findByUser_UserId(clientUser.getUserId())).thenReturn(Optional.empty());

        ContractResponse response = contractService.signWithOtp(902L, otp("123456"));

        assertEquals(new BigDecimal("120000.00"), response.getTuitionFee());
        assertEquals(5, response.getNumberOfSessions());
        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_015_ContractOtpSigningStoresSignerTimestampAndConsumesOtp() {
        User tutorUser = tutorUser();

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, signedCenterSignature()));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);

        contractService.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
        assertEquals(tutorUser, tutorSignature.getSigner());
        assertNotNull(tutorSignature.getSignedAt());
        assertTrue(tutorSignature.getSignatureData().startsWith("OTP_VERIFIED:" + tutorUser.getEmail()));
        assertNotNull(activeOtp.getConsumedAt());
        verify(contractSignatureRepository).save(tutorSignature);
        verify(emailOtpRepository).save(activeOtp);
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_014_PrivateContractShowsTotalAndFirstPaymentAmountFromTerms() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        User clientUser = privateContract.getAssignment().getApplication().getTutoringClass().getCreator();

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractSignatureRepository.findByContractId(901L)).thenReturn(List.of());

        ContractResponse response = contractService.getMyContract(901L);

        assertEquals(new BigDecimal("400000.00"), response.getTotalTuitionAmount());
        assertEquals(new BigDecimal("400000.00"), response.getEscrowAmount());
        assertEquals("Lớp private Toán 12", response.getClassTitle());
    }

    @Test
    @Tag("report52-it")
    void IT_PRV_015_TutorContractListIncludesClientSignedPrivateContractAfterNotification() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        ClassAssignment assignment = privateContract.getAssignment();
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(5));
        User tutorUser = assignment.getTutor().getUser();
        ContractSignature clientSignature = signedClientSignature(privateContract);
        ContractSignature pendingTutorSignature = pendingTutorSignature(privateContract);

        when(authHelper.currentUserId()).thenReturn(tutorUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(contractRepository.findContractsByUserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findBySignatureParty(tutorUser.getUserId(), tutorUser.getEmail())).thenReturn(List.of());
        when(contractRepository.findByAssignment_Tutor_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByAssignment_ClassCreator_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByClassStudent_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_Tutor_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_CenterUser_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(tutorRepository.findByUser_UserId(tutorUser.getUserId())).thenReturn(Optional.of(assignment.getTutor()));
        when(classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(20L)).thenReturn(List.of(assignment));
        when(classAssignmentRepository.findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(tutorUser.getUserId()))
                .thenReturn(List.of());
        when(contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId()))
                .thenReturn(Optional.of(privateContract));
        when(contractRepository.save(privateContract)).thenReturn(privateContract);
        when(contractSignatureRepository.findByContractId(901L))
                .thenReturn(List.of(clientSignature, pendingTutorSignature));
        when(contractSignatureRepository.findByContractIdAndPartyRole(901L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractIdAndPartyRole(901L, PartyRole.TUTOR))
                .thenReturn(Optional.of(pendingTutorSignature));
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(1);

        List<ContractResponse> responses = contractService.getMyContracts();

        assertEquals(1, responses.size());
        assertEquals(901L, responses.get(0).getContractId());
        assertEquals(1, responses.get(0).getSignedCount());
        assertEquals(2, responses.get(0).getRequiredSignatures());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_017_TutorDeclinesPendingCooperationContractAndApplicationIsWithdrawn() {
        RecruitmentApplication application = contract.getRecruitmentApplication();

        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);

        contractService.declineCooperationContract(CONTRACT_ID);

        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertEquals(com.tcs.module.center.enums.RecruitmentApplicationStatus.WITHDRAWN, application.getStatus());
        verify(contractRepository).save(contract);
        verify(recruitmentApplicationRepository).save(application);
    }

    @Test
    void SUPPORT_CONTRACT_RejectOtpSigningWhenSignatureSlotIsMissing() {
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertEquals("Chưa gửi mã OTP cho vai trò này", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_008_PreventUserFromSigningForAnotherAccount() {
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.sign(CONTRACT_ID, "123456", STRANGER_USER_ID));

        assertEquals("Không thể ký thay người dùng khác", exception.getMessage());
        verify(contractSignatureRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CT_020_FullySignedCooperationContractMarksContractSignedAndPublishesEvent() {
        User tutorUser = tutorUser();
        ContractSignature centerSignature = signedCenterSignature();

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);

        contractService.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractStatus.SIGNED, contract.getStatus());
        assertNotNull(contract.getSignedAt());
        assertNotNull(contract.getConfirmedAt());
        verify(contractRepository).save(contract);
        verify(eventPublisher).publishEvent(any(CooperationContractSigned.class));
    }

    private SignWithOtpRequest otp(String code) {
        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode(code);
        return request;
    }

    private User tutorUser() {
        return contract.getRecruitmentApplication().getTutor().getUser();
    }

    private ContractSignature signedCenterSignature() {
        User centerUser = contract.getRecruitmentApplication().getRecruitmentPost().getCenter().getUser();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(2L);
        signature.setContract(contract);
        signature.setPartyRole(PartyRole.CENTER);
        signature.setSigner(centerUser);
        signature.setEmail("center.it@tcs.test");
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now().minusHours(1));
        return signature;
    }

    private ContractSignature signedCenterSignature(Contract targetContract) {
        User centerUser = targetContract.getClassStudent().getTutoringClass().getCreator();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(12L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.CENTER);
        signature.setSigner(centerUser);
        signature.setEmail(centerUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now().minusHours(1));
        return signature;
    }

    private ContractSignature pendingClientSignature(Contract targetContract) {
        User clientUser = targetContract.getClassStudent().getEnrolledByUser();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(11L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.CLIENT);
        signature.setEmail(clientUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        return signature;
    }

    private ContractSignature signedTutorSignature(User tutorUser) {
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(3L);
        signature.setContract(contract);
        signature.setPartyRole(PartyRole.TUTOR);
        signature.setSigner(tutorUser);
        signature.setEmail(tutorUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());
        return signature;
    }

    private ContractSignature signedClientSignature(Contract targetContract) {
        User clientUser = targetContract.getAssignment().getApplication().getTutoringClass().getCreator();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(21L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.CLIENT);
        signature.setSigner(clientUser);
        signature.setEmail(clientUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now().minusMinutes(5));
        return signature;
    }

    private ContractSignature pendingTutorSignature(Contract targetContract) {
        User tutorUser = targetContract.getAssignment().getTutor().getUser();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(22L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.TUTOR);
        signature.setEmail(tutorUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        return signature;
    }

    private Contract studentEnrollmentContract() {
        User centerUser = new User();
        centerUser.setUserId(100L);
        centerUser.setEmail("center.it@tcs.test");
        User clientUser = new User();
        clientUser.setUserId(300L);
        clientUser.setEmail("client.it@tcs.test");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(500L);
        tutoringClass.setCreator(centerUser);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setTitle("Lớp Toán trung tâm");
        tutoringClass.setTuitionFee(new BigDecimal("120000.00"));
        tutoringClass.setNumberOfSessions(5);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(88L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(clientUser);
        classStudent.setStudentName("Nguyễn Minh Anh");

        Contract studentContract = new Contract();
        studentContract.setContractId(902L);
        studentContract.setContractNo("HD-STUDENT-IT");
        studentContract.setStatus(ContractStatus.PENDING);
        studentContract.setClassStudent(classStudent);
        studentContract.setSourceType(com.tcs.module.contract.enums.ContractSourceType.CENTER);
        return studentContract;
    }

    private TutorCenter studentCenter() {
        User centerUser = new User();
        centerUser.setUserId(100L);
        centerUser.setEmail("center.it@tcs.test");
        TutorCenter center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);
        center.setCompanyName("Trung tâm IT");
        return center;
    }

    private void stubContractListForUser(Long userId, String email, Contract visibleContract) {
        when(contractRepository.findContractsByUserId(userId)).thenReturn(List.of(visibleContract));
        when(contractRepository.findBySignatureParty(userId, email)).thenReturn(List.of(visibleContract));
        when(contractRepository.findByAssignment_Tutor_UserId(userId)).thenReturn(List.of(visibleContract));
        when(contractRepository.findByAssignment_ClassCreator_UserId(userId)).thenReturn(List.of());
        when(contractRepository.findByClassStudent_UserId(userId)).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_Tutor_UserId(userId)).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_CenterUser_UserId(userId)).thenReturn(List.of());
        when(tutorRepository.findByUser_UserId(userId)).thenReturn(Optional.of(visibleContract.getAssignment().getTutor()));
        when(classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(20L)).thenReturn(List.of());
        when(classAssignmentRepository.findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(userId))
                .thenReturn(List.of());
    }

    private void preparePrivateTuitionData(Contract privateContract) {
        ClassAssignment assignment = privateContract.getAssignment();
        TutoringClass tutoringClass = assignment.getApplication().getTutoringClass();
        tutoringClass.setTitle("Lớp private Toán 12");
        tutoringClass.setNumberOfSessions(4);
        tutoringClass.setTuitionFee(new BigDecimal("100000.00"));
        tutoringClass.setDetailsJson("""
                {"subjectFees":{"1":100000},"slots":[{"subjectId":"1","start":"18:00","end":"19:00"}],
                 "billingCycle":"MONTH","months":1,"durationUnit":"MONTH","scheduleMode":"WEEKLY"}
                """);
        assignment.getApplication().setProposedRatesJson("{\"1\":100000}");
    }

    private PaymentTransaction pendingEscrowPayment(String referenceCode, BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(710L);
        transaction.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setDescription("Chờ thanh toán ký quỹ hợp đồng private");
        transaction.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        return transaction;
    }

    private Contract privateAssignmentContract() {
        User clientUser = new User();
        clientUser.setUserId(300L);
        clientUser.setEmail("client.it@tcs.test");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(500L);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.PRIVATE);
        tutoringClass.setCreator(clientUser);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(700L);
        application.setTutoringClass(tutoringClass);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(800L);
        assignment.setApplication(application);
        assignment.setTutor(contract.getRecruitmentApplication().getTutor());

        Contract privateContract = new Contract();
        privateContract.setContractId(901L);
        privateContract.setContractNo("HD-PRIVATE-IT");
        privateContract.setStatus(ContractStatus.PENDING);
        privateContract.setAssignment(assignment);
        privateContract.setSourceType(com.tcs.module.contract.enums.ContractSourceType.PRIVATE);
        return privateContract;
    }
}
