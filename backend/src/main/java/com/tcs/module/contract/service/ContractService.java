package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import java.util.List;
import java.util.Map;

public interface ContractService {

    ContractResponse generateContract(Long assignmentId);

    ContractResponse getContract(Long contractId);

    List<ContractResponse> getMyContracts();

    ContractSignatureListResponse getSignatures(Long contractId);

    Map<String, Object> sendOtp(Long contractId);

    ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);
}
