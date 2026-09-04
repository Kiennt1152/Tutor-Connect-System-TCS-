package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.DuplicateEmailException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.identity.dto.request.GoogleLoginRequest;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.RequestPasswordResetOtpRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
import com.tcs.module.identity.dto.request.VerifyPasswordResetOtpRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
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
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.GoogleTokenVerifier;
import com.tcs.security.JwtService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52IdentityServiceITTest {

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
    void configureOtpPolicyForIdentityItCases() {
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

    @Test
    void SUPPORT_AUTH_LoginActiveUserIssuesJwtAndUpdatesLastLoginAtServiceLevel() {
        LoginRequest request = new LoginRequest();
        request.setEmail("client.it@tcs.test");
        request.setPassword("Password123");
        User user = activeUser(1L, "client.it@tcs.test");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "encoded-password")).thenReturn(true);
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(jwtService.generateToken(1L, "client.it@tcs.test", UserRole.CLIENT, 0L)).thenReturn("jwt-token");
        when(platformMapper.toUserListItem(any(), any()))
                .thenReturn(UserListItemResponse.builder().displayName("Client IT").build());

        AuthResponse response = identityService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(UserRole.CLIENT, response.getRole());
        assertNotNull(user.getLastLogin());
        verify(userRepository).save(user);
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_002_GetMeReturnsCurrentUserRoleStatusAndProfileDisplayName() {
        User user = activeUser(7L, "client.it@tcs.test");
        user.setPhone("0912345678");
        when(authHelper.currentUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(platformMapper.toUserListItem(eq(user), any()))
                .thenReturn(UserListItemResponse.builder().displayName("Client IT").build());

        var response = identityService.getMe();

        assertEquals(7L, response.getUserId());
        assertEquals("client.it@tcs.test", response.getEmail());
        assertEquals("0912345678", response.getPhone());
        assertEquals(UserRole.CLIENT, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertEquals("Client IT", response.getDisplayName());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_003_LoginResponseIncludesSessionProfileAndJwtExpiry() {
        LoginRequest request = new LoginRequest();
        request.setEmail("tutor.it@tcs.test");
        request.setPassword("Password123");
        User user = activeUser(2L, "tutor.it@tcs.test");
        user.setPasswordHash("encoded-password");
        user.setTokenVersion(4L);

        when(userRepository.findByEmail("tutor.it@tcs.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "encoded-password")).thenReturn(true);
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);
        when(jwtService.generateToken(2L, "tutor.it@tcs.test", UserRole.TUTOR, 4L)).thenReturn("jwt-tutor");
        when(platformMapper.toUserListItem(eq(user), any()))
                .thenReturn(UserListItemResponse.builder().displayName("Gia sư IT").build());
        ReflectionTestUtils.setField(identityService, "jwtExpirationMs", 3_600_000L);

        AuthResponse response = identityService.login(request);

        assertEquals("jwt-tutor", response.getAccessToken());
        assertEquals(2L, response.getUserId());
        assertEquals(UserRole.TUTOR, response.getRole());
        assertEquals("Gia sư IT", response.getDisplayName());
        assertEquals(3600L, response.getTokenExpiresInSeconds());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_004_RejectBlankLoginPayloadBeforeIdentityServiceIsCalled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);

            assertEquals(2, violations.size());
        }
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_005_RejectWrongPasswordWithoutCreatingJwt() {
        LoginRequest request = new LoginRequest();
        request.setEmail("client.it@tcs.test");
        request.setPassword("WrongPassword123");
        User user = activeUser(1L, "client.it@tcs.test");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123", "encoded-password")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.login(request));

        assertEquals("Email hoặc mật khẩu không đúng", exception.getMessage());
        verify(jwtService, never()).generateToken(anyLong(), anyString(), any(), anyLong());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_006_BlockAnonymousGetMeBeforeLoadingSessionUser() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> identityService.getMe());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_007_RejectSelfRegistrationWithPlatformAdminRole() {
        RegisterRequest request = registerRequest(UserRole.PLATFORM_ADMIN, "admin.it@tcs.test");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(request));

        assertEquals("Vai trò đăng ký không hợp lệ", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_008_RequestPasswordResetOtpConsumesPreviousPendingOtpBeforeIssuingNewOne() {
        RequestPasswordResetOtpRequest request = new RequestPasswordResetOtpRequest();
        request.setEmail("client.it@tcs.test");
        EmailOtp previousOtp = new EmailOtp();
        previousOtp.setEmail("client.it@tcs.test");
        previousOtp.setPurpose(OtpPurpose.PASSWORD_RESET);
        previousOtp.setCode("111111");
        previousOtp.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        previousOtp.setLastSentAt(LocalDateTime.now().minusMinutes(2));

        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.of(activeUser(1L, "client.it@tcs.test")));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(previousOtp));
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(anyString(), any(), any())).thenReturn(1L);

        var response = identityService.requestPasswordResetOtp(request, "127.0.0.1");

        assertEquals("client.it@tcs.test", response.getEmail());
        assertNotNull(previousOtp.getConsumedAt());
        verify(emailOtpRepository, times(3)).save(any(EmailOtp.class));
        verify(emailService).sendPasswordResetOtp(eq("client.it@tcs.test"), anyString(), anyLong());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_009_RejectRegistrationWhenEmailAlreadyExists() {
        RegisterRequest request = registerRequest(UserRole.CLIENT, "client.it@tcs.test");
        EmailVerificationToken token = verificationToken("verified-token", "client.it@tcs.test");

        when(emailVerificationTokenRepository.findByToken("verified-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("client.it@tcs.test"))
                .thenReturn(Optional.of(activeUser(1L, "client.it@tcs.test")));

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> identityService.register(request));

        assertEquals("Email này đã được đăng ký", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_017_RejectBannedAccountBeforePasswordVerification() {
        LoginRequest request = new LoginRequest();
        request.setEmail("banned.it@tcs.test");
        request.setPassword("Password123");
        User user = activeUser(2L, "banned.it@tcs.test");
        user.setStatus(UserStatus.BANNED);

        when(userRepository.findByEmail("banned.it@tcs.test")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.login(request));

        assertEquals(
                "Tài khoản của bạn đã bị khóa và không thể đăng nhập. Vui lòng liên hệ quản trị viên.",
                exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyLong(), anyString(), any(), anyLong());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_010_LogoutIncrementsTokenVersionAndRecordsAudit() {
        User user = activeUser(7L, "client.it@tcs.test");
        user.setTokenVersion(3L);

        when(authHelper.currentUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        identityService.logout();

        assertEquals(4L, user.getTokenVersion());
        verify(userRepository).save(user);
        verify(auditLogService).record(
                eq(7L),
                eq("LOGOUT"),
                eq("User"),
                eq(7L),
                eq(java.util.Map.of("tokenVersion", 3L)),
                eq(java.util.Map.of("tokenVersion", 4L)));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_011_SendRegistrationOtpStoresOtpAndSendsEmailToNormalizedAddress() {
        SendOtpRequest request = new SendOtpRequest();
        request.setEmail(" Client.IT@TCS.Test ");

        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.empty());
        when(emailOtpRepository.findFirstByEmailAndPurposeOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.REGISTRATION))
                .thenReturn(Optional.empty());
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(anyString(), any(), any()))
                .thenReturn(0L);
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        var response = identityService.sendOtp(request, "127.0.0.10");

        assertEquals("client.it@tcs.test", response.getEmail());
        assertEquals(300L, response.getOtpExpiresInSeconds());
        ArgumentCaptor<EmailOtp> otpCaptor = ArgumentCaptor.forClass(EmailOtp.class);
        verify(emailOtpRepository).save(otpCaptor.capture());
        assertEquals("client.it@tcs.test", otpCaptor.getValue().getEmail());
        assertEquals(OtpPurpose.REGISTRATION, otpCaptor.getValue().getPurpose());
        verify(emailService).sendRegistrationOtp(eq("client.it@tcs.test"), anyString(), eq(5L));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_012_VerifyRegistrationOtpCreatesReusableEmailVerificationTokenForRegisterStep() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("client.it@tcs.test");
        request.setCode("123456");
        EmailOtp otp = otp("client.it@tcs.test", OtpPurpose.REGISTRATION, "123456");

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(otp));

        var response = identityService.verifyOtp(request);

        assertEquals("client.it@tcs.test", response.getEmail());
        assertNotNull(response.getVerifiedEmailToken());
        assertNotNull(otp.getConsumedAt());
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_013_GoogleLoginUnknownEmailReturnsSignupContinuationWithoutCreatingAccount() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setAccessToken("google-access-token");

        when(googleTokenVerifier.verify("google-access-token"))
                .thenReturn(new GoogleTokenVerifier.GooglePayload("new.user@tcs.test", "Người dùng Google"));
        when(userRepository.findByEmail("new.user@tcs.test")).thenReturn(Optional.empty());

        var response = identityService.loginWithGoogle(request);

        assertEquals(true, response.isNewUser());
        assertEquals("new.user@tcs.test", response.getEmail());
        assertEquals("Người dùng Google", response.getSuggestedDisplayName());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(anyLong(), anyString(), any(), anyLong());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_014_WrongRegistrationOtpIncrementsAttemptCounterAndShowsRemainingAttempts() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("client.it@tcs.test");
        request.setCode("000000");
        EmailOtp otp = otp("client.it@tcs.test", OtpPurpose.REGISTRATION, "123456");
        otp.setAttempts(1);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(otp));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.verifyOtp(request));

        assertEquals("Mã xác thực không đúng. Bạn còn 3 lần thử.", exception.getMessage());
        assertEquals(2, otp.getAttempts());
        verify(emailOtpRepository).save(otp);
        verify(emailVerificationTokenRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_015_LoginWritesTraceableAuditEntryForPasswordSession() {
        LoginRequest request = new LoginRequest();
        request.setEmail("client.it@tcs.test");
        request.setPassword("Password123");
        User user = activeUser(1L, "client.it@tcs.test");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "encoded-password")).thenReturn(true);
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(jwtService.generateToken(1L, "client.it@tcs.test", UserRole.CLIENT, 0L)).thenReturn("jwt-token");
        when(platformMapper.toUserListItem(any(), any()))
                .thenReturn(UserListItemResponse.builder().displayName("Client IT").build());

        identityService.login(request);

        verify(auditLogService).record(
                eq(1L),
                eq("LOGIN"),
                eq("User"),
                eq(1L),
                eq(null),
                eq(java.util.Map.of("email", "client.it@tcs.test", "method", "PASSWORD")));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_016_RegisterClientCreatesBaselineProfileAndConsumesVerifiedEmailToken() {
        RegisterRequest request = registerRequest(UserRole.CLIENT, "client.it@tcs.test");
        EmailVerificationToken token = verificationToken("verified-token", "client.it@tcs.test");
        User savedUser = activeUser(30L, "client.it@tcs.test");

        when(emailVerificationTokenRepository.findByToken("verified-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0912345678")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = identityService.register(request);

        assertEquals("client.it@tcs.test", response.getEmail());
        assertNotNull(token.getConsumedAt());
        verify(clientRepository).save(any());
        verify(emailVerificationTokenRepository).save(token);
        verify(auditLogService).record(eq(30L), eq("REGISTER"), eq("User"), eq(30L), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_018_RejectExpiredPasswordResetTokenBeforeChangingPasswordHash() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("New12345");
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUser(activeUser(1L, "client.it@tcs.test"));
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.resetPassword(request));

        assertEquals("Token đã hết hạn hoặc đã sử dụng", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @Tag("report52-it")
    void IT_AUTH_019_RejectWeakPasswordWithVietnameseUserFacingMessage() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("password");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.resetPassword(request));

        assertEquals("Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số", exception.getMessage());
        verify(passwordResetTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void SUPPORT_AUTH_RequestPasswordResetOtpCreatesEmailOtpForExistingUser() {
        RequestPasswordResetOtpRequest request = new RequestPasswordResetOtpRequest();
        request.setEmail("client.it@tcs.test");

        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.of(activeUser(1L, "client.it@tcs.test")));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(anyString(), any(), any())).thenReturn(0L);

        var response = identityService.requestPasswordResetOtp(request, "127.0.0.1");

        assertEquals("client.it@tcs.test", response.getEmail());
        verify(emailOtpRepository).save(any(EmailOtp.class));
        verify(emailService).sendPasswordResetOtp(eq("client.it@tcs.test"), anyString(), anyLong());
    }

    @Test
    void SUPPORT_AUTH_VerifyPasswordResetOtpCreatesOneTimeResetToken() {
        VerifyPasswordResetOtpRequest request = new VerifyPasswordResetOtpRequest();
        request.setEmail("client.it@tcs.test");
        request.setCode("123456");
        EmailOtp otp = new EmailOtp();
        otp.setEmail("client.it@tcs.test");
        otp.setCode("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(0);

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "client.it@tcs.test",
                OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(otp));
        when(userRepository.findByEmail("client.it@tcs.test")).thenReturn(Optional.of(activeUser(1L, "client.it@tcs.test")));

        var response = identityService.verifyPasswordResetOtp(request, "127.0.0.1");

        assertEquals("client.it@tcs.test", response.getEmail());
        assertNotNull(response.getResetToken());
        assertNotNull(otp.getConsumedAt());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void SUPPORT_AUTH_ResetPasswordConsumesTokenAndStoresNewHashAtServiceLevel() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("New12345");
        User user = activeUser(1L, "client.it@tcs.test");
        user.setPasswordHash("old-hash");
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("reset-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("New12345")).thenReturn("new-hash");

        identityService.resetPassword(request);

        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
    }

    private User activeUser(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private RegisterRequest registerRequest(UserRole role, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setRole(role);
        request.setDisplayName("Người dùng IT");
        request.setPhone("0912345678");
        request.setPassword("Password123");
        request.setConfirmPassword("Password123");
        request.setVerifiedEmailToken("verified-token");
        return request;
    }

    private EmailVerificationToken verificationToken(String tokenValue, String email) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(tokenValue);
        token.setEmail(email);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return token;
    }

    private EmailOtp otp(String email, OtpPurpose purpose, String code) {
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setPurpose(purpose);
        otp.setCode(code);
        otp.setAttempts(0);
        otp.setLastSentAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return otp;
    }
}
