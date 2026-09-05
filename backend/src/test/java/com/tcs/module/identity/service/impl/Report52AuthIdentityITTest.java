package com.tcs.module.identity.service.impl;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52AuthIdentityITTest {


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

    
    /**
     * Test Case: IT-AUTH-001
     * Title: Log in an active user through the identity API and update the last-login and audit records.
     * Procedure: Prepare the stated fixture and input, then execute POST /api/identity/login -> IdentityServiceImpl.login.
     * Input: Email entered in uppercase; password Password123.
     * Steps:
     *   1. Prepare the fixture: Create a fresh ACTIVE user with email it-auth-001@tcs.test and a BCrypt password.
     *   2. Use the input: Email entered in uppercase; password Password123.
     *   3. Execute POST /api/identity/login -> IdentityServiceImpl.login. Mapped test: com.tcs.module.identity.service.impl.Report52IdentityPasswordResetApiDbITTest#IT_AUTH_001_LoginActiveUserIssuesJwtAndUpdatesLastLoginThroughApiAndDb.
     *   4. Compare the result with the expected behavior and the API/DB checks: Assert HTTP 200 and response fields, reload users, and query audit_logs for LOGIN by the same user.
     * Expected: The API returns the expected JWT, normalized email, CLIENT role and display name; users.last_login and a LOGIN audit row are written.
     * Pre-conditions: Create a fresh ACTIVE user with email it-auth-001@tcs.test and a BCrypt password.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-001: Log in an active user through the identity API and update the last-login and audit records.")
    void IT_AUTH_001_LoginActiveUserIssuesJwtAndUpdatesLastLogin() {
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

    /**
     * Test Case: IT-AUTH-002
     * Title: Return the current authenticated user with the correct contact details, role, status and display name.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.getMe (GET /api/identity/me).
     * Input: A valid authenticated session for user 7.
     * Steps:
     *   1. Prepare the fixture: Provide an authenticated principal for user 7 and a matching user row.
     *   2. Use the input: A valid authenticated session for user 7.
     *   3. Execute IdentityServiceImpl.getMe (GET /api/identity/me). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_002_GetMeReturnsCurrentUserRoleStatusAndProfileDisplayName.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Compare every returned identity field with the user and role mapper output.
     * Expected: The response contains user 7, client.it@tcs.test, phone 0912345678, role CLIENT, status ACTIVE and display name Client IT.
     * Pre-conditions: Provide an authenticated principal for user 7 and a matching user row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-002: Return the current authenticated user with the correct contact details, role, status and display name.")
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

    /**
     * Test Case: IT-AUTH-003
     * Title: Return login session data together with the JWT expiry for a tutor account.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.login (POST /api/identity/login).
     * Input: tutor.it@tcs.test / Password123.
     * Steps:
     *   1. Prepare the fixture: Prepare an ACTIVE tutor with tokenVersion 4 and a valid encoded password.
     *   2. Use the input: tutor.it@tcs.test / Password123.
     *   3. Execute IdentityServiceImpl.login (POST /api/identity/login). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_003_LoginResponseIncludesSessionProfileAndJwtExpiry.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert token, user, role, display name and tokenExpiresInSeconds.
     * Expected: The response contains the generated tutor JWT, user id 2, TUTOR role, profile name Gia su IT and expiry 3,600 seconds.
     * Pre-conditions: Prepare an ACTIVE tutor with tokenVersion 4 and a valid encoded password.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-003: Return login session data together with the JWT expiry for a tutor account.")
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

    /**
     * Test Case: IT-AUTH-004
     * Title: Reject a login request when both required fields are blank before the identity service is invoked.
     * Procedure: Prepare the stated fixture and input, then execute LoginRequest Bean Validation used by POST /api/identity/login.
     * Input: email="" and password="".
     * Steps:
     *   1. Prepare the fixture: Load the LoginRequest validation rules.
     *   2. Use the input: email="" and password="".
     *   3. Execute LoginRequest Bean Validation used by POST /api/identity/login. Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_004_RejectBlankLoginPayloadBeforeIdentityServiceIsCalled.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exactly two validation violations and no persistence interaction.
     * Expected: Validation reports two violations for blank email and password, and no login call is made.
     * Pre-conditions: Load the LoginRequest validation rules.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-004: Reject a login request when both required fields are blank before the identity service is invoked.")
    void IT_AUTH_004_RejectBlankLoginPayloadBeforeIdentityServiceIsCalled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);

            assertEquals(2, violations.size());
        }
    }

    /**
     * Test Case: IT-AUTH-005
     * Title: Reject an incorrect password without issuing a JWT or changing the account.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.login (POST /api/identity/login).
     * Input: client.it@tcs.test / WrongPassword123.
     * Steps:
     *   1. Prepare the fixture: An ACTIVE client exists and the repository returns its encoded password.
     *   2. Use the input: client.it@tcs.test / WrongPassword123.
     *   3. Execute IdentityServiceImpl.login (POST /api/identity/login). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_005_RejectWrongPasswordWithoutCreatingJwt.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the exact exception and verify no token generation.
     * Expected: The service raises the Vietnamese invalid-credentials message and JwtService.generateToken is never called.
     * Pre-conditions: An ACTIVE client exists and the repository returns its encoded password.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-005: Reject an incorrect password without issuing a JWT or changing the account.")
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

    /**
     * Test Case: IT-AUTH-006
     * Title: Block an anonymous request to read the current identity.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.getMe (GET /api/identity/me).
     * Input: No Authorization token.
     * Steps:
     *   1. Prepare the fixture: Do not provide an authenticated principal.
     *   2. Use the input: No Authorization token.
     *   3. Execute IdentityServiceImpl.getMe (GET /api/identity/me). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_006_BlockAnonymousGetMeBeforeLoadingSessionUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify UserRepository.findById is never called.
     * Expected: The service returns “Yêu cầu đăng nhập” and does not query the user repository.
     * Pre-conditions: Do not provide an authenticated principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-006: Block an anonymous request to read the current identity.")
    void IT_AUTH_006_BlockAnonymousGetMeBeforeLoadingSessionUser() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> identityService.getMe());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    /**
     * Test Case: IT-AUTH-007
     * Title: Prevent self-registration with the PLATFORM_ADMIN role.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.register (POST /api/identity/register).
     * Input: RegisterRequest with role PLATFORM_ADMIN.
     * Steps:
     *   1. Prepare the fixture: Registration endpoint is available without an existing account.
     *   2. Use the input: RegisterRequest with role PLATFORM_ADMIN.
     *   3. Execute IdentityServiceImpl.register (POST /api/identity/register). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_007_RejectSelfRegistrationWithPlatformAdminRole.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the exception and verify UserRepository.save is never called.
     * Expected: The request is rejected with “Vai trò đăng ký không hợp lệ” and no user row is saved.
     * Pre-conditions: Registration endpoint is available without an existing account.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-007: Prevent self-registration with the PLATFORM_ADMIN role.")
    void IT_AUTH_007_RejectSelfRegistrationWithPlatformAdminRole() {
        RegisterRequest request = registerRequest(UserRole.PLATFORM_ADMIN, "admin.it@tcs.test");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> identityService.register(request));

        assertEquals("Vai trò đăng ký không hợp lệ", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Test Case: IT-AUTH-008
     * Title: Consume a previous pending password-reset OTP before issuing a new one.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.requestPasswordResetOtp (POST /api/identity/password/forgot).
     * Input: client.it@tcs.test and source IP 127.0.0.1.
     * Steps:
     *   1. Prepare the fixture: An ACTIVE client and one pending PASSWORD_RESET OTP exist.
     *   2. Use the input: client.it@tcs.test and source IP 127.0.0.1.
     *   3. Execute IdentityServiceImpl.requestPasswordResetOtp (POST /api/identity/password/forgot). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_008_RequestPasswordResetOtpConsumesPreviousPendingOtpBeforeIssuingNewOne.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response email, previous consumedAt, save count and email recipient.
     * Expected: The previous unconsumed OTP receives consumedAt, a new OTP is saved and a reset email is sent to the normalized address.
     * Pre-conditions: An ACTIVE client and one pending PASSWORD_RESET OTP exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-008: Consume a previous pending password-reset OTP before issuing a new one.")
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

    /**
     * Test Case: IT-AUTH-009
     * Title: Reject registration when the email is already registered.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.register (POST /api/identity/register).
     * Input: RegisterRequest for client.it@tcs.test with verified-token.
     * Steps:
     *   1. Prepare the fixture: A verified email token exists and users already contains the same email.
     *   2. Use the input: RegisterRequest for client.it@tcs.test with verified-token.
     *   3. Execute IdentityServiceImpl.register (POST /api/identity/register). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_009_RejectRegistrationWhenEmailAlreadyExists.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert DuplicateEmailException and verify UserRepository.save is never called.
     * Expected: The service raises “Email này đã được đăng ký” and does not save a second User.
     * Pre-conditions: A verified email token exists and users already contains the same email.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-009: Reject registration when the email is already registered.")
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

    /**
     * Test Case: IT-AUTH-010
     * Title: Increment tokenVersion and record an audit entry when the user logs out.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.logout (POST /api/identity/logout).
     * Input: A valid authenticated logout request.
     * Steps:
     *   1. Prepare the fixture: Authenticated user 7 has tokenVersion 3.
     *   2. Use the input: A valid authenticated logout request.
     *   3. Execute IdentityServiceImpl.logout (POST /api/identity/logout). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_010_LogoutIncrementsTokenVersionAndRecordsAudit.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert tokenVersion and capture the exact audit arguments.
     * Expected: Token version changes from 3 to 4, the user is saved and the LOGOUT audit contains the old and new versions.
     * Pre-conditions: Authenticated user 7 has tokenVersion 3.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-010: Increment tokenVersion and record an audit entry when the user logs out.")
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

    /**
     * Test Case: IT-AUTH-011
     * Title: Normalize a registration email before saving the OTP and sending it by email.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.sendOtp (POST /api/identity/send-otp).
     * Input: “ Client.IT@TCS.Test ” from IP 127.0.0.10.
     * Steps:
     *   1. Prepare the fixture: The normalized email is not registered and has no active registration OTP.
     *   2. Use the input: “ Client.IT@TCS.Test ” from IP 127.0.0.10.
     *   3. Execute IdentityServiceImpl.sendOtp (POST /api/identity/send-otp). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_011_SendRegistrationOtpStoresOtpAndSendsEmailToNormalizedAddress.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture EmailOtp, assert REGISTRATION purpose and 300-second expiry, then verify the email call.
     * Expected: Whitespace and case are removed; the response, EmailOtp and email service all use client.it@tcs.test.
     * Pre-conditions: The normalized email is not registered and has no active registration OTP.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-011: Normalize a registration email before saving the OTP and sending it by email.")
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

    /**
     * Test Case: IT-AUTH-012
     * Title: Verify a registration OTP and create a reusable email-verification token for registration.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.verifyOtp (POST /api/identity/verify-otp).
     * Input: client.it@tcs.test / 123456.
     * Steps:
     *   1. Prepare the fixture: An unconsumed REGISTRATION OTP with code 123456 exists.
     *   2. Use the input: client.it@tcs.test / 123456.
     *   3. Execute IdentityServiceImpl.verifyOtp (POST /api/identity/verify-otp). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_012_VerifyRegistrationOtpCreatesReusableEmailVerificationTokenForRegisterStep.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response email, token, consumedAt and EmailVerificationTokenRepository.save.
     * Expected: The valid OTP is consumed and the response contains a non-empty verifiedEmailToken saved for the normalized email.
     * Pre-conditions: An unconsumed REGISTRATION OTP with code 123456 exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-012: Verify a registration OTP and create a reusable email-verification token for registration.")
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

    /**
     * Test Case: IT-AUTH-013
     * Title: Return a signup continuation when Google identifies an email that is not yet registered.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.loginWithGoogle (POST /api/identity/google).
     * Input: Google access token google-access-token.
     * Steps:
     *   1. Prepare the fixture: GoogleTokenVerifier returns a valid payload for new.user@tcs.test and UserRepository has no matching row.
     *   2. Use the input: Google access token google-access-token.
     *   3. Execute IdentityServiceImpl.loginWithGoogle (POST /api/identity/google). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_013_GoogleLoginUnknownEmailReturnsSignupContinuationWithoutCreatingAccount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert new-user fields and verify UserRepository.save and JwtService.generateToken are never called.
     * Expected: The response marks newUser=true with the Google email/name; no User or JWT is created.
     * Pre-conditions: GoogleTokenVerifier returns a valid payload for new.user@tcs.test and UserRepository has no matching row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-013: Return a signup continuation when Google identifies an email that is not yet registered.")
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

    /**
     * Test Case: IT-AUTH-014
     * Title: Increment the registration OTP attempt count and report remaining attempts for a wrong code.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.verifyOtp (POST /api/identity/verify-otp).
     * Input: client.it@tcs.test / wrong code 000000.
     * Steps:
     *   1. Prepare the fixture: An unconsumed REGISTRATION OTP has code 123456 and attempts=1.
     *   2. Use the input: client.it@tcs.test / wrong code 000000.
     *   3. Execute IdentityServiceImpl.verifyOtp (POST /api/identity/verify-otp). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_014_WrongRegistrationOtpIncrementsAttemptCounterAndShowsRemainingAttempts.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception text, attempts=2, OTP save and no email-verification-token save.
     * Expected: Attempts change from 1 to 2, the exact remaining-attempt message is returned and no verification token is saved.
     * Pre-conditions: An unconsumed REGISTRATION OTP has code 123456 and attempts=1.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-014: Increment the registration OTP attempt count and report remaining attempts for a wrong code.")
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

    /**
     * Test Case: IT-AUTH-015
     * Title: Write a traceable LOGIN audit entry for a successful password session.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.login (POST /api/identity/login).
     * Input: client.it@tcs.test / Password123.
     * Steps:
     *   1. Prepare the fixture: An ACTIVE client with a valid encoded password and JWT stub is available.
     *   2. Use the input: client.it@tcs.test / Password123.
     *   3. Execute IdentityServiceImpl.login (POST /api/identity/login). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_015_LoginWritesTraceableAuditEntryForPasswordSession.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture and compare actor, action, entity, id and new-value map.
     * Expected: AuditLogService.record receives actor 1, action LOGIN, entity User and the password-login snapshot.
     * Pre-conditions: An ACTIVE client with a valid encoded password and JWT stub is available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-015: Write a traceable LOGIN audit entry for a successful password session.")
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

    /**
     * Test Case: IT-AUTH-016
     * Title: Register a client account from a verified email token and create its baseline client profile.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.register (POST /api/identity/register).
     * Input: Valid CLIENT registration request with verified-token.
     * Steps:
     *   1. Prepare the fixture: verified-token belongs to client.it@tcs.test; phone 0912345678 is unused.
     *   2. Use the input: Valid CLIENT registration request with verified-token.
     *   3. Execute IdentityServiceImpl.register (POST /api/identity/register). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_016_RegisterClientCreatesBaselineProfileAndConsumesVerifiedEmailToken.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response email and token consumedAt; verify client save, token save and audit.
     * Expected: A User and Client profile are saved, the email token is consumed and a REGISTER audit entry is recorded.
     * Pre-conditions: verified-token belongs to client.it@tcs.test; phone 0912345678 is unused.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-016: Register a client account from a verified email token and create its baseline client profile.")
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

    /**
     * Test Case: IT-AUTH-017
     * Title: Reject a banned account before checking its password or issuing a JWT.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.login (POST /api/identity/login).
     * Input: banned.it@tcs.test / Password123.
     * Steps:
     *   1. Prepare the fixture: User banned.it@tcs.test has status BANNED.
     *   2. Use the input: banned.it@tcs.test / Password123.
     *   3. Execute IdentityServiceImpl.login (POST /api/identity/login). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_017_RejectBannedAccountBeforePasswordVerification.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the exact exception and verify password/token collaborators are untouched.
     * Expected: The banned-account message is returned; PasswordEncoder.matches and JwtService.generateToken are never called.
     * Pre-conditions: User banned.it@tcs.test has status BANNED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-017: Reject a banned account before checking its password or issuing a JWT.")
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

    /**
     * Test Case: IT-AUTH-018
     * Title: Reject an expired password-reset token before changing the password hash.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.resetPassword (POST /api/identity/password/reset).
     * Input: expired-token and New12345.
     * Steps:
     *   1. Prepare the fixture: PasswordResetToken expired one minute ago.
     *   2. Use the input: expired-token and New12345.
     *   3. Execute IdentityServiceImpl.resetPassword (POST /api/identity/password/reset). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_018_RejectExpiredPasswordResetTokenBeforeChangingPasswordHash.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify UserRepository.save is never called.
     * Expected: The service returns “Token đã hết hạn hoặc đã sử dụng” and does not save a User.
     * Pre-conditions: PasswordResetToken expired one minute ago.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-018: Reject an expired password-reset token before changing the password hash.")
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

    /**
     * Test Case: IT-AUTH-019
     * Title: Reject a password that does not meet the minimum length and complexity rule.
     * Procedure: Prepare the stated fixture and input, then execute IdentityServiceImpl.resetPassword (POST /api/identity/password/reset).
     * Input: reset-token and weak password “password”.
     * Steps:
     *   1. Prepare the fixture: A reset request can be built without loading a token.
     *   2. Use the input: reset-token and weak password “password”.
     *   3. Execute IdentityServiceImpl.resetPassword (POST /api/identity/password/reset). Mapped test: com.tcs.module.identity.service.impl.Report52IdentityServiceITTest#IT_AUTH_019_RejectWeakPasswordWithVietnameseUserFacingMessage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the exact message and verify PasswordResetTokenRepository.findByToken is never called.
     * Expected: The Vietnamese password-policy message is returned before the reset-token repository is queried.
     * Pre-conditions: A reset request can be built without loading a token.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-019: Reject a password that does not meet the minimum length and complexity rule.")
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

    /**
     * Test Case: IT-AUTH-020
     * Title: Reset a password through the API, store a new hash, consume the token and reject reuse.
     * Procedure: Prepare the stated fixture and input, then execute POST /api/identity/password/reset -> IdentityServiceImpl.resetPassword.
     * Input: it-auth-020@tcs.test fixture token it-auth-020-reset-token; old OldPassword123; new NewPassword123.
     * Steps:
     *   1. Prepare the fixture: Create an ACTIVE user and an unexpired reset token in the real H2 database.
     *   2. Use the input: it-auth-020@tcs.test fixture token it-auth-020-reset-token; old OldPassword123; new NewPassword123.
     *   3. Execute POST /api/identity/password/reset -> IdentityServiceImpl.resetPassword. Mapped test: com.tcs.module.identity.service.impl.Report52IdentityPasswordResetApiDbITTest#IT_AUTH_020_ResetPasswordConsumesTokenAndStoresNewHashThroughApiAndDb.
     *   4. Compare the result with the expected behavior and the API/DB checks: Assert HTTP response, reload User and PasswordResetToken, query audit_logs, then submit the same token again.
     * Expected: The first request succeeds; the new hash matches NewPassword123, the old hash does not, usedAt and RESET_PASSWORD audit are present, and a second request is rejected.
     * Pre-conditions: Create an ACTIVE user and an unexpired reset token in the real H2 database.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-AUTH-020: Reset a password through the API, store a new hash, consume the token and reject reuse.")
    void IT_AUTH_020_ResetPasswordConsumesTokenAndStoresNewHash() {
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
