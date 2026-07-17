package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.DisputeStatus;
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
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
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
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final EscrowService escrowService;

    @Override
    @Transactional
    public DisputeResponse createDispute(CreateDisputeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin tranh chấp");
        }
        validateReportInput(request.getTargetType(), request.getTargetId(), request.getCategory(), request.getDescription());

        EscrowTransaction escrow = resolveEscrow(
                request.getEscrowId(),
                request.getAssignmentId(),
                request.getClassStudentId(),
                request.getTargetType() == ReportTargetType.CLASS ? request.getTargetId() : null);

        Report report = createReport(
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

        EscrowTransaction escrow = resolveEscrow(
                request.getEscrowId(),
                request.getAssignmentId(),
                request.getClassStudentId(),
                request.getClassId());

        Report report = createReport(
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

    private EscrowTransaction resolveEscrow(
            Long escrowId,
            Long assignmentId,
            Long classStudentId,
            Long classId) {
        int selectorCount = countPresent(escrowId, assignmentId, classStudentId);
        if (selectorCount > 1) {
            throw new IllegalArgumentException("Chỉ được chọn một trong escrowId, assignmentId hoặc classStudentId");
        }

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
            return resolveSingleEscrowForClass(classId);
        }
        throw new IllegalArgumentException("Cần cung cấp escrowId, assignmentId hoặc classStudentId");
    }

    private EscrowTransaction resolveSingleEscrowForClass(Long classId) {
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
        if (distinct.size() > 1) {
            throw new BusinessException("Lớp học có nhiều escrow, vui lòng truyền escrowId hoặc classStudentId cụ thể");
        }
        return distinct.get(0);
    }

    private Report createReport(
            ReportTargetType targetType,
            Long targetId,
            ReportCategory category,
            String description,
            String evidenceUrls) {
        User reporter = userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setCategory(category);
        report.setDescription(description.trim());
        report.setEvidenceUrls(StringUtils.hasText(evidenceUrls) ? evidenceUrls.trim() : null);
        return reportRepository.save(report);
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
