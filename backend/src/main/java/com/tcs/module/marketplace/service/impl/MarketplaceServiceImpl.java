package com.tcs.module.marketplace.service.impl;

import com.tcs.exception.BusinessException;
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
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final FavoriteTutorRepository favoriteTutorRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocationRepository locationRepository;

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
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setProposedRate(request.getProposedRate());
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(TutorApplicationStatus.SUBMITTED);
        tutorApplicationRepository.save(application);
    }

    @Override
    @Transactional
    public ClassTerminationResponse requestClassTermination(Long classId, CreateClassTerminationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin yêu cầu chấm dứt lớp");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("Lý do chấm dứt lớp là bắt buộc");
        }
        if (request.getEffectiveDate() != null && request.getEffectiveDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày hiệu lực không được nằm trong quá khứ");
        }

        User requester = requireUser();
        TutoringClass tutoringClass = findClass(classId);
        if (tutoringClass.getStatus() != TutoringClassStatus.IN_PROGRESS) {
            throw new BusinessException("Chỉ lớp đang diễn ra mới có thể yêu cầu chấm dứt sớm");
        }

        ClassAssignment assignment = resolveActiveAssignment(tutoringClass, request.getAssignmentId());
        requireTerminationParticipant(tutoringClass, assignment, requester.getUserId());

        if (classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                assignment.getAssignmentId(), ClassTerminationStatus.PENDING)) {
            throw new BusinessException("Lớp học đã có yêu cầu chấm dứt sớm đang chờ xử lý");
        }

        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setAssignment(assignment);
        termination.setRequestedBy(requester);
        termination.setReason(request.getReason().trim());
        termination.setEffectiveDate(request.getEffectiveDate());
        termination.setStatus(ClassTerminationStatus.PENDING);

        tutoringClass.setStatus(TutoringClassStatus.DISPUTED);
        tutoringClassRepository.save(tutoringClass);

        return toTerminationResponse(classTerminationRequestRepository.save(termination), tutoringClass);
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

    private TutoringClass findClass(Long classId) {
        return tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
    }

    private ClassAssignment resolveActiveAssignment(TutoringClass tutoringClass, Long assignmentId) {
        if (assignmentId != null) {
            ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
            if (assignment.getStatus() != ClassAssignmentStatus.ACTIVE) {
                throw new BusinessException("Phân công lớp không còn hoạt động");
            }
            if (!assignmentBelongsToClass(assignment, tutoringClass.getClassId())) {
                throw new IllegalArgumentException("Phân công lớp không thuộc lớp học này");
            }
            return assignment;
        }

        List<ClassAssignment> activeAssignments =
                classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        tutoringClass.getClassId(), ClassAssignmentStatus.ACTIVE);
        if (activeAssignments.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy phân công đang hoạt động của lớp học");
        }
        if (activeAssignments.size() > 1) {
            throw new BusinessException("Lớp học có nhiều phân công, vui lòng truyền assignmentId cụ thể");
        }
        return activeAssignments.get(0);
    }

    private boolean assignmentBelongsToClass(ClassAssignment assignment, Long classId) {
        return assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null
                && Objects.equals(assignment.getApplication().getTutoringClass().getClassId(), classId);
    }

    private void requireTerminationParticipant(
            TutoringClass tutoringClass,
            ClassAssignment assignment,
            Long requesterUserId) {
        boolean isClassCreator = tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), requesterUserId);
        boolean isAssignedTutor = assignment.getTutor() != null
                && assignment.getTutor().getUser() != null
                && Objects.equals(assignment.getTutor().getUser().getUserId(), requesterUserId);
        if (!isClassCreator && !isAssignedTutor) {
            throw new ForbiddenException("Bạn không có quyền yêu cầu chấm dứt lớp này");
        }
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

    private ClassTerminationResponse toTerminationResponse(
            ClassTerminationRequest request,
            TutoringClass tutoringClass) {
        return ClassTerminationResponse.builder()
                .terminationId(request.getTerminationId())
                .classId(tutoringClass.getClassId())
                .assignmentId(request.getAssignment().getAssignmentId())
                .requestedByUserId(request.getRequestedBy().getUserId())
                .reason(request.getReason())
                .effectiveDate(request.getEffectiveDate())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
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
