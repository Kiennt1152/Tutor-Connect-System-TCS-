package com.tcs.module.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link OtpService} — động cơ OTP dùng chung cho đăng ký, quên mật khẩu
 * và ký hợp đồng.
 *
 * <p>Bám bộ test case trong Report_5.1_UnitTest: sheet otpIssue và sheet otpVerify.</p>
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String EMAIL = "hocvien@example.com";

    @Mock private EmailOtpRepository emailOtpRepository;

    @InjectMocks private OtpService otpService;

    @Captor private ArgumentCaptor<EmailOtp> otpCaptor;

    // ------------------------------------------------------------------ helpers

    /** OTP còn hiệu lực: chưa dùng, chưa hết hạn, số lần nhập sai = {@code attempts}. */
    private EmailOtp activeOtp(String code, int attempts) {
        EmailOtp otp = new EmailOtp();
        otp.setEmail(EMAIL);
        otp.setCode(code);
        otp.setPurpose(OtpPurpose.REGISTRATION);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(attempts);
        otp.setLastSentAt(LocalDateTime.now());
        return otp;
    }

    /** Cấu hình kiểu luồng đăng ký: hiện số lần còn lại, chạm mốc thì báo hết lượt. */
    private OtpVerifyPolicy registrationPolicy() {
        return OtpVerifyPolicy.builder()
                .maxAttempts(5)
                .throwMaxOnReach(true)
                .showRemaining(true)
                .missingMessage("Mã xác thực không tồn tại. Vui lòng yêu cầu gửi lại mã.")
                .notFoundMessage("Mã xác thực không tồn tại. Vui lòng yêu cầu gửi lại mã.")
                .expiredMessage("Mã xác thực đã hết hạn. Vui lòng yêu cầu gửi lại mã.")
                .maxAttemptsMessage("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.")
                .wrongRemainingTemplate("Mã xác thực không đúng. Bạn còn %d lần thử.")
                .build();
    }

    /** Cấu hình kiểu luồng quên mật khẩu: KHÔNG hiện số lần còn lại. */
    private OtpVerifyPolicy passwordResetPolicy() {
        return OtpVerifyPolicy.builder()
                .maxAttempts(5)
                .showRemaining(false)
                .missingMessage("Mã OTP không tồn tại hoặc đã hết hạn")
                .notFoundMessage("Mã OTP không tồn tại hoặc đã hết hạn")
                .expiredMessage("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới")
                .maxAttemptsMessage("Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới")
                .wrongMessage("Mã OTP không đúng")
                .build();
    }

    /** Cấu hình kiểu luồng ký hợp đồng: sai chạm mốc tối đa thì KHOÁ mã. */
    private OtpVerifyPolicy lockingPolicy() {
        return OtpVerifyPolicy.builder()
                .maxAttempts(3)
                .lockOnMaxAttempts(true)
                .showRemaining(false)
                .missingMessage("Vui lòng nhập mã OTP")
                .notFoundMessage("Không tìm thấy mã OTP")
                .expiredMessage("Mã OTP đã hết hạn")
                .maxAttemptsMessage("Bạn đã nhập sai quá số lần cho phép")
                .wrongMessage("Mã OTP không đúng")
                .build();
    }

    private void givenActiveOtp(EmailOtp otp) {
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                EMAIL, OtpPurpose.REGISTRATION)).thenReturn(Optional.ofNullable(otp));
    }

    // ===================================================================
    //  Sheet: otpIssue
    // ===================================================================
    @Nested
    @DisplayName("otpIssue")
    class OtpIssue {

        @Test
        @DisplayName("UTCID01 (N) - chưa có mã cũ -> tạo mã mới, attempts = 0, hạn = now + ttl")
        void utcid01_createsNewOtpWhenNoPrevious() {
            givenActiveOtp(null);
            when(emailOtpRepository.save(any(EmailOtp.class))).thenAnswer(i -> i.getArgument(0));

            LocalDateTime before = LocalDateTime.now();
            EmailOtp result = otpService.issue(EMAIL, OtpPurpose.REGISTRATION, 6, Duration.ofMinutes(5));

            verify(emailOtpRepository, times(1)).save(otpCaptor.capture());
            EmailOtp saved = otpCaptor.getValue();
            assertSame(result, saved);
            assertEquals(EMAIL, saved.getEmail());
            assertEquals(OtpPurpose.REGISTRATION, saved.getPurpose());
            assertEquals(0, saved.getAttempts());
            assertNotNull(saved.getLastSentAt());
            assertNull(saved.getConsumedAt());
            assertTrue(saved.getExpiresAt().isAfter(before.plusMinutes(4)),
                    "expiresAt phải xấp xỉ now + ttl");
        }

        @Test
        @DisplayName("UTCID02 (N) - đang có mã cũ -> mã cũ bị đánh dấu đã dùng trước khi tạo mã mới")
        void utcid02_consumesPreviousOtpFirst() {
            EmailOtp previous = activeOtp("111111", 0);
            givenActiveOtp(previous);
            when(emailOtpRepository.save(any(EmailOtp.class))).thenAnswer(i -> i.getArgument(0));

            otpService.issue(EMAIL, OtpPurpose.REGISTRATION, 6, Duration.ofMinutes(5));

            // save() được gọi 2 lần: 1 cho mã cũ (vô hiệu hoá), 1 cho mã mới.
            verify(emailOtpRepository, times(2)).save(otpCaptor.capture());
            assertNotNull(previous.getConsumedAt(), "mã cũ phải bị đánh dấu đã dùng");
            EmailOtp created = otpCaptor.getAllValues().get(1);
            assertNull(created.getConsumedAt());
            assertEquals(0, created.getAttempts());
        }

        @Test
        @DisplayName("UTCID03 (B) - codeLength = 6 -> mã đúng 6 chữ số, có đệm số 0 ở đầu")
        void utcid03_codeLengthSix() {
            givenActiveOtp(null);
            when(emailOtpRepository.save(any(EmailOtp.class))).thenAnswer(i -> i.getArgument(0));

            EmailOtp result = otpService.issue(EMAIL, OtpPurpose.REGISTRATION, 6, Duration.ofMinutes(5));

            assertTrue(result.getCode().matches("\\d{6}"),
                    "mã phải gồm đúng 6 chữ số, thực tế: " + result.getCode());
        }

        @Test
        @DisplayName("UTCID04 (B) - codeLength = 4 -> mã đúng 4 chữ số")
        void utcid04_codeLengthFour() {
            givenActiveOtp(null);
            when(emailOtpRepository.save(any(EmailOtp.class))).thenAnswer(i -> i.getArgument(0));

            EmailOtp result = otpService.issue(EMAIL, OtpPurpose.REGISTRATION, 4, Duration.ofMinutes(5));

            assertTrue(result.getCode().matches("\\d{4}"),
                    "mã phải gồm đúng 4 chữ số, thực tế: " + result.getCode());
        }

        @Test
        @DisplayName("UTCID05 (A) - ttl = null -> NullPointerException (hàm không kiểm tra đầu vào)")
        void utcid05_nullTtl() {
            givenActiveOtp(null);

            assertThrows(NullPointerException.class,
                    () -> otpService.issue(EMAIL, OtpPurpose.REGISTRATION, 6, null));

            verify(emailOtpRepository, never()).save(any(EmailOtp.class));
        }
    }

    // ===================================================================
    //  Sheet: otpVerify
    // ===================================================================
    @Nested
    @DisplayName("otpVerify")
    class OtpVerify {

        @Test
        @DisplayName("UTCID01 (N) - mã đúng, còn hạn, chưa vượt lượt -> đánh dấu đã dùng và trả về")
        void utcid01_verifySuccessfully() {
            EmailOtp otp = activeOtp("123456", 0);
            givenActiveOtp(otp);

            EmailOtp result = otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "123456", registrationPolicy());

            assertSame(otp, result);
            assertNotNull(otp.getConsumedAt(), "mã phải bị đánh dấu đã dùng");
            verify(emailOtpRepository).save(otp);
        }

        @Test
        @DisplayName("UTCID02 (A) - code null/rỗng -> OtpInvalidException, chặn trước khi truy vấn")
        void utcid02_blankCode() {
            OtpVerifyPolicy policy = registrationPolicy();

            OtpService.OtpInvalidException ex = assertThrows(OtpService.OtpInvalidException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, null, policy));
            assertEquals(policy.getMissingMessage(), ex.getMessage());

            assertThrows(OtpService.OtpInvalidException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "   ", policy));

            verify(emailOtpRepository, never())
                    .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(any(), any());
        }

        @Test
        @DisplayName("UTCID03 (A) - không còn mã chưa dùng -> OtpNotFoundException")
        void utcid03_otpNotFound() {
            givenActiveOtp(null);
            OtpVerifyPolicy policy = registrationPolicy();

            OtpService.OtpNotFoundException ex = assertThrows(OtpService.OtpNotFoundException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "123456", policy));
            assertEquals(policy.getNotFoundMessage(), ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - mã đã hết hạn -> OtpExpiredException")
        void utcid04_otpExpired() {
            EmailOtp otp = activeOtp("123456", 0);
            otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            givenActiveOtp(otp);
            OtpVerifyPolicy policy = registrationPolicy();

            OtpService.OtpExpiredException ex = assertThrows(OtpService.OtpExpiredException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "123456", policy));
            assertEquals(policy.getExpiredMessage(), ex.getMessage());
            verify(emailOtpRepository, never()).save(any(EmailOtp.class));
        }

        @Test
        @DisplayName("UTCID05 (B) - attempts đã bằng maxAttempts -> chặn ngay, không so mã")
        void utcid05_attemptsAlreadyAtMax() {
            EmailOtp otp = activeOtp("123456", 5); // maxAttempts = 5
            givenActiveOtp(otp);
            OtpVerifyPolicy policy = registrationPolicy();

            OtpService.OtpMaxAttemptsException ex = assertThrows(OtpService.OtpMaxAttemptsException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "123456", policy));
            assertEquals(policy.getMaxAttemptsMessage(), ex.getMessage());
            assertEquals(5, otp.getAttempts(), "không được tăng thêm lượt khi đã chạm mốc");
            verify(emailOtpRepository, never()).save(any(EmailOtp.class));
        }

        @Test
        @DisplayName("UTCID06 (B) - attempts = max - 1, nhập sai, throwMaxOnReach -> OtpMaxAttemptsException")
        void utcid06_wrongCodeReachesMax() {
            EmailOtp otp = activeOtp("123456", 4); // max = 5 -> lần sai này chạm mốc
            givenActiveOtp(otp);
            OtpVerifyPolicy policy = registrationPolicy();

            OtpService.OtpMaxAttemptsException ex = assertThrows(OtpService.OtpMaxAttemptsException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "999999", policy));
            assertEquals(policy.getMaxAttemptsMessage(), ex.getMessage());
            assertEquals(5, otp.getAttempts());
            verify(emailOtpRepository).save(otp);
        }

        @Test
        @DisplayName("UTCID07 (A) - nhập sai, showRemaining = true -> báo kèm số lần còn lại")
        void utcid07_wrongCodeShowsRemaining() {
            EmailOtp otp = activeOtp("123456", 1); // sai lần này -> attempts = 2, còn 3 lượt
            givenActiveOtp(otp);

            OtpService.OtpInvalidException ex = assertThrows(OtpService.OtpInvalidException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "999999", registrationPolicy()));
            assertEquals("Mã xác thực không đúng. Bạn còn 3 lần thử.", ex.getMessage());
            assertEquals(2, otp.getAttempts());
            assertNull(otp.getConsumedAt(), "chưa chạm mốc thì không được khoá mã");
        }

        @Test
        @DisplayName("UTCID08 (A) - nhập sai, showRemaining = false -> dùng wrongMessage")
        void utcid08_wrongCodeWithoutRemaining() {
            EmailOtp otp = activeOtp("123456", 0);
            givenActiveOtp(otp);
            OtpVerifyPolicy policy = passwordResetPolicy();

            OtpService.OtpInvalidException ex = assertThrows(OtpService.OtpInvalidException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "999999", policy));
            assertEquals(policy.getWrongMessage(), ex.getMessage());
            assertEquals(1, otp.getAttempts());
        }

        @Test
        @DisplayName("UTCID09 (B) - lockOnMaxAttempts, nhập sai chạm mốc -> mã bị khoá vĩnh viễn")
        void utcid09_lockOnMaxAttempts() {
            EmailOtp otp = activeOtp("123456", 2); // lockingPolicy maxAttempts = 3
            givenActiveOtp(otp);

            assertThrows(OtpService.OtpInvalidException.class,
                    () -> otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "999999", lockingPolicy()));

            assertEquals(3, otp.getAttempts());
            assertNotNull(otp.getConsumedAt(), "chạm mốc với lockOnMaxAttempts thì phải khoá mã");
            verify(emailOtpRepository).save(otp);
        }

        @Test
        @DisplayName("UTCID10 (B) - mã đúng nhưng có khoảng trắng thừa -> vẫn hợp lệ (code.trim())")
        void utcid10_codeWithSurroundingSpaces() {
            EmailOtp otp = activeOtp("123456", 0);
            givenActiveOtp(otp);

            EmailOtp result = otpService.verify(EMAIL, OtpPurpose.REGISTRATION, "  123456  ", registrationPolicy());

            assertSame(otp, result);
            assertNotNull(otp.getConsumedAt());
            verify(emailOtpRepository).save(otp);
        }
    }
}
