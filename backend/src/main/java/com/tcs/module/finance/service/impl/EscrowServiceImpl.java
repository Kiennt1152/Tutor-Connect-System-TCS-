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
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private static final String PRIVATE_REF_PREFIX = "ESCROW_LOCK-A";
    private static final String CENTER_REF_PREFIX = "ESCROW_LOCK-CS";

    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;

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
    public void apply(ReleaseInstruction instruction) {
        throw new UnsupportedOperationException("TODO: release/refund escrow will be implemented in settlement flow");
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
}
