package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.CreateRefundRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.RefundDecisionRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.AdminWithdrawalPageResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentMethod;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Report 5.2 F08 Escrow Payment: all test functions for this sheet.
 * Separate package-private classes preserve each original JUnit test context.
 */
@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52F08EscrowPaymentITTest {

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

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52F08EscrowPaymentPart2ITTest {

    private static final Long USER_ID = 7L;

    @Mock private AuthHelper authHelper;
    @Mock private WalletService walletService;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private WithdrawalRequestRepository withdrawalRequestRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private CenterRequestFeeHoldRepository centerRequestFeeHoldRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private EscrowService escrowService;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClassRequestStore classRequestStore;

    @InjectMocks
    private FinanceServiceImpl financeService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setWalletId(USER_ID);
        User owner = new User();
        owner.setUserId(USER_ID);
        owner.setEmail("finance.it@tcs.test");
        wallet.setUser(owner);
        wallet.setAvailableBalance(new BigDecimal("250000.00"));
        wallet.setFrozenBalance(new BigDecimal("50000.00"));
        wallet.setStatus(WalletStatus.ACTIVE);
        lenient().when(tutorRepository.existsByUser_UserIdAndVerificationStatus(
                        USER_ID,
                        ProfileVerificationStatus.VERIFIED))
                .thenReturn(true);
        lenient().when(tutorCenterRepository.existsByUser_UserIdAndVerificationStatus(
                        USER_ID,
                        ProfileVerificationStatus.VERIFIED))
                .thenReturn(true);
        ReflectionTestUtils.setField(financeService, "directDepositEnabled", true);
        ReflectionTestUtils.setField(financeService, "simulateTopupEnabled", true);
    }




    @Test
    @Tag("report52-it")
    void IT_ESC_001_FundEscrowAndPublishClassActivationEventAfterPaymentWebhook() {
        BigDecimal amount = new BigDecimal("500000");
        PaymentTransaction tx = pendingEscrowPayment("ESCROW-A7", amount);
        EscrowTransaction escrow = privateEscrow(5L, tx, amount);
        SepayWebhookRequest request = incomingWebhook(456L, amount, "Thanh toan hoc phi ESCROW-A7");

        when(paymentTransactionRepository.findByExternalTransactionId("456")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                amount)).thenReturn(List.of(tx));
        when(centerRequestFeeService.isCenterRequestFeePayment(tx)).thenReturn(false);
        when(escrowService.fundConfirmedPayment(tx)).thenAnswer(invocation -> {
            escrow.setStatus(EscrowStatus.FUNDED);
            return escrow;
        });

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("ESCROW-A7", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("456", tx.getExternalTransactionId());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        verify(paymentTransactionRepository).save(tx);
        verify(escrowService).fundConfirmedPayment(tx);
        verify(eventPublisher).publishEvent(any(EscrowFunded.class));
    }





























    private RefundDecisionRequest refundDecision(BigDecimal approvedAmount, String reason) {
        RefundDecisionRequest request = new RefundDecisionRequest();
        request.setApprovedAmount(approvedAmount);
        request.setReason(reason);
        return request;
    }

    private CreateRefundRequest createRefundRequest(Long escrowId, BigDecimal amount) {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setEscrowId(escrowId);
        request.setAmount(amount);
        request.setReason("Phụ huynh yêu cầu hoàn tiền theo thỏa thuận xử lý");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Thu Ha");
        return request;
    }

    private EscrowTransaction privateFundedEscrow(Long escrowId, BigDecimal amount) {
        PaymentTransaction payment = pendingEscrowPayment("ESCROW-" + escrowId, amount);
        payment.setWallet(wallet);
        EscrowTransaction escrow = privateEscrow(escrowId, payment, amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }

    private RefundRequest pendingRefund(Long refundId, EscrowTransaction escrow) {
        RefundRequest refund = new RefundRequest();
        refund.setRefundId(refundId);
        refund.setEscrowTransaction(escrow);
        refund.setRequestedBy(wallet.getUser());
        refund.setAmount(new BigDecimal("300000.00"));
        refund.setBankName("TPBank");
        refund.setAccountNo("0123456789");
        refund.setAccountHolderName("Nguyen Thu Ha");
        refund.setReason("""
                Phụ huynh yêu cầu hoàn tiền

                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyen Thu Ha
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);
        refund.setTransferStatus("PENDING");
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestedAt(LocalDateTime.of(2026, 8, 31, 9, 30));
        return refund;
    }

    private CenterRequestFeeHold centerFeeHold(Long holdId, CenterRequestFeeStatus status) {
        CenterRequestFeeHold hold = new CenterRequestFeeHold();
        hold.setFeeHoldId(holdId);
        hold.setRequestId("REQ-CF");
        hold.setClientUserId(USER_ID);
        hold.setCenterUserId(22L);
        hold.setCenterName("Trung tâm Minh Tâm");
        hold.setProjectedEscrowAmount(new BigDecimal("500000.00"));
        hold.setAmount(new BigDecimal("30000.00"));
        hold.setReferenceCode("CENTERREQ-ABC");
        hold.setPayoutBankName("TPBank");
        hold.setPayoutAccountNo("0123456789");
        hold.setPayoutAccountHolderName("Nguyen Thu Ha");
        hold.setStatus(status);
        return hold;
    }

    private RefundRequest pendingCenterFeeRefund(Long refundId, CenterRequestFeeHold hold) {
        RefundRequest refund = new RefundRequest();
        refund.setRefundId(refundId);
        refund.setCenterRequestFeeHold(hold);
        refund.setRequestedBy(wallet.getUser());
        refund.setAmount(new BigDecimal("30000.00"));
        refund.setBankName("TPBank");
        refund.setAccountNo("0123456789");
        refund.setAccountHolderName("Nguyen Thu Ha");
        refund.setRefundReferenceCode("REFUND-CREQFEE-" + hold.getFeeHoldId());
        refund.setTransferStatus("PENDING");
        refund.setReason("Hoàn phí nhờ trung tâm");
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestedAt(LocalDateTime.of(2026, 8, 31, 9, 30));
        return refund;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPasswordHash("hash");
        return user;
    }

    private DepositRequest topupRequest(String amount) {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private CreateWithdrawalRequest withdrawalRequest(BigDecimal amount, Long paymentMethodId) {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(amount);
        request.setPaymentMethodId(paymentMethodId);
        return request;
    }

    private PaymentMethodRequest paymentMethodRequest(String bankName, String accountNo, String accountHolderName) {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setBankName(bankName);
        request.setAccountNo(accountNo);
        request.setAccountHolderName(accountHolderName);
        return request;
    }

    private SepayWebhookRequest incomingWebhook(Long id, BigDecimal amount, String content) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(id);
        request.setTransferType("in");
        request.setTransferAmount(amount);
        request.setContent(content);
        request.setAccountNumber("02660559201");
        return request;
    }

    private SepayWebhookRequest outgoingWebhook(Long id, BigDecimal amount, String content) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(id);
        request.setTransferType("out");
        request.setTransferAmount(amount);
        request.setContent(content);
        request.setAccountNumber("02660559201");
        return request;
    }

    private PaymentTransaction pendingTopup(String reference, BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setDescription("Nạp tiền ví qua mã QR chuyển khoản");
        tx.setReferenceCode(reference);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    private PaymentTransaction pendingEscrowPayment(String reference, BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(88L);
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setReferenceCode(reference);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    private EscrowTransaction privateEscrow(Long escrowId, PaymentTransaction payment, BigDecimal amount) {
        User payer = new User();
        payer.setUserId(USER_ID);
        payer.setEmail("client@tcs.com");
        User tutorUser = new User();
        tutorUser.setUserId(22L);
        tutorUser.setEmail("tutor@tcs.com");
        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setCreator(payer);
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setTutor(tutor);
        assignment.setApplication(application);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setPayment(payment);
        escrow.setAssignment(assignment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.PENDING);
        return escrow;
    }

    private PaymentMethod savedPaymentMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId(3L);
        method.setWallet(wallet);
        method.setType("BANK_TRANSFER");
        method.setBankName("TPBank");
        method.setAccountNo("1234567890");
        method.setAccountHolderName("Nguyễn Văn A");
        method.setStatus("ACTIVE");
        return method;
    }

    private WithdrawalRequest pendingWithdrawal(
            Long withdrawalId,
            PaymentMethod method,
            BigDecimal amount,
            LocalDateTime requestedAt) {

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(withdrawalId);
        withdrawal.setWallet(wallet);
        withdrawal.setPaymentMethod(method);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalRequestStatus.PENDING);
        withdrawal.setRequestedAt(requestedAt);
        return withdrawal;
    }

    private PaymentTransaction pendingWithdrawalTransaction(
            PaymentMethod method,
            BigDecimal amount,
            String reference,
            LocalDateTime createdAt) {

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setPaymentMethod(method);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setReferenceCode(reference);
        tx.setCreatedAt(createdAt);
        return tx;
    }

    private RefundRequest approvedRefundTransfer(Long refundId, BigDecimal amount) {
        User client = new User();
        client.setUserId(USER_ID);
        client.setEmail("client@tcs.com");

        RefundRequest refund = new RefundRequest();
        refund.setRefundId(refundId);
        refund.setRequestedBy(client);
        refund.setAmount(amount);
        refund.setBankName("TPBank");
        refund.setAccountNo("0123456789");
        refund.setAccountHolderName("Nguyen Thu Ha");
        refund.setReason("""
                Hoàn tiền theo quyết định xử lý

                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyen Thu Ha
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);
        refund.setRefundReferenceCode("REFUND-ESCROW-10");
        refund.setTransferStatus("PENDING");
        refund.setStatus(RefundRequestStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.of(2026, 8, 31, 10, 0));
        refund.setRequestedAt(LocalDateTime.of(2026, 8, 31, 9, 30));
        return refund;
    }
}

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52F08EscrowPaymentPart3ITTest {

    private static final Long USER_ID = 7L;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private PaymentReconciliationService reconciliationService;


    @Test
    @Tag("report52-it")
    void IT_ESC_018_CancelExpiredClassEscrowPaymentSession() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        PaymentTransaction escrowPayment = transaction(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("500000.00"),
                "ESCROW-A7",
                now.minusMinutes(20));

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(escrowPayment));

        int changed = reconciliationService.expirePendingEscrowDeposits(now);

        assertEquals(1, changed);
        assertEquals(PaymentTransactionStatus.CANCELLED, escrowPayment.getStatus());
        assertEquals(now, escrowPayment.getProcessedAt());
        assertNotNull(escrowPayment.getFailureReason());
        verify(paymentTransactionRepository).saveAll(List.of(escrowPayment));
    }




    private PaymentTransaction transaction(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount,
            String referenceCode,
            LocalDateTime createdAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWallet(wallet(USER_ID));
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setCreatedAt(createdAt);
        return transaction;
    }

    private Wallet wallet(Long walletId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        return wallet;
    }

    private WithdrawalRequest withdrawal(
            Wallet wallet,
            BigDecimal amount,
            LocalDateTime requestedAt,
            WithdrawalRequestStatus status) {

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(11L);
        withdrawal.setWallet(wallet);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(status);
        withdrawal.setRequestedAt(requestedAt);
        return withdrawal;
    }

    private PaymentTransaction withdrawalTransaction(
            Wallet wallet,
            BigDecimal amount,
            String referenceCode,
            LocalDateTime createdAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setType(PaymentTransactionType.WITHDRAWAL);
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        transaction.setCreatedAt(createdAt);
        return transaction;
    }
}
