package com.tcs.module.identity.service;

import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
import com.tcs.module.identity.dto.response.RegisterResponse;
import com.tcs.module.identity.dto.response.SendOtpResponse;
import com.tcs.module.identity.dto.response.VerifyOtpResponse;

public interface AuthService {

    /**
     * Gui ma OTP toi email. Khong tao tai khoan (BR-01, BR-07).
     *
     * @param clientIp dia chi IP nguon de gioi han toc do
     */
    SendOtpResponse sendOtp(SendOtpRequest request, String clientIp);

    /**
     * Xac thuc ma OTP; neu hop le cap token chung nhan email da xac thuc (BR-04, BR-05).
     */
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    /**
     * Hoan tat dang ky: kiem tra token, tao tai khoan ACTIVE va ho so co ban (BR-02..BR-09).
     */
    RegisterResponse register(RegisterRequest request);
}
