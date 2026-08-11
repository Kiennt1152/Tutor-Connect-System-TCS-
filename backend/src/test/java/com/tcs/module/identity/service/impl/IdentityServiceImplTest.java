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
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
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
        when(jwtService.generateToken(1L, "test@gmail.com", UserRole.CLIENT)).thenReturn("jwt-token");
        
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
}
