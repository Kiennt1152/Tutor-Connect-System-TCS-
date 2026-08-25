package com.tcs.module.finance.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    private Long paymentMethodId;
    private String type;
    private String provider;
    private String bankName;
    private String accountHolderName;
    private String lastFour;
    private String accountNoMasked;
    private Boolean isDefault;
    private LocalDateTime verifiedAt;
    private LocalDateTime cooldownUntil;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentMethodResponse() {}

    public PaymentMethodResponse(Long paymentMethodId, String type, String provider, String bankName, String accountHolderName, String lastFour, String accountNoMasked, Boolean isDefault, LocalDateTime verifiedAt, LocalDateTime cooldownUntil, LocalDateTime lastUsedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.paymentMethodId = paymentMethodId;
        this.type = type;
        this.provider = provider;
        this.bankName = bankName;
        this.accountHolderName = accountHolderName;
        this.lastFour = lastFour;
        this.accountNoMasked = accountNoMasked;
        this.isDefault = isDefault;
        this.verifiedAt = verifiedAt;
        this.cooldownUntil = cooldownUntil;
        this.lastUsedAt = lastUsedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentMethodResponseBuilder builder() {
        return new PaymentMethodResponseBuilder();
    }

    public static class PaymentMethodResponseBuilder {
        private Long paymentMethodId;
        private String type;
        private String provider;
        private String bankName;
        private String accountHolderName;
        private String lastFour;
        private String accountNoMasked;
        private Boolean isDefault;
        private LocalDateTime verifiedAt;
        private LocalDateTime cooldownUntil;
        private LocalDateTime lastUsedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PaymentMethodResponseBuilder paymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; return this; }
        public PaymentMethodResponseBuilder type(String type) { this.type = type; return this; }
        public PaymentMethodResponseBuilder provider(String provider) { this.provider = provider; return this; }
        public PaymentMethodResponseBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public PaymentMethodResponseBuilder accountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; return this; }
        public PaymentMethodResponseBuilder lastFour(String lastFour) { this.lastFour = lastFour; return this; }
        public PaymentMethodResponseBuilder accountNoMasked(String accountNoMasked) { this.accountNoMasked = accountNoMasked; return this; }
        public PaymentMethodResponseBuilder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public PaymentMethodResponseBuilder verifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; return this; }
        public PaymentMethodResponseBuilder cooldownUntil(LocalDateTime cooldownUntil) { this.cooldownUntil = cooldownUntil; return this; }
        public PaymentMethodResponseBuilder lastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; return this; }
        public PaymentMethodResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PaymentMethodResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PaymentMethodResponse build() {
            return new PaymentMethodResponse(paymentMethodId, type, provider, bankName, accountHolderName, lastFour, accountNoMasked, isDefault, verifiedAt, cooldownUntil, lastUsedAt, createdAt, updatedAt);
        }
    }
}
