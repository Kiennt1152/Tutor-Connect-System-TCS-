package com.tcs.common.event;

/**
 * BF-04: phát ra khi học viên/phụ huynh đã KÝ xong hợp đồng ghi danh.
 * Module marketplace lắng nghe để mở bước thanh toán escrow. Học viên chỉ chuyển sang ENROLLED
 * khi SePay xác nhận thanh toán thành công.
 */
public record StudentContractSigned(Long classStudentId, Long contractId) {}
