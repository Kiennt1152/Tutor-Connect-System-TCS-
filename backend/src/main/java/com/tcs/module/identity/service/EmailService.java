package com.tcs.module.identity.service;

public interface EmailService {

    /**
     * Gui email chua ma OTP xac thuc dang ky tai khoan (UC-01).
     *
     * @param toEmail       dia chi nguoi nhan
     * @param code          ma OTP
     * @param expireMinutes thoi gian hieu luc (phut)
     */
    void sendRegistrationOtp(String toEmail, String code, long expireMinutes);
}
