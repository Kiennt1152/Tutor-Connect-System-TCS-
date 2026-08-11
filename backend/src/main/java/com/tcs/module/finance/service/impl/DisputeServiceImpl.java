package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.request.AppealDisputeRequest;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.request.SubmitDisputeEvidenceRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.ClassIssueRequestedAction;
import com.tcs.module.finance.enums.ClassIssueType;
import com.tcs.module.finance.enums.DisputeResolutionAction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.DisputeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final RefundRequestRepository refundRequestRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final PlatformAdminRepository platformAdminRepository;
    private final AuditLogRepository auditLogRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
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
                RefundPayoutInfoCodec.appendToReason(
                        request.getDescription(),
                        normalizeRefundPayoutInfo(request.getRefundPayoutInfo())),
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
        validateClassIssueInput(request);

        TutoringClass tutoringClass = tutoringClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        ensureClassIssueReportable(tutoringClass);
        User reporter = requireCurrentUser();
        requireClassIssueAccess(
                tutoringClass,
                reporter.getUserId(),
                request.getAssignmentId(),
                request.getClassStudentId());
        preventDuplicateClassIssue(reporter.getUserId(), request.getClassId(), request.getIssueType());

        Report report = createReport(
                reporter,
                ReportTargetType.CLASS,
                request.getClassId(),
                resolveClassIssueCategory(request),
                buildClassIssueDescription(request),
                request.getEvidenceUrls());
        auditReport(
                report,
                "Tạo báo cáo sự cố lớp học",
                jsonObject(
                        "classId", request.getClassId(),
                        "issueType", request.getIssueType(),
                        "requestedAction", request.getRequestedAction(),
                        "lessonRef", request.getLessonRef(),
                        "occurredAt", request.getOccurredAt()));
        notifyClassIssueCreated(report, tutoringClass);

        if (!shouldEscalateClassIssue(request)) {
            return toClassIssueResponse(report);
        }

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
        return createAndHoldDispute(report, escrow, request.getDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDisputeReviewResponse> listDisputesForAdmin(DisputeStatus status) {
        UserPrincipal reviewer = authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Dispute> disputes = status != null
                ? disputeRepository.findByStatus(status, newestFirst)
                : disputeRepository.findAll(newestFirst);
        return disputes.stream()
                .filter(dispute -> canReviewDispute(reviewer, dispute))
                .map(this::toAdminReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDisputeReviewResponse getDisputeForAdmin(Long disputeId) {
        UserPrincipal reviewer = authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId là bắt buộc");
        }
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tranh chấp"));
        requireCanReviewDispute(reviewer, dispute);
        return toAdminReviewResponse(dispute);
    }

    @Override
    @Transactional
    public AdminDisputeReviewResponse resolveDispute(Long disputeId, ResolveDisputeRequest request) {
        UserPrincipal reviewer = authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId là bắt buộc");
        }
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin quyết định xử lý tranh chấp");
        }

        String resolution = normalizeResolution(request.getResolution());
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tranh chấp"));
        requireCanReviewDispute(reviewer, dispute);
        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new BusinessException("Tranh chấp đã được xử lý, vui lòng dùng luồng khiếu nại/mở lại nếu cần");
        }

        if (request.getAction() == null) {
            applyLegacyResolution(dispute, request.getStatus(), resolution);
        } else {
            applyActionResolution(dispute, request, resolution);
        }

        Dispute saved = disputeRepository.save(dispute);
        return toAdminReviewResponse(saved);
    }

    private void applyLegacyResolution(Dispute dispute, DisputeStatus nextStatus, String resolution) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Trạng thái xử lý là bắt buộc");
        }
        if (nextStatus == DisputeStatus.OPEN) {
            throw new IllegalArgumentException("Không thể đưa tranh chấp về trạng thái mới mở");
        }

        dispute.setStatus(nextStatus);
        dispute.setResolution(resolution);

        if (nextStatus == DisputeStatus.RESOLVED) {
            markReportResolved(dispute);
            approveRelatedTermination(dispute);
            notifyLegacyResolution(dispute);
        } else if (nextStatus == DisputeStatus.WAITING) {
            notifyEvidenceRequired(dispute);
        }
        auditDispute(
                dispute,
                nextStatus == DisputeStatus.WAITING ? "Yêu cầu bổ sung bằng chứng" : "Cập nhật trạng thái tranh chấp",
                null,
                jsonObject(
                        "status", nextStatus,
                        "resolution", resolution));
    }

    private void applyActionResolution(
            Dispute dispute,
            ResolveDisputeRequest request,
            String resolution) {

        DisputeResolutionAction action = request.getAction();
        dispute.setResolution(buildActionResolution(action, resolution));

        switch (action) {
            case REQUEST_MORE_EVIDENCE -> {
                dispute.setStatus(DisputeStatus.WAITING);
                notifyEvidenceRequired(dispute);
                auditDispute(
                        dispute,
                        "Yêu cầu bổ sung bằng chứng",
                        null,
                        jsonObject(
                                "action", action,
                                "status", dispute.getStatus(),
                                "resolution", resolution));
                return;
            }
            case CONTINUE_CLASS -> {
                closeDispute(dispute);
                rejectRelatedTermination(dispute);
                restoreEscrowForContinuation(dispute);
                restoreClassForContinuation(dispute);
            }
            case TERMINATE_CLASS -> {
                settleEscrow(
                        dispute,
                        request.getReleaseToBeneficiary(),
                        request.getRefundToPayer(),
                        resolution,
                        request.getRefundPayoutInfo(),
                        true);
                closeDispute(dispute);
                completeRelatedTermination(dispute);
            }
            case APPROVE_FULL_REFUND -> {
                EscrowTransaction escrow = requireEscrow(dispute);
                settleEscrow(dispute, BigDecimal.ZERO, amountOrZero(escrow.getAmount()), resolution, request.getRefundPayoutInfo());
                closeDispute(dispute);
                completeRelatedTermination(dispute);
            }
            case APPROVE_PARTIAL_REFUND -> {
                BigDecimal refundAmount = amountOrZero(request.getRefundToPayer());
                if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("Số tiền hoàn một phần phải lớn hơn 0");
                }
                settleEscrow(dispute, request.getReleaseToBeneficiary(), request.getRefundToPayer(), resolution, request.getRefundPayoutInfo());
                closeDispute(dispute);
                completeRelatedTermination(dispute);
            }
            case REJECT_REFUND -> {
                createRejectedRefundRequest(dispute, resolution);
                closeDispute(dispute);
                rejectRelatedTermination(dispute);
                restoreEscrowForContinuation(dispute);
                restoreClassForContinuation(dispute);
            }
            case CLOSE_MUTUAL_AGREEMENT -> {
                closeDispute(dispute);
                restoreEscrowForContinuation(dispute);
                restoreClassForContinuation(dispute);
            }
        }
        auditDispute(
                dispute,
                "Ra quyết định xử lý tranh chấp",
                null,
                jsonObject(
                        "action", action,
                        "status", dispute.getStatus(),
                        "releaseToBeneficiary", request.getReleaseToBeneficiary(),
                        "refundToPayer", request.getRefundToPayer(),
                        "resolution", resolution));
        notifyFinalResolution(dispute, action);
    }

    private void closeDispute(Dispute dispute) {
        dispute.setStatus(DisputeStatus.RESOLVED);
        markReportResolved(dispute);
    }

    private void markReportResolved(Dispute dispute) {
        Report report = dispute.getReport();
        if (report != null) {
            report.setStatus(ReportStatus.RESOLVED);
            reportRepository.save(report);
        }
    }

    @Override
    @Transactional
    public AdminDisputeReviewResponse appealDispute(Long disputeId, AppealDisputeRequest request) {
        UserPrincipal principal = authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN);
        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId là bắt buộc");
        }
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin khiếu nại/mở lại");
        }

        String reason = normalizeAppealReason(request.getReason());
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tranh chấp"));
        if (dispute.getStatus() != DisputeStatus.RESOLVED) {
            throw new BusinessException("Chỉ tranh chấp đã xử lý mới có thể khiếu nại/mở lại");
        }

        EscrowTransaction escrow = dispute.getEscrowTransaction();
        if (principal.getRole() != UserRole.PLATFORM_ADMIN && !isEscrowParticipant(escrow, principal.getUserId())) {
            throw new ForbiddenException("Bạn không có quyền khiếu nại tranh chấp này");
        }
        ensureEscrowCanReopen(escrow);

        EscrowTransaction heldEscrow = escrowService.holdForDispute(escrow.getEscrowId(), reason);
        dispute.setEscrowTransaction(heldEscrow);
        dispute.setStatus(DisputeStatus.UNDER_INVESTIGATION);
        dispute.setResolution(buildAppealResolution(dispute.getResolution(), reason));

        Report report = dispute.getReport();
        if (report != null) {
            report.setStatus(ReportStatus.PENDING);
            report.setEvidenceUrls(appendEvidenceUrls(report.getEvidenceUrls(), request.getEvidenceUrls()));
            reportRepository.save(report);
        }

        Dispute saved = disputeRepository.save(dispute);
        auditDispute(
                saved,
                "Mở lại tranh chấp",
                jsonObject("status", DisputeStatus.RESOLVED),
                jsonObject(
                        "status", saved.getStatus(),
                        "reason", reason,
                        "evidenceUrls", request.getEvidenceUrls()));
        notifyAppealSubmitted(saved);
        return toAdminReviewResponse(saved);
    }

    private void approveRelatedTermination(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        ClassTerminationRequest terminationRequest = latestTerminationRequest(assignment, classStudent);
        if (terminationRequest == null || terminationRequest.getStatus() == ClassTerminationStatus.REJECTED) {
            return;
        }

        terminationRequest.setStatus(ClassTerminationStatus.APPROVED);
        terminationRequest.setProcessedAt(LocalDateTime.now());
        classTerminationRequestRepository.save(terminationRequest);

        if (assignment != null) {
            assignment.setStatus(ClassAssignmentStatus.TERMINATED);
            classAssignmentRepository.save(assignment);

            contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                    .ifPresent(this::terminateContract);
            return;
        }

        if (classStudent != null) {
            classStudent.setStatus(ClassStudentStatus.DROPPED);
            classStudentRepository.save(classStudent);
            contractRepository.findByClassStudent_ClassStudentId(classStudent.getClassStudentId())
                    .ifPresent(this::terminateContract);
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            if (tutoringClass != null && tutoringClass.getStatus() == TutoringClassStatus.DISPUTED) {
                tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
                tutoringClassRepository.save(tutoringClass);
            }
        }
    }

    private void completeRelatedTermination(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        ClassTerminationRequest terminationRequest = latestTerminationRequest(assignment, classStudent);
        if (terminationRequest != null && terminationRequest.getStatus() != ClassTerminationStatus.REJECTED) {
            terminationRequest.setStatus(ClassTerminationStatus.COMPLETED);
            terminationRequest.setProcessedAt(LocalDateTime.now());
            classTerminationRequestRepository.save(terminationRequest);
        }

        if (assignment != null) {
            assignment.setStatus(ClassAssignmentStatus.TERMINATED);
            classAssignmentRepository.save(assignment);
            contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                    .ifPresent(this::terminateContract);

            TutoringClass tutoringClass = assignment.getApplication() != null
                    ? assignment.getApplication().getTutoringClass()
                    : null;
            if (tutoringClass != null && tutoringClass.getStatus() != TutoringClassStatus.CANCELLED) {
                tutoringClass.setStatus(TutoringClassStatus.CANCELLED);
                tutoringClassRepository.save(tutoringClass);
            }
            return;
        }

        if (classStudent != null) {
            classStudent.setStatus(ClassStudentStatus.DROPPED);
            classStudentRepository.save(classStudent);
            contractRepository.findByClassStudent_ClassStudentId(classStudent.getClassStudentId())
                    .ifPresent(this::terminateContract);
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            if (tutoringClass != null && tutoringClass.getStatus() == TutoringClassStatus.DISPUTED) {
                tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
                tutoringClassRepository.save(tutoringClass);
            }
        }
    }

    private void rejectRelatedTermination(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        ClassTerminationRequest terminationRequest = latestTerminationRequest(assignment, classStudent);
        if (terminationRequest == null
                || terminationRequest.getStatus() == ClassTerminationStatus.REJECTED
                || terminationRequest.getStatus() == ClassTerminationStatus.COMPLETED) {
            return;
        }

        terminationRequest.setStatus(ClassTerminationStatus.REJECTED);
        terminationRequest.setProcessedAt(LocalDateTime.now());
        classTerminationRequestRepository.save(terminationRequest);
    }

    private void restoreEscrowForContinuation(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        if (escrow == null || escrow.getEscrowId() == null || escrow.getStatus() != EscrowStatus.DISPUTED) {
            return;
        }
        escrow.setStatus(EscrowStatus.FUNDED);
        escrowTransactionRepository.save(escrow);
    }

    private void restoreClassForContinuation(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        TutoringClass tutoringClass = resolveTutoringClass(dispute.getReport(), assignment, classStudent);
        if (tutoringClass != null && tutoringClass.getStatus() == TutoringClassStatus.DISPUTED) {
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
            tutoringClassRepository.save(tutoringClass);
        }
    }

    private void settleEscrow(
            Dispute dispute,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            String resolution) {

        settleEscrow(dispute, releaseToBeneficiary, refundToPayer, resolution, false);
    }

    private void settleEscrow(
            Dispute dispute,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            String resolution,
            RefundPayoutInfo payoutInfo) {

        settleEscrow(dispute, releaseToBeneficiary, refundToPayer, resolution, payoutInfo, false);
    }

    private void settleEscrow(
            Dispute dispute,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            String resolution,
            boolean defaultToProRata) {

        settleEscrow(dispute, releaseToBeneficiary, refundToPayer, resolution, null, defaultToProRata);
    }

    private void settleEscrow(
            Dispute dispute,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            String resolution,
            RefundPayoutInfo submittedPayoutInfo,
            boolean defaultToProRata) {

        EscrowTransaction escrow = requireEscrow(dispute);
        SettlementAmounts settlementAmounts = resolveSettlementAmounts(
                dispute,
                releaseToBeneficiary,
                refundToPayer,
                defaultToProRata);
        BigDecimal releaseAmount = settlementAmounts.releaseAmount();
        BigDecimal refundAmount = settlementAmounts.refundAmount();
        BigDecimal escrowAmount = amountOrZero(escrow.getAmount());

        if (releaseAmount.compareTo(BigDecimal.ZERO) < 0 || refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số tiền giải ngân/hoàn không được âm");
        }
        if (releaseAmount.add(refundAmount).compareTo(escrowAmount) != 0) {
            throw new BusinessException("Tổng tiền giải ngân và hoàn tiền phải bằng số tiền escrow");
        }

        RefundPayoutInfo payoutInfo = resolveRefundPayoutInfo(dispute, escrow, submittedPayoutInfo);
        RefundRequest refundRequest = null;
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundRequest = createApprovedRefundRequest(escrow, refundAmount, resolution, payoutInfo);
        }

        escrowService.apply(new ReleaseInstruction(
                escrow.getEscrowId(),
                releaseAmount,
                refundAmount,
                resolution,
                payoutInfo));

        if (refundRequest != null) {
            refundRequest.setStatus(RefundRequestStatus.COMPLETED);
            refundRequest.setProcessedAt(LocalDateTime.now());
            refundRequestRepository.save(refundRequest);
        }
        auditDispute(
                dispute,
                "Tất toán escrow theo quyết định",
                jsonObject("escrowStatus", escrow.getStatus()),
                jsonObject(
                        "escrowId", escrow.getEscrowId(),
                        "releaseToBeneficiary", releaseAmount,
                        "refundToPayer", refundAmount,
                        "reason", resolution,
                        "usedProRata", settlementAmounts.usedProRata()));
    }

    private SettlementAmounts resolveSettlementAmounts(
            Dispute dispute,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            boolean defaultToProRata) {

        EscrowTransaction escrow = requireEscrow(dispute);
        BigDecimal escrowAmount = amountOrZero(escrow.getAmount());

        if (defaultToProRata && releaseToBeneficiary == null && refundToPayer == null) {
            SettlementSuggestion suggestion = calculateSettlementSuggestion(dispute);
            return new SettlementAmounts(
                    suggestion.releaseAmount(),
                    suggestion.refundAmount(),
                    true);
        }

        BigDecimal releaseAmount = releaseToBeneficiary;
        BigDecimal refundAmount = refundToPayer;
        if (releaseAmount == null && refundAmount != null) {
            releaseAmount = escrowAmount.subtract(refundAmount);
        }
        if (refundAmount == null && releaseAmount != null) {
            refundAmount = escrowAmount.subtract(releaseAmount);
        }
        return new SettlementAmounts(amountOrZero(releaseAmount), amountOrZero(refundAmount), false);
    }

    private RefundRequest createApprovedRefundRequest(
            EscrowTransaction escrow,
            BigDecimal refundAmount,
            String reason,
            RefundPayoutInfo payoutInfo) {
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            throw new BusinessException("Thiếu thông tin tài khoản nhận hoàn tiền");
        }
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setEscrowTransaction(escrow);
        refundRequest.setRequestedBy(requireCurrentAdmin());
        refundRequest.setBankName(RefundPayoutInfoCodec.normalize(payoutInfo.bankName()));
        refundRequest.setAccountNo(RefundPayoutInfoCodec.normalizeAccountNo(payoutInfo.accountNo()));
        refundRequest.setReason(RefundPayoutInfoCodec.appendToReason(reason, payoutInfo));
        refundRequest.setAmount(refundAmount);
        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setRequestedAt(LocalDateTime.now());
        return refundRequestRepository.save(refundRequest);
    }

    private void createRejectedRefundRequest(Dispute dispute, String reason) {
        EscrowTransaction escrow = requireEscrow(dispute);
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setEscrowTransaction(escrow);
        refundRequest.setRequestedBy(requireCurrentAdmin());
        refundRequest.setReason(reason);
        refundRequest.setAmount(BigDecimal.ZERO);
        refundRequest.setStatus(RefundRequestStatus.REJECTED);
        refundRequest.setRequestedAt(LocalDateTime.now());
        refundRequest.setProcessedAt(LocalDateTime.now());
        refundRequestRepository.save(refundRequest);
    }

    private EscrowTransaction requireEscrow(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        if (escrow == null || escrow.getEscrowId() == null) {
            throw new BusinessException("Tranh chấp không có escrow liên quan");
        }
        return escrow;
    }

    private User requireCurrentAdmin() {
        return userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên"));
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private String buildActionResolution(DisputeResolutionAction action, String resolution) {
        return "Quyết định: " + actionLabel(action) + "\n" + resolution;
    }

    private String actionLabel(DisputeResolutionAction action) {
        return switch (action) {
            case CONTINUE_CLASS -> "Tiếp tục lớp";
            case TERMINATE_CLASS -> "Chấm dứt lớp và tất toán";
            case APPROVE_FULL_REFUND -> "Hoàn tiền toàn phần";
            case APPROVE_PARTIAL_REFUND -> "Hoàn tiền một phần";
            case REJECT_REFUND -> "Từ chối hoàn tiền";
            case CLOSE_MUTUAL_AGREEMENT -> "Đóng theo thỏa thuận";
            case REQUEST_MORE_EVIDENCE -> "Yêu cầu bổ sung bằng chứng";
        };
    }

    private SettlementSuggestion calculateSettlementSuggestion(Dispute dispute) {
        EscrowTransaction escrow = requireEscrow(dispute);
        ClassAssignment assignment = escrow.getAssignment();
        ClassStudent classStudent = escrow.getClassStudent();
        TutoringClass tutoringClass = resolveTutoringClass(dispute.getReport(), assignment, classStudent);
        if (tutoringClass == null) {
            throw new BusinessException("Không tìm thấy lớp học để tính tất toán theo số buổi");
        }

        int totalSessions = totalSessions(tutoringClass);
        int completedSessions = completedSessions(tutoringClass, classStudent);
        if (completedSessions > totalSessions) {
            completedSessions = totalSessions;
        }

        BigDecimal escrowAmount = amountOrZero(escrow.getAmount());
        BigDecimal releaseAmount = escrowAmount
                .multiply(BigDecimal.valueOf(completedSessions))
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = escrowAmount.subtract(releaseAmount);
        return new SettlementSuggestion(
                totalSessions,
                completedSessions,
                releaseAmount,
                refundAmount,
                "Tính theo số buổi đã học "
                        + completedSessions
                        + "/"
                        + totalSessions
                        + " của lớp.");
    }

    private int totalSessions(TutoringClass tutoringClass) {
        Integer configuredSessions = tutoringClass.getNumberOfSessions();
        if (configuredSessions != null && configuredSessions > 0) {
            return configuredSessions;
        }
        int lessonCount = findClassLessons(tutoringClass).size();
        if (lessonCount > 0) {
            return lessonCount;
        }
        throw new BusinessException("Lớp học chưa có số buổi hợp lệ để tính tất toán");
    }

    private int completedSessions(TutoringClass tutoringClass, ClassStudent classStudent) {
        List<Lesson> lessons = findClassLessons(tutoringClass);
        if (classStudent != null) {
            return completedCenterSessions(lessons, classStudent);
        }
        return completedPrivateSessions(lessons);
    }

    private List<Lesson> findClassLessons(TutoringClass tutoringClass) {
        if (tutoringClass == null || tutoringClass.getClassId() == null) {
            return List.of();
        }
        List<Lesson> lessons = lessonRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
        return lessons != null ? lessons : List.of();
    }

    private int completedPrivateSessions(List<Lesson> lessons) {
        return (int) lessons.stream()
                .filter(lesson -> lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED)
                .count();
    }

    private int completedCenterSessions(List<Lesson> lessons, ClassStudent classStudent) {
        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getLessonId)
                .filter(Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty() || classStudent.getClassStudentId() == null) {
            return 0;
        }
        var attendances = lessonAttendanceRepository.findByLesson_LessonIdIn(lessonIds);
        if (attendances == null || attendances.isEmpty()) {
            return 0;
        }
        return (int) attendances.stream()
                .filter(attendance -> attendance.getClassStudent() != null
                        && Objects.equals(
                                attendance.getClassStudent().getClassStudentId(),
                                classStudent.getClassStudentId()))
                .filter(attendance -> attendance.getStatus() == LessonAttendanceStatus.PRESENT)
                .count();
    }

    @Override
    @Transactional
    public DisputeResponse submitAdditionalEvidence(Long disputeId, SubmitDisputeEvidenceRequest request) {
        UserPrincipal principal = authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN);
        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId là bắt buộc");
        }
        if (request == null || !StringUtils.hasText(request.getEvidenceUrls())) {
            throw new IllegalArgumentException("Bằng chứng bổ sung là bắt buộc");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tranh chấp"));
        if (dispute.getStatus() != DisputeStatus.WAITING) {
            throw new BusinessException("Chỉ tranh chấp đang chờ bổ sung bằng chứng mới nhận thêm bằng chứng");
        }
        if (principal.getRole() != UserRole.PLATFORM_ADMIN
                && !isDisputeParticipant(dispute, principal.getUserId())) {
            throw new ForbiddenException("Bạn không có quyền bổ sung bằng chứng cho tranh chấp này");
        }

        Report report = dispute.getReport();
        if (report == null) {
            throw new BusinessException("Tranh chấp không có báo cáo liên quan");
        }
        report.setEvidenceUrls(appendEvidenceUrls(report.getEvidenceUrls(), request.getEvidenceUrls()));
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);

        dispute.setStatus(DisputeStatus.UNDER_INVESTIGATION);
        dispute.setResolution(appendResolutionNote(
                dispute.getResolution(),
                "Người dùng đã bổ sung bằng chứng"
                        + (StringUtils.hasText(request.getNote()) ? ": " + request.getNote().trim() : "")));
        Dispute saved = disputeRepository.save(dispute);
        auditDispute(
                saved,
                "Người dùng bổ sung bằng chứng",
                jsonObject("status", DisputeStatus.WAITING),
                jsonObject(
                        "status", saved.getStatus(),
                        "evidenceUrls", request.getEvidenceUrls(),
                        "note", request.getNote()));
        notifyAdditionalEvidenceSubmitted(saved);
        return toResponse(saved, saved.getEscrowTransaction());
    }

    private void terminateContract(Contract contract) {
        if (contract.getStatus() != ContractStatus.TERMINATED) {
            contract.setStatus(ContractStatus.TERMINATED);
            contractRepository.save(contract);
        }
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

    private void validateClassIssueInput(CreateClassIssueRequest request) {
        if (request.getIssueType() == null) {
            throw new IllegalArgumentException("Loại sự cố là bắt buộc");
        }
        if (request.getRequestedAction() == null) {
            throw new IllegalArgumentException("Hướng xử lý mong muốn là bắt buộc");
        }
        if (!StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Mô tả báo cáo là bắt buộc");
        }
        if (request.getDescription().trim().length() < 20) {
            throw new IllegalArgumentException("Mô tả báo cáo phải có ít nhất 20 ký tự");
        }
        if (request.getOccurredAt() != null && request.getOccurredAt().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày xảy ra sự cố không được ở tương lai");
        }
        if (requiresRefundPayoutInfo(request) && !RefundPayoutInfoCodec.hasCompletePayout(request.getRefundPayoutInfo())) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền");
        }
        validateEscrowSelector(
                request.getEscrowId(),
                request.getAssignmentId(),
                request.getClassStudentId(),
                request.getClassId());
    }

    private void ensureClassIssueReportable(TutoringClass tutoringClass) {
        TutoringClassStatus status = tutoringClass.getStatus();
        if (status == TutoringClassStatus.DRAFT
                || status == TutoringClassStatus.OPEN
                || status == TutoringClassStatus.COMPLETED
                || status == TutoringClassStatus.CANCELLED) {
            throw new BusinessException("Chỉ có thể báo cáo sự cố cho lớp đã ghép/đang diễn ra hoặc đang tranh chấp");
        }
    }

    private void requireClassIssueAccess(
            TutoringClass tutoringClass,
            Long reporterUserId,
            Long assignmentId,
            Long classStudentId) {

        boolean classOwner = isClassParticipant(tutoringClass, reporterUserId);
        if (assignmentId != null) {
            ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
            if (!assignmentBelongsToClass(assignment, tutoringClass.getClassId())) {
                throw new ForbiddenException("Phân công không thuộc lớp học này");
            }
            if (!classOwner && !isAssignmentTutor(assignment, reporterUserId)) {
                throw new ForbiddenException("Bạn không có quyền báo cáo lớp học này");
            }
            return;
        }

        if (classStudentId != null) {
            ClassStudent classStudent = classStudentRepository.findById(classStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh lớp"));
            if (!classStudentBelongsToClass(classStudent, tutoringClass.getClassId())) {
                throw new ForbiddenException("Ghi danh không thuộc lớp học này");
            }
            if (!classOwner && !isClassStudentOwner(classStudent, reporterUserId)) {
                throw new ForbiddenException("Bạn không có quyền báo cáo lớp học này");
            }
            return;
        }

        if (classOwner
                || isActiveClassTutor(tutoringClass.getClassId(), reporterUserId)
                || classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(
                        tutoringClass.getClassId(),
                        reporterUserId)) {
            return;
        }
        throw new ForbiddenException("Bạn không có quyền báo cáo lớp học này");
    }

    private boolean assignmentBelongsToClass(ClassAssignment assignment, Long classId) {
        return assignment != null
                && assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null
                && Objects.equals(assignment.getApplication().getTutoringClass().getClassId(), classId);
    }

    private boolean classStudentBelongsToClass(ClassStudent classStudent, Long classId) {
        return classStudent != null
                && classStudent.getTutoringClass() != null
                && Objects.equals(classStudent.getTutoringClass().getClassId(), classId);
    }

    private boolean isAssignmentTutor(ClassAssignment assignment, Long userId) {
        return assignment != null
                && assignment.getTutor() != null
                && assignment.getTutor().getUser() != null
                && Objects.equals(assignment.getTutor().getUser().getUserId(), userId);
    }

    private boolean isClassStudentOwner(ClassStudent classStudent, Long userId) {
        return classStudent != null
                && classStudent.getEnrolledByUser() != null
                && Objects.equals(classStudent.getEnrolledByUser().getUserId(), userId);
    }

    private boolean isActiveClassTutor(Long classId, Long userId) {
        List<ClassAssignment> assignments = classAssignmentRepository
                .findByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE);
        if (assignments == null || assignments.isEmpty()) {
            return false;
        }
        return assignments.stream().anyMatch(assignment -> isAssignmentTutor(assignment, userId));
    }

    private void preventDuplicateClassIssue(Long reporterUserId, Long classId, ClassIssueType issueType) {
        List<Report> pendingReports = reportRepository
                .findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                        reporterUserId,
                        ReportTargetType.CLASS,
                        classId,
                        ReportStatus.PENDING);
        if (pendingReports == null || pendingReports.isEmpty()) {
            return;
        }
        String duplicateMarker = "Mã loại sự cố: " + issueType.name();
        boolean hasDuplicate = pendingReports.stream()
                .map(Report::getDescription)
                .filter(StringUtils::hasText)
                .anyMatch(description -> description.contains(duplicateMarker));
        if (hasDuplicate) {
            throw new BusinessException(
                    "Lớp học đã có báo cáo sự cố cùng loại đang mở. Vui lòng theo dõi báo cáo hiện có.");
        }
    }

    private ReportCategory resolveClassIssueCategory(CreateClassIssueRequest request) {
        if (request.getCategory() != null) {
            return request.getCategory();
        }
        return switch (request.getIssueType()) {
            case PAYMENT_OR_REFUND -> ReportCategory.FRAUD;
            case INAPPROPRIATE_BEHAVIOR -> ReportCategory.ABUSE;
            default -> ReportCategory.SPAM;
        };
    }

    private boolean shouldEscalateClassIssue(CreateClassIssueRequest request) {
        return request.getRequestedAction() == ClassIssueRequestedAction.ESCALATE_DISPUTE
                || request.getRequestedAction() == ClassIssueRequestedAction.REFUND_REVIEW
                || request.getRequestedAction() == ClassIssueRequestedAction.TERMINATE_CLASS
                || request.getIssueType() == ClassIssueType.PAYMENT_OR_REFUND
                || request.getCategory() == ReportCategory.FRAUD;
    }

    private boolean requiresRefundPayoutInfo(CreateClassIssueRequest request) {
        return request.getRequestedAction() == ClassIssueRequestedAction.REFUND_REVIEW
                || request.getRequestedAction() == ClassIssueRequestedAction.TERMINATE_CLASS
                || request.getIssueType() == ClassIssueType.PAYMENT_OR_REFUND;
    }

    private String buildClassIssueDescription(CreateClassIssueRequest request) {
        String lessonRef = StringUtils.hasText(request.getLessonRef()) ? request.getLessonRef().trim() : "Không xác định";
        String occurredAt = request.getOccurredAt() != null ? request.getOccurredAt().toString() : "Không xác định";
        String description = "[UC-29] Báo cáo sự cố lớp học\n"
                + "Mã loại sự cố: " + request.getIssueType().name() + "\n"
                + "Loại sự cố: " + classIssueTypeLabel(request.getIssueType()) + "\n"
                + "Buổi/ngày liên quan: " + lessonRef + "\n"
                + "Ngày xảy ra: " + occurredAt + "\n"
                + "Mã hướng xử lý: " + request.getRequestedAction().name() + "\n"
                + "Hướng xử lý mong muốn: " + classIssueActionLabel(request.getRequestedAction()) + "\n\n"
                + "Mô tả:\n"
                + request.getDescription().trim();
        return RefundPayoutInfoCodec.appendToReason(description, normalizeRefundPayoutInfo(request.getRefundPayoutInfo()));
    }

    private String classIssueTypeLabel(ClassIssueType issueType) {
        return switch (issueType) {
            case TUTOR_ABSENT -> "Gia sư vắng mặt";
            case CLIENT_ABSENT -> "Học viên/phụ huynh vắng mặt";
            case TECHNICAL_ISSUE -> "Sự cố kỹ thuật";
            case INAPPROPRIATE_BEHAVIOR -> "Hành vi không phù hợp";
            case SCHEDULE_CONFLICT -> "Xung đột lịch học";
            case QUALITY_ISSUE -> "Chất lượng buổi học";
            case PAYMENT_OR_REFUND -> "Thanh toán/hoàn tiền";
            case OTHER -> "Khác";
        };
    }

    private String classIssueActionLabel(ClassIssueRequestedAction action) {
        return switch (action) {
            case CONTINUE_CLASS -> "Tiếp tục lớp";
            case RESCHEDULE -> "Dời lịch/bù buổi";
            case REPLACE_TUTOR -> "Đổi gia sư";
            case REFUND_REVIEW -> "Yêu cầu xem xét hoàn tiền";
            case ESCALATE_DISPUTE -> "Chuyển thành tranh chấp";
            case TERMINATE_CLASS -> "Đề nghị chấm dứt lớp";
            case OTHER -> "Khác";
        };
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

    private String normalizeAppealReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Nội dung khiếu nại là bắt buộc");
        }
        String trimmed = reason.trim();
        if (trimmed.length() < 10) {
            throw new IllegalArgumentException("Nội dung khiếu nại phải có ít nhất 10 ký tự");
        }
        return trimmed;
    }

    private void ensureEscrowCanReopen(EscrowTransaction escrow) {
        if (escrow == null || escrow.getEscrowId() == null) {
            throw new BusinessException("Tranh chấp không có escrow để mở lại");
        }
        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            throw new BusinessException("Escrow đã tất toán, không thể mở lại tranh chấp tự động");
        }
    }

    private String buildAppealResolution(String currentResolution, String reason) {
        String appealNote = "Mở lại tranh chấp: " + reason;
        if (!StringUtils.hasText(currentResolution)) {
            return appealNote;
        }
        return currentResolution.trim() + "\n\n" + appealNote;
    }

    private String appendEvidenceUrls(String currentEvidenceUrls, String newEvidenceUrls) {
        if (!StringUtils.hasText(newEvidenceUrls)) {
            return currentEvidenceUrls;
        }
        String trimmedNewEvidence = newEvidenceUrls.trim();
        if (!StringUtils.hasText(currentEvidenceUrls)) {
            return trimmedNewEvidence;
        }
        return currentEvidenceUrls.trim() + "\n" + trimmedNewEvidence;
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

    private boolean isDisputeParticipant(Dispute dispute, Long userId) {
        if (dispute == null || userId == null) {
            return false;
        }
        Report report = dispute.getReport();
        if (report != null
                && report.getReporter() != null
                && Objects.equals(report.getReporter().getUserId(), userId)) {
            return true;
        }
        return isEscrowParticipant(dispute.getEscrowTransaction(), userId);
    }

    private boolean isPayer(EscrowTransaction escrow, Long userId) {
        if (escrow == null || userId == null) {
            return false;
        }

        User payer = resolveEscrowPayerUser(escrow);
        if (payer != null && Objects.equals(payer.getUserId(), userId)) {
            return true;
        }

        PaymentTransaction payment = escrow.getPayment();
        Wallet payerWallet = payment != null ? payment.getWallet() : null;
        return payerWallet != null
                && payerWallet.getUser() == null
                && Objects.equals(payerWallet.getWalletId(), userId);
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

    private User resolveEscrowPayerUser(EscrowTransaction escrow) {
        if (escrow == null) {
            return null;
        }

        ClassStudent classStudent = escrow.getClassStudent();
        if (classStudent != null && classStudent.getEnrolledByUser() != null) {
            return classStudent.getEnrolledByUser();
        }

        ClassAssignment assignment = escrow.getAssignment();
        TutoringClass tutoringClass = assignment != null
                && assignment.getApplication() != null
                ? assignment.getApplication().getTutoringClass()
                : null;
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator();
        }

        PaymentTransaction payment = escrow.getPayment();
        Wallet payerWallet = payment != null ? payment.getWallet() : null;
        return payerWallet != null ? payerWallet.getUser() : null;
    }

    private Long fallbackPayerWalletId(EscrowTransaction escrow) {
        PaymentTransaction payment = escrow != null ? escrow.getPayment() : null;
        Wallet payerWallet = payment != null ? payment.getWallet() : null;
        return payerWallet != null && payerWallet.getUser() == null ? payerWallet.getWalletId() : null;
    }

    private DisputeResponse createAndHoldDispute(Report report, EscrowTransaction escrow, String reason) {
        EscrowTransaction heldEscrow = escrowService.holdForDispute(escrow.getEscrowId(), reason);

        Dispute dispute = new Dispute();
        dispute.setReport(report);
        dispute.setEscrowTransaction(heldEscrow);
        dispute.setStatus(DisputeStatus.OPEN);
        Dispute saved = disputeRepository.save(dispute);
        auditDispute(
                saved,
                "Tạo tranh chấp và giữ escrow",
                null,
                jsonObject(
                        "reportId", report.getReportId(),
                        "escrowId", heldEscrow.getEscrowId(),
                        "status", saved.getStatus(),
                        "reason", reason));
        notifyDisputeCreated(saved);
        return toResponse(saved, heldEscrow);
    }

    private DisputeResponse toResponse(Dispute dispute, EscrowTransaction escrow) {
        Report report = dispute.getReport();
        return DisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .disputeStatus(dispute.getStatus())
                .escalatedToDispute(true)
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

    private DisputeResponse toClassIssueResponse(Report report) {
        return DisputeResponse.builder()
                .disputeId(null)
                .disputeStatus(null)
                .escalatedToDispute(false)
                .reportId(report.getReportId())
                .reportStatus(report.getStatus())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .category(report.getCategory())
                .description(report.getDescription())
                .evidenceUrls(report.getEvidenceUrls())
                .escrowId(null)
                .escrowStatus(null)
                .createdAt(report.getCreatedAt())
                .build();
    }

    private AdminDisputeReviewResponse toAdminReviewResponse(Dispute dispute) {
        Report report = dispute.getReport();
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        TutoringClass tutoringClass = resolveTutoringClass(report, assignment, classStudent);
        ClassTerminationRequest terminationRequest = latestTerminationRequest(assignment, classStudent);
        RefundRequest latestRefundRequest = latestRefundRequest(escrow);
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
                .latestRefundRequest(toRefundReviewInfo(latestRefundRequest))
                .tutoringClass(toClassReviewInfo(tutoringClass, assignment, classStudent))
                .terminationRequest(toTerminationReviewInfo(terminationRequest))
                .settlementSuggestion(toSettlementSuggestionInfo(dispute, tutoringClass, escrow))
                .auditTrail(toAuditTrail(dispute))
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
        User payer = resolveEscrowPayerUser(escrow);
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
                .payerUserId(payer != null ? payer.getUserId() : fallbackPayerWalletId(escrow))
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
        RefundPayoutInfo payoutInfo = RefundPayoutInfoCodec.parseFromReason(request.getReason());
        return AdminDisputeReviewResponse.TerminationReviewInfo.builder()
                .terminationId(request.getTerminationId())
                .status(request.getStatus())
                .requestedByUserId(requestedBy != null ? requestedBy.getUserId() : null)
                .requestedByEmail(requestedBy != null ? requestedBy.getEmail() : null)
                .reason(RefundPayoutInfoCodec.stripFromReason(request.getReason()))
                .bankName(payoutInfo != null ? payoutInfo.bankName() : null)
                .accountNoMasked(payoutInfo != null ? RefundPayoutInfoCodec.maskAccountNo(payoutInfo.accountNo()) : null)
                .accountHolderName(payoutInfo != null ? payoutInfo.accountHolderName() : null)
                .effectiveDate(request.getEffectiveDate())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    private AdminDisputeReviewResponse.RefundReviewInfo toRefundReviewInfo(RefundRequest request) {
        if (request == null) {
            return null;
        }
        User requestedBy = request.getRequestedBy();
        RefundPayoutInfo payoutInfo = RefundPayoutInfoCodec.parseFromReason(request.getReason());
        return AdminDisputeReviewResponse.RefundReviewInfo.builder()
                .refundId(request.getRefundId())
                .status(request.getStatus())
                .amount(request.getAmount())
                .bankName(request.getBankName())
                .accountNoMasked(maskAccountNo(request.getAccountNo()))
                .accountHolderName(payoutInfo != null ? payoutInfo.accountHolderName() : null)
                .refundReferenceCode(request.getRefundReferenceCode())
                .transferStatus(request.getTransferStatus())
                .reason(RefundPayoutInfoCodec.stripFromReason(request.getReason()))
                .requestedByUserId(requestedBy != null ? requestedBy.getUserId() : null)
                .requestedByEmail(requestedBy != null ? requestedBy.getEmail() : null)
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .transferProcessedAt(request.getTransferProcessedAt())
                .build();
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return null;
        }
        String normalized = accountNo.trim();
        if (normalized.length() <= 4) {
            return "****" + normalized;
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    private AdminDisputeReviewResponse.SettlementSuggestionInfo toSettlementSuggestionInfo(
            Dispute dispute,
            TutoringClass tutoringClass,
            EscrowTransaction escrow) {

        if (dispute == null || tutoringClass == null || escrow == null || escrow.getAmount() == null) {
            return null;
        }
        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            return null;
        }
        SettlementSuggestion suggestion;
        try {
            suggestion = calculateSettlementSuggestion(dispute);
        } catch (BusinessException ex) {
            return null;
        }
        return AdminDisputeReviewResponse.SettlementSuggestionInfo.builder()
                .totalSessions(suggestion.totalSessions())
                .completedSessions(suggestion.completedSessions())
                .releaseAmount(suggestion.releaseAmount())
                .refundAmount(suggestion.refundAmount())
                .reason(suggestion.reason())
                .build();
    }

    private List<AdminDisputeReviewResponse.AuditReviewInfo> toAuditTrail(Dispute dispute) {
        if (dispute == null || dispute.getDisputeId() == null) {
            return List.of();
        }
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                "DISPUTE",
                dispute.getDisputeId());
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        return logs.stream()
                .map(this::toAuditReviewInfo)
                .toList();
    }

    private AdminDisputeReviewResponse.AuditReviewInfo toAuditReviewInfo(AuditLog log) {
        User actor = log.getActor();
        return AdminDisputeReviewResponse.AuditReviewInfo.builder()
                .auditId(log.getAuditId())
                .actorId(actor != null ? actor.getUserId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .action(log.getAction())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private void auditReport(Report report, String action, String newValue) {
        if (report == null || report.getReportId() == null || !StringUtils.hasText(action)) {
            return;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(currentActorOrNull());
        auditLog.setAction(action);
        auditLog.setEntityType("REPORT");
        auditLog.setEntityId(report.getReportId());
        auditLog.setNewValue(newValue);
        auditLogRepository.save(auditLog);
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

    private boolean canReviewDispute(UserPrincipal reviewer, Dispute dispute) {
        if (reviewer == null || dispute == null) {
            return false;
        }
        if (reviewer.getRole() == UserRole.PLATFORM_ADMIN) {
            return true;
        }
        if (reviewer.getRole() != UserRole.TUTOR_CENTER) {
            return false;
        }
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        Report report = dispute.getReport();
        TutoringClass tutoringClass = resolveTutoringClass(
                report,
                escrow != null ? escrow.getAssignment() : null,
                escrow != null ? escrow.getClassStudent() : null);
        return isOwnedCenterClass(tutoringClass, reviewer.getUserId());
    }

    private void requireCanReviewDispute(UserPrincipal reviewer, Dispute dispute) {
        if (!canReviewDispute(reviewer, dispute)) {
            throw new ForbiddenException("Bạn chỉ có quyền xử lý tranh chấp của lớp trung tâm do mình quản lý");
        }
    }

    private boolean isOwnedCenterClass(TutoringClass tutoringClass, Long centerUserId) {
        if (tutoringClass == null || centerUserId == null || tutoringClass.getClassType() != ClassType.CENTER) {
            return false;
        }
        boolean ownsByCenterProfile = tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null
                && Objects.equals(tutoringClass.getCenter().getUser().getUserId(), centerUserId);
        boolean ownsByCreator = tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), centerUserId);
        return ownsByCenterProfile || ownsByCreator;
    }

    private ClassTerminationRequest latestTerminationRequest(ClassAssignment assignment, ClassStudent classStudent) {
        if (assignment != null && assignment.getAssignmentId() != null) {
            return classTerminationRequestRepository
                    .findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(assignment.getAssignmentId())
                    .orElse(null);
        }
        if (classStudent != null && classStudent.getClassStudentId() != null) {
            return classTerminationRequestRepository
                    .findFirstByClassStudent_ClassStudentIdOrderByCreatedAtDesc(classStudent.getClassStudentId())
                    .orElse(null);
        }
        return null;
    }

    private RefundRequest latestRefundRequest(EscrowTransaction escrow) {
        if (escrow == null || escrow.getEscrowId() == null) {
            return null;
        }
        return refundRequestRepository
                .findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(escrow.getEscrowId())
                .orElse(null);
    }

    private RefundPayoutInfo resolveRefundPayoutInfo(
            Dispute dispute,
            EscrowTransaction escrow,
            RefundPayoutInfo submittedPayoutInfo) {
        RefundPayoutInfo normalizedSubmitted = normalizeRefundPayoutInfo(submittedPayoutInfo);
        if (RefundPayoutInfoCodec.hasCompletePayout(normalizedSubmitted)) {
            return normalizedSubmitted;
        }

        RefundPayoutInfo fromRefund = toRefundPayoutInfo(latestRefundRequest(escrow));
        if (RefundPayoutInfoCodec.hasCompletePayout(fromRefund)) {
            return fromRefund;
        }

        ClassAssignment assignment = escrow != null ? escrow.getAssignment() : null;
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        RefundPayoutInfo fromTermination = toRefundPayoutInfo(latestTerminationRequest(assignment, classStudent));
        if (RefundPayoutInfoCodec.hasCompletePayout(fromTermination)) {
            return fromTermination;
        }

        RefundPayoutInfo fromReport = toRefundPayoutInfo(dispute != null ? dispute.getReport() : null);
        if (RefundPayoutInfoCodec.hasCompletePayout(fromReport)) {
            return fromReport;
        }

        return RefundPayoutInfoCodec.hasCompletePayout(fromRefund) ? fromRefund : fromTermination;
    }

    private RefundPayoutInfo normalizeRefundPayoutInfo(RefundPayoutInfo payoutInfo) {
        if (payoutInfo == null) {
            return null;
        }
        return new RefundPayoutInfo(
                RefundPayoutInfoCodec.normalize(payoutInfo.bankName()),
                RefundPayoutInfoCodec.normalizeAccountNo(payoutInfo.accountNo()),
                RefundPayoutInfoCodec.normalize(payoutInfo.accountHolderName()));
    }

    private RefundPayoutInfo toRefundPayoutInfo(RefundRequest request) {
        if (request == null) {
            return null;
        }
        RefundPayoutInfo parsed = RefundPayoutInfoCodec.parseFromReason(request.getReason());
        if (RefundPayoutInfoCodec.hasCompletePayout(parsed)) {
            return parsed;
        }
        if (!isBlank(request.getBankName()) && !isBlank(request.getAccountNo())) {
            return new RefundPayoutInfo(
                    RefundPayoutInfoCodec.normalize(request.getBankName()),
                    RefundPayoutInfoCodec.normalizeAccountNo(request.getAccountNo()),
                    parsed != null ? RefundPayoutInfoCodec.normalize(parsed.accountHolderName()) : null);
        }
        return parsed;
    }

    private RefundPayoutInfo toRefundPayoutInfo(ClassTerminationRequest request) {
        if (request == null) {
            return null;
        }
        return RefundPayoutInfoCodec.parseFromReason(request.getReason());
    }

    private RefundPayoutInfo toRefundPayoutInfo(Report report) {
        if (report == null) {
            return null;
        }
        return RefundPayoutInfoCodec.parseFromReason(report.getDescription());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void auditDispute(Dispute dispute, String action, String oldValue, String newValue) {
        if (dispute == null || dispute.getDisputeId() == null || !StringUtils.hasText(action)) {
            return;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(currentActorOrNull());
        auditLog.setAction(action);
        auditLog.setEntityType("DISPUTE");
        auditLog.setEntityId(dispute.getDisputeId());
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLogRepository.save(auditLog);
    }

    private User currentActorOrNull() {
        Long actorId = authHelper.currentUserId();
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).orElse(null);
    }

    private String jsonObject(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return "{}";
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("JSON audit payload cần theo cặp key/value");
        }

        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"')
                    .append(escapeJson(String.valueOf(keyValues[i])))
                    .append("\":")
                    .append(jsonValue(keyValues[i + 1]));
        }
        return builder.append('}').toString();
    }

    private String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String appendResolutionNote(String currentResolution, String note) {
        if (!StringUtils.hasText(note)) {
            return currentResolution;
        }
        if (!StringUtils.hasText(currentResolution)) {
            return note.trim();
        }
        return currentResolution.trim() + "\n\n" + note.trim();
    }

    private void notifyClassIssueCreated(Report report, TutoringClass tutoringClass) {
        if (report != null && report.getReporter() != null) {
            createReportNotification(
                    report.getReporter(),
                    "Đã ghi nhận báo cáo sự cố lớp học",
                    "Báo cáo #" + report.getReportId() + " đã được ghi nhận và đang chờ xử lý.",
                    NotificationType.CLASS,
                    report);
        }
        notifyPlatformAdminsOfReport(
                "Có báo cáo sự cố lớp học mới",
                "Báo cáo #" + report.getReportId() + " vừa được tạo cho lớp #" + report.getTargetId() + ".",
                report);
        notifyClassOperatorsOfReport(report, tutoringClass);
    }

    private void notifyClassOperatorsOfReport(Report report, TutoringClass tutoringClass) {
        List<User> users = new ArrayList<>();
        Set<Long> seenUserIds = new LinkedHashSet<>();
        User reporter = report.getReporter();
        if (reporter != null && reporter.getUserId() != null) {
            seenUserIds.add(reporter.getUserId());
        }

        addClassOwners(users, seenUserIds, tutoringClass);
        List<ClassAssignment> activeAssignments = tutoringClass != null && tutoringClass.getClassId() != null
                ? classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        tutoringClass.getClassId(),
                        ClassAssignmentStatus.ACTIVE)
                : List.of();
        if (activeAssignments != null) {
            for (ClassAssignment assignment : activeAssignments) {
                Tutor tutor = assignment.getTutor();
                addUser(users, seenUserIds, tutor != null ? tutor.getUser() : null);
            }
        }

        for (User user : users) {
            createReportNotification(
                    user,
                    "Lớp học có báo cáo sự cố mới",
                    "Lớp #" + report.getTargetId() + " vừa có báo cáo sự cố #" + report.getReportId() + ".",
                    NotificationType.CLASS,
                    report);
        }
    }

    private void notifyPlatformAdminsOfReport(String title, String content, Report report) {
        List<PlatformAdmin> admins = platformAdminRepository.findAll();
        if (admins == null || admins.isEmpty()) {
            return;
        }
        for (PlatformAdmin admin : admins) {
            if (admin != null && admin.getUser() != null) {
                createReportNotification(admin.getUser(), title, content, NotificationType.CLASS, report);
            }
        }
    }

    private void notifyDisputeCreated(Dispute dispute) {
        notifyPlatformAdmins(
                "Có tranh chấp mới cần xem xét",
                "Tranh chấp #" + dispute.getDisputeId() + " vừa được tạo và escrow liên quan đã được tạm giữ.",
                dispute);
    }

    private void notifyEvidenceRequired(Dispute dispute) {
        String title = "Cần bổ sung bằng chứng tranh chấp";
        String content = "Admin cần thêm bằng chứng cho tranh chấp #"
                + dispute.getDisputeId()
                + ". Vui lòng mở thông báo này và gửi thêm tài liệu liên quan.";
        Report report = dispute.getReport();
        if (report != null && report.getReporter() != null) {
            createNotification(report.getReporter(), title, content, NotificationType.CLASS, dispute);
            return;
        }
        notifyParticipants(dispute, title, content);
    }

    private void notifyAdditionalEvidenceSubmitted(Dispute dispute) {
        notifyPlatformAdmins(
                "Người dùng đã bổ sung bằng chứng",
                "Tranh chấp #" + dispute.getDisputeId() + " đã có bằng chứng mới và quay lại trạng thái đang xem xét.",
                dispute);
    }

    private void notifyAppealSubmitted(Dispute dispute) {
        notifyPlatformAdmins(
                "Tranh chấp được mở lại",
                "Tranh chấp #" + dispute.getDisputeId() + " đã được khiếu nại/mở lại với bằng chứng hoặc lý do mới.",
                dispute);
        notifyParticipants(
                dispute,
                "Tranh chấp đã được mở lại",
                "Tranh chấp #" + dispute.getDisputeId() + " đang được xem xét lại theo khiếu nại mới.");
    }

    private void notifyFinalResolution(Dispute dispute, DisputeResolutionAction action) {
        notifyParticipants(
                dispute,
                "Đã có quyết định xử lý tranh chấp",
                "Tranh chấp #" + dispute.getDisputeId() + " đã được xử lý: " + actionLabel(action) + ".");
    }

    private void notifyLegacyResolution(Dispute dispute) {
        notifyParticipants(
                dispute,
                "Đã có quyết định xử lý tranh chấp",
                "Tranh chấp #" + dispute.getDisputeId() + " đã được admin xử lý.");
    }

    private void notifyPlatformAdmins(String title, String content, Dispute dispute) {
        List<PlatformAdmin> admins = platformAdminRepository.findAll();
        if (admins == null || admins.isEmpty()) {
            return;
        }
        for (PlatformAdmin admin : admins) {
            if (admin != null && admin.getUser() != null) {
                createNotification(admin.getUser(), title, content, NotificationType.CLASS, dispute);
            }
        }
    }

    private void notifyParticipants(Dispute dispute, String title, String content) {
        for (User user : collectParticipantUsers(dispute)) {
            createNotification(user, title, content, NotificationType.CLASS, dispute);
        }
    }

    private List<User> collectParticipantUsers(Dispute dispute) {
        List<User> users = new ArrayList<>();
        Set<Long> seenUserIds = new LinkedHashSet<>();

        Report report = dispute.getReport();
        if (report != null) {
            addUser(users, seenUserIds, report.getReporter());
        }

        EscrowTransaction escrow = dispute.getEscrowTransaction();
        if (escrow == null) {
            return users;
        }

        addUser(users, seenUserIds, resolveEscrowPayerUser(escrow));

        ClassAssignment assignment = escrow.getAssignment();
        if (assignment != null) {
            Tutor tutor = assignment.getTutor();
            addUser(users, seenUserIds, tutor != null ? tutor.getUser() : null);
            TutoringClass tutoringClass = assignment.getApplication() != null
                    ? assignment.getApplication().getTutoringClass()
                    : null;
            addClassOwners(users, seenUserIds, tutoringClass);
        }

        ClassStudent classStudent = escrow.getClassStudent();
        if (classStudent != null) {
            addUser(users, seenUserIds, classStudent.getEnrolledByUser());
            addClassOwners(users, seenUserIds, classStudent.getTutoringClass());
        }

        return users;
    }

    private void addClassOwners(List<User> users, Set<Long> seenUserIds, TutoringClass tutoringClass) {
        if (tutoringClass == null) {
            return;
        }
        addUser(users, seenUserIds, tutoringClass.getCreator());
        if (tutoringClass.getCenter() != null) {
            addUser(users, seenUserIds, tutoringClass.getCenter().getUser());
        }
    }

    private void addUser(List<User> users, Set<Long> seenUserIds, User user) {
        if (user == null || user.getUserId() == null || seenUserIds.contains(user.getUserId())) {
            return;
        }
        seenUserIds.add(user.getUserId());
        users.add(user);
    }

    private void createNotification(
            User user,
            String title,
            String content,
            NotificationType type,
            Dispute dispute) {

        notificationDispatchService.notifyUserFromTemplate(
                user,
                type,
                "DISPUTE_EVENT",
                Map.of("title", title, "content", content),
                title,
                content,
                "DISPUTE",
                dispute.getDisputeId());
    }

    private void createReportNotification(
            User user,
            String title,
            String content,
            NotificationType type,
            Report report) {

        notificationDispatchService.notifyUserFromTemplate(
                user,
                type,
                "REPORT_EVENT",
                Map.of("title", title, "content", content),
                title,
                content,
                "REPORT",
                report.getReportId());
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

    private record SettlementAmounts(
            BigDecimal releaseAmount,
            BigDecimal refundAmount,
            boolean usedProRata) {
    }

    private record SettlementSuggestion(
            int totalSessions,
            int completedSessions,
            BigDecimal releaseAmount,
            BigDecimal refundAmount,
            String reason) {
    }
}
