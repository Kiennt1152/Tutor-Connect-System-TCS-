package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.module.finance.service.SettlementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final SettlementService settlementService;
    private final EscrowService escrowService;

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

    @GetMapping("/settlements/preview/{classId}")
    public ReleaseInstruction previewSettlement(@PathVariable Long classId) {
        return settlementService.calculate(classId);
    }

    @PostMapping("/settlements/{classId}/apply")
    public String applySettlement(@PathVariable Long classId) {
        ReleaseInstruction instruction = settlementService.calculate(classId);
        escrowService.apply(instruction);
        return "Da settle classId=" + classId
                + " | release=" + instruction.releaseToBeneficiary()
                + " | refund=" + instruction.refundToPayer();
    }
}
