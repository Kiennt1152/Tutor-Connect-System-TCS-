package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.entity.Contract;
import java.util.List;
import java.util.Map;

public interface ContractService {

    Contract generateForAssignment(Long assignmentId);

    Contract generateForEnrollment(Long classStudentId);

    ContractResponse generateContract(Long assignmentId);

    ContractResponse getContract(Long contractId);

    ContractResponse getMyContract(Long contractId);

    List<ContractResponse> getMyContracts();

    ContractSignatureListResponse getSignatures(Long contractId);

    SignatureStatusResponse getSignatureStatus(Long contractId);

    Map<String, Object> sendOtp(Long contractId);

    OtpSentResponse sendSignOtp(Long contractId);

    ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);

    ContractResponse signContract(Long contractId, SignContractRequest request);

    void sign(Long contractId, String otp, Long signerUserId);

    boolean isFullySigned(Long contractId);
}
