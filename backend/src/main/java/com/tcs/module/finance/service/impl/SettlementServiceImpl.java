package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final EscrowService escrowService;

    @Override
    @Transactional(readOnly = true)
    public ReleaseInstruction calculate(Long classId) {
        throw new UnsupportedOperationException("Chức năng tính toán tất toán thuộc module M4");
    }

    @Override
    @Transactional
    public void execute(ReleaseInstruction instruction) {
        if (instruction == null) {
            throw new BusinessException("Thiếu chỉ dẫn tất toán");
        }
        escrowService.apply(instruction);
    }
}
