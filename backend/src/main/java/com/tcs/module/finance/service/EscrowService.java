package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import java.math.BigDecimal;

/**
 * Seam 0.2 (chu: M3). Diem vao tai chinh cho M1/M2 va M4.
 * - lock  : khoa tien khi kich hoat lop.
 * - apply : thuc thi tat toan theo ReleaseInstruction (0.5) tu M4.
 * - refund: hoan toan bo escrow ve nguoi tra khi lop/hop dong bi huy.
 */
public interface EscrowService {

    EscrowTransaction lock(EscrowLockCommand command);

    PaymentTransaction preparePrivateContractPayment(Long payerUserId, BigDecimal amount, Long assignmentId);

    EscrowTransaction fundPendingPayment(PaymentTransaction payment, String externalTransactionId);

    void apply(ReleaseInstruction instruction);

    EscrowTransaction refund(Long escrowId, String reason);
}
