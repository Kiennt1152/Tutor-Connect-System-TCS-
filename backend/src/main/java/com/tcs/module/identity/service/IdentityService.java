package com.tcs.module.identity.service;

import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.response.AuthResponse;

public interface IdentityService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
