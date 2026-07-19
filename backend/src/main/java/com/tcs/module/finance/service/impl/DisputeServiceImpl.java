package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.DisputeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final DisputeRepository disputeRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final ContractRepository contractRepository;
    private final EscrowService escrowService;

    @Override
    @Transactional
    public DisputeResponse createDispute(CreateDisputeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin tranh chấp");
        }
        validateReportInput(request.getTargetType(), request.getTargetId(), request.getCategory(), request.getDescription());

        Long reportClassId = request.getTargetType() == ReportTargetType.CLASS ? request.getTargetId() : null;
        validateEscrowSelector(
                request.getEscrowId(),
                request.getAssignmentId(),
                request.getClassStudentId(),
                reportClassId);

        User reporter = requireCurrentUser();
        EscrowTransaction escrow = resolveEscrow(
                request.getEscrowId(),
                request.getAssignmentId(),
                request.getClassStudentId(),
                reportClassId,
                reporter.getUserId());
        requireEscrowReportAccess(escrow, reporter.getUserId(), request.getTargetType(), request.getTargetId(), reportClassId);

        Report report = createReport(
                reporter,
                request.getTargetType(),
                request.getTargetId(),
                request.getCategory(),
                request.getDescription(),
                request.getEvidenceUrls());
        return createAndHoldDispute(report, escrow, request.getDescription());
    }

    @Override
    @Transactional
    public DisputeResponse createClassIssue(CreateClassIssueRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin báo cáo lớp học");
        }
        if (request.getClassId() == null) {
            throw new IllegalArgumentException("classId là bắt buộc");
        }
        validateReportInput(ReportTargetType.CLASS, request.getClassId(), request.getCategory(), request.getDescription());

        if (!tutoringClassRepository.existsById(request.getClassId())) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học");
        }

        User reporter = requireCurrentUser();
        EscrowTransaction escrow = resolveEscrow(
                request.getEscrowId(),
                request.getAssignmentId(),
                request.getClassStudentId(),
                request.getClassId(),
                reporter.getUserId());
        requireEscrowReportAccess(
                escrow,
                reporter.getUserId(),
                ReportTargetType.CLASS,
                request.getClassId(),
                request.getClassId());

        Report report = createReport(
                reporter,
                ReportTargetType.CLASS,
                request.getClassId(),
                request.getCategory(),
                request.getDescription(),
                request.getEvidenceUrls());
        return createAndHoldDispute(report, escrow, request.getDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDisputeReviewResponse> listDisputesForAdmin(DisputeStatus status) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Dispute> disputes = status != null
                ? disputeRepository.findByStatus(status, newestFirst)
                : disputeRepository.findAll(newestFirst);
        return disputes.stream().map(this::toAdminReviewResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDisputeReviewResponse getDisputeForAdmin(Long disputeId) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId là bắt buộc");
        }
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tranh chấp"));
        return toAdminReviewResponse(dispute);
    }

    @Override
    @Transactional
    public AdminDisputeReviewResponse resolveDispute(Long disputeId, ResolveDisputeRequest request) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId là bắt buộc");
        }
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin quyết định xử lý tranh chấp");
        }

        DisputeStatus nextStatus = request.getStatus();
        if (nextStatus == null) {
            throw new IllegalArgumentException("Trạng thái xử lý là bắt buộc");
        }
        if (nextStatus == DisputeStatus.OPEN) {
            throw new IllegalArgumentException("Không thể đưa tranh chấp về trạng thái mới mở");
        }

        String resolution = normalizeResolution(request.getResolution());
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tranh chấp"));
        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new BusinessException("Tranh chấp đã được xử lý, vui lòng dùng luồng khiếu nại/mở lại nếu cần");
        }

        dispute.setStatus(nextStatus);
        dispute.setResolution(resolution);

        Report report = dispute.getReport();
        if (nextStatus == DisputeStatus.RESOLVED && report != null) {
            report.setStatus(ReportStatus.RESOLVED);
            reportRepository.save(report);
        }
        if (nextStatus == DisputeStatus.RESOLVED) {
            completeApprovedTermination(dispute, resolution);
        }

        Dispute saved = disputeRepository.save(dispute);
        return toAdminReviewResponse(saved);
    }

    private void completeApprovedTermination(Dispute dispute, String resolution) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassTerminationRequest terminationRequest = latestTerminationRequest(assignment);
        if (terminationRequest == null || terminationRequest.getStatus() == ClassTerminationStatus.REJECTED) {
            return;
        }

        releaseEscrowForTermination(escrow, resolution);

        terminationRequest.setStatus(ClassTerminationStatus.COMPLETED);
        terminationRequest.setProcessedAt(java.time.LocalDateTime.now());
        classTerminationRequestRepository.save(terminationRequest);

        assignment.setStatus(ClassAssignmentStatus.TERMINATED);
        classAssignmentRepository.save(assignment);

        contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                .ifPresent(this::terminateContract);
    }

    private void terminateContract(Contract contract) {
        if (contract.getStatus() != ContractStatus.TERMINATED) {
            contract.setStatus(ContractStatus.TERMINATED);
            contractRepository.save(contract);
        }
    }

    private void releaseEscrowForTermination(EscrowTransaction escrow, String resolution) {
        if (escrow == null || escrow.getEscrowId() == null || escrow.getAmount() == null) {
            return;
        }
        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            return;
        }

        escrowService.apply(new ReleaseInstruction(
                escrow.getEscrowId(),
                escrow.getAmount(),
                BigDecimal.ZERO,
                "Admin xác nhận chấm dứt hợp đồng: " + resolution));
    }

    private void validateReportInput(
            ReportTargetType targetType,
            Long targetId,
            ReportCategory category,
            String description) {
        if (targetType == null || targetId == null || category == null) {
            throw new IllegalArgumentException("targetType, targetId và category là bắt buộc");
        }
        if (!StringUtils.hasText(description)) {
            throw new IllegalArgumentException("Mô tả báo cáo là bắt buộc");
        }
    }

    private String normalizeResolution(String resolution) {
        if (!StringUtils.hasText(resolution)) {
            throw new IllegalArgumentException("Nội dung quyết định là bắt buộc");
        }
        String trimmed = resolution.trim();
        if (trimmed.length() < 10) {
            throw new IllegalArgumentException("Nội dung quyết định phải có ít nhất 10 ký tự");
        }
        return trimmed;
    }

    private EscrowTransaction resolveEscrow(
            Long escrowId,
            Long assignmentId,
            Long classStudentId,
            Long classId,
            Long reporterUserId) {
        validateEscrowSelector(escrowId, assignmentId, classStudentId, classId);

        if (escrowId != null) {
            return escrowTransactionRepository.findById(escrowId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));
        }
        if (assignmentId != null) {
            return escrowTransactionRepository.findByAssignment_AssignmentId(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow của phân công lớp"));
        }
        if (classStudentId != null) {
            return escrowTransactionRepository.findByClassStudent_ClassStudentId(classStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow của ghi danh"));
        }
        if (classId != null) {
            return resolveSingleEscrowForClass(classId, reporterUserId);
        }
        throw new IllegalArgumentException("Cần cung cấp escrowId, assignmentId hoặc classStudentId");
    }

    private void validateEscrowSelector(
            Long escrowId,
            Long assignmentId,
            Long classStudentId,
            Long classId) {
        int selectorCount = countPresent(escrowId, assignmentId, classStudentId);
        if (selectorCount > 1) {
            throw new IllegalArgumentException("Chỉ được chọn một trong escrowId, assignmentId hoặc classStudentId");
        }
        if (selectorCount == 0 && classId == null) {
            throw new IllegalArgumentException("Cần cung cấp escrowId, assignmentId hoặc classStudentId");
        }
    }

    private EscrowTransaction resolveSingleEscrowForClass(Long classId, Long reporterUserId) {
        List<EscrowTransaction> candidates = new ArrayList<>();
        candidates.addAll(escrowTransactionRepository.findByAssignment_Application_TutoringClass_ClassId(classId));
        candidates.addAll(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(classId));

        List<EscrowTransaction> distinct = candidates.stream()
                .filter(Objects::nonNull)
                .filter(escrow -> escrow.getEscrowId() != null)
                .collect(
                        java.util.stream.Collectors.toMap(
                                EscrowTransaction::getEscrowId,
                                escrow -> escrow,
                                (left, right) -> left,
                                java.util.LinkedHashMap::new))
                .values()
                .stream()
                .toList();

        if (distinct.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy escrow của lớp học");
        }

        List<EscrowTransaction> accessible = distinct.stream()
                .filter(escrow -> isEscrowParticipant(escrow, reporterUserId))
                .toList();
        if (accessible.isEmpty()) {
            throw new ForbiddenException("Bạn không có quyền báo cáo lớp học này");
        }
        if (accessible.size() > 1) {
            throw new BusinessException("Lớp học có nhiều escrow, vui lòng truyền escrowId hoặc classStudentId cụ thể");
        }
        return accessible.get(0);
    }

    private Report createReport(
            User reporter,
            ReportTargetType targetType,
            Long targetId,
            ReportCategory category,
            String description,
            String evidenceUrls) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setCategory(category);
        report.setDescription(description.trim());
        report.setEvidenceUrls(StringUtils.hasText(evidenceUrls) ? evidenceUrls.trim() : null);
        return reportRepository.save(report);
    }

    private User requireCurrentUser() {
        return userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private void requireEscrowReportAccess(
            EscrowTransaction escrow,
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            Long classId) {
        if (!isEscrowParticipant(escrow, reporterUserId)) {
            throw new ForbiddenException("Bạn không có quyền báo cáo giao dịch escrow này");
        }
        if (classId != null && !escrowBelongsToClass(escrow, classId)) {
            throw new ForbiddenException("Escrow không thuộc lớp học này");
        }
        if (targetType == ReportTargetType.USER && !isEscrowParticipant(escrow, targetId)) {
            throw new ForbiddenException("Người bị báo cáo không thuộc lớp/giao dịch này");
        }
    }

    private boolean escrowBelongsToClass(EscrowTransaction escrow, Long classId) {
        ClassAssignment assignment = escrow.getAssignment();
        if (assignment != null
                && assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null
                && Objects.equals(assignment.getApplication().getTutoringClass().getClassId(), classId)) {
            return true;
        }

        ClassStudent classStudent = escrow.getClassStudent();
        return classStudent != null
                && classStudent.getTutoringClass() != null
                && Objects.equals(classStudent.getTutoringClass().getClassId(), classId);
    }

    private boolean isEscrowParticipant(EscrowTransaction escrow, Long userId) {
        if (escrow == null || userId == null) {
            return false;
        }

        if (isPayer(escrow, userId)) {
            return true;
        }

        ClassAssignment assignment = escrow.getAssignment();
        if (assignment != null) {
            if (assignment.getTutor() != null
                    && assignment.getTutor().getUser() != null
                    && Objects.equals(assignment.getTutor().getUser().getUserId(), userId)) {
                return true;
            }
            if (assignment.getApplication() != null
                    && isClassParticipant(assignment.getApplication().getTutoringClass(), userId)) {
                return true;
            }
        }

        ClassStudent classStudent = escrow.getClassStudent();
        if (classStudent != null) {
            if (classStudent.getEnrolledByUser() != null
                    && Objects.equals(classStudent.getEnrolledByUser().getUserId(), userId)) {
                return true;
            }
            return isClassParticipant(classStudent.getTutoringClass(), userId);
        }

        return false;
    }

    private boolean isPayer(EscrowTransaction escrow, Long userId) {
        PaymentTransaction payment = escrow.getPayment();
        Wallet payerWallet = payment != null ? payment.getWallet() : null;
        if (payerWallet == null) {
            return false;
        }
        if (payerWallet.getUser() != null
                && Objects.equals(payerWallet.getUser().getUserId(), userId)) {
            return true;
        }
        return payerWallet.getUser() == null && Objects.equals(payerWallet.getWalletId(), userId);
    }

    private boolean isClassParticipant(TutoringClass tutoringClass, Long userId) {
        if (tutoringClass == null || userId == null) {
            return false;
        }
        if (tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), userId)) {
            return true;
        }
        return tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null
                && Objects.equals(tutoringClass.getCenter().getUser().getUserId(), userId);
    }

    private DisputeResponse createAndHoldDispute(Report report, EscrowTransaction escrow, String reason) {
        EscrowTransaction heldEscrow = escrowService.holdForDispute(escrow.getEscrowId(), reason);

        Dispute dispute = new Dispute();
        dispute.setReport(report);
        dispute.setEscrowTransaction(heldEscrow);
        dispute.setStatus(DisputeStatus.OPEN);
        Dispute saved = disputeRepository.save(dispute);
        return toResponse(saved, heldEscrow);
    }

    private DisputeResponse toResponse(Dispute dispute, EscrowTransaction escrow) {
        Report report = dispute.getReport();
        return DisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .disputeStatus(dispute.getStatus())
                .reportId(report.getReportId())
                .reportStatus(report.getStatus())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .category(report.getCategory())
                .description(report.getDescription())
                .evidenceUrls(report.getEvidenceUrls())
                .escrowId(escrow.getEscrowId())
                .escrowStatus(escrow.getStatus())
                .createdAt(dispute.getCreatedAt())
                .build();
    }

    private AdminDisputeReviewResponse toAdminReviewResponse(Dispute dispute) {
        Report report = dispute.getReport();
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        TutoringClass tutoringClass = resolveTutoringClass(report, assignment, classStudent);
        ClassTerminationRequest terminationRequest = latestTerminationRequest(assignment);
        User reporter = report != null ? report.getReporter() : null;

        return AdminDisputeReviewResponse.builder()
                .disputeId(dispute.getDisputeId())
                .disputeStatus(dispute.getStatus())
                .resolution(dispute.getResolution())
                .disputeCreatedAt(dispute.getCreatedAt())
                .disputeUpdatedAt(dispute.getUpdatedAt())
                .reportId(report != null ? report.getReportId() : null)
                .reportStatus(report != null ? report.getStatus() : null)
                .reporterId(reporter != null ? reporter.getUserId() : null)
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .targetType(report != null ? report.getTargetType() : null)
                .targetId(report != null ? report.getTargetId() : null)
                .category(report != null ? report.getCategory() : null)
                .description(report != null ? report.getDescription() : null)
                .evidenceUrls(report != null ? report.getEvidenceUrls() : null)
                .evidenceUrlList(parseEvidenceUrls(report != null ? report.getEvidenceUrls() : null))
                .reportCreatedAt(report != null ? report.getCreatedAt() : null)
                .reportUpdatedAt(report != null ? report.getUpdatedAt() : null)
                .escrow(toEscrowReviewInfo(escrow, assignment, classStudent))
                .tutoringClass(toClassReviewInfo(tutoringClass, assignment, classStudent))
                .terminationRequest(toTerminationReviewInfo(terminationRequest))
                .build();
    }

    private AdminDisputeReviewResponse.EscrowReviewInfo toEscrowReviewInfo(
            EscrowTransaction escrow,
            ClassAssignment assignment,
            ClassStudent classStudent) {
        if (escrow == null) {
            return null;
        }

        PaymentTransaction payment = escrow.getPayment();
        Wallet payerWallet = payment != null ? payment.getWallet() : null;
        User payer = payerWallet != null ? payerWallet.getUser() : null;
        return AdminDisputeReviewResponse.EscrowReviewInfo.builder()
                .escrowId(escrow.getEscrowId())
                .status(escrow.getStatus())
                .amount(escrow.getAmount())
                .depositedAt(escrow.getDepositedAt())
                .releasedAt(escrow.getReleasedAt())
                .assignmentId(assignment != null ? assignment.getAssignmentId() : null)
                .classStudentId(classStudent != null ? classStudent.getClassStudentId() : null)
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paymentType(payment != null ? payment.getType() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentReferenceCode(payment != null ? payment.getReferenceCode() : null)
                .payerUserId(payer != null ? payer.getUserId() : null)
                .payerEmail(payer != null ? payer.getEmail() : null)
                .build();
    }

    private AdminDisputeReviewResponse.ClassReviewInfo toClassReviewInfo(
            TutoringClass tutoringClass,
            ClassAssignment assignment,
            ClassStudent classStudent) {
        if (tutoringClass == null && assignment == null && classStudent == null) {
            return null;
        }

        Tutor tutor = assignment != null ? assignment.getTutor() : null;
        User tutorUser = tutor != null ? tutor.getUser() : null;
        User creator = tutoringClass != null ? tutoringClass.getCreator() : null;
        User enroller = classStudent != null ? classStudent.getEnrolledByUser() : null;

        return AdminDisputeReviewResponse.ClassReviewInfo.builder()
                .classId(tutoringClass != null ? tutoringClass.getClassId() : null)
                .title(tutoringClass != null ? tutoringClass.getTitle() : null)
                .status(tutoringClass != null ? tutoringClass.getStatus() : null)
                .creatorUserId(creator != null ? creator.getUserId() : null)
                .creatorEmail(creator != null ? creator.getEmail() : null)
                .assignmentId(assignment != null ? assignment.getAssignmentId() : null)
                .tutorUserId(tutorUser != null ? tutorUser.getUserId() : null)
                .tutorEmail(tutorUser != null ? tutorUser.getEmail() : null)
                .tutorName(tutor != null ? tutor.getFullName() : null)
                .classStudentId(classStudent != null ? classStudent.getClassStudentId() : null)
                .enrolledByUserId(enroller != null ? enroller.getUserId() : null)
                .enrolledByEmail(enroller != null ? enroller.getEmail() : null)
                .studentName(classStudent != null ? classStudent.getStudentName() : null)
                .build();
    }

    private AdminDisputeReviewResponse.TerminationReviewInfo toTerminationReviewInfo(
            ClassTerminationRequest request) {
        if (request == null) {
            return null;
        }
        User requestedBy = request.getRequestedBy();
        return AdminDisputeReviewResponse.TerminationReviewInfo.builder()
                .terminationId(request.getTerminationId())
                .status(request.getStatus())
                .requestedByUserId(requestedBy != null ? requestedBy.getUserId() : null)
                .requestedByEmail(requestedBy != null ? requestedBy.getEmail() : null)
                .reason(request.getReason())
                .effectiveDate(request.getEffectiveDate())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    private TutoringClass resolveTutoringClass(
            Report report,
            ClassAssignment assignment,
            ClassStudent classStudent) {
        if (assignment != null
                && assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null) {
            return assignment.getApplication().getTutoringClass();
        }
        if (classStudent != null && classStudent.getTutoringClass() != null) {
            return classStudent.getTutoringClass();
        }
        if (report != null
                && report.getTargetType() == ReportTargetType.CLASS
                && report.getTargetId() != null) {
            return tutoringClassRepository.findById(report.getTargetId()).orElse(null);
        }
        return null;
    }

    private ClassTerminationRequest latestTerminationRequest(ClassAssignment assignment) {
        if (assignment == null || assignment.getAssignmentId() == null) {
            return null;
        }
        return classTerminationRequestRepository
                .findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(assignment.getAssignmentId())
                .orElse(null);
    }

    private List<String> parseEvidenceUrls(String evidenceUrls) {
        if (!StringUtils.hasText(evidenceUrls)) {
            return List.of();
        }
        return java.util.Arrays.stream(evidenceUrls.split("[\\r\\n,;]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private int countPresent(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }
}
