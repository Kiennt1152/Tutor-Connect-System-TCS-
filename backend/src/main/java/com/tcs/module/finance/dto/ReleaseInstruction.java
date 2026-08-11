package com.tcs.module.finance.dto;

import java.math.BigDecimal;

/**
 * Seam 0.5 (nguoc): M4 tinh toan tat toan -> chi dan cho M3 thuc thi.
 * releaseToBeneficiary + refundToPayer thuong = tong escrow.
 */
public record ReleaseInstruction(
        Long escrowId,
        BigDecimal releaseToBeneficiary,
        BigDecimal refundToPayer,
        String reason,
        RefundPayoutInfo refundPayoutInfo) {

    public ReleaseInstruction(
            Long escrowId,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            String reason) {
        this(escrowId, releaseToBeneficiary, refundToPayer, reason, null);
    }
}
