package com.tcs.module.platform.service.impl;

import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.platform.dto.response.PageTaskItemResponse;
import com.tcs.module.platform.dto.response.TaskItemResponse;
import com.tcs.module.platform.dto.response.TaskQueueSummaryResponse;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.service.PlatformTaskQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformTaskQueueServiceImpl implements PlatformTaskQueueService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final ReportRepository reportRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final DisputeRepository disputeRepository;

    @Override
    public TaskQueueSummaryResponse getSummary() {
        long pendingVerifications = verificationRequestRepository.countByStatus(VerificationStatus.SUBMITTED) +
                                    verificationRequestRepository.countByStatus(VerificationStatus.UNDER_REVIEW);
        long openReports = reportRepository.countByStatus(ReportStatus.PENDING);
        long openTickets = supportTicketRepository.countByStatusIn(List.of(
                SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.IN_REVIEW));
        long pendingWithdrawals = withdrawalRequestRepository.countByStatus(WithdrawalRequestStatus.PENDING);
        long pendingRefunds = refundRequestRepository.countByStatus(RefundRequestStatus.PENDING);
        long openDisputes = disputeRepository.countByStatusIn(List.of(
                DisputeStatus.OPEN, DisputeStatus.UNDER_INVESTIGATION, DisputeStatus.WAITING));
        long totalPendingTasks = pendingVerifications + openReports + openTickets + pendingWithdrawals + pendingRefunds + openDisputes;

        return TaskQueueSummaryResponse.builder()
                .pendingVerifications(pendingVerifications)
                .openReports(openReports)
                .openTickets(openTickets)
                .pendingWithdrawals(pendingWithdrawals)
                .pendingRefunds(pendingRefunds)
                .openDisputes(openDisputes)
                .totalPendingTasks(totalPendingTasks)
                .build();
    }

    @Override
    public PageTaskItemResponse listTasks(String type, int page, int size) {
        List<TaskItemResponse> allItems = new ArrayList<>();
        boolean all = type == null || type.isBlank() || "ALL".equalsIgnoreCase(type);

        if (all || "VERIFICATION".equalsIgnoreCase(type)) {
            var verifications = new ArrayList<>(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED));
            verifications.addAll(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.UNDER_REVIEW));
            for (var v : verifications) {
                allItems.add(TaskItemResponse.builder()
                        .taskId("VERIF-" + v.getVerificationId())
                        .taskType("VERIFICATION")
                        .title("Hồ sơ xác minh - " + v.getUser().getEmail())
                        .description("Trạng thái: " + v.getStatus().name())
                        .entityId(v.getVerificationId())
                        .targetRoute("/platform/verifications")
                        .status(v.getStatus().name())
                        .priority("MEDIUM")
                        .createdAt(v.getSubmittedAt() != null ? v.getSubmittedAt() : v.getCreatedAt())
                        .build());
            }
        }

        if (all || "REPORT".equalsIgnoreCase(type)) {
            var reports = reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING);
            for (var r : reports) {
                allItems.add(TaskItemResponse.builder()
                        .taskId("REPORT-" + r.getReportId())
                        .taskType("REPORT")
                        .title("Báo cáo vi phạm #" + r.getReportId() + " (" + r.getCategory() + ")")
                        .description(r.getDescription())
                        .entityId(r.getReportId())
                        .targetRoute("/platform/reports")
                        .status(r.getStatus().name())
                        .priority("HIGH")
                        .createdAt(r.getCreatedAt())
                        .build());
            }
        }

        if (all || "SUPPORT_TICKET".equalsIgnoreCase(type)) {
            var tickets = supportTicketRepository.findByStatusInOrderByCreatedAtAsc(List.of(
                    SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.IN_REVIEW));
            for (var t : tickets) {
                allItems.add(TaskItemResponse.builder()
                        .taskId("TICKET-" + t.getTicketId())
                        .taskType("SUPPORT_TICKET")
                        .title("Hỗ trợ #" + t.getTicketId() + " - " + t.getSubject())
                        .description(t.getDescription())
                        .entityId(t.getTicketId())
                        .targetRoute("/platform/tickets")
                        .status(t.getStatus().name())
                        .priority(t.getPriority().name())
                        .createdAt(t.getCreatedAt())
                        .build());
            }
        }

        if (all || "WITHDRAWAL".equalsIgnoreCase(type)) {
            var withdrawals = withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING);
            for (var w : withdrawals) {
                allItems.add(TaskItemResponse.builder()
                        .taskId("WITHDRAW-" + w.getWithdrawalId())
                        .taskType("WITHDRAWAL")
                        .title("Yêu cầu rút tiền - " + w.getWallet().getUser().getEmail())
                        .description("Số tiền: " + w.getAmount() + " VND")
                        .entityId(w.getWithdrawalId())
                        .targetRoute("/finance")
                        .status(w.getStatus().name())
                        .priority("HIGH")
                        .createdAt(w.getRequestedAt())
                        .build());
            }
        }

        if (all || "REFUND_REQUEST".equalsIgnoreCase(type)) {
            var refunds = refundRequestRepository.findByStatusOrderByRequestedAtAsc(RefundRequestStatus.PENDING);
            for (var rf : refunds) {
                allItems.add(TaskItemResponse.builder()
                        .taskId("REFUND-" + rf.getRefundId())
                        .taskType("REFUND_REQUEST")
                        .title("Yêu cầu hoàn tiền - " + rf.getRequestedBy().getEmail())
                        .description("Số tiền: " + rf.getAmount() + " VND - " + rf.getReason())
                        .entityId(rf.getRefundId())
                        .targetRoute("/finance")
                        .status(rf.getStatus().name())
                        .priority("HIGH")
                        .createdAt(rf.getRequestedAt())
                        .build());
            }
        }

        if (all || "DISPUTE".equalsIgnoreCase(type)) {
            var disputes = disputeRepository.findByStatusInOrderByCreatedAtAsc(List.of(
                    DisputeStatus.OPEN, DisputeStatus.UNDER_INVESTIGATION, DisputeStatus.WAITING));
            for (var d : disputes) {
                allItems.add(TaskItemResponse.builder()
                        .taskId("DISPUTE-" + d.getDisputeId())
                        .taskType("DISPUTE")
                        .title("Tranh chấp thanh toán #" + d.getDisputeId())
                        .description(d.getResolution() != null ? d.getResolution() : "Tranh chấp giao dịch/hợp đồng")
                        .entityId(d.getDisputeId())
                        .targetRoute("/finance")
                        .status(d.getStatus().name())
                        .priority("URGENT")
                        .createdAt(d.getCreatedAt())
                        .build());
            }
        }

        allItems.sort(Comparator.comparingInt((TaskItemResponse t) -> getPriorityWeight(t.getPriority())).reversed()
                .thenComparing(TaskItemResponse::getCreatedAt));

        int start = page * size;
        int end = Math.min(start + size, allItems.size());
        List<TaskItemResponse> content = start >= allItems.size() ? Collections.emptyList() : allItems.subList(start, end);
        int totalPages = (int) Math.ceil((double) allItems.size() / size);

        return PageTaskItemResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(allItems.size())
                .totalPages(totalPages)
                .build();
    }

    private int getPriorityWeight(String priority) {
        if (priority == null) return 1;
        return switch (priority.toUpperCase()) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 1;
        };
    }
}
