package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.response.WalletResponse;
import java.util.List;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;

public interface FinanceService {

    WalletResponse getMyWallet();

    WalletResponse deposit(DepositRequest request);

    List<PaymentMethodResponse> getPaymentMethods();
}
