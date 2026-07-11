package com.tcs.module.finance.dto;

import java.math.BigDecimal;

/**
 * Seam 0.8: phien nap tien qua cong thanh toan (QR).
 * status: PENDING / CONFIRMED / EXPIRED.
 */
public record TopupSession(
        String reference,
        BigDecimal amount,
        String qrContent,
        String status) {
}
