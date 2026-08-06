package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.event.ContractSigned;
import com.tcs.common.event.EscrowFunded;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassActivationServiceImplTest {

    @Mock
    private TutoringClassRepository tutoringClassRepository;

    @InjectMocks
    private ClassActivationServiceImpl classActivationService;

    @Test
    void onContractSignedWaitsForEscrowFundedWhenTuitionIsRequired() {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);

        classActivationService.onContractSigned(new ContractSigned(
                2L,
                3L,
                11L,
                21L,
                new BigDecimal("500000.00"),
                7L,
                null));

        assertEquals(TutoringClassStatus.MATCHED, tutoringClass.getStatus());
        verify(tutoringClassRepository, never()).save(any());
    }

    @Test
    void onEscrowFundedActivatesClass() {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(tutoringClass));

        classActivationService.onEscrowFunded(new EscrowFunded(
                5L,
                3L,
                11L,
                21L,
                new BigDecimal("500000.00"),
                7L,
                null));

        assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        verify(tutoringClassRepository).save(tutoringClass);
    }
}
