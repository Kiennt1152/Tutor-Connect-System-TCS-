package com.tcs.module.finance.service.impl;

import com.tcs.common.event.ContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
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
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ContractRepository contractRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EscrowTransaction lock(EscrowLockCommand command) {
        validateCommand(command);
        if (command.assignmentId() != null) {
            return lockPrivateAssignment(command);
        }
        return lockCenterEnrollment(command);
    }

    @Override
    @Transactional
    public PaymentTransaction preparePrivateContractPayment(Long payerUserId, BigDecimal amount, Long assignmentId) {
        if (payerUserId == null) {
            throw new BusinessException("Thiếu người thanh toán escrow");
        }
        if (assignmentId == null) {
            throw new BusinessException("Thiếu phân công lớp cần ký quỹ");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền escrow phải lớn hơn 0");
        }

        escrowTransactionRepository.findByAssignment_AssignmentId(assignmentId)
                .ifPresent(existing -> {
                    throw new BusinessException("Escrow của hợp đồng này đã được khóa");
                });

        String reference = privateReference(assignmentId);
        PaymentTransaction existing = paymentTransactionRepository.findByReferenceCode(reference).orElse(null);
        if (existing != null) {
            return existing;
        }

        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(platformEscrowWallet());
        tx.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setDescription("Chờ học viên chuyển khoản ký quỹ tháng đầu cho assignment #" + assignment.getAssignmentId());
        tx.setReferenceCode(reference);
        return paymentTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    public EscrowTransaction fundPendingPayment(PaymentTransaction payment, String externalTransactionId) {
        validatePendingEscrowPayment(payment);

        Long assignmentId = parsePrivateAssignmentId(payment.getReferenceCode());
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        EscrowTransaction existing = escrowTransactionRepository
                .findByAssignment_AssignmentId(assignmentId)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Long platformUserId = payment.getWallet().getWalletId();
        walletService.credit(platformUserId, payment.getAmount(), payment.getReferenceCode());
        walletService.lockFunds(platformUserId, payment.getAmount(), payment.getReferenceCode());

        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setExternalTransactionId(externalTransactionId);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setFailureReason(null);
        paymentTransactionRepository.save(payment);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setPayment(payment);
        escrow.setAssignment(assignment);
        escrow.setAmount(payment.getAmount());
        escrow.setStatus(EscrowStatus.FUNDED);
        escrow.setDepositedAt(LocalDateTime.now());
        EscrowTransaction saved = escrowTransactionRepository.save(escrow);

        Contract contract = activateContract(assignmentId);
        publishContractSigned(saved, assignment, contract);
        log.info("[Escrow] Đã khóa escrow từ SePay cho assignment={} amount={}",
                assignmentId, payment.getAmount());
        return saved;
    }

    @Override
    @Transactional
    public void apply(ReleaseInstruction instruction) {
        validateReleaseInstruction(instruction);

        EscrowTransaction escrow = escrowTransactionRepository.findById(instruction.escrowId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));

        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            log.warn("[Escrow] Escrow id={} đã tất toán, bỏ qua", escrow.getEscrowId());
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

        Long payerUserId = payerUserId(escrow);
        if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            releaseToBeneficiary(escrow, payerUserId, releaseAmount, instruction.reason());
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundToPayer(escrow, payerUserId, refundAmount, instruction.reason());
        }

        escrow.setStatus(releaseAmount.compareTo(BigDecimal.ZERO) > 0
                ? EscrowStatus.RELEASED
                : EscrowStatus.REFUNDED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowTransactionRepository.save(escrow);

        log.info("[Escrow] Đã tất toán escrow id={} release={} refund={} reason={}",
                escrow.getEscrowId(), releaseAmount, refundAmount, instruction.reason());
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
                    String reference = privateReference(command.assignmentId());
                    PaymentTransaction payment = createEscrowPayment(command, reference);

                    EscrowTransaction escrow = new EscrowTransaction();
                    escrow.setPayment(payment);
                    escrow.setAssignment(assignment);
                    escrow.setAmount(command.amount());
                    escrow.setStatus(EscrowStatus.FUNDED);
                    escrow.setDepositedAt(LocalDateTime.now());
                    EscrowTransaction saved = escrowTransactionRepository.save(escrow);
                    log.info("[Escrow] Đã khóa escrow cho assignment={} amount={}",
                            command.assignmentId(), command.amount());
                    return saved;
                });
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
                    EscrowTransaction saved = escrowTransactionRepository.save(escrow);
                    log.info("[Escrow] Đã khóa escrow cho ghi danh={} amount={}",
                            command.classStudentId(), command.amount());
                    return saved;
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

    private Wallet platformEscrowWallet() {
        PlatformAdmin admin = platformAdminRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Chưa có tài khoản admin để làm ví ký quỹ hệ thống"));
        if (admin.getUser() == null || admin.getUser().getUserId() == null) {
            throw new BusinessException("Tài khoản admin không hợp lệ để làm ví ký quỹ hệ thống");
        }
        return walletService.getOrCreate(admin.getUser().getUserId());
    }

    private void validatePendingEscrowPayment(PaymentTransaction payment) {
        if (payment == null) {
            throw new BusinessException("Thiếu giao dịch ký quỹ cần xử lý");
        }
        if (payment.getType() != PaymentTransactionType.ESCROW_DEPOSIT) {
            throw new BusinessException("Giao dịch không phải ký quỹ escrow");
        }
        if (payment.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new BusinessException("Giao dịch ký quỹ không ở trạng thái chờ thanh toán");
        }
        if (payment.getWallet() == null || payment.getWallet().getWalletId() == null) {
            throw new BusinessException("Giao dịch ký quỹ thiếu ví hệ thống");
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền ký quỹ không hợp lệ");
        }
        parsePrivateAssignmentId(payment.getReferenceCode());
    }

    private Long parsePrivateAssignmentId(String reference) {
        if (reference == null || !reference.startsWith(PRIVATE_REF_PREFIX)) {
            throw new BusinessException("Mã ký quỹ không hợp lệ");
        }
        try {
            return Long.valueOf(reference.substring(PRIVATE_REF_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new BusinessException("Mã ký quỹ không xác định được phân công lớp");
        }
    }

    private String privateReference(Long assignmentId) {
        return PRIVATE_REF_PREFIX + assignmentId;
    }

    private Contract activateContract(Long assignmentId) {
        Contract contract = contractRepository.findByAssignmentId(assignmentId).orElse(null);
        if (contract != null && contract.getStatus() == ContractStatus.SIGNED) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setConfirmedAt(LocalDateTime.now());
            return contractRepository.save(contract);
        }
        return contract;
    }

    private void publishContractSigned(EscrowTransaction escrow, ClassAssignment assignment, Contract contract) {
        TutorApplication application = assignment.getApplication();
        TutoringClass tutoringClass = application != null ? application.getTutoringClass() : null;
        if (tutoringClass == null || tutoringClass.getCreator() == null
                || assignment.getTutor() == null || assignment.getTutor().getUser() == null) {
            return;
        }
        eventPublisher.publishEvent(new ContractSigned(
                contract != null ? contract.getContractId() : null,
                tutoringClass.getClassId(),
                tutoringClass.getCreator().getUserId(),
                assignment.getTutor().getUser().getUserId(),
                escrow.getAmount(),
                assignment.getAssignmentId(),
                null));
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
        Wallet wallet = escrow.getPayment().getWallet();
        if (wallet.getUser() != null && wallet.getUser().getUserId() != null) {
            return wallet.getUser().getUserId();
        }
        if (wallet.getWalletId() != null) {
            return wallet.getWalletId();
        }
        throw new BusinessException("Escrow không xác định được người thanh toán");
    }

    private Long beneficiaryUserId(EscrowTransaction escrow) {
        if (escrow.getAssignment() != null
                && escrow.getAssignment().getTutor() != null
                && escrow.getAssignment().getTutor().getUser() != null) {
            return escrow.getAssignment().getTutor().getUser().getUserId();
        }

        ClassStudent classStudent = escrow.getClassStudent();
        TutoringClass tutoringClass = classStudent != null ? classStudent.getTutoringClass() : null;
        if (tutoringClass != null
                && tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null) {
            return tutoringClass.getCenter().getUser().getUserId();
        }
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator().getUserId();
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
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
