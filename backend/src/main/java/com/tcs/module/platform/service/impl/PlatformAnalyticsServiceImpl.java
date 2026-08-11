package com.tcs.module.platform.service.impl;

import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.MonthlyMetricResponse;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.entity.SystemParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAnalyticsServiceImpl implements PlatformAnalyticsService {

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
        List<User> allUsers = userRepository.findAll().stream()
                .filter(user -> inRange(user.getCreatedAt(), from, to)).toList();
        long totalUsers = allUsers.size();
        long totalTutors = tutorRepository.count();
        long totalParents = clientRepository.count();
        long totalCenters = tutorCenterRepository.count();
        long totalStudents = Math.max(0, totalUsers - totalTutors - totalParents - totalCenters);

        List<TutoringClass> allClasses = tutoringClassRepository.findAll().stream()
                .filter(item -> inRange(item.getCreatedAt(), from, to)).toList();
        long totalClasses = allClasses.size();
        
        long activeClasses = allClasses.stream()
                .filter(c -> c.getStatus() == TutoringClassStatus.IN_PROGRESS 
                          || c.getStatus() == TutoringClassStatus.OPEN 
                          || c.getStatus() == TutoringClassStatus.MATCHED)
                .count();

        long completedClasses = allClasses.stream()
                .filter(c -> c.getStatus() == TutoringClassStatus.COMPLETED)
                .count();

        List<PaymentTransaction> allTransactions = paymentTransactionRepository.findAll().stream()
                .filter(item -> inRange(item.getCreatedAt(), from, to)).toList();
        BigDecimal totalRevenue = allTransactions.stream()
                .filter(pt -> pt.getStatus() == PaymentTransactionStatus.SUCCESS 
                           && (pt.getType() == PaymentTransactionType.DEPOSIT 
                            || pt.getType() == PaymentTransactionType.ESCROW_DEPOSIT))
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformFeeRate = new BigDecimal("0.10");
        Optional<SystemParameter> paramOpt = systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE");
        if (paramOpt.isPresent() && paramOpt.get().getParamValue() != null) {
            try {
                platformFeeRate = new BigDecimal(paramOpt.get().getParamValue().trim());
            } catch (Exception e) {
                // ignore, fallback to default
            }
        }
        BigDecimal platformFeeRevenue = totalRevenue.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deposits = sumTransactions(allTransactions, PaymentTransactionType.DEPOSIT);
        BigDecimal withdrawals = sumTransactions(allTransactions, PaymentTransactionType.WITHDRAWAL);
        BigDecimal escrowDeposited = sumTransactions(allTransactions, PaymentTransactionType.ESCROW_DEPOSIT);
        BigDecimal escrowReleased = sumTransactions(allTransactions, PaymentTransactionType.ESCROW_RELEASE);
        BigDecimal escrowRefunded = sumTransactions(allTransactions, PaymentTransactionType.REFUND);
        BigDecimal escrowHeld = escrowDeposited.subtract(escrowReleased).subtract(escrowRefunded).max(BigDecimal.ZERO);

        long totalVerif = verificationRequestRepository.count();
        long approvedVerif = verificationRequestRepository.findAll().stream()
                .filter(v -> v.getStatus() == VerificationStatus.VERIFIED)
                .count();
        double verificationConversionRate = totalVerif == 0 ? 0.0 : (double) approvedVerif / totalVerif * 100.0;

        long totalTx = allTransactions.size();
        long totalDisputes = disputeRepository.count();
        double disputeRate = totalTx == 0 ? 0.0 : (double) totalDisputes / totalTx * 100.0;

        long totalContracts = contractRepository.count();
        long completedContracts = contractRepository.countByStatus(ContractStatus.COMPLETED);
        double contractCompletionRate = totalContracts == 0 ? 0.0 : (double) completedContracts / totalContracts * 100.0;

        List<MonthlyMetricResponse> monthlyMetrics = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            
            long newUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && YearMonth.from(u.getCreatedAt()).equals(ym))
                    .count();
            
            long newClasses = allClasses.stream()
                    .filter(c -> c.getCreatedAt() != null && YearMonth.from(c.getCreatedAt()).equals(ym))
                    .count();
            
            BigDecimal revenue = allTransactions.stream()
                    .filter(pt -> pt.getCreatedAt() != null 
                               && YearMonth.from(pt.getCreatedAt()).equals(ym)
                               && pt.getStatus() == PaymentTransactionStatus.SUCCESS
                               && (pt.getType() == PaymentTransactionType.DEPOSIT 
                                || pt.getType() == PaymentTransactionType.ESCROW_DEPOSIT))
                    .map(PaymentTransaction::getAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            String label = "T" + ym.getMonthValue() + "/" + ym.getYear();
            
            monthlyMetrics.add(MonthlyMetricResponse.builder()
                    .month(label)
                    .newUsers(newUsers)
                    .newClasses(newClasses)
                    .revenue(revenue)
                    .build());
        }

        return AnalyticsSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalTutors(totalTutors)
                .totalParents(totalParents)
                .totalCenters(totalCenters)
                .totalStudents(totalStudents)
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .completedClasses(completedClasses)
                .totalRevenue(totalRevenue)
                .platformFeeRevenue(platformFeeRevenue)
                .deposits(deposits)
                .withdrawals(withdrawals)
                .escrowHeld(escrowHeld)
                .escrowReleased(escrowReleased)
                .escrowRefunded(escrowRefunded)
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
        
        if ("classes".equalsIgnoreCase(type)) {
            sb.append("ID,Tiêu đề,Môn học,Trạng thái,Học phí (VND),Ngày tạo\n");
            for (TutoringClass c : tutoringClassRepository.findAll().stream().filter(item -> inRange(item.getCreatedAt(), from, to)).toList()) {
                sb.append(c.getClassId()).append(",")
                  .append(escapeCsv(c.getTitle())).append(",")
                  .append(c.getSubject() != null ? escapeCsv(c.getSubject().getSubjectName()) : "").append(",")
                  .append(c.getStatus()).append(",")
                  .append(c.getTuitionFee() != null ? c.getTuitionFee() : "0").append(",")
                  .append(c.getCreatedAt()).append("\n");
            }
        } else if ("revenue".equalsIgnoreCase(type)) {
            sb.append("ID,Mã tham chiếu,Loại giao dịch,Số tiền (VND),Trạng thái,Ngày giao dịch\n");
            for (PaymentTransaction pt : paymentTransactionRepository.findAll().stream().filter(item -> inRange(item.getCreatedAt(), from, to)).toList()) {
                sb.append(pt.getTransactionId()).append(",")
                  .append(escapeCsv(pt.getReferenceCode())).append(",")
                  .append(pt.getType()).append(",")
                  .append(pt.getAmount()).append(",")
                  .append(pt.getStatus()).append(",")
                  .append(pt.getCreatedAt()).append("\n");
            }
        } else {
            sb.append("ID,Email,Số điện thoại,Trạng thái,Ngày tạo\n");
            for (User u : userRepository.findAll().stream().filter(item -> inRange(item.getCreatedAt(), from, to)).toList()) {
                sb.append(u.getUserId()).append(",")
                  .append(escapeCsv(u.getEmail())).append(",")
                  .append(escapeCsv(u.getPhone())).append(",")
                  .append(u.getStatus()).append(",")
                  .append(u.getCreatedAt()).append("\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BigDecimal sumTransactions(List<PaymentTransaction> transactions, PaymentTransactionType type) {
        return transactions.stream()
                .filter(item -> item.getStatus() == PaymentTransactionStatus.SUCCESS && item.getType() == type)
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean inRange(LocalDateTime value, LocalDate from, LocalDate to) {
        if (value == null) return from == null && to == null;
        return (from == null || !value.toLocalDate().isBefore(from))
                && (to == null || !value.toLocalDate().isAfter(to));
    }
    
    private String escapeCsv(String val) {
        if (val == null) return "";
        String clean = val.replace("\"", "\"\"");
        // Neutralize formula-injection characters (Excel/Sheets execute cells starting with these).
        if (clean.length() > 0 && "=+-@\t".indexOf(clean.charAt(0)) >= 0) {
            clean = "'" + clean;
        }
        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}
