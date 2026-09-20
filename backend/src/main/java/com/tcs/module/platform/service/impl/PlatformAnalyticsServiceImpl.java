package com.tcs.module.platform.service.impl;

import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.EscrowFlowResponse;
import com.tcs.module.platform.dto.response.MonthlyMetricResponse;
import com.tcs.module.platform.dto.response.TransactionTypeBreakdown;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * DỊCH VỤ PHÂN TÍCH VÀ THỐNG KÊ TOÀN DIỆN NỀN TẢNG (PLATFORM ANALYTICS SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả chức năng:
 *   - Tổng hợp số liệu vận hành: Người dùng, Gia sư, Phụ huynh, Trung tâm, Lớp học, Hợp đồng, Tranh chấp.
 *   - Thống kê dòng tiền tài chính: Tổng tiền nạp (Deposit), Rút (Withdrawal), Escrow (Ký quỹ, Giải ngân, Hoàn trả), Doanh thu phí sàn.
 *   - Lập báo cáo tự động định kỳ (Scheduled Daily Report) phục vụ kiểm toán và theo dõi hiệu suất.
 *   - Xuất dữ liệu CSV an toàn: Giới hạn số dòng chống tràn bộ nhớ (OOM), chèn UTF-8 BOM cho Excel tiếng Việt,
 *     và lọc chống lỗ hổng CSV Formula Injection (DDE Injection).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAnalyticsServiceImpl implements PlatformAnalyticsService {
    /** Tiền tố mã giao dịch thu phí nền tảng */
    private static final String PLATFORM_FEE_REFERENCE_PREFIX = "PLATFORM_FEE-";

    /** Giới hạn số dòng tối đa khi xuất báo cáo CSV nhằm chống nghẽn bộ nhớ */
    private static final int MAX_EXPORT_ROWS = 10_000;
    /** Khoảng thời gian xuất báo cáo mặc định (90 ngày gần nhất) */
    private static final int DEFAULT_EXPORT_DAYS = 90;

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final DisputeRepository disputeRepository;
    private final ContractRepository contractRepository;
    private final SystemParameterRepository systemParameterRepository;
    private final com.tcs.module.platform.service.AuditLogService auditLogService;

    /**
     * Tác vụ tự động sinh báo cáo tổng kết hàng ngày và ghi nhận vào Audit Log.
     * 
     * @return số lượng báo cáo được tạo (1 nếu thành công)
     */
    @Override
    @Transactional
    public int generateScheduledDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        AnalyticsSummaryResponse summary = getSummary(yesterday, LocalDate.now());
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("reportDate", String.valueOf(yesterday));
        meta.put("totalUsers", summary.getTotalUsers());
        meta.put("totalClasses", summary.getTotalClasses());
        meta.put("totalRevenue", summary.getPlatformRevenue() != null ? summary.getPlatformRevenue().toString() : "0");
        meta.put("activeEscrow", summary.getEscrowHeld() != null ? summary.getEscrowHeld().toString() : "0");
        auditLogService.record("SCHEDULED_REPORT_GENERATION", "ScheduledAnalyticsReport", 0L, null, meta);
        return 1;
    }

    @Override
    public AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : null;

        long totalUsers = (fromDt != null && toDt != null)
                ? userRepository.countByCreatedAtBetween(fromDt, toDt)
                : userRepository.count();
        long totalTutors = tutorRepository.count();
        long totalParents = clientRepository.count();
        long totalCenters = tutorCenterRepository.count();
        long totalStudents = Math.max(0, totalUsers - totalTutors - totalParents - totalCenters);

        long totalClasses = (fromDt != null && toDt != null)
                ? tutoringClassRepository.countByCreatedAtBetween(fromDt, toDt)
                : tutoringClassRepository.count();

        long activeClasses = tutoringClassRepository.countByStatusIn(
                List.of(TutoringClassStatus.IN_PROGRESS, TutoringClassStatus.OPEN, TutoringClassStatus.MATCHED)
        );
        long completedClasses = tutoringClassRepository.countByStatus(TutoringClassStatus.COMPLETED);

        List<PaymentTransaction> allTransactions = paymentTransactionRepository.findAll().stream()
                .filter(item -> inRange(item.getCreatedAt(), from, to)).toList();
        BigDecimal totalRevenue = allTransactions.stream()
                .filter(pt -> pt.getStatus() == PaymentTransactionStatus.SUCCESS
                           && (pt.getType() == PaymentTransactionType.DEPOSIT
                            || pt.getType() == PaymentTransactionType.ESCROW_DEPOSIT)
                           && !isPlatformFeeTransaction(pt))
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformFeeRate = new BigDecimal("0.02");
        Optional<SystemParameter> paramOpt = systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE");
        if (paramOpt.isPresent() && paramOpt.get().getParamValue() != null) {
            try {
                platformFeeRate = new BigDecimal(paramOpt.get().getParamValue().trim());
            } catch (Exception ignored) {}
        }
        BigDecimal platformFeeRevenue = sumPlatformFeeTransactions(allTransactions);
        BigDecimal deposits = sumTransactions(allTransactions, PaymentTransactionType.DEPOSIT);
        BigDecimal withdrawals = sumTransactions(allTransactions, PaymentTransactionType.WITHDRAWAL);
        BigDecimal escrowDeposited = sumTransactions(allTransactions, PaymentTransactionType.ESCROW_DEPOSIT);
        BigDecimal escrowReleased = sumTransactions(allTransactions, PaymentTransactionType.ESCROW_RELEASE);
        BigDecimal escrowRefunded = sumTransactions(allTransactions, PaymentTransactionType.REFUND);
        BigDecimal escrowHeld = escrowDeposited
                .subtract(escrowReleased)
                .subtract(escrowRefunded)
                .subtract(platformFeeRevenue)
                .max(BigDecimal.ZERO);

        long totalVerif = verificationRequestRepository.count();
        long approvedVerif = verificationRequestRepository.countByStatus(VerificationStatus.VERIFIED);
        double verificationConversionRate = totalVerif == 0 ? 0.0 : (double) approvedVerif / totalVerif * 100.0;

        long totalTx = (fromDt != null && toDt != null)
                ? paymentTransactionRepository.countByCreatedAtBetween(fromDt, toDt)
                : paymentTransactionRepository.count();
        long totalDisputes = disputeRepository.count();
        double disputeRate = totalTx == 0 ? 0.0 : (double) totalDisputes / totalTx * 100.0;

        long totalContracts = contractRepository.count();
        long completedContracts = contractRepository.countByStatus(ContractStatus.COMPLETED);
        double contractCompletionRate = totalContracts == 0 ? 0.0 : (double) completedContracts / totalContracts * 100.0;

        List<MonthlyMetricResponse> monthlyMetrics = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay();

            long newUsers = userRepository.countByCreatedAtBetween(monthStart, monthEnd);
            long newClasses = tutoringClassRepository.countByCreatedAtBetween(monthStart, monthEnd);
            List<PaymentTransaction> monthTransactions =
                    paymentTransactionRepository.findByCreatedAtBetween(monthStart, monthEnd);
            BigDecimal revenue = sumTransactions(monthTransactions, PaymentTransactionType.DEPOSIT)
                    .add(sumTransactions(monthTransactions, PaymentTransactionType.ESCROW_DEPOSIT));

            String label = "T" + ym.getMonthValue() + "/" + ym.getYear();

            monthlyMetrics.add(MonthlyMetricResponse.builder()
                    .month(label)
                    .newUsers(newUsers)
                    .newClasses(newClasses)
                    .revenue(revenue)
                    .build());
        }

        // --- Money In / Out / Net ---
        BigDecimal moneyIn = deposits.add(escrowDeposited).add(platformFeeRevenue);
        BigDecimal moneyOut = withdrawals.add(escrowRefunded);
        BigDecimal netMovement = moneyIn.subtract(moneyOut);

        // --- Escrow Flow ---
        EscrowFlowResponse escrowFlow = EscrowFlowResponse.builder()
                .deposited(escrowDeposited)
                .released(escrowReleased)
                .refunded(escrowRefunded)
                .held(escrowHeld)
                .platformFee(platformFeeRevenue)
                .build();

        // --- Transaction Type Breakdown ---
        List<TransactionTypeBreakdown> breakdown = new ArrayList<>();
        for (PaymentTransactionType txType : PaymentTransactionType.values()) {
            long count = countTransactions(allTransactions, txType);
            BigDecimal sum = sumTransactions(allTransactions, txType);
            boolean isMoneyIn = txType == PaymentTransactionType.DEPOSIT
                    || txType == PaymentTransactionType.ESCROW_DEPOSIT;
            breakdown.add(TransactionTypeBreakdown.builder()
                    .type(txType.name())
                    .count((int) count)
                    .totalAmount(sum)
                    .direction(isMoneyIn ? "IN" : "OUT")
                    .build());
        }
        breakdown.add(TransactionTypeBreakdown.builder()
                .type("PLATFORM_FEE")
                .count((int) countPlatformFeeTransactions(allTransactions))
                .totalAmount(platformFeeRevenue)
                .direction("IN")
                .build());

        return AnalyticsSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalTutors(totalTutors)
                .totalParents(totalParents)
                .totalStudents(totalStudents)
                .totalCenters(totalCenters)
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .completedClasses(completedClasses)
                .totalRevenue(totalRevenue)
                .platformFeeRevenue(platformFeeRevenue)
                .platformFeeRate(platformFeeRate)
                .deposits(deposits)
                .withdrawals(withdrawals)
                .escrowHeld(escrowHeld)
                .escrowReleased(escrowReleased)
                .escrowRefunded(escrowRefunded)
                .moneyIn(moneyIn)
                .moneyOut(moneyOut)
                .netMovement(netMovement)
                .escrowFlow(escrowFlow)
                .transactionTypeBreakdown(breakdown)
                .verificationConversionRate(verificationConversionRate)
                .disputeRate(disputeRate)
                .contractCompletionRate(contractCompletionRate)
                .monthlyMetrics(monthlyMetrics)
                .build();
    }

    // =========================================================================
    // LUỒNG 12: BÁO CÁO TÀI CHÍNH ĐA CHIỀU & XUẤT DỮ LIỆU CSV AN TOÀN (UC-41, UC-43)
    // =========================================================================

    // Luồng 12 - Xuất dữ liệu CSV an toàn chống OOM và mã độc Formula Injection
    @Override
    public byte[] exportCsv(String type, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        // Luồng 12 - Bước 1: Chèn UTF-8 BOM Header (\uFEFF) giúp Microsoft Excel hiển thị đúng Tiếng Việt có dấu
        sb.append("\uFEFF");

        // Luồng 12 - Bước 2: Giới hạn mặc định 90 ngày và tối đa 10,000 dòng chống tràn RAM máy chủ (OOM Protection)
        LocalDateTime fromDt = from != null
                ? from.atStartOfDay()
                : LocalDate.now().minusDays(DEFAULT_EXPORT_DAYS).atStartOfDay();
        LocalDateTime toDt = to != null
                ? to.plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        Pageable exportLimit = PageRequest.of(0, MAX_EXPORT_ROWS);

        if ("classes".equalsIgnoreCase(type)) {
            sb.append("ID,Tiêu đề,Môn học,Trạng thái,Học phí (VND),Ngày tạo\n");
            List<TutoringClass> classes = tutoringClassRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromDt, toDt, exportLimit);
            for (TutoringClass c : classes) {
                sb.append(c.getClassId()).append(",")
                  .append(escapeCsv(c.getTitle())).append(",")
                  .append(c.getSubject() != null ? escapeCsv(c.getSubject().getSubjectName()) : "").append(",")
                  .append(c.getStatus()).append(",")
                  .append(c.getTuitionFee() != null ? c.getTuitionFee() : "0").append(",")
                  .append(c.getCreatedAt()).append("\n");
            }
        } else if ("revenue".equalsIgnoreCase(type)) {
            sb.append("ID,Mã tham chiếu,Loại giao dịch,Số tiền (VND),Trạng thái,Ngày giao dịch\n");
            List<PaymentTransaction> transactions = paymentTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromDt, toDt, exportLimit);
            for (PaymentTransaction pt : transactions) {
                sb.append(pt.getTransactionId()).append(",")
                  .append(escapeCsv(pt.getReferenceCode())).append(",")
                  .append(displayTransactionType(pt)).append(",")
                  .append(pt.getAmount()).append(",")
                  .append(pt.getStatus()).append(",")
                  .append(pt.getCreatedAt()).append("\n");
            }
        } else if ("cashflow".equalsIgnoreCase(type)) {
            sb.append("Ngày,Tiền vào (VND),Tiền ra (VND),Ròng (VND)\n");
            java.util.Map<LocalDate, BigDecimal[]> daily = new java.util.TreeMap<>();
            List<PaymentTransaction> allTransactions = paymentTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromDt, toDt, exportLimit);
            for (PaymentTransaction pt : allTransactions) {
                if (pt.getStatus() != PaymentTransactionStatus.SUCCESS || pt.getCreatedAt() == null) continue;
                LocalDate day = pt.getCreatedAt().toLocalDate();
                daily.computeIfAbsent(day, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal amt = pt.getAmount() != null ? pt.getAmount() : BigDecimal.ZERO;
                boolean isIn = pt.getType() == PaymentTransactionType.DEPOSIT
                        || pt.getType() == PaymentTransactionType.ESCROW_DEPOSIT;
                if (isIn) daily.get(day)[0] = daily.get(day)[0].add(amt);
                else daily.get(day)[1] = daily.get(day)[1].add(amt);
            }
            for (var entry : daily.entrySet()) {
                BigDecimal in = entry.getValue()[0];
                BigDecimal out = entry.getValue()[1];
                sb.append(entry.getKey()).append(",").append(in).append(",").append(out).append(",").append(in.subtract(out)).append("\n");
            }
        } else if ("transaction-breakdown".equalsIgnoreCase(type)) {
            sb.append("Loại giao dịch,Hướng,Số lượng,Tổng tiền (VND)\n");
            List<PaymentTransaction> allTransactions = paymentTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromDt, toDt, exportLimit);
            for (PaymentTransactionType txType : PaymentTransactionType.values()) {
                List<PaymentTransaction> filtered = allTransactions.stream()
                        .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCESS
                                && t.getType() == txType
                                && !isPlatformFeeTransaction(t))
                        .toList();
                BigDecimal sum = filtered.stream().map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                boolean isIn = txType == PaymentTransactionType.DEPOSIT
                        || txType == PaymentTransactionType.ESCROW_DEPOSIT;
                sb.append(txType.name()).append(",").append(isIn ? "IN" : "OUT").append(",")
                  .append(filtered.size()).append(",").append(sum).append("\n");
            }
            List<PaymentTransaction> platformFeeTransactions = allTransactions.stream()
                    .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCESS && isPlatformFeeTransaction(t))
                    .toList();
            BigDecimal platformFeeSum = platformFeeTransactions.stream().map(PaymentTransaction::getAmount)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            sb.append("PLATFORM_FEE,IN,")
                    .append(platformFeeTransactions.size()).append(",")
                    .append(platformFeeSum).append("\n");
        } else {
            sb.append("ID,Email,Số điện thoại,Trạng thái,Ngày tạo\n");
            List<User> users = userRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromDt, toDt, exportLimit);
            for (User u : users) {
                sb.append(u.getUserId()).append(",")
                  .append(escapeCsv(u.getEmail())).append(",")
                  .append(escapeCsv(u.getPhone() != null ? u.getPhone() : "")).append(",")
                  .append(u.getStatus()).append(",")
                  .append(u.getCreatedAt()).append("\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BigDecimal sumTransactions(List<PaymentTransaction> transactions, PaymentTransactionType type) {
        return transactions.stream()
                .filter(item -> item.getStatus() == PaymentTransactionStatus.SUCCESS
                        && item.getType() == type
                        && !isPlatformFeeTransaction(item))
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countTransactions(List<PaymentTransaction> transactions, PaymentTransactionType type) {
        return transactions.stream()
                .filter(item -> item.getStatus() == PaymentTransactionStatus.SUCCESS
                        && item.getType() == type
                        && !isPlatformFeeTransaction(item))
                .count();
    }

    private BigDecimal sumPlatformFeeTransactions(List<PaymentTransaction> transactions) {
        return transactions.stream()
                .filter(item -> item.getStatus() == PaymentTransactionStatus.SUCCESS
                        && isPlatformFeeTransaction(item))
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countPlatformFeeTransactions(List<PaymentTransaction> transactions) {
        return transactions.stream()
                .filter(item -> item.getStatus() == PaymentTransactionStatus.SUCCESS
                        && isPlatformFeeTransaction(item))
                .count();
    }

    private boolean isPlatformFeeTransaction(PaymentTransaction transaction) {
        return transaction != null
                && transaction.getReferenceCode() != null
                && transaction.getReferenceCode().startsWith(PLATFORM_FEE_REFERENCE_PREFIX);
    }

    private boolean inRange(LocalDateTime value, LocalDate from, LocalDate to) {
        if (value == null) return from == null && to == null;
        return (from == null || !value.toLocalDate().isBefore(from))
                && (to == null || !value.toLocalDate().isAfter(to));
    }

    private String displayTransactionType(PaymentTransaction transaction) {
        return isPlatformFeeTransaction(transaction) ? "PLATFORM_FEE" : transaction.getType().name();
    }

    // Luồng 12 - Thuật toán phòng chống tấn công CSV Injection (DDE Injection Sanitization)
    private String escapeCsv(String val) {
        if (val == null) return "";
        String clean = val.replace("\"", "\"\"");
        // Vô hiệu hóa việc thực thi công thức bằng cách chèn thêm dấu nháy đơn (') phía trước
        if (!clean.isEmpty() && "=+-@\t".indexOf(clean.charAt(0)) >= 0) {
            clean = "'" + clean;
        }
        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}
