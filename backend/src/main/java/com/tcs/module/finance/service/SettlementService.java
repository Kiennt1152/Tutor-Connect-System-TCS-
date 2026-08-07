package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.ExecuteRefundRequest;
import com.tcs.module.finance.dto.response.RefundExecutionResponse;

/**
 * Seam 0.5 (chu: M4). M4 tinh so tien phai tra tu buoi hoc/diem danh
 * roi goi execute() de M3 thuc thi chuyen tien escrow.
 */
public interface SettlementService {

    ReleaseInstruction calculate(Long classId);

    void execute(ReleaseInstruction instruction);

    RefundExecutionResponse executeRefund(ExecuteRefundRequest request);
}
