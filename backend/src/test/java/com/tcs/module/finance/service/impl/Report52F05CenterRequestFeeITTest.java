package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.EscrowTransaction;
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
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.LessonRescheduleRequestRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.impl.*;
import com.tcs.module.marketplace.service.impl.LessonReminderService;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
import org.springframework.context.ApplicationEventPublisher;

/**
 * Report 5.2 F05 Center Request Fee: all test functions for this sheet.
 * Separate package-private classes preserve each original JUnit test context.
 */
@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F05CenterRequestFeeITTest {

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

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F05CenterRequestFeePart2ITTest {

    private static final Long CLASS_ID = 5L;
    private static final Long ASSIGNMENT_ID = 7L;
    private static final Long CLASS_STUDENT_ID = 8L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;

    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractService contractService;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private EscrowService escrowService;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private FavoriteTutorRepository favoriteTutorRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private AuditLogService auditLogService;
    @Mock private CccdService cccdService;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private TutorSubjectRepository tutorSubjectRepository;
    @Mock private LessonRescheduleRequestRepository rescheduleRequestRepository;
    @Mock private LessonReminderService lessonReminderService;
    @Mock private EmailOtpRepository emailOtpRepository;
    @Mock private OtpService otpService;
    @Mock private EmailService contractEmailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClassRequestStore classRequestStore;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;
















































    @Test
    @Tag("report52-it")
    void IT_CFR_006_BlockAnonymousCenterRequestBeforeCreatingFeeHold() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClassRequest(77L, classRequestCreateRequest()));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(classRequestStore, never()).create(any(), any(), any(), any(), any(), any());
        verify(centerRequestFeeService, never()).createPayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_007_BlockTutorRoleFromCreatingClientCenterRequest() {
        User tutorUser = user(TUTOR_USER_ID);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClassRequest(77L, classRequestCreateRequest()));

        assertEquals("Chỉ phụ huynh/khách hàng mới tạo lớp học", exception.getMessage());
        verify(tutorCenterRepository, never()).findById(any());
        verify(classRequestStore, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void SUPPORT_CFR_PreventClientFromCancellingAnotherClientsCenterRequestFeeHold() {
        User otherClient = user(333L);
        ClassRequestStore.ClassRequestData data = new ClassRequestStore.ClassRequestData(
                "REQ-CFR-008",
                CLIENT_USER_ID,
                77L,
                null,
                "Nhờ trung tâm tìm gia sư Toán",
                new BigDecimal("500000.00"),
                ClassRequestStore.STATUS_PAYMENT_PENDING,
                null,
                LocalDateTime.now().toString(),
                "{}",
                List.of(),
                null);

        when(authHelper.currentUserId()).thenReturn(otherClient.getUserId());
        when(userRepository.findById(otherClient.getUserId())).thenReturn(Optional.of(otherClient));
        when(classRequestStore.find("REQ-CFR-008")).thenReturn(Optional.of(data));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.cancelClassRequest("REQ-CFR-008"));

        assertEquals("Bạn không có quyền hủy yêu cầu này", exception.getMessage());
        verify(centerRequestFeeService, never()).cancelUnpaid(anyString());
    }

    private CreateClassTerminationRequest terminationRequest() {
        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");
        return request;
    }

    private CreateClassRequest createClassRequest() {
        CreateClassRequest request = new CreateClassRequest();
        request.setTitle("Cần gia sư Toán lớp 9");
        request.setDetailsJson("{\"subjectIds\":[\"101\"],\"slots\":[]}");
        request.setBudget(new BigDecimal("120000.00"));
        return request;
    }

    private ApplyClassRequest applyClassRequest() {
        ApplyClassRequest request = new ApplyClassRequest();
        request.setProposedRate(new BigDecimal("120000.00"));
        request.setCoverLetter("Em có kinh nghiệm dạy Toán THCS.");
        return request;
    }

    private ClassRequestCreateRequest classRequestCreateRequest() {
        ClassRequestCreateRequest request = new ClassRequestCreateRequest();
        request.setNote("Gia đình muốn tìm gia sư Toán lớp 9 học buổi tối.");
        request.setDesiredBudget(new BigDecimal("500000.00"));
        request.setRefundPayoutInfo(new com.tcs.module.finance.dto.RefundPayoutInfo(
                "TPBank",
                "0123456789",
                "Nguyen Thu Ha"));
        return request;
    }

    private CccdInfoDto completeCccd(String fullName, String cccdNumber) {
        return CccdInfoDto.builder()
                .fullName(fullName)
                .cccdNumber(cccdNumber)
                .dateOfBirth("01/01/2000")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build();
    }

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
    }

    private Client client(User user) {
        Client client = new Client();
        client.setClientId(user.getUserId());
        client.setUser(user);
        client.setFullName("Phụ huynh test");
        client.setPhone("0900000000");
        client.setDateOfBirth(LocalDate.of(1988, 1, 1));
        return client;
    }

    private Wallet activeWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(user.getUserId());
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private ClassAssignment pendingSignedAssignment(TutoringClass tutoringClass, User tutorUser) {
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(20));
        assignment.setTutorSignedAt(LocalDateTime.now().minusMinutes(10));
        return assignment;
    }

    private Contract privateContract(ClassAssignment assignment) {
        Contract contract = new Contract();
        contract.setContractId(880L);
        contract.setContractNo("BF08P-PRIVATE-001");
        contract.setAssignment(assignment);
        return contract;
    }

    private PaymentTransaction privateEscrowPayment(Long transactionId, String referenceCode) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setReferenceCode(referenceCode);
        payment.setAmount(new BigDecimal("500000.00"));
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.PENDING);
        return payment;
    }

    private TutoringClass tutoringClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle("Lớp toán");
        tutoringClass.setDescription("Lớp toán test");
        tutoringClass.setStatus(status);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.PRIVATE);
        return tutoringClass;
    }

    private TutoringClass centerClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = tutoringClass(creator, status);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.CENTER);
        tutoringClass.setMaxStudents(20);
        return tutoringClass;
    }

    private void stubSuccessfulCenterEnrollment(User clientUser, Client client, TutoringClass tutoringClass) {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndStudentEmail(CLASS_ID, clientUser.getEmail()))
                .thenReturn(false);
        when(clientLegalAccountService.resolveForClient(client)).thenReturn(
                ClientLegalAccountService.LegalAccountContext.builder()
                        .sessionUserId(CLIENT_USER_ID)
                        .legalUserId(CLIENT_USER_ID)
                        .legalHolderName(client.getFullName())
                        .legalHolderEmail(clientUser.getEmail())
                        .delegatedToParent(false)
                        .build());
        when(classStudentRepository.save(any(ClassStudent.class))).thenAnswer(invocation -> {
            ClassStudent saved = invocation.getArgument(0);
            saved.setClassStudentId(CLASS_STUDENT_ID);
            return saved;
        });
    }

    private Tutor tutor(User tutorUser) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(44L);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư test");
        return tutor;
    }

    private TutorApplication tutorApplication(TutoringClass tutoringClass, Tutor tutor) {
        TutorApplication application = new TutorApplication();
        application.setApplicationId(55L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        return application;
    }

    private ClassAssignment assignment(TutoringClass tutoringClass, User tutorUser) {
        Tutor tutor = tutor(tutorUser);
        TutorApplication application = tutorApplication(tutoringClass, tutor);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID);
        assignment.setTutor(tutor);
        assignment.setApplication(application);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
    }

    private ClassStudent classStudent(TutoringClass tutoringClass, User enrolledUser) {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(CLASS_STUDENT_ID);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(enrolledUser);
        classStudent.setStudentName("Học viên test");
        classStudent.setStudentEmail(enrolledUser.getEmail());
        classStudent.setStatus(ClassStudentStatus.ENROLLED);
        return classStudent;
    }

    private EscrowTransaction escrow(Long escrowId, BigDecimal amount) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }

    private void configurePrivateHourlyDeal(TutoringClass tutoringClass, BigDecimal hourlyRate) {
        tutoringClass.setTuitionFee(hourlyRate);
        tutoringClass.setDetailsJson("""
                {
                  "subjectIds": ["101"],
                  "subjectFees": {"101": "%s"},
                  "slots": [{"day": "T2", "start": "18:00", "end": "19:00", "subjectId": "101"}]
                }
                """.formatted(hourlyRate.toPlainString()));
    }

    private List<Lesson> lessons(TutoringClass tutoringClass, Tutor tutor, int total, int completed) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(19L);
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(19, 0));
        return java.util.stream.IntStream.rangeClosed(1, total)
                .mapToObj(sequence -> {
                    Lesson lesson = new Lesson();
                    lesson.setLessonId(1000L + sequence);
                    lesson.setTutoringClass(tutoringClass);
                    lesson.setTutor(tutor);
                    lesson.setSlot(slot);
                    lesson.setSequenceNo(sequence);
                    lesson.setLessonDate(LocalDate.now().minusDays(total - sequence));
                    lesson.setAttendanceStatus(sequence <= completed ? AttendanceStatus.COMPLETED : AttendanceStatus.PENDING);
                    return lesson;
                })
                .toList();
    }
}

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52F05CenterRequestFeePart3ITTest {

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
    void IT_CFR_018_KeepCenterRequestFeeOutsideClassEscrowTimeout() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        PaymentTransaction centerRequestFee = transaction(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("2500.00"),
                "CENTERREQ-ABC12345",
                now.minusMinutes(20));

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(centerRequestFee));

        int changed = reconciliationService.expirePendingEscrowDeposits(now);

        assertEquals(0, changed);
        assertEquals(PaymentTransactionStatus.PENDING, centerRequestFee.getStatus());
        verify(paymentTransactionRepository).saveAll(List.of());
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
