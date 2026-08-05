package com.tcs.exception;

/**
 * Ném khi một hành động yêu cầu tài khoản đã được xác minh nhưng người dùng chưa VERIFIED
 * (VD: gia sư chưa xác minh hồ sơ nhưng bấm ứng tuyển tin tuyển dụng).
 *
 * <p>Cố ý KHÔNG dùng {@link ForbiddenException} (403) vì axios interceptor ở frontend thấy 403
 * sẽ tự điều hướng sang /forbidden. Exception này trả về mã lỗi riêng để frontend điều hướng
 * người dùng sang trang Xác minh thay vì trang cấm truy cập.
 */
public class VerificationRequiredException extends RuntimeException {

    /** Mã lỗi ổn định để frontend nhận diện (thay vì so khớp chuỗi thông báo). */
    public static final String CODE = "VERIFICATION_REQUIRED";

    public VerificationRequiredException(String message) {
        super(message);
    }
}
