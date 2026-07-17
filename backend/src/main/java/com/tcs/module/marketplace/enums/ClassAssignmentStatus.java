package com.tcs.module.marketplace.enums;

public enum ClassAssignmentStatus {
    /** Client đã chọn gia sư, đang chờ gia sư bấm nhận lớp. */
    PENDING,
    /** Gia sư đã nhận lớp — lịch dạy đã được sinh. */
    ACTIVE,
    /** Gia sư từ chối; lớp quay lại OPEN để Client chọn người khác. */
    DECLINED,
    TERMINATED
}
