package com.tcs.module.profile.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Đăng ký bận cho NHIỀU ngày cùng lúc (chọn nhiều ô trên lịch tháng rồi lưu một lần).
 *
 * <p>Khung giờ, theo thứ tự ưu tiên:
 * <ol>
 *   <li>{@code ranges} có phần tử: bận đúng các khoảng đó (vd. sáng + tối, rảnh chiều);</li>
 *   <li>{@code startTime}/{@code endTime}: một khoảng duy nhất;</li>
 *   <li>bỏ trống tất cả: bận cả ngày.</li>
 * </ol>
 */
@Getter
@Setter
public class TutorBusyTimeRequest {

    private List<LocalDate> dates;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<TimeRange> ranges;
    private String note;

    @Getter
    @Setter
    public static class TimeRange {
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
