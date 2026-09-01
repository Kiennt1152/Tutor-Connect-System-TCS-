package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.event.ContractSigned;
import com.tcs.common.event.EscrowFunded;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

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

    @Test
    void onEscrowFundedDoesNotActivateCenterClassUntilEnoughStudentsPaid() {
        TutoringClass tutoringClass = centerClass();
        when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(tutoringClass));

        classActivationService.onEscrowFunded(new EscrowFunded(
                5L,
                3L,
                11L,
                21L,
                new BigDecimal("100000.00"),
                null,
                15L));

        assertEquals(TutoringClassStatus.MATCHED, tutoringClass.getStatus());
        verify(tutoringClassRepository, never()).save(any());
    }

    @Test
    void onEscrowFundedActivatesCenterClassWhenEnoughStudentsPaid() {
        TutoringClass tutoringClass = centerClass();
        when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(tutoringClass));

        classActivationService.onEscrowFunded(new EscrowFunded(
                6L,
                3L,
                12L,
                21L,
                new BigDecimal("100000.00"),
                null,
                16L));

        assertEquals(TutoringClassStatus.MATCHED, tutoringClass.getStatus());
        verify(tutoringClassRepository, never()).save(any());
    }

    private TutoringClass centerClass() {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setMinStudents(2);
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        return tutoringClass;
    }

    private EscrowTransaction escrow(EscrowStatus status) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setStatus(status);
        return escrow;
    }

    // ===================================================================
    //  Sheet: clsActivate
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("clsActivate")
    class ClsActivate {

        private TutoringClass privateClass(TutoringClassStatus status) {
            TutoringClass c = new TutoringClass();
            c.setClassId(3L);
            c.setStatus(status);
            return c;
        }

        private TutoringClass centerClass(int minStudents) {
            TutoringClass c = new TutoringClass();
            c.setClassId(3L);
            c.setClassType(ClassType.CENTER);
            c.setMinStudents(minStudents);
            c.setStatus(TutoringClassStatus.MATCHED);
            return c;
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - lop private chua kich hoat -> chuyen sang IN_PROGRESS")
        void utcid01_activatePrivateClass() {
            TutoringClass c = privateClass(TutoringClassStatus.MATCHED);
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(c));

            classActivationService.activate(3L);

            assertEquals(TutoringClassStatus.IN_PROGRESS, c.getStatus());
            verify(tutoringClassRepository).save(c);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (N) - lop CENTER du escrow FUNDED (>= minStudents) -> IN_PROGRESS")
        void utcid02_activateCenterClassWithEnoughEscrows() {
            TutoringClass c = centerClass(2);
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(c));
            when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(3L))
                    .thenReturn(List.of(escrow(EscrowStatus.FUNDED), escrow(EscrowStatus.FUNDED)));

            classActivationService.activate(3L);

            assertEquals(TutoringClassStatus.IN_PROGRESS, c.getStatus());
            verify(tutoringClassRepository).save(c);
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - classId = null -> bo qua, khong truy van")
        void utcid03_nullClassId() {
            classActivationService.activate(null);

            verify(tutoringClassRepository, never()).findById(any());
            verify(tutoringClassRepository, never()).save(any(TutoringClass.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - khong tim thay lop -> bo qua")
        void utcid04_classNotFound() {
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.empty());

            classActivationService.activate(3L);

            verify(tutoringClassRepository, never()).save(any(TutoringClass.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (B) - lop da IN_PROGRESS -> bo qua, khong doi trang thai")
        void utcid05_alreadyInProgress() {
            TutoringClass c = privateClass(TutoringClassStatus.IN_PROGRESS);
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(c));

            classActivationService.activate(3L);

            verify(tutoringClassRepository, never()).save(any(TutoringClass.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (B) - lop da COMPLETED -> bo qua")
        void utcid06_alreadyCompleted() {
            TutoringClass c = privateClass(TutoringClassStatus.COMPLETED);
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(c));

            classActivationService.activate(3L);

            assertEquals(TutoringClassStatus.COMPLETED, c.getStatus());
            verify(tutoringClassRepository, never()).save(any(TutoringClass.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (B) - lop da CANCELLED -> bo qua")
        void utcid07_alreadyCancelled() {
            TutoringClass c = privateClass(TutoringClassStatus.CANCELLED);
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(c));

            classActivationService.activate(3L);

            assertEquals(TutoringClassStatus.CANCELLED, c.getStatus());
            verify(tutoringClassRepository, never()).save(any(TutoringClass.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID08 (A) - lop CENTER chua du escrow FUNDED -> bo qua")
        void utcid08_centerClassNotEnoughEscrows() {
            TutoringClass c = centerClass(2);
            when(tutoringClassRepository.findById(3L)).thenReturn(Optional.of(c));
            when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(3L))
                    .thenReturn(List.of(escrow(EscrowStatus.FUNDED)));

            classActivationService.activate(3L);

            assertEquals(TutoringClassStatus.MATCHED, c.getStatus());
            verify(tutoringClassRepository, never()).save(any(TutoringClass.class));
        }
    }
}
