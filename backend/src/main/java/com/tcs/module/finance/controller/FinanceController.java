package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.service.FinanceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/wallet")
    public WalletResponse getMyWallet() {
        return financeService.getMyWallet();
    }

    @PostMapping("/wallet/deposit")
    public WalletResponse deposit(@RequestBody DepositRequest request) {
        return financeService.deposit(request);
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> getPaymentMethods() {
        return financeService.getPaymentMethods();
    }
}
