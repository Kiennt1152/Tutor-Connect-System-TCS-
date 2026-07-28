package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.service.ReviewService;
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
    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }

    @GetMapping("/reviews/tutor/{tutorUserId}")
    public List<ReviewResponse> getReviewsForTutor(@PathVariable Long tutorUserId) {
        return reviewService.getReviewsForTutor(tutorUserId);
    }

    @GetMapping
    public List<ContractResponse> getMyContracts() {
        return contractService.getMyContracts();
    }

    @GetMapping("/{contractId}")
    public ContractResponse getContract(@PathVariable Long contractId) {
        return contractService.getMyContract(contractId);
    }

    @GetMapping("/{contractId}/signatures")
    public SignatureStatusResponse getSignatureStatus(@PathVariable Long contractId) {
        return contractService.getSignatureStatus(contractId);
    }

    @GetMapping("/{contractId}/signature-details")
    public ContractSignatureListResponse getSignatureDetails(@PathVariable Long contractId) {
        return contractService.getSignatures(contractId);
    }

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

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateContract(@RequestBody java.util.Map<String, Long> body) {
        Long assignmentId = body.get("assignmentId");
        if (assignmentId == null) {
            throw new IllegalArgumentException("assignmentId là bắt buộc");
        }
        return contractService.generateContract(assignmentId);
    }

    @PostMapping("/generate/assignment/{assignmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForAssignment(@PathVariable Long assignmentId) {
        return contractService.getMyContract(contractService.generateForAssignment(assignmentId).getContractId());
    }

    @PostMapping("/generate/enrollment/{classStudentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForEnrollment(@PathVariable Long classStudentId) {
        return contractService.getMyContract(contractService.generateForEnrollment(classStudentId).getContractId());
    }
}
