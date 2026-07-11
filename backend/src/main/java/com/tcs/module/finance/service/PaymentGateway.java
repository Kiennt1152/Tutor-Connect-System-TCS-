package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.TopupSession;
import java.math.BigDecimal;

/**
 * Seam 0.8 (chu: M3). Cong thanh toan ngoai (QR/PSP).
 * Pha 1 dung ban mock tu confirm (xem PhaseOneStubConfig).
 */
public interface PaymentGateway {

    TopupSession createQr(BigDecimal amount, String reference);

    boolean isConfirmed(String reference);
}
