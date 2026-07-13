package com.tcs.module.marketplace.service;

import com.tcs.common.event.ContractSigned;

/**
 * Seam 0.6 (chu: M4, tac dong len trang thai lop cua M1/M2).
 * Khi hop dong du chu ky + escrow da khoa -> kich hoat lop
 * (lat TutoringClassStatus sang IN_PROGRESS) va thong bao cac ben.
 */
public interface ClassActivationService {

    void onContractSigned(ContractSigned event);

    void activate(Long classId);
}
