package com.tcs.module.marketplace.service.impl;

import com.tcs.exception.ConflictException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.TutorApplicationReviewRequest;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.TutorApplicationResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ApplicationStatusHistory;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.mapper.MarketplaceMapper;
import com.tcs.module.marketplace.repository.ApplicationStatusHistoryRepository;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.MarketplaceService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private static final Set<TutorApplicationStatus> REVIEWABLE_STATUSES =
            Set.of(TutorApplicationStatus.SUBMITTED, TutorApplicationStatus.UNDER_REVIEW);

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final FavoriteTutorRepository favoriteTutorRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocationRepository locationRepository;
    private final MarketplaceMapper marketplaceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(TutoringClassStatus status) {
        List<TutoringClass> classes =
                status != null ? tutoringClassRepository.findByStatus(status) : tutoringClassRepository.findAll();
        return classes.stream().map(this::toClassResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClass(Long classId) {
        return toClassResponse(findClass(classId));
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        User creator = requireUser();
        requireClient(creator.getUserId());
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Tiêu đề và mô tả là bắt buộc");
        }
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle(request.getTitle());
        tutoringClass.setDescription(request.getDescription());
        tutoringClass.setCategory(resolveCategory(request.getCategoryId()));
        tutoringClass.setSubject(resolveSubject(request.getSubjectId()));
        tutoringClass.setGrade(resolveGrade(request.getGradeId()));
        tutoringClass.setLocation(resolveLocation(request.getLocationId()));
        if (request.getLessonMode() != null) tutoringClass.setLessonMode(request.getLessonMode());
        if (request.getNumberOfSessions() != null) tutoringClass.setNumberOfSessions(request.getNumberOfSessions());
        if (request.getStartDate() != null) tutoringClass.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) tutoringClass.setEndDate(request.getEndDate());
        if (request.getTuitionFee() != null) tutoringClass.setTuitionFee(request.getTuitionFee());
        tutoringClass.setBudget(request.getBudget() != null ? request.getBudget() : BigDecimal.ZERO);
        if (request.getRecurringType() != null) tutoringClass.setRecurringType(request.getRecurringType());
        tutoringClass.setStatus(TutoringClassStatus.DRAFT);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public ClassResponse publishClass(Long classId) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền đăng lớp này");
        }
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public void applyToClass(Long classId, ApplyClassRequest request) {
        Tutor tutor = requireTutor();
        TutoringClass tutoringClass = findClass(classId);
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp không mở đơn ứng tuyển");
        }
        if (tutorApplicationRepository.existsByTutoringClass_ClassIdAndTutor_TutorIdAndStatusIn(
                classId, tutor.getTutorId(),
                Set.of(
                        TutorApplicationStatus.SUBMITTED,
                        TutorApplicationStatus.UNDER_REVIEW,
                        TutorApplicationStatus.ACCEPTED))) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển đang xử lý cho lớp này");
        }
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setProposedRate(request.getProposedRate());
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(TutorApplicationStatus.SUBMITTED);
        TutorApplication saved = tutorApplicationRepository.save(application);
        recordStatusHistory(saved, null, TutorApplicationStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorSearchResponse> searchTutors(String keyword, Long subjectId) {
        String q = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        return tutorRepository.findAll().stream()
                .filter(t -> !StringUtils.hasText(q)
                        || t.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || (t.getBio() != null && t.getBio().toLowerCase(Locale.ROOT).contains(q)))
                .map(this::toTutorSearch)
                .toList();
    }

    @Override
    @Transactional
    public void addFavorite(Long tutorId) {
        authHelper.requireRole(UserRole.CLIENT);
        User user = requireUser();
        Tutor tutor = tutorRepository
                .findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));
        if (favoriteTutorRepository.existsByUser_UserIdAndTutor_TutorId(user.getUserId(), tutorId)) {
            return;
        }
        FavoriteTutor favorite = new FavoriteTutor();
        favorite.setUser(user);
        favorite.setTutor(tutor);
        favoriteTutorRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long tutorId) {
        authHelper.requireRole(UserRole.CLIENT);
        favoriteTutorRepository
                .findByUser_UserIdAndTutor_TutorId(authHelper.currentUserId(), tutorId)
                .ifPresent(favoriteTutorRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorSearchResponse> getFavorites() {
        authHelper.requireRole(UserRole.CLIENT);
        return favoriteTutorRepository.findByUser_UserId(authHelper.currentUserId()).stream()
                .map(f -> toTutorSearch(f.getTutor()))
                .toList();
    }

    // ---------------- Tutoring Request Management ----------------

    @Override
    @Transactional(readOnly = true)
    public List<TutorApplicationResponse> listApplicationsByClass(Long classId) {
        authHelper.requireRole(UserRole.CLIENT);
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Chỉ chủ lớp mới xem được danh sách ứng tuyển");
        }
        return marketplaceMapper.toResponseList(
                tutorApplicationRepository.findByTutoringClass_ClassIdOrderByAppliedAtDesc(classId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorApplicationResponse> listMyApplications() {
        Tutor tutor = requireTutor();
        return marketplaceMapper.toResponseList(
                tutorApplicationRepository.findByTutor_TutorIdOrderByAppliedAtDesc(tutor.getTutorId()));
    }

    @Override
    @Transactional(readOnly = true)
    public TutorApplicationResponse getApplication(Long applicationId) {
        TutorApplication application = findApplicationVisibleToCurrentUser(applicationId);
        return marketplaceMapper.toResponse(application);
    }

    @Override
    @Transactional
    public void withdrawApplication(Long applicationId) {
        Tutor tutor = requireTutor();
        TutorApplication application = tutorApplicationRepository
                .findByApplicationIdAndTutor_User_UserId(applicationId, authHelper.currentUserId())
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền rút đơn này"));
        if (application.getStatus() != TutorApplicationStatus.SUBMITTED) {
            throw new ConflictException(
                    "Chỉ có thể rút đơn đang ở trạng thái SUBMITTED. Trạng thái hiện tại: "
                            + application.getStatus());
        }
        TutorApplicationStatus oldStatus = application.getStatus();
        application.setStatus(TutorApplicationStatus.WITHDRAWN);
        application.setReviewedAt(LocalDateTime.now());
        tutorApplicationRepository.save(application);
        recordStatusHistory(application, oldStatus, TutorApplicationStatus.WITHDRAWN);
    }

    @Override
    @Transactional
    public TutorApplicationResponse reviewApplication(Long applicationId, TutorApplicationReviewRequest request) {
        authHelper.requireRole(UserRole.CLIENT);
        if (request == null
                || (request.getDecision() != TutorApplicationStatus.ACCEPTED
                        && request.getDecision() != TutorApplicationStatus.REJECTED)) {
            throw new IllegalArgumentException("Decision phải là ACCEPTED hoặc REJECTED");
        }
        TutorApplication application = tutorApplicationRepository
                .findByApplicationIdAndTutoringClass_Creator_UserId(applicationId, authHelper.currentUserId())
                .orElseThrow(() -> new ForbiddenException("Chỉ chủ lớp mới duyệt được đơn này"));
        TutoringClass tutoringClass = application.getTutoringClass();
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new ConflictException(
                    "Lớp đã đóng đơn (trạng thái: " + tutoringClass.getStatus() + ")");
        }
        if (!REVIEWABLE_STATUSES.contains(application.getStatus())) {
            throw new ConflictException(
                    "Đơn không ở trạng thái có thể duyệt. Trạng thái hiện tại: " + application.getStatus());
        }
        if (request.getDecision() == TutorApplicationStatus.ACCEPTED
                && classAssignmentRepository.existsByApplication_ApplicationId(applicationId)) {
            throw new ConflictException("Đơn này đã có phân công lớp");
        }

        TutorApplicationStatus oldStatus = application.getStatus();
        User actor = requireUser();
        LocalDateTime now = LocalDateTime.now();

        if (request.getDecision() == TutorApplicationStatus.ACCEPTED) {
            application.setStatus(TutorApplicationStatus.ACCEPTED);
            application.setReviewedAt(now);
            tutorApplicationRepository.save(application);

            ClassAssignment assignment = new ClassAssignment();
            assignment.setApplication(application);
            assignment.setStatus(ClassAssignmentStatus.ACTIVE);
            classAssignmentRepository.save(assignment);

            tutoringClass.setStatus(TutoringClassStatus.MATCHED);
            tutoringClassRepository.save(tutoringClass);

            recordStatusHistory(application, oldStatus, TutorApplicationStatus.ACCEPTED);

            // Auto-reject tất cả các đơn còn lại của cùng lớp
            List<TutorApplication> others = tutorApplicationRepository
                    .findByTutoringClass_ClassIdAndApplicationIdNotAndStatusIn(
                            tutoringClass.getClassId(),
                            application.getApplicationId(),
                            REVIEWABLE_STATUSES);
            for (TutorApplication other : others) {
                TutorApplicationStatus prev = other.getStatus();
                other.setStatus(TutorApplicationStatus.REJECTED);
                other.setReviewedAt(now);
                tutorApplicationRepository.save(other);
                recordStatusHistory(other, prev, TutorApplicationStatus.REJECTED);
            }
        } else {
            application.setStatus(TutorApplicationStatus.REJECTED);
            application.setReviewedAt(now);
            tutorApplicationRepository.save(application);
            recordStatusHistory(application, oldStatus, TutorApplicationStatus.REJECTED);
        }

        return marketplaceMapper.toResponse(application);
    }

    // ---------------- helpers ----------------

    private TutoringClass findClass(Long classId) {
        return tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
    }

    private TutorApplication findApplicationVisibleToCurrentUser(Long applicationId) {
        Long currentUserId = authHelper.currentUserId();
        return tutorApplicationRepository
                .findByApplicationIdAndTutoringClass_Creator_UserId(applicationId, currentUserId)
                .or(() -> tutorApplicationRepository.findByApplicationIdAndTutor_User_UserId(applicationId, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
    }

    private void recordStatusHistory(TutorApplication application, TutorApplicationStatus oldStatus,
                                     TutorApplicationStatus newStatus) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(requireUser());
        applicationStatusHistoryRepository.save(history);
    }

    private User requireUser() {
        return userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private void requireClient(Long userId) {
        if (clientRepository.findByUser_UserId(userId).isEmpty()) {
            throw new ForbiddenException("Chỉ phụ huynh/khách hàng mới tạo lớp học");
        }
    }

    private Tutor requireTutor() {
        authHelper.requireRole(UserRole.TUTOR);
        return tutorRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
    }

    private Category resolveCategory(Long id) {
        if (id == null) return null;
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }

    private Subject resolveSubject(Long id) {
        if (id == null) return null;
        return subjectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
    }

    private Grade resolveGrade(Long id) {
        if (id == null) return null;
        return gradeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối/lớp"));
    }

    private Location resolveLocation(Long id) {
        if (id == null) return null;
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm"));
    }

    private ClassResponse toClassResponse(TutoringClass c) {
        Client client = clientRepository.findByUser_UserId(c.getCreator().getUserId()).orElse(null);
        return ClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .description(c.getDescription())
                .creatorId(c.getCreator().getUserId())
                .creatorName(client != null ? client.getFullName() : c.getCreator().getEmail())
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeId(c.getGrade() != null ? c.getGrade().getGradeId() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .lessonMode(c.getLessonMode())
                .numberOfSessions(c.getNumberOfSessions())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .tuitionFee(c.getTuitionFee())
                .budget(c.getBudget())
                .recurringType(c.getRecurringType())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private TutorSearchResponse toTutorSearch(Tutor tutor) {
        return TutorSearchResponse.builder()
                .tutorId(tutor.getTutorId())
                .userId(tutor.getUser().getUserId())
                .fullName(tutor.getFullName())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(tutor.getVerificationStatus().name())
                .build();
    }
}