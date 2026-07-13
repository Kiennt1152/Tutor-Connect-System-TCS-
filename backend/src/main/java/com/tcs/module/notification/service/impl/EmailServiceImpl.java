package com.tcs.module.notification.service.impl;

import com.tcs.module.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailEnabled;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@tcs.local}") String fromAddress,
            @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailEnabled = mailEnabled;
    }

    @Override
    public void sendEmail(String to, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.info("[EMAIL DISABLED] To: {}, Subject: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    public void sendContractOtp(String to, String otpCode, String contractNo) {
        String subject = "Mã xác nhận ký hợp đồng " + contractNo;
        String htmlContent = buildContractOtpEmail(otpCode, contractNo);
        sendEmail(to, subject, htmlContent);
    }

    private String buildContractOtpEmail(String otpCode, String contractNo) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Mã xác nhận ký hợp đồng</title>
            </head>
            <body style="margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background:#f4f4f4;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 16px;">
                <tr>
                  <td align="center">
                    <table width="100%%" style="max-width:520px;background:#ffffff;border-radius:12px;overflow:hidden;
                                 box-shadow:0 2px 12px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background:#4f46e5;padding:32px 32px 24px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;">
                            Tutor Connect System
                          </h1>
                          <p style="margin:8px 0 0;color:#c7d2fe;font-size:14px;">
                            Hệ thống kết nối gia sư chuyên nghiệp
                          </p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:32px;">
                          <p style="margin:0 0 8px;font-size:16px;color:#374151;">
                            Xin chào,
                          </p>
                          <p style="margin:0 0 24px;font-size:15px;color:#6b7280;line-height:1.6;">
                            Bạn đã yêu cầu ký hợp đồng <strong>%s</strong>.
                            Vui lòng sử dụng mã xác nhận bên dưới để hoàn tất việc ký.
                          </p>

                          <!-- OTP Code -->
                          <div style="background:#f0f4ff;border:2px dashed #4f46e5;border-radius:12px;
                                       padding:24px;text-align:center;margin-bottom:24px;">
                            <p style="margin:0 0 8px;font-size:13px;color:#6b7280;text-transform:uppercase;
                                       letter-spacing:1px;">Mã xác nhận</p>
                            <p style="margin:0;font-size:36px;font-weight:800;color:#4f46e5;
                                       letter-spacing:8px;">%s</p>
                          </div>

                          <!-- Info -->
                          <div style="background:#fef9c3;border-radius:8px;padding:14px 16px;margin-bottom:20px;">
                            <p style="margin:0;font-size:13px;color:#92400e;line-height:1.6;">
                              <strong>Lưu ý:</strong> Mã có hiệu lực trong <strong>5 phút</strong>.
                              Không chia sẻ mã này với bất kỳ ai. Nếu bạn không yêu cầu ký hợp đồng,
                              vui lòng bỏ qua email này.
                            </p>
                          </div>

                          <p style="margin:0;font-size:13px;color:#9ca3af;">
                            Thời gian gửi: %s
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f9fafb;border-top:1px solid #e5e7eb;
                                    padding:20px 32px;text-align:center;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;">
                            Email được gửi tự động từ hệ thống Tutor Connect System.<br>
                            Vui lòng không reply email này.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(contractNo, otpCode, timestamp);
    }
}
