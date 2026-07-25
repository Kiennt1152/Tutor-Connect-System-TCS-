package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.entity.Contract;
import java.util.List;

public interface ContractService {

    ReviewResponse createReview(CreateReviewRequest request);

    List<ReviewResponse> getReviewsForTutor(Long tutorUserId);

    /** Danh sach lop da hoan thanh cua khach hang hien tai, kem trang thai da danh gia. */
    List<ReviewableAssignmentResponse> getMyReviewableAssignments();

    // ----- Seam 0.1 (chu: M4) - hop dong dien tu. Default stub de M4 hoan thien. -----

    /** Sinh hop dong PRIVATE tu mot ClassAssignment. */
    default Contract generateForAssignment(Long assignmentId) {
        throw new UnsupportedOperationException("TODO M4: generateForAssignment");
    }

    /** Sinh hop dong CENTER tu mot ghi danh (ClassStudent). */
    default Contract generateForEnrollment(Long classStudentId) {
        throw new UnsupportedOperationException("TODO M4: generateForEnrollment");
    }

    /** Ky hop dong bang OTP email. */
    default void sign(Long contractId, String otp, Long signerUserId) {
        throw new UnsupportedOperationException("TODO M4: sign");
    }

    /** Da du chu ky cua tat ca cac ben chua. */
    default boolean isFullySigned(Long contractId) {
        throw new UnsupportedOperationException("TODO M4: isFullySigned");
    }
}
