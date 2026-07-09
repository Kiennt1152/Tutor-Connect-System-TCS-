package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.ReleaseInstruction;

/**
 * Seam 0.5 (chu: M4). Tinh so tien phai tra tu buoi hoc/diem danh
 * roi tra ReleaseInstruction cho EscrowService.apply() thuc thi.
 */
public interface SettlementService {

    ReleaseInstruction calculate(Long classId);
}
