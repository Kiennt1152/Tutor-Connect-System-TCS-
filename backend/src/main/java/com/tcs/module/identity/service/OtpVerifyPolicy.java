package com.tcs.module.identity.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu hình cách xác minh OTP cho từng luồng (đăng ký, quên mật khẩu, ký hợp đồng…).
 * Giữ nguyên thông báo lỗi và hành vi (khoá/hiển thị số lần còn lại) như code cũ của mỗi luồng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyPolicy {

    /** Số lần thử tối đa. */
    private int maxAttempts;

    /** Khi nhập sai chạm mốc tối đa thì đánh dấu mã đã dùng (khoá) — luồng ký hợp đồng marketplace. */
    private boolean lockOnMaxAttempts;

    /** Khi nhập sai chạm mốc tối đa thì ném {@code maxAttemptsMessage} thay vì "còn 0 lần". */
    private boolean throwMaxOnReach;

    /** Ghép "còn N lần thử" vào thông báo sai mã ({@code wrongRemainingTemplate}). */
    private boolean showRemaining;

    /** Thông báo khi không nhập mã. */
    private String missingMessage;

    /** Thông báo khi không tìm thấy mã còn hiệu lực. */
    private String notFoundMessage;

    /** Thông báo khi mã hết hạn. */
    private String expiredMessage;

    /** Thông báo khi vượt số lần thử. */
    private String maxAttemptsMessage;

    /** Thông báo sai mã khi KHÔNG hiển thị số lần còn lại. */
    private String wrongMessage;

    /** Mẫu thông báo sai mã có "%d" cho số lần còn lại (khi {@code showRemaining}). */
    private String wrongRemainingTemplate;

    public OtpVerifyPolicy() {}

    public OtpVerifyPolicy(int maxAttempts, boolean lockOnMaxAttempts, boolean throwMaxOnReach, boolean showRemaining, String missingMessage, String notFoundMessage, String expiredMessage, String maxAttemptsMessage, String wrongMessage, String wrongRemainingTemplate) {
        this.maxAttempts = maxAttempts;
        this.lockOnMaxAttempts = lockOnMaxAttempts;
        this.throwMaxOnReach = throwMaxOnReach;
        this.showRemaining = showRemaining;
        this.missingMessage = missingMessage;
        this.notFoundMessage = notFoundMessage;
        this.expiredMessage = expiredMessage;
        this.maxAttemptsMessage = maxAttemptsMessage;
        this.wrongMessage = wrongMessage;
        this.wrongRemainingTemplate = wrongRemainingTemplate;
    }

    public int getMaxAttempts() { return maxAttempts; }
    public boolean isLockOnMaxAttempts() { return lockOnMaxAttempts; }
    public boolean isThrowMaxOnReach() { return throwMaxOnReach; }
    public boolean isShowRemaining() { return showRemaining; }
    public String getMissingMessage() { return missingMessage; }
    public String getNotFoundMessage() { return notFoundMessage; }
    public String getExpiredMessage() { return expiredMessage; }
    public String getMaxAttemptsMessage() { return maxAttemptsMessage; }
    public String getWrongMessage() { return wrongMessage; }
    public String getWrongRemainingTemplate() { return wrongRemainingTemplate; }

    public static OtpVerifyPolicyBuilder builder() {
        return new OtpVerifyPolicyBuilder();
    }

    public static class OtpVerifyPolicyBuilder {
        private int maxAttempts;
        private boolean lockOnMaxAttempts;
        private boolean throwMaxOnReach;
        private boolean showRemaining;
        private String missingMessage;
        private String notFoundMessage;
        private String expiredMessage;
        private String maxAttemptsMessage;
        private String wrongMessage;
        private String wrongRemainingTemplate;

        public OtpVerifyPolicyBuilder maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public OtpVerifyPolicyBuilder lockOnMaxAttempts(boolean lockOnMaxAttempts) { this.lockOnMaxAttempts = lockOnMaxAttempts; return this; }
        public OtpVerifyPolicyBuilder throwMaxOnReach(boolean throwMaxOnReach) { this.throwMaxOnReach = throwMaxOnReach; return this; }
        public OtpVerifyPolicyBuilder showRemaining(boolean showRemaining) { this.showRemaining = showRemaining; return this; }
        public OtpVerifyPolicyBuilder missingMessage(String missingMessage) { this.missingMessage = missingMessage; return this; }
        public OtpVerifyPolicyBuilder notFoundMessage(String notFoundMessage) { this.notFoundMessage = notFoundMessage; return this; }
        public OtpVerifyPolicyBuilder expiredMessage(String expiredMessage) { this.expiredMessage = expiredMessage; return this; }
        public OtpVerifyPolicyBuilder maxAttemptsMessage(String maxAttemptsMessage) { this.maxAttemptsMessage = maxAttemptsMessage; return this; }
        public OtpVerifyPolicyBuilder wrongMessage(String wrongMessage) { this.wrongMessage = wrongMessage; return this; }
        public OtpVerifyPolicyBuilder wrongRemainingTemplate(String wrongRemainingTemplate) { this.wrongRemainingTemplate = wrongRemainingTemplate; return this; }

        public OtpVerifyPolicy build() {
            return new OtpVerifyPolicy(maxAttempts, lockOnMaxAttempts, throwMaxOnReach, showRemaining, missingMessage, notFoundMessage, expiredMessage, maxAttemptsMessage, wrongMessage, wrongRemainingTemplate);
        }
    }
}
