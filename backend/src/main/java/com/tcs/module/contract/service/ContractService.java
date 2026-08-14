package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.entity.Contract;
import java.util.List;
import java.util.Map;

public interface ContractService {

    /**
     * BF-03: tạo thỏa thuận hợp tác center–gia sư từ một đơn ứng tuyển tuyển dụng đã duyệt.
     * {@code templateId} (tuỳ chọn): mẫu hợp đồng center chọn khi duyệt.
     * {@code editedTerms} (tuỳ chọn): nội dung điều khoản center tự nhập/sửa khi duyệt — ưu tiên
     * dùng thay nội dung mẫu. Hệ thống tự chèn khung chuẩn + điền dữ liệu (placeholder) rồi ĐÓNG BĂNG.
     */
    ContractResponse generateCooperationContract(
            Long recruitmentApplicationId, Long templateId, String editedTerms);

    /** BF-03 (exception): gia sư từ chối thỏa thuận hợp tác chưa ký -> chấm dứt + đóng đơn. */
    void declineCooperationContract(Long contractId);

    /** BF-04: tạo hợp đồng theo học viên khi ghi danh (dùng mẫu hợp đồng của lớp nếu có). */
    ContractResponse generateStudentContract(Long classStudentId);

    ContractResponse getContract(Long contractId);

    ContractSignatureListResponse getSignatures(Long contractId);

    Map<String, Object> sendOtp(Long contractId);

    ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);

    ContractResponse saveRefundPayoutInfo(Long contractId, SaveRefundPayoutRequest request);

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

    ReviewResponse createReview(CreateReviewRequest request);

    /** True nếu lớp đã có ít nhất một đánh giá của phụ huynh/học viên dành cho gia sư. */
    boolean hasClientReviewedClass(Long classId);

    ReviewResponse replyToReview(Long reviewId, ReplyReviewRequest request);

    ReviewResponse updateReview(Long reviewId, CreateReviewRequest request);

    void recomputeReputationByTutorUser(Long tutorUserId);

    TutorReputationResponse getTutorReputation(Long tutorId);

    TutorReputationResponse getMyTutorReputation();

    List<ReviewableAssignmentResponse> getMyReviewableAssignments();
}
