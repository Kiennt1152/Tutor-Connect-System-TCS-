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
    private int signedCount;
    private int requiredSignatures;
    private List<ContractSignatureResponse> signatures;
}
