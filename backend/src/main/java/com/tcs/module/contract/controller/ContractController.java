package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.GenerateContractRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.service.ContractService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // ----- Review (existing) -----

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@RequestBody CreateReviewRequest request) {
        return contractService.createReview(request);
    }

    @GetMapping("/reviews/tutor/{tutorUserId}")
    public List<ReviewResponse> getReviewsForTutor(@PathVariable Long tutorUserId) {
        return contractService.getReviewsForTutor(tutorUserId);
    }

    // ----- 4.1: Generate contract -----

    @PostMapping("/generate/assignment/{assignmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForAssignment(@PathVariable Long assignmentId) {
        ContractResponse result = contractService.getMyContract(
                contractService.generateForAssignment(assignmentId).getContractId());
        return result;
    }

    @PostMapping("/generate/enrollment/{classStudentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForEnrollment(@PathVariable Long classStudentId) {
        return contractService.getMyContract(
                contractService.generateForEnrollment(classStudentId).getContractId());
    }

    // ----- 4.2: View contract -----

    @GetMapping
    public List<ContractResponse> getMyContracts() {
        return contractService.getMyContracts();
    }

    @GetMapping("/{contractId}")
    public ContractResponse getContract(@PathVariable Long contractId) {
        return contractService.getMyContract(contractId);
    }

    // ----- 4.3: Sign via OTP -----

    @PostMapping("/{contractId}/send-otp")
    public OtpSentResponse sendSignOtp(@PathVariable Long contractId) {
        return contractService.sendSignOtp(contractId);
    }

    @PostMapping("/{contractId}/sign")
    public ContractResponse signContract(
            @PathVariable Long contractId,
            @RequestBody SignContractRequest request) {
        return contractService.signContract(contractId, request);
    }

    // ----- 4.4: Multi-party signature status -----

    @GetMapping("/{contractId}/signatures")
    public SignatureStatusResponse getSignatureStatus(@PathVariable Long contractId) {
        return contractService.getSignatureStatus(contractId);
    }
}
