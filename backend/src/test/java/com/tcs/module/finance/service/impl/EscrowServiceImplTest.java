package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
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
class EscrowServiceImplTest {

    private static final Long PAYER_ID = 11L;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @InjectMocks
    private EscrowServiceImpl escrowService;

    @Captor
    private ArgumentCaptor<PaymentTransaction> paymentCaptor;

    @Captor
    private ArgumentCaptor<EscrowTransaction> escrowCaptor;

    @Test
    void lockPrivateAssignmentCreatesFundedEscrow() {
        BigDecimal amount = new BigDecimal("500000.00");
        Wallet wallet = payerWallet();
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(walletService.lockFunds(PAYER_ID, amount, "ESCROW_LOCK-A7")).thenReturn(wallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowTransaction result = escrowService.lock(new EscrowLockCommand(PAYER_ID, amount, 7L, null));

        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction payment = paymentCaptor.getValue();
        assertSame(wallet, payment.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_DEPOSIT, payment.getType());
        assertEquals(PaymentTransactionStatus.SUCCESS, payment.getStatus());
        assertEquals("ESCROW_LOCK-A7", payment.getReferenceCode());

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction escrow = escrowCaptor.getValue();
        assertSame(assignment, escrow.getAssignment());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        assertEquals(amount, escrow.getAmount());
        assertSame(escrow, result);
    }

    @Test
    void lockCenterEnrollmentCreatesFundedEscrow() {
        BigDecimal amount = new BigDecimal("300000.00");
        Wallet wallet = payerWallet();
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);

        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(9L)).thenReturn(Optional.empty());
        when(classStudentRepository.findById(9L)).thenReturn(Optional.of(classStudent));
        when(walletService.lockFunds(PAYER_ID, amount, "ESCROW_LOCK-CS9")).thenReturn(wallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowTransaction result = escrowService.lock(new EscrowLockCommand(PAYER_ID, amount, null, 9L));

        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        assertEquals("ESCROW_LOCK-CS9", paymentCaptor.getValue().getReferenceCode());

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction escrow = escrowCaptor.getValue();
        assertSame(classStudent, escrow.getClassStudent());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        assertSame(escrow, result);
    }

    @Test
    void lockReturnsExistingEscrowWithoutChargingAgain() {
        EscrowTransaction existing = new EscrowTransaction();
        existing.setEscrowId(100L);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.of(existing));

        EscrowTransaction result = escrowService.lock(
                new EscrowLockCommand(PAYER_ID, new BigDecimal("500000.00"), 7L, null));

        assertSame(existing, result);
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    void lockRequiresExactlyOneTarget() {
        assertThrows(BusinessException.class, () ->
                escrowService.lock(new EscrowLockCommand(PAYER_ID, new BigDecimal("1.00"), null, null)));
        assertThrows(BusinessException.class, () ->
                escrowService.lock(new EscrowLockCommand(PAYER_ID, new BigDecimal("1.00"), 1L, 2L)));
    }

    private Wallet payerWallet() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(PAYER_ID);
        wallet.setAvailableBalance(new BigDecimal("1000000.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        return wallet;
    }
}
