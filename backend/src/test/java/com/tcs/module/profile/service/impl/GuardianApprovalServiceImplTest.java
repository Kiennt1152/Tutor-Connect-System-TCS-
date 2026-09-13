package com.tcs.module.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.profile.dto.response.GuardianApprovalResponse;
import com.tcs.module.profile.enums.GuardianApprovalActionType;
import com.tcs.module.profile.enums.GuardianApprovalStatus;
import com.tcs.module.profile.service.ClientLegalAccountService.LegalAccountContext;
import com.tcs.module.profile.support.GuardianApprovalPayloadCodec;
import com.tcs.module.profile.support.GuardianApprovalPayloadCodec.Payload;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test module Profile — luong phu huynh xac nhan thay cho hoc sinh vi thanh nien.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: cac sheet gaSubmitDeposit, gaSubmitContract,
 * gaApprove va gaReject.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuardianApprovalServiceImplTest {

    private static final Long PARENT_USER_ID = 100L;
    private static final Long MINOR_USER_ID = 101L;
    private static final Long PARENT_NOTIFICATION_ID = 900L;
    private static final Long MINOR_NOTIFICATION_ID = 901L;
    private static final Long TRANSACTION_ID = 500L;

    @Mock private AuthHelper authHelper;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks private GuardianApprovalServiceImpl service;

    /** Codec that (khong mock) de payload JSON duoc ma hoa / giai ma dung nhu chay that. */
    private final GuardianApprovalPayloadCodec codec = new GuardianApprovalPayloadCodec();

    private User parentUser;
    private User minorUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "payloadCodec", codec);

        parentUser = new User();
        parentUser.setUserId(PARENT_USER_ID);
        parentUser.setEmail("phuhuynh@tcs.vn");

        minorUser = new User();
        minorUser.setUserId(MINOR_USER_ID);
        minorUser.setEmail("hocsinh@tcs.vn");

        when(userRepository.findById(PARENT_USER_ID)).thenReturn(Optional.of(parentUser));
        when(userRepository.findById(MINOR_USER_ID)).thenReturn(Optional.of(minorUser));
        when(authHelper.currentUserId()).thenReturn(PARENT_USER_ID);
    }

    private LegalAccountContext legalContext() {
        return LegalAccountContext.builder()
                .sessionUserId(MINOR_USER_ID)
                .legalUserId(PARENT_USER_ID)
                .legalHolderName("Nguyen Van Bo")
                .legalHolderEmail("phuhuynh@tcs.vn")
                .delegatedToParent(true)
                .beneficiaryMinorUserId(MINOR_USER_ID)
                .beneficiaryMinorName("Nguyen Van Con")
                .build();
    }

    /** notificationRepository.save gan id: ban dau cho phu huynh, sau do cho hoc sinh. */
    private void givenNotificationIdsAssignedInOrder() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getNotificationId() == null) {
                n.setNotificationId(GuardianApprovalPayloadCodec.REF_PARENT.equals(n.getReferenceType())
                        ? PARENT_NOTIFICATION_ID
                        : MINOR_NOTIFICATION_ID);
            }
            return n;
        });
    }

    private PaymentTransaction pendingTopup(String description) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(MINOR_USER_ID);
        wallet.setAvailableBalance(new BigDecimal("100000.00"));

        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(TRANSACTION_ID);
        tx.setWallet(wallet);
        tx.setAmount(new BigDecimal("500000.00"));
        tx.setDescription(description);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        return tx;
    }

    /** Thong bao phia phu huynh mang payload dang cho xu ly. */
    private Notification parentNotification(Payload payload) {
        Notification n = new Notification();
        n.setNotificationId(PARENT_NOTIFICATION_ID);
        n.setUser(parentUser);
        n.setReferenceType(GuardianApprovalPayloadCodec.REF_PARENT);
        n.setContent(codec.encode(payload));
        n.setIsRead(false);
        return n;
    }

    private Payload pendingPayload(GuardianApprovalActionType actionType, Long parentNotificationId) {
        return Payload.builder()
                .actionType(actionType)
                .status(GuardianApprovalStatus.PENDING)
                .minorUserId(MINOR_USER_ID)
                .minorName("Nguyen Van Con")
                .parentUserId(PARENT_USER_ID)
                .parentName("Nguyen Van Bo")
                .amount(new BigDecimal("500000.00"))
                .description("Nap tien vao vi")
                .tutorName("Gia su 1")
                .paymentTransactionId(
                        actionType == GuardianApprovalActionType.DEPOSIT ? TRANSACTION_ID : null)
                .parentNotificationId(parentNotificationId)
                .build();
    }

    // ===================================================================
    //  Sheet: gaSubmitDeposit
    // ===================================================================
    @Nested
    @DisplayName("gaSubmitDeposit")
    class GaSubmitDeposit {

        @Test
        @DisplayName("UTCID01 (N) - Boi canh phap ly hop le -> tao thong bao PAYMENT cho phu huynh va hoc sinh")
        void utcid01_submitSuccessfully() {
            givenNotificationIdsAssignedInOrder();

            GuardianApprovalResponse response =
                    service.submitDepositApproval(legalContext(), pendingTopup("Nap tien vao vi"));

            assertEquals(GuardianApprovalActionType.DEPOSIT, response.getActionType());
            assertEquals(GuardianApprovalStatus.PENDING, response.getStatus());
            assertEquals(new BigDecimal("500000.00"), response.getAmount());
            assertEquals(TRANSACTION_ID, response.getPaymentTransactionId());

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            // 3 lan luu: tao thong bao phu huynh, cap nhat payload, tao thong bao hoc sinh.
            verify(notificationRepository, org.mockito.Mockito.times(3)).save(captor.capture());
            Notification minorNotification = captor.getAllValues().get(2);
            assertEquals(GuardianApprovalPayloadCodec.REF_MINOR, minorNotification.getReferenceType());
            assertEquals(PARENT_NOTIFICATION_ID, minorNotification.getReferenceId(),
                    "Thong bao cua hoc sinh phai tro ve thong bao cua phu huynh");
            assertEquals(PARENT_NOTIFICATION_ID,
                    codec.decode(minorNotification.getContent()).getParentNotificationId());
            verify(notificationDispatchService).notifyUserByEmail(
                    org.mockito.ArgumentMatchers.eq(parentUser),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("UTCID02 (A) - legalUserId khong khop tai khoan nao -> 'Không tìm thấy người dùng'")
        void utcid02_parentUserNotFound() {
            when(userRepository.findById(PARENT_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.submitDepositApproval(legalContext(), pendingTopup("Nap tien")));
            assertEquals("Không tìm thấy người dùng", ex.getMessage());
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - sessionUserId khong khop tai khoan nao -> 'Không tìm thấy người dùng'")
        void utcid03_minorUserNotFound() {
            when(userRepository.findById(MINOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.submitDepositApproval(legalContext(), pendingTopup("Nap tien")));
            assertEquals("Không tìm thấy người dùng", ex.getMessage());
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (B) - Giao dich khong co description -> van tao duoc yeu cau, description = null")
        void utcid04_transactionWithoutDescription() {
            givenNotificationIdsAssignedInOrder();

            GuardianApprovalResponse response =
                    service.submitDepositApproval(legalContext(), pendingTopup(null));

            assertNull(response.getDescription(), "description cua giao dich rong thi payload cung rong");
            assertEquals(GuardianApprovalStatus.PENDING, response.getStatus());
            assertEquals(TRANSACTION_ID, response.getPaymentTransactionId());
        }
    }

    // ===================================================================
    //  Sheet: gaSubmitContract
    // ===================================================================
    @Nested
    @DisplayName("gaSubmitContract")
    class GaSubmitContract {

        @Test
        @DisplayName("UTCID01 (N) - Boi canh phap ly hop le -> tao yeu cau CONTRACT_SIGN dang PENDING")
        void utcid01_submitSuccessfully() {
            givenNotificationIdsAssignedInOrder();

            GuardianApprovalResponse response = service.submitContractApproval(
                    legalContext(), "Gia su 1", "Toan", "HD-2026-001");

            assertEquals(GuardianApprovalActionType.CONTRACT_SIGN, response.getActionType());
            assertEquals(GuardianApprovalStatus.PENDING, response.getStatus());
            assertEquals("HD-2026-001", response.getContractReference());
            assertTrue(response.getDescription().contains("Gia su 1"),
                    "Mo ta phai neu ten gia su: " + response.getDescription());
            assertTrue(response.getDescription().contains("Toan"),
                    "Mo ta phai neu mon hoc: " + response.getDescription());
            assertTrue(response.getDescription().contains("Nguyen Van Con"),
                    "Mo ta phai neu hoc sinh duoc dai dien: " + response.getDescription());

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, org.mockito.Mockito.times(3)).save(captor.capture());
            assertNull(captor.getAllValues().get(0).getReferenceId(),
                    "Yeu cau ky hop dong khong gan giao dich thanh toan");
        }

        @Test
        @DisplayName("UTCID02 (A) - legalUserId khong khop tai khoan nao -> 'Không tìm thấy người dùng'")
        void utcid02_parentUserNotFound() {
            when(userRepository.findById(PARENT_USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.submitContractApproval(
                    legalContext(), "Gia su 1", "Toan", "HD-2026-001"));
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - sessionUserId khong khop tai khoan nao -> 'Không tìm thấy người dùng'")
        void utcid03_minorUserNotFound() {
            when(userRepository.findById(MINOR_USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.submitContractApproval(
                    legalContext(), "Gia su 1", "Toan", "HD-2026-001"));
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (B) - tutorName va subjectName null -> van tao duoc yeu cau")
        void utcid04_missingTutorAndSubject() {
            givenNotificationIdsAssignedInOrder();

            GuardianApprovalResponse response =
                    service.submitContractApproval(legalContext(), null, null, "HD-2026-002");

            assertEquals(GuardianApprovalActionType.CONTRACT_SIGN, response.getActionType());
            assertEquals(GuardianApprovalStatus.PENDING, response.getStatus());
            assertNotNull(response.getDescription());
        }
    }

    // ===================================================================
    //  Sheet: gaApprove
    // ===================================================================
    @Nested
    @DisplayName("gaApprove")
    class GaApprove {

        private void givenParentNotification(Payload payload) {
            when(notificationRepository.findByNotificationIdAndUser_UserId(
                    PARENT_NOTIFICATION_ID, PARENT_USER_ID))
                    .thenReturn(Optional.of(parentNotification(payload)));
            when(notificationRepository.findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                    MINOR_USER_ID, GuardianApprovalPayloadCodec.REF_MINOR)).thenReturn(List.of());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - Yeu cau DEPOSIT dang PENDING -> cong tien vao vi va chuyen APPROVED")
        void utcid01_approveDeposit() {
            givenParentNotification(pendingPayload(GuardianApprovalActionType.DEPOSIT, PARENT_NOTIFICATION_ID));
            PaymentTransaction tx = pendingTopup("Nap tien vao vi");
            when(paymentTransactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(tx));

            GuardianApprovalResponse response = service.approve(PARENT_NOTIFICATION_ID);

            assertEquals(GuardianApprovalStatus.APPROVED, response.getStatus());
            assertNotNull(response.getResolvedAt());
            assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
            assertEquals(new BigDecimal("600000.00"), tx.getWallet().getAvailableBalance());
            verify(walletRepository).save(tx.getWallet());
        }

        @Test
        @DisplayName("UTCID02 (N) - Yeu cau CONTRACT_SIGN dang PENDING -> chuyen APPROVED, khong dong toi giao dich")
        void utcid02_approveContractSign() {
            givenParentNotification(
                    pendingPayload(GuardianApprovalActionType.CONTRACT_SIGN, PARENT_NOTIFICATION_ID));

            GuardianApprovalResponse response = service.approve(PARENT_NOTIFICATION_ID);

            assertEquals(GuardianApprovalStatus.APPROVED, response.getStatus());
            verify(paymentTransactionRepository, never()).save(any());
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Thong bao thuoc nguoi dung khac -> ForbiddenException")
        void utcid03_notificationOfAnotherUser() {
            when(notificationRepository.findByNotificationIdAndUser_UserId(
                    PARENT_NOTIFICATION_ID, PARENT_USER_ID)).thenReturn(Optional.empty());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.approve(PARENT_NOTIFICATION_ID));
            assertEquals("Không tìm thấy yêu cầu chờ xác nhận hoặc bạn không có quyền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - approvalId khong khop thong bao nao -> ForbiddenException")
        void utcid04_approvalNotFound() {
            when(notificationRepository.findByNotificationIdAndUser_UserId(
                    999L, PARENT_USER_ID)).thenReturn(Optional.empty());

            assertThrows(ForbiddenException.class, () -> service.approve(999L));
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau da duoc xu ly truoc do -> 'Yêu cầu đã được xử lý trước đó'")
        void utcid05_alreadyResolved() {
            Payload resolved = pendingPayload(GuardianApprovalActionType.DEPOSIT, PARENT_NOTIFICATION_ID);
            resolved.setStatus(GuardianApprovalStatus.APPROVED);
            givenParentNotification(resolved);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.approve(PARENT_NOTIFICATION_ID));
            assertEquals("Yêu cầu đã được xử lý trước đó", ex.getMessage());
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID06 (B) - payload khong luu parentNotificationId -> bo qua buoc dong bo thong bao hoc sinh")
        void utcid06_withoutParentNotificationId() {
            givenParentNotification(pendingPayload(GuardianApprovalActionType.CONTRACT_SIGN, null));

            GuardianApprovalResponse response = service.approve(PARENT_NOTIFICATION_ID);

            assertEquals(GuardianApprovalStatus.APPROVED, response.getStatus());
            verify(notificationRepository, never()).findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                    MINOR_USER_ID, GuardianApprovalPayloadCodec.REF_MINOR);
        }
    }

    // ===================================================================
    //  Sheet: gaReject
    // ===================================================================
    @Nested
    @DisplayName("gaReject")
    class GaReject {

        private void givenParentNotification(Payload payload) {
            when(notificationRepository.findByNotificationIdAndUser_UserId(
                    PARENT_NOTIFICATION_ID, PARENT_USER_ID))
                    .thenReturn(Optional.of(parentNotification(payload)));
            when(notificationRepository.findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                    MINOR_USER_ID, GuardianApprovalPayloadCodec.REF_MINOR)).thenReturn(List.of());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - Tu choi yeu cau DEPOSIT -> huy giao dich, khong cong tien vao vi")
        void utcid01_rejectDeposit() {
            givenParentNotification(pendingPayload(GuardianApprovalActionType.DEPOSIT, PARENT_NOTIFICATION_ID));
            PaymentTransaction tx = pendingTopup("Nap tien vao vi");
            when(paymentTransactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(tx));

            GuardianApprovalResponse response = service.reject(PARENT_NOTIFICATION_ID);

            assertEquals(GuardianApprovalStatus.REJECTED, response.getStatus());
            assertEquals(PaymentTransactionStatus.CANCELLED, tx.getStatus());
            assertEquals("Phụ huynh từ chối xác nhận", tx.getFailureReason());
            assertEquals(new BigDecimal("100000.00"), tx.getWallet().getAvailableBalance(),
                    "Bi tu choi thi so du vi phai giu nguyen");
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Tu choi yeu cau CONTRACT_SIGN -> chuyen REJECTED")
        void utcid02_rejectContractSign() {
            givenParentNotification(
                    pendingPayload(GuardianApprovalActionType.CONTRACT_SIGN, PARENT_NOTIFICATION_ID));

            GuardianApprovalResponse response = service.reject(PARENT_NOTIFICATION_ID);

            assertEquals(GuardianApprovalStatus.REJECTED, response.getStatus());
            assertNotNull(response.getResolvedAt());
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Thong bao thuoc nguoi dung khac -> ForbiddenException")
        void utcid03_notificationOfAnotherUser() {
            when(notificationRepository.findByNotificationIdAndUser_UserId(
                    PARENT_NOTIFICATION_ID, PARENT_USER_ID)).thenReturn(Optional.empty());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.reject(PARENT_NOTIFICATION_ID));
            assertEquals("Không tìm thấy yêu cầu chờ xác nhận hoặc bạn không có quyền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - approvalId khong khop thong bao nao -> ForbiddenException")
        void utcid04_approvalNotFound() {
            when(notificationRepository.findByNotificationIdAndUser_UserId(
                    999L, PARENT_USER_ID)).thenReturn(Optional.empty());

            assertThrows(ForbiddenException.class, () -> service.reject(999L));
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau da duoc xu ly truoc do -> 'Yêu cầu đã được xử lý trước đó'")
        void utcid05_alreadyResolved() {
            Payload resolved = pendingPayload(GuardianApprovalActionType.DEPOSIT, PARENT_NOTIFICATION_ID);
            resolved.setStatus(GuardianApprovalStatus.REJECTED);
            givenParentNotification(resolved);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reject(PARENT_NOTIFICATION_ID));
            assertEquals("Yêu cầu đã được xử lý trước đó", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (B) - payload khong luu parentNotificationId -> bo qua buoc dong bo thong bao hoc sinh")
        void utcid06_withoutParentNotificationId() {
            givenParentNotification(pendingPayload(GuardianApprovalActionType.CONTRACT_SIGN, null));

            GuardianApprovalResponse response = service.reject(PARENT_NOTIFICATION_ID);

            assertEquals(GuardianApprovalStatus.REJECTED, response.getStatus());
            verify(notificationRepository, never()).findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                    MINOR_USER_ID, GuardianApprovalPayloadCodec.REF_MINOR);
        }
    }
}
