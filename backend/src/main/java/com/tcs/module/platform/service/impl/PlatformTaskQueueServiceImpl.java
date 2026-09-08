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
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.service.PlatformTaskQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * DỊCH VỤ HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (PLATFORM TASK QUEUE SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả chức năng:
 *   - Tập hợp toàn bộ nhiệm vụ chờ xử lý từ các phân hệ khác nhau vào một bảng điều khiển duy nhất:
 *     1. Xác minh danh tính/bằng cấp (Verification)
 *     2. Báo cáo vi phạm & Nghi vấn lách sàn (Report / Circumvention)
 *     3. Yêu cầu hỗ trợ kỹ thuật & khiếu nại (Support Ticket)
 *     4. Yêu cầu rút tiền số dư ví (Withdrawal)
 *     5. Yêu cầu hoàn tiền học phí (Refund Request)
 *     6. Tranh chấp ký quỹ Escrow (Dispute)
 *   - Tính toán hạn chót xử lý (SLA Due Date) và cờ cảnh báo quá hạn (SlaBreached).
 *   - Đo lường tổng giá trị tài chính rủi ro (Escrow Exposure / Money At Risk).
 *   - Phân loại, sắp xếp ưu tiên theo độ khẩn cấp (URGENT > HIGH > MEDIUM > LOW).
 */
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

    // =========================================================================
    // LUỒNG 8: TỔNG HỢP HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (UC-56)
    // =========================================================================

    /**
     * Tổng hợp các chỉ số thống kê hàng đợi nhiệm vụ trực ban của Admin.
     * 
     * @return đối tượng TaskQueueSummaryResponse chứa số lượng công việc theo từng nhóm và mức ưu tiên
     */
    // Luồng 8 - Phân vùng 1 & 5: Tổng hợp số lượng công việc tồn đọng (Tickets, Báo cáo, Rút tiền, Tiền rủi ro)
    @Override
    public TaskQueueSummaryResponse getSummary() {
        List<TaskItemResponse> allItems = getAllTasks();
        long pendingVerifications = allItems.stream().filter(t -> "VERIFICATION".equals(t.getTaskType())).count();
        long openReports = allItems.stream().filter(t -> "REPORT".equals(t.getTaskType())).count();
        long openTickets = allItems.stream().filter(t -> "SUPPORT_TICKET".equals(t.getTaskType())).count();
        long pendingWithdrawals = allItems.stream().filter(t -> "WITHDRAWAL".equals(t.getTaskType())).count();
        long pendingRefunds = allItems.stream().filter(t -> "REFUND_REQUEST".equals(t.getTaskType())).count();
        long openDisputes = allItems.stream().filter(t -> "DISPUTE".equals(t.getTaskType())).count();
        long circumventions = allItems.stream().filter(t -> "CIRCUMVENTION".equals(t.getTaskType())).count();

        Map<String, Long> byType = new HashMap<>();
        byType.put("VERIFICATION", pendingVerifications);
        byType.put("REPORT", openReports);
        byType.put("SUPPORT_TICKET", openTickets);
        byType.put("WITHDRAWAL", pendingWithdrawals);
        byType.put("REFUND_REQUEST", pendingRefunds);
        byType.put("DISPUTE", openDisputes);
        byType.put("CIRCUMVENTION", circumventions);

        Map<String, Long> byPriority = allItems.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPriority() != null ? t.getPriority() : "LOW",
                        Collectors.counting()
                ));

        // Đếm tổng số task trễ hạn SLA
        long overdueCount = allItems.stream().filter(t -> Boolean.TRUE.equals(t.getSlaBreached())).count();
        
        // Tính tổng số tiền rủi ro đang bị tranh chấp hoặc chờ hoàn tiền (Escrow Exposure)
        BigDecimal moneyAtRisk = allItems.stream()
                .filter(t -> ("DISPUTE".equals(t.getTaskType()) || "REFUND_REQUEST".equals(t.getTaskType())) && t.getAmount() != null)
                .map(TaskItemResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TaskQueueSummaryResponse.builder()
                .pendingVerifications(pendingVerifications)
                .openReports(openReports + circumventions)
                .openTickets(openTickets)
                .pendingWithdrawals(pendingWithdrawals)
                .pendingRefunds(pendingRefunds)
                .openDisputes(openDisputes)
                .totalPendingTasks(allItems.size())
                .byType(byType)
                .byPriority(byPriority)
                .overdueCount(overdueCount)
                .moneyAtRisk(moneyAtRisk)
                .build();
    }

    @Override
    public PageTaskItemResponse listTasks(String type, String priority, Boolean slaBreached, int page, int size) {
        List<TaskItemResponse> allItems = getAllTasks();
        
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            allItems = allItems.stream()
                    .filter(t -> type.equalsIgnoreCase(t.getTaskType()))
                    .collect(Collectors.toList());
        }

        if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) {
            allItems = allItems.stream()
                    .filter(t -> priority.equalsIgnoreCase(t.getPriority()))
                    .collect(Collectors.toList());
        }

        if (slaBreached != null) {
            allItems = allItems.stream()
                    .filter(t -> Boolean.valueOf(slaBreached).equals(t.getSlaBreached()))
                    .collect(Collectors.toList());
        }

        // Sort by Priority then Due Date
        allItems.sort(Comparator.comparingInt((TaskItemResponse t) -> getPriorityWeight(t.getPriority())).reversed()
                .thenComparing(t -> t.getDueAt() != null ? t.getDueAt() : LocalDateTime.MAX)
                .thenComparing(TaskItemResponse::getCreatedAt));

        int start = page * size;
        int end = Math.min(start + size, allItems.size());
        List<TaskItemResponse> content = start >= allItems.size() ? Collections.emptyList() : allItems.subList(start, end);
        int totalPages = (int) Math.ceil((double) allItems.size() / Math.max(1, size));

        return PageTaskItemResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(allItems.size())
                .totalPages(totalPages)
                .build();
    }
    
    public List<TaskItemResponse> getAllTasks() {
        List<TaskItemResponse> allItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. Verifications
        var verifications = new ArrayList<>(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED));
        verifications.addAll(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.UNDER_REVIEW));
        for (var v : verifications) {
            LocalDateTime created = v.getSubmittedAt() != null ? v.getSubmittedAt() : v.getCreatedAt();
            LocalDateTime dueAt = created.plusHours(48);
            allItems.add(TaskItemResponse.builder()
                    .taskId("VERIF-" + v.getVerificationId())
                    .taskType("VERIFICATION")
                    .title("Hồ sơ xác minh - " + v.getUser().getEmail())
                    .description("Trạng thái: " + v.getStatus().name())
                    .entityId(v.getVerificationId())
                    .targetRoute("/platform/verifications")
                    .targetQuery("?id=" + v.getVerificationId())
                    .status(v.getStatus().name())
                    .priority("MEDIUM")
                    .createdAt(created)
                    .dueAt(dueAt)
                    .slaBreached(now.isAfter(dueAt))
                    .build());
        }

        // 2. Reports & Circumventions
        var reports = reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING);
        for (var r : reports) {
            LocalDateTime created = r.getCreatedAt();
            LocalDateTime dueAt = created.plusHours(24);
            boolean isCircumvention = r.getCategory() == ReportCategory.PLATFORM_CIRCUMVENTION;
            
            allItems.add(TaskItemResponse.builder()
                    .taskId((isCircumvention ? "CIRCUMVENTION-" : "REPORT-") + r.getReportId())
                    .taskType(isCircumvention ? "CIRCUMVENTION" : "REPORT")
                    .title("Báo cáo vi phạm #" + r.getReportId() + " (" + r.getCategory() + ")")
                    .description(r.getDescription())
                    .entityId(r.getReportId())
                    .targetRoute("/platform/reports")
                    .targetQuery("?tab=" + (isCircumvention ? "circumvention" : "reports") + "&id=" + r.getReportId())
                    .status(r.getStatus().name())
                    .priority(isCircumvention ? "HIGH" : "MEDIUM")
                    .createdAt(created)
                    .dueAt(dueAt)
                    .slaBreached(now.isAfter(dueAt))
                    .riskReason(isCircumvention ? "Nguy cơ trốn phí nền tảng" : null)
                    .build());
        }

        // 3. Support Tickets
        var tickets = supportTicketRepository.findByStatusInOrderByCreatedAtAsc(List.of(
                SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.IN_REVIEW));
        for (var t : tickets) {
            LocalDateTime created = t.getCreatedAt();
            int slaHours = ("URGENT".equals(t.getPriority().name()) || "HIGH".equals(t.getPriority().name())) ? 12 : 24;
            LocalDateTime dueAt = created.plusHours(slaHours);
            allItems.add(TaskItemResponse.builder()
                    .taskId("TICKET-" + t.getTicketId())
                    .taskType("SUPPORT_TICKET")
                    .title("Hỗ trợ #" + t.getTicketId() + " - " + t.getSubject())
                    .description(t.getDescription())
                    .entityId(t.getTicketId())
                    .targetRoute("/platform/tickets")
                    .targetQuery("?id=" + t.getTicketId())
                    .status(t.getStatus().name())
                    .priority(t.getPriority().name())
                    .createdAt(created)
                    .dueAt(dueAt)
                    .slaBreached(now.isAfter(dueAt))
                    .build());
        }

        // 4. Withdrawals
        var withdrawals = withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING);
        for (var w : withdrawals) {
            LocalDateTime created = w.getRequestedAt();
            LocalDateTime dueAt = created.plusHours(48);
            allItems.add(TaskItemResponse.builder()
                    .taskId("WITHDRAW-" + w.getWithdrawalId())
                    .taskType("WITHDRAWAL")
                    .title("Yêu cầu rút tiền - " + w.getWallet().getUser().getEmail())
                    .description("Số tiền: " + w.getAmount() + " VND")
                    .entityId(w.getWithdrawalId())
                    .targetRoute("/platform/withdrawals")
                    .targetQuery("?id=" + w.getWithdrawalId())
                    .status(w.getStatus().name())
                    .priority("HIGH")
                    .createdAt(created)
                    .dueAt(dueAt)
                    .slaBreached(now.isAfter(dueAt))
                    .amount(w.getAmount())
                    .currency("VND")
                    .build());
        }

        // 5. Refunds
        var refunds = refundRequestRepository.findByStatusOrderByRequestedAtAsc(RefundRequestStatus.PENDING);
        for (var rf : refunds) {
            LocalDateTime created = rf.getRequestedAt();
            LocalDateTime dueAt = created.plusHours(72);
            allItems.add(TaskItemResponse.builder()
                    .taskId("REFUND-" + rf.getRefundId())
                    .taskType("REFUND_REQUEST")
                    .title("Yêu cầu hoàn tiền - " + rf.getRequestedBy().getEmail())
                    .description("Số tiền: " + rf.getAmount() + " VND - " + rf.getReason())
                    .entityId(rf.getRefundId())
                    .targetRoute("/platform/withdrawals")
                    .targetQuery("?id=REFUND-" + rf.getRefundId())
                    .status(rf.getStatus().name())
                    .priority("HIGH")
                    .createdAt(created)
                    .dueAt(dueAt)
                    .slaBreached(now.isAfter(dueAt))
                    .amount(rf.getAmount())
                    .currency("VND")
                    .build());
        }

        // 6. Disputes
        var disputes = disputeRepository.findByStatusInOrderByCreatedAtAsc(List.of(
                DisputeStatus.OPEN, DisputeStatus.UNDER_INVESTIGATION, DisputeStatus.WAITING));
        for (var d : disputes) {
            LocalDateTime created = d.getCreatedAt();
            LocalDateTime dueAt = created.plusHours(24);
            BigDecimal disputeAmount = d.getEscrowTransaction() != null ? d.getEscrowTransaction().getAmount() : BigDecimal.ZERO;
            allItems.add(TaskItemResponse.builder()
                    .taskId("DISPUTE-" + d.getDisputeId())
                    .taskType("DISPUTE")
                    .title("Tranh chấp thanh toán #" + d.getDisputeId())
                    .description(d.getResolution() != null ? d.getResolution() : "Tranh chấp giao dịch/hợp đồng")
                    .entityId(d.getDisputeId())
                    .targetRoute("/platform/reports")
                    .targetQuery("?tab=disputes&id=" + d.getDisputeId())
                    .status(d.getStatus().name())
                    .priority("URGENT")
                    .createdAt(created)
                    .dueAt(dueAt)
                    .slaBreached(now.isAfter(dueAt))
                    .amount(disputeAmount)
                    .currency("VND")
                    .riskReason("Tiền Escrow có nguy cơ tranh chấp dài hạn")
                    .build());
        }

        return allItems;
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
