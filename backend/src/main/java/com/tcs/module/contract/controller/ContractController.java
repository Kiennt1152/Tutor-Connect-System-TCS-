package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.service.ReviewService;
import java.util.List;
import java.util.Map;
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

    // ─── REVIEW (existing endpoints, delegate to ReviewService) ──────────────

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }

    @GetMapping("/reviews/tutor/{tutorUserId}")
    public List<ReviewResponse> getReviewsForTutor(@PathVariable Long tutorUserId) {
        return reviewService.getReviewsForTutor(tutorUserId);
    }

    // ─── CONTRACT ENDPOINTS (UC-44 M4 - DucHM) ──────────────────────────────

    @GetMapping("/my")
    public List<ContractResponse> getMyContracts() {
        return contractService.getMyContracts();
    }

    @GetMapping("/{id}")
    public ContractResponse getContract(@PathVariable Long id) {
        return contractService.getContract(id);
    }

    @GetMapping("/{id}/signatures")
    public ContractSignatureListResponse getSignatures(@PathVariable Long id) {
        return contractService.getSignatures(id);
    }

    @PostMapping("/{id}/send-otp")
    public Map<String, Object> sendOtp(@PathVariable Long id) {
        return contractService.sendOtp(id);
    }

    @PostMapping("/{id}/sign")
    public ContractResponse signWithOtp(@PathVariable Long id, @RequestBody SignWithOtpRequest request) {
        return contractService.signWithOtp(id, request);
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
}
