package com.tcs.module.contract.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.catalog.repository.SystemParameterRepository;
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
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * System Test ST-ECO-001: Both parties sign the e-contract via email OTP.
 * Procedure:
 * 1. System generates the e-contract after tutor selection.
 * 2. Client & Tutor review the contract.
 * 3. Each party signs with an email OTP.
 * Expected:
 * OTP signing succeeds for both -> contract becomes SIGNED; class moves ASSIGNED (awaiting deposit); signing time recorded.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ST_ECO_001_BothPartiesSignOtpTest {

    private static final Long CONTRACT_ID = 501L;
    private static final Long CLIENT_USER_ID = 101L;
    private static final Long TUTOR_USER_ID = 202L;
    private static final String CLIENT_EMAIL = "client01@tcs.test";
    private static final String TUTOR_EMAIL = "tutor01@tcs.test";

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
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ReputationHistoryRepository reputationHistoryRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private CccdService cccdService;
    @Mock private EmailOtpRepository emailOtpRepository;

    @InjectMocks private ContractServiceImpl contractService;

    private Contract contract;
    private ContractSignature clientSignature;
    private ContractSignature tutorSignature;
    private ClassAssignment assignment;
    private User clientUser;
    private User tutorUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contractService, "otpService", new OtpService(emailOtpRepository));

        clientUser = new User();
        clientUser.setUserId(CLIENT_USER_ID);
        clientUser.setEmail(CLIENT_EMAIL);

        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail(TUTOR_EMAIL);

        Client client = new Client();
        client.setClientId(10L);
        client.setUser(clientUser);
        client.setDateOfBirth(LocalDate.of(1990, 5, 20));

        Tutor tutor = new Tutor();
        tutor.setTutorId(20L);
        tutor.setUser(tutorUser);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(301L);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setCreator(clientUser);
        tutoringClass.setDetailsJson("{\"subjectFees\":{\"MATH\":200000},\"slots\":[{\"subjectId\":\"MATH\",\"start\":\"18:00\",\"end\":\"20:00\"}]}");

        TutorApplication app = new TutorApplication();
        app.setApplicationId(302L);
        app.setTutoringClass(tutoringClass);
        app.setTutor(tutor);

        assignment = new ClassAssignment();
        assignment.setAssignmentId(401L);
        assignment.setTutor(tutor);
        assignment.setApplication(app);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);

        contract = new Contract();
        contract.setContractId(CONTRACT_ID);
        contract.setContractNo("HD-ST-001");
        contract.setStatus(ContractStatus.PENDING);
        contract.setAssignment(assignment);

        clientSignature = new ContractSignature();
        clientSignature.setSignatureId(1L);
        clientSignature.setContract(contract);
        clientSignature.setPartyRole(PartyRole.CLIENT);
        clientSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        clientSignature.setEmail(CLIENT_EMAIL);

        tutorSignature = new ContractSignature();
        tutorSignature.setSignatureId(2L);
        tutorSignature.setContract(contract);
        tutorSignature.setPartyRole(PartyRole.TUTOR);
        tutorSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        tutorSignature.setEmail(TUTOR_EMAIL);

        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(Arrays.asList(clientSignature, tutorSignature));

        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));

        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(cccdService.isComplete(anyLong())).thenReturn(true);
        when(cccdService.getByUserId(anyLong())).thenReturn(CccdInfoDto.builder().complete(true).build());
    }

    @Test
    @DisplayName("ST-ECO-001: Bước 1 & 2 - Client gửi OTP và ký hợp đồng thành công")
    void testClientSignsWithOtp() {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));

        EmailOtp clientOtp = new EmailOtp();
        clientOtp.setEmail(CLIENT_EMAIL);
        clientOtp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        clientOtp.setCode("654321");
        clientOtp.setAttempts(0);
        clientOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(CLIENT_EMAIL, OtpPurpose.CONTRACT_SIGNING))
                .thenReturn(Optional.of(clientOtp));

        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode("654321");

        ContractResponse response = contractService.signWithOtp(CONTRACT_ID, request);

        assertEquals(ContractSignatureStatus.SIGNED, clientSignature.getSignatureStatus());
        assertNotNull(clientSignature.getSignedAt());
        assertTrue(clientSignature.getSignatureData().contains("OTP_VERIFIED"));

        // Only 1 party signed -> contract remains PENDING
        assertEquals(ContractStatus.PENDING, contract.getStatus());
        verify(contractSignatureRepository).save(clientSignature);
    }

    @Test
    @DisplayName("ST-ECO-001: Bước 3 - Cả hai bên ký đủ -> Hợp đồng chuyển SIGNED, chuẩn bị Escrow")
    void testBothPartiesSignContractFully() {
        // Step 1: Client has already signed
        clientSignature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        clientSignature.setSignedAt(LocalDateTime.now());
        clientSignature.setSigner(clientUser);

        // Step 2: Tutor logs in and signs
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));

        EmailOtp tutorOtp = new EmailOtp();
        tutorOtp.setEmail(TUTOR_EMAIL);
        tutorOtp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        tutorOtp.setCode("888999");
        tutorOtp.setAttempts(0);
        tutorOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(TUTOR_EMAIL, OtpPurpose.CONTRACT_SIGNING))
                .thenReturn(Optional.of(tutorOtp));

        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);

        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode("888999");

        ContractResponse response = contractService.signWithOtp(CONTRACT_ID, request);

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
        assertNotNull(tutorSignature.getSignedAt());

        // Contract transitions to SIGNED
        assertEquals(ContractStatus.SIGNED, contract.getStatus());
        verify(contractRepository).save(contract);

        // Escrow payment prepared
        verify(escrowService).preparePayment(any());

        // Tutor signed timestamp synced to assignment
        verify(classAssignmentRepository).save(assignment);
        assertNotNull(assignment.getTutorSignedAt());
    }
}
