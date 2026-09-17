package com.tcs.module.center.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * UC-41: báo cáo tài chính của một trung tâm.
 *
 * <p>Tiền học viên đóng được giữ ở ký quỹ theo từng học viên, giải ngân về ví trung tâm khi lớp
 * hoàn tất, và phí nền tảng bị trừ ngay lúc giải ngân. Báo cáo tách rõ ba mốc đó để trung tâm
 * đối chiếu được: đã thu bao nhiêu, còn giữ bao nhiêu, thực nhận bao nhiêu.
 */
@Getter
@Builder
public class CenterFinanceReportResponse {

    private Summary summary;
    private List<ClassRow> classes;
    private List<MonthRow> months;

    @Getter
    @Builder
    public static class Summary {
        /** Khoảng thời gian của báo cáo (đã áp mặc định nếu người dùng bỏ trống). */
        private LocalDate from;
        private LocalDate to;
        /** Học phí học viên đã đóng vào ký quỹ trong kỳ (chưa trừ phí nền tảng). */
        private BigDecimal grossCollected;
        /** Đã giải ngân về ví trong kỳ, tính gộp trước phí. */
        private BigDecimal releasedGross;
        /** Phí nền tảng đã bị trừ trong kỳ. */
        private BigDecimal platformFee;
        /** Thực nhận = đã giải ngân − phí nền tảng. */
        private BigDecimal netReceived;
        /** Đã hoàn lại cho học viên trong kỳ. */
        private BigDecimal refunded;
        /** Đã rút khỏi ví trong kỳ. */
        private BigDecimal withdrawn;
        /** Số dư tức thời, không phụ thuộc khoảng thời gian. */
        private BigDecimal heldInEscrow;
        private BigDecimal availableBalance;
        private BigDecimal frozenBalance;
    }

    /** Một dòng cho mỗi lớp của trung tâm. */
    @Getter
    @Builder
    public static class ClassRow {
        private Long classId;
        private String title;
        private String subjectName;
        private String status;
        private BigDecimal tuitionFee;
        /** Số học viên đã đóng tiền (có ký quỹ chưa bị hoàn). */
        private int paidStudents;
        private BigDecimal gross;
        private BigDecimal held;
        private BigDecimal released;
        private BigDecimal refunded;
    }

    /** Một dòng cho mỗi tháng, để vẽ biểu đồ cột. */
    @Getter
    @Builder
    public static class MonthRow {
        /** Dạng yyyy-MM. */
        private String month;
        private BigDecimal gross;
        private BigDecimal released;
    }
}
