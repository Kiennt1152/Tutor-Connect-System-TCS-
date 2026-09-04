package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.BusinessException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52CenterRequestFeeITTest {

    private static final String REQUEST_ID = "REQ-CENTER-001";
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long CENTER_USER_ID = 22L;

    @Mock private CenterRequestFeeHoldRepository feeHoldRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private WalletService walletService;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;

    @InjectMocks
    private CenterRequestFeeServiceImpl centerRequestFeeService;

    @Test
    @Tag("report52-it")
    void IT_CFR_001_CreateCenterRequestFeePaymentBuildsPendingQrHold() {
        SystemParameter feeRate = new SystemParameter();
        feeRate.setParamKey("PLATFORM_FEE_RATE");
        feeRate.setParamValue("0.02");
        Wallet systemWallet = wallet(999L);

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(feeRate));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setTransactionId(501L);
            return tx;
        });
        when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(invocation -> {
            CenterRequestFeeHold hold = invocation.getArgument(0);
            hold.setFeeHoldId(601L);
            return hold;
        });

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(CenterRequestFeeStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(new BigDecimal("10000"), response.getAmount());
        assertTrue(response.getReferenceCode().startsWith("CENTERREQ-"));
        assertEquals(response.getReferenceCode(), response.getTransferContent());
        assertTrue(response.getQrUrl().contains("img.vietqr.io"));
        assertEquals("****6789", response.getPayoutAccountNoMasked());
        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Phí xử lý yêu cầu đã sẵn sàng"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_002_GetPaymentReturnsExistingCenterRequestFeeHold() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        Optional<CenterRequestFeePaymentResponse> response = centerRequestFeeService.getPayment(REQUEST_ID);

        assertTrue(response.isPresent());
        assertEquals(REQUEST_ID, response.get().getRequestId());
        assertEquals(CenterRequestFeeStatus.PENDING_PAYMENT, response.get().getStatus());
        assertEquals("CENTERREQ-ABC", response.get().getReferenceCode());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_003_PaymentDetailResponseIncludesQrAndMaskedPayoutAccount() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));
        existing.setClassId(71L);
        existing.setAssignmentId(81L);
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.getPayment(REQUEST_ID).orElseThrow();

        assertEquals(71L, response.getClassId());
        assertEquals(81L, response.getAssignmentId());
        assertEquals("TPBank", response.getBankName());
        assertEquals("02660559201", response.getAccountNumber());
        assertEquals("CENTERREQ-ABC", response.getTransferContent());
        assertTrue(response.getQrUrl().contains("amount=10000"));
        assertTrue(response.getQrUrl().contains("addInfo=CENTERREQ-ABC"));
        assertEquals("****6789", response.getPayoutAccountNoMasked());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_004_RejectPaymentCreationWhenPayoutInformationIsMissing() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> centerRequestFeeService.createPayment(
                        REQUEST_ID,
                        CLIENT_USER_ID,
                        CENTER_USER_ID,
                        "Trung tâm Minh Tâm",
                        new BigDecimal("500000.00"),
                        new RefundPayoutInfo("TPBank", "", "Nguyen Van A")));

        assertEquals("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_005_RejectPaymentCreationWhenRequiredRequestOrCenterDataIsMissing() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> centerRequestFeeService.createPayment(
                        "",
                        CLIENT_USER_ID,
                        CENTER_USER_ID,
                        "Trung tâm Minh Tâm",
                        new BigDecimal("500000.00"),
                        payoutInfo()));

        assertEquals("Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_009_ReturnExistingHoldInsteadOfCreatingDuplicatePayment() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(601L, response.getFeeHoldId());
        assertEquals("CENTERREQ-ABC", response.getReferenceCode());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_010_LinkFulfilledRequestStoresClassAndAssignmentTrace() {
        CenterRequestFeeHold hold = pendingHold(601L, pendingPayment(501L));
        hold.setStatus(CenterRequestFeeStatus.HELD);
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));

        centerRequestFeeService.linkFulfilledAssignment(REQUEST_ID, 71L, 81L);

        assertEquals(71L, hold.getClassId());
        assertEquals(81L, hold.getAssignmentId());
        verify(feeHoldRepository).save(hold);
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_011_RequestRefundNotifiesClientAndPlatformAdmin() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        hold.setStatus(CenterRequestFeeStatus.HELD);
        User centerUser = user(CENTER_USER_ID, "center.it@tcs.test");
        User adminUser = user(1L, "admin.it@tcs.test");

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));
        when(refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(601L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(CENTER_USER_ID)).thenReturn(Optional.of(centerUser));
        when(platformAdminRepository.findAll()).thenReturn(java.util.List.of(platformAdmin(adminUser)));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refund = invocation.getArgument(0);
            refund.setRefundId(701L);
            return refund;
        });

        centerRequestFeeService.requestRefund(REQUEST_ID, "Trung tâm không thể tìm gia sư phù hợp");

        ArgumentCaptor<RefundRequest> refundCaptor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(refundRequestRepository).save(refundCaptor.capture());
        assertEquals(RefundRequestStatus.PENDING, refundCaptor.getValue().getStatus());
        assertEquals("PENDING", refundCaptor.getValue().getTransferStatus());
        assertEquals(new BigDecimal("10000.00"), refundCaptor.getValue().getAmount());
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu hoàn phí trung tâm mới"),
                any(),
                eq("REFUND_REQUEST"),
                eq(701L));
        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Đã tạo yêu cầu hoàn phí"),
                any(),
                eq("REFUND_REQUEST"),
                eq(701L));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_012_ReloadPendingPaymentReturnsSameQrReferenceWithoutCreatingANewHold() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(601L, response.getFeeHoldId());
        assertEquals("CENTERREQ-ABC", response.getTransferContent());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_013_CompleteIncomingPaymentMovesHoldToHeldAndNotifiesClientAndCenter() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

        CenterRequestFeePaymentResponse response = centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-1");

        assertEquals(CenterRequestFeeStatus.HELD, response.getStatus());
        assertEquals(PaymentTransactionStatus.SUCCESS, payment.getStatus());
        assertEquals("SEPAY-IN-1", payment.getExternalTransactionId());
        verify(paymentTransactionRepository).save(payment);
        verify(feeHoldRepository).save(hold);
        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Thanh toán phí yêu cầu thành công"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
        verify(paymentNotificationService).notifyPayment(
                eq(CENTER_USER_ID),
                eq("Có yêu cầu mới đã thanh toán phí"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_014_ConfiguredFeeRateMatchesQrAmountAndPendingTransaction() {
        SystemParameter feeRate = new SystemParameter();
        feeRate.setParamKey("PLATFORM_FEE_RATE");
        feeRate.setParamValue("0.05");
        Wallet systemWallet = wallet(999L);

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(feeRate));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setTransactionId(502L);
            return tx;
        });
        when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(invocation -> {
            CenterRequestFeeHold hold = invocation.getArgument(0);
            hold.setFeeHoldId(602L);
            return hold;
        });

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(new BigDecimal("25000"), response.getAmount());
        assertTrue(response.getQrUrl().contains("amount=25000"));
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(new BigDecimal("25000"), txCaptor.getValue().getAmount());
        assertTrue(txCaptor.getValue().getDescription().contains("5%"));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_015_PaymentConfirmationUpdatesHoldAndClassRequestStatusTrace() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        ClassRequestStore.ClassRequestData data = new ClassRequestStore.ClassRequestData(
                REQUEST_ID,
                CLIENT_USER_ID,
                33L,
                null,
                "Nhờ trung tâm tìm gia sư",
                new BigDecimal("500000.00"),
                ClassRequestStore.STATUS_PAYMENT_PENDING,
                null,
                LocalDateTime.now().toString(),
                "{}",
                java.util.List.of(),
                null);
        ClassRequestStore.ClassRequestData pendingData = new ClassRequestStore.ClassRequestData(
                REQUEST_ID,
                CLIENT_USER_ID,
                33L,
                null,
                "Nhờ trung tâm tìm gia sư",
                new BigDecimal("500000.00"),
                ClassRequestStore.STATUS_PENDING,
                null,
                data.createdAt(),
                "{}",
                java.util.List.of(),
                null);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
        when(classRequestStore.withStatus(data, ClassRequestStore.STATUS_PENDING, null)).thenReturn(pendingData);

        CenterRequestFeePaymentResponse response = centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-2");

        assertEquals(CenterRequestFeeStatus.HELD, response.getStatus());
        assertEquals(CenterRequestFeeStatus.HELD, hold.getStatus());
        assertEquals(PaymentTransactionStatus.SUCCESS, payment.getStatus());
        verify(classRequestStore).save(pendingData);
        verify(feeHoldRepository).save(hold);
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_019_PaymentSuccessNotificationsUseClassRequestFeeReferenceForRequestList() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

        centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-3");

        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Thanh toán phí yêu cầu thành công"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
        verify(paymentNotificationService).notifyPayment(
                eq(CENTER_USER_ID),
                eq("Có yêu cầu mới đã thanh toán phí"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    @Test
    @Tag("report52-it")
    void IT_ADM_014_PlatformFeeParameterIsUsedWhenBuildingCenterRequestPayment() {
        SystemParameter feeRate = new SystemParameter();
        feeRate.setParamKey("PLATFORM_FEE_RATE");
        feeRate.setParamValue("0.10");

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(feeRate));
        when(walletService.getSystemEscrowWallet()).thenReturn(wallet(999L));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(invocation -> {
            CenterRequestFeeHold hold = invocation.getArgument(0);
            hold.setFeeHoldId(603L);
            return hold;
        });

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("800000.00"),
                payoutInfo());

        assertEquals(new BigDecimal("80000"), response.getAmount());
        assertTrue(response.getQrUrl().contains("amount=80000"));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_008_ReplayedSuccessfulWebhookDoesNotCreateDuplicatePaidRequest() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        hold.setStatus(CenterRequestFeeStatus.HELD);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-DUP");

        assertEquals(CenterRequestFeeStatus.HELD, response.getStatus());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
        verify(classRequestStore, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_016_ReleaseFulfilledCenterRequestFeeToCenterWallet() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        hold.setStatus(CenterRequestFeeStatus.HELD);
        hold.setAssignmentId(77L);
        Wallet centerWallet = wallet(CENTER_USER_ID);

        when(feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(77L)).thenReturn(Optional.of(hold));
        when(walletService.getOrCreate(CENTER_USER_ID)).thenReturn(centerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        centerRequestFeeService.releaseForFulfilledAssignment(77L, "Lớp đã hoàn thành");

        assertEquals(CenterRequestFeeStatus.RELEASED, hold.getStatus());
        verify(walletService).credit(CENTER_USER_ID, hold.getAmount(), "CENTERREQ_RELEASE-601");
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(paymentNotificationService).notifyPayment(
                eq(CENTER_USER_ID),
                eq("Đã nhận phí xử lý yêu cầu"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_017_CancelUnpaidFeeHoldCancelsPendingPaymentAndDeletesDraftRequest() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));

        centerRequestFeeService.cancelUnpaid(REQUEST_ID);

        assertEquals(PaymentTransactionStatus.CANCELLED, payment.getStatus());
        assertEquals(CenterRequestFeeStatus.CANCELLED, hold.getStatus());
        verify(paymentTransactionRepository).save(payment);
        verify(feeHoldRepository).save(hold);
        verify(classRequestStore).delete(REQUEST_ID);
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_020_RequestRefundCreatesAdminTransferAndMarksHoldRefundRequested() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        hold.setStatus(CenterRequestFeeStatus.HELD);
        User centerUser = user(CENTER_USER_ID, "center.it@tcs.test");
        User adminUser = user(1L, "admin.it@tcs.test");

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));
        when(refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(601L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(CENTER_USER_ID)).thenReturn(Optional.of(centerUser));
        when(platformAdminRepository.findAll()).thenReturn(java.util.List.of(platformAdmin(adminUser)));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refund = invocation.getArgument(0);
            refund.setRefundId(701L);
            return refund;
        });

        centerRequestFeeService.requestRefund(REQUEST_ID, "Trung tâm không thể tìm gia sư phù hợp");

        assertEquals(CenterRequestFeeStatus.REFUND_REQUESTED, hold.getStatus());
        ArgumentCaptor<RefundRequest> refundCaptor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(refundRequestRepository).save(refundCaptor.capture());
        RefundRequest refund = refundCaptor.getValue();
        assertEquals(RefundRequestStatus.PENDING, refund.getStatus());
        assertEquals("PENDING", refund.getTransferStatus());
        assertEquals("REFUND-CREQFEE-601", refund.getRefundReferenceCode());
        assertEquals("****6789", com.tcs.module.finance.util.RefundPayoutInfoCodec.maskAccountNo(refund.getAccountNo()));
        verify(feeHoldRepository).save(hold);
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu hoàn phí trung tâm mới"),
                any(),
                eq("REFUND_REQUEST"),
                eq(701L));
    }

    private CenterRequestFeeHold pendingHold(Long holdId, PaymentTransaction payment) {
        CenterRequestFeeHold hold = new CenterRequestFeeHold();
        hold.setFeeHoldId(holdId);
        hold.setRequestId(REQUEST_ID);
        hold.setClientUserId(CLIENT_USER_ID);
        hold.setCenterUserId(CENTER_USER_ID);
        hold.setCenterName("Trung tâm Minh Tâm");
        hold.setPaymentTransaction(payment);
        hold.setProjectedEscrowAmount(new BigDecimal("500000.00"));
        hold.setAmount(new BigDecimal("10000.00"));
        hold.setReferenceCode("CENTERREQ-ABC");
        hold.setPayoutBankName("TPBank");
        hold.setPayoutAccountNo("0123456789");
        hold.setPayoutAccountHolderName("Nguyen Van A");
        hold.setStatus(CenterRequestFeeStatus.PENDING_PAYMENT);
        hold.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
        return hold;
    }

    private PaymentTransaction pendingPayment(Long transactionId) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setWallet(wallet(999L));
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setAmount(new BigDecimal("10000.00"));
        payment.setReferenceCode("CENTERREQ-ABC");
        payment.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
        return payment;
    }

    private RefundPayoutInfo payoutInfo() {
        return new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A");
    }

    private Wallet wallet(Long walletId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        return wallet;
    }

    private PlatformAdmin platformAdmin(User user) {
        PlatformAdmin admin = new PlatformAdmin();
        admin.setAdminId(user.getUserId());
        admin.setUser(user);
        return admin;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        return user;
    }
}
