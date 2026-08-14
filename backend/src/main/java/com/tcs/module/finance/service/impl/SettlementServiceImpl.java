package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.request.ExecuteRefundRequest;
import com.tcs.module.finance.dto.response.RefundExecutionResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.SettlementService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final EscrowService escrowService;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final ContractRepository contractRepository;
    private final AuthHelper authHelper;

    @Override
    @Transactional(readOnly = true)
    public ReleaseInstruction calculate(Long classId) {
        throw new UnsupportedOperationException("Chức năng tính toán tất toán thuộc module M4");
    }

    @Override
    @Transactional
    public void execute(ReleaseInstruction instruction) {
        if (instruction == null) {
            throw new BusinessException("Thiếu chỉ dẫn tất toán");
        }
        escrowService.apply(instruction);
    }

    @Override
    @Transactional
    public RefundExecutionResponse executeRefund(ExecuteRefundRequest request) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        validateRefundRequest(request);

        EscrowTransaction escrow = escrowTransactionRepository.findById(request.getEscrowId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));
        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            throw new BusinessException("Escrow đã được tất toán, không thể hoàn tiền thêm");
        }

        BigDecimal releaseAmount = amountOrZero(request.getReleaseToBeneficiary());
        BigDecimal refundAmount = amountOrZero(request.getRefundToPayer());
        BigDecimal escrowAmount = amountOrZero(escrow.getAmount());
        if (releaseAmount.add(refundAmount).compareTo(escrowAmount) != 0) {
            throw new BusinessException("Tổng tiền giải ngân và hoàn tiền phải bằng số tiền escrow");
        }
        RefundPayoutInfo payoutInfo = validateRefundPayoutInfo(request.getRefundPayoutInfo());

        User admin = userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên"));
        LocalDateTime now = LocalDateTime.now();
        String reason = request.getReason().trim();

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setEscrowTransaction(escrow);
        refundRequest.setRequestedBy(admin);
        refundRequest.setBankName(payoutInfo.bankName());
        refundRequest.setAccountNo(payoutInfo.accountNo());
        refundRequest.setAccountHolderName(payoutInfo.accountHolderName());
        refundRequest.setReason(RefundPayoutInfoCodec.appendToReason(reason, payoutInfo));
        refundRequest.setAmount(refundAmount);
        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setRequestedAt(now);
        refundRequest = refundRequestRepository.save(refundRequest);

        escrowService.apply(new ReleaseInstruction(
                escrow.getEscrowId(),
                releaseAmount,
                refundAmount,
                reason));

        EscrowTransaction settledEscrow = escrowTransactionRepository.findById(escrow.getEscrowId())
                .orElse(escrow);
        refundRequest.setStatus(RefundRequestStatus.COMPLETED);
        refundRequest.setProcessedAt(LocalDateTime.now());
        refundRequest = refundRequestRepository.save(refundRequest);

        completeRelatedTermination(settledEscrow);

        return RefundExecutionResponse.builder()
                .refundId(refundRequest.getRefundId())
                .escrowId(settledEscrow.getEscrowId())
                .escrowStatus(settledEscrow.getStatus())
                .refundStatus(refundRequest.getStatus())
                .escrowAmount(escrowAmount)
                .releaseToBeneficiary(releaseAmount)
                .refundToPayer(refundAmount)
                .reason(refundRequest.getReason())
                .requestedAt(refundRequest.getRequestedAt())
                .processedAt(refundRequest.getProcessedAt())
                .message("Đã thực thi hoàn tiền escrow")
                .build();
    }

    private void validateRefundRequest(ExecuteRefundRequest request) {
        if (request == null) {
            throw new BusinessException("Thiếu thông tin hoàn tiền");
        }
        if (request.getEscrowId() == null) {
            throw new BusinessException("Thiếu escrow cần hoàn tiền");
        }
        BigDecimal releaseAmount = amountOrZero(request.getReleaseToBeneficiary());
        BigDecimal refundAmount = amountOrZero(request.getRefundToPayer());
        if (releaseAmount.compareTo(BigDecimal.ZERO) < 0 || refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số tiền giải ngân/hoàn không được âm");
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền hoàn phải lớn hơn 0");
        }
        if (!StringUtils.hasText(request.getReason()) || request.getReason().trim().length() < 10) {
            throw new BusinessException("Lý do hoàn tiền phải có ít nhất 10 ký tự");
        }
    }

    private RefundPayoutInfo validateRefundPayoutInfo(RefundPayoutInfo payoutInfo) {
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            throw new BusinessException("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền");
        }
        return new RefundPayoutInfo(
                RefundPayoutInfoCodec.normalize(payoutInfo.bankName()),
                RefundPayoutInfoCodec.normalizeAccountNo(payoutInfo.accountNo()),
                RefundPayoutInfoCodec.normalize(payoutInfo.accountHolderName()));
    }

    private void completeRelatedTermination(EscrowTransaction escrow) {
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        if (assignment == null || assignment.getAssignmentId() == null) {
            completeCenterEnrollment(escrow);
            return;
        }

        classTerminationRequestRepository
                .findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(assignment.getAssignmentId())
                .filter(request -> request.getStatus() != ClassTerminationStatus.REJECTED)
                .ifPresent(request -> {
                    request.setStatus(ClassTerminationStatus.COMPLETED);
                    request.setProcessedAt(LocalDateTime.now());
                    classTerminationRequestRepository.save(request);
                });

        if (assignment.getStatus() != ClassAssignmentStatus.TERMINATED) {
            assignment.setStatus(ClassAssignmentStatus.TERMINATED);
            classAssignmentRepository.save(assignment);
        }

        contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                .ifPresent(this::terminateContract);
    }

    private void completeCenterEnrollment(EscrowTransaction escrow) {
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        if (classStudent == null || classStudent.getClassStudentId() == null) {
            return;
        }
        classTerminationRequestRepository
                .findFirstByClassStudent_ClassStudentIdOrderByCreatedAtDesc(classStudent.getClassStudentId())
                .filter(request -> request.getStatus() != ClassTerminationStatus.REJECTED)
                .ifPresent(request -> {
                    request.setStatus(ClassTerminationStatus.COMPLETED);
                    request.setProcessedAt(LocalDateTime.now());
                    classTerminationRequestRepository.save(request);
                });
        if (classStudent.getStatus() != ClassStudentStatus.DROPPED) {
            classStudent.setStatus(ClassStudentStatus.DROPPED);
            classStudentRepository.save(classStudent);
        }
        contractRepository.findByClassStudent_ClassStudentId(classStudent.getClassStudentId())
                .ifPresent(this::terminateContract);
    }

    private void terminateContract(Contract contract) {
        if (contract.getStatus() != ContractStatus.TERMINATED) {
            contract.setStatus(ContractStatus.TERMINATED);
            contractRepository.save(contract);
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
