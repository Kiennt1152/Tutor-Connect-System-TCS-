package com.tcs.module.identity.service;

public interface EmailService {

    /**
     * Gui email chua ma OTP xac thuc dang ky tai khoan.
     *
     * @param toEmail         dia chi nguoi nhan
     * @param fullName        ho ten nguoi nhan (de chao)
     * @param code            ma OTP
     * @param expireMinutes   thoi gian hieu luc cua ma (phut)
     */
    void sendRegistrationOtp(String toEmail, String fullName, String code, long expireMinutes);
}
