package com.tcs.module.marketplace.service.impl;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.exception.VerificationRequiredException;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
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
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.CenterSummaryResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.dto.response.ScheduleSlotResponse;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.MarketplaceService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ContractRepository contractRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final EscrowService escrowService;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassStudentRepository classStudentRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
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
        return classes.stream().map(c -> toClassResponse(c, null, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClass(Long classId, Long assignmentId, Long classStudentId) {
        return toClassResponse(findClass(classId), assignmentId, classStudentId);
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
        return toClassResponse(saved, null, null);
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
        return toClassResponse(saved, null, null);
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
        if (!isTerminationRequestableStatus(tutoringClass.getStatus())) {
            throw new BusinessException("Chỉ lớp đang diễn ra mới có thể yêu cầu chấm dứt sớm");
        }

        validateTerminationSelector(request.getAssignmentId(), request.getClassStudentId());
        TerminationTarget target = resolveTerminationTarget(
                tutoringClass,
                request.getAssignmentId(),
                request.getClassStudentId(),
                requester.getUserId());

        if (hasPendingTermination(target)) {
            throw new BusinessException("Lớp học đã có yêu cầu chấm dứt sớm đang chờ xử lý");
        }

        EscrowTransaction escrow = resolveEscrowForTermination(target);

        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setAssignment(target.assignment());
        termination.setClassStudent(target.classStudent());
        termination.setRequestedBy(requester);
        termination.setReason(request.getReason().trim());
        termination.setEffectiveDate(request.getEffectiveDate());

        if (requiresAdminTerminationReview(tutoringClass, escrow)) {
            termination.setStatus(ClassTerminationStatus.PENDING);
            tutoringClass.setStatus(TutoringClassStatus.DISPUTED);
            tutoringClassRepository.save(tutoringClass);
            return toTerminationResponse(classTerminationRequestRepository.save(termination), tutoringClass);
        }

        SettlementSplit settlement = calculateEarlyTerminationSettlement(tutoringClass, target, escrow);
        termination.setStatus(ClassTerminationStatus.COMPLETED);
        termination.setProcessedAt(LocalDateTime.now());

        escrowService.apply(new ReleaseInstruction(
                settlement.escrow().getEscrowId(),
                settlement.releaseAmount(),
                settlement.refundAmount(),
                buildEarlyTerminationSettlementReason(request.getReason(), settlement)));

        completeTerminationTarget(target);

        tutoringClass.setStatus(TutoringClassStatus.CANCELLED);
        tutoringClassRepository.save(tutoringClass);

        return toTerminationResponse(classTerminationRequestRepository.save(termination), tutoringClass);
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

    private void validateTerminationSelector(Long assignmentId, Long classStudentId) {
        if (assignmentId != null && classStudentId != null) {
            throw new IllegalArgumentException("Chỉ được chọn một trong assignmentId hoặc classStudentId");
        }
    }

    private TerminationTarget resolveTerminationTarget(
            TutoringClass tutoringClass,
            Long assignmentId,
            Long classStudentId,
            Long requesterUserId) {
        if (assignmentId != null) {
            ClassAssignment assignment = resolveActiveAssignment(tutoringClass, assignmentId);
            requireAssignmentTerminationParticipant(tutoringClass, assignment, requesterUserId);
            return new TerminationTarget(assignment, null);
        }
        if (classStudentId != null) {
            ClassStudent classStudent = resolveActiveClassStudent(tutoringClass, classStudentId);
            requireClassStudentTerminationParticipant(tutoringClass, classStudent, requesterUserId);
            return new TerminationTarget(null, classStudent);
        }

        List<TerminationTarget> candidates = accessibleTerminationTargets(tutoringClass, requesterUserId);
        if (candidates.isEmpty()) {
            throw new ForbiddenException("Bạn không có quyền yêu cầu chấm dứt lớp này");
        }
        if (candidates.size() > 1) {
            throw new BusinessException("Lớp học có nhiều hợp đồng hoặc ghi danh, vui lòng mở từ hợp đồng cụ thể");
        }
        return candidates.get(0);
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

    private ClassStudent resolveActiveClassStudent(TutoringClass tutoringClass, Long classStudentId) {
        ClassStudent classStudent = classStudentRepository.findById(classStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh học viên"));
        if (classStudent.getStatus() != ClassStudentStatus.ENROLLED) {
            throw new BusinessException("Ghi danh học viên không còn hoạt động");
        }
        if (!classStudentBelongsToClass(classStudent, tutoringClass.getClassId())) {
            throw new IllegalArgumentException("Ghi danh học viên không thuộc lớp học này");
        }
        return classStudent;
    }

    private boolean assignmentBelongsToClass(ClassAssignment assignment, Long classId) {
        return assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null
                && Objects.equals(assignment.getApplication().getTutoringClass().getClassId(), classId);
    }

    private boolean classStudentBelongsToClass(ClassStudent classStudent, Long classId) {
        return classStudent.getTutoringClass() != null
                && Objects.equals(classStudent.getTutoringClass().getClassId(), classId);
    }

    private void requireAssignmentTerminationParticipant(
            TutoringClass tutoringClass,
            ClassAssignment assignment,
            Long requesterUserId) {
        if (!isAssignmentTerminationParticipant(tutoringClass, assignment, requesterUserId)) {
            throw new ForbiddenException("Bạn không có quyền yêu cầu chấm dứt lớp này");
        }
    }

    private boolean isAssignmentTerminationParticipant(
            TutoringClass tutoringClass,
            ClassAssignment assignment,
            Long requesterUserId) {
        boolean isClassCreator = tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), requesterUserId);
        boolean isAssignedTutor = assignment.getTutor() != null
                && assignment.getTutor().getUser() != null
                && Objects.equals(assignment.getTutor().getUser().getUserId(), requesterUserId);
        return isClassCreator || isAssignedTutor;
    }

    private void requireClassStudentTerminationParticipant(
            TutoringClass tutoringClass,
            ClassStudent classStudent,
            Long requesterUserId) {
        if (!isClassStudentTerminationParticipant(tutoringClass, classStudent, requesterUserId)) {
            throw new ForbiddenException("Bạn không có quyền yêu cầu chấm dứt lớp này");
        }
    }

    private boolean isClassStudentTerminationParticipant(
            TutoringClass tutoringClass,
            ClassStudent classStudent,
            Long requesterUserId) {
        boolean isClassCreator = tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), requesterUserId);
        boolean isEnrolledUser = classStudent.getEnrolledByUser() != null
                && Objects.equals(classStudent.getEnrolledByUser().getUserId(), requesterUserId);
        return isClassCreator || isEnrolledUser;
    }

    private TerminationTarget canRequestTerminationTarget(
            TutoringClass tutoringClass,
            Long assignmentId,
            Long classStudentId) {
        Long currentUserId = currentUserIdOrNull();
        if (currentUserId == null || !isTerminationRequestableStatus(tutoringClass.getStatus())) {
            return null;
        }
        if (assignmentId != null && classStudentId != null) {
            return null;
        }

        try {
            if (assignmentId != null) {
                ClassAssignment assignment = resolveActiveAssignment(tutoringClass, assignmentId);
                TerminationTarget target = new TerminationTarget(assignment, null);
                return !hasPendingTermination(target)
                        && isAssignmentTerminationParticipant(tutoringClass, assignment, currentUserId)
                        ? target
                        : null;
            }
            if (classStudentId != null) {
                ClassStudent classStudent = resolveActiveClassStudent(tutoringClass, classStudentId);
                TerminationTarget target = new TerminationTarget(null, classStudent);
                return !hasPendingTermination(target)
                        && isClassStudentTerminationParticipant(tutoringClass, classStudent, currentUserId)
                        ? target
                        : null;
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        List<TerminationTarget> candidates = accessibleTerminationTargets(tutoringClass, currentUserId).stream()
                .filter(target -> !hasPendingTermination(target))
                .toList();
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private List<TerminationTarget> accessibleTerminationTargets(TutoringClass tutoringClass, Long currentUserId) {
        List<TerminationTarget> targets = new java.util.ArrayList<>();

        classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        tutoringClass.getClassId(), ClassAssignmentStatus.ACTIVE)
                .stream()
                .filter(assignment -> isAssignmentTerminationParticipant(tutoringClass, assignment, currentUserId))
                .map(assignment -> new TerminationTarget(assignment, null))
                .forEach(targets::add);

        classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                        tutoringClass.getClassId(), ClassStudentStatus.ENROLLED)
                .stream()
                .filter(classStudent -> isClassStudentTerminationParticipant(tutoringClass, classStudent, currentUserId))
                .map(classStudent -> new TerminationTarget(null, classStudent))
                .forEach(targets::add);

        return targets;
    }

    private boolean hasPendingTermination(TerminationTarget target) {
        if (target.assignment() != null) {
            return classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                    target.assignment().getAssignmentId(), ClassTerminationStatus.PENDING);
        }
        if (target.classStudent() != null) {
            return classTerminationRequestRepository.existsByClassStudent_ClassStudentIdAndStatus(
                    target.classStudent().getClassStudentId(), ClassTerminationStatus.PENDING);
        }
        return false;
    }

    private boolean isTerminationRequestableStatus(TutoringClassStatus status) {
        return status == TutoringClassStatus.IN_PROGRESS || status == TutoringClassStatus.DISPUTED;
    }

    private boolean requiresAdminTerminationReview(TutoringClass tutoringClass, EscrowTransaction escrow) {
        return tutoringClass.getStatus() == TutoringClassStatus.DISPUTED
                || escrow.getStatus() == EscrowStatus.ON_HOLD
                || escrow.getStatus() == EscrowStatus.DISPUTED;
    }

    private SettlementSplit calculateEarlyTerminationSettlement(
            TutoringClass tutoringClass,
            TerminationTarget target,
            EscrowTransaction escrow) {
        int totalSessions = totalSessions(tutoringClass);
        int completedSessions = completedSessions(tutoringClass, target);
        if (completedSessions > totalSessions) {
            completedSessions = totalSessions;
        }

        BigDecimal releaseAmount = escrow.getAmount()
                .multiply(BigDecimal.valueOf(completedSessions))
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = escrow.getAmount().subtract(releaseAmount);
        return new SettlementSplit(escrow, totalSessions, completedSessions, releaseAmount, refundAmount);
    }

    private EscrowTransaction resolveEscrowForTermination(TerminationTarget target) {
        if (target.assignment() != null) {
            return escrowTransactionRepository.findByAssignment_AssignmentId(target.assignment().getAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow của phân công lớp"));
        }
        if (target.classStudent() != null) {
            return escrowTransactionRepository.findByClassStudent_ClassStudentId(target.classStudent().getClassStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow của ghi danh"));
        }
        throw new BusinessException("Không xác định được escrow cần tất toán");
    }

    private int totalSessions(TutoringClass tutoringClass) {
        Integer configuredSessions = tutoringClass.getNumberOfSessions();
        if (configuredSessions != null && configuredSessions > 0) {
            return configuredSessions;
        }
        int lessonCount = lessonRepository.findByTutoringClass_ClassId(tutoringClass.getClassId()).size();
        if (lessonCount > 0) {
            return lessonCount;
        }
        throw new BusinessException("Lớp học chưa có số buổi hợp lệ để tính tất toán");
    }

    private int completedSessions(TutoringClass tutoringClass, TerminationTarget target) {
        List<Lesson> lessons = lessonRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
        if (target.classStudent() != null) {
            return completedCenterSessions(lessons, target.classStudent());
        }
        return completedPrivateSessions(lessons);
    }

    private int completedPrivateSessions(List<Lesson> lessons) {
        return (int) lessons.stream()
                .filter(lesson -> lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED)
                .count();
    }

    private int completedCenterSessions(List<Lesson> lessons, ClassStudent classStudent) {
        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getLessonId)
                .filter(Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty()) {
            return 0;
        }
        return (int) lessonAttendanceRepository.findByLesson_LessonIdIn(lessonIds).stream()
                .filter(attendance -> attendance.getClassStudent() != null
                        && Objects.equals(
                                attendance.getClassStudent().getClassStudentId(),
                                classStudent.getClassStudentId()))
                .filter(attendance -> attendance.getStatus() == LessonAttendanceStatus.PRESENT)
                .count();
    }

    private String buildEarlyTerminationSettlementReason(String reason, SettlementSplit settlement) {
        return "Chấm dứt sớm lớp học: đã học "
                + settlement.completedSessions()
                + "/"
                + settlement.totalSessions()
                + " buổi. Lý do: "
                + reason.trim();
    }

    private void completeTerminationTarget(TerminationTarget target) {
        if (target.assignment() != null) {
            ClassAssignment assignment = target.assignment();
            assignment.setStatus(ClassAssignmentStatus.TERMINATED);
            classAssignmentRepository.save(assignment);
            contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                    .ifPresent(this::terminateContract);
            return;
        }

        if (target.classStudent() != null) {
            ClassStudent classStudent = target.classStudent();
            classStudent.setStatus(ClassStudentStatus.DROPPED);
            classStudentRepository.save(classStudent);
            contractRepository.findByClassStudent_ClassStudentId(classStudent.getClassStudentId())
                    .ifPresent(this::terminateContract);
        }
    }

    private void terminateContract(com.tcs.module.contract.entity.Contract contract) {
        if (contract.getStatus() != ContractStatus.TERMINATED) {
            contract.setStatus(ContractStatus.TERMINATED);
            contractRepository.save(contract);
        }
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUserId();
    }

    // ===== Yêu cầu mở lớp gửi tới một trung tâm cụ thể (phía phụ huynh) =====

    private static final int MAX_PENDING_CLASS_REQUESTS = 10;

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
        User creator = requireUser();
        requireClient(creator.getUserId());
        TutorCenter center = tutorCenterRepository
                .findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm"));
        if (center.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Chỉ có thể gửi yêu cầu tới trung tâm đã được xác minh.");
        }
        if (!StringUtils.hasText(request.getNote())) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung nguyện vọng");
        }
        // Môn học không bắt buộc: phụ huynh chỉ gửi nguyện vọng ngắn gọn, môn nằm trong nội dung.
        // Nếu có gửi categoryId thì kiểm tra tồn tại.
        Category category = resolveCategory(request.getCategoryId());
        long pending = classRequestStore.findByClient(creator.getUserId()).stream()
                .filter(d -> ClassRequestStore.STATUS_PENDING.equals(d.status()))
                .count();
        if (pending >= MAX_PENDING_CLASS_REQUESTS) {
            throw new IllegalArgumentException("Bạn đang có quá nhiều yêu cầu chờ xử lý.");
        }
        ClassRequestStore.ClassRequestData data = classRequestStore.create(
                creator.getUserId(),
                center.getCenterId(),
                category != null ? category.getCategoryId() : null,
                request.getNote().trim(),
                request.getDesiredBudget());
        return classRequestStore.toResponse(data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRequestResponse> listMyClassRequests() {
        Long userId = requireUser().getUserId();
        return classRequestStore.findByClient(userId).stream()
                .map(classRequestStore::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelClassRequest(String requestId) {
        Long userId = requireUser().getUserId();
        ClassRequestStore.ClassRequestData data = classRequestStore
                .find(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu mở lớp"));
        if (!userId.equals(data.clientUserId())) {
            throw new ForbiddenException("Bạn không có quyền hủy yêu cầu này");
        }
        if (!ClassRequestStore.STATUS_PENDING.equals(data.status())) {
            throw new IllegalArgumentException("Chỉ hủy được yêu cầu đang chờ xử lý");
        }
        classRequestStore.delete(requestId);
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

    private ClassResponse toClassResponse(TutoringClass c, Long assignmentId, Long classStudentId) {
        Client client = clientRepository.findByUser_UserId(c.getCreator().getUserId()).orElse(null);
        TerminationTarget terminationTarget = canRequestTerminationTarget(c, assignmentId, classStudentId);
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
                .canRequestTermination(terminationTarget != null)
                .terminationAssignmentId(terminationTarget != null && terminationTarget.assignment() != null
                        ? terminationTarget.assignment().getAssignmentId()
                        : null)
                .terminationClassStudentId(terminationTarget != null && terminationTarget.classStudent() != null
                        ? terminationTarget.classStudent().getClassStudentId()
                        : null)
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

    private ClassTerminationResponse toTerminationResponse(
            ClassTerminationRequest request,
            TutoringClass tutoringClass) {
        return ClassTerminationResponse.builder()
                .terminationId(request.getTerminationId())
                .classId(tutoringClass.getClassId())
                .assignmentId(request.getAssignment() != null ? request.getAssignment().getAssignmentId() : null)
                .classStudentId(request.getClassStudent() != null
                        ? request.getClassStudent().getClassStudentId()
                        : null)
                .requestedByUserId(request.getRequestedBy().getUserId())
                .reason(request.getReason())
                .effectiveDate(request.getEffectiveDate())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    private record TerminationTarget(ClassAssignment assignment, ClassStudent classStudent) {
    }

    private record SettlementSplit(
            EscrowTransaction escrow,
            int totalSessions,
            int completedSessions,
            BigDecimal releaseAmount,
            BigDecimal refundAmount) {
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
