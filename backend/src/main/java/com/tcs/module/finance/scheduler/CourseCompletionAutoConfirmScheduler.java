package com.tcs.module.finance.scheduler;

import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * BF-04 exception "Completion Not Confirmed": nếu cả gia sư lẫn trung tâm KHÔNG xác nhận, hệ thống
 * tự xác nhận hoàn thành sau 7 NGÀY kể từ buổi học cuối (dựa trên ngày kết thúc lớp).
 *
 * <p>Chỉ tự đóng khi lớp thực sự đã học đủ buổi (điểm danh đủ) và không có khiếu nại — tái dùng
 * {@link CenterEscrowAutoSettlementService#trySettleCompletedCenterClass(Long)} (không cần cờ thủ công).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCompletionAutoConfirmScheduler {

    private static final long SIX_HOURS = 21_600_000L;
    private static final int GRACE_DAYS = 7;

    private final TutoringClassRepository tutoringClassRepository;
    private final CenterEscrowAutoSettlementService settlementService;

    @Scheduled(fixedRate = SIX_HOURS)
    @Transactional
    public void autoConfirmStaleCompletions() {
        LocalDate cutoff = LocalDate.now().minusDays(GRACE_DAYS);
        for (TutoringClass c : tutoringClassRepository.findAll()) {
            if (c.getClassType() != ClassType.CENTER || c.getEndDate() == null) {
                continue;
            }
            if (!isPendingCompletion(c.getStatus())) {
                continue;
            }
            // Buổi cuối (≈ ngày kết thúc lớp) đã qua > 7 ngày.
            if (c.getEndDate().isAfter(cutoff)) {
                continue;
            }
            boolean settled = settlementService.trySettleCompletedCenterClass(c.getClassId());
            if (settled) {
                log.info("[AutoConfirm] Tự xác nhận hoàn thành lớp {} sau {} ngày kể từ buổi cuối.",
                        c.getClassId(), GRACE_DAYS);
            }
        }
    }

    private boolean isPendingCompletion(TutoringClassStatus status) {
        return status == TutoringClassStatus.IN_PROGRESS
                || status == TutoringClassStatus.MATCHED
                || status == TutoringClassStatus.ENROLLMENT_CLOSED;
    }
}
