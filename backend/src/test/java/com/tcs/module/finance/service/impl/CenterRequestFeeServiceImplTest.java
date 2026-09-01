package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link CenterRequestFeeServiceImpl} — phi xu ly yeu cau tim gia su
 * ma phu huynh tra cho trung tam.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: cac sheet crfCreatePayment,
 * crfCompletePayment, crfReleaseForRequest, crfReleaseFulfilled, crfRequestRefund,
 * crfCancelUnpaid.</p>
 */
@ExtendWith(MockitoExtension.class)
class CenterRequestFeeServiceImplTest {

    private static final String REQUEST_ID = "REQ-001";
    private static final Long CLIENT_ID = 1L;
    private static final Long CENTER_ID = 2L;
    private static final Long HOLD_ID = 100L;

    @Mock private CenterRequestFeeHoldRepository feeHoldRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private WalletService walletService;
    @Mock private com.tcs.common.classrequest.ClassRequestStore classRequestStore;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;

    @InjectMocks private CenterRequestFeeServiceImpl service;

    // ------------------------------------------------------------------ helpers

    private RefundPayoutInfo payout() {
        return new RefundPayoutInfo("Vietcombank", "0123456789", "NGUYEN VAN A");
    }

    private CenterRequestFeeHold hold(CenterRequestFeeStatus status) {
        CenterRequestFeeHold h = new CenterRequestFeeHold();
        h.setFeeHoldId(HOLD_ID);
        h.setRequestId(REQUEST_ID);
        h.setClientUserId(CLIENT_ID);
        h.setCenterUserId(CENTER_ID);
        h.setAmount(new BigDecimal("20000"));
        h.setReferenceCode("CENTERREQ-ABCD1234");
        h.setStatus(status);
        return h;
    }

    private void givenFeeRate(String rate) {
        SystemParameter p = new SystemParameter();
        p.setParamKey("PLATFORM_FEE_RATE");
        p.setParamValue(rate);
        when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.of(p));
    }

    // ===================================================================
    //  Sheet: crfCreatePayment
    // ===================================================================
    @Nested
    @DisplayName("crfCreatePayment")
    class CrfCreatePayment {

        private void givenCreatePath() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
            when(walletService.getSystemEscrowWallet()).thenReturn(new Wallet());
            when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - du thong tin, chua co hold -> tao hold + PaymentTransaction PENDING")
        void utcid01_createSuccessfully() {
            givenCreatePath();
            givenFeeRate("0.10");

            ArgumentCaptor<CenterRequestFeeHold> holdCaptor = ArgumentCaptor.forClass(CenterRequestFeeHold.class);
            service.createPayment(REQUEST_ID, CLIENT_ID, CENTER_ID, "Trung tam ABC",
                    new BigDecimal("1000000"), payout());

            verify(feeHoldRepository).save(holdCaptor.capture());
            CenterRequestFeeHold saved = holdCaptor.getValue();
            assertEquals(CenterRequestFeeStatus.PENDING_PAYMENT, saved.getStatus());
            assertTrue(saved.getReferenceCode().startsWith("CENTERREQ-"));
            assertEquals(0, new BigDecimal("100000").compareTo(saved.getAmount()),
                    "phi = 1.000.000 x 10% = 100.000");
        }

        @Test
        @DisplayName("UTCID02 (N) - da co hold cho requestId -> tra ve hold cu, khong tao moi")
        void utcid02_idempotent() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold(CenterRequestFeeStatus.HELD)));

            service.createPayment(REQUEST_ID, CLIENT_ID, CENTER_ID, "Trung tam ABC",
                    new BigDecimal("1000000"), payout());

            verify(feeHoldRepository, never()).save(any(CenterRequestFeeHold.class));
            verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - requestId rong -> 'Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý'")
        void utcid03_blankRequestId() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createPayment("  ", CLIENT_ID, CENTER_ID, "C", new BigDecimal("1000"), payout()));
            assertEquals("Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - clientUserId = null -> cung thong bao thieu thong tin")
        void utcid04_nullClient() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createPayment(REQUEST_ID, null, CENTER_ID, "C", new BigDecimal("1000"), payout()));
            assertEquals("Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - centerUserId = null -> cung thong bao thieu thong tin")
        void utcid05_nullCenter() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createPayment(REQUEST_ID, CLIENT_ID, null, "C", new BigDecimal("1000"), payout()));
            assertEquals("Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (B) - projectedEscrowAmount = 0 -> phi duoc nang len 1")
        void utcid06_zeroBaseAmountRaisedToOne() {
            givenCreatePath();
            givenFeeRate("0.10");

            ArgumentCaptor<CenterRequestFeeHold> captor = ArgumentCaptor.forClass(CenterRequestFeeHold.class);
            service.createPayment(REQUEST_ID, CLIENT_ID, CENTER_ID, "C", BigDecimal.ZERO, payout());

            verify(feeHoldRepository).save(captor.capture());
            assertEquals(0, BigDecimal.ONE.compareTo(captor.getValue().getAmount()));
        }

        @Test
        @DisplayName("UTCID07 (B) - so tien nho khien phi lam tron ve 0 -> van nang len 1")
        void utcid07_roundedToZeroRaisedToOne() {
            givenCreatePath();
            givenFeeRate("0.10");

            ArgumentCaptor<CenterRequestFeeHold> captor = ArgumentCaptor.forClass(CenterRequestFeeHold.class);
            service.createPayment(REQUEST_ID, CLIENT_ID, CENTER_ID, "C", new BigDecimal("1"), payout());

            verify(feeHoldRepository).save(captor.capture());
            assertEquals(0, BigDecimal.ONE.compareTo(captor.getValue().getAmount()));
        }

        @Test
        @DisplayName("UTCID08 (N) - khong cau hinh PLATFORM_FEE_RATE -> dung ty le mac dinh 0.02")
        void utcid08_defaultFeeRate() {
            givenCreatePath();
            when(systemParameterRepository.findByParamKey(anyString())).thenReturn(Optional.empty());

            ArgumentCaptor<CenterRequestFeeHold> captor = ArgumentCaptor.forClass(CenterRequestFeeHold.class);
            service.createPayment(REQUEST_ID, CLIENT_ID, CENTER_ID, "C", new BigDecimal("1000000"), payout());

            verify(feeHoldRepository).save(captor.capture());
            assertEquals(0, new BigDecimal("20000").compareTo(captor.getValue().getAmount()),
                    "1.000.000 x 2% = 20.000");
        }

        @Test
        @DisplayName("UTCID09 (A) - thieu thong tin nhan tien -> 'Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản'")
        void utcid09_incompletePayoutInfo() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createPayment(REQUEST_ID, CLIENT_ID, CENTER_ID, "C",
                            new BigDecimal("1000000"), new RefundPayoutInfo("Vietcombank", null, "A")));
            assertEquals("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: crfCompletePayment
    // ===================================================================
    @Nested
    @DisplayName("crfCompletePayment")
    class CrfCompletePayment {

        private PaymentTransaction tx(PaymentTransactionStatus status) {
            PaymentTransaction t = new PaymentTransaction();
            t.setTransactionId(55L);
            t.setStatus(status);
            return t;
        }

        @Test
        @DisplayName("UTCID01 (N) - payment PENDING + hold PENDING_PAYMENT -> payment SUCCESS, hold HELD")
        void utcid01_completeSuccessfully() {
            PaymentTransaction t = tx(PaymentTransactionStatus.PENDING);
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.PENDING_PAYMENT);
            when(feeHoldRepository.findByPaymentTransaction_TransactionId(55L)).thenReturn(Optional.of(h));
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

            service.completeIncomingPayment(t, "SEPAY-999");

            assertEquals(PaymentTransactionStatus.SUCCESS, t.getStatus());
            assertEquals("SEPAY-999", t.getExternalTransactionId());
            assertEquals(CenterRequestFeeStatus.HELD, h.getStatus());
            assertTrue(h.getPaidAt() != null);
            verify(paymentTransactionRepository).save(t);
            verify(feeHoldRepository).save(h);
        }

        @Test
        @DisplayName("UTCID02 (N) - payment SUCCESS + hold HELD (webhook lap) -> tra ve nguyen trang")
        void utcid02_idempotentRetry() {
            PaymentTransaction t = tx(PaymentTransactionStatus.SUCCESS);
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.HELD);
            when(feeHoldRepository.findByPaymentTransaction_TransactionId(55L)).thenReturn(Optional.of(h));

            service.completeIncomingPayment(t, "SEPAY-999");

            verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
            verify(feeHoldRepository, never()).save(any(CenterRequestFeeHold.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - khong tim thay hold theo transactionId -> 'Không tìm thấy phí xử lý yêu cầu trung tâm'")
        void utcid03_holdNotFound() {
            PaymentTransaction t = tx(PaymentTransactionStatus.PENDING);
            when(feeHoldRepository.findByPaymentTransaction_TransactionId(55L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.completeIncomingPayment(t, "SEPAY-999"));
            assertEquals("Không tìm thấy phí xử lý yêu cầu trung tâm", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - payment da CANCELLED -> 'Giao dịch phí trung tâm không còn ở trạng thái chờ thanh toán'")
        void utcid04_paymentNotPending() {
            PaymentTransaction t = tx(PaymentTransactionStatus.CANCELLED);
            when(feeHoldRepository.findByPaymentTransaction_TransactionId(55L))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.PENDING_PAYMENT)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeIncomingPayment(t, "SEPAY-999"));
            assertEquals("Giao dịch phí trung tâm không còn ở trạng thái chờ thanh toán", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - hold khong con PENDING_PAYMENT -> cung thong bao tren")
        void utcid05_holdNotPendingPayment() {
            PaymentTransaction t = tx(PaymentTransactionStatus.PENDING);
            when(feeHoldRepository.findByPaymentTransaction_TransactionId(55L))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.RELEASED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeIncomingPayment(t, "SEPAY-999"));
            assertEquals("Giao dịch phí trung tâm không còn ở trạng thái chờ thanh toán", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: crfReleaseForRequest
    // ===================================================================
    @Nested
    @DisplayName("crfReleaseForRequest")
    class CrfReleaseForRequest {

        private void givenReleasePath() {
            when(walletService.getOrCreate(CENTER_ID)).thenReturn(new Wallet());
            when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - hold dang HELD -> cong tien vao vi trung tam, hold ve RELEASED")
        void utcid01_releaseSuccessfully() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.HELD);
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(h));
            givenReleasePath();

            service.releaseForRequest(REQUEST_ID, "Da ghep gia su");

            assertEquals(CenterRequestFeeStatus.RELEASED, h.getStatus());
            assertTrue(h.getReleasedAt() != null);
            verify(walletService).credit(CENTER_ID, h.getAmount(), "CENTERREQ_RELEASE-" + HOLD_ID);
        }

        @Test
        @DisplayName("UTCID02 (B) - hold da RELEASED -> tra ve lang le, khong giai ngan lan hai")
        void utcid02_alreadyReleased() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.RELEASED)));

            assertDoesNotThrow(() -> service.releaseForRequest(REQUEST_ID, "x"));
            verify(walletService, never()).credit(anyLong(), any(BigDecimal.class), anyString());
        }

        @Test
        @DisplayName("UTCID03 (B) - hold da REFUNDED -> tra ve lang le")
        void utcid03_alreadyRefunded() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.REFUNDED)));

            assertDoesNotThrow(() -> service.releaseForRequest(REQUEST_ID, "x"));
            verify(walletService, never()).credit(anyLong(), any(BigDecimal.class), anyString());
        }

        @Test
        @DisplayName("UTCID04 (A) - hold dang PENDING_PAYMENT -> 'Phí xử lý yêu cầu chưa sẵn sàng để giải ngân'")
        void utcid04_pendingPayment() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.PENDING_PAYMENT)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.releaseForRequest(REQUEST_ID, "x"));
            assertEquals("Phí xử lý yêu cầu chưa sẵn sàng để giải ngân", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - hold da CANCELLED -> cung thong bao tren")
        void utcid05_cancelled() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.CANCELLED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.releaseForRequest(REQUEST_ID, "x"));
            assertEquals("Phí xử lý yêu cầu chưa sẵn sàng để giải ngân", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - khong tim thay hold -> 'Không tìm thấy phí xử lý yêu cầu trung tâm'")
        void utcid06_holdNotFound() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.releaseForRequest(REQUEST_ID, "x"));
            assertEquals("Không tìm thấy phí xử lý yêu cầu trung tâm", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: crfReleaseFulfilled
    // ===================================================================
    @Nested
    @DisplayName("crfReleaseFulfilled")
    class CrfReleaseFulfilled {

        @Test
        @DisplayName("UTCID01 (N) - co hold HELD theo assignmentId -> giai ngan cho trung tam")
        void utcid01_releaseSuccessfully() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.HELD);
            when(feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(9L)).thenReturn(Optional.of(h));
            when(walletService.getOrCreate(CENTER_ID)).thenReturn(new Wallet());
            when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));

            service.releaseForFulfilledAssignment(9L, "Hoan thanh phan cong");

            assertEquals(CenterRequestFeeStatus.RELEASED, h.getStatus());
        }

        @Test
        @DisplayName("UTCID02 (B) - khong co hold (lop khong tu yeu cau trung tam) -> tra ve lang le, KHONG nem")
        void utcid02_noHoldReturnsSilently() {
            when(feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(9L)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.releaseForFulfilledAssignment(9L, "x"));
            verify(walletService, never()).credit(anyLong(), any(BigDecimal.class), anyString());
        }

        @Test
        @DisplayName("UTCID03 (B) - hold khong o trang thai HELD -> tra ve lang le")
        void utcid03_holdNotHeld() {
            when(feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(9L))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.RELEASED)));

            assertDoesNotThrow(() -> service.releaseForFulfilledAssignment(9L, "x"));
            verify(walletService, never()).credit(anyLong(), any(BigDecimal.class), anyString());
        }

        @Test
        @DisplayName("UTCID04 (B) - assignmentId = null -> khong tim thay hold, tra ve lang le")
        void utcid04_nullAssignmentId() {
            when(feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(null)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.releaseForFulfilledAssignment(null, "x"));
        }
    }

    // ===================================================================
    //  Sheet: crfRequestRefund
    // ===================================================================
    @Nested
    @DisplayName("crfRequestRefund")
    class CrfRequestRefund {

        @Test
        @DisplayName("UTCID01 (N) - hold HELD, chua co yeu cau hoan PENDING -> tao RefundRequest")
        void utcid01_createRefundRequest() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.HELD);
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(h));
            when(refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(HOLD_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(CENTER_ID)).thenReturn(Optional.of(new User()));
            when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));

            service.requestRefund(REQUEST_ID, "Trung tam khong tim duoc gia su");

            verify(refundRequestRepository).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("UTCID02 (N) - hold PENDING_PAYMENT -> chuyen sang huy phi chua thanh toan")
        void utcid02_delegatesToCancelUnpaid() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.PENDING_PAYMENT);
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(h));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));

            service.requestRefund(REQUEST_ID, "x");

            assertEquals(CenterRequestFeeStatus.CANCELLED, h.getStatus());
            verify(refundRequestRepository, never()).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("UTCID03 (B) - hold da RELEASED -> tra ve lang le")
        void utcid03_alreadyReleased() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.RELEASED)));

            assertDoesNotThrow(() -> service.requestRefund(REQUEST_ID, "x"));
            verify(refundRequestRepository, never()).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("UTCID04 (B) - hold da REFUNDED -> tra ve lang le")
        void utcid04_alreadyRefunded() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.REFUNDED)));

            assertDoesNotThrow(() -> service.requestRefund(REQUEST_ID, "x"));
            verify(refundRequestRepository, never()).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("UTCID05 (B) - da co RefundRequest PENDING -> khong tao trung")
        void utcid05_pendingRefundAlreadyExists() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.HELD)));
            RefundRequest pending = new RefundRequest();
            pending.setStatus(RefundRequestStatus.PENDING);
            when(refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(HOLD_ID))
                    .thenReturn(Optional.of(pending));

            assertDoesNotThrow(() -> service.requestRefund(REQUEST_ID, "x"));
            verify(refundRequestRepository, never()).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("UTCID06 (A) - khong tim thay hold -> 'Không tìm thấy phí xử lý yêu cầu trung tâm'")
        void utcid06_holdNotFound() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.requestRefund(REQUEST_ID, "x"));
            assertEquals("Không tìm thấy phí xử lý yêu cầu trung tâm", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: crfCancelUnpaid
    // ===================================================================
    @Nested
    @DisplayName("crfCancelUnpaid")
    class CrfCancelUnpaid {

        @Test
        @DisplayName("UTCID01 (N) - hold PENDING_PAYMENT + payment PENDING -> huy ca hai, xoa request khoi store")
        void utcid01_cancelSuccessfully() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.PENDING_PAYMENT);
            PaymentTransaction t = new PaymentTransaction();
            t.setTransactionId(55L);
            t.setStatus(PaymentTransactionStatus.PENDING);
            h.setPaymentTransaction(t);
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(h));
            when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));

            service.cancelUnpaid(REQUEST_ID);

            assertEquals(PaymentTransactionStatus.CANCELLED, t.getStatus());
            assertTrue(t.getProcessedAt() != null);
            assertEquals(CenterRequestFeeStatus.CANCELLED, h.getStatus());
            verify(classRequestStore).delete(REQUEST_ID);
        }

        @Test
        @DisplayName("UTCID02 (B) - hold PENDING_PAYMENT nhung khong co payment -> van huy hold")
        void utcid02_noPaymentTransaction() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.PENDING_PAYMENT);
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(h));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));

            service.cancelUnpaid(REQUEST_ID);

            assertEquals(CenterRequestFeeStatus.CANCELLED, h.getStatus());
            verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
            verify(classRequestStore).delete(REQUEST_ID);
        }

        @Test
        @DisplayName("UTCID03 (B) - payment da CANCELLED -> khong dung toi payment, van huy hold")
        void utcid03_paymentAlreadyCancelled() {
            CenterRequestFeeHold h = hold(CenterRequestFeeStatus.PENDING_PAYMENT);
            PaymentTransaction t = new PaymentTransaction();
            t.setStatus(PaymentTransactionStatus.CANCELLED);
            h.setPaymentTransaction(t);
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(h));
            when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(i -> i.getArgument(0));

            service.cancelUnpaid(REQUEST_ID);

            assertEquals(CenterRequestFeeStatus.CANCELLED, h.getStatus());
            verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("UTCID04 (A) - hold dang HELD -> 'Chỉ có thể hủy khi phí trung tâm chưa thanh toán'")
        void utcid04_holdAlreadyHeld() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.HELD)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelUnpaid(REQUEST_ID));
            assertEquals("Chỉ có thể hủy khi phí trung tâm chưa thanh toán", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - hold da RELEASED -> cung thong bao tren")
        void utcid05_holdReleased() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID))
                    .thenReturn(Optional.of(hold(CenterRequestFeeStatus.RELEASED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelUnpaid(REQUEST_ID));
            assertEquals("Chỉ có thể hủy khi phí trung tâm chưa thanh toán", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - khong tim thay hold -> 'Không tìm thấy phí xử lý yêu cầu trung tâm'")
        void utcid06_holdNotFound() {
            when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.cancelUnpaid(REQUEST_ID));
            assertEquals("Không tìm thấy phí xử lý yêu cầu trung tâm", ex.getMessage());
        }
    }
}
