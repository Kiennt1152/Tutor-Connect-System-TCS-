package com.tcs.module.finance.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;

    @Override
    @Transactional
    public EscrowTransaction lock(EscrowLockCommand command) {
        if (command.assignmentId() == null && command.classStudentId() == null) {
            throw new IllegalArgumentException("Phai cung cap assignmentId hoac classStudentId");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien lock phai > 0");
        }

        Wallet payerWallet = walletRepository.findByUser_UserId(command.payerUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay wallet cua payer userId=" + command.payerUserId()));

        PaymentTransaction payment = new PaymentTransaction();
        payment.setWallet(payerWallet);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setAmount(command.amount());
        payment.setProcessedAt(LocalDateTime.now());
        payment.setReferenceCode("ESCROW-LOCK-" + System.currentTimeMillis());
        payment.setDescription("Khoa escrow khi hop dong duoc ky du");
        payment = paymentTransactionRepository.save(payment);

        payerWallet.setFrozenBalance(
                payerWallet.getFrozenBalance().add(command.amount()));
        if (payerWallet.getAvailableBalance().compareTo(command.amount()) >= 0) {
            payerWallet.setAvailableBalance(
                    payerWallet.getAvailableBalance().subtract(command.amount()));
        }
        walletRepository.save(payerWallet);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setPayment(payment);
        escrow.setAmount(command.amount());
        escrow.setStatus(EscrowStatus.ON_HOLD);
        escrow.setDepositedAt(LocalDateTime.now());

        if (command.assignmentId() != null) {
            ClassAssignment assignment = classAssignmentRepository.findById(command.assignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khong tim thay assignment id=" + command.assignmentId()));
            escrow.setAssignment(assignment);
        } else {
            ClassStudent cs = classStudentRepository.findById(command.classStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khong tim thay classStudent id=" + command.classStudentId()));
            escrow.setClassStudent(cs);
        }

        escrow = escrowTransactionRepository.save(escrow);
        log.info("[Escrow] Da lock escrow id={} amount={} payer={}",
                escrow.getEscrowId(), command.amount(), command.payerUserId());
        return escrow;
    }

    @Override
    @Transactional
    public void apply(ReleaseInstruction instruction) {
        if (instruction.escrowId() == null) {
            throw new IllegalArgumentException("ReleaseInstruction phai co escrowId");
        }

        EscrowTransaction escrow = escrowTransactionRepository.findById(instruction.escrowId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay escrow id=" + instruction.escrowId()));

        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            log.warn("[Escrow] Escrow id={} da settled (status={}), bo qua",
                    escrow.getEscrowId(), escrow.getStatus());
            return;
        }

        BigDecimal releaseAmount = instruction.releaseToBeneficiary() != null
                ? instruction.releaseToBeneficiary() : BigDecimal.ZERO;
        BigDecimal refundAmount = instruction.refundToPayer() != null
                ? instruction.refundToPayer() : BigDecimal.ZERO;

        Wallet payerWallet = escrow.getPayment().getWallet();

        if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            Wallet beneficiaryWallet = resolveBeneficiaryWallet(escrow);
            beneficiaryWallet.setAvailableBalance(
                    beneficiaryWallet.getAvailableBalance().add(releaseAmount));
            walletRepository.save(beneficiaryWallet);

            PaymentTransaction release = new PaymentTransaction();
            release.setWallet(beneficiaryWallet);
            release.setType(PaymentTransactionType.ESCROW_RELEASE);
            release.setStatus(PaymentTransactionStatus.SUCCESS);
            release.setAmount(releaseAmount);
            release.setProcessedAt(LocalDateTime.now());
            release.setReferenceCode("ESCROW-RELEASE-" + escrow.getEscrowId());
            release.setDescription(instruction.reason());
            paymentTransactionRepository.save(release);
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            payerWallet.setAvailableBalance(
                    payerWallet.getAvailableBalance().add(refundAmount));
            PaymentTransaction refund = new PaymentTransaction();
            refund.setWallet(payerWallet);
            refund.setType(PaymentTransactionType.REFUND);
            refund.setStatus(PaymentTransactionStatus.SUCCESS);
            refund.setAmount(refundAmount);
            refund.setProcessedAt(LocalDateTime.now());
            refund.setReferenceCode("REFUND-" + escrow.getEscrowId());
            refund.setDescription(instruction.reason());
            paymentTransactionRepository.save(refund);
        }

        BigDecimal totalFrozen = releaseAmount.add(refundAmount);
        BigDecimal currentFrozen = payerWallet.getFrozenBalance();
        BigDecimal deducted = totalFrozen.min(currentFrozen);
        payerWallet.setFrozenBalance(currentFrozen.subtract(deducted));
        walletRepository.save(payerWallet);

        escrow.setStatus(EscrowStatus.RELEASED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowTransactionRepository.save(escrow);

        log.info("[Escrow] Da settle escrow id={} release={} refund={} reason={}",
                escrow.getEscrowId(), releaseAmount, refundAmount, instruction.reason());
    }

    private Wallet resolveBeneficiaryWallet(EscrowTransaction escrow) {
        if (escrow.getAssignment() != null
                && escrow.getAssignment().getTutor() != null
                && escrow.getAssignment().getTutor().getUser() != null) {
            Long tutorUserId = escrow.getAssignment().getTutor().getUser().getUserId();
            return walletRepository.findByUser_UserId(tutorUserId)
                    .orElseGet(() -> createWalletFor(tutorUserId));
        }
        if (escrow.getClassStudent() != null
                && escrow.getClassStudent().getTutoringClass() != null
                && escrow.getClassStudent().getTutoringClass().getCreator() != null) {
            Long centerUserId = escrow.getClassStudent().getTutoringClass()
                    .getCreator().getUserId();
            return walletRepository.findByUser_UserId(centerUserId)
                    .orElseGet(() -> createWalletFor(centerUserId));
        }
        throw new IllegalStateException(
                "Khong xac dinh duoc beneficiary cho escrow id=" + escrow.getEscrowId());
    }

    private Wallet createWalletFor(Long userId) {
        Wallet wallet = new Wallet();
        User user = new User();
        user.setUserId(userId);
        wallet.setUser(user);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }
}