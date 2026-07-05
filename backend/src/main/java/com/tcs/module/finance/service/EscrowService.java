package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;

/**
 * Seam 0.2 (chu: M3). Diem vao tai chinh cho M1/M2 va M4.
 * - lock  : khoa tien khi kich hoat lop.
 * - apply : thuc thi tat toan theo ReleaseInstruction (0.5) tu M4.
 */
public interface EscrowService {

    EscrowTransaction lock(EscrowLockCommand command);

    void apply(ReleaseInstruction instruction);
}
