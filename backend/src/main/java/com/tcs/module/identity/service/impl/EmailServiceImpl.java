package com.tcs.module.identity.service.impl;

import com.tcs.module.identity.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    public void sendRegistrationOtp(String toEmail, String code, long expireMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đăng ký tài khoản Tutor Connect");
            helper.setText(buildHtml(code, expireMinutes), true);

            mailSender.send(message);
            log.info("Da gui OTP dang ky toi email {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            log.error("Khong gui duoc email OTP toi {}: {}", toEmail, ex.getMessage());
            throw new IllegalArgumentException("Không gửi được email xác thực. Vui lòng thử lại sau.");
        }
    }

    private String buildHtml(String code, long expireMinutes) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;\
                border:1px solid #e2e8f0;border-radius:16px;overflow:hidden">
                  <div style="background:#2563eb;padding:24px;text-align:center">
                    <h1 style="color:#fff;margin:0;font-size:20px">Tutor Connect System</h1>
                  </div>
                  <div style="padding:28px 24px;color:#0f172a">
                    <p style="margin:0 0 16px">Vui lòng sử dụng mã OTP bên dưới để hoàn tất xác thực email \
                và đăng ký tài khoản:</p>
                    <div style="text-align:center;margin:24px 0">
                      <span style="display:inline-block;font-size:32px;font-weight:700;letter-spacing:8px;\
                color:#2563eb;background:#eff6ff;padding:14px 24px;border-radius:12px">%s</span>
                    </div>
                    <p style="margin:0 0 8px;color:#64748b">Mã có hiệu lực trong <strong>%d phút</strong>.</p>
                    <p style="margin:0;color:#64748b">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>
                  </div>
                  <div style="background:#f1f5f9;padding:16px 24px;text-align:center;color:#94a3b8;font-size:12px">
                    © Tutor Connect System
                  </div>
                </div>
                """.formatted(code, expireMinutes);
    }
}
