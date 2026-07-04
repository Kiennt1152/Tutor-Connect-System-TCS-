package com.tcs.common.event;

import java.math.BigDecimal;

/**
 * Seam 0.1 -> 0.6: phat ra khi mot hop dong da du chu ky.
 * ClassActivationService (marketplace) lang nghe de kich hoat lop
 * sau khi escrow da duoc khoa. Dat o common de tranh coupling giua cac module.
 */
public record ContractSigned(
        Long contractId,
        Long classId,
        Long payerUserId,
        Long beneficiaryUserId,
        BigDecimal amount) {
}
