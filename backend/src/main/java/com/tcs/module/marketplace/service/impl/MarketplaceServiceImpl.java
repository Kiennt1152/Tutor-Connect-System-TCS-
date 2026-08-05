package com.tcs.module.marketplace.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.exception.VerificationRequiredException;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
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
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.dto.response.ScheduleSlotResponse;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.MarketplaceService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.response.CenterSummaryResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.TutorCenterRepository;
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
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final FavoriteTutorRepository favoriteTutorRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocationRepository locationRepository;
    private final AuditLogService auditLogService;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClassRequestStore classRequestStore;

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
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        auditLogService.record(creator.getUserId(), "CREATE_CLASS", "TutoringClass", saved.getClassId(), null, request);
        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse publishClass(Long classId) {
        TutoringClass tutoringClass = findClass(classId);
        Long userId = authHelper.currentUserId();
        if (!tutoringClass.getCreator().getUserId().equals(userId)) {
            throw new ForbiddenException("Không có quyền đăng lớp này");
        }
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        auditLogService.record(userId, "PUBLISH_CLASS", "TutoringClass", saved.getClassId(), null, null);
        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public void applyToClass(Long classId, ApplyClassRequest request) {
        Tutor tutor = requireTutor();
        // Chặn cứng: chỉ gia sư đã được xác minh mới được ứng tuyển vào lớp.
        if (tutor.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
            throw new VerificationRequiredException(
                    "Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp.");
        }
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
        TutorApplication saved = tutorApplicationRepository.save(application);
        auditLogService.record(tutor.getUser().getUserId(), "APPLY_CLASS", "TutorApplication",
                saved.getApplicationId(), null, request);
    }

    @Override
    @Transactional
    public void registerToClass(Long classId) {
        User user = requireUser();
        TutoringClass tutoringClass = findClass(classId);
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp chưa mở đăng ký");
        }
        Long userId = user.getUserId();

        // Gia sư -> nộp đơn dạy.
        Tutor tutor = tutorRepository.findByUser_UserId(userId).orElse(null);
        if (tutor != null) {
            // Chặn cứng: chỉ gia sư đã được xác minh mới được ứng tuyển vào lớp.
            if (tutor.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
                throw new VerificationRequiredException(
                        "Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp.");
            }
            if (tutorApplicationRepository
                    .existsByTutoringClass_ClassIdAndTutor_TutorId(classId, tutor.getTutorId())) {
                throw new IllegalArgumentException("Bạn đã đăng ký lớp này rồi");
            }
            TutorApplication application = new TutorApplication();
            application.setTutoringClass(tutoringClass);
            application.setTutor(tutor);
            application.setStatus(TutorApplicationStatus.SUBMITTED);
            TutorApplication saved = tutorApplicationRepository.save(application);
            auditLogService.record(userId, "APPLY_CLASS", "TutorApplication", saved.getApplicationId(), null,
                    java.util.Map.of("classId", classId));
            return;
        }

        // Phụ huynh/học viên -> ghi danh.
        Client client = clientRepository.findByUser_UserId(userId).orElse(null);
        if (client != null) {
            if (classStudentRepository
                    .existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(classId, userId)) {
                throw new IllegalArgumentException("Bạn đã đăng ký lớp này rồi");
            }
            ClassStudent student = new ClassStudent();
            student.setTutoringClass(tutoringClass);
            student.setEnrolledByUser(user);
            student.setStudentName(client.getFullName());
            student.setStudentPhone(client.getPhone());
            student.setStudentEmail(user.getEmail());
            student.setStatus(ClassStudentStatus.ENROLLED);
            ClassStudent savedStudent = classStudentRepository.save(student);
            auditLogService.record(userId, "REGISTER_CLASS", "ClassStudent", savedStudent.getClassStudentId(),
                    null, java.util.Map.of("classId", classId));

            // Đủ sĩ số tối đa -> tự đóng ghi danh. Lớp tự tạo đã gán gia sư trước khi mở ghi danh
            // nên đủ học sinh là đã ghép (MATCHED).
            Integer max = tutoringClass.getMaxStudents();
            if (max != null && max > 0) {
                long enrolled = classStudentRepository
                        .countByTutoringClass_ClassIdAndStatus(classId, ClassStudentStatus.ENROLLED);
                if (enrolled >= max) {
                    tutoringClass.setStatus(TutoringClassStatus.MATCHED);
                    tutoringClassRepository.save(tutoringClass);
                }
            }
            return;
        }

        throw new ForbiddenException("Chỉ gia sư hoặc phụ huynh/học viên mới đăng ký lớp");
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
        auditLogService.record(user.getUserId(), "ADD_FAVORITE_TUTOR", "Tutor", tutorId, null, null);
    }

    @Override
    @Transactional
    public void removeFavorite(Long tutorId) {
        authHelper.requireRole(UserRole.CLIENT);
        Long userId = authHelper.currentUserId();
        favoriteTutorRepository
                .findByUser_UserIdAndTutor_TutorId(userId, tutorId)
                .ifPresent(favoriteTutorRepository::delete);
        auditLogService.record(userId, "REMOVE_FAVORITE_TUTOR", "Tutor", tutorId, null, null);
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
                .maxStudents(c.getMaxStudents())
                .enrolledCount(classStudentRepository
                        .countByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED))
                .schedule(scheduleSlotRepository.findByTutoringClass_ClassId(c.getClassId()).stream()
                        .map(s -> ScheduleSlotResponse.builder()
                                .slotId(s.getSlotId())
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .toList())
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

    // ===== Yêu cầu mở lớp gửi tới một trung tâm cụ thể (phía phụ huynh) =====

    @Override
    @Transactional(readOnly = true)
    public List<CenterSummaryResponse> listCenters() {
        return tutorCenterRepository.findAll().stream()
                .filter(c -> c.getVerificationStatus() == ProfileVerificationStatus.VERIFIED)
                .map(c -> CenterSummaryResponse.builder()
                        .centerId(c.getCenterId())
                        .companyName(c.getCompanyName())
                        .description(c.getDescription())
                        .address(c.getAddress())
                        .phone(c.getPhone())
                        .avatar(c.getAvatar())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ClassRequestResponse createClassRequest(Long centerId, ClassRequestCreateRequest request) {
        User user = requireUser();
        TutorCenter center = tutorCenterRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm"));
        if (center.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Trung tâm chưa được xác minh");
        }
        ClassRequestStore.ClassRequestData data = classRequestStore.create(
                user.getUserId(), centerId, request.getCategoryId(), request.getNote(), request.getDesiredBudget());
        return classRequestStore.toResponse(data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRequestResponse> listMyClassRequests() {
        User user = requireUser();
        return classRequestStore.findByClient(user.getUserId()).stream()
                .map(classRequestStore::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelClassRequest(String requestId) {
        User user = requireUser();
        ClassRequestStore.ClassRequestData data = classRequestStore.find(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu mở lớp"));
        if (!user.getUserId().equals(data.clientUserId())) {
            throw new ForbiddenException("Không có quyền hủy yêu cầu này");
        }
        if (!ClassRequestStore.STATUS_PENDING.equals(data.status())) {
            throw new IllegalArgumentException("Chỉ có thể hủy yêu cầu đang chờ xử lý");
        }
        classRequestStore.delete(requestId);
    }
}
