package com.tcs.module.notification.service;

public interface EmailService {

    void sendEmail(String to, String subject, String htmlContent);

    void sendContractOtp(String to, String otpCode, String contractNo);
}
