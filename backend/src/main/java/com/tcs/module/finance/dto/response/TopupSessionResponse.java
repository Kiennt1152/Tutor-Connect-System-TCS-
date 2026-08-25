package com.tcs.module.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupSessionResponse {

    private String reference;
    private BigDecimal amount;
    private String status;
    private String qrUrl;
    private String bankName;
    private String bankBin;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private LocalDateTime expiresAt;
    private long expiresAtMillis;

    public TopupSessionResponse() {}

    public TopupSessionResponse(String reference, BigDecimal amount, String status, String qrUrl, String bankName, String bankBin, String accountNumber, String accountName, String transferContent, LocalDateTime expiresAt, long expiresAtMillis) {
        this.reference = reference;
        this.amount = amount;
        this.status = status;
        this.qrUrl = qrUrl;
        this.bankName = bankName;
        this.bankBin = bankBin;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.transferContent = transferContent;
        this.expiresAt = expiresAt;
        this.expiresAtMillis = expiresAtMillis;
    }

    public static TopupSessionResponseBuilder builder() {
        return new TopupSessionResponseBuilder();
    }

    public static class TopupSessionResponseBuilder {
        private String reference;
        private BigDecimal amount;
        private String status;
        private String qrUrl;
        private String bankName;
        private String bankBin;
        private String accountNumber;
        private String accountName;
        private String transferContent;
        private LocalDateTime expiresAt;
        private long expiresAtMillis;

        public TopupSessionResponseBuilder reference(String reference) { this.reference = reference; return this; }
        public TopupSessionResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TopupSessionResponseBuilder status(String status) { this.status = status; return this; }
        public TopupSessionResponseBuilder qrUrl(String qrUrl) { this.qrUrl = qrUrl; return this; }
        public TopupSessionResponseBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public TopupSessionResponseBuilder bankBin(String bankBin) { this.bankBin = bankBin; return this; }
        public TopupSessionResponseBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public TopupSessionResponseBuilder accountName(String accountName) { this.accountName = accountName; return this; }
        public TopupSessionResponseBuilder transferContent(String transferContent) { this.transferContent = transferContent; return this; }
        public TopupSessionResponseBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public TopupSessionResponseBuilder expiresAtMillis(long expiresAtMillis) { this.expiresAtMillis = expiresAtMillis; return this; }

        public TopupSessionResponse build() {
            return new TopupSessionResponse(reference, amount, status, qrUrl, bankName, bankBin, accountNumber, accountName, transferContent, expiresAt, expiresAtMillis);
        }
    }
}
