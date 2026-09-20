package com.tcs.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Tiện ích cho khung giờ học có thể kết thúc lúc NỬA ĐÊM.
 *
 * <p>{@link LocalTime} không biểu diễn được 24:00, nên khung giờ chạm nửa đêm được lưu là
 * {@code 00:00} — hiểu là 24:00 của chính ngày hôm đó, không phải 00:00 đầu ngày. Mọi phép so
 * sánh/độ dài trên giờ KẾT THÚC phải đi qua lớp này, nếu không sẽ sai nghiêm trọng:
 * {@code 23:00–00:00} bị coi là âm (kiểm tra trùng lịch sai) hoặc bằng 0 (tính tiền sai).
 */
public final class SlotTime {

    /** Giờ kết thúc nửa đêm (24:00 của ngày hôm đó). */
    public static final LocalTime MIDNIGHT_END = LocalTime.MIDNIGHT;

    private static final int MINUTES_PER_DAY = 24 * 60;

    private SlotTime() {
    }

    /** Giờ kết thúc này có phải mốc nửa đêm không. */
    public static boolean isMidnightEnd(LocalTime end) {
        return MIDNIGHT_END.equals(end);
    }

    /** Phút trong ngày của giờ BẮT ĐẦU (0..1439). */
    public static int startMinute(LocalTime start) {
        return start == null ? 0 : start.toSecondOfDay() / 60;
    }

    /** Phút trong ngày của giờ KẾT THÚC (1..1440); nửa đêm = 1440. */
    public static int endMinute(LocalTime end) {
        if (end == null) {
            return 0;
        }
        return isMidnightEnd(end) ? MINUTES_PER_DAY : end.toSecondOfDay() / 60;
    }

    /** Thời điểm kết thúc thực tế: nửa đêm thuộc về 00:00 ngày hôm sau. */
    public static LocalDateTime endAt(LocalDate date, LocalTime end) {
        if (date == null || end == null) {
            return null;
        }
        return isMidnightEnd(end) ? date.plusDays(1).atStartOfDay() : date.atTime(end);
    }

    /** Độ dài khung giờ tính bằng giờ; trả 0 nếu thiếu dữ liệu hoặc khoảng không hợp lệ. */
    public static double hours(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return 0;
        }
        int minutes = endMinute(end) - startMinute(start);
        return minutes > 0 ? minutes / 60.0 : 0;
    }

    /** Khung giờ hợp lệ (kết thúc thực sự sau bắt đầu, tính cả mốc nửa đêm). */
    public static boolean isValidRange(LocalTime start, LocalTime end) {
        return start != null && end != null && endMinute(end) > startMinute(start);
    }

    /** Hai khung giờ trong cùng một ngày có chồng lấn nhau không. */
    public static boolean overlaps(
            LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        if (start == null || end == null || otherStart == null || otherEnd == null) {
            return false;
        }
        return startMinute(start) < endMinute(otherEnd) && startMinute(otherStart) < endMinute(end);
    }
}
