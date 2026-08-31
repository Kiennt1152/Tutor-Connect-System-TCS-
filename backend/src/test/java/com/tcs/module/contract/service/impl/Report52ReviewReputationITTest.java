package com.tcs.module.contract.service.impl;

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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52ReviewReputationITTest {

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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_REV_006_BlockAnonymousUserFromReadingReviewableAssignments() {
        when(authHelper.requireRole(UserRole.CLIENT)).thenThrow(new ForbiddenException("Yêu cầu quyền CLIENT"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyReviewableAssignments());

        assertEquals("Yêu cầu quyền CLIENT", exception.getMessage());
        verify(reviewRepository, never()).findByReviewer_UserId(any());
    }

    @Test
    @Tag("report52-it")
    void IT_REV_007_BlockTutorRoleFromCreatingClientReview() {
        when(authHelper.requireRole(UserRole.CLIENT)).thenThrow(new ForbiddenException("Yêu cầu quyền CLIENT"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.createReview(reviewRequest(5)));

        assertEquals("Yêu cầu quyền CLIENT", exception.getMessage());
        verify(classAssignmentRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_REV_016_HasClientReviewedClassReturnsTrueWhenReviewExists() {
        when(reviewRepository.existsByTutoringClass_ClassIdAndReviewType(
                CLASS_ID, ReviewType.CLIENT_TO_TUTOR))
                .thenReturn(true);

        assertTrue(contractService.hasClientReviewedClass(CLASS_ID));
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
