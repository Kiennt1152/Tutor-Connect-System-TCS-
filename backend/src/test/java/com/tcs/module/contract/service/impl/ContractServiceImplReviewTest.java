package com.tcs.module.contract.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReviewCriterionDto;
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
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit test module Contract — danh gia gia su va tinh lai diem uy tin.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet createReview va recomputeReputation.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceImplReviewTest {

    private static final Long CLIENT_USER_ID = 100L;
    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long CLASS_ID = 500L;
    private static final Long ASSIGNMENT_ID = 700L;

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
    @Mock private EmailOtpRepository emailOtpRepository;

    @InjectMocks private ContractServiceImpl service;

    private User clientUser;
    private User tutorUser;
    private Tutor tutor;
    private TutoringClass tutoringClass;
    private ClassAssignment assignment;

    @BeforeEach
    void setUp() {
        clientUser = new User();
        clientUser.setUserId(CLIENT_USER_ID);
        clientUser.setEmail("phuhuynh@tcs.vn");

        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("giasu@tcs.vn");

        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");
        tutor.setRatingAvg(BigDecimal.ZERO);

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setTitle("Toan 9");
        tutoringClass.setCreator(clientUser);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(600L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);

        assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID);
        assignment.setApplication(application);
        assignment.setTutor(tutor);

        when(authHelper.requireRole(UserRole.CLIENT))
                .thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setReviewId(900L);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());
    }

    /** So buoi da dien ra cua lop (buoi COMPLETED moi tinh). */
    private void givenOccurredLessons(int count) {
        java.util.List<Lesson> lessons = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Lesson lesson = new Lesson();
            lesson.setLessonId(1000L + i);
            lesson.setTutoringClass(tutoringClass);
            lesson.setSequenceNo(i);
            lesson.setLessonDate(LocalDate.now().minusDays(count - i));
            lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
            lessons.add(lesson);
        }
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(lessons);
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(any())).thenReturn(List.of());
    }

    /** So luot danh gia CLIENT_TO_TUTOR ma client da gui cho phan cong nay. */
    private void givenSubmittedReviews(int count) {
        java.util.List<Review> reviews = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Review r = new Review();
            r.setReviewId(800L + i);
            r.setAssignment(assignment);
            r.setTutoringClass(tutoringClass);
            r.setReviewer(clientUser);
            r.setReviewee(tutorUser);
            r.setReviewType(ReviewType.CLIENT_TO_TUTOR);
            r.setRating(BigDecimal.valueOf(5));
            r.setCreatedAt(LocalDateTime.now().minusDays(i));
            reviews.add(r);
        }
        when(reviewRepository.findByReviewer_UserId(CLIENT_USER_ID)).thenReturn(reviews);
    }

    private CreateReviewRequest reviewRequest(Integer... scores) {
        CreateReviewRequest r = new CreateReviewRequest();
        r.setAssignmentId(ASSIGNMENT_ID);
        r.setComment("Gia su day de hieu");
        if (scores.length > 0) {
            java.util.List<ReviewCriterionDto> criteria = new java.util.ArrayList<>();
            for (int i = 0; i < scores.length; i++) {
                ReviewCriterionDto c = new ReviewCriterionDto();
                c.setCode("C" + (i + 1));
                c.setQuestion("Tieu chi " + (i + 1));
                c.setScore(scores[i]);
                criteria.add(c);
            }
            r.setCriteria(criteria);
        }
        return r;
    }

    // ===================================================================
    //  Sheet: createReview
    // ===================================================================
    @Nested
    @DisplayName("createReview")
    class CreateReview {

        @Test
        @DisplayName("UTCID01 (N) - CLIENT cua lop, da co buoi hoc, criteria [5,4,5] -> luu Review, rating = 4.7")
        void utcid01_createReviewSuccessfully() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);

            service.createReview(reviewRequest(5, 4, 5));

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            Review saved = captor.getValue();
            assertEquals(ReviewType.CLIENT_TO_TUTOR, saved.getReviewType());
            assertEquals(TUTOR_USER_ID, saved.getReviewee().getUserId());
            assertEquals(new BigDecimal("4.7"), saved.getRating());
            verify(eventPublisher).publishEvent(any(Object.class));
            // Tinh lai diem uy tin gia su ngay sau khi luu danh gia.
            verify(reviewRepository).findByReviewee_UserIdAndReviewTypeAndStatus(
                    TUTOR_USER_ID, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE);
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong co vai tro CLIENT -> ForbiddenException")
        void utcid02_notAClient() {
            when(authHelper.requireRole(UserRole.CLIENT))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class, () -> service.createReview(reviewRequest(5)));
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - request = null hoac assignmentId = null -> 'Thiếu thông tin đánh giá'")
        void utcid03_missingPayload() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createReview(null));
            assertEquals("Thiếu thông tin đánh giá", ex.getMessage());

            CreateReviewRequest noAssignment = new CreateReviewRequest();
            noAssignment.setRating(5);
            assertThrows(IllegalArgumentException.class, () -> service.createReview(noAssignment));
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - assignmentId khong khop phan cong nao -> 'Không tìm thấy phân công lớp'")
        void utcid04_assignmentNotFound() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.createReview(reviewRequest(5)));
            assertEquals("Không tìm thấy phân công lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Phan cong ton tai nhung application (va lop) null -> BusinessException")
        void utcid05_assignmentWithoutClass() {
            assignment.setApplication(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createReview(reviewRequest(5)));
            assertEquals("Bạn chỉ có thể đánh giá lớp học của mình", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Nguoi goi khong phai chu lop cung khong phai hoc vien ghi danh -> BusinessException")
        void utcid06_notReviewerOfClass() {
            User another = new User();
            another.setUserId(999L);
            tutoringClass.setCreator(another);
            when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(
                    CLASS_ID, CLIENT_USER_ID)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createReview(reviewRequest(5)));
            assertEquals("Bạn chỉ có thể đánh giá lớp học của mình", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Chua co buoi hoc nao dien ra -> 'Chưa có buổi học nào diễn ra để đánh giá'")
        void utcid07_noLessonOccurred() {
            givenOccurredLessons(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createReview(reviewRequest(5)));
            assertEquals("Chưa có buổi học nào diễn ra để đánh giá", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - 2 buoi da hoc va da gui 1 danh gia -> van cho danh gia tiep")
        void utcid08_quotaStillAvailable() {
            givenOccurredLessons(2);
            givenSubmittedReviews(1);

            service.createReview(reviewRequest(5, 5, 5));

            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("UTCID09 (B) - 2 buoi da hoc va da gui 2 danh gia -> 'Bạn đã đánh giá đủ số lượt cho các buổi đã học'")
        void utcid09_quotaExhausted() {
            givenOccurredLessons(2);
            givenSubmittedReviews(2);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createReview(reviewRequest(5)));
            assertEquals("Bạn đã đánh giá đủ số lượt cho các buổi đã học", ex.getMessage());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID10 (B) - criteria o dung hai can 1 va 5 -> chap nhan, rating = 3.0")
        void utcid10_criteriaAtBoundaries() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);

            service.createReview(reviewRequest(1, 5));

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            assertEquals(new BigDecimal("3.0"), captor.getValue().getRating());
        }

        @Test
        @DisplayName("UTCID11 (B) - criteria co diem 0 hoac 6 (ngoai can) -> 'Mỗi tiêu chí phải được chấm từ 1 đến 5 sao'")
        void utcid11_criteriaOutOfRange() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);

            IllegalArgumentException below = assertThrows(IllegalArgumentException.class,
                    () -> service.createReview(reviewRequest(0)));
            assertEquals("Mỗi tiêu chí phải được chấm từ 1 đến 5 sao", below.getMessage());

            assertThrows(IllegalArgumentException.class, () -> service.createReview(reviewRequest(6)));
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID12 (A) - criteria co diem null -> 'Mỗi tiêu chí phải được chấm từ 1 đến 5 sao'")
        void utcid12_criteriaWithNullScore() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createReview(reviewRequest(new Integer[] {null})));
            assertEquals("Mỗi tiêu chí phải được chấm từ 1 đến 5 sao", ex.getMessage());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID13 (A) - khong co criteria va rating = null -> 'Vui lòng chấm điểm đánh giá'")
        void utcid13_noCriteriaAndNoRating() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createReview(reviewRequest()));
            assertEquals("Vui lòng chấm điểm đánh giá", ex.getMessage());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID14 (B) - khong criteria, rating = 1 va rating = 5 -> dung truc tiep rating")
        void utcid14_ratingWithoutCriteria() {
            givenOccurredLessons(5);
            givenSubmittedReviews(0);

            CreateReviewRequest low = reviewRequest();
            low.setRating(1);
            service.createReview(low);

            CreateReviewRequest high = reviewRequest();
            high.setRating(5);
            service.createReview(high);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository, org.mockito.Mockito.times(2)).save(captor.capture());
            assertEquals(new BigDecimal("1.0"), captor.getAllValues().get(0).getRating());
            assertEquals(new BigDecimal("5.0"), captor.getAllValues().get(1).getRating());
        }

        @Test
        @DisplayName("UTCID15 (N) - anonymous = true kem displayName -> giu lai displayName")
        void utcid15_anonymousKeepsDisplayName() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);
            CreateReviewRequest request = reviewRequest(5);
            request.setAnonymous(true);
            request.setDisplayName("Phu huynh A");

            service.createReview(request);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            assertEquals("Phu huynh A", captor.getValue().getDisplayName());
            org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().isAnonymous());
        }

        @Test
        @DisplayName("UTCID16 (N) - anonymous = false kem displayName -> displayName bi xoa ve null")
        void utcid16_nonAnonymousDropsDisplayName() {
            givenOccurredLessons(2);
            givenSubmittedReviews(0);
            CreateReviewRequest request = reviewRequest(5);
            request.setAnonymous(false);
            request.setDisplayName("Phu huynh A");

            service.createReview(request);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            assertNull(captor.getValue().getDisplayName(),
                    "Khong an danh thi khong duoc dung ten hien thi tu nhap");
            org.junit.jupiter.api.Assertions.assertFalse(captor.getValue().isAnonymous());
        }
    }

    // ===================================================================
    //  Sheet: recomputeReputation (recomputeReputationByTutorUser)
    // ===================================================================
    @Nested
    @DisplayName("recomputeReputation")
    class RecomputeReputation {

        private Review visibleReview(Long reviewId, Long classId, User reviewer, String rating, int daysAgo) {
            TutoringClass cls = new TutoringClass();
            cls.setClassId(classId);

            Review r = new Review();
            r.setReviewId(reviewId);
            r.setTutoringClass(cls);
            r.setReviewer(reviewer);
            r.setReviewee(tutorUser);
            r.setReviewType(ReviewType.CLIENT_TO_TUTOR);
            r.setStatus(ReviewStatus.VISIBLE);
            r.setRating(new BigDecimal(rating));
            r.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
            return r;
        }

        private void givenVisibleReviews(List<Review> reviews) {
            when(reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                    TUTOR_USER_ID, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE)).thenReturn(reviews);
        }

        @Test
        @DisplayName("UTCID01 (N) - Gia su co danh gia VISIBLE -> tinh lai diem trung binh va ghi lich su")
        void utcid01_recomputeAverage() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
            givenVisibleReviews(List.of(
                    visibleReview(1L, 501L, clientUser, "5.0", 3),
                    visibleReview(2L, 502L, clientUser, "4.0", 2)));

            service.recomputeReputationByTutorUser(TUTOR_USER_ID);

            assertEquals(new BigDecimal("4.50"), tutor.getRatingAvg());
            verify(tutorRepository).save(tutor);
            verify(reputationHistoryRepository).save(any());
        }

        @Test
        @DisplayName("UTCID02 (B) - userId khong co ho so gia su -> khong ghi gi ca")
        void utcid02_noTutorProfile() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

            service.recomputeReputationByTutorUser(TUTOR_USER_ID);

            verify(tutorRepository, never()).save(any());
            verify(reputationHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (B) - Gia su khong co danh gia VISIBLE nao -> diem trung binh ve 0")
        void utcid03_noVisibleReview() {
            tutor.setRatingAvg(new BigDecimal("4.50"));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
            givenVisibleReviews(List.of());

            service.recomputeReputationByTutorUser(TUTOR_USER_ID);

            assertEquals(new BigDecimal("0.00"), tutor.getRatingAvg());
            verify(tutorRepository).save(tutor);
        }

        @Test
        @DisplayName("UTCID04 (B) - Mot lop co nhieu danh gia cua cung khach -> chi tinh danh gia cuoi cua lop do")
        void utcid04_onlyFinalReviewPerClass() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
            givenVisibleReviews(List.of(
                    visibleReview(1L, 501L, clientUser, "1.0", 5),
                    visibleReview(2L, 501L, clientUser, "1.0", 4),
                    visibleReview(3L, 501L, clientUser, "5.0", 1),   // ban cuoi cua lop 501
                    visibleReview(4L, 502L, clientUser, "3.0", 2)));

            service.recomputeReputationByTutorUser(TUTOR_USER_ID);

            // Chi con 5.0 (lop 501) va 3.0 (lop 502) -> trung binh 4.00.
            assertEquals(new BigDecimal("4.00"), tutor.getRatingAvg(),
                    "Mot lop danh gia nhieu lan khong duoc lan at diem trung binh");
        }

        @Test
        @DisplayName("UTCID05 (A) - tutorUserId = null -> khong tim thay gia su, khong nem loi")
        void utcid05_nullTutorUserId() {
            when(tutorRepository.findByUser_UserId(null)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> service.recomputeReputationByTutorUser(null));
            verify(tutorRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: generateForAssignment (hop dong lop gia su rieng - BF-05)
    // ===================================================================
    @Nested
    @DisplayName("generateForAssignment")
    class GenerateForAssignment {

        @BeforeEach
        void makeClassPrivate() {
            tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.PRIVATE);
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());
            when(contractRepository.save(any(com.tcs.module.contract.entity.Contract.class)))
                    .thenAnswer(i -> {
                        com.tcs.module.contract.entity.Contract c = i.getArgument(0);
                        c.setContractId(1100L);
                        return c;
                    });
            when(contractRepository.countTodayContracts()).thenReturn(0L);
            when(tutorCenterRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());
        }

        private com.tcs.module.contract.entity.ContractTemplate activeTemplate() {
            com.tcs.module.contract.entity.ContractTemplate t =
                    new com.tcs.module.contract.entity.ContractTemplate();
            t.setTemplateId(10L);
            t.setName("Mẫu hợp đồng lớp gia sư riêng");
            t.setContent("Điều 1. Nội dung mẫu đang hiệu lực.");
            t.setStatus(com.tcs.module.contract.enums.ContractTemplateStatus.ACTIVE);
            return t;
        }

        @Test
        @DisplayName("UTCID01 (N) - Phan cong hop le, chua co hop dong -> tao Contract PENDING nguon PRIVATE")
        void utcid01_generateSuccessfully() {
            when(contractTemplateRepository.findAll()).thenReturn(List.of(activeTemplate()));
            when(systemParameterRepository.findByParamKey("tpltype:10")).thenReturn(Optional.empty());

            com.tcs.module.contract.entity.Contract contract = service.generateForAssignment(ASSIGNMENT_ID);

            assertEquals(com.tcs.module.contract.enums.ContractStatus.PENDING, contract.getStatus());
            assertEquals(com.tcs.module.contract.enums.ContractSourceType.PRIVATE, contract.getSourceType());
            org.junit.jupiter.api.Assertions.assertNotNull(contract.getContractNo());
            org.junit.jupiter.api.Assertions.assertNotNull(contract.getExpiresAt());
            assertEquals("Điều 1. Nội dung mẫu đang hiệu lực.", contract.getTermsSummary());
            // O ky cho ca gia su va nguoi tao lop.
            verify(contractSignatureRepository, org.mockito.Mockito.times(2))
                    .save(any(com.tcs.module.contract.entity.ContractSignature.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - assignmentId khong khop phan cong nao -> 'Không tìm thấy phân công lớp'")
        void utcid02_assignmentNotFound() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.generateForAssignment(ASSIGNMENT_ID));
            assertEquals("Không tìm thấy phân công lớp", ex.getMessage());
            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Phan cong da co hop dong -> 'Hợp đồng đã tồn tại cho phân công này'")
        void utcid03_contractAlreadyExists() {
            when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                    .thenReturn(Optional.of(new com.tcs.module.contract.entity.Contract()));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.generateForAssignment(ASSIGNMENT_ID));
            assertEquals("Hợp đồng đã tồn tại cho phân công này", ex.getMessage());
            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - Nguoi goi khong phai gia su cung khong phai chu lop -> ForbiddenException")
        void utcid04_noPermission() {
            when(authHelper.currentUserId()).thenReturn(999L);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.generateForAssignment(ASSIGNMENT_ID));
            assertEquals("Bạn không có quyền tạo hợp đồng cho phân công này", ex.getMessage());
            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (B) - Khong co mau hop dong ACTIVE -> termsSummary lay tu ban tom tat tu dung")
        void utcid05_noActiveTemplate() {
            when(contractTemplateRepository.findAll()).thenReturn(List.of());

            com.tcs.module.contract.entity.Contract contract = service.generateForAssignment(ASSIGNMENT_ID);

            assertNull(contract.getTemplate(), "Khong co mau ACTIVE thi khong gan template");
            org.junit.jupiter.api.Assertions.assertNotNull(contract.getTermsSummary(),
                    "Van phai co dieu khoan tom tat de hai ben ky");
            assertEquals(com.tcs.module.contract.enums.ContractStatus.PENDING, contract.getStatus());
        }
    }

    // ===================================================================
    //  Sheet: generateForEnrollment (hop dong hoc vien lop trung tam)
    // ===================================================================
    @Nested
    @DisplayName("generateForEnrollment")
    class GenerateForEnrollment {

        private static final Long CLASS_STUDENT_ID = 800L;

        private com.tcs.module.marketplace.entity.ClassStudent enrollment;

        @BeforeEach
        void initEnrollment() {
            enrollment = new com.tcs.module.marketplace.entity.ClassStudent();
            enrollment.setClassStudentId(CLASS_STUDENT_ID);
            enrollment.setTutoringClass(tutoringClass);
            enrollment.setEnrolledByUser(clientUser);
            enrollment.setStudentName("Nguyen Van A");

            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.of(enrollment));
            when(contractRepository.findByClassStudent_ClassStudentId(CLASS_STUDENT_ID))
                    .thenReturn(Optional.empty());
            when(contractRepository.save(any(com.tcs.module.contract.entity.Contract.class)))
                    .thenAnswer(i -> {
                        com.tcs.module.contract.entity.Contract c = i.getArgument(0);
                        c.setContractId(1200L);
                        return c;
                    });
            when(contractRepository.countTodayContracts()).thenReturn(0L);
            when(tutorCenterRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());
        }

        private com.tcs.module.contract.entity.ContractTemplate activeTemplate() {
            com.tcs.module.contract.entity.ContractTemplate t =
                    new com.tcs.module.contract.entity.ContractTemplate();
            t.setTemplateId(11L);
            t.setName("Mẫu hợp đồng học viên");
            t.setContent("Điều 1. Nội dung mẫu học viên đang hiệu lực.");
            t.setStatus(com.tcs.module.contract.enums.ContractTemplateStatus.ACTIVE);
            return t;
        }

        @Test
        @DisplayName("UTCID01 (N) - Ghi danh hop le, chua co hop dong -> tao Contract PENDING nguon CENTER")
        void utcid01_generateSuccessfully() {
            when(contractTemplateRepository.findAll()).thenReturn(List.of(activeTemplate()));
            when(systemParameterRepository.findByParamKey("tpltype:11")).thenReturn(Optional.empty());

            com.tcs.module.contract.entity.Contract contract = service.generateForEnrollment(CLASS_STUDENT_ID);

            assertEquals(com.tcs.module.contract.enums.ContractStatus.PENDING, contract.getStatus());
            assertEquals(com.tcs.module.contract.enums.ContractSourceType.CENTER, contract.getSourceType());
            org.junit.jupiter.api.Assertions.assertNotNull(contract.getContractNo());
            org.junit.jupiter.api.Assertions.assertNotNull(contract.getExpiresAt());
            assertEquals("Điều 1. Nội dung mẫu học viên đang hiệu lực.", contract.getTermsSummary());
            verify(contractSignatureRepository, org.mockito.Mockito.atLeastOnce())
                    .save(any(com.tcs.module.contract.entity.ContractSignature.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - classStudentId khong khop ghi danh nao -> 'Không tìm thấy ghi danh'")
        void utcid02_enrollmentNotFound() {
            when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.generateForEnrollment(CLASS_STUDENT_ID));
            assertEquals("Không tìm thấy ghi danh", ex.getMessage());
            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Ghi danh da co hop dong -> 'Hợp đồng đã tồn tại cho ghi danh này'")
        void utcid03_contractAlreadyExists() {
            when(contractRepository.findByClassStudent_ClassStudentId(CLASS_STUDENT_ID))
                    .thenReturn(Optional.of(new com.tcs.module.contract.entity.Contract()));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.generateForEnrollment(CLASS_STUDENT_ID));
            assertEquals("Hợp đồng đã tồn tại cho ghi danh này", ex.getMessage());
            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - Nguoi goi khong phai trung tam cung khong phai nguoi ghi danh -> ForbiddenException")
        void utcid04_noPermission() {
            when(authHelper.currentUserId()).thenReturn(999L);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.generateForEnrollment(CLASS_STUDENT_ID));
            assertEquals("Bạn không có quyền tạo hợp đồng cho ghi danh này", ex.getMessage());
            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (B) - Khong co mau hop dong ACTIVE -> termsSummary lay tu ban tom tat lop trung tam")
        void utcid05_noActiveTemplate() {
            when(contractTemplateRepository.findAll()).thenReturn(List.of());

            com.tcs.module.contract.entity.Contract contract = service.generateForEnrollment(CLASS_STUDENT_ID);

            assertNull(contract.getTemplate(), "Khong co mau ACTIVE thi khong gan template");
            org.junit.jupiter.api.Assertions.assertNotNull(contract.getTermsSummary(),
                    "Van phai co dieu khoan tom tat de hai ben ky");
            assertEquals(com.tcs.module.contract.enums.ContractSourceType.CENTER, contract.getSourceType());
        }
    }
}
