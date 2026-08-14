package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.PaymentTransaction;
import java.math.BigDecimal;
import java.util.Optional;

public interface CenterRequestFeeService {

    CenterRequestFeePaymentResponse createPayment(
            String requestId,
            Long clientUserId,
            Long centerUserId,
            String centerName,
            BigDecimal projectedEscrowAmount,
            RefundPayoutInfo payoutInfo);

    Optional<CenterRequestFeePaymentResponse> getPayment(String requestId);

    boolean isCenterRequestFeePayment(PaymentTransaction tx);

    CenterRequestFeePaymentResponse completeIncomingPayment(
            PaymentTransaction tx, String externalTransactionId);

    void linkFulfilledAssignment(String requestId, Long classId, Long assignmentId);

    void releaseForRequest(String requestId, String reason);

    void releaseForFulfilledAssignment(Long assignmentId, String reason);

    void requestRefund(String requestId, String reason);

    void cancelUnpaid(String requestId);
}
