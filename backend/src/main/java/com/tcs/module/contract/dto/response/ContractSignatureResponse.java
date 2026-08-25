package com.tcs.module.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.PartyRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignatureResponse {

    private Long signatureId;
    private PartyRole partyRole;
    private String partyLabel;

    private Long signerId;
    private String signerName;
    private String signerEmail;

    private ContractSignatureStatus signatureStatus;
    private LocalDateTime signedAt;

    // Giữ nguyên tên "isCurrentUser" trong JSON (Jackson mặc định sẽ bỏ tiền tố "is").
    /** Ô ký này thuộc về người đang xem (để ẩn/hiện phần ký). */
    @JsonProperty("isCurrentUser")
    private boolean isCurrentUser;

    public static ContractSignatureResponseBuilder builder() {
        return new ContractSignatureResponseBuilder();
    }

    public static class ContractSignatureResponseBuilder {
        private Long signatureId;
        private PartyRole partyRole;
        private String partyLabel;
        private Long signerId;
        private String signerName;
        private String signerEmail;
        private ContractSignatureStatus signatureStatus;
        private LocalDateTime signedAt;
        private boolean isCurrentUser;

        public ContractSignatureResponseBuilder signatureId(Long signatureId) { this.signatureId = signatureId; return this; }
        public ContractSignatureResponseBuilder partyRole(PartyRole partyRole) { this.partyRole = partyRole; return this; }
        public ContractSignatureResponseBuilder partyLabel(String partyLabel) { this.partyLabel = partyLabel; return this; }
        public ContractSignatureResponseBuilder signerId(Long signerId) { this.signerId = signerId; return this; }
        public ContractSignatureResponseBuilder signerName(String signerName) { this.signerName = signerName; return this; }
        public ContractSignatureResponseBuilder signerEmail(String signerEmail) { this.signerEmail = signerEmail; return this; }
        public ContractSignatureResponseBuilder signatureStatus(ContractSignatureStatus signatureStatus) { this.signatureStatus = signatureStatus; return this; }
        public ContractSignatureResponseBuilder signedAt(LocalDateTime signedAt) { this.signedAt = signedAt; return this; }
        public ContractSignatureResponseBuilder isCurrentUser(boolean isCurrentUser) { this.isCurrentUser = isCurrentUser; return this; }

        public ContractSignatureResponse build() {
            return new ContractSignatureResponse(signatureId, partyRole, partyLabel, signerId, signerName, signerEmail, signatureStatus, signedAt, isCurrentUser);
        }
    }
}
