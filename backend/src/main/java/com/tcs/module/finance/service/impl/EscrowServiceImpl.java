package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
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
import com.tcs.module.finance.service.WalletService;
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

    private static final String PRIVATE_REF_PREFIX = "ESCROW_LOCK-A";
    private static final String CENTER_REF_PREFIX = "ESCROW_LOCK-CS";
    private static final String RELEASE_REF_PREFIX = "ESCROW_RELEASE-";
    private static final String REFUND_REF_PREFIX = "REFUND-ESCROW-";

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;

    @Override
    @Transactional
    public EscrowTransaction lock(EscrowLockCommand command) {
        validateCommand(command);
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
            return lockPrivateAssignment(command);
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
        return lockCenterEnrollment(command);

        escrow = escrowTransactionRepository.save(escrow);
        log.info("[Escrow] Da lock escrow id={} amount={} payer={}",
                escrow.getEscrowId(), command.amount(), command.payerUserId());
        return escrow;
    }

    @Override
    @Transactional
    public void apply(ReleaseInstruction instruction) {
        validateReleaseInstruction(instruction);
        if (instruction.escrowId() == null) {
            throw new IllegalArgumentException("ReleaseInstruction phai co escrowId");
        }

        EscrowTransaction escrow = escrowTransactionRepository.findById(instruction.escrowId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay escrow id=" + instruction.escrowId()));

        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            log.warn("[Escrow] Escrow id={} da settled (status={}), bo qua",
                    escrow.getEscrowId(), escrow.getStatus());
            return;
        }
        if (escrow.getStatus() != EscrowStatus.FUNDED) {
            throw new BusinessException("Chỉ escrow đã được khóa tiền mới có thể giải ngân");
        }

        BigDecimal releaseAmount = amountOrZero(instruction.releaseToBeneficiary());
        BigDecimal refundAmount = amountOrZero(instruction.refundToPayer());
        BigDecimal totalSettlement = releaseAmount.add(refundAmount);
        if (totalSettlement.compareTo(escrow.getAmount()) != 0) {
            throw new BusinessException("Tổng tiền giải ngân/hoàn phải bằng số tiền escrow");
        }
        BigDecimal releaseAmount = instruction.releaseToBeneficiary() != null
                ? instruction.releaseToBeneficiary() : BigDecimal.ZERO;
        BigDecimal refundAmount = instruction.refundToPayer() != null
                ? instruction.refundToPayer() : BigDecimal.ZERO;

        Wallet payerWallet = escrow.getPayment().getWallet();

        Long payerUserId = payerUserId(escrow);
        if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            releaseToBeneficiary(escrow, payerUserId, releaseAmount, instruction.reason());
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
            refundToPayer(escrow, payerUserId, refundAmount, instruction.reason());
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

        escrow.setStatus(releaseAmount.compareTo(BigDecimal.ZERO) > 0
                ? EscrowStatus.RELEASED
                : EscrowStatus.REFUNDED);
        BigDecimal totalFrozen = releaseAmount.add(refundAmount);
        BigDecimal currentFrozen = payerWallet.getFrozenBalance();
        BigDecimal deducted = totalFrozen.min(currentFrozen);
        payerWallet.setFrozenBalance(currentFrozen.subtract(deducted));
        walletRepository.save(payerWallet);

        escrow.setStatus(EscrowStatus.RELEASED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowTransactionRepository.save(escrow);
    }

    @Override
    @Transactional
    public EscrowTransaction refund(Long escrowId, String reason) {
        if (escrowId == null) {
            throw new BusinessException("Thiếu escrow cần hoàn tiền");
        }

        EscrowTransaction escrow = escrowTransactionRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));
        if (escrow.getStatus() == EscrowStatus.REFUNDED) {
            return escrow;
        }
        if (escrow.getStatus() == EscrowStatus.RELEASED) {
            throw new BusinessException("Escrow đã giải ngân nên không thể hoàn tiền");
        }
        if (escrow.getStatus() != EscrowStatus.FUNDED) {
            throw new BusinessException("Chỉ escrow đã được khóa tiền mới có thể hoàn tiền");
        }
        if (escrow.getAmount() == null || escrow.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền escrow không hợp lệ");
        }

        refundToPayer(escrow, payerUserId(escrow), escrow.getAmount(), reason);
        escrow.setStatus(EscrowStatus.REFUNDED);
        escrow.setReleasedAt(LocalDateTime.now());
        return escrowTransactionRepository.save(escrow);
    }

    private EscrowTransaction lockPrivateAssignment(EscrowLockCommand command) {
        return escrowTransactionRepository.findByAssignment_AssignmentId(command.assignmentId())
                .orElseGet(() -> {
                    ClassAssignment assignment = classAssignmentRepository.findById(command.assignmentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
                    String reference = PRIVATE_REF_PREFIX + command.assignmentId();
                    PaymentTransaction payment = createEscrowPayment(command, reference);

                    EscrowTransaction escrow = new EscrowTransaction();
                    escrow.setPayment(payment);
                    escrow.setAssignment(assignment);
                    escrow.setAmount(command.amount());
                    escrow.setStatus(EscrowStatus.FUNDED);
                    escrow.setDepositedAt(LocalDateTime.now());
                    return escrowTransactionRepository.save(escrow);
                });
        log.info("[Escrow] Da settle escrow id={} release={} refund={} reason={}",
                escrow.getEscrowId(), releaseAmount, refundAmount, instruction.reason());
    }

    private EscrowTransaction lockCenterEnrollment(EscrowLockCommand command) {
        return escrowTransactionRepository.findByClassStudent_ClassStudentId(command.classStudentId())
                .orElseGet(() -> {
                    ClassStudent classStudent = classStudentRepository.findById(command.classStudentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh học viên"));
                    String reference = CENTER_REF_PREFIX + command.classStudentId();
                    PaymentTransaction payment = createEscrowPayment(command, reference);

                    EscrowTransaction escrow = new EscrowTransaction();
                    escrow.setPayment(payment);
                    escrow.setClassStudent(classStudent);
                    escrow.setAmount(command.amount());
                    escrow.setStatus(EscrowStatus.FUNDED);
                    escrow.setDepositedAt(LocalDateTime.now());
                    return escrowTransactionRepository.save(escrow);
                });
    }

    private PaymentTransaction createEscrowPayment(EscrowLockCommand command, String reference) {
        Wallet payerWallet = walletService.lockFunds(command.payerUserId(), command.amount(), reference);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(payerWallet);
        tx.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(command.amount());
        tx.setDescription("Khóa học phí vào escrow");
        tx.setReferenceCode(reference);
        tx.setProcessedAt(LocalDateTime.now());
        return paymentTransactionRepository.save(tx);
    }

    private void releaseToBeneficiary(
            EscrowTransaction escrow,
            Long payerUserId,
            BigDecimal amount,
            String reason) {
        String reference = RELEASE_REF_PREFIX + escrow.getEscrowId();
        walletService.releaseLockedFunds(payerUserId, amount, reference);

        Long beneficiaryUserId = beneficiaryUserId(escrow);
        walletService.credit(beneficiaryUserId, amount, reference);
        Wallet beneficiaryWallet = walletService.getOrCreate(beneficiaryUserId);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(beneficiaryWallet);
        tx.setType(PaymentTransactionType.ESCROW_RELEASE);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(amount);
        tx.setDescription(buildSettlementDescription("Giải ngân escrow", reason));
        tx.setReferenceCode(reference);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);
    }

    private void refundToPayer(
            EscrowTransaction escrow,
            Long payerUserId,
            BigDecimal amount,
            String reason) {
        String reference = REFUND_REF_PREFIX + escrow.getEscrowId();
        Wallet payerWallet = walletService.refundLockedFunds(payerUserId, amount, reference);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(payerWallet);
        tx.setType(PaymentTransactionType.REFUND);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(amount);
        tx.setDescription(buildSettlementDescription("Hoàn tiền escrow", reason));
        tx.setReferenceCode(reference);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);
    }

    private Long payerUserId(EscrowTransaction escrow) {
        if (escrow.getPayment() == null || escrow.getPayment().getWallet() == null) {
            throw new BusinessException("Escrow không có ví người thanh toán");
        }
        return escrow.getPayment().getWallet().getWalletId();
    }

    private Long beneficiaryUserId(EscrowTransaction escrow) {
    private Wallet resolveBeneficiaryWallet(EscrowTransaction escrow) {
        if (escrow.getAssignment() != null
                && escrow.getAssignment().getTutor() != null
                && escrow.getAssignment().getTutor().getUser() != null) {
            return escrow.getAssignment().getTutor().getUser().getUserId();
            Long tutorUserId = escrow.getAssignment().getTutor().getUser().getUserId();
            return walletRepository.findByUser_UserId(tutorUserId)
                    .orElseGet(() -> createWalletFor(tutorUserId));
        }
        if (escrow.getClassStudent() != null
                && escrow.getClassStudent().getTutoringClass() != null
                && escrow.getClassStudent().getTutoringClass().getCenter() != null
                && escrow.getClassStudent().getTutoringClass().getCenter().getUser() != null) {
            return escrow.getClassStudent().getTutoringClass().getCenter().getUser().getUserId();
        }
        throw new BusinessException("Không xác định được người nhận giải ngân escrow");
    }

    private String buildSettlementDescription(String prefix, String reason) {
        if (reason == null || reason.isBlank()) {
            return prefix;
        }
        return prefix + ": " + reason.trim();
    }

    private void validateCommand(EscrowLockCommand command) {
        if (command == null) {
            throw new BusinessException("Thiếu thông tin khóa escrow");
        }
        if (command.payerUserId() == null) {
            throw new BusinessException("Thiếu người thanh toán escrow");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền escrow phải lớn hơn 0");
        }
        boolean hasAssignment = command.assignmentId() != null;
        boolean hasClassStudent = command.classStudentId() != null;
        if (hasAssignment == hasClassStudent) {
            throw new BusinessException("Escrow phải gắn đúng một trong assignmentId hoặc classStudentId");
        }
    }

    private void validateReleaseInstruction(ReleaseInstruction instruction) {
        if (instruction == null) {
            throw new BusinessException("Thiếu thông tin giải ngân escrow");
        }
        if (instruction.escrowId() == null) {
            throw new BusinessException("Thiếu escrow cần giải ngân");
        }
        BigDecimal releaseAmount = amountOrZero(instruction.releaseToBeneficiary());
        BigDecimal refundAmount = amountOrZero(instruction.refundToPayer());
        if (releaseAmount.compareTo(BigDecimal.ZERO) < 0 || refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số tiền giải ngân/hoàn không được âm");
        }
        if (releaseAmount.add(refundAmount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Cần có số tiền giải ngân hoặc hoàn tiền");
                && escrow.getClassStudent().getTutoringClass().getCreator() != null) {
            Long centerUserId = escrow.getClassStudent().getTutoringClass()
                    .getCreator().getUserId();
            return walletRepository.findByUser_UserId(centerUserId)
                    .orElseGet(() -> createWalletFor(centerUserId));
        }
        throw new IllegalStateException(
                "Khong xac dinh duoc beneficiary cho escrow id=" + escrow.getEscrowId());
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
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
