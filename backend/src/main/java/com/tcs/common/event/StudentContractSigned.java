package com.tcs.common.event;

/**
 * BF-04: phát ra khi học viên/phụ huynh đã KÝ xong hợp đồng ghi danh.
 * Module marketplace lắng nghe để chuyển học viên sang ENROLLED (chính thức vào lớp)
 * và đóng ghi danh khi đủ sĩ số. Đặt ở common để tránh coupling giữa contract và marketplace.
 */
public record StudentContractSigned(Long classStudentId, Long contractId) {}
