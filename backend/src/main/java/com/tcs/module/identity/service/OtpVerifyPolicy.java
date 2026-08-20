package com.tcs.module.identity.service;

import lombok.Builder;
import lombok.Getter;

/**
 * Cấu hình cách xác minh OTP cho từng luồng (đăng ký, quên mật khẩu, ký hợp đồng…).
 * Giữ nguyên thông báo lỗi và hành vi (khoá/hiển thị số lần còn lại) như code cũ của mỗi luồng.
 */
@Getter
@Builder
public class OtpVerifyPolicy {

    /** Số lần thử tối đa. */
    private final int maxAttempts;

    /** Khi nhập sai chạm mốc tối đa thì đánh dấu mã đã dùng (khoá) — luồng ký hợp đồng marketplace. */
    private final boolean lockOnMaxAttempts;

    /** Khi nhập sai chạm mốc tối đa thì ném {@code maxAttemptsMessage} thay vì "còn 0 lần". */
    private final boolean throwMaxOnReach;

    /** Ghép "còn N lần thử" vào thông báo sai mã ({@code wrongRemainingTemplate}). */
    private final boolean showRemaining;

    /** Thông báo khi không nhập mã. */
    private final String missingMessage;

    /** Thông báo khi không tìm thấy mã còn hiệu lực. */
    private final String notFoundMessage;

    /** Thông báo khi mã hết hạn. */
    private final String expiredMessage;

    /** Thông báo khi vượt số lần thử. */
    private final String maxAttemptsMessage;

    /** Thông báo sai mã khi KHÔNG hiển thị số lần còn lại. */
    private final String wrongMessage;

    /** Mẫu thông báo sai mã có "%d" cho số lần còn lại (khi {@code showRemaining}). */
    private final String wrongRemainingTemplate;
}
