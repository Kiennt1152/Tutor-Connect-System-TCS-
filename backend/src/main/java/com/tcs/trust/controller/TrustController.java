package com.tcs.trust.controller;

import com.tcs.trust.service.TrustService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trust")
@RequiredArgsConstructor
public class TrustController {

    private final TrustService trustService;
}
