package com.tcs.module.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookResponse {

    private String status;
    private String message;
    private String reference;

    public PaymentWebhookResponse() {}

    public PaymentWebhookResponse(String status, String message, String reference) {
        this.status = status;
        this.message = message;
        this.reference = reference;
    }

    public static PaymentWebhookResponseBuilder builder() {
        return new PaymentWebhookResponseBuilder();
    }

    public static class PaymentWebhookResponseBuilder {
        private String status;
        private String message;
        private String reference;

        public PaymentWebhookResponseBuilder status(String status) { this.status = status; return this; }
        public PaymentWebhookResponseBuilder message(String message) { this.message = message; return this; }
        public PaymentWebhookResponseBuilder reference(String reference) { this.reference = reference; return this; }

        public PaymentWebhookResponse build() {
            return new PaymentWebhookResponse(status, message, reference);
        }
    }
}
