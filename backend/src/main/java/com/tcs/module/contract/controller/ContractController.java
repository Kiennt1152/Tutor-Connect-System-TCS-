package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.SignContractResponse;
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

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@RequestBody CreateReviewRequest request) {
        return contractService.createReview(request);
    }

    @GetMapping("/reviews/tutor/{tutorUserId}")
    public List<ReviewResponse> getReviewsForTutor(@PathVariable Long tutorUserId) {
        return contractService.getReviewsForTutor(tutorUserId);
    }

    @PostMapping("/sign")
    @ResponseStatus(HttpStatus.CREATED)
    public SignContractResponse signContract(@RequestBody SignContractRequest request) {
        return contractService.signContract(request);
    }
}
