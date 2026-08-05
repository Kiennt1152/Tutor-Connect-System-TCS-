package com.tcs.module.contract.dto.response;

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

    private boolean isOtpExpired;
}
