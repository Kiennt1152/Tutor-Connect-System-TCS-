package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.TopupSession;
import com.tcs.module.finance.entity.FinancialJournal;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.JournalEntryType;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.FinancialJournalRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.PaymentGateway;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seam 0.3 (chu: M3). Implement WalletService interface.
 * - getOrCreate : lazy-init wallet when user accesses finance features.
 * - balance      : return BigDecimal balance.
 * - credit/debit : update available balance + write FinancialJournal.
 * - createTopup  : delegate to PaymentGateway (stub in phase 1).
 */
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final FinancialJournalRepository financialJournalRepository;
    private final PaymentGateway paymentGateway;

    private static final String WALLET_NOT_FOUND = "Không tìm thấy ví cho người dùng này";
    private static final String INSUFFICIENT_BALANCE = "Số dư khả dụng không đủ";
    private static final String WALLET_NOT_ACTIVE = "Ví không ở trạng thái hoạt động";
    private static final String WALLET_ALREADY_EXISTS = "Ví đã tồn tại cho người dùng này";

    @Override
    @Transactional
    public Wallet getOrCreate(Long userId) {
        return walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> createWallet(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balance(Long userId) {
        return walletRepository.findByUser_UserId(userId)
                .map(Wallet::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount, String ref) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền ghi nợ phải lớn hơn 0");
        }
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(WALLET_NOT_FOUND));

        validateWalletActive(wallet);
        validateSufficientBalance(wallet, amount);

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        wallet.setAvailableBalance(balanceBefore.subtract(amount));
        walletRepository.save(wallet);

        writeJournal(wallet, ref, amount, balanceBefore, JournalEntryType.DEBIT);
    }

    @Override
    @Transactional
    public void credit(Long userId, BigDecimal amount, String ref) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền ghi có phải lớn hơn 0");
        }
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> createWallet(userId));

        validateWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        wallet.setAvailableBalance(balanceBefore.add(amount));
        walletRepository.save(wallet);

        writeJournal(wallet, ref, amount, balanceBefore, JournalEntryType.CREDIT);
    }

    @Override
    @Transactional
    public Wallet lockFunds(Long userId, BigDecimal amount, String ref) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền khóa escrow phải lớn hơn 0");
        }
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(WALLET_NOT_FOUND));

        validateWalletActive(wallet);
        validateSufficientBalance(wallet, amount);

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        BigDecimal frozenBefore = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
        wallet.setAvailableBalance(balanceBefore.subtract(amount));
        wallet.setFrozenBalance(frozenBefore.add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        writeJournal(savedWallet, ref, amount, balanceBefore, JournalEntryType.DEBIT);
        return savedWallet;
    }

    @Override
    @Transactional
    public Wallet releaseLockedFunds(Long userId, BigDecimal amount, String ref) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền giải ngân escrow phải lớn hơn 0");
        }
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(WALLET_NOT_FOUND));

        validateWalletActive(wallet);
        validateSufficientFrozenBalance(wallet, amount);

        BigDecimal frozenBefore = frozenBalance(wallet);
        wallet.setFrozenBalance(frozenBefore.subtract(amount));
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet refundLockedFunds(Long userId, BigDecimal amount, String ref) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền hoàn escrow phải lớn hơn 0");
        }
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(WALLET_NOT_FOUND));

        validateWalletActive(wallet);
        validateSufficientFrozenBalance(wallet, amount);

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        BigDecimal frozenBefore = frozenBalance(wallet);
        wallet.setFrozenBalance(frozenBefore.subtract(amount));
        wallet.setAvailableBalance(balanceBefore.add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        writeJournal(savedWallet, ref, amount, balanceBefore, JournalEntryType.CREDIT);
        return savedWallet;
    }

    @Override
    @Transactional
    public TopupSession createTopup(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền nạp phải lớn hơn 0");
        }
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseGet(() -> createWallet(userId));

        validateWalletActive(wallet);

        String reference = "TOPUP-" + UUID.randomUUID();
        return paymentGateway.createQr(amount, reference);
    }

    private Wallet createWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng: " + userId));

        if (walletRepository.findByUser_UserId(userId).isPresent()) {
            throw new BusinessException(WALLET_ALREADY_EXISTS);
        }

        Wallet wallet = new Wallet();
        wallet.setWalletId(userId);
        wallet.setUser(user);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        return walletRepository.save(wallet);
    }

    private void writeJournal(
            Wallet wallet,
            String reference,
            BigDecimal amount,
            BigDecimal balanceBefore,
            JournalEntryType entryType) {

        FinancialJournal journal = new FinancialJournal();
        journal.setWallet(wallet);
        journal.setReferenceType(determineReferenceType(reference));
        journal.setReferenceId(0L);
        journal.setEntryType(entryType);
        journal.setAmount(amount);
        journal.setBalanceBefore(balanceBefore);
        journal.setBalanceAfter(entryType == JournalEntryType.CREDIT
                ? balanceBefore.add(amount)
                : balanceBefore.subtract(amount));
        financialJournalRepository.save(journal);
    }

    private String determineReferenceType(String ref) {
        if (ref == null) {
            return "UNKNOWN";
        }
        if (ref.startsWith("TOPUP")) {
            return "TOPUP";
        }
        if (ref.startsWith("ESCROW_LOCK")) {
            return "ESCROW_LOCK";
        }
        if (ref.startsWith("ESCROW_RELEASE")) {
            return "ESCROW_RELEASE";
        }
        if (ref.startsWith("REFUND")) {
            return "REFUND";
        }
        if (ref.startsWith("WITHDRAW")) {
            return "WITHDRAWAL";
        }
        return "TRANSFER";
    }

    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(WALLET_NOT_ACTIVE);
        }
    }

    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException(INSUFFICIENT_BALANCE);
        }
    }

    private void validateSufficientFrozenBalance(Wallet wallet, BigDecimal amount) {
        if (frozenBalance(wallet).compareTo(amount) < 0) {
            throw new BusinessException("Số dư bị khóa không đủ");
        }
    }

    private BigDecimal frozenBalance(Wallet wallet) {
        return wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
    }
}
