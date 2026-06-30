package com.tcs.module.identity.service.impl;

import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
import com.tcs.module.identity.dto.response.RegisterResponse;
import com.tcs.module.identity.dto.response.SendOtpResponse;
import com.tcs.module.identity.dto.response.VerifyOtpResponse;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.EmailVerificationToken;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.EmailVerificationTokenRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.AuthService;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RATE_LIMIT_MESSAGE = "Quá nhiều yêu cầu, vui lòng thử lại sau.";

    /** Bo dem so lan gui ma theo IP trong 1 gio (best-effort, theo tien trinh). */
    private final Map<String, Deque<Long>> ipRequestLog = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.otp.max-per-email-per-hour:3}")
    private int maxPerEmailPerHour;

    @Value("${app.otp.max-per-ip-per-hour:5}")
    private int maxPerIpPerHour;

    @Value("${app.verified-token.expiration-minutes:15}")
    private long tokenExpirationMinutes;

    // ============================================================ Send OTP

    @Override
    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());

        // BR-01 + AF-01: email da thuoc mot tai khoan -> khong gui ma.
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký");
        }

        // BR-07: cooldown toi thieu giua hai lan gui cho cung email.
        emailOtpRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION)
                .ifPresent(last -> {
                    long elapsed = Duration.between(last.getLastSentAt(), LocalDateTime.now()).getSeconds();
                    if (elapsed < resendCooldownSeconds) {
                        throw new IllegalArgumentException(RATE_LIMIT_MESSAGE);
                    }
                });

        // BR-07: gioi han so ma moi email trong 1 gio.
        long sentLastHour = emailOtpRepository.countByEmailAndPurposeAndCreatedAtAfter(
                email, OtpPurpose.REGISTRATION, LocalDateTime.now().minusHours(1));
        if (sentLastHour >= maxPerEmailPerHour) {
            throw new IllegalArgumentException(RATE_LIMIT_MESSAGE);
        }

        // BR-07: gioi han so ma moi IP trong 1 gio.
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

        emailService.sendRegistrationOtp(email, "", otp.getCode(), otpExpirationMinutes);

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
        // Da nhap sai du so lan cho phep -> khoa, khong cho thu tiep.
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

        // Thanh cong: tieu thu OTP va cap token chung nhan email da xac thuc (BR-05).
        otp.setConsumedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(generateOpaqueToken());
        token.setEmail(email);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
        tokenRepository.save(token);

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

        // BR-08 / AF-05b
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        // BR-05: kiem tra token chung nhan email da xac thuc.
        EmailVerificationToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Phiên xác thực email không hợp lệ. Vui lòng xác thực lại email."));
        if (token.isConsumed()) {
            throw new IllegalArgumentException(
                    "Phiên xác thực email đã được sử dụng. Vui lòng xác thực lại email.");
        }
        if (token.isExpired()) {
            // AF-06
            throw new IllegalArgumentException(
                    "Phiên xác thực email đã hết hạn. Vui lòng xác thực lại email.");
        }
        if (!token.getEmail().equals(email)) {
            throw new IllegalArgumentException("Mã xác thực email không khớp với email đăng ký.");
        }

        // BR-01 / AF-01 (kiem tra lai phong tranh dieu kien tranh chap)
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký");
        }

        // AF-05c: so dien thoai hop le va duy nhat.
        String phone = normalizePhone(request.getPhone());
        if (isPhoneTaken(phone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }

        // Tao tai khoan thang ACTIVE (BR-09).
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        createProfile(request, user, phone);
        createWallet(user);

        // Tieu thu token (dung mot lan - BR-05).
        token.setConsumedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return RegisterResponse.builder()
                .email(email)
                .message("Đăng ký thành công! Tài khoản của bạn đã được kích hoạt. Vui lòng đăng nhập.")
                .build();
    }

    // ============================================================== helpers

    private void createProfile(RegisterRequest request, User user, String phone) {
        String displayName = request.getDisplayName().trim();

        switch (request.getRole()) {
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
                tutor.setPhone(phone);
                tutorRepository.save(tutor);
            }
            case TUTOR_CENTER -> {
                TutorCenter center = new TutorCenter();
                center.setUser(user);
                center.setCompanyName(displayName);
                center.setPhone(phone);
                tutorCenterRepository.save(center);
            }
        }
    }

    private void createWallet(User user) {
        if (!walletRepository.existsById(user.getUserId())) {
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            walletRepository.save(wallet);
        }
    }

    private boolean isPhoneTaken(String phone) {
        return clientRepository.existsByPhone(phone)
                || tutorRepository.existsByPhone(phone)
                || tutorCenterRepository.existsByPhone(phone);
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

    /** Ghi nhan mot luot gui ma cho IP; tra ve false neu vuot gioi han 1 gio (BR-07). */
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
}
