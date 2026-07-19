package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.ExecuteRefundRequest;
import com.tcs.module.finance.dto.response.RefundExecutionResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock
    private EscrowService escrowService;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ClassTerminationRequestRepository classTerminationRequestRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private AuthHelper authHelper;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Test
    void executeDelegatesReleaseInstructionToEscrowService() {
        ReleaseInstruction instruction = new ReleaseInstruction(
                10L,
                new BigDecimal("400000.00"),
                new BigDecimal("100000.00"),
                "Hoàn thành một phần");

        settlementService.execute(instruction);

        verify(escrowService).apply(instruction);
    }

    @Test
    void executeRejectsMissingInstruction() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                settlementService.execute(null));

        assertEquals("Thiếu chỉ dẫn tất toán", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    @Test
    void executeRefundCreatesCompletedRefundRequestAndTerminatesContract() {
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        escrow.setAssignment(assignment);

        User admin = new User();
        admin.setUserId(1L);
        admin.setEmail("admin@tcs.com");

        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setTerminationId(20L);
        termination.setAssignment(assignment);
        termination.setRequestedBy(admin);
        termination.setReason("Dừng lớp sớm");
        termination.setStatus(ClassTerminationStatus.APPROVED);

        Contract contract = new Contract();
        contract.setContractId(30L);
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.ACTIVE);

        ExecuteRefundRequest request = new ExecuteRefundRequest();
        request.setEscrowId(10L);
        request.setReleaseToBeneficiary(new BigDecimal("400000.00"));
        request.setRefundToPayer(new BigDecimal("100000.00"));
        request.setReason("Hoàn tiền một phần theo quyết định admin");

        when(authHelper.currentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest saved = invocation.getArgument(0);
            if (saved.getRefundId() == null) {
                saved.setRefundId(99L);
            }
            return saved;
        });
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(termination));
        when(contractRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.of(contract));

        RefundExecutionResponse response = settlementService.executeRefund(request);

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(escrowService).apply(instructionCaptor.capture());
        assertEquals(10L, instructionCaptor.getValue().escrowId());
        assertEquals(new BigDecimal("400000.00"), instructionCaptor.getValue().releaseToBeneficiary());
        assertEquals(new BigDecimal("100000.00"), instructionCaptor.getValue().refundToPayer());
        assertEquals(RefundRequestStatus.COMPLETED, response.getRefundStatus());
        assertEquals(99L, response.getRefundId());
        assertNotNull(response.getProcessedAt());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        verify(classTerminationRequestRepository).save(termination);
        verify(classAssignmentRepository).save(assignment);
        verify(contractRepository).save(contract);
    }

    @Test
    void executeRefundRejectsZeroRefundAmount() {
        ExecuteRefundRequest request = new ExecuteRefundRequest();
        request.setEscrowId(10L);
        request.setReleaseToBeneficiary(new BigDecimal("500000.00"));
        request.setRefundToPayer(BigDecimal.ZERO);
        request.setReason("Chỉ giải ngân không phải hoàn tiền");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                settlementService.executeRefund(request));

        assertEquals("Số tiền hoàn phải lớn hơn 0", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    @Test
    void executeRefundRejectsSettlementTotalMismatch() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);

        ExecuteRefundRequest request = new ExecuteRefundRequest();
        request.setEscrowId(10L);
        request.setReleaseToBeneficiary(new BigDecimal("300000.00"));
        request.setRefundToPayer(new BigDecimal("100000.00"));
        request.setReason("Hoàn tiền một phần theo quyết định admin");

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                settlementService.executeRefund(request));

        assertEquals("Tổng tiền giải ngân và hoàn tiền phải bằng số tiền escrow", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    @Test
    void calculateIsOwnedByM4() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                settlementService.calculate(1L));

        assertEquals("Chức năng tính toán tất toán thuộc module M4", exception.getMessage());
    }
}
