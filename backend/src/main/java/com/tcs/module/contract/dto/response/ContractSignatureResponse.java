package com.tcs.module.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.PartyRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractSignatureResponse {

    private Long signatureId;
    private PartyRole partyRole;
    private String partyLabel;

    private Long signerId;
    private String signerName;
    private String signerEmail;

    private ContractSignatureStatus signatureStatus;
    private LocalDateTime signedAt;
    private LocalDateTime otpExpiresAt;
    private int remainingOtpAttempts;

    // Giữ nguyên tên "isOtpExpired"/"isCurrentUser" trong JSON (Jackson mặc định sẽ bỏ tiền tố "is").
    @JsonProperty("isOtpExpired")
    private boolean isOtpExpired;
    /** Ô ký này thuộc về người đang xem (để ẩn/hiện phần ký). */
    @JsonProperty("isCurrentUser")
    private boolean isCurrentUser;
}
