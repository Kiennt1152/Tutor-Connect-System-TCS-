package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.TopupSession;
import com.tcs.module.finance.entity.Wallet;
import java.math.BigDecimal;

/**
 * Seam 0.3 (chu: M3). Dich vu vi dung chung cho ca escrow va nap/rut.
 * M1/M2 khong goi truc tiep - chi goi qua EscrowService.
 */
public interface WalletService {

    /** Dam bao user co vi (tao neu chua co) - 0.9. */
    Wallet getOrCreate(Long userId);

    BigDecimal balance(Long userId);

    void debit(Long userId, BigDecimal amount, String ref);

    void credit(Long userId, BigDecimal amount, String ref);

    /** Chuyen tien kha dung sang so du bi khoa de dam bao escrow. */
    Wallet lockFunds(Long userId, BigDecimal amount, String ref);

    /** Giam so du bi khoa khi escrow duoc giai ngan ra khoi vi nguoi tra. */
    Wallet releaseLockedFunds(Long userId, BigDecimal amount, String ref);

    /** Chuyen tien escrow bi khoa ve lai so du kha dung cua nguoi tra. */
    Wallet refundLockedFunds(Long userId, BigDecimal amount, String ref);

    /** Tao phien nap tien qua QR (dung PaymentGateway 0.8). */
    TopupSession createTopup(Long userId, BigDecimal amount);
}
