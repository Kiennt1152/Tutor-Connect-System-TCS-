package com.tcs.module.messaging.service.impl;

import com.tcs.module.messaging.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * ====================================================================================================
 * [UC-52] DỊCH VỤ GỬI EMAIL THÔNG BÁO HỆ THỐNG (MESSAGING EMAIL SERVICE IMPLEMENTATION)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Gửi các thông báo tự động: Nhắc lịch học, thông báo hợp đồng mới, kết quả xử lý khiếu nại.
 * 2. Điền dữ liệu biến động vào các khuôn mẫu email HTML có sẵn.
 * * @author Nguyễn Trung Kiên (Kiennt1152)
 */
@Service("messagingEmailService")
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@tutorconnect.local}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPlainText(String toEmail, String subject, String body) {
        if (!mailEnabled || mailSender == null) {
            log.info("[EMAIL-DEV] To: {} | Subject: {} | Body: {}", toEmail, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Sent email to {} with subject {}", toEmail, subject);
    }
}
