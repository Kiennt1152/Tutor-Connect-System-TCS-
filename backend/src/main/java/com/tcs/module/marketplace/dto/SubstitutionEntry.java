package com.tcs.module.marketplace.dto;

import java.time.LocalDate;

/**
 * Một ngoại lệ lịch: buổi học ngày {@code date} của lớp được gia sư phụ (tutorId) dạy thay
 * cho gia sư chính. Được lưu trong bảng system_parameters (không cần bảng mới), tương tự
 * cơ chế dời lịch {@link RescheduleEntry}.
 */
public record SubstitutionEntry(
        Long classId,
        LocalDate date,
        Long tutorId, // gia sư phụ dạy thay
        String status, // PENDING | APPROVED | REJECTED
        String reason) {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
}
