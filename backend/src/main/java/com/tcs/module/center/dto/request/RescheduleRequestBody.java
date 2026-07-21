package com.tcs.module.center.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Gia sư yêu cầu dời buổi học từ originalDate sang newDate + khung giờ mới (báo ốm). */
@Getter
@Setter
public class RescheduleRequestBody {

    private LocalDate originalDate;
    private LocalDate newDate;
    // Nhận dạng chuỗi "HH:mm" để tránh phụ thuộc cấu hình Jackson; parse ở service.
    private String newStartTime;
    private String newEndTime;
    private String reason;
}
