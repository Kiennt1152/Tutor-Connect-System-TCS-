package com.tcs.module.marketplace.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một ngoại lệ lịch: dời buổi học của lớp từ {@code originalDate} sang {@code newDate}
 * với khung giờ mới. Được lưu trong bảng system_parameters (không cần bảng mới).
 */
public record RescheduleEntry(
        Long classId,
        LocalDate originalDate,
        LocalDate newDate,
        LocalTime newStartTime,
        LocalTime newEndTime,
        String status, // PENDING | APPROVED | REJECTED
        Long tutorId,
        String reason) {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
}
