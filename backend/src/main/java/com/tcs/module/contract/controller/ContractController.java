package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.service.ContractService;
import java.util.List;
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

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@RequestBody CreateReviewRequest request) {
        return contractService.createReview(request);
    }

    @GetMapping("/reviews/tutor/{tutorUserId}")
    public List<ReviewResponse> getReviewsForTutor(@PathVariable Long tutorUserId) {
        return contractService.getReviewsForTutor(tutorUserId);
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
