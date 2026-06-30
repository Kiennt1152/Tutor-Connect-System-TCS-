package com.tcs.module.identity.controller;

import com.tcs.module.identity.dto.request.ChangePasswordRequest;
import com.tcs.module.identity.dto.request.ForgotPasswordRequest;
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
import com.tcs.module.identity.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @PostMapping("/send-otp")
    public SendOtpResponse sendOtp(
            @Valid @RequestBody SendOtpRequest request, HttpServletRequest servletRequest) {
        return identityService.sendOtp(request, resolveClientIp(servletRequest));
    }

    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return identityService.verifyOtp(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return identityService.register(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return identityService.login(request);
    }

    @GetMapping("/me")
    public MeResponse getMe() {
        return identityService.getMe();
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        identityService.changePassword(request);
        return Map.of("message", "Đổi mật khẩu thành công");
    }

    @PostMapping("/password/forgot")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        identityService.forgotPassword(request);
        return Map.of("message", "Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi");
    }

    @PostMapping("/password/reset")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        identityService.resetPassword(request);
        return Map.of("message", "Đặt lại mật khẩu thành công");
    }
}
