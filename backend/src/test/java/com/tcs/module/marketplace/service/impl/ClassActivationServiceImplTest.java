package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.event.ContractSigned;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassActivationServiceImplTest {

    @Mock
    private EscrowService escrowService;

    @Mock
    private TutoringClassRepository tutoringClassRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @InjectMocks
    private ClassActivationServiceImpl classActivationService;

    @Captor
    private ArgumentCaptor<EscrowLockCommand> commandCaptor;

    @Test
    void onContractSignedLocksEscrowThenActivatesClass() {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(tutoringClass));

        classActivationService.onContractSigned(new ContractSigned(
                2L,
                3L,
                11L,
                21L,
                new BigDecimal("500000.00"),
                7L,
                null));

        verify(escrowService).lock(commandCaptor.capture());
        EscrowLockCommand command = commandCaptor.getValue();
        assertEquals(11L, command.payerUserId());
        assertEquals(new BigDecimal("500000.00"), command.amount());
        assertEquals(7L, command.assignmentId());

        assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        verify(tutoringClassRepository).save(tutoringClass);
    }
}
