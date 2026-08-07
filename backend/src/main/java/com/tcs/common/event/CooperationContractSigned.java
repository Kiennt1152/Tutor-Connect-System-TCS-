package com.tcs.common.event;

/**
 * BF-03: phát ra khi gia sư đã ký xong thỏa thuận hợp tác với trung tâm.
 * Module center lắng nghe để kích hoạt thành viên (ACTIVE) và đóng tin khi đủ số đã ký.
 * Đặt ở common để tránh coupling giữa module contract và center.
 */
public record CooperationContractSigned(Long recruitmentApplicationId, Long contractId) {}
