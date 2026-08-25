package com.tcs.module.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupStatusResponse {

    private String reference;
    private String status;
    private String message;
    private WalletResponse wallet;

    public TopupStatusResponse() {}

    public TopupStatusResponse(String reference, String status, String message, WalletResponse wallet) {
        this.reference = reference;
        this.status = status;
        this.message = message;
        this.wallet = wallet;
    }

    public static TopupStatusResponseBuilder builder() {
        return new TopupStatusResponseBuilder();
    }

    public static class TopupStatusResponseBuilder {
        private String reference;
        private String status;
        private String message;
        private WalletResponse wallet;

        public TopupStatusResponseBuilder reference(String reference) { this.reference = reference; return this; }
        public TopupStatusResponseBuilder status(String status) { this.status = status; return this; }
        public TopupStatusResponseBuilder message(String message) { this.message = message; return this; }
        public TopupStatusResponseBuilder wallet(WalletResponse wallet) { this.wallet = wallet; return this; }

        public TopupStatusResponse build() {
            return new TopupStatusResponse(reference, status, message, wallet);
        }
    }
}
