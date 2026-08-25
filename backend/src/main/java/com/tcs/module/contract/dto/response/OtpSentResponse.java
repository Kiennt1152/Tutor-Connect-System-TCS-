package com.tcs.module.contract.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSentResponse {

    private String maskedEmail;
    private String message;
    private Integer expiresInMinutes;
    private Integer maxAttempts;

    public OtpSentResponse() {}

    public OtpSentResponse(String maskedEmail, String message, Integer expiresInMinutes, Integer maxAttempts) {
        this.maskedEmail = maskedEmail;
        this.message = message;
        this.expiresInMinutes = expiresInMinutes;
        this.maxAttempts = maxAttempts;
    }

    public String getMaskedEmail() { return maskedEmail; }
    public String getMessage() { return message; }
    public Integer getExpiresInMinutes() { return expiresInMinutes; }
    public Integer getMaxAttempts() { return maxAttempts; }

    public static OtpSentResponseBuilder builder() {
        return new OtpSentResponseBuilder();
    }

    public static class OtpSentResponseBuilder {
        private String maskedEmail;
        private String message;
        private Integer expiresInMinutes;
        private Integer maxAttempts;

        public OtpSentResponseBuilder maskedEmail(String maskedEmail) { this.maskedEmail = maskedEmail; return this; }
        public OtpSentResponseBuilder message(String message) { this.message = message; return this; }
        public OtpSentResponseBuilder expiresInMinutes(Integer expiresInMinutes) { this.expiresInMinutes = expiresInMinutes; return this; }
        public OtpSentResponseBuilder maxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; return this; }

        public OtpSentResponse build() {
            return new OtpSentResponse(maskedEmail, message, expiresInMinutes, maxAttempts);
        }
    }
}
