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
 * [UC-64] HÀNG ĐỢI TÁC VỤ TRỰC BAN & ĐIỀU PHỐI NHIỆM VỤ KHẨN CẤP (TASK QUEUE SERVICE)
 * ============================================================================
 * * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Đồng tác giả: tienanh6677 (Nguyễn Tiến Anh)
 * Ngày tạo: 2026-07-29
 * * Mô tả Use Case:
 *   - Tập hợp và điều phối toàn bộ các nhiệm vụ cần xử lý khẩn cấp từ các phân hệ vào một hàng đợi duy nhất.
 *   - Giúp Quản trị viên trực ban phản ứng nhanh với các vấn đề an ninh, tài chính và trải nghiệm người dùng.
 * * Chức năng chính:
 *   1. Tập hợp đa nguồn nhiệm vụ: Xác minh danh tính KYC, Báo cáo vi phạm & Lách sàn, Ticket hỗ trợ, Yêu cầu rút tiền, Hoàn tiền học phí, Tranh chấp ký quỹ.
 *   2. Tính toán cam kết dịch vụ (SLA): Xác định hạn chót xử lý (Due Date) và cờ vi phạm cam kết (slaBreached).
 *   3. Đo lường giá trị rủi ro tài chính: Tính tổng tiền ký quỹ/giao dịch đang bị phong tỏa chờ quyết định (Money At Risk).
 *   4. Phân cấp mức độ ưu tiên: Tự động gắn nhãn mức độ ưu tiên nghiệp vụ (URGENT > HIGH > MEDIUM > LOW).
 *   5. Hỗ trợ phân trang và lọc: Lọc tác vụ theo phân hệ, mức độ ưu tiên, tình trạng vi phạm hạn xử lý.
 * * Luồng xử lý chính:
 *   - Bước 1: Admin tải bảng thông số tổng quan (`getSummary`), tính tổng task tồn đọng và tiền rủi ro.
 *   - Bước 2: Tải danh sách tác vụ phân trang (`getTasks`), hệ thống truy vấn từ 6 repository tương ứng.
 *   - Bước 3: Chuẩn hóa dữ liệu sang cấu trúc `TaskItemResponse`, tính toán SLA và độ ưu tiên.
 *   - Bước 4: Sắp xếp danh sách theo thứ tự khẩn cấp và trả về cho bàn trực ban của Quản trị viên.
 * ============================================================================
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
    // LUỒNG 8: TỔNG HỢP HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (UC-64)
    // =========================================================================

    /**
     * [UC-64] Tổng hợp các chỉ số thống kê hàng đợi nhiệm vụ trực ban của Quản trị viên.
     * 
     * Luồng xử lý:
     * 1. Thu thập toàn bộ tác vụ đang mở từ 6 repository nguồn qua getAllTasks().
     * 2. Gom nhóm và đếm số lượng công việc theo từng phân hệ nghiệp vụ.
     * 3. Phân loại theo thang mức ưu tiên (URGENT, HIGH, MEDIUM, LOW).
     * 4. Tính toán tổng số lượng tác vụ đã vượt quá ngưỡng cam kết dịch vụ (SLA Breached).
     * 5. Tổng hợp tổng số tiền rủi ro (Money At Risk) từ các vụ tranh chấp Escrow và yêu cầu hoàn tiền đang chờ duyệt.
     * 
     * @return TaskQueueSummaryResponse chứa số lượng công việc theo từng nhóm, mức ưu tiên, số vụ quá hạn và tiền rủi ro
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

    /**
     * [UC-64] Lấy danh sách nhiệm vụ trực ban được phân trang và lọc theo đa tiêu chí.
     * 
     * Luồng xử lý:
     * 1. Thu thập toàn bộ danh sách tác vụ đang chờ xử lý từ tất cả các phân hệ.
     * 2. Áp dụng bộ lọc loại tác vụ (type) nếu được chỉ định và khác "ALL".
     * 3. Áp dụng bộ lọc mức độ ưu tiên (priority) nếu được chỉ định và khác "ALL".
     * 4. Lọc theo trạng thái vi phạm hạn xử lý SLA (slaBreached) nếu có giá trị.
     * 5. Sắp xếp danh sách: Ưu tiên cao nhất lên đầu -> Hạn xử lý gần nhất -> Thời gian tạo sớm nhất.
     * 6. Phân trang dữ liệu theo tham số page và size, đóng gói phản hồi PageTaskItemResponse.
     * 
     * @param type Phân loại tác vụ cần lọc (VERIFICATION, REPORT, SUPPORT_TICKET, WITHDRAWAL, REFUND_REQUEST, DISPUTE, CIRCUMVENTION hoặc ALL)
     * @param priority Mức độ ưu tiên cần lọc (URGENT, HIGH, MEDIUM, LOW hoặc ALL)
     * @param slaBreached Trạng thái vi phạm cam kết SLA
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số phần tử trên mỗi trang
     * @return PageTaskItemResponse chứa danh sách công việc cùng thông tin phân trang
     */
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
    /**
     * [UC-64] Tập hợp toàn bộ nhiệm vụ tồn đọng từ 6 phân hệ nghiệp vụ và chuẩn hóa SLA.
     * 
     * Luồng xử lý:
     * 1. Truy vấn hồ sơ xác minh KYC đang ở trạng thái SUBMITTED hoặc UNDER_REVIEW (SLA: 48h, độ ưu tiên: MEDIUM).
     * 2. Truy vấn báo cáo vi phạm PENDING, nhận diện báo cáo lách sàn PLATFORM_CIRCUMVENTION (SLA: 24h, độ ưu tiên: HIGH/MEDIUM).
     * 3. Truy vấn các Support Ticket đang mở (OPEN, IN_PROGRESS, IN_REVIEW) với SLA 12h cho URGENT/HIGH, 24h cho loại khác.
     * 4. Truy vấn các yêu cầu rút tiền ví PENDING (SLA: 48h, độ ưu tiên: HIGH).
     * 5. Truy vấn các yêu cầu hoàn tiền học phí PENDING (SLA: 72h, độ ưu tiên: HIGH).
     * 6. Truy vấn các vụ tranh chấp giao dịch OPEN, UNDER_INVESTIGATION, WAITING (SLA: 24h, độ ưu tiên: URGENT, kèm số tiền rủi ro).
     * 
     * @return Danh sách TaskItemResponse chứa toàn bộ các tác vụ đang cần Quản trị viên xử lý
     */
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
                    .targetRoute(isCircumvention ? "/platform/circumvention" : "/platform/reports")
                    .targetQuery(isCircumvention ? "?status=PENDING" : ("?tab=reports&id=" + r.getReportId()))
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
