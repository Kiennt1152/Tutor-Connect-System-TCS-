package com.tcs.module.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit test module Notification - gui email hop dong.
 * Bam bo test case trong Report_5.1_UnitTest: cac sheet sendEmail, emailContractOtpNotify.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractEmailServiceImplTest {

    private static final String TO = "phuhuynh@example.com";
    private static final String SUBJECT = "Thông báo hợp đồng";
    private static final String HTML = "<p>Xin chào</p>";
    private static final String OTP = "123456";
    private static final String CONTRACT_NO = "HD-2026-001";

    @Mock private JavaMailSender mailSender;

    private ContractEmailServiceImpl enabledService;
    private ContractEmailServiceImpl disabledService;

    @BeforeEach
    void setUp() {
        enabledService = new ContractEmailServiceImpl(mailSender, "no-reply@tcs.vn", true);
        disabledService = new ContractEmailServiceImpl(mailSender, "no-reply@tcs.vn", false);
    }

    /** MimeMessage that de MimeMessageHelper co the thao tac. */
    private MimeMessage realMimeMessage() {
        return new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
    }

    // ========================================================================
    //  Sheet: sendEmail
    // ========================================================================

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("UTCID01 (N) - mail.enabled = true, gui thanh cong -> email duoc gui di")
        void utcid01_sendSuccessfully() {
            MimeMessage message = realMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(message);

            enabledService.sendEmail(TO, SUBJECT, HTML);

            verify(mailSender).send(message);
        }

        @Test
        @DisplayName("UTCID02 (N) - mail.enabled = false -> bo qua viec gui, chi ghi log [EMAIL DISABLED]")
        void utcid02_mailDisabled() {
            disabledService.sendEmail(TO, SUBJECT, HTML);

            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - MessagingException khi dung dia chi sai -> bat loi, ghi log, khong nem ngoai le")
        void utcid03_messagingExceptionIsSwallowed() {
            // Dia chi khong parse duoc -> MimeMessageHelper.setTo nem AddressException
            // (mot MessagingException). Phuong thuc phai nuot loi va khong nem ra ngoai.
            MimeMessage message = realMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(message);

            assertDoesNotThrow(() -> enabledService.sendEmail("khong hop le @@", SUBJECT, HTML));
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    // ========================================================================
    //  Sheet: emailContractOtpNotify
    // ========================================================================

    @Nested
    @DisplayName("emailContractOtpNotify")
    class SendContractOtp {

        @Test
        @DisplayName("UTCID01 (N) - SMTP binh thuong -> gui email OTP ky hop dong voi tieu de chua so hop dong")
        void utcid01_sendSuccessfully() throws Exception {
            MimeMessage message = realMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(message);

            enabledService.sendContractOtp(TO, OTP, CONTRACT_NO);

            ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
            verify(mailSender).send(sent.capture());
            assertTrue(sent.getValue().getSubject().contains(CONTRACT_NO));
        }

        @Test
        @DisplayName("UTCID02 (A) - App Password Gmail bi thu hoi -> MailAuthenticationException thoat ra ngoai")
        void utcid02_appPasswordRevoked() {
            // sendEmail chi bat MessagingException; MailAuthenticationException la RuntimeException
            // cua Spring nen thoat ra nguyen trang, khong bi boc lai thanh thong diep tieng Viet.
            MimeMessage message = realMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(message);
            doThrow(new MailAuthenticationException("535 auth failed"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThrows(MailAuthenticationException.class,
                    () -> enabledService.sendContractOtp(TO, OTP, CONTRACT_NO));
        }

        @Test
        @DisplayName("UTCID03 (A) - loi MessagingException khac -> bat loi, ghi log, khong nem ngoai le")
        void utcid03_messagingExceptionIsSwallowed() {
            MimeMessage message = realMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(message);

            assertDoesNotThrow(
                    () -> enabledService.sendContractOtp("khong hop le @@", OTP, CONTRACT_NO));
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }
}
