package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
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

    /** Sheet settlementExecute - UTCID01 (N): chỉ dẫn hợp lệ, escrow chưa tất toán, tổng = số tiền escrow -> uỷ quyền cho EscrowService */
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

    /** Sheet settlementExecute - UTCID02 (A): instruction = null -> 'Thiếu chỉ dẫn tất toán' */
    @Test
    void executeRejectsMissingInstruction() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                settlementService.execute(null));

        assertEquals("Thiếu chỉ dẫn tất toán", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    /** Sheet settlementExecuteRefund - UTCID01 (N): escrow hợp lệ, số tiền khớp, lý do đủ dài, đủ thông tin tài khoản -> tạo yêu cầu hoàn tiền hoàn tất và kết thúc hợp đồng */
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
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));

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

    /** Sheet settlementExecuteRefund - UTCID07 (B): số tiền hoàn = 0 (ngay tại ngưỡng không hợp lệ) */
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

    /** Sheet settlementExecuteRefund - UTCID05 (A): tổng tiền không khớp số tiền escrow */
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

    /** Sheet settlementCalculate - UTCID01 (A): calculate chưa hiện thực -> UnsupportedOperationException (thuộc phạm vi module M4) */
    @Test
    void calculateIsOwnedByM4() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                settlementService.calculate(1L));

        assertEquals("Chức năng tính toán tất toán thuộc module M4", exception.getMessage());
    }
    // =====================================================================
    //  Sheet: settlementExecute - cac ca con lai
    // =====================================================================

    /**
     * Sheet settlementExecute - UTCID03 (A): escrow da tat toan.
     * execute() chi kiem tra instruction null roi uy quyen cho EscrowService, nen guard
     * trang thai escrow nam o EscrowService va loi phai thoat nguyen trang qua execute().
     */
    @Test
    void executePropagatesAlreadySettledEscrowError() {
        ReleaseInstruction instruction = new ReleaseInstruction(
                10L, new BigDecimal("500000.00"), BigDecimal.ZERO, "Tat toan lop");
        doThrow(new BusinessException("Chỉ escrow đã khóa, tạm giữ hoặc tranh chấp mới có thể tất toán"))
                .when(escrowService).apply(instruction);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.execute(instruction));
        assertEquals("Chỉ escrow đã khóa, tạm giữ hoặc tranh chấp mới có thể tất toán", ex.getMessage());
    }

    /** Sheet settlementExecute - UTCID04 (A): tong release + refund khac so tien escrow. */
    @Test
    void executePropagatesSettlementTotalMismatchError() {
        ReleaseInstruction instruction = new ReleaseInstruction(
                10L, new BigDecimal("100000.00"), BigDecimal.ZERO, "Tat toan lop");
        doThrow(new BusinessException("Tổng tiền giải ngân/hoàn phải bằng số tiền escrow"))
                .when(escrowService).apply(instruction);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.execute(instruction));
        assertEquals("Tổng tiền giải ngân/hoàn phải bằng số tiền escrow", ex.getMessage());
    }

    // =====================================================================
    //  Sheet: settlementExecuteRefund - cac ca con lai
    // =====================================================================

    private ExecuteRefundRequest validRefundRequest() {
        ExecuteRefundRequest request = new ExecuteRefundRequest();
        request.setEscrowId(10L);
        request.setReleaseToBeneficiary(new BigDecimal("400000.00"));
        request.setRefundToPayer(new BigDecimal("100000.00"));
        request.setReason("Hoàn tiền một phần theo quyết định admin");
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));
        return request;
    }

    private EscrowTransaction escrowWith(EscrowStatus status, String amount) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal(amount));
        escrow.setStatus(status);
        return escrow;
    }

    /** Sheet settlementExecuteRefund - UTCID02 (A): khong truyen thong tin hoan tien. */
    @Test
    void executeRefundRejectsNullRequest() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.executeRefund(null));
        assertEquals("Thiếu thông tin hoàn tiền", ex.getMessage());
    }

    /** Sheet settlementExecuteRefund - UTCID03 (A): khong truyen escrowId. */
    @Test
    void executeRefundRejectsMissingEscrowId() {
        ExecuteRefundRequest request = validRefundRequest();
        request.setEscrowId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.executeRefund(request));
        assertEquals("Thiếu escrow cần hoàn tiền", ex.getMessage());
    }

    /** Sheet settlementExecuteRefund - UTCID04 (A): escrow da duoc tat toan. */
    @Test
    void executeRefundRejectsAlreadySettledEscrow() {
        when(escrowTransactionRepository.findById(10L))
                .thenReturn(Optional.of(escrowWith(EscrowStatus.RELEASED, "500000.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.executeRefund(validRefundRequest()));
        assertEquals("Escrow đã được tất toán, không thể hoàn tiền thêm", ex.getMessage());
    }

    /** Sheet settlementExecuteRefund - UTCID06 (A): so tien am. */
    @Test
    void executeRefundRejectsNegativeAmount() {
        ExecuteRefundRequest request = validRefundRequest();
        request.setReleaseToBeneficiary(new BigDecimal("-1.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.executeRefund(request));
        assertEquals("Số tiền giải ngân/hoàn không được âm", ex.getMessage());
    }

    /** Sheet settlementExecuteRefund - UTCID08 (B): ly do ngan hon 10 ky tu (ngay duoi nguong). */
    @Test
    void executeRefundRejectsTooShortReason() {
        ExecuteRefundRequest request = validRefundRequest();
        request.setReason("Hoan tien");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.executeRefund(request));
        assertEquals("Lý do hoàn tiền phải có ít nhất 10 ký tự", ex.getMessage());
    }

    /** Sheet settlementExecuteRefund - UTCID09 (A): thieu thong tin tai khoan nhan hoan tien. */
    @Test
    void executeRefundRejectsIncompletePayoutInfo() {
        ExecuteRefundRequest request = validRefundRequest();
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", null, "Nguyen Van A"));
        when(escrowTransactionRepository.findById(10L))
                .thenReturn(Optional.of(escrowWith(EscrowStatus.DISPUTED, "500000.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settlementService.executeRefund(request));
        assertEquals("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền", ex.getMessage());
    }
}
