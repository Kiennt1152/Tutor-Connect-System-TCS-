package com.tcs.module.contract.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.SignContractResponse;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.profile.dto.response.GuardianApprovalResponse;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.module.profile.service.ClientLegalAccountService.LegalAccountContext;
import com.tcs.module.profile.service.GuardianApprovalService;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final AuthHelper authHelper;
    private final ReviewRepository reviewRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ClientLegalAccountService clientLegalAccountService;
    private final GuardianApprovalService guardianApprovalService;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        if (request.getAssignmentId() == null || request.getRevieweeId() == null || request.getRating() == null) {
            throw new IllegalArgumentException("assignmentId, revieweeId và rating là bắt buộc");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");
        }
        User reviewer = userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        User reviewee = userRepository
                .findById(request.getRevieweeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người được đánh giá"));
        ClassAssignment assignment = classAssignmentRepository
                .findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        Review review = new Review();
        review.setAssignment(assignment);
        review.setTutoringClass(assignment.getApplication().getTutoringClass());
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setReviewType(request.getReviewType());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForTutor(Long tutorUserId) {
        return reviewRepository.findByReviewee_UserId(tutorUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SignContractResponse signContract(SignContractRequest request) {
        if (!StringUtils.hasText(request.getTutorName())) {
            throw new IllegalArgumentException("Tên gia sư là bắt buộc");
        }

        Client client = clientRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ForbiddenException("Chỉ khách hàng mới có thể ký hợp đồng"));

        LegalAccountContext legalContext = clientLegalAccountService.resolveForClient(client);
        String contractReference = "CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (legalContext.isDelegatedToParent()) {
            GuardianApprovalResponse approval = guardianApprovalService.submitContractApproval(
                    legalContext, request.getTutorName().trim(), request.getSubjectName(), contractReference);
            return SignContractResponse.builder()
                    .contractReference(contractReference)
                    .signerName(legalContext.getLegalHolderName())
                    .beneficiaryMinorName(legalContext.getBeneficiaryMinorName())
                    .signedByParentOnBehalf(true)
                    .pendingGuardianApproval(true)
                    .guardianApprovalId(approval.getApprovalId())
                    .guardianApprovalStatus(approval.getStatus())
                    .message("Yêu cầu ký hợp đồng đã gửi. Phụ huynh "
                            + legalContext.getLegalHolderName()
                            + " sẽ nhận thông báo qua hệ thống và email để xác nhận.")
                    .build();
        }

        return SignContractResponse.builder()
                .contractReference(contractReference)
                .signerName(client.getFullName())
                .signedByParentOnBehalf(false)
                .pendingGuardianApproval(false)
                .signedAt(LocalDateTime.now())
                .message("Hợp đồng đã được ký thành công")
                .build();
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .assignmentId(review.getAssignment().getAssignmentId())
                .reviewerId(review.getReviewer().getUserId())
                .revieweeId(review.getReviewee().getUserId())
                .reviewType(review.getReviewType())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
