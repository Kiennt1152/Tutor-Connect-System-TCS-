package com.tcs.module.contract.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDateTime;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * System Test ST-ECO-003: Signing OTP wrong or expired.
 * Procedure:
 * 1. Enter the signing step.
 * 2. Enter a wrong / expired OTP.
 * Expected:
 * Signing blocked; contract stays in the unsigned state; OTP error shown.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ST_ECO_003_WrongOrExpiredOtpTest {

    private static final Long CONTRACT_ID = 601L;
    private static final Long TUTOR_USER_ID = 202L;
    private static final String TUTOR_EMAIL = "tutor01@tcs.test";

    @Mock private AuthHelper authHelper;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private CccdService cccdService;
    @Mock private EmailOtpRepository emailOtpRepository;

    @InjectMocks private ContractServiceImpl contractService;

    private Contract contract;
    private ContractSignature tutorSignature;
    private User tutorUser;
    private EmailOtp activeOtp;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contractService, "otpService", new OtpService(emailOtpRepository));

        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail(TUTOR_EMAIL);

        Tutor tutor = new Tutor();
        tutor.setTutorId(20L);
        tutor.setUser(tutorUser);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassType(ClassType.PRIVATE);

        TutorApplication app = new TutorApplication();
        app.setTutoringClass(tutoringClass);
        app.setTutor(tutor);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setTutor(tutor);
        assignment.setApplication(app);

        contract = new Contract();
        contract.setContractId(CONTRACT_ID);
        contract.setStatus(ContractStatus.PENDING);
        contract.setAssignment(assignment);

        tutorSignature = new ContractSignature();
        tutorSignature.setSignatureId(2L);
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

        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(cccdService.isComplete(anyLong())).thenReturn(true);
        when(cccdService.getByUserId(anyLong())).thenReturn(CccdInfoDto.builder().complete(true).build());
    }

    @Test
    @DisplayName("ST-ECO-003: Nhập sai OTP -> chặn ký, hiển thị thông báo còn số lần thử, giữ nguyên PENDING")
    void testWrongOtpBlocksSigning() {
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(TUTOR_EMAIL, OtpPurpose.CONTRACT_SIGNING))
                .thenReturn(Optional.of(activeOtp));

        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode("000000"); // wrong OTP

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, request));

        assertTrue(ex.getMessage().startsWith("Mã OTP không đúng"),
                "Thông báo phải báo sai OTP: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("lần thử"),
                "Thông báo phải chứa số lần thử còn lại: " + ex.getMessage());

        // Contract and signature remain unsigned
        assertEquals(ContractSignatureStatus.PENDING, tutorSignature.getSignatureStatus());
        assertNull(tutorSignature.getSignedAt());
        assertEquals(ContractStatus.PENDING, contract.getStatus());
        verify(contractRepository, never()).save(contract);
    }

    @Test
    @DisplayName("ST-ECO-003: OTP đã hết hạn (>5 phút) -> chặn ký, yêu cầu gửi mã mới")
    void testExpiredOtpBlocksSigning() {
        activeOtp.setExpiresAt(LocalDateTime.now().minusMinutes(2)); // Expired 2 minutes ago
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(TUTOR_EMAIL, OtpPurpose.CONTRACT_SIGNING))
                .thenReturn(Optional.of(activeOtp));

        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode("123456"); // Correct code but expired

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, request));

        assertEquals("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.", ex.getMessage());

        // Signature marked as EXPIRED, contract remains PENDING
        assertEquals(ContractSignatureStatus.EXPIRED, tutorSignature.getSignatureStatus());
        assertNull(tutorSignature.getSignedAt());
        assertEquals(ContractStatus.PENDING, contract.getStatus());
        verify(contractRepository, never()).save(contract);
    }
}
