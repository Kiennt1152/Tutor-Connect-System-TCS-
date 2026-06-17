package com.tcs.verification.controller;

import com.tcs.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
}
