package com.tcs.module.contract.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText("Mã OTP của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    public void sendContractOtp(String toEmail, String otpCode, String contractNo, int expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[TutorConnect] Xác nhận ký hợp đồng " + contractNo);
        message.setText(buildContractOtpText(otpCode, contractNo, expireMinutes));
        try {
            mailSender.send(message);
            log.info("Đã gửi OTP ký hợp đồng {} tới {}", contractNo, toEmail);
        } catch (Exception ex) {
            log.error("Không gửi được OTP ký hợp đồng {} tới {}: {}", contractNo, toEmail, ex.getMessage());
            throw new IllegalArgumentException("Không gửi được email OTP. Vui lòng thử lại sau.");
        }
    }

    private String buildContractOtpText(String otpCode, String contractNo, int expireMinutes) {
        return """
                Xin chào,

                Bạn nhận được email này vì có một yêu cầu ký hợp đồng trên TutorConnect.

                Mã xác nhận: %s
                Số hợp đồng: %s
                Hiệu lực: %d phút

                Vui lòng sử dụng mã này để xác nhận ký hợp đồng.

                Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.

                Trung tâm TutorConnect
                """.formatted(otpCode, contractNo, expireMinutes);
    }
}
