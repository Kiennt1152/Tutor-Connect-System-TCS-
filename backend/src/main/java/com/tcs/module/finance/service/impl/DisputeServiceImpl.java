package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.DisputeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.security.AuthHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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
