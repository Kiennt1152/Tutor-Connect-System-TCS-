package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.GenerateContractRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.service.ReviewService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateContract(@RequestBody GenerateContractRequest request) {
        if (request.getAssignmentId() != null) {
            return contractService.generateContract(request.getAssignmentId());
        }
        if (request.getClassStudentId() != null) {
            return contractService.getMyContract(
                    contractService.generateForEnrollment(request.getClassStudentId()).getContractId());
        }
        throw new IllegalArgumentException("assignmentId hoặc classStudentId là bắt buộc");
    }

    @PostMapping("/generate/assignment/{assignmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForAssignment(@PathVariable Long assignmentId) {
        return contractService.getMyContract(
                contractService.generateForAssignment(assignmentId).getContractId());
    }

    @PostMapping("/generate/enrollment/{classStudentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForEnrollment(@PathVariable Long classStudentId) {
        return contractService.getMyContract(
                contractService.generateForEnrollment(classStudentId).getContractId());
    }

    @GetMapping
    public List<ContractResponse> getMyContracts() {
        return contractService.getMyContracts();
    }

    @GetMapping("/my")
    public List<ContractResponse> getMyContractsAlias() {
        return contractService.getMyContracts();
    }

    @GetMapping("/{contractId}")
    public ContractResponse getContract(@PathVariable Long contractId) {
        return contractService.getMyContract(contractId);
    }

    @GetMapping("/{contractId}/signatures")
    public ContractSignatureListResponse getSignatures(@PathVariable Long contractId) {
        return contractService.getSignatures(contractId);
    }

    @PostMapping("/{contractId}/send-otp")
    public Map<String, Object> sendSignOtp(@PathVariable Long contractId) {
        return contractService.sendOtp(contractId);
    }

    @PostMapping("/{contractId}/sign")
    public ContractResponse signContract(
            @PathVariable Long contractId,
            @RequestBody SignContractRequest request) {
        return contractService.signContract(contractId, request);
    }

    @PostMapping("/reviews/{reviewId}/reply")
    public ReviewResponse replyToReview(
            @PathVariable Long reviewId, @RequestBody ReplyReviewRequest request) {
        return contractService.replyToReview(reviewId, request);
    }

    @PutMapping("/reviews/{reviewId}")
    public ReviewResponse updateReview(
            @PathVariable Long reviewId, @RequestBody CreateReviewRequest request) {
        return contractService.updateReview(reviewId, request);
    }

    @GetMapping("/reviews/reputation/{tutorId}")
    public TutorReputationResponse getTutorReputation(@PathVariable Long tutorId) {
        return contractService.getTutorReputation(tutorId);
    }

    @GetMapping("/reviews/my-reputation")
    public TutorReputationResponse getMyTutorReputation() {
        return contractService.getMyTutorReputation();
    }

    @GetMapping("/reviews/reviewable")
    public List<ReviewableAssignmentResponse> getMyReviewableAssignments() {
        return contractService.getMyReviewableAssignments();
    }
}
