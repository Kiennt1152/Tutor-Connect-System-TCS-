package com.tcs.module.finance.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.AdminEscrowService;
import com.tcs.module.identity.entity.User;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AdminEscrowServiceImpl implements AdminEscrowService {
    private final EscrowTransactionRepository repository;

    public AdminEscrowPageResponse search(EscrowStatus status, LocalDate from, LocalDate to, String reference,
            String payer, String beneficiary, int page, int size) {
        Page<EscrowTransaction> result = repository.searchAdmin(status,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay(),
                blankToNull(reference), blankToNull(payer), blankToNull(beneficiary),
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return AdminEscrowPageResponse.builder().content(result.map(this::map).getContent()).page(result.getNumber())
                .size(result.getSize()).totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
    }

    public AdminEscrowResponse get(Long escrowId) {
        return map(repository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow: " + escrowId)));
    }

    private AdminEscrowResponse map(EscrowTransaction escrow) {
        User payer = escrow.getPayment().getWallet().getUser();
        User beneficiary = escrow.getAssignment() != null ? escrow.getAssignment().getTutor().getUser()
                : escrow.getClassStudent() != null && escrow.getClassStudent().getTutoringClass().getCenter() != null
                    ? escrow.getClassStudent().getTutoringClass().getCenter().getUser() : null;
        return AdminEscrowResponse.builder().escrowId(escrow.getEscrowId())
                .paymentId(escrow.getPayment().getTransactionId()).referenceCode(escrow.getPayment().getReferenceCode())
                .amount(escrow.getAmount()).status(escrow.getStatus()).payerUserId(payer.getUserId()).payerEmail(payer.getEmail())
                .beneficiaryUserId(beneficiary == null ? null : beneficiary.getUserId())
                .beneficiaryEmail(beneficiary == null ? null : beneficiary.getEmail())
                .assignmentId(escrow.getAssignment() == null ? null : escrow.getAssignment().getAssignmentId())
                .classStudentId(escrow.getClassStudent() == null ? null : escrow.getClassStudent().getClassStudentId())
                .depositedAt(escrow.getDepositedAt()).releasedAt(escrow.getReleasedAt())
                .createdAt(escrow.getCreatedAt()).updatedAt(escrow.getUpdatedAt()).build();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
