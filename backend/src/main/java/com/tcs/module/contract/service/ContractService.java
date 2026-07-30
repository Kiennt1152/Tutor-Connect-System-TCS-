package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.entity.Contract;
import java.util.List;

public interface ContractService {

    ReviewResponse createReview(CreateReviewRequest request);

    List<ReviewResponse> getReviewsForTutor(Long tutorUserId);

    ReviewResponse replyToReview(Long reviewId, ReplyReviewRequest request);

    ReviewResponse updateReview(Long reviewId, CreateReviewRequest request);

    void recomputeReputationByTutorUser(Long tutorUserId);

    TutorReputationResponse getTutorReputation(Long tutorId);

    TutorReputationResponse getMyTutorReputation();

    List<ReviewableAssignmentResponse> getMyReviewableAssignments();

    default Contract generateForAssignment(Long assignmentId) {
        throw new UnsupportedOperationException("TODO M4: generateForAssignment");
    }

    default Contract generateForEnrollment(Long classStudentId) {
        throw new UnsupportedOperationException("TODO M4: generateForEnrollment");
    }

    default void sign(Long contractId, String otp, Long signerUserId) {
        throw new UnsupportedOperationException("TODO M4: sign");
    }

    default boolean isFullySigned(Long contractId) {
        throw new UnsupportedOperationException("TODO M4: isFullySigned");
    }
}
