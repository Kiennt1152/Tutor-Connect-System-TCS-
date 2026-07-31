package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.ReleaseInstruction;

/**
 * Seam 0.5 (chu: M4). M4 tinh so tien phai tra tu buoi hoc/diem danh
 * roi goi execute() de M3 thuc thi chuyen tien escrow.
 */
public interface SettlementService {

    ReleaseInstruction calculate(Long classId);

    void execute(ReleaseInstruction instruction);
}
