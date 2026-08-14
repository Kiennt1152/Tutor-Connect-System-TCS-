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
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @Mock
    private SystemParameterRepository systemParameterRepository;

    @InjectMocks
    private EscrowServiceImpl escrowService;

    @Captor
    private ArgumentCaptor<PaymentTransaction> paymentCaptor;

    @Captor
    private ArgumentCaptor<EscrowTransaction> escrowCaptor;

    @Captor
    private ArgumentCaptor<RefundRequest> refundRequestCaptor;

    @BeforeEach
    void defaultToZeroPlatformFeeForLegacySettlementAssertions() {
        SystemParameter parameter = new SystemParameter();
        parameter.setParamValue("0.00");
        org.mockito.Mockito.lenient()
                .when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE"))
                .thenReturn(Optional.of(parameter));
    }

    @Test
    void preparePrivateAssignmentCreatesPendingEscrowPayment() {
        BigDecimal amount = new BigDecimal("500000.00");
        Wallet wallet = wallet(999L);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findByReferenceCode("ESCROW-A7")).thenReturn(Optional.empty());
        when(walletService.getSystemEscrowWallet()).thenReturn(wallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(new EscrowLockCommand(PAYER_ID, amount, 7L, null));

        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction payment = paymentCaptor.getValue();
        assertSame(wallet, payment.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_DEPOSIT, payment.getType());
        assertEquals(PaymentTransactionStatus.PENDING, payment.getStatus());
        assertEquals("ESCROW-A7", payment.getReferenceCode());
        assertSame(payment, result);
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    void prepareCenterEnrollmentCreatesPendingEscrowPayment() {
        BigDecimal amount = new BigDecimal("300000.00");
        Wallet wallet = wallet(999L);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);

        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(9L)).thenReturn(Optional.empty());
        when(classStudentRepository.findById(9L)).thenReturn(Optional.of(classStudent));
        when(paymentTransactionRepository.findByReferenceCode("ESCROW-CS9")).thenReturn(Optional.empty());
        when(walletService.getSystemEscrowWallet()).thenReturn(wallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(new EscrowLockCommand(PAYER_ID, amount, null, 9L));

        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        assertEquals("ESCROW-CS9", paymentCaptor.getValue().getReferenceCode());
        assertEquals(PaymentTransactionStatus.PENDING, paymentCaptor.getValue().getStatus());
        assertSame(paymentCaptor.getValue(), result);
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    void preparePaymentReturnsExistingEscrowPaymentWithoutChargingAgain() {
        PaymentTransaction payment = new PaymentTransaction();
        EscrowTransaction existing = new EscrowTransaction();
        existing.setEscrowId(100L);
        existing.setPayment(payment);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.of(existing));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(PAYER_ID, new BigDecimal("500000.00"), 7L, null));

        assertSame(payment, result);
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    void fundConfirmedPaymentCreatesFundedEscrow() {
        BigDecimal amount = new BigDecimal("500000.00");
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(88L);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setAmount(amount);
        payment.setReferenceCode("ESCROW-A7");
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByPayment_TransactionId(88L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowTransaction result = escrowService.fundConfirmedPayment(payment);

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction escrow = escrowCaptor.getValue();
        assertSame(assignment, escrow.getAssignment());
        assertSame(payment, escrow.getPayment());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        assertEquals(amount, escrow.getAmount());
        assertSame(escrow, result);
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
    void applyDeductsConfiguredPlatformFeeAndRecordsActualFeeTransaction() {
        BigDecimal amount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(15L, amount);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);
        Wallet platformWallet = wallet(99L);
        User platformUser = new User();
        platformUser.setUserId(99L);
        platformWallet.setUser(platformUser);
        SystemParameter parameter = new SystemParameter();
        parameter.setParamValue("0.10");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE"))
                .thenReturn(Optional.of(parameter));
        when(escrowTransactionRepository.findById(15L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.getSystemEscrowWallet()).thenReturn(platformWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(15L, amount, BigDecimal.ZERO, "Hoàn thành lớp"));

        verify(walletService).releaseLockedFunds(PAYER_ID, amount, "ESCROW_RELEASE-15");
        verify(walletService).credit(TUTOR_USER_ID, new BigDecimal("450000.00"), "ESCROW_RELEASE-15");
        verify(walletService).credit(99L, new BigDecimal("50000.00"), "PLATFORM_FEE-15");
        verify(paymentTransactionRepository, times(2)).save(paymentCaptor.capture());
        PaymentTransaction release = paymentCaptor.getAllValues().get(0);
        PaymentTransaction fee = paymentCaptor.getAllValues().get(1);
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, release.getType());
        assertEquals(new BigDecimal("450000.00"), release.getAmount());
        assertEquals(PaymentTransactionType.PLATFORM_FEE, fee.getType());
        assertEquals(new BigDecimal("50000.00"), fee.getAmount());
        assertEquals("PLATFORM_FEE-15", fee.getReferenceCode());
    }

    @Test
    void applyReleasesDisputedEscrowToTutor() {
        BigDecimal amount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(5L, amount);
        escrow.setStatus(EscrowStatus.DISPUTED);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(5L, amount, BigDecimal.ZERO, "Admin giải ngân sau tranh chấp"));

        verify(walletService).releaseLockedFunds(PAYER_ID, amount, "ESCROW_RELEASE-5");
        verify(walletService).credit(TUTOR_USER_ID, amount, "ESCROW_RELEASE-5");
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
    void applyCreatesApprovedRefundTransferRequestWhenPayerDidNotUseWallet() {
        BigDecimal releaseAmount = new BigDecimal("300000.00");
        BigDecimal refundAmount = new BigDecimal("600000.00");
        EscrowTransaction escrow = fundedPrivateEscrowPaidThroughSystem(21L, new BigDecimal("900000.00"));
        Wallet systemWallet = wallet(999L);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(21L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.getSystemEscrowWallet()).thenReturn(systemWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refundRequestRepository.findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(21L))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        escrowService.apply(new ReleaseInstruction(
                21L,
                releaseAmount,
                refundAmount,
                "Auto tất toán sớm",
                new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A")));

        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(walletService).credit(TUTOR_USER_ID, releaseAmount, "ESCROW_RELEASE-21");

        verify(paymentTransactionRepository, times(2)).save(paymentCaptor.capture());
        PaymentTransaction refundTx = paymentCaptor.getAllValues().get(1);
        assertSame(systemWallet, refundTx.getWallet());
        assertEquals(PaymentTransactionType.REFUND, refundTx.getType());
        assertEquals(PaymentTransactionStatus.PENDING, refundTx.getStatus());
        assertEquals(refundAmount, refundTx.getAmount());
        assertEquals("REFUND-ESCROW-21", refundTx.getReferenceCode());

        verify(refundRequestRepository).save(refundRequestCaptor.capture());
        RefundRequest refundRequest = refundRequestCaptor.getValue();
        assertSame(escrow, refundRequest.getEscrowTransaction());
        assertEquals(PAYER_ID, refundRequest.getRequestedBy().getUserId());
        assertEquals(refundAmount, refundRequest.getAmount());
        assertEquals(RefundRequestStatus.APPROVED, refundRequest.getStatus());
        assertEquals("PENDING", refundRequest.getTransferStatus());
        assertEquals("REFUND-ESCROW-21", refundRequest.getRefundReferenceCode());
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
    void holdForDisputeMarksEscrowAsDisputed() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowTransaction result = escrowService.holdForDispute(5L, "Có tranh chấp từ report");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.DISPUTED, escrow.getStatus());
        verify(escrowTransactionRepository).save(escrow);
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
    }

    @Test
    void holdForDisputeReturnsAlreadyDisputedEscrowWithoutSavingAgain() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        EscrowTransaction result = escrowService.holdForDispute(5L, "Retry");

        assertSame(escrow, result);
        verify(escrowTransactionRepository, never()).save(any());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
    }

    @Test
    void holdForDisputeRejectsReleasedEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);
        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        assertThrows(BusinessException.class, () -> escrowService.holdForDispute(5L, "Too late"));

        verify(escrowTransactionRepository, never()).save(any());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
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

    private EscrowTransaction fundedPrivateEscrowPaidThroughSystem(Long escrowId, BigDecimal amount) {
        User payer = new User();
        payer.setUserId(PAYER_ID);
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(payer);
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setTutor(tutor);
        assignment.setApplication(application);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setWallet(wallet(999L));
        payment.setAmount(amount);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setPayment(payment);
        escrow.setAssignment(assignment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
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
