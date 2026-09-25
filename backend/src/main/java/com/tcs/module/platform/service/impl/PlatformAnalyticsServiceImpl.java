package com.tcs.module.platform.service.impl;

import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.entity.CenterTutorMembership;
import com.tcs.module.center.repository.CenterTutorMembershipRepository;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.CenterFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.ClientFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.EscrowFlowResponse;
import com.tcs.module.platform.dto.response.FinancialLedgerItemResponse;
import com.tcs.module.platform.dto.response.MonthlyMetricResponse;
import com.tcs.module.platform.dto.response.TransactionTypeBreakdown;
import com.tcs.module.platform.dto.response.TutorFinancialAnalyticsResponse;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * [UC-41] [UC-43] DỊCH VỤ PHÂN TÍCH VÀ THỐNG KÊ TOÀN DIỆN NỀN TẢNG (PLATFORM ANALYTICS SERVICE)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-07-29
 * * 1. Mục đích & Chức năng:
 *   - Tổng hợp số liệu vận hành: Người dùng, Gia sư, Phụ huynh, Trung tâm, Lớp học, Hợp đồng, Tranh chấp.
 *   - Thống kê dòng tiền tài chính: Tổng tiền nạp (Deposit), Rút (Withdrawal), Escrow (Ký quỹ, Giải ngân, Hoàn trả), Doanh thu phí sàn.
 *   - Phân tích chi tiết theo từng Trung tâm, Gia sư, Phụ huynh và Sổ cái giao dịch thời gian thực.
 *   - Lập báo cáo tự động định kỳ (Scheduled Daily Report) phục vụ kiểm toán và theo dõi hiệu suất.
 *   - Xuất dữ liệu CSV an toàn: Giới hạn số dòng chống tràn bộ nhớ (OOM), chèn UTF-8 BOM cho Excel tiếng Việt,
 *     và lọc chống lỗ hổng CSV Formula Injection (DDE Injection).
 * * 2. Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận khoảng thời gian lọc (TimeFilterValue) từ Admin.
 *   - Bước 2: Truy vấn dữ liệu tài chính đa nguồn từ PaymentTransaction, EscrowTransaction và FinancialJournal.
 *   - Bước 3: Tính toán tỷ suất tăng trưởng, doanh thu ròng, phí sàn và số dư bảo chứng chưa giải ngân.
 *   - Bước 4: Trả về DTO tổng hợp hoặc xuất tệp CSV báo cáo tải về.
 * ============================================================================
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
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final com.tcs.module.platform.service.AuditLogService auditLogService;
    private final WalletRepository walletRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final CenterTutorMembershipRepository centerTutorMembershipRepository;
    private final ReviewRepository reviewRepository;
    private final PlatformAdminRepository platformAdminRepository;

    /**
     * Tác vụ tự động sinh báo cáo tổng kết hàng ngày và ghi nhận vào Audit Log.
     *     * @return số lượng báo cáo được tạo (1 nếu thành công)
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
        BigDecimal pendingWithdrawals = allTransactions.stream()
                .filter(item -> item.getType() == PaymentTransactionType.WITHDRAWAL
                        && item.getStatus() == PaymentTransactionStatus.PENDING)
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal escrowDeposited = sumTransactions(allTransactions, PaymentTransactionType.ESCROW_DEPOSIT);
        BigDecimal escrowReleased = sumTransactions(allTransactions, PaymentTransactionType.ESCROW_RELEASE);
        BigDecimal escrowRefundedTx = sumTransactions(allTransactions, PaymentTransactionType.REFUND);
        // UC-41 Fix: Dùng bảng escrow_transactions (status=REFUNDED) làm nguồn chính xác nhất
        // vì escrow table phản ánh trạng thái thực tế của giao dịch ký quỹ.
        // Nếu PaymentTransaction REFUND cao hơn (hoàn trực tiếp ngoài escrow), cộng phần chênh lệch.
        BigDecimal escrowRefundedFromEscrows = escrowTransactionRepository.findAll().stream()
                .filter(e -> e.getStatus() == EscrowStatus.REFUNDED)
                .filter(e -> inRange(e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt(), from, to))
                .map(EscrowTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Lấy giá trị lớn hơn giữa 2 nguồn để đảm bảo không bỏ sót bất kỳ khoản hoàn nào.
        // Nếu cả 2 nguồn đều ghi nhận cùng 1 khoản refund, max() tránh double-count.
        // Nếu chỉ 1 nguồn ghi nhận (ví dụ escrow REFUNDED nhưng chưa tạo PaymentTransaction),
        // max() vẫn bắt được.
        BigDecimal escrowRefunded = escrowRefundedTx.compareTo(escrowRefundedFromEscrows) >= 0
                ? escrowRefundedTx : escrowRefundedFromEscrows;
        // Fallback: nếu cả 2 đều = 0 nhưng tồn tại escrow REFUNDED không lọc được theo date range,
        // thử không lọc theo ngày
        if (escrowRefunded.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal escrowRefundedAllTime = escrowTransactionRepository.findAll().stream()
                    .filter(e -> e.getStatus() == EscrowStatus.REFUNDED)
                    .map(EscrowTransaction::getAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (escrowRefundedAllTime.compareTo(BigDecimal.ZERO) > 0 && from == null && to == null) {
                escrowRefunded = escrowRefundedAllTime;
            }
        }
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
        BigDecimal moneyIn = deposits.add(escrowDeposited);
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

        // --- Transaction Type Breakdown (Phân loại rõ ràng theo Tiền vào IN và Tiền ra OUT) ---
        List<TransactionTypeBreakdown> breakdown = new ArrayList<>();

        // 1. Nhóm Dòng tiền vào Sàn (INFLOW)
        breakdown.add(TransactionTypeBreakdown.builder()
                .type(PaymentTransactionType.DEPOSIT.name())
                .label(getTxTypeLabel(PaymentTransactionType.DEPOSIT))
                .count(countTransactions(allTransactions, PaymentTransactionType.DEPOSIT))
                .totalAmount(sumTransactions(allTransactions, PaymentTransactionType.DEPOSIT))
                .direction("IN")
                .build());

        breakdown.add(TransactionTypeBreakdown.builder()
                .type(PaymentTransactionType.ESCROW_DEPOSIT.name())
                .label(getTxTypeLabel(PaymentTransactionType.ESCROW_DEPOSIT))
                .count(countTransactions(allTransactions, PaymentTransactionType.ESCROW_DEPOSIT))
                .totalAmount(sumTransactions(allTransactions, PaymentTransactionType.ESCROW_DEPOSIT))
                .direction("IN")
                .build());

        breakdown.add(TransactionTypeBreakdown.builder()
                .type("PLATFORM_FEE")
                .label("Phí dịch vụ nền tảng (2%)")
                .count(countPlatformFeeTransactions(allTransactions))
                .totalAmount(platformFeeRevenue)
                .direction("IN")
                .build());

        // 2. Nhóm Dòng tiền ra khỏi Sàn (OUTFLOW)
        breakdown.add(TransactionTypeBreakdown.builder()
                .type(PaymentTransactionType.WITHDRAWAL.name())
                .label(getTxTypeLabel(PaymentTransactionType.WITHDRAWAL))
                .count(countTransactions(allTransactions, PaymentTransactionType.WITHDRAWAL))
                .totalAmount(sumTransactions(allTransactions, PaymentTransactionType.WITHDRAWAL))
                .direction("OUT")
                .build());

        breakdown.add(TransactionTypeBreakdown.builder()
                .type(PaymentTransactionType.ESCROW_RELEASE.name())
                .label(getTxTypeLabel(PaymentTransactionType.ESCROW_RELEASE))
                .count(countTransactions(allTransactions, PaymentTransactionType.ESCROW_RELEASE))
                .totalAmount(sumTransactions(allTransactions, PaymentTransactionType.ESCROW_RELEASE))
                .direction("OUT")
                .build());

        breakdown.add(TransactionTypeBreakdown.builder()
                .type(PaymentTransactionType.REFUND.name())
                .label(getTxTypeLabel(PaymentTransactionType.REFUND))
                .count(countTransactions(allTransactions, PaymentTransactionType.REFUND))
                .totalAmount(sumTransactions(allTransactions, PaymentTransactionType.REFUND))
                .direction("OUT")
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
                .pendingWithdrawals(pendingWithdrawals)
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

            // 1. Nhóm Tiền vào (IN)
            PaymentTransactionType[] inTypes = { PaymentTransactionType.DEPOSIT, PaymentTransactionType.ESCROW_DEPOSIT };
            for (PaymentTransactionType txType : inTypes) {
                List<PaymentTransaction> filtered = allTransactions.stream()
                        .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCESS
                                && t.getType() == txType
                                && !isPlatformFeeTransaction(t))
                        .toList();
                BigDecimal sum = filtered.stream().map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                sb.append(escapeCsv(getTxTypeLabel(txType))).append(",IN,")
                  .append(filtered.size()).append(",").append(sum).append("\n");
            }
            List<PaymentTransaction> platformFeeTransactions = allTransactions.stream()
                    .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCESS && isPlatformFeeTransaction(t))
                    .toList();
            BigDecimal platformFeeSum = platformFeeTransactions.stream().map(PaymentTransaction::getAmount)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            sb.append("Phí dịch vụ nền tảng (2%),IN,")
                    .append(platformFeeTransactions.size()).append(",")
                    .append(platformFeeSum).append("\n");

            // 2. Nhóm Tiền ra (OUT)
            PaymentTransactionType[] outTypes = { PaymentTransactionType.WITHDRAWAL, PaymentTransactionType.ESCROW_RELEASE, PaymentTransactionType.REFUND };
            for (PaymentTransactionType txType : outTypes) {
                List<PaymentTransaction> filtered = allTransactions.stream()
                        .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCESS
                                && t.getType() == txType
                                && !isPlatformFeeTransaction(t))
                        .toList();
                BigDecimal sum = filtered.stream().map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                sb.append(escapeCsv(getTxTypeLabel(txType))).append(",OUT,")
                  .append(filtered.size()).append(",").append(sum).append("\n");
            }
        } else if ("centers".equalsIgnoreCase(type)) {
            sb.append("ID,Tên trung tâm,Số giấy phép,Số điện thoại,Email,Tổng lớp,Lớp đang học,Lớp hoàn thành,Gia sư trực thuộc,Gia sư mới,Tiền vào (VND),Tiền ra (VND),Ký quỹ giữ (VND),Đã giải ngân (VND),Phí sàn nộp (VND),Số dư ví (VND)\n");
            List<CenterFinancialAnalyticsResponse> centers = getCenterAnalytics(from, to);
            for (CenterFinancialAnalyticsResponse c : centers) {
                sb.append(c.getCenterId()).append(",")
                  .append(escapeCsv(c.getCompanyName())).append(",")
                  .append(escapeCsv(c.getLicenseNo() != null ? c.getLicenseNo() : "")).append(",")
                  .append(escapeCsv(c.getPhone() != null ? c.getPhone() : "")).append(",")
                  .append(escapeCsv(c.getEmail() != null ? c.getEmail() : "")).append(",")
                  .append(c.getTotalClasses()).append(",")
                  .append(c.getActiveClasses()).append(",")
                  .append(c.getCompletedClasses()).append(",")
                  .append(c.getTotalTutors()).append(",")
                  .append(c.getNewTutorsInPeriod()).append(",")
                  .append(c.getMoneyIn() != null ? c.getMoneyIn() : "0").append(",")
                  .append(c.getMoneyOut() != null ? c.getMoneyOut() : "0").append(",")
                  .append(c.getEscrowHeld() != null ? c.getEscrowHeld() : "0").append(",")
                  .append(c.getEscrowReleased() != null ? c.getEscrowReleased() : "0").append(",")
                  .append(c.getPlatformFeePaid() != null ? c.getPlatformFeePaid() : "0").append(",")
                  .append(c.getWalletBalance() != null ? c.getWalletBalance() : "0").append("\n");
            }
        } else if ("tutors".equalsIgnoreCase(type)) {
            sb.append("ID,Họ tên,Email,Số điện thoại,Trạng thái xác minh,Tổng lớp,Lớp đang dạy,Lớp hoàn thành,Lớp mới nhận,Thu nhập tích lũy (VND),Tiền đã rút (VND),Tiền chờ rút (VND),Ký quỹ đang giữ (VND),Số dư ví (VND),Đánh giá sao\n");
            List<TutorFinancialAnalyticsResponse> tutors = getTutorAnalytics(from, to);
            for (TutorFinancialAnalyticsResponse t : tutors) {
                sb.append(t.getTutorId()).append(",")
                  .append(escapeCsv(t.getFullName())).append(",")
                  .append(escapeCsv(t.getEmail())).append(",")
                  .append(escapeCsv(t.getPhone() != null ? t.getPhone() : "")).append(",")
                  .append(t.getVerificationStatus()).append(",")
                  .append(t.getTotalClasses()).append(",")
                  .append(t.getActiveClasses()).append(",")
                  .append(t.getCompletedClasses()).append(",")
                  .append(t.getNewClassesInPeriod()).append(",")
                  .append(t.getTotalEarnings() != null ? t.getTotalEarnings() : "0").append(",")
                  .append(t.getTotalWithdrawn() != null ? t.getTotalWithdrawn() : "0").append(",")
                  .append(t.getPendingWithdrawals() != null ? t.getPendingWithdrawals() : "0").append(",")
                  .append(t.getEscrowHolding() != null ? t.getEscrowHolding() : "0").append(",")
                  .append(t.getAvailableBalance() != null ? t.getAvailableBalance() : "0").append(",")
                  .append(t.getAverageRating() != null ? t.getAverageRating() : "0.0").append("\n");
            }
        } else if ("clients".equalsIgnoreCase(type)) {
            sb.append("ID,Họ tên,Email,Số điện thoại,Tổng lớp đăng ký,Lớp đang học,Lớp hoàn thành,Tổng tiền nạp (VND),Tổng hoàn tiền (VND),Ký quỹ bảo vệ (VND),Số dư ví (VND)\n");
            List<ClientFinancialAnalyticsResponse> clients = getClientAnalytics(from, to);
            for (ClientFinancialAnalyticsResponse cl : clients) {
                sb.append(cl.getClientId()).append(",")
                  .append(escapeCsv(cl.getFullName())).append(",")
                  .append(escapeCsv(cl.getEmail())).append(",")
                  .append(escapeCsv(cl.getPhone() != null ? cl.getPhone() : "")).append(",")
                  .append(cl.getTotalClassesRegistered()).append(",")
                  .append(cl.getActiveClasses()).append(",")
                  .append(cl.getCompletedClasses()).append(",")
                  .append(cl.getTotalDeposited() != null ? cl.getTotalDeposited() : "0").append(",")
                  .append(cl.getTotalRefunded() != null ? cl.getTotalRefunded() : "0").append(",")
                  .append(cl.getActiveEscrow() != null ? cl.getActiveEscrow() : "0").append(",")
                  .append(cl.getAvailableBalance() != null ? cl.getAvailableBalance() : "0").append("\n");
            }
        } else if ("ledger".equalsIgnoreCase(type)) {
            sb.append("Mã GD,Mã tham chiếu,Loại giao dịch,Phân loại,Số tiền (VND),Trạng thái,Người thực hiện,Vai trò,Nội dung,Thời gian\n");
            Page<FinancialLedgerItemResponse> ledger = getFinancialLedger(null, null, null, from, to, PageRequest.of(0, MAX_EXPORT_ROWS));
            for (FinancialLedgerItemResponse item : ledger.getContent()) {
                sb.append(item.getTransactionId()).append(",")
                  .append(escapeCsv(item.getReferenceCode())).append(",")
                  .append(escapeCsv(item.getTypeLabel())).append(",")
                  .append(item.getDirection()).append(",")
                  .append(item.getAmount() != null ? item.getAmount() : "0").append(",")
                  .append(item.getStatus()).append(",")
                  .append(escapeCsv(item.getActorName())).append(",")
                  .append(item.getActorRole()).append(",")
                  .append(escapeCsv(item.getDescription() != null ? item.getDescription() : "")).append(",")
                  .append(item.getCreatedAt()).append("\n");
            }
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
                .filter(item -> item.getType() == type
                        && !isPlatformFeeTransaction(item)
                        && item.getStatus() == PaymentTransactionStatus.SUCCESS)
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countTransactions(List<PaymentTransaction> transactions, PaymentTransactionType type) {
        return transactions.stream()
                .filter(item -> item.getType() == type
                        && !isPlatformFeeTransaction(item)
                        && item.getStatus() == PaymentTransactionStatus.SUCCESS)
                .count();
    }

    private String getTxTypeLabel(PaymentTransactionType type) {
        if (type == null) return "Khác";
        return switch (type) {
            case DEPOSIT -> "Nạp tiền vào ví (Deposit)";
            case WITHDRAWAL -> "Rút tiền về tài khoản (Withdrawal)";
            case ESCROW_DEPOSIT -> "Ký quỹ lớp học (Escrow Deposit)";
            case ESCROW_RELEASE -> "Giải ngân học phí (Escrow Release)";
            case REFUND -> "Hoàn tiền ký quỹ (Escrow Refund)";
            case PLATFORM_FEE -> "Phí dịch vụ nền tảng (2%)";
        };
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

    @Override
    public List<CenterFinancialAnalyticsResponse> getCenterAnalytics(LocalDate from, LocalDate to) {
        BigDecimal platformFeeRate = getPlatformFeeRate();
        List<TutorCenter> centers = tutorCenterRepository.findAll();
        List<CenterFinancialAnalyticsResponse> result = new ArrayList<>();
        List<EscrowTransaction> allEscrows = escrowTransactionRepository.findAll();

        for (TutorCenter center : centers) {
            result.add(buildCenterAnalytics(center, from, to, platformFeeRate, allEscrows));
        }
        return result;
    }

    @Override
    public CenterFinancialAnalyticsResponse getCenterAnalyticsByCenterId(Long centerId, LocalDate from, LocalDate to) {
        TutorCenter center = tutorCenterRepository.findById(centerId)
                .orElseThrow(() -> new com.tcs.exception.ResourceNotFoundException("Không tìm thấy trung tâm gia sư"));
        BigDecimal platformFeeRate = getPlatformFeeRate();
        List<EscrowTransaction> allEscrows = escrowTransactionRepository.findAll();
        return buildCenterAnalytics(center, from, to, platformFeeRate, allEscrows);
    }

    private CenterFinancialAnalyticsResponse buildCenterAnalytics(
            TutorCenter center, LocalDate from, LocalDate to, BigDecimal platformFeeRate, List<EscrowTransaction> allEscrows) {
        Long centerId = center.getCenterId();
        List<TutoringClass> centerClasses = tutoringClassRepository.findByCenter_CenterId(centerId);
        long totalClasses = centerClasses.size();
        long activeClasses = centerClasses.stream()
                .filter(c -> c.getStatus() == TutoringClassStatus.IN_PROGRESS || c.getStatus() == TutoringClassStatus.OPEN)
                .count();
        long completedClasses = centerClasses.stream()
                .filter(c -> c.getStatus() == TutoringClassStatus.COMPLETED)
                .count();

        List<CenterTutorMembership> memberships = centerTutorMembershipRepository.findByCenter_CenterIdOrderByJoinedAtDesc(centerId);
        long totalTutors = memberships.stream()
                .filter(m -> m.getStatus() != null && "ACTIVE".equalsIgnoreCase(m.getStatus().name()))
                .count();
        long newTutorsInPeriod = memberships.stream()
                .filter(m -> inRange(m.getJoinedAt(), from, to))
                .count();

        BigDecimal walletBalance = BigDecimal.ZERO;
        BigDecimal moneyOut = BigDecimal.ZERO;
        if (center.getUser() != null && center.getUser().getUserId() != null) {
            Optional<com.tcs.module.finance.entity.Wallet> wOpt = walletRepository.findByUser_UserId(center.getUser().getUserId());
            if (wOpt.isPresent()) {
                walletBalance = wOpt.get().getAvailableBalance() != null ? wOpt.get().getAvailableBalance() : BigDecimal.ZERO;
                List<PaymentTransaction> centerTxs = paymentTransactionRepository.findByWallet_WalletIdAndTypeAndStatus(
                        center.getUser().getUserId(), PaymentTransactionType.WITHDRAWAL, PaymentTransactionStatus.SUCCESS);
                moneyOut = centerTxs.stream()
                        .filter(tx -> inRange(tx.getCreatedAt(), from, to))
                        .map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        List<EscrowTransaction> centerEscrows = allEscrows.stream()
                .filter(e -> e.getClassStudent() != null
                        && e.getClassStudent().getTutoringClass() != null
                        && e.getClassStudent().getTutoringClass().getCenter() != null
                        && centerId.equals(e.getClassStudent().getTutoringClass().getCenter().getCenterId()))
                .toList();

        BigDecimal moneyIn = centerEscrows.stream()
                .filter(e -> inRange(e.getCreatedAt(), from, to))
                .map(EscrowTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal escrowHeld = centerEscrows.stream()
                .filter(e -> e.getStatus() == EscrowStatus.FUNDED || e.getStatus() == EscrowStatus.ON_HOLD || e.getStatus() == EscrowStatus.DISPUTED)
                .map(EscrowTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal escrowReleased = centerEscrows.stream()
                .filter(e -> e.getStatus() == EscrowStatus.RELEASED)
                .filter(e -> inRange(e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt(), from, to))
                .map(EscrowTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformFeePaid = escrowReleased.multiply(platformFeeRate);

        return CenterFinancialAnalyticsResponse.builder()
                .centerId(centerId)
                .companyName(center.getCompanyName())
                .licenseNo(center.getLicenseNo())
                .phone(center.getPhone())
                .email(center.getUser() != null ? center.getUser().getEmail() : "")
                .address(center.getAddress())
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .completedClasses(completedClasses)
                .totalTutors(totalTutors)
                .newTutorsInPeriod(newTutorsInPeriod)
                .moneyIn(moneyIn)
                .moneyOut(moneyOut)
                .escrowHeld(escrowHeld)
                .escrowReleased(escrowReleased)
                .platformFeePaid(platformFeePaid)
                .walletBalance(walletBalance)
                .build();
    }

    @Override
    public List<TutorFinancialAnalyticsResponse> getTutorAnalytics(LocalDate from, LocalDate to) {
        List<Tutor> tutors = tutorRepository.findAll();
        List<TutorFinancialAnalyticsResponse> result = new ArrayList<>();
        List<EscrowTransaction> allEscrows = escrowTransactionRepository.findAll();

        for (Tutor tutor : tutors) {
            Long tutorId = tutor.getTutorId();
            List<ClassAssignment> assignments =
                    classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(tutorId);
            long totalClasses = assignments.size();
            long activeClasses = assignments.stream()
                    .filter(a -> a.getStatus() == ClassAssignmentStatus.ACTIVE)
                    .count();
            long completedClasses = assignments.stream()
                    .filter(a -> a.getTutorCompletedAt() != null && a.getClientCompletedAt() != null)
                    .count();
            long newClassesInPeriod = assignments.stream()
                    .filter(a -> inRange(a.getAssignedDate(), from, to))
                    .count();

            BigDecimal availableBalance = BigDecimal.ZERO;
            BigDecimal totalWithdrawn = BigDecimal.ZERO;
            BigDecimal pendingWithdrawals = BigDecimal.ZERO;

            if (tutor.getUser() != null && tutor.getUser().getUserId() != null) {
                Long uId = tutor.getUser().getUserId();
                Optional<com.tcs.module.finance.entity.Wallet> wOpt = walletRepository.findByUser_UserId(uId);
                if (wOpt.isPresent()) {
                    availableBalance = wOpt.get().getAvailableBalance() != null ? wOpt.get().getAvailableBalance() : BigDecimal.ZERO;
                }
                List<PaymentTransaction> tutorTxs = paymentTransactionRepository.findByWallet_WalletIdAndTypeAndStatus(
                        uId, PaymentTransactionType.WITHDRAWAL, PaymentTransactionStatus.SUCCESS);
                totalWithdrawn = tutorTxs.stream()
                        .filter(tx -> inRange(tx.getCreatedAt(), from, to))
                        .map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<PaymentTransaction> pendingTxs = paymentTransactionRepository.findByWallet_WalletIdAndTypeAndStatus(
                        uId, PaymentTransactionType.WITHDRAWAL, PaymentTransactionStatus.PENDING);
                pendingWithdrawals = pendingTxs.stream()
                        .map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            List<EscrowTransaction> tutorEscrows = allEscrows.stream()
                    .filter(e -> e.getAssignment() != null
                            && e.getAssignment().getTutor() != null
                            && tutorId.equals(e.getAssignment().getTutor().getTutorId()))
                    .toList();

            BigDecimal totalEarnings = tutorEscrows.stream()
                    .filter(e -> e.getStatus() == EscrowStatus.RELEASED)
                    .filter(e -> inRange(e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt(), from, to))
                    .map(EscrowTransaction::getAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal escrowHolding = tutorEscrows.stream()
                    .filter(e -> e.getStatus() == EscrowStatus.FUNDED || e.getStatus() == EscrowStatus.ON_HOLD || e.getStatus() == EscrowStatus.DISPUTED)
                    .map(EscrowTransaction::getAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Double averageRating = 0.0;
            if (tutor.getUser() != null) {
                List<Review> reviews = reviewRepository.findByReviewee_UserId(tutor.getUser().getUserId());
                if (!reviews.isEmpty()) {
                    averageRating = reviews.stream()
                            .map(Review::getRating)
                            .filter(java.util.Objects::nonNull)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0);
                }
            }

            result.add(TutorFinancialAnalyticsResponse.builder()
                    .tutorId(tutorId)
                    .fullName(tutor.getFullName() != null ? tutor.getFullName() : (tutor.getUser() != null ? tutor.getUser().getEmail() : "Gia sư #" + tutorId))
                    .email(tutor.getUser() != null ? tutor.getUser().getEmail() : "")
                    .phone(tutor.getUser() != null ? tutor.getUser().getPhone() : "")
                    .verificationStatus(tutor.getVerificationStatus() != null ? tutor.getVerificationStatus().name() : "UNVERIFIED")
                    .totalClasses(totalClasses)
                    .activeClasses(activeClasses)
                    .completedClasses(completedClasses)
                    .newClassesInPeriod(newClassesInPeriod)
                    .totalEarnings(totalEarnings)
                    .totalWithdrawn(totalWithdrawn)
                    .pendingWithdrawals(pendingWithdrawals)
                    .escrowHolding(escrowHolding)
                    .availableBalance(availableBalance)
                    .averageRating(averageRating)
                    .build());
        }
        return result;
    }

    @Override
    public List<ClientFinancialAnalyticsResponse> getClientAnalytics(LocalDate from, LocalDate to) {
        List<Client> clients = clientRepository.findAll();
        List<ClientFinancialAnalyticsResponse> result = new ArrayList<>();
        List<EscrowTransaction> allEscrows = escrowTransactionRepository.findAll();

        for (Client client : clients) {
            Long clientId = client.getClientId();
            Long userId = client.getUser() != null ? client.getUser().getUserId() : null;

            long totalClasses = 0;
            long activeClasses = 0;
            long completedClasses = 0;
            BigDecimal availableBalance = BigDecimal.ZERO;
            BigDecimal totalDeposited = BigDecimal.ZERO;
            BigDecimal totalRefunded = BigDecimal.ZERO;
            BigDecimal activeEscrow = BigDecimal.ZERO;

            if (userId != null) {
                List<TutoringClass> classes = tutoringClassRepository.findByCreator_UserId(userId);
                totalClasses = classes.size();
                activeClasses = classes.stream()
                        .filter(c -> c.getStatus() == TutoringClassStatus.IN_PROGRESS || c.getStatus() == TutoringClassStatus.OPEN)
                        .count();
                completedClasses = classes.stream()
                        .filter(c -> c.getStatus() == TutoringClassStatus.COMPLETED)
                        .count();

                Optional<com.tcs.module.finance.entity.Wallet> wOpt = walletRepository.findByUser_UserId(userId);
                if (wOpt.isPresent()) {
                    availableBalance = wOpt.get().getAvailableBalance() != null ? wOpt.get().getAvailableBalance() : BigDecimal.ZERO;
                    List<PaymentTransaction> clientTxs = paymentTransactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(
                            userId, PageRequest.of(0, 500)).getContent();

                    totalDeposited = clientTxs.stream()
                            .filter(tx -> tx.getStatus() == PaymentTransactionStatus.SUCCESS
                                    && (tx.getType() == PaymentTransactionType.DEPOSIT || tx.getType() == PaymentTransactionType.ESCROW_DEPOSIT))
                            .filter(tx -> inRange(tx.getCreatedAt(), from, to))
                            .map(PaymentTransaction::getAmount)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    totalRefunded = clientTxs.stream()
                            .filter(tx -> tx.getStatus() == PaymentTransactionStatus.SUCCESS && tx.getType() == PaymentTransactionType.REFUND)
                            .filter(tx -> inRange(tx.getCreatedAt(), from, to))
                            .map(PaymentTransaction::getAmount)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                activeEscrow = allEscrows.stream()
                        .filter(e -> e.getPayment() != null
                                && e.getPayment().getWallet() != null
                                && userId.equals(e.getPayment().getWallet().getWalletId()))
                        .filter(e -> e.getStatus() == EscrowStatus.FUNDED || e.getStatus() == EscrowStatus.ON_HOLD || e.getStatus() == EscrowStatus.DISPUTED)
                        .map(EscrowTransaction::getAmount)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            result.add(ClientFinancialAnalyticsResponse.builder()
                    .clientId(clientId)
                    .fullName(client.getFullName() != null ? client.getFullName() : (client.getUser() != null ? client.getUser().getEmail() : "Khách hàng #" + clientId))
                    .email(client.getUser() != null ? client.getUser().getEmail() : "")
                    .phone(client.getUser() != null ? client.getUser().getPhone() : "")
                    .totalClassesRegistered(totalClasses)
                    .activeClasses(activeClasses)
                    .completedClasses(completedClasses)
                    .totalDeposited(totalDeposited)
                    .totalRefunded(totalRefunded)
                    .activeEscrow(activeEscrow)
                    .availableBalance(availableBalance)
                    .build());
        }
        return result;
    }

    @Override
    public Page<FinancialLedgerItemResponse> getFinancialLedger(
            String role, String direction, String search, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<PaymentTransaction> allTx = paymentTransactionRepository.findByCreatedAtBetween(
                fromDt != null ? fromDt : LocalDateTime.of(2000, 1, 1, 0, 0),
                toDt != null ? toDt : LocalDateTime.now().plusDays(1));

        String searchLower = search != null ? search.trim().toLowerCase() : "";

        List<FinancialLedgerItemResponse> filtered = allTx.stream()
                .filter(tx -> {
                    boolean isMoneyIn = tx.getType() == PaymentTransactionType.DEPOSIT
                            || tx.getType() == PaymentTransactionType.ESCROW_DEPOSIT
                            || isPlatformFeeTransaction(tx);
                    String dir = isMoneyIn ? "IN" : "OUT";
                    if (direction != null && !direction.isBlank() && !"ALL".equalsIgnoreCase(direction)) {
                        if (!dir.equalsIgnoreCase(direction)) return false;
                    }

                    User actor = tx.getWallet() != null ? tx.getWallet().getUser() : null;
                    String actorRole = resolveActorRole(actor);
                    if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                        if (!actorRole.toUpperCase().contains(role.toUpperCase())) return false;
                    }

                    if (!searchLower.isEmpty()) {
                        String ref = tx.getReferenceCode() != null ? tx.getReferenceCode().toLowerCase() : "";
                        String desc = tx.getDescription() != null ? tx.getDescription().toLowerCase() : "";
                        String name = resolveActorName(actor).toLowerCase();
                        String email = actor != null && actor.getEmail() != null ? actor.getEmail().toLowerCase() : "";
                        if (!ref.contains(searchLower) && !desc.contains(searchLower) && !name.contains(searchLower) && !email.contains(searchLower)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(tx -> {
                    boolean isMoneyIn = tx.getType() == PaymentTransactionType.DEPOSIT
                            || tx.getType() == PaymentTransactionType.ESCROW_DEPOSIT
                            || isPlatformFeeTransaction(tx);
                    User actor = tx.getWallet() != null ? tx.getWallet().getUser() : null;
                    return FinancialLedgerItemResponse.builder()
                            .transactionId(tx.getTransactionId())
                            .referenceCode(tx.getReferenceCode())
                            .type(displayTransactionType(tx))
                            .typeLabel(getTxTypeLabel(tx.getType()))
                            .direction(isMoneyIn ? "IN" : "OUT")
                            .amount(tx.getAmount())
                            .status(tx.getStatus() != null ? tx.getStatus().name() : "PENDING")
                            .description(tx.getDescription())
                            .actorUserId(actor != null ? actor.getUserId() : null)
                            .actorName(resolveActorName(actor))
                            .actorEmail(actor != null ? actor.getEmail() : "")
                            .actorRole(resolveActorRole(actor))
                            .relatedEntity(extractRelatedEntity(tx))
                            .createdAt(tx.getCreatedAt())
                            .build();
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<FinancialLedgerItemResponse> paged = start <= end ? filtered.subList(start, end) : List.of();
        return new PageImpl<>(paged, pageable, filtered.size());
    }

    private String extractRelatedEntity(PaymentTransaction tx) {
        if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
            return tx.getDescription();
        }
        return tx.getReferenceCode() != null ? tx.getReferenceCode() : "N/A";
    }

    private BigDecimal getPlatformFeeRate() {
        BigDecimal platformFeeRate = new BigDecimal("0.02");
        Optional<SystemParameter> paramOpt = systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE");
        if (paramOpt.isPresent() && paramOpt.get().getParamValue() != null) {
            try {
                platformFeeRate = new BigDecimal(paramOpt.get().getParamValue().trim());
            } catch (Exception ignored) {}
        }
        return platformFeeRate;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        // Chống CSV Formula Injection (DDE Injection: =, +, -, @)
        if (escaped.startsWith("=") || escaped.startsWith("+") || escaped.startsWith("-") || escaped.startsWith("@")) {
            escaped = "'" + escaped;
        }
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String resolveActorName(User user) {
        if (user == null) return "Hệ thống";
        Long userId = user.getUserId();
        if (userId == null) return user.getEmail() != null ? user.getEmail() : "Hệ thống";

        Optional<PlatformAdmin> admin = platformAdminRepository.findByUser_UserId(userId);
        if (admin.isPresent() && admin.get().getFullName() != null && !admin.get().getFullName().isBlank()) {
            return admin.get().getFullName();
        }
        Optional<TutorCenter> center = tutorCenterRepository.findByUser_UserId(userId);
        if (center.isPresent() && center.get().getCompanyName() != null && !center.get().getCompanyName().isBlank()) {
            return center.get().getCompanyName();
        }
        Optional<Tutor> tutor = tutorRepository.findByUser_UserId(userId);
        if (tutor.isPresent() && tutor.get().getFullName() != null && !tutor.get().getFullName().isBlank()) {
            return tutor.get().getFullName();
        }
        Optional<Client> client = clientRepository.findByUser_UserId(userId);
        if (client.isPresent() && client.get().getFullName() != null && !client.get().getFullName().isBlank()) {
            return client.get().getFullName();
        }
        return user.getEmail() != null ? user.getEmail() : ("Người dùng #" + userId);
    }

    private String resolveActorRole(User user) {
        if (user == null) return "SYSTEM";
        Long userId = user.getUserId();
        if (userId == null) return "SYSTEM";

        if (platformAdminRepository.findByUser_UserId(userId).isPresent()) {
            return "PLATFORM_ADMIN";
        }
        if (tutorCenterRepository.findByUser_UserId(userId).isPresent()) {
            return "TUTOR_CENTER";
        }
        if (tutorRepository.findByUser_UserId(userId).isPresent()) {
            return "TUTOR";
        }
        if (clientRepository.findByUser_UserId(userId).isPresent()) {
            return "CLIENT";
        }
        return "USER";
    }
}
