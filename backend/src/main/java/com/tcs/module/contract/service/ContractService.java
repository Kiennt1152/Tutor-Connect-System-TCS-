package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.SignContractResponse;
import java.util.List;

public interface ContractService {

    ReviewResponse createReview(CreateReviewRequest request);

    List<ReviewResponse> getReviewsForTutor(Long tutorUserId);

    SignContractResponse signContract(SignContractRequest request);
}
