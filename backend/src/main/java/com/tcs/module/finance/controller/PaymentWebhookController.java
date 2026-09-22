package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ====================================================================================================
 * [UC-41] XÁC THỰC WEBHOOK THANH TOÁN TỰ ĐỘNG (PAYMENT WEBHOOK CONTROLLER)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Tiếp nhận tín hiệu Webhook từ cổng thanh toán đối tác (SePay) khi có biến động số dư.
 * 2. Xác thực tính hợp lệ của chữ ký điện tử, mã giao dịch và số tiền chuyển khoản.
 * 3. Tự động cập nhật số dư ví hoặc kích hoạt trạng thái ký quỹ hợp đồng ngay tức thì.
 * * @author Nguyễn Tiến Anh (tienanh6677)
 */
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
