package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.RefundPayoutInfo;
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
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52SettlementServiceITTest {

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
    @Tag("report52-it")
    void IT_SET_003_GenericSettlementExecuteRejectsMissingInstruction() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.execute(null));

        assertEquals("Thiếu chỉ dẫn tất toán", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_001_AdminPartialRefundDecisionCreatesRefundRecordAndTerminatesPrivateContract() {
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        EscrowTransaction escrow = disputedPrivateEscrow(10L, new BigDecimal("500000.00"), assignment);
        User admin = adminUser();
        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setTerminationId(20L);
        termination.setAssignment(assignment);
        termination.setStatus(ClassTerminationStatus.APPROVED);
        Contract contract = new Contract();
        contract.setContractId(30L);
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.ACTIVE);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin hoàn tiền theo số buổi còn lại");

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
        assertEquals(new BigDecimal("200000.00"), instructionCaptor.getValue().releaseToBeneficiary());
        assertEquals(new BigDecimal("300000.00"), instructionCaptor.getValue().refundToPayer());
        assertEquals(RefundRequestStatus.COMPLETED, response.getRefundStatus());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertNotNull(response.getProcessedAt());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_020_AdminPartialRefundDecisionDropsCenterEnrollmentAndTerminatesCenterContract() {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);
        classStudent.setStatus(ClassStudentStatus.ENROLLED);
        EscrowTransaction escrow = disputedCenterEscrow(11L, new BigDecimal("600000.00"), classStudent);
        User admin = adminUser();
        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setTerminationId(21L);
        termination.setClassStudent(classStudent);
        termination.setStatus(ClassTerminationStatus.APPROVED);
        Contract contract = new Contract();
        contract.setContractId(31L);
        contract.setClassStudent(classStudent);
        contract.setStatus(ContractStatus.ACTIVE);
        ExecuteRefundRequest request = refundRequest(
                11L,
                new BigDecimal("240000.00"),
                new BigDecimal("360000.00"),
                "Admin hoàn tiền cho học viên lớp trung tâm");

        when(authHelper.currentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(escrowTransactionRepository.findById(11L)).thenReturn(Optional.of(escrow));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classTerminationRequestRepository.findFirstByClassStudent_ClassStudentIdOrderByCreatedAtDesc(9L))
                .thenReturn(Optional.of(termination));
        when(contractRepository.findByClassStudent_ClassStudentId(9L)).thenReturn(Optional.of(contract));

        RefundExecutionResponse response = settlementService.executeRefund(request);

        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(escrowService).apply(any(ReleaseInstruction.class));
        assertEquals(RefundRequestStatus.COMPLETED, response.getRefundStatus());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassStudentStatus.DROPPED, classStudent.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_005_RejectRefundDecisionWhenAmountsDoNotMatchEscrowTotal() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("300000.00"),
                new BigDecimal("100000.00"),
                "Admin nhập sai tổng tiền chia");

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Tổng tiền giải ngân và hoàn tiền phải bằng số tiền escrow", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_004_RejectRefundDecisionWithoutCompleteClientPayout() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin chưa có đủ tài khoản nhận hoàn");
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", ""));

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_011_RejectRefundDecisionWhenEscrowWasAlreadyReleased() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin thử hoàn tiền sau tất toán");

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Escrow đã được tất toán, không thể hoàn tiền thêm", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_012_RejectRefundDecisionWhenReleaseAmountIsNegative() {
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("-1.00"),
                new BigDecimal("300000.00"),
                "Admin nhập số tiền âm");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Số tiền giải ngân/hoàn không được âm", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_014_RejectRefundDecisionWhenReasonIsTooShort() {
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "ngắn");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Lý do hoàn tiền phải có ít nhất 10 ký tự", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_015_RejectRefundDecisionWhenAdminAccountCannotBeLoaded() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin duyệt hoàn tiền hợp lệ");

        when(authHelper.currentUserId()).thenReturn(1L);
        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Không tìm thấy quản trị viên", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_018_GenericSettlementExecuteDelegatesReleaseInstructionToEscrowService() {
        ReleaseInstruction instruction = new ReleaseInstruction(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin tất toán theo quyết định");

        settlementService.execute(instruction);

        verify(escrowService).apply(instruction);
    }

    private ExecuteRefundRequest refundRequest(
            Long escrowId,
            BigDecimal releaseAmount,
            BigDecimal refundAmount,
            String reason) {

        ExecuteRefundRequest request = new ExecuteRefundRequest();
        request.setEscrowId(escrowId);
        request.setReleaseToBeneficiary(releaseAmount);
        request.setRefundToPayer(refundAmount);
        request.setReason(reason);
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));
        return request;
    }

    private EscrowTransaction disputedPrivateEscrow(Long escrowId, BigDecimal amount, ClassAssignment assignment) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setAssignment(assignment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.DISPUTED);
        return escrow;
    }

    private EscrowTransaction disputedCenterEscrow(Long escrowId, BigDecimal amount, ClassStudent classStudent) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setClassStudent(classStudent);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.DISPUTED);
        return escrow;
    }

    private User adminUser() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setEmail("admin.it@tcs.test");
        return admin;
    }
}
