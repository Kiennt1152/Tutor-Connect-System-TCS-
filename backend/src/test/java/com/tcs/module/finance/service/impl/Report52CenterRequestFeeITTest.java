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
