package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.request.AddPaymentMethodRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.ReviewWithdrawalRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.AdminWithdrawalResponse;
import com.tcs.module.finance.dto.response.DepositResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TransactionResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.util.List;

public interface FinanceService {

    /** Lay vi cua nguoi dung hien tai; tu tao vi neu chua co. */
    WalletResponse getMyWallet();

    /** Tao don nap tien: sinh giao dich PENDING + tra ve ma QR VietQR de quet. */
    DepositResponse createDeposit(DepositRequest request);

    /** Lich su giao dich cua vi hien tai (moi nhat truoc). */
    List<TransactionResponse> getMyTransactions();

    /** Xu ly webhook SePay khi co tien vao tai khoan ngan hang. */
    void handleSepayWebhook(SepayWebhookRequest request);

    // ----- Tai khoan ngan hang da luu (payment_methods) -----

    List<PaymentMethodResponse> getPaymentMethods();

    PaymentMethodResponse addPaymentMethod(AddPaymentMethodRequest request);

    void deletePaymentMethod(Long paymentMethodId);

    // ----- Rut tien -----

    /**
     * Tao yeu cau rut tien.
     * CLIENT: tao don PENDING, chuyen tien available->frozen, cho admin duyet.
     * TUTOR / TUTOR_CENTER: rut truc tiep, hoan tat ngay.
     */
    WithdrawalResponse createWithdrawal(CreateWithdrawalRequest request);

    /** Danh sach yeu cau rut cua vi hien tai. */
    List<WithdrawalResponse> getMyWithdrawals();

    // ----- Admin duyet rut -----

    List<AdminWithdrawalResponse> listAllWithdrawals(WithdrawalRequestStatus status);

    AdminWithdrawalResponse reviewWithdrawal(Long withdrawalId, ReviewWithdrawalRequest request);
}
