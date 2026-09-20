package com.tcs.module.center.service.impl;

import com.tcs.module.center.dto.response.CenterFinanceReportResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * UC-41: gom tiền từ danh sách ký quỹ của một trung tâm.
 *
 * <p>Tách khỏi service để phần tính tiền không phụ thuộc cơ sở dữ liệu và kiểm thử được trực tiếp.
 * Quy ước phân loại một khoản ký quỹ:
 *
 * <ul>
 *   <li>PENDING — học viên chưa nộp, không tính vào đâu cả.
 *   <li>Đã nộp và chưa bị hoàn — tính vào "đã thu", theo ngày nộp.
 *   <li>RELEASED — tính vào "đã giải ngân", theo ngày giải ngân.
 *   <li>REFUNDED — tính vào "đã hoàn", và không tính vào "đã thu".
 *   <li>FUNDED / ON_HOLD / DISPUTED — vẫn nằm trong ký quỹ, cộng vào số dư đang giữ.
 * </ul>
 *
 * <p>"Đã thu" và "đã giải ngân" lọc theo kỳ báo cáo, mỗi loại theo mốc thời gian của chính nó.
 * "Đang giữ" và "đã hoàn" là số dư tích luỹ nên không lọc theo kỳ.
 */
final class CenterFinanceAggregator {

    private CenterFinanceAggregator() {}

    /** Kết quả gom tiền, chưa gồm phần lấy từ ví (phí nền tảng, rút tiền, số dư). */
    record Buckets(
            BigDecimal grossCollected,
            BigDecimal releasedGross,
            BigDecimal refunded,
            BigDecimal heldInEscrow,
            List<CenterFinanceReportResponse.ClassRow> classes,
            List<CenterFinanceReportResponse.MonthRow> months) {}

    static Buckets aggregate(
            List<EscrowTransaction> escrows, LocalDateTime fromDt, LocalDateTime toDt) {
        BigDecimal grossCollected = BigDecimal.ZERO;
        BigDecimal releasedGross = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal heldInEscrow = BigDecimal.ZERO;

        Map<Long, ClassMoney> perClass = new LinkedHashMap<>();
        Map<String, BigDecimal[]> perMonth = new TreeMap<>();

        for (EscrowTransaction e : escrows) {
            TutoringClass c = classOf(e);
            if (c == null || c.getClassId() == null) {
                continue;
            }
            BigDecimal amount = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
            EscrowStatus status = e.getStatus();
            ClassMoney row = perClass.computeIfAbsent(c.getClassId(), k -> new ClassMoney(c));

            LocalDateTime paidAt = e.getDepositedAt() != null ? e.getDepositedAt() : e.getCreatedAt();
            boolean paidIn = status != null && status != EscrowStatus.PENDING;

            if (paidIn && status != EscrowStatus.REFUNDED && inRange(paidAt, fromDt, toDt)) {
                grossCollected = grossCollected.add(amount);
                row.gross = row.gross.add(amount);
                row.paidStudents++;
                addMonth(perMonth, paidAt, 0, amount);
            }
            if (status == EscrowStatus.RELEASED && inRange(e.getReleasedAt(), fromDt, toDt)) {
                releasedGross = releasedGross.add(amount);
                row.released = row.released.add(amount);
                addMonth(perMonth, e.getReleasedAt(), 1, amount);
            }
            if (status == EscrowStatus.REFUNDED) {
                refunded = refunded.add(amount);
                row.refunded = row.refunded.add(amount);
            }
            if (status == EscrowStatus.FUNDED
                    || status == EscrowStatus.ON_HOLD
                    || status == EscrowStatus.DISPUTED) {
                heldInEscrow = heldInEscrow.add(amount);
                row.held = row.held.add(amount);
            }
        }

        List<CenterFinanceReportResponse.ClassRow> classRows = new ArrayList<>();
        perClass.values().forEach(m -> classRows.add(m.toRow()));

        List<CenterFinanceReportResponse.MonthRow> monthRows = new ArrayList<>();
        perMonth.forEach((month, cell) -> monthRows.add(CenterFinanceReportResponse.MonthRow.builder()
                .month(month)
                .gross(cell[0])
                .released(cell[1])
                .build()));

        return new Buckets(grossCollected, releasedGross, refunded, heldInEscrow, classRows, monthRows);
    }

    private static TutoringClass classOf(EscrowTransaction e) {
        ClassStudent cs = e.getClassStudent();
        return cs != null ? cs.getTutoringClass() : null;
    }

    private static boolean inRange(LocalDateTime at, LocalDateTime fromDt, LocalDateTime toDt) {
        return at != null && !at.isBefore(fromDt) && at.isBefore(toDt);
    }

    /** Cộng dồn vào ô tháng: chỉ số 0 = tiền thu, 1 = tiền giải ngân. */
    private static void addMonth(
            Map<String, BigDecimal[]> perMonth, LocalDateTime at, int slot, BigDecimal amount) {
        if (at == null) {
            return;
        }
        String key = String.format("%04d-%02d", at.getYear(), at.getMonthValue());
        BigDecimal[] cell = perMonth.computeIfAbsent(
                key, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        cell[slot] = cell[slot].add(amount);
    }

    /** Bộ đếm tiền của một lớp trong lúc duyệt danh sách ký quỹ. */
    private static final class ClassMoney {
        private final TutoringClass tutoringClass;
        private int paidStudents;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal held = BigDecimal.ZERO;
        private BigDecimal released = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;

        private ClassMoney(TutoringClass tutoringClass) {
            this.tutoringClass = tutoringClass;
        }

        private CenterFinanceReportResponse.ClassRow toRow() {
            return CenterFinanceReportResponse.ClassRow.builder()
                    .classId(tutoringClass.getClassId())
                    .title(tutoringClass.getTitle())
                    .subjectName(tutoringClass.getSubject() != null
                            ? tutoringClass.getSubject().getSubjectName() : null)
                    .status(tutoringClass.getStatus() != null ? tutoringClass.getStatus().name() : null)
                    .tuitionFee(tutoringClass.getTuitionFee())
                    .paidStudents(paidStudents)
                    .gross(gross)
                    .held(held)
                    .released(released)
                    .refunded(refunded)
                    .build();
        }
    }
}
