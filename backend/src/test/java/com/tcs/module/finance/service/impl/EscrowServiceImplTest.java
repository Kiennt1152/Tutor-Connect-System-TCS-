package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EscrowServiceImplTest {

    private static final Long PAYER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;
    private static final Long CENTER_USER_ID = 33L;

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

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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

    @Test
    void applyReleasesPrivateEscrowToTutor() {
        BigDecimal amount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(5L, amount);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(5L, amount, BigDecimal.ZERO, "Hoàn thành lớp"));

        verify(walletService).releaseLockedFunds(PAYER_ID, amount, "ESCROW_RELEASE-5");
        verify(walletService).credit(TUTOR_USER_ID, amount, "ESCROW_RELEASE-5");
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction releaseTx = paymentCaptor.getValue();
        assertSame(tutorWallet, releaseTx.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, releaseTx.getType());
        assertEquals(PaymentTransactionStatus.SUCCESS, releaseTx.getStatus());
        assertEquals("ESCROW_RELEASE-5", releaseTx.getReferenceCode());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    @Test
    void applyReleasesCenterEscrowToCenterWallet() {
        BigDecimal amount = new BigDecimal("300000.00");
        EscrowTransaction escrow = fundedCenterEscrow(6L, amount);
        Wallet centerWallet = wallet(CENTER_USER_ID);

        when(escrowTransactionRepository.findById(6L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(CENTER_USER_ID)).thenReturn(centerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(6L, amount, BigDecimal.ZERO, "Hoàn thành ghi danh"));

        verify(walletService).releaseLockedFunds(PAYER_ID, amount, "ESCROW_RELEASE-6");
        verify(walletService).credit(CENTER_USER_ID, amount, "ESCROW_RELEASE-6");
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        assertSame(centerWallet, paymentCaptor.getValue().getWallet());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, paymentCaptor.getValue().getType());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    @Test
    void applyRefundsEscrowToPayer() {
        BigDecimal amount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(5L, amount);
        Wallet payerWallet = payerWallet();

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.refundLockedFunds(PAYER_ID, amount, "REFUND-ESCROW-5")).thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(5L, BigDecimal.ZERO, amount, "Hủy lớp"));

        verify(walletService).refundLockedFunds(PAYER_ID, amount, "REFUND-ESCROW-5");
        verify(walletService, never()).credit(any(), any(), any());
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction refundTx = paymentCaptor.getValue();
        assertSame(payerWallet, refundTx.getWallet());
        assertEquals(PaymentTransactionType.REFUND, refundTx.getType());
        assertEquals("REFUND-ESCROW-5", refundTx.getReferenceCode());
        assertEquals(EscrowStatus.REFUNDED, escrow.getStatus());
    }

    @Test
    void applySupportsSplitReleaseAndRefund() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        Wallet payerWallet = payerWallet();
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.refundLockedFunds(PAYER_ID, new BigDecimal("100000.00"), "REFUND-ESCROW-5"))
                .thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(
                5L,
                new BigDecimal("400000.00"),
                new BigDecimal("100000.00"),
                "Hoàn một phần"));

        verify(walletService).releaseLockedFunds(PAYER_ID, new BigDecimal("400000.00"), "ESCROW_RELEASE-5");
        verify(walletService).refundLockedFunds(PAYER_ID, new BigDecimal("100000.00"), "REFUND-ESCROW-5");
        verify(paymentTransactionRepository, times(2)).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, paymentCaptor.getAllValues().get(0).getType());
        assertEquals(PaymentTransactionType.REFUND, paymentCaptor.getAllValues().get(1).getType());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    @Test
    void applyRejectsSettlementTotalMismatch() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        assertThrows(BusinessException.class, () ->
                escrowService.apply(new ReleaseInstruction(5L, new BigDecimal("400000.00"), BigDecimal.ZERO, null)));

        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void applyReturnsWhenEscrowAlreadySettled() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        escrowService.apply(new ReleaseInstruction(5L, new BigDecimal("500000.00"), BigDecimal.ZERO, null));

        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    void refundReturnsEscrowMoneyToPayer() {
        BigDecimal amount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(5L, amount);
        Wallet payerWallet = payerWallet();

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.refundLockedFunds(PAYER_ID, amount, "REFUND-ESCROW-5")).thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowTransaction result = escrowService.refund(5L, "Client hủy lớp");

        assertSame(escrow, result);
        verify(walletService).refundLockedFunds(PAYER_ID, amount, "REFUND-ESCROW-5");
        verify(walletService, never()).credit(any(), any(), any());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction refundTx = paymentCaptor.getValue();
        assertSame(payerWallet, refundTx.getWallet());
        assertEquals(PaymentTransactionType.REFUND, refundTx.getType());
        assertEquals(PaymentTransactionStatus.SUCCESS, refundTx.getStatus());
        assertEquals("REFUND-ESCROW-5", refundTx.getReferenceCode());
        assertEquals(EscrowStatus.REFUNDED, escrow.getStatus());
    }

    @Test
    void refundReturnsExistingRefundedEscrowWithoutChargingAgain() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.REFUNDED);
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        EscrowTransaction result = escrowService.refund(5L, "Retry");

        assertSame(escrow, result);
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    void refundRejectsReleasedEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        assertThrows(BusinessException.class, () -> escrowService.refund(5L, "Too late"));

        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundRequiresFundedEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.PENDING);
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        assertThrows(BusinessException.class, () -> escrowService.refund(5L, "Not funded"));

        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundRequiresEscrowId() {
        assertThrows(BusinessException.class, () -> escrowService.refund(null, "Missing id"));
    }

    private Wallet payerWallet() {
        return wallet(PAYER_ID);
    }

    private Wallet wallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(userId);
        wallet.setAvailableBalance(new BigDecimal("1000000.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        return wallet;
    }

    private EscrowTransaction fundedPrivateEscrow(Long escrowId, BigDecimal amount) {
        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setTutor(tutor);

        EscrowTransaction escrow = fundedEscrow(escrowId, amount);
        escrow.setAssignment(assignment);
        return escrow;
    }

    private EscrowTransaction fundedCenterEscrow(Long escrowId, BigDecimal amount) {
        User centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        TutorCenter center = new TutorCenter();
        center.setUser(centerUser);
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCenter(center);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);
        classStudent.setTutoringClass(tutoringClass);

        EscrowTransaction escrow = fundedEscrow(escrowId, amount);
        escrow.setClassStudent(classStudent);
        return escrow;
    }

    private EscrowTransaction fundedEscrow(Long escrowId, BigDecimal amount) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setWallet(payerWallet());
        payment.setAmount(amount);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setPayment(payment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }
}
