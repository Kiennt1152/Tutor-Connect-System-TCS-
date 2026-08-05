package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.TopupStatusResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.module.finance.service.SettlementService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final SettlementService settlementService;

    @GetMapping("/wallet")
    public WalletResponse getMyWallet() {
        return financeService.getMyWallet();
    }

    @GetMapping("/wallet/transactions")
    public WalletTransactionsResponse getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financeService.getMyTransactions(page, size, type, from, to);
    }

    @PostMapping("/wallet/deposit")
    public WalletResponse deposit(@RequestBody DepositRequest request) {
        return financeService.deposit(request);
    }

    @PostMapping("/wallet/topups")
    public TopupSessionResponse createTopup(@RequestBody DepositRequest request) {
        return financeService.createTopup(request);
    }

    @GetMapping("/wallet/topups/{reference}")
    public TopupStatusResponse getTopupStatus(@PathVariable String reference) {
        return financeService.getTopupStatus(reference);
    }

    @PostMapping("/wallet/topups/{reference}/simulate-success")
    public TopupStatusResponse simulateTopupSuccess(@PathVariable String reference) {
        return financeService.simulateTopupSuccess(reference);
    }

    @PostMapping("/webhooks/sepay")
    public PaymentWebhookResponse handleSepayWebhook(@RequestBody SepayWebhookRequest request) {
        return financeService.handleSepayWebhook(request);
    }

    @PostMapping("/webhooks/sepay/in")
    public PaymentWebhookResponse handleSepayIncomingWebhook(@RequestBody SepayWebhookRequest request) {
        return financeService.handleSepayIncomingWebhook(request);
    }

    @PostMapping("/webhooks/sepay/out")
    public PaymentWebhookResponse handleSepayOutgoingWebhook(@RequestBody SepayWebhookRequest request) {
        return financeService.handleSepayOutgoingWebhook(request);
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> getPaymentMethods() {
        return financeService.getPaymentMethods();
    }

    @PostMapping("/payment-methods")
    public PaymentMethodResponse createPaymentMethod(@RequestBody PaymentMethodRequest request) {
        return financeService.createPaymentMethod(request);
    }

    @PatchMapping("/payment-methods/{paymentMethodId}")
    public PaymentMethodResponse updatePaymentMethod(
            @PathVariable Long paymentMethodId,
            @RequestBody PaymentMethodRequest request) {
        return financeService.updatePaymentMethod(paymentMethodId, request);
    }

    @DeleteMapping("/payment-methods/{paymentMethodId}")
    public void deletePaymentMethod(@PathVariable Long paymentMethodId) {
        financeService.deletePaymentMethod(paymentMethodId);
    }

    @PostMapping("/withdrawals")
    public WithdrawalResponse createWithdrawal(@RequestBody CreateWithdrawalRequest request) {
        return financeService.createWithdrawal(request);
    }

    @PostMapping("/withdrawals/{withdrawalId}/accept")
    public WithdrawalResponse acceptWithdrawal(@PathVariable Long withdrawalId) {
        return financeService.acceptWithdrawal(withdrawalId);
    }

    @GetMapping("/settlements/preview/{classId}")
    public ReleaseInstruction previewSettlement(@PathVariable Long classId) {
        return settlementService.calculate(classId);
    }

    @PostMapping("/settlements/{classId}/apply")
    public String applySettlement(@PathVariable Long classId) {
        ReleaseInstruction instruction = settlementService.calculate(classId);
        settlementService.execute(instruction);
        return "Đã tất toán classId=" + classId
                + " | release=" + instruction.releaseToBeneficiary()
                + " | refund=" + instruction.refundToPayer();
    }

    @PostMapping("/settlements/execute")
    public String executeSettlement(@RequestBody ReleaseInstruction instruction) {
        settlementService.execute(instruction);
        return "Đã thực thi tất toán escrowId=" + instruction.escrowId()
                + " | release=" + instruction.releaseToBeneficiary()
                + " | refund=" + instruction.refundToPayer();
    }
}
