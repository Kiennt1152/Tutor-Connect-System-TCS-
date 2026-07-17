package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final FinanceService financeService;

    @PostMapping("/webhook")
    public PaymentWebhookResponse handleSepayWebhook(@RequestBody SepayWebhookRequest request) {
        return financeService.handleSepayWebhook(request);
    }
}
