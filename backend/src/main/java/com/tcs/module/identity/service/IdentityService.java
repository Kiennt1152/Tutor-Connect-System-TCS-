package com.tcs.module.identity.service;

import com.tcs.module.identity.dto.request.ChangePasswordRequest;
import com.tcs.module.identity.dto.request.ForgotPasswordRequest;
import com.tcs.module.identity.dto.request.GoogleLoginRequest;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.dto.request.SendOtpRequest;
import com.tcs.module.identity.dto.request.VerifyOtpRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
import com.tcs.module.identity.dto.response.MeResponse;
import com.tcs.module.identity.dto.response.RegisterResponse;
import com.tcs.module.identity.dto.response.SendOtpResponse;
import com.tcs.module.identity.dto.response.VerifyOtpResponse;

public interface IdentityService {

    /** UC-01: gui ma OTP toi email (chua tao tai khoan). */
    SendOtpResponse sendOtp(SendOtpRequest request, String clientIp);

    /** UC-01: xac thuc OTP, cap token chung nhan email da xac thuc. */
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    /** UC-01: tao tai khoan ACTIVE sau khi email da xac thuc qua OTP. */
    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Dang nhap bang Google: xac thuc ID token, tu tao tai khoan CLIENT neu email chua ton tai. */
    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    MeResponse getMe();

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
