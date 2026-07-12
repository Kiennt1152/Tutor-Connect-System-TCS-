package com.tcs.module.finance.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.SettlementService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tinh so tien release cho beneficiary va refund cho payer tu:
 * - attendance status buoi hoc
 * - so tien escrow locked cua assignment/classStudent
 * - commission rate (PLATFORM_COMMISSION_RATE) tu system_parameters, mac dinh 10%
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final EscrowService escrowService;
    public static final String COMMISSION_PARAM_KEY = "PLATFORM_COMMISSION_RATE";
    public static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.10");

    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
    private final EscrowTransactionRepository escrowRepository;
    private final SystemParameterRepository systemParameterRepository;

    @Override
    @Transactional(readOnly = true)
    public ReleaseInstruction calculate(Long classId) {
        throw new UnsupportedOperationException("Chức năng tính toán tất toán thuộc module M4");
        List<EscrowTransaction> escrows = escrowRepository.findAll().stream()
                .filter(e -> belongsToClass(e, classId))
                .toList();

        if (escrows.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Khong tim thay escrow nao dang locked cho classId=" + classId);
        }

        BigDecimal totalPaid = escrows.stream()
                .filter(e -> e.getStatus() == EscrowStatus.ON_HOLD
                        || e.getStatus() == EscrowStatus.FUNDED)
                .map(EscrowTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Lesson> lessons = lessonRepository.findByTutoringClass_ClassId(classId);
        long completedLessons = lessons.stream()
                .filter(l -> l.getAttendanceStatus() == AttendanceStatus.COMPLETED)
                .count();
        long totalLessons = lessons.size();

        BigDecimal completedRatio = totalLessons == 0
                ? BigDecimal.ONE
                : BigDecimal.valueOf(completedLessons)
                        .divide(BigDecimal.valueOf(totalLessons), 4, RoundingMode.HALF_UP);

        BigDecimal releasableGross = totalPaid.multiply(completedRatio)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal commissionRate = getCommissionRate();
        BigDecimal platformFee = releasableGross.multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal releaseToBeneficiary = releasableGross.subtract(platformFee);
        BigDecimal refundToPayer = totalPaid.subtract(releasableGross);

        log.info("[Settlement] classId={} totalEscrow={} completedLessons={}/{} "
                        + "commissionRate={} release={} refund={} platformFee={}",
                classId, totalPaid, completedLessons, totalLessons,
                commissionRate, releaseToBeneficiary, refundToPayer, platformFee);

        return new ReleaseInstruction(
                escrows.get(0).getEscrowId(),
                releaseToBeneficiary,
                refundToPayer,
                String.format("Settlement class %d: %d/%d buoi hoan thanh",
                        classId, completedLessons, totalLessons));
    }

    private boolean belongsToClass(EscrowTransaction e, Long classId) {
        if (e.getAssignment() != null
                && e.getAssignment().getApplication() != null
                && e.getAssignment().getApplication().getTutoringClass() != null) {
            return classId.equals(
                    e.getAssignment().getApplication().getTutoringClass().getClassId());
        }
        if (e.getClassStudent() != null
                && e.getClassStudent().getTutoringClass() != null) {
            return classId.equals(
                    e.getClassStudent().getTutoringClass().getClassId());
        }
        return false;
    }

    @Override
    @Transactional
    public void execute(ReleaseInstruction instruction) {
        if (instruction == null) {
            throw new BusinessException("Thiếu chỉ dẫn tất toán");
        }
        escrowService.apply(instruction);
    }
}
    private BigDecimal getCommissionRate() {
        Optional<SystemParameter> param = systemParameterRepository.findByParamKey(COMMISSION_PARAM_KEY);
        if (param.isPresent()) {
            try {
                BigDecimal rate = new BigDecimal(param.get().getParamValue());
                if (rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(BigDecimal.ONE) <= 0) {
                    return rate;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        log.warn("[Settlement] Commission rate khong hop le, su dung mac dinh {}",
                DEFAULT_COMMISSION_RATE);
        return DEFAULT_COMMISSION_RATE;
    }

    /**
     * Cho CENTER: diem danh theo hoc sinh. Tinh xem co bao nhieu attendance PRESENT.
     * Hien dang tra ve PRESENT count tham khao (chua dung de override completedRatio).
     */
    public long countPresentAttendances(Long classId) {
        List<Lesson> lessons = lessonRepository.findByTutoringClass_ClassId(classId);
        if (lessons.isEmpty()) return 0L;
        List<Long> lessonIds = lessons.stream().map(Lesson::getLessonId).toList();
        List<LessonAttendance> attendances =
                lessonAttendanceRepository.findByLesson_LessonIdIn(lessonIds);
        return attendances.stream()
                .filter(a -> a.getStatus() == LessonAttendanceStatus.PRESENT)
                .count();
    }
}