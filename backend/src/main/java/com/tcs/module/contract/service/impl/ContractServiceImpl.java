package com.tcs.module.contract.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.ReviewCriterionDto;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.entity.ReputationHistory;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.enums.ReviewType;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuthHelper authHelper;
    private final ReviewRepository reviewRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final ClientRepository clientRepository;
    private final ReputationHistoryRepository reputationHistoryRepository;
    private final LessonRepository lessonRepository;

    private static final String DEFAULT_ANONYMOUS_NAME = "Người dùng ẩn danh";

    private static final int REVIEW_REQUIRED_WITHIN_MONTHS = 1;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        Long reviewerId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        if (request.getAssignmentId() == null) {
            throw new IllegalArgumentException("assignmentId là bắt buộc");
        }
        BigDecimal overallRating = resolveOverallRating(request);
        if (overallRating.compareTo(BigDecimal.ONE) < 0
                || overallRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }

        ClassAssignment assignment = classAssignmentRepository
                .findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        TutoringClass tutoringClass = resolveClass(assignment);

        if (!tutoringClass.getCreator().getUserId().equals(reviewerId)) {
            throw new BusinessException("Bạn chỉ có thể đánh giá lớp do chính mình tạo");
        }

        int eligible = occurredLessonCount(tutoringClass.getClassId());
        long submitted = reviewRepository.countByAssignment_AssignmentIdAndReviewer_UserIdAndReviewType(
                assignment.getAssignmentId(), reviewerId, ReviewType.CLIENT_TO_TUTOR);
        if (submitted >= eligible) {
            throw new BusinessException(
                    "Chưa có buổi học mới để đánh giá. Sau mỗi buổi học bạn đánh giá thêm được một lần.");
        }

        Tutor tutor = assignment.getTutor();
        User tutorUser = tutor.getUser();

        User reviewer = userRepository
                .findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Review review = new Review();
        review.setAssignment(assignment);
        review.setTutoringClass(tutoringClass);
        review.setReviewer(reviewer);
        review.setReviewee(tutorUser);
        review.setReviewType(ReviewType.CLIENT_TO_TUTOR);
        review.setRating(overallRating);
        review.setComment(trimToNull(request.getComment()));
        review.setCriteriaJson(serializeCriteria(request.getCriteria()));
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        review.setAnonymous(anonymous);
        review.setDisplayName(anonymous ? trimToNull(request.getDisplayName()) : null);
        Review saved = reviewRepository.save(review);

        recomputeTutorReputation(tutor, tutorUser.getUserId());

        return toResponse(saved);
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
    public ReviewResponse replyToReview(Long reviewId, ReplyReviewRequest request) {
        Long tutorUserId = authHelper.requireRole(UserRole.TUTOR).getUserId();

        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        if (review.getReviewType() != ReviewType.CLIENT_TO_TUTOR
                || !review.getReviewee().getUserId().equals(tutorUserId)) {
            throw new BusinessException("Bạn chỉ có thể phản hồi đánh giá dành cho chính mình");
        }

        String reply = trimToNull(request.getReply());
        if (reply == null) {
            throw new IllegalArgumentException("Nội dung phản hồi không được để trống");
        }

        review.setTutorReply(reply);
        review.setTutorReplyAt(java.time.LocalDateTime.now());
        Review saved = reviewRepository.save(review);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, CreateReviewRequest request) {
        Long reviewerId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        if (review.getReviewType() != ReviewType.CLIENT_TO_TUTOR
                || !review.getReviewer().getUserId().equals(reviewerId)) {
            throw new BusinessException("Bạn chỉ có thể chỉnh sửa đánh giá do chính mình gửi");
        }

        BigDecimal overallRating = resolveOverallRating(request);
        if (overallRating.compareTo(BigDecimal.ONE) < 0
                || overallRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }

        review.setRating(overallRating);
        review.setComment(trimToNull(request.getComment()));
        review.setCriteriaJson(serializeCriteria(request.getCriteria()));
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        review.setAnonymous(anonymous);
        review.setDisplayName(anonymous ? trimToNull(request.getDisplayName()) : null);
        Review saved = reviewRepository.save(review);

        Tutor tutor = review.getAssignment().getTutor();
        recomputeTutorReputation(tutor, review.getReviewee().getUserId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorReputationResponse getTutorReputation(Long tutorId) {
        Tutor tutor = tutorRepository
                .findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));
        return buildReputation(tutor);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorReputationResponse getMyTutorReputation() {
        Long userId = authHelper.requireRole(UserRole.TUTOR).getUserId();
        Tutor tutor = tutorRepository
                .findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
        return buildReputation(tutor);
    }

    private TutorReputationResponse buildReputation(Tutor tutor) {
        Long tutorUserId = tutor.getUser().getUserId();

        List<Review> visible = reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                tutorUserId, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE);

        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, 0);
        }
        for (Review r : visible) {
            int star = r.getRating().setScale(0, RoundingMode.HALF_UP).intValue();
            star = Math.min(5, Math.max(1, star));
            distribution.merge(star, 1, Integer::sum);
        }

        List<ReviewResponse> reviews = visible.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();

        return TutorReputationResponse.builder()
                .tutorId(tutor.getTutorId())
                .tutorUserId(tutorUserId)
                .fullName(tutor.getFullName())
                .avatar(tutor.getAvatar())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .verificationStatus(
                        tutor.getVerificationStatus() != null
                                ? tutor.getVerificationStatus().name()
                                : null)
                .ratingAvg(tutor.getRatingAvg() != null ? tutor.getRatingAvg() : BigDecimal.ZERO)
                .totalReviews(visible.size())
                .ratingDistribution(distribution)
                .criteriaAverages(criteriaAverages(visible))
                .reviews(reviews)
                .build();
    }

    private List<TutorReputationResponse.CriterionAverage> criteriaAverages(List<Review> reviews) {
        Map<String, int[]> sumCount = new LinkedHashMap<>();
        Map<String, String> questions = new LinkedHashMap<>();
        for (Review r : reviews) {
            if (r.getCriteriaJson() == null || r.getCriteriaJson().isBlank()) {
                continue;
            }
            List<ReviewCriterionDto> criteria;
            try {
                criteria = OBJECT_MAPPER.readValue(
                        r.getCriteriaJson(), new TypeReference<List<ReviewCriterionDto>>() {});
            } catch (Exception e) {
                continue;
            }
            for (ReviewCriterionDto c : criteria) {
                if (c.getCode() == null || c.getScore() == null) {
                    continue;
                }
                int[] acc = sumCount.computeIfAbsent(c.getCode(), k -> new int[2]);
                acc[0] += c.getScore();
                acc[1] += 1;
                questions.putIfAbsent(c.getCode(), c.getQuestion());
            }
        }
        return sumCount.entrySet().stream()
                .map(e -> TutorReputationResponse.CriterionAverage.builder()
                        .code(e.getKey())
                        .question(questions.get(e.getKey()))
                        .average(BigDecimal.valueOf((double) e.getValue()[0] / e.getValue()[1])
                                .setScale(1, RoundingMode.HALF_UP))
                        .count(e.getValue()[1])
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewableAssignmentResponse> getMyReviewableAssignments() {
        Long clientId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        Map<Long, List<Review>> reviewsByAssignment =
                reviewRepository.findByReviewer_UserId(clientId).stream()
                        .filter(r -> r.getReviewType() == ReviewType.CLIENT_TO_TUTOR)
                        .collect(Collectors.groupingBy(r -> r.getAssignment().getAssignmentId()));

        return classAssignmentRepository
                .findByApplication_TutoringClass_Creator_UserId(clientId).stream()
                .filter(a -> a.getApplication() != null
                        && a.getApplication().getTutoringClass() != null)
                .map(a -> {
                    TutoringClass c = a.getApplication().getTutoringClass();
                    List<Review> reviews = reviewsByAssignment.getOrDefault(a.getAssignmentId(), List.of());
                    int submitted = reviews.size();

                    List<LocalDate> occurred = occurredLessonDates(c.getClassId());
                    boolean reviewable = submitted < occurred.size();

                    if (!reviewable && submitted == 0) {
                        return null;
                    }

                    boolean reviewOverdue = reviewable && isReviewOverdue(reviews, occurred);

                    Tutor tutor = a.getTutor();
                    Review latest = reviews.stream()
                            .max((x, y) -> x.getCreatedAt().compareTo(y.getCreatedAt()))
                            .orElse(null);
                    return ReviewableAssignmentResponse.builder()
                            .assignmentId(a.getAssignmentId())
                            .classId(c.getClassId())
                            .classTitle(c.getTitle())
                            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                            .classStatus(c.getStatus().name())
                            .tutorUserId(tutor.getUser().getUserId())
                            .tutorName(tutor.getFullName())
                            .reviewed(submitted > 0)
                            .reviewsSubmitted(submitted)
                            .reviewable(reviewable)
                            .reviewOverdue(reviewOverdue)
                            .reviewId(latest != null ? latest.getReviewId() : null)
                            .rating(latest != null ? latest.getRating() : null)
                            .comment(latest != null ? latest.getComment() : null)
                            .criteriaJson(latest != null ? latest.getCriteriaJson() : null)
                            .anonymous(latest != null && latest.isAnonymous())
                            .reviewerDisplayName(latest != null ? resolveReviewerDisplayName(latest) : null)
                            .reviewedAt(latest != null ? latest.getCreatedAt() : null)
                            .tutorReply(latest != null ? latest.getTutorReply() : null)
                            .tutorReplyAt(latest != null ? latest.getTutorReplyAt() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<LocalDate> occurredLessonDates(Long classId) {
        LocalDate today = LocalDate.now();
        return lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(classId).stream()
                .map(Lesson::getLessonDate)
                .filter(d -> !d.isAfter(today))
                .sorted()
                .toList();
    }

    private int occurredLessonCount(Long classId) {
        return occurredLessonDates(classId).size();
    }

    private boolean isReviewOverdue(List<Review> reviews, List<LocalDate> occurred) {
        LocalDate ref = reviews.isEmpty()
                ? (occurred.isEmpty() ? null : occurred.get(0))
                : reviews.stream()
                        .map(r -> r.getCreatedAt().toLocalDate())
                        .max((x, y) -> x.compareTo(y))
                        .orElse(null);
        return ref != null
                && !LocalDate.now().isBefore(ref.plusMonths(REVIEW_REQUIRED_WITHIN_MONTHS));
    }

    private BigDecimal resolveOverallRating(CreateReviewRequest request) {
        List<ReviewCriterionDto> criteria = request.getCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            for (ReviewCriterionDto c : criteria) {
                if (c.getScore() == null || c.getScore() < 1 || c.getScore() > 5) {
                    throw new IllegalArgumentException("Mỗi tiêu chí phải được chấm từ 1 đến 5 sao");
                }
            }
            double average = criteria.stream().mapToInt(ReviewCriterionDto::getScore).average().orElse(0d);
            return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        }
        if (request.getRating() == null) {
            throw new IllegalArgumentException("Vui lòng chấm điểm đánh giá");
        }
        return BigDecimal.valueOf(request.getRating()).setScale(1, RoundingMode.HALF_UP);
    }

    private String serializeCriteria(List<ReviewCriterionDto> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(criteria);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được dữ liệu tiêu chí đánh giá", e);
        }
    }

    private TutoringClass resolveClass(ClassAssignment assignment) {
        if (assignment.getApplication() == null || assignment.getApplication().getTutoringClass() == null) {
            throw new BusinessException("Lớp này chưa hỗ trợ đánh giá");
        }
        return assignment.getApplication().getTutoringClass();
    }

    @Override
    @Transactional
    public void recomputeReputationByTutorUser(Long tutorUserId) {
        tutorRepository
                .findByUser_UserId(tutorUserId)
                .ifPresent(tutor -> recomputeTutorReputation(tutor, tutorUserId));
    }

    private void recomputeTutorReputation(Tutor tutor, Long tutorUserId) {
        List<Review> visible = reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                tutorUserId, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE);
        double average = visible.isEmpty()
                ? 0d
                : visible.stream()
                        .mapToDouble(r -> r.getRating().doubleValue())
                        .average()
                        .orElse(0d);
        BigDecimal newScore = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        BigDecimal oldScore = tutor.getRatingAvg() == null ? BigDecimal.ZERO : tutor.getRatingAvg();
        if (oldScore.compareTo(newScore) == 0) {
            return;
        }
        tutor.setRatingAvg(newScore);
        tutorRepository.save(tutor);

        ReputationHistory history = new ReputationHistory();
        history.setTutor(tutor);
        history.setOldScore(oldScore);
        history.setNewScore(newScore);
        history.setTriggerType("REVIEW");
        history.setReason("Cập nhật điểm trung bình sau đánh giá mới (" + visible.size() + " lượt)");
        reputationHistoryRepository.save(history);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewResponse toResponse(Review review) {
        TutoringClass reviewClass = review.getTutoringClass();
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .assignmentId(review.getAssignment().getAssignmentId())
                .reviewerId(review.getReviewer().getUserId())
                .revieweeId(review.getReviewee().getUserId())
                .reviewType(review.getReviewType())
                .rating(review.getRating())
                .comment(review.getComment())
                .tutorReply(review.getTutorReply())
                .tutorReplyAt(review.getTutorReplyAt())
                .criteriaJson(review.getCriteriaJson())
                .classTitle(reviewClass != null ? reviewClass.getTitle() : null)
                .subjectName(
                        reviewClass != null && reviewClass.getSubject() != null
                                ? reviewClass.getSubject().getSubjectName()
                                : null)
                .anonymous(review.isAnonymous())
                .reviewerDisplayName(resolveReviewerDisplayName(review))
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String resolveReviewerDisplayName(Review review) {
        if (review.isAnonymous()) {
            String custom = trimToNull(review.getDisplayName());
            return custom != null ? custom : DEFAULT_ANONYMOUS_NAME;
        }
        return clientRepository
                .findByUser_UserId(review.getReviewer().getUserId())
                .map(Client::getFullName)
                .orElse(DEFAULT_ANONYMOUS_NAME);
    }
}
