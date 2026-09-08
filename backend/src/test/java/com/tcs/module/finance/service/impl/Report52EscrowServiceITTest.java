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
