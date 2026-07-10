package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.TopupStatusResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import java.util.List;

public interface FinanceService {

    WalletResponse getMyWallet();

    WalletResponse deposit(DepositRequest request);

    TopupSessionResponse createTopup(DepositRequest request);

    TopupStatusResponse getTopupStatus(String reference);

    TopupStatusResponse simulateTopupSuccess(String reference);

    PaymentWebhookResponse handleSepayWebhook(SepayWebhookRequest request);

    List<PaymentMethodResponse> getPaymentMethods();

    WithdrawalResponse createWithdrawal(CreateWithdrawalRequest request);

    WalletTransactionsResponse getMyTransactions(
            int page,
            int size,
            String type,
            java.time.LocalDate from,
            java.time.LocalDate to);
}
