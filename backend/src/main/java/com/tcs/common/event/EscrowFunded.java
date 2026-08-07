package com.tcs.common.event;

import java.math.BigDecimal;

/**
 * Phat ra khi SePay xac nhan hoc phi da vao escrow.
 */
public record EscrowFunded(
        Long escrowId,
        Long classId,
        Long payerUserId,
        Long beneficiaryUserId,
        BigDecimal amount,
        Long assignmentId,
        Long classStudentId) {
}
