package com.tcs.module.marketplace.event;

/**
 * Phát ra khi phụ huynh/học viên gửi đánh giá gia sư cho một lớp.
 * Marketplace lắng nghe để đóng lớp (hoàn thành) nếu gia sư đã yêu cầu hoàn thành trước đó.
 */
public record ClientReviewedClassEvent(Long classId) {
}
