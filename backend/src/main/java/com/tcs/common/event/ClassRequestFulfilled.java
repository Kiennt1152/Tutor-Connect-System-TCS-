package com.tcs.common.event;

/**
 * Phát ra khi phụ huynh đã chọn xong gia sư cho một "yêu cầu nhờ trung tâm tìm gia sư"
 * (yêu cầu chuyển ACCEPTED). Module center lắng nghe để đóng tin tuyển dụng đã đăng cho
 * yêu cầu đó (nếu có). Đặt ở common để tránh coupling giữa module marketplace và center.
 */
public record ClassRequestFulfilled(String requestId) {}
