package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tcs.exception.DuplicateEmailException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.dto.request.ChangePasswordRequest;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.dto.request.GoogleCompleteRequest;
import com.tcs.module.identity.dto.request.GoogleLoginRequest;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
import com.tcs.module.identity.dto.request.VerifyPasswordResetOtpRequest;
import com.tcs.module.identity.dto.request.RequestPasswordResetOtpRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
import com.tcs.module.identity.dto.response.RegisterResponse;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.EmailVerificationToken;
import com.tcs.module.identity.entity.PasswordResetToken;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.EmailVerificationTokenRepository;
import com.tcs.module.identity.repository.PasswordResetTokenRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.GoogleTokenVerifier;
import com.tcs.security.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IdentityServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlatformAdminRepository platformAdminRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorCenterRepository tutorCenterRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailOtpRepository emailOtpRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private PlatformMapper platformMapper;
    @Mock
    private AuthHelper authHelper;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private IdentityServiceImpl identityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(identityService, "otpService", new OtpService(emailOtpRepository));
        ReflectionTestUtils.setField(identityService, "otpLength", 6);
        ReflectionTestUtils.setField(identityService, "otpExpirationMinutes", 5);
        ReflectionTestUtils.setField(identityService, "maxAttempts", 5);
        ReflectionTestUtils.setField(identityService, "resendCooldownSeconds", 60);
        ReflectionTestUtils.setField(identityService, "maxPerEmailPerWindow", 5);
        ReflectionTestUtils.setField(identityService, "emailWindowMinutes", 6);
        ReflectionTestUtils.setField(identityService, "maxPerIpPerHour", 5);
        ReflectionTestUtils.setField(identityService, "tokenExpirationMinutes", 15);
    }

    // =========================================================================================
    // REGISTER TESTS (TC-UNIT-IdentityService-001 -> 013)
    // =========================================================================================

    @Test
    void TC_UNIT_IdentityService_001_register_happyPath() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");
        req.setDisplayName("Test User");
        req.setPhone("0123456789");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("test@gmail.com");
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0123456789")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_pass");

        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setEmail("test@gmail.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse res = identityService.register(req);

        assertEquals("test@gmail.com", res.getEmail());
        assertNotNull(token.getConsumedAt());
        verify(userRepository).save(any(User.class));
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void TC_UNIT_IdentityService_002_register_confirmPasswordMismatch() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password1234");
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Mật khẩu xác nhận không khớp", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void TC_UNIT_IdentityService_003_register_passwordTooShort() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Test123");
        req.setConfirmPassword("Test123");
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_004_register_passwordValid() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Test1234");
        req.setConfirmPassword("Test1234");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");
        req.setDisplayName("Test");
        req.setPhone("0123456789");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("test@gmail.com");
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.save(any())).thenReturn(new User());

        assertDoesNotThrow(() -> identityService.register(req));
    }

    @Test
    void TC_UNIT_IdentityService_005_register_passwordMissingDigits() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("TestTest");
        req.setConfirmPassword("TestTest");
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_006_register_passwordNonAscii() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Test1234é");
        req.setConfirmPassword("Test1234é");
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Mật khẩu không được chứa ký tự có dấu hoặc ký tự không thuộc ASCII", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_007_register_rolePlatformAdmin() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.PLATFORM_ADMIN);
        
        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Vai trò đăng ký không hợp lệ", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_008_register_verifiedTokenNotFound() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("invalid-token");
        
        when(emailVerificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Phiên xác thực email không hợp lệ. Vui lòng xác thực lại email.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_009_register_verifiedTokenConsumed() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setConsumedAt(LocalDateTime.now());
        
        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Phiên xác thực email đã được sử dụng. Vui lòng xác thực lại email.", ex.getMessage());
    }

    /** Sheet register - UTCID08 (B): phiên xác thực email đã hết hạn. */
    @Test
    void TC_UNIT_IdentityService_009b_register_verifiedTokenExpired() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("test@gmail.com");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Phiên xác thực email đã hết hạn. Vui lòng xác thực lại email.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_010_register_verifiedTokenEmailMismatch() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("other@gmail.com");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Mã xác thực email không khớp với email đăng ký.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_011_register_emailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("test@gmail.com");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        User existingUser = new User();
        existingUser.setStatus(UserStatus.ACTIVE);

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));

        Exception ex = assertThrows(DuplicateEmailException.class, () -> identityService.register(req));
        assertEquals("Email này đã được đăng ký", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_012_register_emailBanned() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("test@gmail.com");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        User existingUser = new User();
        existingUser.setStatus(UserStatus.BANNED);

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));

        Exception ex = assertThrows(DuplicateEmailException.class, () -> identityService.register(req));
        assertEquals("Email này đã bị khóa và không thể đăng ký tài khoản mới.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_013_register_phoneExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");
        req.setRole(UserRole.CLIENT);
        req.setVerifiedEmailToken("valid-token");
        req.setPhone("0123456789");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("test@gmail.com");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0123456789")).thenReturn(true);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.register(req));
        assertEquals("Số điện thoại đã được sử dụng", ex.getMessage());
    }


    // =========================================================================================
    // VERIFY OTP TESTS (TC-UNIT-IdentityService-014 -> 018)
    // =========================================================================================

    @Test
    void TC_UNIT_IdentityService_014_verifyOtp_happyPath() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(0);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        var res = identityService.verifyOtp(req);

        assertEquals("test@gmail.com", res.getEmail());
        assertNotNull(otp.getConsumedAt());
        assertNotNull(res.getVerifiedEmailToken());
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
    }

    /** Sheet verifyOTP - UTCID02 (A): không tìm thấy bản ghi OTP chưa dùng. */
    @Test
    void TC_UNIT_IdentityService_014b_verifyOtp_recordNotFound() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Mã xác thực không tồn tại. Vui lòng yêu cầu gửi lại mã.", ex.getMessage());
    }

    /** Sheet verifyOTP - UTCID04 (A): OTP sai, attempts = 0 -> còn lượt thử. */
    @Test
    void TC_UNIT_IdentityService_014c_verifyOtp_wrongCode_attempts0() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("999999");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(0);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Mã xác thực không đúng. Bạn còn 4 lần thử.", ex.getMessage());
        assertEquals(1, otp.getAttempts());
    }

    /** Sheet verifyOTP - UTCID05 (B): OTP sai khi attempts = 3 -> lần thử thứ 4, còn 1 lượt. */
    @Test
    void TC_UNIT_IdentityService_014d_verifyOtp_wrongCode_lastRemainingAttempt() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("999999");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(3);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Mã xác thực không đúng. Bạn còn 1 lần thử.", ex.getMessage());
    }

    /** Sheet verifyOTP - UTCID06 (B): OTP sai khi attempts = 4 -> chạm ngưỡng 5, khoá mã. */
    @Test
    void TC_UNIT_IdentityService_014e_verifyOtp_wrongCode_reachesMaxAttempts() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("999999");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(4);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.", ex.getMessage());
    }

    /** Sheet verifyOTP - UTCID07 (A): attempts đã vượt ngưỡng -> chặn ngay, không so mã. */
    @Test
    void TC_UNIT_IdentityService_014f_verifyOtp_alreadyOverMaxAttempts() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456"); // đúng mã, nhưng đã vượt ngưỡng nên vẫn bị chặn

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(5);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_015_verifyOtp_otpExpired() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        EmailOtp otp = new EmailOtp();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Mã xác thực đã hết hạn. Vui lòng yêu cầu gửi lại mã.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_016_verifyOtp_wrongOtp_underLimit() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("111111");

        EmailOtp otp = new EmailOtp();
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(3);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Mã xác thực không đúng. Bạn còn 1 lần thử.", ex.getMessage());
        assertEquals(4, otp.getAttempts());
        verify(emailOtpRepository).save(otp);
    }

    @Test
    void TC_UNIT_IdentityService_017_verifyOtp_wrongOtp_reachLimit() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("111111");

        EmailOtp otp = new EmailOtp();
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(4); // Lần thử thứ 5

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.", ex.getMessage());
        assertEquals(5, otp.getAttempts());
    }

    @Test
    void TC_UNIT_IdentityService_018_verifyOtp_notFound() {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyOtp(req));
        assertEquals("Mã xác thực không tồn tại. Vui lòng yêu cầu gửi lại mã.", ex.getMessage());
    }


    // =========================================================================================
    // LOGIN TESTS (TC-UNIT-IdentityService-019 -> 022)
    // =========================================================================================

    @Test
    void TC_UNIT_IdentityService_019_login_happyPath() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");

        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@gmail.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hash")).thenReturn(true);
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(jwtService.generateToken(1L, "test@gmail.com", UserRole.CLIENT, 0L)).thenReturn("jwt-token");
        
        UserListItemResponse uli = UserListItemResponse.builder().displayName("Test").build();
        when(platformMapper.toUserListItem(any(), any())).thenReturn(uli);

        AuthResponse res = identityService.login(req);

        assertEquals("jwt-token", res.getAccessToken());
        assertNotNull(user.getLastLogin());
        verify(userRepository).save(user);
    }

    @Test
    void TC_UNIT_IdentityService_020_login_bannedUser() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");

        User user = new User();
        user.setStatus(UserStatus.BANNED);

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.login(req));
        assertEquals("Tài khoản của bạn đã bị khóa và không thể đăng nhập. Vui lòng liên hệ quản trị viên.", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_suspendedUser_isRejected() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");

        User user = new User();
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.login(req));

        assertEquals("Tài khoản của bạn đang bị tạm ngừng. Vui lòng liên hệ quản trị viên.", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void logout_incrementsTokenVersionAndWritesAudit() {
        User user = new User();
        user.setUserId(7L);
        user.setTokenVersion(3L);
        when(authHelper.currentUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        identityService.logout();

        assertEquals(4L, user.getTokenVersion());
        verify(userRepository).save(user);
        verify(auditLogService).record(
                eq(7L), eq("LOGOUT"), eq("User"), eq(7L),
                eq(java.util.Map.of("tokenVersion", 3L)),
                eq(java.util.Map.of("tokenVersion", 4L)));
    }

    @Test
    /** Sheet login - UTCID05 (A): tài khoản đang bị tạm ngừng -> chặn trước khi kiểm mật khẩu. */
    void TC_UNIT_IdentityService_020b_login_suspendedUser() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");

        User user = new User();
        user.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.login(req));
        assertEquals("Tài khoản của bạn đang bị tạm ngừng. Vui lòng liên hệ quản trị viên.", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void TC_UNIT_IdentityService_021_login_wrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");

        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hash");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hash")).thenReturn(false);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.login(req));
        assertEquals("Email hoặc mật khẩu không đúng", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_022_login_emailNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@gmail.com");
        req.setPassword("Password123");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.login(req));
        assertEquals("Email hoặc mật khẩu không đúng", ex.getMessage());
    }


    // =========================================================================================
    // CHANGE/RESET PASSWORD TESTS (TC-UNIT-IdentityService-023 -> 024)
    // =========================================================================================

    @Test
    void TC_UNIT_IdentityService_023_changePassword_wrongCurrentPassword() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("New12345");

        when(authHelper.currentUserId()).thenReturn(1L);
        User user = new User();
        user.setPasswordHash("hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.changePassword(req));
        assertEquals("Mật khẩu hiện tại không đúng", ex.getMessage());
    }

    /** Sheet changePassword - UTCID01 (N): mật khẩu hiện tại đúng, mật khẩu mới hợp lệ và khác cũ. */
    @Test
    void TC_UNIT_IdentityService_023a_changePassword_happyPath() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("Old12345");
        req.setNewPassword("New12345");

        when(authHelper.currentUserId()).thenReturn(1L);
        User user = new User();
        user.setUserId(1L);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old12345", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("New12345", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("New12345")).thenReturn("new-hash");

        identityService.changePassword(req);

        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    /** Sheet changePassword - UTCID03 (A): mật khẩu mới trùng mật khẩu hiện tại. */
    @Test
    void TC_UNIT_IdentityService_023b_changePassword_newSameAsCurrent() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("Old12345");
        req.setNewPassword("Old12345");

        when(authHelper.currentUserId()).thenReturn(1L);
        User user = new User();
        user.setPasswordHash("old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old12345", "old-hash")).thenReturn(true);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.changePassword(req));
        assertEquals("Mật khẩu mới phải khác mật khẩu hiện tại", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    /** Sheet changePassword - UTCID04 (B): mật khẩu mới quá ngắn / thiếu số. */
    @Test
    void TC_UNIT_IdentityService_023c_changePassword_newPasswordInvalidFormat() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("Old12345");
        req.setNewPassword("abc");

        when(authHelper.currentUserId()).thenReturn(1L);
        User user = new User();
        user.setPasswordHash("old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old12345", "old-hash")).thenReturn(true);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.changePassword(req));
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số", ex.getMessage());
    }

    /** Sheet changePassword - UTCID05 (A): mật khẩu mới có ký tự có dấu (không thuộc ASCII). */
    @Test
    void TC_UNIT_IdentityService_023d_changePassword_newPasswordNonAscii() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("Old12345");
        req.setNewPassword("MatKhau123á");

        when(authHelper.currentUserId()).thenReturn(1L);
        User user = new User();
        user.setPasswordHash("old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Old12345", "old-hash")).thenReturn(true);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.changePassword(req));
        assertEquals("Mật khẩu không được chứa ký tự có dấu hoặc ký tự không thuộc ASCII", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_024_resetPassword_tokenExpired() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("expired-token");
        req.setNewPassword("New12345");

        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.resetPassword(req));
        assertEquals("Token đã hết hạn hoặc đã sử dụng", ex.getMessage());
    }


    // =========================================================================================
    // SEND OTP TESTS (TC-UNIT-IdentityService-025)
    // =========================================================================================

    @Test
    void TC_UNIT_IdentityService_025_sendOtp_rateLimitExceeded() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("test@gmail.com");

        EmailOtp lastOtp = new EmailOtp();
        lastOtp.setLastSentAt(LocalDateTime.now().minusSeconds(10)); // < 60s cooldown

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(lastOtp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.sendOtp(req, "127.0.0.1"));
        assertEquals("Quá nhiều yêu cầu, vui lòng thử lại sau.", ex.getMessage());
        verify(emailService, never()).sendRegistrationOtp(anyString(), anyString(), anyLong());
    }

    /** Sheet sendOTP - UTCID01 (N): chưa gửi lần nào, chưa chạm giới hạn -> gửi mã thành công. */
    @Test
    void TC_UNIT_IdentityService_025a_sendOtp_happyPath() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.empty());
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(
                eq("test@gmail.com"), eq(OtpPurpose.REGISTRATION), any(LocalDateTime.class))).thenReturn(0L);
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.empty());

        var res = identityService.sendOtp(req, "127.0.0.1");

        assertEquals("test@gmail.com", res.getEmail());
        verify(emailOtpRepository).save(any(EmailOtp.class));
        verify(emailService).sendRegistrationOtp(eq("test@gmail.com"), anyString(), anyLong());
    }

    /** Sheet sendOTP - UTCID03 (B): đã gửi đủ 5 mã trong cửa sổ 6 phút -> chặn. */
    @Test
    void TC_UNIT_IdentityService_025b_sendOtp_emailWindowLimitReached() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("test@gmail.com");

        EmailOtp lastOtp = new EmailOtp();
        lastOtp.setLastSentAt(LocalDateTime.now().minusSeconds(120)); // đã qua cooldown

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(lastOtp));
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(
                eq("test@gmail.com"), eq(OtpPurpose.REGISTRATION), any(LocalDateTime.class))).thenReturn(5L);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.sendOtp(req, "127.0.0.1"));
        assertEquals("Quá nhiều yêu cầu, vui lòng thử lại sau.", ex.getMessage());
        verify(emailService, never()).sendRegistrationOtp(anyString(), anyString(), anyLong());
    }

    /** Sheet sendOTP - UTCID04 (B): còn 4 mã trong cửa sổ (dưới ngưỡng 5) -> vẫn gửi được. */
    @Test
    void TC_UNIT_IdentityService_025c_sendOtp_justUnderWindowLimit() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("test@gmail.com");

        EmailOtp lastOtp = new EmailOtp();
        lastOtp.setLastSentAt(LocalDateTime.now().minusSeconds(120));

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.of(lastOtp));
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(
                eq("test@gmail.com"), eq(OtpPurpose.REGISTRATION), any(LocalDateTime.class))).thenReturn(4L);
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.REGISTRATION)).thenReturn(Optional.empty());

        identityService.sendOtp(req, "127.0.0.1");

        verify(emailService).sendRegistrationOtp(eq("test@gmail.com"), anyString(), anyLong());
    }

    /** Sheet sendOTP - UTCID05 (A): email đã có tài khoản -> không gửi mã. */
    @Test
    void TC_UNIT_IdentityService_025d_sendOtp_emailAlreadyRegistered() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("test@gmail.com");

        User existing = new User();
        existing.setEmail("test@gmail.com");
        existing.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(DuplicateEmailException.class, () -> identityService.sendOtp(req, "127.0.0.1"));
        assertEquals("Email này đã được đăng ký", ex.getMessage());
        verify(emailService, never()).sendRegistrationOtp(anyString(), anyString(), anyLong());
    }

    /** Sheet sendOTP - UTCID06 (A): email đã bị khóa -> không gửi mã. */
    @Test
    void TC_UNIT_IdentityService_025e_sendOtp_emailBanned() {
        SendOtpRequest req = new SendOtpRequest();
        req.setEmail("banned@gmail.com");

        User banned = new User();
        banned.setEmail("banned@gmail.com");
        banned.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail("banned@gmail.com")).thenReturn(Optional.of(banned));

        Exception ex = assertThrows(DuplicateEmailException.class, () -> identityService.sendOtp(req, "127.0.0.1"));
        assertEquals("Email này đã bị khóa và không thể đăng ký tài khoản mới.", ex.getMessage());
        verify(emailService, never()).sendRegistrationOtp(anyString(), anyString(), anyLong());
    }
    // =========================================================================================
    // FORGOT PASSWORD TESTS
    // =========================================================================================

    @Test
    void TC_UNIT_IdentityService_026_requestPasswordResetOtp_userNotFound() {
        RequestPasswordResetOtpRequest req = new RequestPasswordResetOtpRequest();
        req.setEmail("notfound@gmail.com");

        when(userRepository.findByEmail("notfound@gmail.com")).thenReturn(Optional.empty());

        var res = identityService.requestPasswordResetOtp(req, "127.0.0.1");
        assertEquals("notfound@gmail.com", res.getEmail());
        assertEquals("Nếu email tồn tại, mã OTP đặt lại mật khẩu đã được gửi", res.getMessage());

        verify(emailOtpRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyLong());
    }

    @Test
    void TC_UNIT_IdentityService_027_requestPasswordResetOtp_happyPath() {
        RequestPasswordResetOtpRequest req = new RequestPasswordResetOtpRequest();
        req.setEmail("found@gmail.com");

        when(userRepository.findByEmail("found@gmail.com")).thenReturn(Optional.of(new User()));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.empty());
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(anyString(), any(), any())).thenReturn(0L);

        var res = identityService.requestPasswordResetOtp(req, "127.0.0.1");
        assertEquals("found@gmail.com", res.getEmail());

        verify(emailOtpRepository).save(any(EmailOtp.class));
        verify(emailService).sendPasswordResetOtp(eq("found@gmail.com"), anyString(), anyLong());
    }

    @Test
    void TC_UNIT_IdentityService_028_verifyPasswordResetOtp_happyPath() {
        VerifyPasswordResetOtpRequest req = new VerifyPasswordResetOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(0);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(new User()));

        var res = identityService.verifyPasswordResetOtp(req, "127.0.0.1");
        assertEquals("test@gmail.com", res.getEmail());
        assertNotNull(otp.getConsumedAt());
        assertNotNull(res.getResetToken());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }
    @Test
    void TC_UNIT_IdentityService_029_requestPasswordResetOtp_cooldown() {
        RequestPasswordResetOtpRequest req = new RequestPasswordResetOtpRequest();
        req.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(new User()));

        EmailOtp lastOtp = new EmailOtp();
        lastOtp.setLastSentAt(LocalDateTime.now().minusSeconds(30)); // 30s < 60s cooldown
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(lastOtp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.requestPasswordResetOtp(req, "127.0.0.1"));
        assertEquals("Quá nhiều yêu cầu, vui lòng thử lại sau.", ex.getMessage());
    }

    @Test
    void TC_UNIT_IdentityService_030_verifyPasswordResetOtp_wrongTooManyTimes() {
        VerifyPasswordResetOtpRequest req = new VerifyPasswordResetOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("wrong");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(5);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.verifyPasswordResetOtp(req, "127.0.0.1"));
        assertEquals("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới", ex.getMessage());
    }

    /** Sheet resetPassword - UTCID02 (A): không tìm thấy bản ghi OTP đặt lại mật khẩu. */
    @Test
    void TC_UNIT_IdentityService_030a_verifyPasswordResetOtp_recordNotFound() {
        VerifyPasswordResetOtpRequest req = new VerifyPasswordResetOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> identityService.verifyPasswordResetOtp(req, "127.0.0.1"));
    }

    /** Sheet resetPassword - UTCID03 (B): OTP đặt lại mật khẩu đã hết hạn. */
    @Test
    void TC_UNIT_IdentityService_030b_verifyPasswordResetOtp_expired() {
        VerifyPasswordResetOtpRequest req = new VerifyPasswordResetOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("123456");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otp.setAttempts(0);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.verifyPasswordResetOtp(req, "127.0.0.1"));
        assertEquals("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới", ex.getMessage());
    }

    /** Sheet resetPassword - UTCID05 (A): nhập sai OTP nhưng chưa chạm ngưỡng. */
    @Test
    void TC_UNIT_IdentityService_030c_verifyPasswordResetOtp_wrongCode() {
        VerifyPasswordResetOtpRequest req = new VerifyPasswordResetOtpRequest();
        req.setEmail("test@gmail.com");
        req.setCode("999999");

        EmailOtp otp = new EmailOtp();
        otp.setEmail("test@gmail.com");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(1);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.verifyPasswordResetOtp(req, "127.0.0.1"));
        assertEquals("Mã OTP không đúng", ex.getMessage());
        assertEquals(2, otp.getAttempts());
    }

    /** Sheet resetPassword - UTCID01 (N): token hợp lệ + mật khẩu mới hợp lệ -> đổi mật khẩu. */
    @Test
    void TC_UNIT_IdentityService_030d_resetPassword_happyPath() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset-token");
        req.setNewPassword("New12345");

        User user = new User();
        user.setUserId(1L);
        user.setPasswordHash("old-hash");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("reset-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("New12345")).thenReturn("new-hash");

        identityService.resetPassword(req);

        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(token.getUsedAt(), "Token phải được đánh dấu đã dùng để không tái sử dụng");
        verify(userRepository).save(user);
    }

    /** Sheet resetPassword - UTCID06 (A): token không tồn tại. */
    @Test
    void TC_UNIT_IdentityService_030e_resetPassword_tokenNotFound() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("unknown-token");
        req.setNewPassword("New12345");

        when(passwordResetTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.resetPassword(req));
        assertEquals("Token không hợp lệ", ex.getMessage());
    }

    // =========================================================================================
    // SIGN IN BY GOOGLE (sheet signInByGoogle - UTCID01..05)
    // =========================================================================================

    /** Sheet signInByGoogle - UTCID01 (N): token hợp lệ, email đã có tài khoản ACTIVE -> đăng nhập. */
    @Test
    void TC_UNIT_IdentityService_030_googleLogin_happyPath() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("valid-google-token");

        GoogleTokenVerifier.GooglePayload payload =
                new GoogleTokenVerifier.GooglePayload("user@gmail.com", "User");

        User user = new User();
        user.setUserId(60L);
        user.setEmail("user@gmail.com");
        user.setStatus(UserStatus.ACTIVE);

        when(googleTokenVerifier.verify("valid-google-token")).thenReturn(payload);
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(platformMapper.toUserListItem(any(), any()))
                .thenReturn(UserListItemResponse.builder().displayName("User").build());
        when(jwtService.generateToken(anyLong(), anyString(), any(), anyLong())).thenReturn("jwt-token");

        var res = identityService.loginWithGoogle(req);

        assertFalse(res.isNewUser());
        verify(userRepository).save(user);
    }

    /** Sheet signInByGoogle - UTCID02 (N): email chưa có tài khoản -> trả cờ newUser để FE chuyển trang đăng ký. */
    @Test
    void TC_UNIT_IdentityService_031_googleLogin_newUserRedirectsToSignup() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("valid-google-token");

        GoogleTokenVerifier.GooglePayload payload =
                new GoogleTokenVerifier.GooglePayload("newone@gmail.com", "New One");

        when(googleTokenVerifier.verify("valid-google-token")).thenReturn(payload);
        when(userRepository.findByEmail("newone@gmail.com")).thenReturn(Optional.empty());

        var res = identityService.loginWithGoogle(req);

        assertTrue(res.isNewUser(), "Phải trả newUser=true để frontend chuyển sang trang đăng ký");
        assertEquals("newone@gmail.com", res.getEmail());
        verify(userRepository, never()).save(any());
    }

    /** Sheet signInByGoogle - UTCID03 (A): tài khoản đã bị khóa. */
    @Test
    void TC_UNIT_IdentityService_032_googleLogin_bannedUser() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("valid-google-token");

        GoogleTokenVerifier.GooglePayload payload =
                new GoogleTokenVerifier.GooglePayload("banned@gmail.com", "Banned");

        User user = new User();
        user.setEmail("banned@gmail.com");
        user.setStatus(UserStatus.BANNED);

        when(googleTokenVerifier.verify("valid-google-token")).thenReturn(payload);
        when(userRepository.findByEmail("banned@gmail.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.loginWithGoogle(req));
        assertEquals("Tài khoản của bạn đã bị khóa và không thể đăng nhập. Vui lòng liên hệ quản trị viên.",
                ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    /** Sheet signInByGoogle - UTCID04 (A): token Google không hợp lệ. */
    @Test
    void TC_UNIT_IdentityService_033_googleLogin_invalidToken() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("bad-token");

        when(googleTokenVerifier.verify("bad-token"))
                .thenThrow(new IllegalArgumentException("Google token không hợp lệ hoặc đã hết hạn."));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.loginWithGoogle(req));
        assertEquals("Google token không hợp lệ hoặc đã hết hạn.", ex.getMessage());
    }

    /** Sheet signInByGoogle - UTCID05 (A): tài khoản đang bị tạm ngừng. */
    @Test
    void TC_UNIT_IdentityService_034_googleLogin_suspendedUser() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("valid-google-token");

        GoogleTokenVerifier.GooglePayload payload =
                new GoogleTokenVerifier.GooglePayload("suspended@gmail.com", "Suspended");

        User user = new User();
        user.setEmail("suspended@gmail.com");
        user.setStatus(UserStatus.SUSPENDED);

        when(googleTokenVerifier.verify("valid-google-token")).thenReturn(payload);
        when(userRepository.findByEmail("suspended@gmail.com")).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.loginWithGoogle(req));
        assertEquals("Tài khoản của bạn đang bị tạm ngừng. Vui lòng liên hệ quản trị viên.", ex.getMessage());
    }

    // =========================================================================================
    // SIGN UP BY GOOGLE (sheet signUpByGoogle - UTCID01..03)
    // Test viết theo ĐẶC TẢ trong Report_5.1_UnitTest, không theo code, để phát hiện lệch spec.
    // =========================================================================================

    /**
     * DEF-09. register() chan role == null bang thong bao "Vai tro dang ky khong hop le",
     * nhung completeGoogleSignup() chi chan PLATFORM_ADMIN/UNKNOWN. Voi role = null,
     * user duoc save truoc roi createBaselineProfile() switch tren enum null -> NullPointerException
     * (HTTP 500) thay vi loi 400 co thong bao ro rang.
     */
    @Test
    void TC_UNIT_IdentityService_027b_googleSignup_nullRole() {
        GoogleCompleteRequest req = new GoogleCompleteRequest();
        req.setAccessToken("valid-google-token");
        req.setPhone("0123456789");
        req.setRole(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.completeGoogleSignup(req),
                "role = null phai bi chan bang IllegalArgumentException nhu register(), "
                        + "khong duoc de rot xuong NullPointerException");
        assertEquals("Vai trò đăng ký không hợp lệ", ex.getMessage());
    }

    /** Sheet signUpByGoogle - UTCID01 (N): token hợp lệ, email & phone chưa tồn tại -> đăng ký thành công. */
    @Test
    void TC_UNIT_IdentityService_027_googleSignup_happyPath() {
        GoogleCompleteRequest req = new GoogleCompleteRequest();
        req.setAccessToken("valid-google-token");
        req.setPhone("0123456789");
        req.setRole(UserRole.CLIENT);

        GoogleTokenVerifier.GooglePayload payload =
                new GoogleTokenVerifier.GooglePayload("newuser@gmail.com", "New User");

        when(googleTokenVerifier.verify("valid-google-token")).thenReturn(payload);
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0123456789")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setUserId(50L);
            return u;
        });
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(platformMapper.toUserListItem(any(), any()))
                .thenReturn(UserListItemResponse.builder().displayName("New User").build());

        identityService.completeGoogleSignup(req);

        verify(userRepository).save(any(User.class));
    }

    /** Sheet signUpByGoogle - UTCID02 (A): token Google không hợp lệ. */
    @Test
    void TC_UNIT_IdentityService_028_googleSignup_invalidToken() {
        GoogleCompleteRequest req = new GoogleCompleteRequest();
        req.setAccessToken("bad-token");
        req.setPhone("0123456789");
        req.setRole(UserRole.CLIENT);

        when(googleTokenVerifier.verify("bad-token"))
                .thenThrow(new IllegalArgumentException("Google token không hợp lệ hoặc đã hết hạn."));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.completeGoogleSignup(req));
        assertEquals("Google token không hợp lệ hoặc đã hết hạn.", ex.getMessage());
    }

    /** Sheet signUpByGoogle - UTCID03 (A): số điện thoại đã thuộc email khác. */
    @Test
    void TC_UNIT_IdentityService_029_googleSignup_phoneAlreadyUsed() {
        GoogleCompleteRequest req = new GoogleCompleteRequest();
        req.setAccessToken("valid-google-token");
        req.setPhone("0123456789");
        req.setRole(UserRole.CLIENT);

        GoogleTokenVerifier.GooglePayload payload =
                new GoogleTokenVerifier.GooglePayload("newuser@gmail.com", "New User");

        when(googleTokenVerifier.verify("valid-google-token")).thenReturn(payload);
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0123456789")).thenReturn(true);

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.completeGoogleSignup(req));
        assertEquals("Số điện thoại này đã được đăng kí bởi email khác", ex.getMessage());
    }

    // =========================================================================================
    //  Bo sung: sheet resetPassword UTCID08, signInByGoogle UTCID06-07, signUpByGoogle UTCID05-06
    // =========================================================================================

    /** Sheet resetPassword - UTCID08 (A): token con han nhung da duoc su dung roi. */
    @Test
    void TC_UNIT_IdentityService_024b_resetPassword_tokenAlreadyUsed() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("used-token");
        req.setNewPassword("New12345");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("used-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setUsedAt(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> identityService.resetPassword(req));
        assertEquals("Token đã hết hạn hoặc đã sử dụng", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    /** Sheet signInByGoogle - UTCID06 (A): Google tra ve email chua duoc xac thuc. */
    @Test
    void TC_UNIT_IdentityService_035_googleLogin_emailNotVerified() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("unverified-email-token");

        when(googleTokenVerifier.verify("unverified-email-token"))
                .thenThrow(new IllegalArgumentException("Email Google chưa được xác thực."));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.loginWithGoogle(req));
        assertEquals("Email Google chưa được xác thực.", ex.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    /** Sheet signInByGoogle - UTCID07 (A): token duoc cap cho client id khac (sai audience). */
    @Test
    void TC_UNIT_IdentityService_036_googleLogin_wrongAudience() {
        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setAccessToken("other-app-token");

        when(googleTokenVerifier.verify("other-app-token"))
                .thenThrow(new IllegalArgumentException("Google token không dành cho ứng dụng này."));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.loginWithGoogle(req));
        assertEquals("Google token không dành cho ứng dụng này.", ex.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    /** Sheet signUpByGoogle - UTCID05 (A): Google tra ve email chua duoc xac thuc. */
    @Test
    void TC_UNIT_IdentityService_037_googleSignup_emailNotVerified() {
        GoogleCompleteRequest req = new GoogleCompleteRequest();
        req.setAccessToken("unverified-email-token");
        req.setPhone("0123456789");
        req.setRole(UserRole.CLIENT);

        when(googleTokenVerifier.verify("unverified-email-token"))
                .thenThrow(new IllegalArgumentException("Email Google chưa được xác thực."));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.completeGoogleSignup(req));
        assertEquals("Email Google chưa được xác thực.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    /** Sheet signUpByGoogle - UTCID06 (A): token duoc cap cho client id khac (sai audience). */
    @Test
    void TC_UNIT_IdentityService_038_googleSignup_wrongAudience() {
        GoogleCompleteRequest req = new GoogleCompleteRequest();
        req.setAccessToken("other-app-token");
        req.setPhone("0123456789");
        req.setRole(UserRole.CLIENT);

        when(googleTokenVerifier.verify("other-app-token"))
                .thenThrow(new IllegalArgumentException("Google token không dành cho ứng dụng này."));

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> identityService.completeGoogleSignup(req));
        assertEquals("Google token không dành cho ứng dụng này.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // =========================================================================================
    //  Sheet: idRequestResetOtp  &  idVerifyResetOtp  (luong quen mat khau)
    // =========================================================================================

    private static final String RESET_EMAIL = "quenmatkhau@example.com";
    private static final String RATE_LIMIT = "Quá nhiều yêu cầu, vui lòng thử lại sau.";

    private com.tcs.module.identity.dto.request.RequestPasswordResetOtpRequest resetReq(String email) {
        var r = new com.tcs.module.identity.dto.request.RequestPasswordResetOtpRequest();
        r.setEmail(email);
        return r;
    }

    private com.tcs.module.identity.dto.request.VerifyPasswordResetOtpRequest verifyReq(String email, String code) {
        var r = new com.tcs.module.identity.dto.request.VerifyPasswordResetOtpRequest();
        r.setEmail(email);
        r.setCode(code);
        return r;
    }

    private com.tcs.module.identity.entity.EmailOtp resetOtp(String code, java.time.LocalDateTime lastSentAt) {
        var otp = new com.tcs.module.identity.entity.EmailOtp();
        otp.setEmail(RESET_EMAIL);
        otp.setCode(code);
        otp.setPurpose(com.tcs.module.identity.enums.OtpPurpose.PASSWORD_RESET);
        otp.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(0);
        otp.setLastSentAt(lastSentAt);
        return otp;
    }

    private void givenActiveResetOtp(com.tcs.module.identity.entity.EmailOtp otp) {
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                RESET_EMAIL, com.tcs.module.identity.enums.OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.ofNullable(otp));
    }

    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("idRequestResetOtp")
    class IdRequestResetOtp {

        private void givenAccountExists() {
            when(userRepository.findByEmail(RESET_EMAIL))
                    .thenReturn(java.util.Optional.of(new com.tcs.module.identity.entity.User()));
        }

        private void givenWithinWindow(long sentInWindow) {
            when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(
                    org.mockito.ArgumentMatchers.eq(RESET_EMAIL),
                    org.mockito.ArgumentMatchers.eq(com.tcs.module.identity.enums.OtpPurpose.PASSWORD_RESET),
                    org.mockito.ArgumentMatchers.any()))
                    .thenReturn(sentInWindow);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - email ton tai, chua co OTP cu -> sinh OTP va gui mail")
        void utcid01_issueResetOtp() {
            givenAccountExists();
            givenActiveResetOtp(null);
            givenWithinWindow(0);
            when(emailOtpRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

            identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), null);

            verify(emailService).sendPasswordResetOtp(org.mockito.ArgumentMatchers.eq(RESET_EMAIL),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (N) - email khong ton tai -> tra ve thong bao chung, KHONG gui mail")
        void utcid02_unknownEmailDoesNotLeak() {
            when(userRepository.findByEmail(RESET_EMAIL)).thenReturn(java.util.Optional.empty());

            var res = identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), null);

            assertEquals("Nếu email tồn tại, mã OTP đặt lại mật khẩu đã được gửi", res.getMessage());
            verify(emailService, never()).sendPasswordResetOtp(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - gui lai truoc thoi gian cho -> 'Quá nhiều yêu cầu, vui lòng thử lại sau.'")
        void utcid03_resendTooSoon() {
            givenAccountExists();
            givenActiveResetOtp(resetOtp("111111", java.time.LocalDateTime.now().minusSeconds(10)));

            Exception ex = assertThrows(IllegalArgumentException.class,
                    () -> identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), null));
            assertEquals(RATE_LIMIT, ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (B) - da du thoi gian cho (60s) -> cho phep, ma cu bi vo hieu")
        void utcid04_resendAfterCooldown() {
            givenAccountExists();
            var previous = resetOtp("111111", java.time.LocalDateTime.now().minusSeconds(60));
            givenActiveResetOtp(previous);
            givenWithinWindow(0);
            when(emailOtpRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

            identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), null);

            assertNotNull(previous.getConsumedAt(), "ma cu phai bi danh dau da dung");
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - vuot so lan gui trong cua so thoi gian -> chan")
        void utcid05_emailWindowExceeded() {
            givenAccountExists();
            givenActiveResetOtp(null);
            givenWithinWindow(5);

            Exception ex = assertThrows(IllegalArgumentException.class,
                    () -> identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), null));
            assertEquals(RATE_LIMIT, ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (B) - da gui 4/5 lan trong cua so -> van cho phep")
        void utcid06_emailWindowAtBoundary() {
            givenAccountExists();
            givenActiveResetOtp(null);
            givenWithinWindow(4);
            when(emailOtpRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

            identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), null);

            verify(emailService).sendPasswordResetOtp(org.mockito.ArgumentMatchers.eq(RESET_EMAIL),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (A) - IP het han muc trong gio -> chan")
        void utcid07_ipQuotaExhausted() {
            ReflectionTestUtils.setField(identityService, "maxPerIpPerHour", 0);
            givenAccountExists();
            givenActiveResetOtp(null);
            givenWithinWindow(0);

            Exception ex = assertThrows(IllegalArgumentException.class,
                    () -> identityService.requestPasswordResetOtp(resetReq(RESET_EMAIL), "1.2.3.4"));
            assertEquals(RATE_LIMIT, ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID08 (N) - email co hoa/thuong va khoang trang -> duoc chuan hoa truoc khi tra cuu")
        void utcid08_emailNormalised() {
            givenAccountExists();
            givenActiveResetOtp(null);
            givenWithinWindow(0);
            when(emailOtpRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

            var res = identityService.requestPasswordResetOtp(
                    resetReq("  QuenMatKhau@Example.COM  "), null);

            assertEquals(RESET_EMAIL, res.getEmail());
            verify(userRepository).findByEmail(RESET_EMAIL);
        }
    }

    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("idVerifyResetOtp")
    class IdVerifyResetOtp {

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - OTP dung -> tao PasswordResetToken va tra ve resetToken")
        void utcid01_verifySuccessfully() {
            givenActiveResetOtp(resetOtp("123456", java.time.LocalDateTime.now()));
            when(userRepository.findByEmail(RESET_EMAIL))
                    .thenReturn(java.util.Optional.of(new com.tcs.module.identity.entity.User()));
            when(passwordResetTokenRepository.save(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(i -> i.getArgument(0));

            var res = identityService.verifyPasswordResetOtp(verifyReq(RESET_EMAIL, "123456"), null);

            assertEquals("Xác thực OTP thành công", res.getMessage());
            assertNotNull(res.getResetToken());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (A) - IP het han muc -> 'Quá nhiều yêu cầu, vui lòng thử lại sau.'")
        void utcid02_ipQuotaExhausted() {
            ReflectionTestUtils.setField(identityService, "maxPerIpPerHour", 0);

            Exception ex = assertThrows(IllegalArgumentException.class,
                    () -> identityService.verifyPasswordResetOtp(verifyReq(RESET_EMAIL, "123456"), "1.2.3.4"));
            assertEquals(RATE_LIMIT, ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - OtpService.verify tu choi ma -> loi truyen ra, khong tao token")
        void utcid03_otpRejected() {
            givenActiveResetOtp(null);

            assertThrows(IllegalArgumentException.class,
                    () -> identityService.verifyPasswordResetOtp(verifyReq(RESET_EMAIL, "999999"), null));
            verify(passwordResetTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - OTP hop le nhung khong co tai khoan -> 'Mã OTP không hợp lệ'")
        void utcid04_userNotFound() {
            givenActiveResetOtp(resetOtp("123456", java.time.LocalDateTime.now()));
            when(userRepository.findByEmail(RESET_EMAIL)).thenReturn(java.util.Optional.empty());

            Exception ex = assertThrows(IllegalArgumentException.class,
                    () -> identityService.verifyPasswordResetOtp(verifyReq(RESET_EMAIL, "123456"), null));
            assertEquals("Mã OTP không hợp lệ", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (N) - email co hoa/thuong -> duoc chuan hoa truoc khi tra cuu")
        void utcid05_emailNormalised() {
            givenActiveResetOtp(resetOtp("123456", java.time.LocalDateTime.now()));
            when(userRepository.findByEmail(RESET_EMAIL))
                    .thenReturn(java.util.Optional.of(new com.tcs.module.identity.entity.User()));
            when(passwordResetTokenRepository.save(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(i -> i.getArgument(0));

            var res = identityService.verifyPasswordResetOtp(
                    verifyReq("  QuenMatKhau@Example.COM  ", "123456"), null);

            assertEquals(RESET_EMAIL, res.getEmail());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (B) - han token = now + tokenExpirationMinutes (15 phut)")
        void utcid06_tokenExpiry() {
            givenActiveResetOtp(resetOtp("123456", java.time.LocalDateTime.now()));
            when(userRepository.findByEmail(RESET_EMAIL))
                    .thenReturn(java.util.Optional.of(new com.tcs.module.identity.entity.User()));
            when(passwordResetTokenRepository.save(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(i -> i.getArgument(0));

            var res = identityService.verifyPasswordResetOtp(verifyReq(RESET_EMAIL, "123456"), null);

            assertEquals(15 * 60, res.getResetTokenExpiresInSeconds());
        }
    }
}
