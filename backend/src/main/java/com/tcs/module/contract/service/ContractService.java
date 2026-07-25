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

    ContractResponse getContract(Long contractId);

    ContractSignatureListResponse getSignatures(Long contractId);

    Map<String, Object> sendOtp(Long contractId);

    ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);

    ContractResponse generateContract(Long assignmentId);

    Contract generateForAssignment(Long assignmentId);

    Contract generateForEnrollment(Long classStudentId);

    void sign(Long contractId, String otp, Long signerUserId);

    boolean isFullySigned(Long contractId);

    ContractResponse getMyContract(Long contractId);

    List<ContractResponse> getMyContracts();

    OtpSentResponse sendSignOtp(Long contractId);

    ContractResponse signContract(Long contractId, SignContractRequest request);

    SignatureStatusResponse getSignatureStatus(Long contractId);
}
