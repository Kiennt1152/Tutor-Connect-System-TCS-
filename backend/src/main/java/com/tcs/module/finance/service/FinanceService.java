package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import java.util.List;

public interface FinanceService {

    WalletResponse getMyWallet();

    WalletResponse deposit(DepositRequest request);

    List<PaymentMethodResponse> getPaymentMethods();

    WalletTransactionsResponse getMyTransactions(
            int page,
            int size,
            String type,
            java.time.LocalDate from,
            java.time.LocalDate to);
}
