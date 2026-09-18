package com.tcs.module.center.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcs.module.center.dto.response.CenterFinanceReportResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * UC-41: kiểm tra phần gom tiền của báo cáo tài chính trung tâm. Đây là số tiền hiển thị cho
 * trung tâm đối chiếu nên từng quy tắc phân loại đều được kiểm riêng.
 */
class CenterFinanceAggregatorTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 4, 1, 0, 0); // hết 31/03

    private static TutoringClass clazz(long id, String title) {
        TutoringClass c = new TutoringClass();
        c.setClassId(id);
        c.setTitle(title);
        c.setTuitionFee(new BigDecimal("500000"));
        return c;
    }

    /** Một khoản ký quỹ của lớp đã cho. */
    private static EscrowTransaction escrow(
            TutoringClass c, String amount, EscrowStatus status, LocalDateTime paidAt, LocalDateTime releasedAt) {
        ClassStudent cs = new ClassStudent();
        cs.setTutoringClass(c);
        EscrowTransaction e = new EscrowTransaction();
        e.setClassStudent(cs);
        e.setAmount(new BigDecimal(amount));
        e.setStatus(status);
        e.setDepositedAt(paidAt);
        e.setReleasedAt(releasedAt);
        return e;
    }

    private static CenterFinanceAggregator.Buckets aggregate(EscrowTransaction... escrows) {
        return CenterFinanceAggregator.aggregate(List.of(escrows), FROM, TO);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "mong đợi " + expected + " nhưng là " + actual);
    }

    @Nested
    @DisplayName("Phân loại từng khoản ký quỹ")
    class Classification {

        @Test
        @DisplayName("Chưa nộp tiền thì không tính vào đâu cả")
        void pendingCountsNowhere() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(escrow(c, "500000", EscrowStatus.PENDING, null, null));

            assertMoney("0", b.grossCollected());
            assertMoney("0", b.heldInEscrow());
            assertMoney("0", b.releasedGross());
            assertEquals(0, b.classes().get(0).getPaidStudents());
        }

        @Test
        @DisplayName("Đã nộp, chưa giải ngân: tính vào đã thu và đang giữ")
        void fundedCountsAsCollectedAndHeld() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(escrow(c, "500000", EscrowStatus.FUNDED, LocalDateTime.of(2026, 3, 10, 9, 0), null));

            assertMoney("500000", b.grossCollected());
            assertMoney("500000", b.heldInEscrow());
            assertMoney("0", b.releasedGross());
            assertEquals(1, b.classes().get(0).getPaidStudents());
        }

        @Test
        @DisplayName("Đã giải ngân: vẫn tính đã thu, chuyển khỏi số đang giữ")
        void releasedLeavesEscrow() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(escrow(c, "500000", EscrowStatus.RELEASED,
                    LocalDateTime.of(2026, 3, 5, 9, 0), LocalDateTime.of(2026, 3, 25, 9, 0)));

            assertMoney("500000", b.grossCollected());
            assertMoney("500000", b.releasedGross());
            assertMoney("0", b.heldInEscrow());
        }

        @Test
        @DisplayName("Đã hoàn tiền: không tính là đã thu, không nằm trong số đang giữ")
        void refundedIsNotRevenue() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(escrow(c, "500000", EscrowStatus.REFUNDED,
                    LocalDateTime.of(2026, 3, 5, 9, 0), null));

            assertMoney("0", b.grossCollected());
            assertMoney("0", b.heldInEscrow());
            assertMoney("500000", b.refunded());
        }

        @Test
        @DisplayName("Đang tranh chấp và đang tạm giữ vẫn nằm trong ký quỹ")
        void disputedAndOnHoldStayInEscrow() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(
                    escrow(c, "300000", EscrowStatus.DISPUTED, LocalDateTime.of(2026, 3, 2, 9, 0), null),
                    escrow(c, "200000", EscrowStatus.ON_HOLD, LocalDateTime.of(2026, 3, 3, 9, 0), null));

            assertMoney("500000", b.heldInEscrow());
            assertMoney("500000", b.grossCollected());
        }
    }

    @Nested
    @DisplayName("Lọc theo kỳ báo cáo")
    class DateRange {

        @Test
        @DisplayName("Tiền nộp ngoài kỳ không tính vào đã thu, nhưng vẫn nằm trong số đang giữ")
        void collectedOutsideRangeStillHeld() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(escrow(c, "500000", EscrowStatus.FUNDED,
                    LocalDateTime.of(2026, 1, 20, 9, 0), null));

            assertMoney("0", b.grossCollected());
            assertMoney("500000", b.heldInEscrow());
        }

        @Test
        @DisplayName("Nộp kỳ trước, giải ngân trong kỳ: chỉ tính vào đã giải ngân")
        void releasedInRangeOnly() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(escrow(c, "500000", EscrowStatus.RELEASED,
                    LocalDateTime.of(2026, 2, 10, 9, 0), LocalDateTime.of(2026, 3, 15, 9, 0)));

            assertMoney("0", b.grossCollected());
            assertMoney("500000", b.releasedGross());
        }

        @Test
        @DisplayName("Ngày cuối kỳ vẫn được tính, ngày đầu kỳ cũng vậy")
        void boundariesAreInclusive() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = aggregate(
                    escrow(c, "100000", EscrowStatus.FUNDED, LocalDateTime.of(2026, 3, 1, 0, 0), null),
                    escrow(c, "200000", EscrowStatus.FUNDED, LocalDateTime.of(2026, 3, 31, 23, 59), null));

            assertMoney("300000", b.grossCollected());
        }

        @Test
        @DisplayName("Thiếu ngày nộp thì lấy ngày tạo bản ghi")
        void fallsBackToCreatedAt() {
            TutoringClass c = clazz(1L, "Toán 9");
            EscrowTransaction e = escrow(c, "500000", EscrowStatus.FUNDED, null, null);
            e.setCreatedAt(LocalDateTime.of(2026, 3, 8, 9, 0));

            var b = CenterFinanceAggregator.aggregate(List.of(e), FROM, TO);

            assertMoney("500000", b.grossCollected());
        }
    }

    @Nested
    @DisplayName("Gom theo lớp và theo tháng")
    class Grouping {

        @Test
        @DisplayName("Mỗi lớp một dòng, cộng dồn đúng số học viên đã đóng")
        void groupsByClass() {
            TutoringClass toan = clazz(1L, "Toán 9");
            TutoringClass ly = clazz(2L, "Lý 11");
            var b = aggregate(
                    escrow(toan, "500000", EscrowStatus.FUNDED, LocalDateTime.of(2026, 3, 2, 9, 0), null),
                    escrow(toan, "500000", EscrowStatus.FUNDED, LocalDateTime.of(2026, 3, 3, 9, 0), null),
                    escrow(ly, "400000", EscrowStatus.RELEASED,
                            LocalDateTime.of(2026, 3, 4, 9, 0), LocalDateTime.of(2026, 3, 20, 9, 0)));

            assertEquals(2, b.classes().size());
            CenterFinanceReportResponse.ClassRow rowToan = b.classes().get(0);
            assertEquals("Toán 9", rowToan.getTitle());
            assertEquals(2, rowToan.getPaidStudents());
            assertMoney("1000000", rowToan.getGross());
            assertMoney("1000000", rowToan.getHeld());

            CenterFinanceReportResponse.ClassRow rowLy = b.classes().get(1);
            assertMoney("400000", rowLy.getReleased());
            assertMoney("0", rowLy.getHeld());
        }

        @Test
        @DisplayName("Tháng xếp tăng dần, tiền thu và tiền giải ngân nằm đúng tháng của nó")
        void groupsByMonth() {
            TutoringClass c = clazz(1L, "Toán 9");
            var b = CenterFinanceAggregator.aggregate(
                    List.of(
                            escrow(c, "500000", EscrowStatus.RELEASED,
                                    LocalDateTime.of(2026, 1, 10, 9, 0), LocalDateTime.of(2026, 3, 10, 9, 0)),
                            escrow(c, "300000", EscrowStatus.FUNDED,
                                    LocalDateTime.of(2026, 2, 5, 9, 0), null)),
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 4, 1, 0, 0));

            List<CenterFinanceReportResponse.MonthRow> months = b.months();
            assertEquals(List.of("2026-01", "2026-02", "2026-03"),
                    months.stream().map(CenterFinanceReportResponse.MonthRow::getMonth).toList());
            assertMoney("500000", months.get(0).getGross());   // tháng 1: thu
            assertMoney("0", months.get(0).getReleased());
            assertMoney("300000", months.get(1).getGross());   // tháng 2: thu
            assertMoney("0", months.get(2).getGross());        // tháng 3: chỉ giải ngân
            assertMoney("500000", months.get(2).getReleased());
        }

        @Test
        @DisplayName("Ký quỹ không gắn lớp nào thì bỏ qua, không làm hỏng báo cáo")
        void skipsEscrowWithoutClass() {
            EscrowTransaction orphan = new EscrowTransaction();
            orphan.setAmount(new BigDecimal("500000"));
            orphan.setStatus(EscrowStatus.FUNDED);
            orphan.setDepositedAt(LocalDateTime.of(2026, 3, 9, 9, 0));

            var b = CenterFinanceAggregator.aggregate(List.of(orphan), FROM, TO);

            assertTrue(b.classes().isEmpty());
            assertMoney("0", b.grossCollected());
        }

        @Test
        @DisplayName("Không có ký quỹ nào thì mọi số về 0, không ném lỗi")
        void emptyInput() {
            var b = CenterFinanceAggregator.aggregate(List.of(), FROM, TO);

            assertMoney("0", b.grossCollected());
            assertMoney("0", b.releasedGross());
            assertMoney("0", b.refunded());
            assertMoney("0", b.heldInEscrow());
            assertTrue(b.classes().isEmpty());
            assertTrue(b.months().isEmpty());
        }
    }
}
