package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test cho {@link EmailServiceImpl} cua module Identity.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet emailContractOtpIdentity.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplTest {

    private static final String TO = "phuhuynh@example.com";
    private static final String OTP = "123456";
    private static final String CONTRACT_NO = "HD-2026-001";

    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@tcs.vn");
        ReflectionTestUtils.setField(emailService, "fromName", "Tutor Connect System");
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
    }

    /** MimeMessage that de MimeMessageHelper co the thao tac. */
    private MimeMessage realMimeMessage() {
        return new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
    }

    @Nested
    @DisplayName("emailContractOtpIdentity")
    class EmailContractOtpIdentity {

        @Test
        @DisplayName("UTCID01 (N) - mail bat, SMTP binh thuong -> gui email tieu de 'Mã OTP ký hợp đồng - <contractNo>'")
        void utcid01_sendSuccessfully() throws Exception {
            MimeMessage message = realMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(message);

            emailService.sendContractOtp(TO, OTP, CONTRACT_NO, 5);

            verify(mailSender).send(message);
            assertEquals("Mã OTP ký hợp đồng - " + CONTRACT_NO, message.getSubject());
        }

        @Test
        @DisplayName("UTCID02 (B) - app.mail.enabled = false -> chi ghi log, khong goi mailSender")
        void utcid02_mailDisabled() {
            ReflectionTestUtils.setField(emailService, "mailEnabled", false);

            assertDoesNotThrow(() -> emailService.sendContractOtp(TO, OTP, CONTRACT_NO, 5));

            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - App Password Gmail bi thu hoi -> 'Không gửi được mã OTP do App Password Gmail đã bị thu hồi hoặc không còn hợp lệ.'")
        void utcid03_mailAuthenticationFailure() {
            when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailAuthenticationException("535 auth failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> emailService.sendContractOtp(TO, OTP, CONTRACT_NO, 5));
            assertEquals("Không gửi được mã OTP do App Password Gmail đã bị thu hồi hoặc không còn hợp lệ.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - loi SMTP khac -> 'Không gửi được email OTP. Vui lòng thử lại sau.'")
        void utcid04_otherSmtpFailure() {
            when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailSendException("connection timed out"))
                    .when(mailSender).send(any(MimeMessage.class));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> emailService.sendContractOtp(TO, OTP, CONTRACT_NO, 5));
            assertEquals("Không gửi được email OTP. Vui lòng thử lại sau.", ex.getMessage());
        }
    }
}
