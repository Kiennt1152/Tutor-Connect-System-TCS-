package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import java.util.List;
import java.util.Map;

public interface ContractService {

    ContractResponse generateContract(Long assignmentId);

    /** BF-03: tạo thỏa thuận hợp tác center–gia sư từ một đơn ứng tuyển tuyển dụng đã duyệt. */
    ContractResponse generateCooperationContract(Long recruitmentApplicationId);

    /** BF-04: tạo hợp đồng theo học viên khi ghi danh (dùng mẫu hợp đồng của lớp nếu có). */
    ContractResponse generateStudentContract(Long classStudentId);

    ContractResponse getContract(Long contractId);

    List<ContractResponse> getMyContracts();

    ContractSignatureListResponse getSignatures(Long contractId);

    Map<String, Object> sendOtp(Long contractId);

    ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);

    ReviewResponse replyToReview(Long reviewId, ReplyReviewRequest request);

    ReviewResponse updateReview(Long reviewId, CreateReviewRequest request);

    void recomputeReputationByTutorUser(Long tutorUserId);

    TutorReputationResponse getTutorReputation(Long tutorId);

    TutorReputationResponse getMyTutorReputation();

    List<ReviewableAssignmentResponse> getMyReviewableAssignments();
}
