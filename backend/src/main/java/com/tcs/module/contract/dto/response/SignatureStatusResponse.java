package com.tcs.module.contract.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignatureStatusResponse {

    private Long contractId;
    private String contractNo;
    private boolean fullySigned;
    private int signedCount;
    private int totalRequired;
    private java.util.List<SignatureInfo> signatures;

    @Getter
    @Builder
    public static class SignatureInfo {
        private Long signatureId;
        private Long signerUserId;
        private String signerName;
        private String signerRole;
        private LocalDateTime signedAt;
        private boolean isCurrentUser;
    }
}
