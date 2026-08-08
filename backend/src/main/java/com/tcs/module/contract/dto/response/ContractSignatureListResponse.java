package com.tcs.module.contract.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractSignatureListResponse {

    private Long contractId;
    private String contractNo;
    private boolean hasAllSignatures;
    /** Alias của hasAllSignatures cho frontend. */
    private boolean fullySigned;
    private int signedCount;
    private int requiredSignatures;
    /** Alias của requiredSignatures cho frontend. */
    private int totalRequired;
    private List<ContractSignatureResponse> signatures;
}
