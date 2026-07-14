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

        @Test
        @DisplayName("returns existing wallet when found")
        void returnsExistingWallet() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            Wallet result = walletService.getOrCreate(USER_ID);

            assertEquals(activeWallet, result);
            verify(walletRepository, never()).save(any());
        }

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
    @DisplayName("balance")
    class Balance {

        @Test
        @DisplayName("returns available balance when wallet exists")
        void returnsBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            BigDecimal result = walletService.balance(USER_ID);

            assertEquals(new BigDecimal("100000.00"), result);
        }

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
    @DisplayName("credit")
    class Credit {

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

        @Test
        @DisplayName("throws when amount is zero or negative")
        void throwsOnInvalidAmount() {
            assertThrows(BusinessException.class, () ->
                    walletService.credit(USER_ID, BigDecimal.ZERO, "REF"));
            assertThrows(BusinessException.class, () ->
                    walletService.credit(USER_ID, new BigDecimal("-100"), "REF"));
        }

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
    @DisplayName("debit")
    class Debit {

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

        @Test
        @DisplayName("throws when insufficient balance")
        void throwsOnInsufficientBalance() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.debit(USER_ID, new BigDecimal("999999.00"), "REF"));
        }

        @Test
        @DisplayName("throws when wallet not found")
        void throwsWhenWalletNotFound() {
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () ->
                    walletService.debit(USER_ID, new BigDecimal("100"), "REF"));
        }

        @Test
        @DisplayName("throws when wallet is suspended")
        void throwsWhenWalletSuspended() {
            activeWallet.setStatus(WalletStatus.SUSPENDED);
            when(walletRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(activeWallet));

            assertThrows(BusinessException.class, () ->
                    walletService.debit(USER_ID, new BigDecimal("100"), "REF"));
        }
    }

    // ─── createTopup ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTopup")
    class CreateTopup {

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

        @Test
        @DisplayName("throws when amount is invalid")
        void throwsOnInvalidAmount() {
            assertThrows(BusinessException.class, () ->
                    walletService.createTopup(USER_ID, BigDecimal.ZERO));
            assertThrows(BusinessException.class, () ->
                    walletService.createTopup(USER_ID, new BigDecimal("-1")));
        }
    }
}
