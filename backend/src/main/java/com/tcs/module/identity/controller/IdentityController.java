package com.tcs.module.identity.controller;

import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
import com.tcs.module.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return identityService.login(request);
    }
}
