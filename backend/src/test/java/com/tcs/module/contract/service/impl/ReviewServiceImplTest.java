package com.tcs.module.contract.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewType;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.profile.entity.Tutor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test cho ReviewServiceImpl.createReview - sheet createReview.
 *
 * <p>Luu y: cung mot nghiep vu "danh gia gia su" duoc hien thuc HAI lan trong codebase.
 * ContractServiceImpl.createClassReview kiem tra day du (dung phan cong, da co buoi hoc,
 * chua vuot so luot, tinh lai diem uy tin). Con ReviewServiceImpl.createReview - duong di
 * dang duoc tai lieu hoa o sheet nay - khong kiem tra gi ngoai rang buoc rating 1..5.
 * Cac UTCID05/06/07 duoi day viet theo DAC TA va hien dang FAIL.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceImplTest {

    private static final Long REVIEWER_ID = 10L;   // client thuc su hoc lop nay
    private static final Long TUTOR_USER_ID = 20L; // gia su duoc danh gia
    private static final Long STRANGER_ID = 99L;   // nguoi ngoai, khong lien quan lop
    private static final Long ASSIGNMENT_ID = 7L;

    @Mock private ReviewRepository reviewRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.tcs.security.AuthHelper authHelper;

    @InjectMocks private ReviewServiceImpl service;

    private User reviewer;
    private User tutorUser;
    private ClassAssignment assignment;

    @BeforeEach
    void setUp() {
        reviewer = user(REVIEWER_ID);
        tutorUser = user(TUTOR_USER_ID);

        Tutor tutor = new Tutor();
        tutor.setTutorId(1L);
        tutor.setUser(tutorUser);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(100L);
        tutoringClass.setCreator(reviewer);

        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);

        assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID);
        assignment.setTutor(tutor);
        assignment.setApplication(application);

        when(authHelper.currentUserId()).thenReturn(REVIEWER_ID);
        when(userRepository.findById(REVIEWER_ID)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(userRepository.findById(STRANGER_ID)).thenReturn(Optional.of(user(STRANGER_ID)));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setReviewId(500L);
            return r;
        });
    }

    private User user(Long id) {
        User u = new User();
        u.setUserId(id);
        u.setEmail("user" + id + "@tcs.vn");
        return u;
    }

    private CreateReviewRequest req(Long assignmentId, Long revieweeId, Integer rating) {
        CreateReviewRequest r = new CreateReviewRequest();
        r.setAssignmentId(assignmentId);
        r.setRevieweeId(revieweeId);
        r.setRating(rating);
        r.setReviewType(ReviewType.CLIENT_TO_TUTOR);
        r.setComment("Gia su day de hieu");
        return r;
    }

    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID01 (N)). */
    @Test
    @DisplayName("Ca 01 - Client trong lớp đánh giá gia sư, rating 4 -> tạo Review")
    void utcid01_createSuccessfully() {
        var result = service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 4));

        assertEquals(REVIEWER_ID, result.getReviewerId());
        assertEquals(TUTOR_USER_ID, result.getRevieweeId());
        assertEquals(0, result.getRating().compareTo(java.math.BigDecimal.valueOf(4)));
        verify(reviewRepository).save(any(Review.class));
    }

    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID02 (A)). */
    @Test
    @DisplayName("Ca 02 - Thiếu assignmentId/revieweeId/rating -> chặn")
    void utcid02_missingRequiredFields() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createReview(req(null, TUTOR_USER_ID, 4)));
        assertEquals("assignmentId, revieweeId và rating là bắt buộc", ex.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, null, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, null)));
        verify(reviewRepository, never()).save(any());
    }

    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID03 (B)). */
    @Test
    @DisplayName("Ca 03 - rating = 1 và rating = 5 (hai cận) -> hợp lệ")
    void utcid03_ratingBoundsAccepted() {
        service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 1));
        service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 5));

        verify(reviewRepository, org.mockito.Mockito.times(2)).save(any(Review.class));
    }

    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID04 (B)). */
    @Test
    @DisplayName("Ca 04 - rating = 0 và rating = 6 (ngoài cận) -> chặn")
    void utcid04_ratingOutOfBounds() {
        IllegalArgumentException low = assertThrows(IllegalArgumentException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 0)));
        assertEquals("Rating phải từ 1 đến 5", low.getMessage());

        IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 6)));
        assertEquals("Rating phải từ 1 đến 5", high.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    /**
     * UTCID05 (A) - DEF-03.
     * Đặc tả: chỉ người thực sự tham gia lớp (client thuê lớp hoặc gia sư được phân công)
     * mới được đánh giá phân công đó.
     * Thực tế: service không đối chiếu người đăng nhập với assignment -> người lạ vẫn ghi được.
     */
    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID05 (A)). */
    @Test
    @DisplayName("Ca 05 - Người ngoài lớp đánh giá -> phải bị từ chối [DEF-03]")
    void utcid05_strangerCannotReview() {
        when(authHelper.currentUserId()).thenReturn(STRANGER_ID);

        assertThrows(RuntimeException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 5)),
                "Người không tham gia phân công lớp phải bị từ chối, "
                        + "nhưng service vẫn tạo đánh giá thành công");
    }

    /**
     * UTCID06 (A) - DEF-04.
     * Đặc tả: mỗi người chỉ đánh giá một lần cho một phân công lớp.
     * Thực tế: không truy vấn kiểm tra trùng -> gửi bao nhiêu lần cũng được.
     */
    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID06 (A)). */
    @Test
    @DisplayName("Ca 06 - Đánh giá lần 2 cho cùng phân công -> phải bị từ chối [DEF-04]")
    void utcid06_duplicateReviewRejected() {
        Review existing = new Review();
        existing.setReviewId(1L);
        existing.setReviewer(reviewer);
        existing.setAssignment(assignment);
        when(reviewRepository.findByReviewer_UserId(REVIEWER_ID)).thenReturn(List.of(existing));

        assertThrows(RuntimeException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 3)),
                "Đã có đánh giá cho phân công này, lần gửi thứ 2 phải bị chặn");
    }

    /**
     * UTCID07 (A) - DEF-05.
     * Đặc tả: không ai được tự đánh giá chính mình.
     * Thực tế: không so sánh reviewer với reviewee -> tự cộng sao cho bản thân được.
     */
    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID07 (A)). */
    @Test
    @DisplayName("Ca 07 - Tự đánh giá chính mình -> phải bị từ chối [DEF-05]")
    void utcid07_selfReviewRejected() {
        assertThrows(RuntimeException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, REVIEWER_ID, 5)),
                "reviewer trùng reviewee phải bị chặn, nhưng service vẫn lưu đánh giá");
    }

    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID08 (A)). */
    @Test
    @DisplayName("Ca 08 - Phân công lớp không tồn tại -> ResourceNotFoundException")
    void utcid08_assignmentNotFound() {
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 4)));
        assertEquals("Không tìm thấy phân công lớp", ex.getMessage());
    }

    /** Ngoài phạm vi Report 5.1: MethodList trỏ sheet createReview tới ContractService.createReview; đây là ReviewServiceImpl - hiện thực song song (ca tương ứng: UTCID09 (A)). */
    @Test
    @DisplayName("Ca 09 - Người được đánh giá không tồn tại -> ResourceNotFoundException")
    void utcid09_revieweeNotFound() {
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.createReview(req(ASSIGNMENT_ID, TUTOR_USER_ID, 4)));
        assertEquals("Không tìm thấy người được đánh giá", ex.getMessage());
    }
}
