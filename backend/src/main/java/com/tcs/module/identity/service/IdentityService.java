package com.tcs.module.identity.service;

import com.tcs.module.identity.dto.request.ChangePasswordRequest;
import com.tcs.module.identity.dto.request.ForgotPasswordRequest;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
import com.tcs.module.identity.dto.response.MeResponse;

public interface IdentityService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MeResponse getMe();

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
