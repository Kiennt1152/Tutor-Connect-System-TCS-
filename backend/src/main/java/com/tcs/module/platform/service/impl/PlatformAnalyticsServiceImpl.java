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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAnalyticsServiceImpl implements PlatformAnalyticsService {

    private static final int MAX_EXPORT_ROWS = 10_000;
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

        BigDecimal totalRevenue = paymentTransactionRepository.sumAmountByStatusAndTypeInAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS,
                List.of(PaymentTransactionType.DEPOSIT, PaymentTransactionType.ESCROW_DEPOSIT),
                fromDt, toDt
        );

        BigDecimal platformFeeRate = new BigDecimal("0.10");
        Optional<SystemParameter> paramOpt = systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE");
        if (paramOpt.isPresent() && paramOpt.get().getParamValue() != null) {
            try {
                platformFeeRate = new BigDecimal(paramOpt.get().getParamValue().trim());
            } catch (Exception ignored) {}
        }
        BigDecimal platformFeeRevenue = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS, PaymentTransactionType.PLATFORM_FEE, fromDt, toDt);
        BigDecimal deposits = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS, PaymentTransactionType.DEPOSIT, fromDt, toDt);
        BigDecimal withdrawals = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS, PaymentTransactionType.WITHDRAWAL, fromDt, toDt);
        BigDecimal escrowDeposited = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS, PaymentTransactionType.ESCROW_DEPOSIT, fromDt, toDt);
        BigDecimal escrowReleased = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS, PaymentTransactionType.ESCROW_RELEASE, fromDt, toDt);
        BigDecimal escrowRefunded = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                PaymentTransactionStatus.SUCCESS, PaymentTransactionType.REFUND, fromDt, toDt);
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
            BigDecimal revenue = paymentTransactionRepository.sumAmountByStatusAndTypeInAndCreatedAtBetween(
                    PaymentTransactionStatus.SUCCESS,
                    List.of(PaymentTransactionType.DEPOSIT, PaymentTransactionType.ESCROW_DEPOSIT),
                    monthStart,
                    monthEnd
            );
            
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
            long count = paymentTransactionRepository.countByStatusAndTypeAndDateRange(
                    PaymentTransactionStatus.SUCCESS, txType, fromDt, toDt);
            BigDecimal sum = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                    PaymentTransactionStatus.SUCCESS, txType, fromDt, toDt);
            boolean isMoneyIn = txType == PaymentTransactionType.DEPOSIT 
                    || txType == PaymentTransactionType.ESCROW_DEPOSIT
                    || txType == PaymentTransactionType.PLATFORM_FEE;
            breakdown.add(TransactionTypeBreakdown.builder()
                    .type(txType.name())
                    .count((int) count)
                    .totalAmount(sum)
                    .direction(isMoneyIn ? "IN" : "OUT")
                    .build());
        }

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

    @Override
    public byte[] exportCsv(String type, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        
        // Default to last 90 days if date bounds not provided, preventing unbounded table dumps
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
                  .append(pt.getType()).append(",")
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
                        || pt.getType() == PaymentTransactionType.ESCROW_DEPOSIT
                        || pt.getType() == PaymentTransactionType.PLATFORM_FEE;
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
                        .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCESS && t.getType() == txType)
                        .toList();
                BigDecimal sum = filtered.stream().map(PaymentTransaction::getAmount)
                        .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                boolean isIn = txType == PaymentTransactionType.DEPOSIT
                        || txType == PaymentTransactionType.ESCROW_DEPOSIT
                        || txType == PaymentTransactionType.PLATFORM_FEE;
                sb.append(txType.name()).append(",").append(isIn ? "IN" : "OUT").append(",")
                  .append(filtered.size()).append(",").append(sum).append("\n");
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

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
