package com.tcs.module.finance.dto;

import java.math.BigDecimal;

/**
 * Seam 0.2: lenh khoa escrow. Dung DUNG MOT trong hai target:
 *   - assignmentId  : lop PRIVATE (giai ngan ve tutor)
 *   - classStudentId: lop CENTER  (escrow theo tung ghi danh, giai ngan ve center)
 */
public record EscrowLockCommand(
        Long payerUserId,
        BigDecimal amount,
        Long assignmentId,
        Long classStudentId) {
}
