package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
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
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52EscrowServiceITTest {

    private static final Long CLIENT_USER_ID = 11L;
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
    void defaultPlatformFeeIsZeroUnlessAnItCaseOverridesIt() {
        SystemParameter parameter = new SystemParameter();
        parameter.setParamValue("0.00");
        org.mockito.Mockito.lenient()
                .when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE"))
                .thenReturn(Optional.of(parameter));
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_002_PreparePrivateAssignmentQrPaymentTransaction() {
        BigDecimal amount = new BigDecimal("500000.00");
        Wallet systemEscrowWallet = wallet(999L);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findEscrowReferenceFamily("ESCROW-A7")).thenReturn(List.of());
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, 7L, null));

        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction savedPayment = paymentCaptor.getValue();
        assertSame(savedPayment, result);
        assertSame(systemEscrowWallet, savedPayment.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_DEPOSIT, savedPayment.getType());
        assertEquals(PaymentTransactionStatus.PENDING, savedPayment.getStatus());
        assertEquals(amount, savedPayment.getAmount());
        assertEquals("ESCROW-A7", savedPayment.getReferenceCode());
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_003_PrepareCenterStudentQrPaymentTransaction() {
        BigDecimal amount = new BigDecimal("600000.00");
        Wallet systemEscrowWallet = wallet(999L);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);

        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(9L)).thenReturn(Optional.empty());
        when(classStudentRepository.findById(9L)).thenReturn(Optional.of(classStudent));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-CS9",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findEscrowReferenceFamily("ESCROW-CS9")).thenReturn(List.of());
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, null, 9L));

        assertSame(systemEscrowWallet, result.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_DEPOSIT, result.getType());
        assertEquals(PaymentTransactionStatus.PENDING, result.getStatus());
        assertEquals(amount, result.getAmount());
        assertEquals("ESCROW-CS9", result.getReferenceCode());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_004_RejectEscrowCommandWhenTargetSelectorIsInvalid() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.preparePayment(new EscrowLockCommand(
                        CLIENT_USER_ID,
                        new BigDecimal("500000.00"),
                        7L,
                        9L)));

        assertEquals("Escrow phải gắn đúng một trong assignmentId hoặc classStudentId", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_017_ReopenPaymentAfterCancelledEscrowSession() {
        BigDecimal amount = new BigDecimal("500000.00");
        Wallet systemEscrowWallet = wallet(999L);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        PaymentTransaction cancelledPayment = new PaymentTransaction();
        cancelledPayment.setReferenceCode("ESCROW-A7");
        cancelledPayment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        cancelledPayment.setStatus(PaymentTransactionStatus.CANCELLED);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findEscrowReferenceFamily("ESCROW-A7"))
                .thenReturn(List.of(cancelledPayment));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, 7L, null));

        assertTrue(result.getReferenceCode().startsWith("ESCROW-A7-"));
        assertNotEquals("ESCROW-A7", result.getReferenceCode());
        assertEquals(PaymentTransactionStatus.PENDING, result.getStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_008_PreventDuplicateEscrowForSuccessfulPaymentWebhook() {
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(88L, "ESCROW-A7", new BigDecimal("500000.00"));
        EscrowTransaction existingEscrow = new EscrowTransaction();
        existingEscrow.setEscrowId(100L);
        existingEscrow.setStatus(EscrowStatus.FUNDED);

        when(escrowTransactionRepository.findByPayment_TransactionId(88L)).thenReturn(Optional.of(existingEscrow));

        EscrowTransaction result = escrowService.fundConfirmedPayment(paidEscrowPayment);

        assertSame(existingEscrow, result);
        verify(escrowTransactionRepository, never()).save(any());
        verify(classAssignmentRepository, never()).findById(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_020_FundConfirmedPrivatePaymentCreatesFundedEscrow() {
        BigDecimal amount = new BigDecimal("500000.00");
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(88L, "ESCROW-A7", amount);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByPayment_TransactionId(88L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.fundConfirmedPayment(paidEscrowPayment);

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction savedEscrow = escrowCaptor.getValue();
        assertSame(savedEscrow, result);
        assertSame(assignment, savedEscrow.getAssignment());
        assertSame(paidEscrowPayment, savedEscrow.getPayment());
        assertEquals(EscrowStatus.FUNDED, savedEscrow.getStatus());
        assertEquals(amount, savedEscrow.getAmount());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_016_HoldFundedEscrowWhenDisputeOrTerminationStarts() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.holdForDispute(5L, "Client báo sự cố lớp học");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.DISPUTED, escrow.getStatus());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_005_RejectHoldingReleasedEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.holdForDispute(5L, "Yêu cầu đến sau khi đã giải ngân"));

        assertEquals("Escrow đã tất toán nên không thể chuyển sang tranh chấp", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_006_LockEscrowRequiresSuccessfulPaymentBeforeFunding() {
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.SUCCESS))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.lock(new EscrowLockCommand(
                        CLIENT_USER_ID,
                        new BigDecimal("500000.00"),
                        7L,
                        null)));

        assertEquals("Chưa có giao dịch thanh toán escrow", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_007_FundConfirmedPaymentRejectsPendingTransaction() {
        PaymentTransaction pendingPayment = successfulEscrowPayment(88L, "ESCROW-A7", new BigDecimal("500000.00"));
        pendingPayment.setStatus(PaymentTransactionStatus.PENDING);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.fundConfirmedPayment(pendingPayment));

        assertEquals("Chỉ giao dịch đã thanh toán thành công mới sinh escrow", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_009_PreparePaymentReusesExistingPendingEscrowReference() {
        BigDecimal amount = new BigDecimal("500000.00");
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        PaymentTransaction pendingPayment = successfulEscrowPayment(55L, "ESCROW-A7", amount);
        pendingPayment.setStatus(PaymentTransactionStatus.PENDING);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of(pendingPayment));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, 7L, null));

        assertSame(pendingPayment, result);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_011_ApplySettlementSplitsReleaseAndRefundToClientWallet() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        Wallet payerWallet = payerWallet();
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.refundLockedFunds(CLIENT_USER_ID, new BigDecimal("100000.00"), "REFUND-ESCROW-5"))
                .thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        escrowService.apply(new ReleaseInstruction(
                5L,
                new BigDecimal("400000.00"),
                new BigDecimal("100000.00"),
                "Admin chia tiền sau chấm dứt sớm"));

        verify(walletService).releaseLockedFunds(CLIENT_USER_ID, new BigDecimal("400000.00"), "ESCROW_RELEASE-5");
        verify(walletService).credit(TUTOR_USER_ID, new BigDecimal("400000.00"), "ESCROW_RELEASE-5");
        verify(walletService).refundLockedFunds(CLIENT_USER_ID, new BigDecimal("100000.00"), "REFUND-ESCROW-5");
        verify(paymentTransactionRepository, times(2)).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, paymentCaptor.getAllValues().get(0).getType());
        assertEquals(PaymentTransactionType.REFUND, paymentCaptor.getAllValues().get(1).getType());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_012_RejectSettlementWhenReleaseAndRefundDoNotEqualEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.apply(new ReleaseInstruction(
                        5L,
                        new BigDecimal("400000.00"),
                        BigDecimal.ZERO,
                        "Sai tổng chia tiền")));

        assertEquals("Tổng tiền giải ngân/hoàn phải bằng số tiền escrow", exception.getMessage());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_013_HoldAlreadyDisputedEscrowIsIdempotent() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        EscrowTransaction result = escrowService.holdForDispute(5L, "Tranh chấp đang xử lý");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.DISPUTED, result.getStatus());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_014_FundConfirmedCenterPaymentCreatesFundedStudentEscrow() {
        BigDecimal amount = new BigDecimal("600000.00");
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(89L, "ESCROW-CS9", amount);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);

        when(escrowTransactionRepository.findByPayment_TransactionId(89L)).thenReturn(Optional.empty());
        when(classStudentRepository.findById(9L)).thenReturn(Optional.of(classStudent));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.fundConfirmedPayment(paidEscrowPayment);

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction savedEscrow = escrowCaptor.getValue();
        assertSame(savedEscrow, result);
        assertSame(classStudent, savedEscrow.getClassStudent());
        assertSame(paidEscrowPayment, savedEscrow.getPayment());
        assertEquals(EscrowStatus.FUNDED, savedEscrow.getStatus());
        assertEquals(amount, savedEscrow.getAmount());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_015_RejectConfirmedPaymentWhenEscrowReferenceIsMalformed() {
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(90L, "ESCROW-UNKNOWN", new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findByPayment_TransactionId(90L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.fundConfirmedPayment(paidEscrowPayment));

        assertEquals("Giao dịch thanh toán không xác định được đối tượng escrow", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_016_CreateRefundTransferRequestForDirectBankPaidEscrow() {
        BigDecimal releaseAmount = new BigDecimal("480000.00");
        BigDecimal refundAmount = new BigDecimal("720000.00");
        EscrowTransaction escrow = fundedPrivateEscrowPaidThroughQr(22L, new BigDecimal("1200000.00"));
        Wallet systemEscrowWallet = wallet(999L);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(22L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refundRequestRepository.findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(22L))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        escrowService.apply(new ReleaseInstruction(
                22L,
                releaseAmount,
                refundAmount,
                "Admin chia tiền chấm dứt sớm",
                new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A")));

        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(walletService).credit(TUTOR_USER_ID, releaseAmount, "ESCROW_RELEASE-22");
        verify(refundRequestRepository).save(refundRequestCaptor.capture());
        RefundRequest refundRequest = refundRequestCaptor.getValue();
        assertSame(escrow, refundRequest.getEscrowTransaction());
        assertEquals(CLIENT_USER_ID, refundRequest.getRequestedBy().getUserId());
        assertEquals(refundAmount, refundRequest.getAmount());
        assertEquals(RefundRequestStatus.APPROVED, refundRequest.getStatus());
        assertEquals("PENDING", refundRequest.getTransferStatus());
        assertEquals("REFUND-ESCROW-22", refundRequest.getRefundReferenceCode());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_010_DeductConfiguredPlatformFeeAsSeparateTransactions() {
        BigDecimal escrowAmount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(15L, escrowAmount);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);
        Wallet platformWallet = wallet(99L);
        User platformUser = new User();
        platformUser.setUserId(99L);
        platformWallet.setUser(platformUser);
        SystemParameter parameter = new SystemParameter();
        parameter.setParamValue("0.10");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(parameter));
        when(escrowTransactionRepository.findById(15L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.getSystemEscrowWallet()).thenReturn(platformWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        escrowService.apply(new ReleaseInstruction(15L, escrowAmount, BigDecimal.ZERO, "Hoàn thành lớp"));

        verify(walletService).credit(TUTOR_USER_ID, escrowAmount, "ESCROW_RELEASE-15");
        verify(walletService).debit(TUTOR_USER_ID, new BigDecimal("50000.00"), "PLATFORM_FEE-15");
        verify(walletService).credit(99L, new BigDecimal("50000.00"), "PLATFORM_FEE-INCOME-15");
        verify(paymentTransactionRepository, times(3)).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, paymentCaptor.getAllValues().get(0).getType());
        assertEquals(PaymentTransactionType.PLATFORM_FEE, paymentCaptor.getAllValues().get(1).getType());
        assertEquals(PaymentTransactionType.DEPOSIT, paymentCaptor.getAllValues().get(2).getType());
        assertTrue(paymentCaptor.getAllValues().get(1).getDescription().contains("10%"));
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_019_RefundFundedWalletPaidEscrowMarksEscrowRefunded() {
        EscrowTransaction escrow = fundedPrivateEscrow(19L, new BigDecimal("500000.00"));
        Wallet payerWallet = payerWallet();

        when(escrowTransactionRepository.findById(19L)).thenReturn(Optional.of(escrow));
        when(walletService.refundLockedFunds(CLIENT_USER_ID, escrow.getAmount(), "REFUND-ESCROW-19"))
                .thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.refund(19L, "Admin hoàn toàn bộ do hủy lớp");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.REFUNDED, escrow.getStatus());
        verify(walletService).refundLockedFunds(CLIENT_USER_ID, escrow.getAmount(), "REFUND-ESCROW-19");
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.REFUND, paymentCaptor.getValue().getType());
    }

    private PaymentTransaction successfulEscrowPayment(Long transactionId, String reference, BigDecimal amount) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setAmount(amount);
        payment.setReferenceCode(reference);
        return payment;
    }

    private Wallet payerWallet() {
        Wallet wallet = wallet(CLIENT_USER_ID);
        wallet.setFrozenBalance(new BigDecimal("1000000.00"));
        return wallet;
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

    private EscrowTransaction fundedPrivateEscrowPaidThroughQr(Long escrowId, BigDecimal amount) {
        User payer = new User();
        payer.setUserId(CLIENT_USER_ID);
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

    @SuppressWarnings("unused")
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
}
