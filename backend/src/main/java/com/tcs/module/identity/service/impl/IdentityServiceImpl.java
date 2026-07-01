package com.tcs.module.identity.service.impl;

import com.tcs.exception.DuplicateEmailException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.dto.request.ChangePasswordRequest;
import com.tcs.module.identity.dto.request.ForgotPasswordRequest;
import com.tcs.module.identity.dto.request.GoogleLoginRequest;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
import com.tcs.module.identity.dto.response.MeResponse;
import com.tcs.module.identity.dto.response.RegisterResponse;
import com.tcs.module.identity.dto.response.SendOtpResponse;
import com.tcs.module.identity.dto.response.VerifyOtpResponse;
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
import com.tcs.module.identity.service.IdentityService;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.Gender;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.GoogleTokenVerifier;
import com.tcs.security.JwtService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RATE_LIMIT_MESSAGE = "Quá nhiều yêu cầu, vui lòng thử lại sau.";

    /** Bo dem so lan gui ma theo IP trong 1 gio (best-effort, theo tien trinh). */
    private final Map<String, Deque<Long>> ipRequestLog = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final WalletRepository walletRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlatformMapper platformMapper;
    private final AuthHelper authHelper;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.otp.max-per-email-per-window:5}")
    private int maxPerEmailPerWindow;

    @Value("${app.otp.email-window-minutes:6}")
    private long emailWindowMinutes;

    @Value("${app.otp.max-per-ip-per-hour:5}")
    private int maxPerIpPerHour;

    @Value("${app.verified-token.expiration-minutes:15}")
    private long tokenExpirationMinutes;

    // ============================================================ Send OTP

    @Override
    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());

        // BR-UC01-01 + AF-01: email da thuoc tai khoan ACTIVE -> khong gui ma.
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email này đã được đăng ký");
        }

        // BR-UC01-07: cooldown toi thieu giua hai lan gui cho cung email.
        emailOtpRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION)
                .ifPresent(last -> {
                    long elapsed = Duration.between(last.getLastSentAt(), LocalDateTime.now()).getSeconds();
                    if (elapsed < resendCooldownSeconds) {
                        throw new IllegalArgumentException(RATE_LIMIT_MESSAGE);
                    }
                });

        // BR-UC01-07: gioi han so ma moi email trong cua so cau hinh (mac dinh 6 phut).
        long sentInWindow = emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(
                email, OtpPurpose.REGISTRATION, LocalDateTime.now().minusMinutes(emailWindowMinutes));
        if (sentInWindow >= maxPerEmailPerWindow) {
            throw new IllegalArgumentException(RATE_LIMIT_MESSAGE);
        }

        // BR-UC01-07: gioi han so ma moi IP trong 1 gio.
        if (!acquireIpSlot(clientIp)) {
            throw new IllegalArgumentException(RATE_LIMIT_MESSAGE);
        }

        // AF-03: gui ma moi lam vo hieu ma cu chua dung.
        emailOtpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION)
                .ifPresent(prev -> {
                    prev.setConsumedAt(LocalDateTime.now());
                    emailOtpRepository.save(prev);
                });

        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setCode(generateOtpCode());
        otp.setPurpose(OtpPurpose.REGISTRATION);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otp.setAttempts(0);
        otp.setLastSentAt(LocalDateTime.now());
        emailOtpRepository.save(otp);

        emailService.sendRegistrationOtp(email, otp.getCode(), otpExpirationMinutes);

        return SendOtpResponse.builder()
                .email(email)
                .message("Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.")
                .otpExpiresInSeconds(otpExpirationMinutes * 60)
                .resendCooldownSeconds(resendCooldownSeconds)
                .build();
    }

    // ========================================================== Verify OTP

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        EmailOtp otp = emailOtpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã xác thực không tồn tại. Vui lòng yêu cầu gửi lại mã."));

        if (otp.isExpired()) {
            throw new IllegalArgumentException("Mã xác thực đã hết hạn. Vui lòng yêu cầu gửi lại mã.");
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException(
                    "Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.");
        }
        if (!otp.getCode().equals(request.getCode().trim())) {
            otp.setAttempts(otp.getAttempts() + 1);
            emailOtpRepository.save(otp);
            if (otp.getAttempts() >= maxAttempts) {
                throw new IllegalArgumentException(
                        "Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.");
            }
            int remaining = maxAttempts - otp.getAttempts();
            throw new IllegalArgumentException("Mã xác thực không đúng. Bạn còn " + remaining + " lần thử.");
        }

        otp.setConsumedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(generateOpaqueToken());
        token.setEmail(email);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
        emailVerificationTokenRepository.save(token);

        return VerifyOtpResponse.builder()
                .email(email)
                .message("Xác thực email thành công.")
                .verifiedEmailToken(token.getToken())
                .tokenExpiresInSeconds(tokenExpirationMinutes * 60)
                .build();
    }

    // ============================================================ Register

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        // BR-UC01-08 / AF-05b
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        // BR-UC01-02 / AF-05, AF-05d
        validatePassword(request.getPassword());

        // BR-UC01-03
        if (request.getRole() == null
                || request.getRole() == UserRole.PLATFORM_ADMIN
                || request.getRole() == UserRole.UNKNOWN) {
            throw new IllegalArgumentException("Vai trò đăng ký không hợp lệ");
        }

        // BR-UC01-05: kiem tra token chung nhan email da xac thuc.
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByToken(request.getVerifiedEmailToken())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Phiên xác thực email không hợp lệ. Vui lòng xác thực lại email."));
        if (token.isConsumed()) {
            throw new IllegalArgumentException(
                    "Phiên xác thực email đã được sử dụng. Vui lòng xác thực lại email.");
        }
        if (token.isExpired()) {
            throw new IllegalArgumentException(
                    "Phiên xác thực email đã hết hạn. Vui lòng xác thực lại email.");
        }
        if (!token.getEmail().equals(email)) {
            throw new IllegalArgumentException("Mã xác thực email không khớp với email đăng ký.");
        }

        // BR-UC01-01
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email này đã được đăng ký");
        }

        // BR-UC01-06: so dien thoai hop le va duy nhat toan he thong.
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        createBaselineProfile(savedUser, request.getRole(), request.getDisplayName().trim(), phone);

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        walletRepository.save(wallet);

        // Tieu thu token (dung mot lan - BR-UC01-05).
        token.setConsumedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(token);

        return RegisterResponse.builder()
                .email(email)
                .message("Đăng ký thành công! Tài khoản của bạn đã được kích hoạt. Vui lòng đăng nhập.")
                .build();
    }

    // ============================================================== Login

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa hoặc tạm ngưng");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserProfileBundle profiles = loadProfiles(user.getUserId());
        UserRole role = platformMapper.resolveRole(profiles);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), role);
        return buildAuthResponse(user, profiles, token);
    }

    // ======================================================= Login by Google

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GooglePayload payload = googleTokenVerifier.verify(request.getAccessToken());
        String email = normalizeEmail(payload.getEmail());

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Lan dau dang nhap Google -> tu tao tai khoan CLIENT (khong co mat khau dung duoc).
            user = provisionGoogleClient(email, payload.getName());
        } else if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa hoặc tạm ngưng");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserProfileBundle profiles = loadProfiles(user.getUserId());
        UserRole role = platformMapper.resolveRole(profiles);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), role);
        return buildAuthResponse(user, profiles, token);
    }

    /**
     * Tao tai khoan CLIENT cho nguoi dung Google moi. Mat khau la chuoi ngau nhien (khong dung de
     * dang nhap bang password). Ten hien thi lay tu Google; SDT de trong (bo sung sau khi dang nhap).
     */
    private User provisionGoogleClient(String email, String googleName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        String displayName = (googleName == null || googleName.isBlank())
                ? email.substring(0, email.indexOf('@'))
                : googleName.trim();
        createBaselineProfile(savedUser, UserRole.CLIENT, displayName, "");

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        walletRepository.save(wallet);

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getMe() {
        Long userId = authHelper.currentUserId();
        User user = findUser(userId);
        UserProfileBundle profiles = loadProfiles(userId);
        return MeResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(platformMapper.resolveRole(profiles))
                .status(user.getStatus())
                .displayName(platformMapper.toUserListItem(user, profiles).getDisplayName())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = findUser(authHelper.currentUserId());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().trim().toLowerCase()).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(LocalDateTime.now().plusHours(24));
            passwordResetTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đã hết hạn hoặc đã sử dụng");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    // ============================================================== helpers

    /**
     * Tao ho so baseline theo vai tro (BR-UC01-09). Chi dung ten hien thi va so dien thoai;
     * cac truong NOT NULL khac (gioi tinh, dia chi trung tam) duoc gan gia tri mac dinh
     * va bo sung sau khi dang nhap. Tutor/Tutor Center mac dinh verification_status = UNDER_VERIFY.
     */
    private void createBaselineProfile(User user, UserRole role, String displayName, String phone) {
        switch (role) {
            case CLIENT -> {
                Client client = new Client();
                client.setUser(user);
                client.setFullName(displayName);
                client.setPhone(phone);
                clientRepository.save(client);
            }
            case TUTOR -> {
                Tutor tutor = new Tutor();
                tutor.setUser(user);
                tutor.setFullName(displayName);
                tutor.setGender(Gender.OTHER);
                tutor.setPhone(phone);
                tutorRepository.save(tutor);
            }
            case TUTOR_CENTER -> {
                TutorCenter center = new TutorCenter();
                center.setUser(user);
                center.setCompanyName(displayName);
                center.setPhone(phone);
                center.setAddress("N/A");
                tutorCenterRepository.save(center);
            }
            default -> throw new IllegalArgumentException("Vai trò đăng ký không hợp lệ");
        }
    }

    private void validatePassword(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) > 127) {
                throw new IllegalArgumentException(
                        "Mật khẩu không được chứa ký tự có dấu hoặc ký tự không thuộc ASCII");
            }
        }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số");
        }
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        return String.format("%0" + otpLength + "d", RANDOM.nextInt(bound));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Ghi nhan mot luot gui ma cho IP; tra ve false neu vuot gioi han 1 gio (BR-UC01-07). */
    private boolean acquireIpSlot(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long windowStart = now - Duration.ofHours(1).toMillis();
        Deque<Long> log = ipRequestLog.computeIfAbsent(clientIp, key -> new ArrayDeque<>());
        synchronized (log) {
            while (!log.isEmpty() && log.peekFirst() < windowStart) {
                log.pollFirst();
            }
            if (log.size() >= maxPerIpPerHour) {
                return false;
            }
            log.addLast(now);
            return true;
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Chuan hoa so dien thoai VN: +84xxxxxxxxx -> 0xxxxxxxxx. */
    private String normalizePhone(String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        if (trimmed.startsWith("+84")) {
            return "0" + trimmed.substring(3);
        }
        return trimmed;
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                null,
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
    }

    private User findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private AuthResponse buildAuthResponse(User user, UserProfileBundle profiles, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(platformMapper.resolveRole(profiles))
                .displayName(platformMapper.toUserListItem(user, profiles).getDisplayName())
                .status(user.getStatus())
                .build();
    }
}
