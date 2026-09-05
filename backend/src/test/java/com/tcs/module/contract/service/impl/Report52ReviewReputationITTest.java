package com.tcs.module.contract.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
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
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52ReviewReputationITTest {


    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;
    private static final Long STRANGER_USER_ID = 33L;
    private static final Long CLASS_ID = 44L;
    private static final Long ASSIGNMENT_ID = 55L;
    private static final Long LESSON_ID = 66L;

    @Mock private AuthHelper authHelper;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;
    @Mock private EscrowService escrowService;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ReviewRepository reviewRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ReputationHistoryRepository reputationHistoryRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private CccdService cccdService;

    @InjectMocks
    private ContractServiceImpl contractService;

    
    /**
     * Test Case: IT-REV-001
     * Title: Create a client review after an eligible lesson and recalculate tutor reputation.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: assignmentId=800; rating=4.
     * Steps:
     *   1. Prepare the fixture: Client owns an eligible completed assignment and has not used the review allowance.
     *   2. Use the input: assignmentId=800; rating=4.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_001_ClientCreatesReviewAfterCompletedLessonAndTutorReputationIsRecalculated.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert review ids/parties/rating, tutor average, history save and event.
     * Expected: Review 501 is saved for the tutor with rating 4, tutor average becomes 4.00, reputation history is saved and the class-reviewed event is published.
     * Pre-conditions: Client owns an eligible completed assignment and has not used the review allowance.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-001: Create a client review after an eligible lesson and recalculate tutor reputation.")
    void IT_REV_001_ClientCreatesReviewAfterCompletedLessonAndTutorReputationIsRecalculated() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        ClassAssignment assignment = privateAssignment(client, tutor);
        Review[] savedHolder = new Review[1];

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(CLASS_ID, CLIENT_USER_ID))
                .thenReturn(false);
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLesson()));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of());
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(clientProfile(client)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setReviewId(501L);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 31, 10, 0));
            savedHolder[0] = saved;
            return saved;
        });
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenAnswer(invocation -> List.of(savedHolder[0]));

        ReviewResponse response = contractService.createReview(reviewRequest(4));

        assertEquals(501L, response.getReviewId());
        assertEquals(CLIENT_USER_ID, response.getReviewerId());
        assertEquals(TUTOR_USER_ID, response.getRevieweeId());
        assertEquals(new BigDecimal("4.0"), response.getRating());
        assertEquals(new BigDecimal("4.00"), tutor.getRatingAvg());
        verify(tutorRepository).save(tutor);
        verify(reputationHistoryRepository).save(any(ReputationHistory.class));
        verify(eventPublisher).publishEvent(any(com.tcs.module.marketplace.event.ClientReviewedClassEvent.class));
    }

    /**
     * Test Case: IT-REV-002
     * Title: List classes that the current client may review and has not yet reviewed.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyReviewableAssignments (GET /api/contract/reviews/reviewable).
     * Input: Authenticated client session.
     * Steps:
     *   1. Prepare the fixture: Client has an occurred completed assignment and no review for it.
     *   2. Use the input: Authenticated client session.
     *   3. Execute ContractServiceImpl.getMyReviewableAssignments (GET /api/contract/reviews/reviewable). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_002_ListMyReviewableAssignmentsReturnsOccurredUnreviewedClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert assignment id, class title, reviewable=true and reviewed=false.
     * Expected: One eligible assignment is returned as reviewable and unreviewed with the expected class title.
     * Pre-conditions: Client has an occurred completed assignment and no review for it.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-002: List classes that the current client may review and has not yet reviewed.")
    void IT_REV_002_ListMyReviewableAssignmentsReturnsOccurredUnreviewedClass() {
        User client = user(CLIENT_USER_ID);
        Tutor tutor = tutor(user(TUTOR_USER_ID));
        ClassAssignment assignment = privateAssignment(client, tutor);

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of());
        when(classAssignmentRepository.findByApplication_TutoringClass_Creator_UserId(CLIENT_USER_ID))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByEnrolledByUser_UserIdAndStatus(
                CLIENT_USER_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLesson()));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());

        List<ReviewableAssignmentResponse> responses = contractService.getMyReviewableAssignments();

        assertEquals(1, responses.size());
        assertEquals(ASSIGNMENT_ID + CLASS_ID, responses.get(0).getAssignmentId());
        assertEquals("Lớp Toán 12", responses.get(0).getClassTitle());
        assertTrue(responses.get(0).isReviewable());
        assertFalse(responses.get(0).isReviewed());
    }

    /**
     * Test Case: IT-REV-003
     * Title: Return the latest visible review per class in a tutor reputation response.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getTutorReputation (GET /api/contract/reviews/reputation/{tutorId}).
     * Input: tutorId=77.
     * Steps:
     *   1. Prepare the fixture: Tutor 77 has visible client reviews including older duplicates for a class.
     *   2. Use the input: tutorId=77.
     *   3. Execute ContractServiceImpl.getTutorReputation (GET /api/contract/reviews/reputation/{tutorId}). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_003_GetTutorReputationReturnsOnlyLatestVisibleReviewPerClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert total/review list/id and rating distribution.
     * Expected: Tutor 77’s response contains one visible review, its id and a correct rating distribution.
     * Pre-conditions: Tutor 77 has visible client reviews including older duplicates for a class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-003: Return the latest visible review per class in a tutor reputation response.")
    void IT_REV_003_GetTutorReputationReturnsOnlyLatestVisibleReviewPerClass() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        Review older = review(400L, client, tutorUser, privateAssignment(client, tutor), "3.0",
                LocalDateTime.of(2026, 8, 20, 10, 0));
        Review latest = review(401L, client, tutorUser, privateAssignment(client, tutor), "5.0",
                LocalDateTime.of(2026, 8, 25, 10, 0));

        when(tutorRepository.findById(77L)).thenReturn(Optional.of(tutor));
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenReturn(List.of(older, latest));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(clientProfile(client)));

        TutorReputationResponse response = contractService.getTutorReputation(77L);

        assertEquals(1, response.getTotalReviews());
        assertEquals(1, response.getReviews().size());
        assertEquals(401L, response.getReviews().get(0).getReviewId());
        assertEquals(1, response.getRatingDistribution().get(5));
        assertEquals(0, response.getRatingDistribution().get(3));
    }

    /**
     * Test Case: IT-REV-004
     * Title: Reject review creation when required assignment information is missing.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: Empty CreateReviewRequest.
     * Steps:
     *   1. Prepare the fixture: Client role is authenticated.
     *   2. Use the input: Empty CreateReviewRequest.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_004_RejectCreateReviewWhenAssignmentIdIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify assignment/review writes are skipped.
     * Expected: The service returns “Thiếu thông tin đánh giá” without loading the assignment or saving a review.
     * Pre-conditions: Client role is authenticated.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-004: Reject review creation when required assignment information is missing.")
    void IT_REV_004_RejectCreateReviewWhenAssignmentIdIsMissing() {
        User client = user(CLIENT_USER_ID);

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contractService.createReview(new CreateReviewRequest()));

        assertEquals("Thiếu thông tin đánh giá", exception.getMessage());
        verify(classAssignmentRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-005
     * Title: Reject a review rating outside the 1-to-5 range.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: rating=6.
     * Steps:
     *   1. Prepare the fixture: Client owns an otherwise eligible assignment.
     *   2. Use the input: rating=6.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_005_RejectReviewRatingOutsideAllowedRange.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify ReviewRepository.save is never called.
     * Expected: Rating 6 is rejected with “Số sao phải từ 1 đến 5” and no review is saved.
     * Pre-conditions: Client owns an otherwise eligible assignment.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-005: Reject a review rating outside the 1-to-5 range.")
    void IT_REV_005_RejectReviewRatingOutsideAllowedRange() {
        User client = user(CLIENT_USER_ID);
        Tutor tutor = tutor(user(TUTOR_USER_ID));
        ClassAssignment assignment = privateAssignment(client, tutor);

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(CLASS_ID, CLIENT_USER_ID))
                .thenReturn(false);
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLesson()));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contractService.createReview(reviewRequest(6)));

        assertEquals("Số sao phải từ 1 đến 5", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-006
     * Title: Block an anonymous user from reading reviewable assignments.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyReviewableAssignments (GET /api/contract/reviews/reviewable).
     * Input: No access token.
     * Steps:
     *   1. Prepare the fixture: No authenticated client principal.
     *   2. Use the input: No access token.
     *   3. Execute ContractServiceImpl.getMyReviewableAssignments (GET /api/contract/reviews/reviewable). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_006_BlockAnonymousUserFromReadingReviewableAssignments.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify repository lookup is skipped.
     * Expected: The role guard returns the client-permission error before querying reviews.
     * Pre-conditions: No authenticated client principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-006: Block an anonymous user from reading reviewable assignments.")
    void IT_REV_006_BlockAnonymousUserFromReadingReviewableAssignments() {
        when(authHelper.requireRole(UserRole.CLIENT)).thenThrow(new ForbiddenException("Yêu cầu quyền CLIENT"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyReviewableAssignments());

        assertEquals("Yêu cầu quyền CLIENT", exception.getMessage());
        verify(reviewRepository, never()).findByReviewer_UserId(any());
    }

    /**
     * Test Case: IT-REV-007
     * Title: Prevent a tutor from creating a client review.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: Valid-looking rating=5 request.
     * Steps:
     *   1. Prepare the fixture: Authenticated principal has TUTOR role.
     *   2. Use the input: Valid-looking rating=5 request.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_007_BlockTutorRoleFromCreatingClientReview.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify no save.
     * Expected: The client-only role check rejects the tutor and no review/assignment lookup occurs.
     * Pre-conditions: Authenticated principal has TUTOR role.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-007: Prevent a tutor from creating a client review.")
    void IT_REV_007_BlockTutorRoleFromCreatingClientReview() {
        when(authHelper.requireRole(UserRole.CLIENT)).thenThrow(new ForbiddenException("Yêu cầu quyền CLIENT"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.createReview(reviewRequest(5)));

        assertEquals("Yêu cầu quyền CLIENT", exception.getMessage());
        verify(classAssignmentRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-008
     * Title: Reject a review from a user who did not own or enroll in the class.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: Valid-looking rating=5 request for another client’s assignment.
     * Steps:
     *   1. Prepare the fixture: The current client is not the assignment owner/enrolled student.
     *   2. Use the input: Valid-looking rating=5 request for another client’s assignment.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_008_RejectReviewFromUserWhoDidNotOwnOrEnrollInClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ownership error and no review save.
     * Expected: The service returns “Bạn chỉ có thể đánh giá lớp học của mình” and does not save a review.
     * Pre-conditions: The current client is not the assignment owner/enrolled student.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-008: Reject a review from a user who did not own or enroll in the class.")
    void IT_REV_008_RejectReviewFromUserWhoDidNotOwnOrEnrollInClass() {
        User stranger = user(STRANGER_USER_ID);
        User client = user(CLIENT_USER_ID);
        Tutor tutor = tutor(user(TUTOR_USER_ID));
        ClassAssignment assignment = privateAssignment(client, tutor);

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(stranger, UserRole.CLIENT));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(CLASS_ID, STRANGER_USER_ID))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contractService.createReview(reviewRequest(5)));

        assertEquals("Bạn chỉ có thể đánh giá lớp học của mình", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-009
     * Title: Prevent additional reviews after the client has used all eligible lesson reviews.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: Another rating=4 request for the same assignment.
     * Steps:
     *   1. Prepare the fixture: All completed lessons in the class already have reviews from the client.
     *   2. Use the input: Another rating=4 request for the same assignment.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_009_RejectDuplicateReviewWhenClientAlreadyReviewedEveryCompletedLesson.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert limit error and verify no save.
     * Expected: The service returns the review-limit message and does not create another review.
     * Pre-conditions: All completed lessons in the class already have reviews from the client.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-009: Prevent additional reviews after the client has used all eligible lesson reviews.")
    void IT_REV_009_RejectDuplicateReviewWhenClientAlreadyReviewedEveryCompletedLesson() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        ClassAssignment assignment = privateAssignment(client, tutor);
        Review existing = review(399L, client, tutorUser, assignment, "5.0", LocalDateTime.of(2026, 8, 20, 10, 0));

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(CLASS_ID, CLIENT_USER_ID))
                .thenReturn(false);
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLesson()));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of(existing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contractService.createReview(reviewRequest(4)));

        assertEquals("Bạn đã đánh giá đủ số lượt cho các buổi đã học", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-010
     * Title: Recalculate tutor reputation when a client updates a review.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.updateReview (PUT /api/contract/reviews/{reviewId}).
     * Input: reviewId=501; new rating=3.
     * Steps:
     *   1. Prepare the fixture: Client owns visible review 501 for tutor 22.
     *   2. Use the input: reviewId=501; new rating=3.
     *   3. Execute ContractServiceImpl.updateReview (PUT /api/contract/reviews/{reviewId}). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_010_UpdateReviewRecomputesTutorReputation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response/tutor average and verify tutor/history saves.
     * Expected: Review 501 changes to rating 3, tutor average becomes 3.00 and a new reputation history row is saved.
     * Pre-conditions: Client owns visible review 501 for tutor 22.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-010: Recalculate tutor reputation when a client updates a review.")
    void IT_REV_010_UpdateReviewRecomputesTutorReputation() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        Review review = review(501L, client, tutorUser, privateAssignment(client, tutor), "5.0",
                LocalDateTime.of(2026, 8, 25, 10, 0));

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(reviewRepository.findById(501L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenReturn(List.of(review));

        ReviewResponse response = contractService.updateReview(501L, reviewRequest(3));

        assertEquals(new BigDecimal("3.0"), response.getRating());
        assertEquals(new BigDecimal("3.00"), tutor.getRatingAvg());
        verify(tutorRepository).save(tutor);
        verify(reputationHistoryRepository).save(any(ReputationHistory.class));
    }

    /**
     * Test Case: IT-REV-011
     * Title: Allow the tutor to reply to a review and return the reply timestamp.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.replyToReview (POST /api/contract/reviews/{reviewId}/reply).
     * Input: reviewId=501; non-empty reply.
     * Steps:
     *   1. Prepare the fixture: Tutor owns review 501.
     *   2. Use the input: reviewId=501; non-empty reply.
     *   3. Execute ContractServiceImpl.replyToReview (POST /api/contract/reviews/{reviewId}/reply). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_011_TutorRepliesToReviewAndResponseContainsReplyTimestamp.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert reply text/timestamp and verify review save.
     * Expected: The reply is saved as “Cảm ơn phụ huynh đã phản hồi.” with a non-null tutorReplyAt.
     * Pre-conditions: Tutor owns review 501.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-011: Allow the tutor to reply to a review and return the reply timestamp.")
    void IT_REV_011_TutorRepliesToReviewAndResponseContainsReplyTimestamp() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        Review review = review(501L, client, tutorUser, privateAssignment(client, tutor), "5.0",
                LocalDateTime.of(2026, 8, 25, 10, 0));
        ReplyReviewRequest request = new ReplyReviewRequest();
        request.setReply("Cảm ơn phụ huynh đã phản hồi.");

        when(authHelper.requireRole(UserRole.TUTOR)).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(reviewRepository.findById(501L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(clientProfile(client)));

        ReviewResponse response = contractService.replyToReview(501L, request);

        assertEquals("Cảm ơn phụ huynh đã phản hồi.", response.getTutorReply());
        assertNotNull(response.getTutorReplyAt());
        verify(reviewRepository).save(review);
    }

    /**
     * Test Case: IT-REV-012
     * Title: Reject an empty tutor reply.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.replyToReview (POST /api/contract/reviews/{reviewId}/reply).
     * Input: Blank reply text.
     * Steps:
     *   1. Prepare the fixture: Tutor owns review 501.
     *   2. Use the input: Blank reply text.
     *   3. Execute ContractServiceImpl.replyToReview (POST /api/contract/reviews/{reviewId}/reply). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_012_RejectEmptyTutorReply.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no review save.
     * Expected: The service returns “Nội dung phản hồi không được để trống” and leaves the review unchanged.
     * Pre-conditions: Tutor owns review 501.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-012: Reject an empty tutor reply.")
    void IT_REV_012_RejectEmptyTutorReply() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        Review review = review(501L, client, tutorUser, privateAssignment(client, tutor), "5.0",
                LocalDateTime.of(2026, 8, 25, 10, 0));
        ReplyReviewRequest request = new ReplyReviewRequest();
        request.setReply("   ");

        when(authHelper.requireRole(UserRole.TUTOR)).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(reviewRepository.findById(501L)).thenReturn(Optional.of(review));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contractService.replyToReview(501L, request));

        assertEquals("Nội dung phản hồi không được để trống", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-013
     * Title: Calculate overall rating from criteria scores and store criteria JSON.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: Criteria scores including clarity; overall rating request.
     * Steps:
     *   1. Prepare the fixture: Client has an eligible assignment and no prior review.
     *   2. Use the input: Criteria scores including clarity; overall rating request.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_013_CriteriaScoresDriveOverallRatingAndCriteriaJson.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert rating, criteriaJson content and reputation update.
     * Expected: The response stores the criteria payload, calculates rating 4.0 and updates tutor average to 4.00.
     * Pre-conditions: Client has an eligible assignment and no prior review.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-013: Calculate overall rating from criteria scores and store criteria JSON.")
    void IT_REV_013_CriteriaScoresDriveOverallRatingAndCriteriaJson() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        ClassAssignment assignment = privateAssignment(client, tutor);
        Review[] savedHolder = new Review[1];
        CreateReviewRequest request = reviewRequest(5);
        request.setCriteria(List.of(
                criterion("clarity", "Giảng dễ hiểu", 5),
                criterion("punctuality", "Đúng giờ", 3)));

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(CLASS_ID, CLIENT_USER_ID))
                .thenReturn(false);
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLesson()));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of());
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setReviewId(502L);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 31, 11, 0));
            savedHolder[0] = saved;
            return saved;
        });
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenAnswer(invocation -> List.of(savedHolder[0]));

        ReviewResponse response = contractService.createReview(request);

        assertEquals(new BigDecimal("4.0"), response.getRating());
        assertNotNull(response.getCriteriaJson());
        assertTrue(response.getCriteriaJson().contains("clarity"));
        assertEquals(new BigDecimal("4.00"), tutor.getRatingAvg());
    }

    /**
     * Test Case: IT-REV-014
     * Title: Use the supplied display name for an anonymous review.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.createReview (POST /api/contract/reviews).
     * Input: anonymous=true; displayName=Phụ huynh ẩn danh lớp 12.
     * Steps:
     *   1. Prepare the fixture: Client can submit an eligible review.
     *   2. Use the input: anonymous=true; displayName=Phụ huynh ẩn danh lớp 12.
     *   3. Execute ContractServiceImpl.createReview (POST /api/contract/reviews). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_014_AnonymousReviewUsesProvidedDisplayName.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert anonymous flag/display name in response and captured entity.
     * Expected: The review is anonymous and both response and saved row use “Phụ huynh ẩn danh lớp 12”.
     * Pre-conditions: Client can submit an eligible review.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-014: Use the supplied display name for an anonymous review.")
    void IT_REV_014_AnonymousReviewUsesProvidedDisplayName() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        ClassAssignment assignment = privateAssignment(client, tutor);
        Review[] savedHolder = new Review[1];
        CreateReviewRequest request = reviewRequest(5);
        request.setAnonymous(true);
        request.setDisplayName("Phụ huynh ẩn danh lớp 12");

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(CLASS_ID, CLIENT_USER_ID))
                .thenReturn(false);
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLesson()));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of());
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setReviewId(503L);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 31, 12, 0));
            savedHolder[0] = saved;
            return saved;
        });
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenAnswer(invocation -> List.of(savedHolder[0]));

        ReviewResponse response = contractService.createReview(request);

        assertTrue(response.isAnonymous());
        assertEquals("Phụ huynh ẩn danh lớp 12", response.getReviewerDisplayName());
        assertEquals("Phụ huynh ẩn danh lớp 12", savedHolder[0].getDisplayName());
    }

    /**
     * Test Case: IT-REV-015
     * Title: Load the current tutor’s own reputation summary.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyTutorReputation (GET /api/contract/reviews/my-reputation).
     * Input: Authenticated tutor session.
     * Steps:
     *   1. Prepare the fixture: Authenticated user is linked to tutor 77.
     *   2. Use the input: Authenticated tutor session.
     *   3. Execute ContractServiceImpl.getMyTutorReputation (GET /api/contract/reviews/my-reputation). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_015_GetMyTutorReputationLoadsTutorProfileFromCurrentUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert tutor id/user id/total reviews.
     * Expected: The response identifies tutor 77 and the current user, with the expected review count.
     * Pre-conditions: Authenticated user is linked to tutor 77.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-015: Load the current tutor’s own reputation summary.")
    void IT_REV_015_GetMyTutorReputationLoadsTutorProfileFromCurrentUser() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);

        when(authHelper.requireRole(UserRole.TUTOR)).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenReturn(List.of());

        TutorReputationResponse response = contractService.getMyTutorReputation();

        assertEquals(77L, response.getTutorId());
        assertEquals(TUTOR_USER_ID, response.getTutorUserId());
        assertEquals(0, response.getTotalReviews());
    }

    /**
     * Test Case: IT-REV-016
     * Title: Report whether the client has already reviewed a class.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.hasClientReviewedClass (service query).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: ReviewRepository reports an existing client-to-tutor review for class 77.
     *   2. Use the input: classId=77.
     *   3. Execute ContractServiceImpl.hasClientReviewedClass (service query). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_016_HasClientReviewedClassReturnsTrueWhenReviewExists.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert boolean true.
     * Expected: The method returns true when a visible client review exists for class 77.
     * Pre-conditions: ReviewRepository reports an existing client-to-tutor review for class 77.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-016: Report whether the client has already reviewed a class.")
    void IT_REV_016_HasClientReviewedClassReturnsTrueWhenReviewExists() {
        when(reviewRepository.existsByTutoringClass_ClassIdAndReviewType(
                CLASS_ID, ReviewType.CLIENT_TO_TUTOR))
                .thenReturn(true);

        assertTrue(contractService.hasClientReviewedClass(CLASS_ID));
    }

    /**
     * Test Case: IT-REV-017
     * Title: Prevent a tutor from replying to another tutor’s review.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.replyToReview (POST /api/contract/reviews/{reviewId}/reply).
     * Input: reviewId=501; non-empty reply.
     * Steps:
     *   1. Prepare the fixture: Review 501 belongs to a different tutor.
     *   2. Use the input: reviewId=501; non-empty reply.
     *   3. Execute ContractServiceImpl.replyToReview (POST /api/contract/reviews/{reviewId}/reply). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_017_RejectTutorReplyForReviewBelongingToAnotherTutor.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no review save.
     * Expected: The service returns the ownership error and does not save the reply.
     * Pre-conditions: Review 501 belongs to a different tutor.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-017: Prevent a tutor from replying to another tutor’s review.")
    void IT_REV_017_RejectTutorReplyForReviewBelongingToAnotherTutor() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        User anotherTutorUser = user(99L);
        Review review = review(501L, client, tutorUser, privateAssignment(client, tutor(tutorUser)), "5.0",
                LocalDateTime.of(2026, 8, 25, 10, 0));
        ReplyReviewRequest request = new ReplyReviewRequest();
        request.setReply("Phản hồi nhầm đánh giá");

        when(authHelper.requireRole(UserRole.TUTOR)).thenReturn(new UserPrincipal(anotherTutorUser, UserRole.TUTOR));
        when(reviewRepository.findById(501L)).thenReturn(Optional.of(review));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contractService.replyToReview(501L, request));

        assertEquals("Bạn chỉ có thể phản hồi đánh giá dành cho chính mình", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * Test Case: IT-REV-018
     * Title: Publish the class-reviewed event when a client updates a review.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.updateReview (PUT /api/contract/reviews/{reviewId}).
     * Input: reviewId=501; rating=5.
     * Steps:
     *   1. Prepare the fixture: Client owns visible review 501 and the update is valid.
     *   2. Use the input: reviewId=501; rating=5.
     *   3. Execute ContractServiceImpl.updateReview (PUT /api/contract/reviews/{reviewId}). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_018_UpdateReviewPublishesClientReviewedClassEvent.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify event publisher receives ClientReviewedClassEvent.
     * Expected: Updating review 501 publishes ClientReviewedClassEvent for the completion flow.
     * Pre-conditions: Client owns visible review 501 and the update is valid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-018: Publish the class-reviewed event when a client updates a review.")
    void IT_REV_018_UpdateReviewPublishesClientReviewedClassEvent() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        Review review = review(501L, client, tutorUser, privateAssignment(client, tutor), "4.0",
                LocalDateTime.of(2026, 8, 25, 10, 0));

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(reviewRepository.findById(501L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenReturn(List.of(review));

        contractService.updateReview(501L, reviewRequest(5));

        verify(eventPublisher).publishEvent(any(com.tcs.module.marketplace.event.ClientReviewedClassEvent.class));
    }

    /**
     * Test Case: IT-REV-019
     * Title: Mark an eligible review as overdue after one month without client action.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyReviewableAssignments (GET /api/contract/reviews/reviewable).
     * Input: Authenticated client reviewable-list request.
     * Steps:
     *   1. Prepare the fixture: Client has an occurred unreviewed assignment whose lesson date is beyond the review deadline.
     *   2. Use the input: Authenticated client reviewable-list request.
     *   3. Execute ContractServiceImpl.getMyReviewableAssignments (GET /api/contract/reviews/reviewable). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_019_ReviewableAssignmentsMarksOverdueAfterOneMonthWithoutReview.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert reviewOverdue flag.
     * Expected: The returned assignment has reviewOverdue=true when its lesson is older than one month and remains unreviewed.
     * Pre-conditions: Client has an occurred unreviewed assignment whose lesson date is beyond the review deadline.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-019: Mark an eligible review as overdue after one month without client action.")
    void IT_REV_019_ReviewableAssignmentsMarksOverdueAfterOneMonthWithoutReview() {
        User client = user(CLIENT_USER_ID);
        Tutor tutor = tutor(user(TUTOR_USER_ID));
        ClassAssignment assignment = privateAssignment(client, tutor);

        when(authHelper.requireRole(UserRole.CLIENT)).thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(List.of());
        when(classAssignmentRepository.findByApplication_TutoringClass_Creator_UserId(CLIENT_USER_ID))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByEnrolledByUser_UserIdAndStatus(
                CLIENT_USER_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(completedLessonOn(LESSON_ID, LocalDate.now().minusMonths(2))));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(LESSON_ID))).thenReturn(List.of());

        List<ReviewableAssignmentResponse> responses = contractService.getMyReviewableAssignments();

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).isReviewOverdue());
    }

    /**
     * Test Case: IT-REV-020
     * Title: Recompute and persist a tutor’s average rating and reputation history.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.recomputeReputationByTutorUser (service event path).
     * Input: tutorUserId=22.
     * Steps:
     *   1. Prepare the fixture: Visible reviews for tutor user 22 are available.
     *   2. Use the input: tutorUserId=22.
     *   3. Execute ContractServiceImpl.recomputeReputationByTutorUser (service event path). Mapped test: com.tcs.module.contract.service.impl.Report52ReviewReputationITTest#IT_REV_020_RecomputeReputationByTutorUserStoresNewAverageAndHistory.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert average and verify tutor/history saves.
     * Expected: Tutor 22’s average becomes 4.00 and a ReputationHistory row is saved.
     * Pre-conditions: Visible reviews for tutor user 22 are available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-REV-020: Recompute and persist a tutor’s average rating and reputation history.")
    void IT_REV_020_RecomputeReputationByTutorUserStoresNewAverageAndHistory() {
        User client = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setRatingAvg(BigDecimal.ZERO);
        ClassAssignment assignment = privateAssignment(client, tutor);

        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                TUTOR_USER_ID,
                ReviewType.CLIENT_TO_TUTOR,
                ReviewStatus.VISIBLE))
                .thenReturn(List.of(
                        review(401L, client, tutorUser, assignment, "5.0", LocalDateTime.of(2026, 8, 25, 10, 0)),
                        review(402L, user(12L), tutorUser, assignmentWithClassId(45L, user(12L), tutor), "3.0",
                                LocalDateTime.of(2026, 8, 26, 10, 0))));

        contractService.recomputeReputationByTutorUser(TUTOR_USER_ID);

        assertEquals(new BigDecimal("4.00"), tutor.getRatingAvg());
        verify(tutorRepository).save(tutor);
        verify(reputationHistoryRepository).save(any(ReputationHistory.class));
    }


    private CreateReviewRequest reviewRequest(int rating) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setAssignmentId(ASSIGNMENT_ID);
        request.setRating(rating);
        request.setComment("Gia sư giảng dễ hiểu");
        return request;
    }

    private ReviewCriterionDto criterion(String code, String question, int score) {
        ReviewCriterionDto criterion = new ReviewCriterionDto();
        criterion.setCode(code);
        criterion.setQuestion(question);
        criterion.setScore(score);
        return criterion;
    }

    private ClassAssignment privateAssignment(User client, Tutor tutor) {
        return assignmentWithClassId(CLASS_ID, client, tutor);
    }

    private ClassAssignment assignmentWithClassId(Long classId, User client, Tutor tutor) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(classId);
        tutoringClass.setTitle("Lớp Toán 12");
        tutoringClass.setCreator(client);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(30L + classId);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID + classId);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        assignment.setApplication(application);
        assignment.setTutor(tutor);
        return assignment;
    }

    private Lesson completedLesson() {
        return completedLessonOn(LESSON_ID, LocalDate.of(2026, 8, 30));
    }

    private Lesson completedLessonOn(Long lessonId, LocalDate lessonDate) {
        Lesson lesson = new Lesson();
        lesson.setLessonId(lessonId);
        lesson.setLessonDate(lessonDate);
        lesson.setSequenceNo(1);
        lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
        return lesson;
    }

    private Review review(
            Long reviewId,
            User reviewer,
            User reviewee,
            ClassAssignment assignment,
            String rating,
            LocalDateTime createdAt) {

        Review review = new Review();
        review.setReviewId(reviewId);
        review.setAssignment(assignment);
        review.setTutoringClass(assignment.getApplication().getTutoringClass());
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setReviewType(ReviewType.CLIENT_TO_TUTOR);
        review.setRating(new BigDecimal(rating));
        review.setComment("Đánh giá lớp học");
        review.setStatus(ReviewStatus.VISIBLE);
        review.setCreatedAt(createdAt);
        return review;
    }

    private Tutor tutor(User tutorUser) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(77L);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư Nguyễn Minh Anh");
        tutor.setRatingAvg(BigDecimal.ZERO);
        return tutor;
    }

    private Client clientProfile(User user) {
        Client client = new Client();
        client.setUser(user);
        client.setFullName("Nguyễn Thu Hà");
        client.setPhone("0900000000");
        return client;
    }

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
    }

}
