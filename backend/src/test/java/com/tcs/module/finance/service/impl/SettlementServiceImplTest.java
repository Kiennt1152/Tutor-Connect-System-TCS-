package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.service.EscrowService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock
    private EscrowService escrowService;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Test
    void executeDelegatesReleaseInstructionToEscrowService() {
        ReleaseInstruction instruction = new ReleaseInstruction(
                10L,
                new BigDecimal("400000.00"),
                new BigDecimal("100000.00"),
                "Hoàn thành một phần");

        settlementService.execute(instruction);

        verify(escrowService).apply(instruction);
    }

    @Test
    void executeRejectsMissingInstruction() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                settlementService.execute(null));

        assertEquals("Thiếu chỉ dẫn tất toán", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    @Test
    void calculateIsOwnedByM4() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                settlementService.calculate(1L));

        assertEquals("Chức năng tính toán tất toán thuộc module M4", exception.getMessage());
    }
}
