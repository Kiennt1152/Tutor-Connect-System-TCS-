package com.tcs.module.marketplace.enums;

/** Vòng đời một yêu cầu đổi lịch/thêm buổi. CANCELLED là do chính người gửi thu hồi. */
public enum RescheduleRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
