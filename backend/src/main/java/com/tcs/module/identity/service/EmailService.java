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

    void sendPasswordResetOtp(String toEmail, String code, long expireMinutes);

    /**
     * Gui email chua ma OTP xac thuc ky hop dong (UC-44).
     *
     * @param toEmail       dia chi nguoi nhan
     * @param otpCode       ma OTP
     * @param contractNo    so hop dong
     * @param expireMinutes thoi gian hieu luc (phut)
     */
    void sendContractOtp(String toEmail, String otpCode, String contractNo, int expireMinutes);
}
