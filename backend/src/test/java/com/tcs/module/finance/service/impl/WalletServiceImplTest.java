package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.TopupSession;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.JournalEntryType;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.FinancialJournalRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.PaymentGateway;
import com.tcs.module.identity.entity.User;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private com.tcs.module.identity.repository.UserRepository userRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private FinancialJournalRepository financialJournalRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    @Captor
    private ArgumentCaptor<com.tcs.module.finance.entity.FinancialJournal> journalCaptor;

    private static final Long USER_ID = 1L;
    private Wallet activeWallet;

    @BeforeEach
    void setUp() {
        activeWallet = new Wallet();
        activeWallet.setWalletId(USER_ID);
        activeWallet.setAvailableBalance(new BigDecimal("100000.00"));
        activeWallet.setFrozenBalance(BigDecimal.ZERO);
        activeWallet.setStatus(WalletStatus.ACTIVE);
    }

    // ─── getOrCreate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        /** Ngoài phạm vi Report 5.1 (MethodList không có getOrCreate) - test bổ sung */
        @Test
        @DisplayName("returns existing wallet when found")
        void returnsExistingWallet() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            Wallet result = walletService.getOrCreate(USER_ID);

            assertEquals(activeWallet, result);
            verify(walletRepository, never()).save(any());
        }

        /** Ngoài phạm vi Report 5.1 (MethodList không có getOrCreate) - test bổ sung */
        @Test
        @DisplayName("creates new wallet when not found")
        void createsNewWallet() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            User user = new User();
            user.setUserId(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
                Wallet w = inv.getArgument(0);
                w.setWalletId(USER_ID);
                return w;
            });

            Wallet result = walletService.getOrCreate(USER_ID);

            verify(walletRepository).save(walletCaptor.capture());
            Wallet saved = walletCaptor.getValue();
            assertEquals(BigDecimal.ZERO, saved.getAvailableBalance());
            assertEquals(BigDecimal.ZERO, saved.getFrozenBalance());
            assertEquals(WalletStatus.ACTIVE, saved.getStatus());
            assertEquals(USER_ID, saved.getWalletId());
        }
    }

    // ─── balance ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        /** Ngoài phạm vi Report 5.1 (MethodList không có create) - test bổ sung */
        @Test
        @DisplayName("returns existing wallet when user already has one")
        void returnsExistingWallet() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            Wallet result = walletService.create(USER_ID);

            assertEquals(activeWallet, result);
            verify(walletRepository, never()).save(any());
        }

        /** Ngoài phạm vi Report 5.1 (MethodList không có create) - test bổ sung */
        @Test
        @DisplayName("creates active zero-balance wallet when missing")
        void createsWalletWhenMissing() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
            User user = new User();
            user.setUserId(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.create(USER_ID);

            verify(walletRepository).save(walletCaptor.capture());
            Wallet saved = walletCaptor.getValue();
            assertEquals(saved, result);
            assertEquals(USER_ID, saved.getWalletId());
            assertEquals(user, saved.getUser());
            assertEquals(BigDecimal.ZERO, saved.getAvailableBalance());
            assertEquals(BigDecimal.ZERO, saved.getFrozenBalance());
            assertEquals(WalletStatus.ACTIVE, saved.getStatus());
        }
    }

    @Nested
    @DisplayName("balance")
    class Balance {

        /** Ngoài phạm vi Report 5.1 (MethodList không có balance) - test bổ sung */
        @Test
        @DisplayName("returns available balance when wallet exists")
        void returnsBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            BigDecimal result = walletService.balance(USER_ID);

            assertEquals(new BigDecimal("100000.00"), result);
        }

        /** Ngoài phạm vi Report 5.1 (MethodList không có balance) - test bổ sung */
        @Test
        @DisplayName("returns ZERO when wallet not found")
        void returnsZeroWhenNotFound() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            BigDecimal result = walletService.balance(USER_ID);

            assertEquals(BigDecimal.ZERO, result);
        }
    }

    // ─── credit ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("walletCredit")
    class Credit {

        /** Sheet walletCredit - UTCID01 (N): ví ACTIVE, amount > 0 -> tăng số dư khả dụng và ghi FinancialJournal CREDIT */
        @Test
        @DisplayName("increases available balance and writes journal")
        void creditIncreasesBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.credit(USER_ID, new BigDecimal("50000.00"), "TOPUP-123");

            verify(walletRepository).save(walletCaptor.capture());
            Wallet saved = walletCaptor.getValue();
            assertEquals(new BigDecimal("150000.00"), saved.getAvailableBalance());

            verify(financialJournalRepository).save(journalCaptor.capture());
            var journal = journalCaptor.getValue();
            assertEquals(JournalEntryType.CREDIT, journal.getEntryType());
            assertEquals(new BigDecimal("50000.00"), journal.getAmount());
            assertEquals(new BigDecimal("100000.00"), journal.getBalanceBefore());
            assertEquals(new BigDecimal("150000.00"), journal.getBalanceAfter());
        }

        /** Sheet walletCredit - UTCID02 (N): chưa có ví -> getOrCreate tạo ví mới rồi ghi có */
        @Test
        @DisplayName("creates wallet on credit if not exists")
        void creditCreatesWalletIfMissing() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
            User user = new User();
            user.setUserId(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.credit(USER_ID, new BigDecimal("200000.00"), "TOPUP-456");

            verify(walletRepository, atLeast(2)).save(walletCaptor.capture());
            Wallet lastSaved = walletCaptor.getAllValues().get(walletCaptor.getAllValues().size() - 1);
            assertEquals(new BigDecimal("200000.00"), lastSaved.getAvailableBalance());
        }

        /** Sheet walletCredit - UTCID03 (B): amount = 0 -> 'Số tiền ghi có phải lớn hơn 0' */
        @Test
        @DisplayName("throws when amount is zero or negative")
        void throwsOnInvalidAmount() {
            assertThrows(BusinessException.class, () ->
                    walletService.credit(USER_ID, BigDecimal.ZERO, "REF"));
            assertThrows(BusinessException.class, () ->
                    walletService.credit(USER_ID, new BigDecimal("-100"), "REF"));
        }

        /** Sheet walletCredit - UTCID05 (A): ví SUSPENDED -> 'Ví không ở trạng thái hoạt động' */
        @Test
        @DisplayName("throws when wallet is not active")
        void throwsWhenWalletNotActive() {
            activeWallet.setStatus(WalletStatus.CLOSED);
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.credit(USER_ID, new BigDecimal("100"), "REF"));
        }
    }

    // ─── debit ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("walletDebit")
    class Debit {

        /** Sheet walletDebit - UTCID01 (N): ví ACTIVE, số dư khả dụng > amount -> trừ đúng amount và ghi journal DEBIT */
        @Test
        @DisplayName("decreases available balance and writes journal")
        void debitDecreasesBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.debit(USER_ID, new BigDecimal("30000.00"), "WITHDRAW-789");

            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(new BigDecimal("70000.00"), walletCaptor.getValue().getAvailableBalance());

            verify(financialJournalRepository).save(journalCaptor.capture());
            var journal = journalCaptor.getValue();
            assertEquals(JournalEntryType.DEBIT, journal.getEntryType());
            assertEquals(new BigDecimal("70000.00"), journal.getBalanceAfter());
        }

        /** Sheet walletDebit - UTCID07 (A): số dư khả dụng < amount -> 'Số dư khả dụng không đủ' */
        @Test
        @DisplayName("throws when insufficient balance")
        void throwsOnInsufficientBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.debit(USER_ID, new BigDecimal("999999.00"), "REF"));
        }

        /** Sheet walletDebit - UTCID05 (A): chưa có ví -> 'Không tìm thấy ví cho người dùng này' */
        @Test
        @DisplayName("throws when wallet not found")
        void throwsWhenWalletNotFound() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () ->
                    walletService.debit(USER_ID, new BigDecimal("100"), "REF"));
        }

        /** Sheet walletDebit - UTCID06 (A): ví SUSPENDED -> 'Ví không ở trạng thái hoạt động' */
        @Test
        @DisplayName("throws when wallet is suspended")
        void throwsWhenWalletSuspended() {
            activeWallet.setStatus(WalletStatus.SUSPENDED);
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.debit(USER_ID, new BigDecimal("100"), "REF"));
        }
        /** Sheet walletDebit - UTCID03 (B): amount = 0 -> 'So tien ghi no phai lon hon 0'. */
        @Test
        @DisplayName("debit UTCID03 (B) - amount = 0 -> 'Số tiền ghi nợ phải lớn hơn 0'")
        void debitZeroAmount() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.debit(USER_ID, BigDecimal.ZERO, "DEBIT-0"));
            assertEquals("Số tiền ghi nợ phải lớn hơn 0", ex.getMessage());
            verify(walletRepository, never()).save(any());
        }

    }

    // ─── lockFunds ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("walletLockFunds")
    class LockFunds {

        /** Sheet walletLockFunds - UTCID01 (N): ví ACTIVE, khả dụng > amount -> chuyển amount sang frozen và ghi journal DEBIT */
        @Test
        @DisplayName("moves available balance to frozen balance and writes journal")
        void lockFundsMovesAvailableToFrozen() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.lockFunds(USER_ID, new BigDecimal("40000.00"), "ESCROW_LOCK-A10");

            assertEquals(new BigDecimal("60000.00"), result.getAvailableBalance());
            assertEquals(new BigDecimal("40000.00"), result.getFrozenBalance());

            verify(financialJournalRepository).save(journalCaptor.capture());
            var journal = journalCaptor.getValue();
            assertEquals(JournalEntryType.DEBIT, journal.getEntryType());
            assertEquals(new BigDecimal("40000.00"), journal.getAmount());
            assertEquals(new BigDecimal("100000.00"), journal.getBalanceBefore());
            assertEquals(new BigDecimal("60000.00"), journal.getBalanceAfter());
        }

        /** Sheet walletLockFunds - UTCID06 (A): số dư khả dụng < amount -> 'Số dư khả dụng không đủ' */
        @Test
        @DisplayName("throws when balance is insufficient")
        void throwsOnInsufficientBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.lockFunds(USER_ID, new BigDecimal("150000.00"), "ESCROW_LOCK-A10"));
        }
    }

    // ─── release/refund locked funds ───────────────────────────────────────

    @Nested
    @DisplayName("release/refund locked funds")
    class ReleaseLockedFunds {

        /** Sheet walletReleaseLocked - UTCID01 (N): frozen > amount -> giảm frozen, không đổi khả dụng, không ghi journal */
        @Test
        @DisplayName("releaseLockedFunds decreases frozen balance")
        void releaseLockedFundsDecreasesFrozen() {
            activeWallet.setFrozenBalance(new BigDecimal("40000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.releaseLockedFunds(USER_ID, new BigDecimal("30000.00"), "ESCROW_RELEASE-1");

            assertEquals(new BigDecimal("10000.00"), result.getFrozenBalance());
            assertEquals(new BigDecimal("100000.00"), result.getAvailableBalance());
            verify(financialJournalRepository, never()).save(any());
        }

        /** Sheet walletRefundLocked - UTCID01 (N): ví ACTIVE, frozen > amount -> chuyển frozen về khả dụng và ghi journal CREDIT */
        @Test
        @DisplayName("refundLockedFunds moves frozen balance back to available balance and writes journal")
        void refundLockedFundsMovesFrozenToAvailable() {
            activeWallet.setFrozenBalance(new BigDecimal("40000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.refundLockedFunds(USER_ID, new BigDecimal("30000.00"), "REFUND-ESCROW-1");

            assertEquals(new BigDecimal("10000.00"), result.getFrozenBalance());
            assertEquals(new BigDecimal("130000.00"), result.getAvailableBalance());

            verify(financialJournalRepository).save(journalCaptor.capture());
            var journal = journalCaptor.getValue();
            assertEquals(JournalEntryType.CREDIT, journal.getEntryType());
            assertEquals(new BigDecimal("30000.00"), journal.getAmount());
            assertEquals(new BigDecimal("100000.00"), journal.getBalanceBefore());
            assertEquals(new BigDecimal("130000.00"), journal.getBalanceAfter());
        }

        /** Sheet walletReleaseLocked - UTCID06 (A) + walletRefundLocked - UTCID08 (B): frozen < amount -> 'Số dư bị khóa không đủ' */
        @Test
        @DisplayName("throws when frozen balance is insufficient")
        void throwsOnInsufficientFrozenBalance() {
            activeWallet.setFrozenBalance(new BigDecimal("10000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.releaseLockedFunds(USER_ID, new BigDecimal("30000.00"), "ESCROW_RELEASE-1"));
            assertThrows(BusinessException.class, () ->
                    walletService.refundLockedFunds(USER_ID, new BigDecimal("30000.00"), "REFUND-ESCROW-1"));
        }

    }

    // ─── createTopup ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTopup")
    class CreateTopup {

        /** Ngoài phạm vi Report 5.1 (MethodList không có createTopup) - test bổ sung */
        @Test
        @DisplayName("delegates to PaymentGateway with correct params")
        void delegatesToGateway() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            TopupSession session = new TopupSession("TOPUP-abc", new BigDecimal("100000"), "qr-data", "PENDING");
            when(paymentGateway.createQr(any(BigDecimal.class), anyString())).thenReturn(session);

            TopupSession result = walletService.createTopup(USER_ID, new BigDecimal("100000"));

            assertEquals("PENDING", result.status());
            assertEquals(new BigDecimal("100000"), result.amount());
            verify(paymentGateway).createQr(any(BigDecimal.class), argThat(arg -> arg != null && arg.startsWith("TOPUP-")));
        }

        /** Ngoài phạm vi Report 5.1 (MethodList không có createTopup) - test bổ sung */
        @Test
        @DisplayName("throws when amount is invalid")
        void throwsOnInvalidAmount() {
            assertThrows(BusinessException.class, () ->
                    walletService.createTopup(USER_ID, BigDecimal.ZERO));
            assertThrows(BusinessException.class, () ->
                    walletService.createTopup(USER_ID, new BigDecimal("-1")));
        }
    }

    // ─── các nhánh biên còn thiếu so với bảng quyết định ───────────────────

    @Nested
    @DisplayName("boundary & guard cases missing from the decision tables")
    class BoundaryGaps {

        private Wallet suspended() {
            activeWallet.setStatus(WalletStatus.SUSPENDED);
            return activeWallet;
        }

        // ---- walletDebit ----

        @Test
        @DisplayName("debit UTCID02 (B) - rút đúng bằng toàn bộ số dư -> về 0, vẫn hợp lệ")
        void debitExactlyWholeBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.debit(USER_ID, new BigDecimal("100000.00"), "TRANSFER-1");

            assertEquals(0, activeWallet.getAvailableBalance().compareTo(BigDecimal.ZERO));
            verify(financialJournalRepository).save(journalCaptor.capture());
            assertEquals(JournalEntryType.DEBIT, journalCaptor.getValue().getEntryType());
            assertEquals(0, journalCaptor.getValue().getBalanceAfter().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("debit UTCID04 (A) - amount = null -> 'Số tiền ghi nợ phải lớn hơn 0'")
        void debitNullAmount() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.debit(USER_ID, null, "TRANSFER-1"));
            assertEquals("Số tiền ghi nợ phải lớn hơn 0", ex.getMessage());
            verify(walletRepository, never()).findByUser_UserId(anyLong());
        }

        // ---- walletCredit ----

        @Test
        @DisplayName("credit UTCID04 (A) - amount = null -> 'Số tiền ghi có phải lớn hơn 0'")
        void creditNullAmount() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.credit(USER_ID, null, "TRANSFER-1"));
            assertEquals("Số tiền ghi có phải lớn hơn 0", ex.getMessage());
        }

        // ---- walletLockFunds ----

        @Test
        @DisplayName("lockFunds UTCID02 (B) - khóa đúng toàn bộ số dư khả dụng -> available về 0")
        void lockExactlyWholeAvailable() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.lockFunds(USER_ID, new BigDecimal("100000.00"), "ESCROW_LOCK-A1");

            assertEquals(0, result.getAvailableBalance().compareTo(BigDecimal.ZERO));
            assertEquals(new BigDecimal("100000.00"), result.getFrozenBalance());
        }

        @Test
        @DisplayName("lockFunds UTCID03 (B) - amount = 0 -> 'Số tiền khóa escrow phải lớn hơn 0'")
        void lockZeroAmount() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.lockFunds(USER_ID, BigDecimal.ZERO, "ESCROW_LOCK-A1"));
            assertEquals("Số tiền khóa escrow phải lớn hơn 0", ex.getMessage());
        }

        @Test
        @DisplayName("lockFunds UTCID04 (A) - chưa có ví -> 'Không tìm thấy ví cho người dùng này'")
        void lockWalletNotFound() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.lockFunds(USER_ID, new BigDecimal("1000.00"), "ESCROW_LOCK-A1"));
            assertEquals("Không tìm thấy ví cho người dùng này", ex.getMessage());
        }

        @Test
        @DisplayName("lockFunds UTCID05 (A) - ví bị tạm ngưng -> 'Ví không ở trạng thái hoạt động'")
        void lockWalletNotActive() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(suspended()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.lockFunds(USER_ID, new BigDecimal("1000.00"), "ESCROW_LOCK-A1"));
            assertEquals("Ví không ở trạng thái hoạt động", ex.getMessage());
        }

        // ---- walletReleaseLocked ----

        @Test
        @DisplayName("releaseLocked UTCID02 (B) - giải ngân đúng toàn bộ frozen -> frozen về 0")
        void releaseExactlyWholeFrozen() {
            activeWallet.setFrozenBalance(new BigDecimal("40000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.releaseLockedFunds(USER_ID, new BigDecimal("40000.00"), "ESCROW_RELEASE-1");

            assertEquals(0, result.getFrozenBalance().compareTo(BigDecimal.ZERO));
            assertEquals(new BigDecimal("100000.00"), result.getAvailableBalance());
        }

        @Test
        @DisplayName("releaseLocked UTCID03 (B) - amount = 0 -> 'Số tiền giải ngân escrow phải lớn hơn 0'")
        void releaseZeroAmount() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.releaseLockedFunds(USER_ID, BigDecimal.ZERO, "ESCROW_RELEASE-1"));
            assertEquals("Số tiền giải ngân escrow phải lớn hơn 0", ex.getMessage());
        }

        @Test
        @DisplayName("releaseLocked UTCID04 (A) - chưa có ví -> 'Không tìm thấy ví cho người dùng này'")
        void releaseWalletNotFound() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.releaseLockedFunds(USER_ID, new BigDecimal("1000.00"), "ESCROW_RELEASE-1"));
            assertEquals("Không tìm thấy ví cho người dùng này", ex.getMessage());
        }

        @Test
        @DisplayName("releaseLocked UTCID05 (A) - ví bị tạm ngưng -> 'Ví không ở trạng thái hoạt động'")
        void releaseWalletNotActive() {
            activeWallet.setFrozenBalance(new BigDecimal("40000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(suspended()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.releaseLockedFunds(USER_ID, new BigDecimal("1000.00"), "ESCROW_RELEASE-1"));
            assertEquals("Ví không ở trạng thái hoạt động", ex.getMessage());
        }
    }

    // ─── walletRefundLocked ─────────────────────────────────────────────────

    @Nested
    @DisplayName("walletRefundLocked")
    class WalletRefundLocked {

        private static final String REF = "ESCROW_REFUND-1";

        /** Ví bị tạm ngưng, có sẵn số dư bị khóa để đi tới bước kiểm tra trạng thái. */
        private Wallet suspendedWallet() {
            Wallet w = new Wallet();
            w.setWalletId(USER_ID);
            w.setAvailableBalance(new BigDecimal("100000.00"));
            w.setFrozenBalance(new BigDecimal("50000.00"));
            w.setStatus(WalletStatus.SUSPENDED);
            return w;
        }

        @Test
        @DisplayName("UTCID01 (N) - ví ACTIVE, frozen đủ -> trả tiền về khả dụng + ghi journal CREDIT")
        void utcid01_refundSuccessfully() {
            activeWallet.setAvailableBalance(new BigDecimal("100000.00"));
            activeWallet.setFrozenBalance(new BigDecimal("50000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

            Wallet result = walletService.refundLockedFunds(USER_ID, new BigDecimal("20000.00"), REF);

            assertEquals(0, new BigDecimal("30000.00").compareTo(result.getFrozenBalance()));
            assertEquals(0, new BigDecimal("120000.00").compareTo(result.getAvailableBalance()));

            verify(financialJournalRepository).save(journalCaptor.capture());
            assertEquals(JournalEntryType.CREDIT, journalCaptor.getValue().getEntryType());
            assertEquals(0, new BigDecimal("20000.00").compareTo(journalCaptor.getValue().getAmount()));
        }

        @Test
        @DisplayName("UTCID02 (B) - hoàn đúng toàn bộ số dư bị khóa -> frozen về 0")
        void utcid02_refundWholeFrozenBalance() {
            activeWallet.setAvailableBalance(new BigDecimal("100000.00"));
            activeWallet.setFrozenBalance(new BigDecimal("50000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

            Wallet result = walletService.refundLockedFunds(USER_ID, new BigDecimal("50000.00"), REF);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getFrozenBalance()));
            assertEquals(0, new BigDecimal("150000.00").compareTo(result.getAvailableBalance()));
        }

        @Test
        @DisplayName("UTCID03 (A) - amount = null -> 'Số tiền hoàn escrow phải lớn hơn 0'")
        void utcid03_amountNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.refundLockedFunds(USER_ID, null, REF));
            assertEquals("Số tiền hoàn escrow phải lớn hơn 0", ex.getMessage());
            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @Test
        @DisplayName("UTCID04 (B) - amount = 0 -> 'Số tiền hoàn escrow phải lớn hơn 0'")
        void utcid04_amountZero() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.refundLockedFunds(USER_ID, BigDecimal.ZERO, REF));
            assertEquals("Số tiền hoàn escrow phải lớn hơn 0", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - amount âm -> 'Số tiền hoàn escrow phải lớn hơn 0'")
        void utcid05_amountNegative() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.refundLockedFunds(USER_ID, new BigDecimal("-1.00"), REF));
            assertEquals("Số tiền hoàn escrow phải lớn hơn 0", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - chưa có ví -> 'Không tìm thấy ví cho người dùng này'")
        void utcid06_walletNotFound() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.refundLockedFunds(USER_ID, new BigDecimal("1000.00"), REF));
            assertEquals("Không tìm thấy ví cho người dùng này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - ví bị tạm ngưng -> 'Ví không ở trạng thái hoạt động'")
        void utcid07_walletNotActive() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(suspendedWallet()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.refundLockedFunds(USER_ID, new BigDecimal("1000.00"), REF));
            assertEquals("Ví không ở trạng thái hoạt động", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - amount vượt frozen đúng 0.01 -> 'Số dư bị khóa không đủ'")
        void utcid08_frozenBalanceNotEnough() {
            activeWallet.setFrozenBalance(new BigDecimal("50000.00"));
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> walletService.refundLockedFunds(USER_ID, new BigDecimal("50000.01"), REF));
            assertEquals("Số dư bị khóa không đủ", ex.getMessage());
            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }
}
