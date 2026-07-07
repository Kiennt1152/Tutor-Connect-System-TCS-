package com.tcs.module.finance.controller;

import com.tcs.exception.UnauthorizedException;
import com.tcs.module.finance.dto.request.AddPaymentMethodRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.DepositResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TransactionResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.service.FinanceService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @Value("${app.sepay.api-key:}")
    private String sepayApiKey;

    // ----- Vi + nap tien -----

    @GetMapping("/wallet")
    public WalletResponse getMyWallet() {
        return financeService.getMyWallet();
    }

    @PostMapping("/wallet/deposit")
    public DepositResponse deposit(@RequestBody DepositRequest request) {
        return financeService.createDeposit(request);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> getMyTransactions() {
        return financeService.getMyTransactions();
    }

    // ----- Tai khoan ngan hang da luu -----

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> getPaymentMethods() {
        return financeService.getPaymentMethods();
    }

    @PostMapping("/payment-methods")
    public PaymentMethodResponse addPaymentMethod(@RequestBody AddPaymentMethodRequest request) {
        return financeService.addPaymentMethod(request);
    }

    @DeleteMapping("/payment-methods/{id}")
    public Map<String, Object> deletePaymentMethod(@PathVariable Long id) {
        financeService.deletePaymentMethod(id);
        return Map.of("success", true);
    }

    // ----- Rut tien -----

    @PostMapping("/withdrawals")
    public WithdrawalResponse createWithdrawal(@RequestBody CreateWithdrawalRequest request) {
        return financeService.createWithdrawal(request);
    }

    @GetMapping("/withdrawals")
    public List<WithdrawalResponse> getMyWithdrawals() {
        return financeService.getMyWithdrawals();
    }

    // ----- Webhook SePay (public) -----

    @PostMapping("/sepay/webhook")
    public Map<String, Object> sepayWebhook(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody SepayWebhookRequest request) {
        verifySepayKey(authorization);
        financeService.handleSepayWebhook(request);
        return Map.of("success", true);
    }

    private void verifySepayKey(String authorization) {
        if (!StringUtils.hasText(sepayApiKey)) {
            return;
        }
        String expected = "Apikey " + sepayApiKey;
        if (authorization == null || !authorization.equals(expected)) {
            throw new UnauthorizedException("Webhook khong hop le");
        }
    }
}
