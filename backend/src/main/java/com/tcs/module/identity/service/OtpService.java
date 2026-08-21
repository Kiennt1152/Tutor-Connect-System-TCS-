package com.tcs.module.identity.service;

import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Dịch vụ OTP dùng chung cho mọi luồng: đăng ký, quên mật khẩu, ký hợp đồng.
 * Mọi mã lưu ở bảng {@code email_otps} theo (email, purpose). Việc gửi email và
 * giới hạn tần suất (cooldown/rate-limit) do bên gọi tự lo; ở đây chỉ sinh, lưu và xác minh mã.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpRepository emailOtpRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Vô hiệu hoá mã còn hiệu lực (chưa dùng) của (email, purpose). */
    public void invalidateActive(String email, OtpPurpose purpose) {
        emailOtpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .ifPresent(prev -> {
                    prev.setConsumedAt(LocalDateTime.now());
                    emailOtpRepository.save(prev);
                });
    }

    /** Sinh mã mới (vô hiệu mã cũ trước), lưu và trả về bản ghi. KHÔNG tự gửi email. */
    public EmailOtp issue(String email, OtpPurpose purpose, int codeLength, Duration ttl) {
        invalidateActive(email, purpose);
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setCode(randomCode(codeLength));
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plus(ttl));
        otp.setAttempts(0);
        otp.setLastSentAt(LocalDateTime.now());
        emailOtpRepository.save(otp);
        return otp;
    }

    /** Sinh chuỗi số ngẫu nhiên độ dài cho trước (vd 6 -> "004213"). */
    public String randomCode(int codeLength) {
        int bound = (int) Math.pow(10, codeLength);
        return String.format("%0" + codeLength + "d", RANDOM.nextInt(bound));
    }

    /**
     * Xác minh mã theo {@code policy}. Đúng: đánh dấu đã dùng và trả về bản ghi.
     * Sai/hết hạn/hết lượt: ném ngoại lệ (đều kế thừa {@link IllegalArgumentException}) với
     * thông báo tương ứng để giữ nguyên hành vi từng luồng.
     */
    public EmailOtp verify(String email, OtpPurpose purpose, String code, OtpVerifyPolicy policy) {
        if (!StringUtils.hasText(code)) {
            throw new OtpInvalidException(policy.getMissingMessage());
        }
        EmailOtp otp = emailOtpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new OtpNotFoundException(policy.getNotFoundMessage()));
        if (otp.isExpired()) {
            throw new OtpExpiredException(policy.getExpiredMessage());
        }
        if (otp.getAttempts() >= policy.getMaxAttempts()) {
            throw new OtpMaxAttemptsException(policy.getMaxAttemptsMessage());
        }
        if (!otp.getCode().equals(code.trim())) {
            otp.setAttempts(otp.getAttempts() + 1);
            boolean reachedMax = otp.getAttempts() >= policy.getMaxAttempts();
            if (policy.isLockOnMaxAttempts() && reachedMax) {
                otp.setConsumedAt(LocalDateTime.now());
            }
            emailOtpRepository.save(otp);
            if (policy.isThrowMaxOnReach() && reachedMax) {
                throw new OtpMaxAttemptsException(policy.getMaxAttemptsMessage());
            }
            if (policy.isShowRemaining()) {
                int remaining = Math.max(0, policy.getMaxAttempts() - otp.getAttempts());
                throw new OtpInvalidException(String.format(policy.getWrongRemainingTemplate(), remaining));
            }
            throw new OtpInvalidException(policy.getWrongMessage());
        }
        otp.setConsumedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);
        return otp;
    }

    // ---- Ngoại lệ: đều kế thừa IllegalArgumentException để giữ HTTP 400 như trước. ----

    /** Gốc cho mọi lỗi OTP. */
    public static class OtpException extends IllegalArgumentException {
        public OtpException(String message) {
            super(message);
        }
    }

    public static class OtpNotFoundException extends OtpException {
        public OtpNotFoundException(String message) {
            super(message);
        }
    }

    public static class OtpExpiredException extends OtpException {
        public OtpExpiredException(String message) {
            super(message);
        }
    }

    public static class OtpMaxAttemptsException extends OtpException {
        public OtpMaxAttemptsException(String message) {
            super(message);
        }
    }

    public static class OtpInvalidException extends OtpException {
        public OtpInvalidException(String message) {
            super(message);
        }
    }
}
