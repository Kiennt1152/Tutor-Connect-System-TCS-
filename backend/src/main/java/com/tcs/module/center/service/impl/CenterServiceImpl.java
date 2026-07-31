package com.tcs.module.center.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.request.RescheduleDecisionBody;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.ScheduleSlotRequest;
import com.tcs.module.center.dto.request.SubstitutionDecisionBody;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.center.dto.response.ScheduleSlotResponse;
import com.tcs.module.center.dto.response.StudentAttendanceResponse;
import com.tcs.module.center.dto.response.SubstitutionResponse;
import com.tcs.module.center.dto.response.TutorOptionResponse;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import com.tcs.module.center.enums.RecruitmentPostStatus;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.center.service.CenterService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.dto.RescheduleEntry;
import com.tcs.module.marketplace.dto.SubstitutionEntry;
import com.tcs.module.marketplace.service.RescheduleService;
import com.tcs.module.marketplace.service.SubstitutionService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CenterServiceImpl implements CenterService {

    private final AuthHelper authHelper;
    private final RecruitmentPostRepository recruitmentPostRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final GradeRepository gradeRepository;
    private final ProvinceRepository provinceRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassStudentRepository classStudentRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
    private final RescheduleService rescheduleService;
    private final SubstitutionService substitutionService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter D_MM = DateTimeFormatter.ofPattern("dd/MM");

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentPostResponse> listRecruitmentPosts() {
        return recruitmentPostRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public RecruitmentPostResponse createRecruitmentPost(CreateRecruitmentPostRequest request) {
        TutorCenter center = requireCenter();
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Tiêu đề và mô tả là bắt buộc");
        }
        RecruitmentPost post = new RecruitmentPost();
        post.setCenter(center);
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setRequirements(request.getRequirements());
        post.setBenefits(request.getBenefits());
        post.setRequiredExperience(request.getRequiredExperience() != null ? request.getRequiredExperience() : 0);
        post.setMaxPositions(request.getMaxPositions() != null ? request.getMaxPositions() : 1);
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository
                    .findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
            post.setSubject(subject);
        }
        if (request.getLocationId() != null) {
            Location location = locationRepository
                    .findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm"));
            post.setLocation(location);
        }
        post.setStatus(RecruitmentPostStatus.DRAFT);
        RecruitmentPost saved = recruitmentPostRepository.save(post);
        auditLogService.record(center.getUser().getUserId(), "CREATE_RECRUITMENT_POST", "RecruitmentPost",
                saved.getRecruitmentId(), null, request);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId) {
        RecruitmentPost post = findPost(recruitmentId);
        Long userId = authHelper.currentUserId();
        if (!post.getCenter().getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("Không có quyền đăng tin tuyển dụng này");
        }
        post.setStatus(RecruitmentPostStatus.ACTIVE);
        post.setPublishedAt(LocalDateTime.now());
        RecruitmentPost saved = recruitmentPostRepository.save(post);
        auditLogService.record(userId, "PUBLISH_RECRUITMENT_POST", "RecruitmentPost", saved.getRecruitmentId(), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request) {
        Tutor tutor = requireTutor();
        RecruitmentPost post = findPost(recruitmentId);
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Tin tuyển dụng chưa mở");
        }
        RecruitmentApplication application = new RecruitmentApplication();
        application.setRecruitmentPost(post);
        application.setTutor(tutor);
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(RecruitmentApplicationStatus.APPLIED);
        RecruitmentApplication saved = recruitmentApplicationRepository.save(application);
        auditLogService.record(tutor.getUser().getUserId(), "APPLY_RECRUITMENT", "RecruitmentApplication",
                saved.getRecruitmentAppId(), null, request);
    }

    // ===================== UC-14-B: Manage Classes (Tutor Center) =====================

    @Override
    @Transactional(readOnly = true)
    public List<CenterClassResponse> listMyClasses() {
        requireCenter();
        return tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId()).stream()
                .map(this::toClassResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CenterClassResponse getMyClass(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass); // BR-07 / AF-04
        return toClassResponse(tutoringClass);
    }

    @Override
    @Transactional
    public CenterClassResponse createClass(SaveClassRequest request) {
        TutorCenter center = requireCenter();
        validate(request, true);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(center.getUser());
        tutoringClass.setCenter(center);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.DRAFT); // BR-05
        applyFields(tutoringClass, request);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);

        replaceScheduleSlots(saved, request);
        auditLogService.record(center.getUser().getUserId(), "CREATE_CENTER_CLASS", "TutoringClass",
                saved.getClassId(), null, request);
        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public CenterClassResponse updateClass(Long classId, SaveClassRequest request) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass); // BR-07 / AF-04
        // BR-06 / AF-03: chỉ được sửa khi lớp ở trạng thái DRAFT hoặc OPEN.
        if (tutoringClass.getStatus() != TutoringClassStatus.DRAFT
                && tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp học này không thể chỉnh sửa nữa.");
        }
        validate(request, false);
        applyFields(tutoringClass, request);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);

        replaceScheduleSlots(saved, request);
        auditLogService.record(authHelper.currentUserId(), "UPDATE_CENTER_CLASS", "TutoringClass",
                saved.getClassId(), null, request);
        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public CenterClassResponse publishClass(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass); // BR-07 / AF-04
        if (tutoringClass.getStatus() != TutoringClassStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ lớp ở trạng thái nháp mới có thể đăng tải");
        }
        // Bắt buộc đủ 2 gia sư (chính + phụ) trước khi đăng tải.
        boolean hasMain = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .isPresent();
        if (!hasMain) {
            throw new IllegalArgumentException("Cần gán gia sư chính trước khi đăng tải lớp.");
        }
        if (substitutionService.findAssistant(classId).isEmpty()) {
            throw new IllegalArgumentException("Cần gán gia sư phụ trước khi đăng tải lớp.");
        }
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        auditLogService.record(authHelper.currentUserId(), "PUBLISH_CENTER_CLASS", "TutoringClass",
                saved.getClassId(), null, null);
        return toClassResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorOptionResponse> listTutors(Long classId) {
        requireCenter();
        // Nếu có classId: nạp lớp (đảm bảo quyền sở hữu) để đánh dấu gia sư trùng lịch.
        TutoringClass target = null;
        if (classId != null) {
            target = findClass(classId);
            requireOwner(target);
        }
        final TutoringClass targetClass = target;
        return tutorRepository.findAll().stream()
                .map(t -> {
                    TutoringClass conflict =
                            targetClass != null ? findScheduleConflict(targetClass, t.getTutorId()) : null;
                    return TutorOptionResponse.builder()
                            .tutorId(t.getTutorId())
                            .fullName(t.getFullName())
                            .experienceYears(t.getExperienceYears())
                            .ratingAvg(t.getRatingAvg())
                            .verificationStatus(t.getVerificationStatus() == null
                                    ? null : t.getVerificationStatus().name())
                            .phone(t.getPhone())
                            .avatar(t.getAvatar())
                            .bio(t.getBio())
                            .scheduleConflict(conflict != null)
                            .conflictClassTitle(conflict != null ? conflict.getTitle() : null)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public CenterClassResponse assignTutor(Long classId, Long tutorId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        if (tutorId == null) {
            throw new IllegalArgumentException("Vui lòng chọn gia sư");
        }
        Tutor tutor = tutorRepository
                .findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));

        // Chống trùng lịch: gia sư không được có lịch dạy trùng thời gian với lớp đang gán.
        TutoringClass conflict = findScheduleConflict(tutoringClass, tutorId);
        if (conflict != null) {
            throw new IllegalArgumentException("Gia sư " + tutor.getFullName()
                    + " đã có lịch dạy trùng thời gian với lớp này (trùng lớp \""
                    + conflict.getTitle() + "\"). Vui lòng chọn gia sư khác.");
        }

        // Kết thúc lượt gán cũ (nếu có) rồi tạo lượt gán mới.
        classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .ifPresent(old -> {
                    old.setStatus(ClassAssignmentStatus.TERMINATED);
                    classAssignmentRepository.save(old);
                });

        // Liên kết lớp với gia sư qua tutor_applications (schema sẵn có, không cần cột class_id).
        // Dùng lại đơn cũ nếu gia sư đã từng ứng tuyển/được gán lớp này — tránh vi phạm UNIQUE(class_id, tutor_id).
        TutorApplication application = tutorApplicationRepository
                .findFirstByTutoringClass_ClassIdAndTutor_TutorId(classId, tutor.getTutorId())
                .orElseGet(() -> {
                    TutorApplication app = new TutorApplication();
                    app.setTutoringClass(tutoringClass);
                    app.setTutor(tutor);
                    return app;
                });
        application.setStatus(TutorApplicationStatus.ACCEPTED);
        TutorApplication savedApp = tutorApplicationRepository.save(application);

        // Mỗi application chỉ có 1 lượt gán (UNIQUE application_id) — dùng lại nếu đã có, đặt lại ACTIVE.
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_ApplicationId(savedApp.getApplicationId())
                .orElseGet(ClassAssignment::new);
        assignment.setApplication(savedApp);
        assignment.setTutor(tutor);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        classAssignmentRepository.save(assignment);
        auditLogService.record(authHelper.currentUserId(), "ASSIGN_TUTOR", "TutoringClass", classId,
                null, java.util.Map.of("tutorId", tutorId));
        return toClassResponse(tutoringClass);
    }

    @Override
    @Transactional
    public CenterClassResponse unassignTutor(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .ifPresent(a -> {
                    a.setStatus(ClassAssignmentStatus.TERMINATED);
                    classAssignmentRepository.save(a);
                });
        auditLogService.record(authHelper.currentUserId(), "UNASSIGN_TUTOR", "TutoringClass", classId, null, null);
        return toClassResponse(tutoringClass);
    }

    @Override
    @Transactional
    public CenterClassResponse assignAssistant(Long classId, Long tutorId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        if (tutorId == null) {
            throw new IllegalArgumentException("Vui lòng chọn gia sư phụ");
        }
        Tutor tutor = tutorRepository
                .findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));

        // Gia sư phụ phải khác gia sư chính đang dạy lớp.
        ClassAssignment main = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        if (main != null && main.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new IllegalArgumentException(
                    "Gia sư phụ phải khác gia sư chính. Vui lòng chọn gia sư khác.");
        }

        substitutionService.assignAssistant(classId, tutor.getTutorId());
        auditLogService.record(authHelper.currentUserId(), "ASSIGN_ASSISTANT", "TutoringClass", classId,
                null, java.util.Map.of("tutorId", tutorId));
        return toClassResponse(tutoringClass);
    }

    @Override
    @Transactional
    public CenterClassResponse unassignAssistant(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        substitutionService.removeAssistant(classId);
        auditLogService.record(authHelper.currentUserId(), "UNASSIGN_ASSISTANT", "TutoringClass", classId, null, null);
        return toClassResponse(tutoringClass);
    }

    // ===================== Lịch lớp CENTER =====================

    @Override
    @Transactional(readOnly = true)
    public List<CenterScheduleClassResponse> getSchedule(LocalDate date) {
        requireCenter();
        LocalDate d = date != null ? date : LocalDate.now();
        int weekday = d.getDayOfWeek().getValue();

        Map<Long, TutoringClass> myClasses = classesByCenter();
        List<RescheduleEntry> approved = rescheduleService.listApprovedByClassIds(myClasses.keySet());
        Set<Long> movedAway = approved.stream()
                .filter(e -> e.originalDate().equals(d))
                .map(RescheduleEntry::classId)
                .collect(Collectors.toSet());
        // Buổi có gia sư phụ dạy thay (đã duyệt) trong ngày.
        Map<Long, SubstitutionEntry> subOnDate = substitutionService
                .listApprovedByClassIds(myClasses.keySet()).stream()
                .filter(e -> e.date().equals(d))
                .collect(Collectors.toMap(SubstitutionEntry::classId, e -> e, (a, b) -> a));

        List<CenterScheduleClassResponse> result = new ArrayList<>();
        // Buổi thường trong ngày (bỏ qua buổi đã bị dời đi).
        for (TutoringClass c : myClasses.values()) {
            if (c.getStartDate() == null || c.getEndDate() == null
                    || d.isBefore(c.getStartDate()) || d.isAfter(c.getEndDate())
                    || movedAway.contains(c.getClassId())) {
                continue;
            }
            CenterScheduleClassResponse item = buildScheduleItem(c, d, weekday);
            if (item != null) {
                applySubstitution(item, subOnDate.get(c.getClassId()));
                result.add(item);
            }
        }
        // Buổi được dời TỚI ngày này (khung giờ theo thứ của ngày gốc).
        for (RescheduleEntry e : approved) {
            if (!e.newDate().equals(d)) {
                continue;
            }
            TutoringClass c = myClasses.get(e.classId());
            if (c == null) {
                continue;
            }
            CenterScheduleClassResponse item =
                    buildScheduleItem(c, d, e.originalDate().getDayOfWeek().getValue());
            if (item != null) {
                if (e.newStartTime() != null && e.newEndTime() != null) {
                    item.setSlots(List.of(ScheduleSlotResponse.builder()
                            .dayOfWeek(d.getDayOfWeek().getValue())
                            .startTime(e.newStartTime())
                            .endTime(e.newEndTime())
                            .build()));
                }
                item.setRescheduled(true);
                item.setRescheduleNote("Dời từ " + e.originalDate().format(D_MM));
                result.add(item);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleResponse> listReschedules() {
        requireCenter();
        Map<Long, TutoringClass> myClasses = classesByCenter();
        return rescheduleService.listByClassIds(myClasses.keySet()).stream()
                .map(e -> toRescheduleResponse(e, myClasses.get(e.classId())))
                .toList();
    }

    @Override
    @Transactional
    public RescheduleResponse decideReschedule(RescheduleDecisionBody body) {
        requireCenter();
        if (body.getClassId() == null || body.getOriginalDate() == null) {
            throw new IllegalArgumentException("Thiếu thông tin yêu cầu dời lịch");
        }
        TutoringClass c = findClass(body.getClassId());
        requireOwner(c); // chỉ trung tâm sở hữu lớp mới được duyệt
        RescheduleEntry entry =
                rescheduleService.decide(body.getClassId(), body.getOriginalDate(), body.isApprove());
        auditLogService.record(authHelper.currentUserId(), "DECIDE_RESCHEDULE", "TutoringClass", body.getClassId(),
                null, body);
        return toRescheduleResponse(entry, c);
    }

    // ===================== Duyệt yêu cầu dạy thay =====================

    @Override
    @Transactional(readOnly = true)
    public List<SubstitutionResponse> listSubstitutions() {
        requireCenter();
        Map<Long, TutoringClass> myClasses = classesByCenter();
        return substitutionService.listByClassIds(myClasses.keySet()).stream()
                .map(e -> toSubstitutionResponse(e, myClasses.get(e.classId())))
                .toList();
    }

    @Override
    @Transactional
    public SubstitutionResponse decideSubstitution(SubstitutionDecisionBody body) {
        requireCenter();
        if (body.getClassId() == null || body.getDate() == null) {
            throw new IllegalArgumentException("Thiếu thông tin yêu cầu dạy thay");
        }
        TutoringClass c = findClass(body.getClassId());
        requireOwner(c); // chỉ trung tâm sở hữu lớp mới được duyệt
        SubstitutionEntry entry =
                substitutionService.decide(body.getClassId(), body.getDate(), body.isApprove());
        auditLogService.record(authHelper.currentUserId(), "DECIDE_SUBSTITUTION", "TutoringClass", body.getClassId(),
                null, body);
        return toSubstitutionResponse(entry, c);
    }

    /** Ghi đè hiển thị buổi học thành "gia sư phụ dạy thay" (góc nhìn trung tâm, chỉ xem). */
    private void applySubstitution(CenterScheduleClassResponse item, SubstitutionEntry sub) {
        if (sub == null) {
            return;
        }
        String mainName = item.getAssignedTutorName();
        Tutor assistant = sub.tutorId() != null
                ? tutorRepository.findById(sub.tutorId()).orElse(null) : null;
        if (assistant != null) {
            item.setAssignedTutorId(assistant.getTutorId());
            item.setAssignedTutorName(assistant.getFullName());
        }
        item.setSubstituted(true);
        item.setSubstituteNote("Dạy thay" + (mainName != null ? " cho " + mainName : ""));
    }

    private SubstitutionResponse toSubstitutionResponse(SubstitutionEntry e, TutoringClass c) {
        ClassAssignment main = c == null ? null : classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        c.getClassId(), ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        String assistantName = e.tutorId() != null
                ? tutorRepository.findById(e.tutorId()).map(Tutor::getFullName).orElse(null) : null;
        return SubstitutionResponse.builder()
                .classId(e.classId())
                .className(c != null ? c.getTitle() : null)
                .date(e.date())
                .status(e.status())
                .reason(e.reason())
                .mainTutorId(main != null ? main.getTutor().getTutorId() : null)
                .mainTutorName(main != null ? main.getTutor().getFullName() : null)
                .assistantTutorId(e.tutorId())
                .assistantTutorName(assistantName)
                .build();
    }

    private Map<Long, TutoringClass> classesByCenter() {
        Map<Long, TutoringClass> map = new HashMap<>();
        for (TutoringClass c : tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId())) {
            map.put(c.getClassId(), c);
        }
        return map;
    }

    private RescheduleResponse toRescheduleResponse(RescheduleEntry e, TutoringClass c) {
        String tutorName = e.tutorId() != null
                ? tutorRepository.findById(e.tutorId()).map(Tutor::getFullName).orElse(null)
                : null;
        return RescheduleResponse.builder()
                .classId(e.classId())
                .className(c != null ? c.getTitle() : null)
                .originalDate(e.originalDate())
                .newDate(e.newDate())
                .newStartTime(e.newStartTime())
                .newEndTime(e.newEndTime())
                .status(e.status())
                .tutorId(e.tutorId())
                .tutorName(tutorName)
                .reason(e.reason())
                .build();
    }

    private List<ScheduleSlot> slotsOn(Long classId, int weekday) {
        return scheduleSlotRepository.findByTutoringClass_ClassId(classId).stream()
                .filter(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == weekday)
                .sorted(Comparator.comparing(ScheduleSlot::getStartTime))
                .toList();
    }

    private int sessionSequence(LocalDate start, LocalDate date) {
        return (int) Math.max(0, ChronoUnit.DAYS.between(start, date));
    }

    private CenterScheduleClassResponse buildScheduleItem(TutoringClass c, LocalDate date, int weekday) {
        List<ScheduleSlot> slotsToday = slotsOn(c.getClassId(), weekday);
        if (slotsToday.isEmpty()) {
            return null; // lớp không học hôm nay
        }

        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(c.getClassId(), ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        List<ClassStudent> students = classStudentRepository
                .findByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED);

        // Đọc điểm danh của buổi hôm nay (nếu đã có lesson).
        Map<Long, String> attendanceByStudent = new HashMap<>();
        ScheduleSlot repSlot = slotsToday.get(0);
        int seq = sessionSequence(c.getStartDate(), date);
        lessonRepository
                .findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
                        c.getClassId(), repSlot.getSlotId(), seq)
                .ifPresent(lesson -> lessonAttendanceRepository.findByLesson_LessonId(lesson.getLessonId())
                        .forEach(a -> attendanceByStudent.put(
                                a.getClassStudent().getClassStudentId(), a.getStatus().name())));

        List<StudentAttendanceResponse> studentItems = students.stream()
                .map(s -> StudentAttendanceResponse.builder()
                        .classStudentId(s.getClassStudentId())
                        .studentName(s.getStudentName())
                        .studentPhone(s.getStudentPhone())
                        .status(attendanceByStudent.get(s.getClassStudentId()))
                        .build())
                .toList();

        List<ScheduleSlotResponse> slotResponses = slotsToday.stream()
                .map(s -> ScheduleSlotResponse.builder()
                        .slotId(s.getSlotId())
                        .dayOfWeek(s.getDayOfWeek())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .toList();

        return CenterScheduleClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .lessonMode(c.getLessonMode())
                .slots(slotResponses)
                .assignedTutorId(assignment != null ? assignment.getTutor().getTutorId() : null)
                .assignedTutorName(assignment != null ? assignment.getTutor().getFullName() : null)
                .studentCount(students.size())
                .students(studentItems)
                .attendanceTaken(!attendanceByStudent.isEmpty())
                .build();
    }

    private void validate(SaveClassRequest request, boolean isCreate) {
        // BR-01: các trường bắt buộc.
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Tiêu đề là bắt buộc");
        }
        if (!StringUtils.hasText(request.getCategoryName())) {
            throw new IllegalArgumentException("Danh mục là bắt buộc");
        }
        if (!StringUtils.hasText(request.getSubjectName())) {
            throw new IllegalArgumentException("Môn học là bắt buộc");
        }
        if (!StringUtils.hasText(request.getGradeName())) {
            throw new IllegalArgumentException("Khối/lớp là bắt buộc");
        }
        if (!StringUtils.hasText(request.getProvinceName())) {
            throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố");
        }
        if (!StringUtils.hasText(request.getWardName())) {
            throw new IllegalArgumentException("Vui lòng chọn Phường/Xã");
        }
        if (!StringUtils.hasText(request.getAddressDetail())) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ cụ thể");
        }
        if (request.getLessonMode() == null) {
            throw new IllegalArgumentException("Hình thức học là bắt buộc");
        }
        if (request.getRecurringType() == null) {
            throw new IllegalArgumentException("Kiểu lặp lịch là bắt buộc");
        }
        // BR-12: lớp trung tâm chỉ nhận lịch lặp Hằng ngày hoặc Hằng tuần; không cho ONCE
        // (chặn ở service để caller gọi API trực tiếp cũng không lách được, không chỉ dựa vào UI).
        if (request.getRecurringType() != RecurringType.DAILY
                && request.getRecurringType() != RecurringType.WEEKLY) {
            throw new IllegalArgumentException("Kiểu lặp lịch chỉ được là Hằng ngày hoặc Hằng tuần");
        }
        // BR-04: học phí là số dương.
        if (request.getTuitionFee() == null || request.getTuitionFee().signum() <= 0) {
            throw new IllegalArgumentException("Học phí phải là số dương");
        }
        // Số học sinh tối đa: số nguyên dương.
        if (request.getMaxStudents() == null || request.getMaxStudents() <= 0) {
            throw new IllegalArgumentException("Số học sinh tối đa phải là số nguyên dương");
        }
        // Ngày bắt đầu/kết thúc bắt buộc + BR-02.
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }
        // Khi tạo mới: ngày bắt đầu không được ở quá khứ.
        if (isCreate && request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải từ hôm nay trở đi");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        // Lịch học: tối thiểu một khung, mỗi khung hợp lệ.
        if (request.getSchedule() == null || request.getSchedule().isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một khung lịch học");
        }
        boolean daily = request.getRecurringType() == RecurringType.DAILY;
        List<ScheduleSlotRequest> slots = request.getSchedule();
        for (ScheduleSlotRequest slot : slots) {
            if (slot.getStartTime() == null || slot.getEndTime() == null
                    || !slot.getEndTime().isAfter(slot.getStartTime())) {
                throw new IllegalArgumentException("Giờ kết thúc của khung lịch phải sau giờ bắt đầu");
            }
            // Hằng ngày: không cần thứ; Hằng tuần: bắt buộc thứ 1-7.
            if (!daily && (slot.getDayOfWeek() == null || slot.getDayOfWeek() < 1 || slot.getDayOfWeek() > 7)) {
                throw new IllegalArgumentException("Thứ trong tuần của khung lịch không hợp lệ (1-7)");
            }
        }
        // Chống trùng/chồng giờ: DAILY so mọi tiết (áp cho mọi ngày); WEEKLY so trong cùng một thứ.
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ScheduleSlotRequest a = slots.get(i);
                ScheduleSlotRequest b = slots.get(j);
                boolean timeOverlap = a.getStartTime().isBefore(b.getEndTime())
                        && b.getStartTime().isBefore(a.getEndTime());
                if (!timeOverlap) {
                    continue;
                }
                if (daily) {
                    throw new IllegalArgumentException("Các tiết trong ngày bị trùng/chồng giờ");
                }
                if (a.getDayOfWeek().equals(b.getDayOfWeek())) {
                    throw new IllegalArgumentException(
                            "Lịch học bị trùng/chồng giờ giữa các khung trong cùng một ngày");
                }
            }
        }
        // Hằng tuần: các thứ đã chọn phải nằm trong khoảng ngày bắt đầu–kết thúc.
        if (!daily) {
            Set<Integer> allowedDays = weekdaysInRange(request.getStartDate(), request.getEndDate());
            for (ScheduleSlotRequest slot : slots) {
                if (!allowedDays.contains(slot.getDayOfWeek())) {
                    throw new IllegalArgumentException(
                            "Lịch học phải nằm trong khoảng ngày bắt đầu và ngày kết thúc");
                }
            }
        }
    }

    /**
     * Tìm lớp mà gia sư đang phụ trách (ACTIVE) bị trùng lịch với lớp {@code target}.
     * Trùng khi: khoảng ngày giao nhau, và có tiết cùng thứ (thứ đó nằm trong phần ngày giao)
     * với giờ chồng lên nhau. Trả về lớp trùng đầu tiên tìm được, hoặc null nếu không trùng.
     */
    private TutoringClass findScheduleConflict(TutoringClass target, Long tutorId) {
        if (target.getStartDate() == null || target.getEndDate() == null) {
            return null;
        }
        List<ScheduleSlot> targetSlots =
                scheduleSlotRepository.findByTutoringClass_ClassId(target.getClassId());
        if (targetSlots.isEmpty()) {
            return null;
        }
        for (ClassAssignment a :
                classAssignmentRepository.findByTutor_TutorIdAndStatus(tutorId, ClassAssignmentStatus.ACTIVE)) {
            TutoringClass other = a.getApplication() != null ? a.getApplication().getTutoringClass() : null;
            if (other == null || other.getClassId().equals(target.getClassId())) {
                continue; // bỏ qua chính lớp đang gán
            }
            if (other.getStartDate() == null || other.getEndDate() == null) {
                continue;
            }
            // Phần ngày giao nhau giữa hai lớp.
            LocalDate overlapStart = target.getStartDate().isAfter(other.getStartDate())
                    ? target.getStartDate() : other.getStartDate();
            LocalDate overlapEnd = target.getEndDate().isBefore(other.getEndDate())
                    ? target.getEndDate() : other.getEndDate();
            if (overlapStart.isAfter(overlapEnd)) {
                continue; // không giao ngày
            }
            Set<Integer> windowDays = weekdaysInRange(overlapStart, overlapEnd);
            List<ScheduleSlot> otherSlots =
                    scheduleSlotRepository.findByTutoringClass_ClassId(other.getClassId());
            for (ScheduleSlot s1 : targetSlots) {
                if (s1.getDayOfWeek() == null || s1.getStartTime() == null || s1.getEndTime() == null
                        || !windowDays.contains(s1.getDayOfWeek())) {
                    continue;
                }
                for (ScheduleSlot s2 : otherSlots) {
                    if (s2.getDayOfWeek() == null || s2.getStartTime() == null || s2.getEndTime() == null) {
                        continue;
                    }
                    if (!s1.getDayOfWeek().equals(s2.getDayOfWeek())) {
                        continue;
                    }
                    boolean timeOverlap = s1.getStartTime().isBefore(s2.getEndTime())
                            && s2.getStartTime().isBefore(s1.getEndTime());
                    if (timeOverlap) {
                        return other;
                    }
                }
            }
        }
        return null;
    }

    /** Tập các thứ (1=T2..7=CN) xuất hiện trong khoảng ngày [start, end]. */
    private Set<Integer> weekdaysInRange(LocalDate start, LocalDate end) {
        Set<Integer> days = new HashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days.add(d.getDayOfWeek().getValue());
            if (days.size() == 7) {
                break;
            }
        }
        return days;
    }

    /**
     * Số buổi học tự tính theo lịch trong khoảng ngày.
     * DAILY: (số ngày trong khoảng) × (số tiết/ngày); WEEKLY: đếm mọi lần lặp theo thứ; ONCE: mỗi khung một lần.
     */
    private int computeSessions(SaveClassRequest request) {
        List<ScheduleSlotRequest> slots = request.getSchedule();
        RecurringType type = request.getRecurringType();
        if (type == RecurringType.ONCE) {
            return slots.size();
        }
        if (type == RecurringType.DAILY) {
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            return (int) days * slots.size();
        }
        int count = 0;
        for (LocalDate d = request.getStartDate(); !d.isAfter(request.getEndDate()); d = d.plusDays(1)) {
            int iso = d.getDayOfWeek().getValue();
            for (ScheduleSlotRequest slot : slots) {
                if (slot.getDayOfWeek() != null && slot.getDayOfWeek() == iso) {
                    count++;
                }
            }
        }
        return count;
    }

    private void applyFields(TutoringClass tutoringClass, SaveClassRequest request) {
        tutoringClass.setTitle(request.getTitle().trim());
        tutoringClass.setDescription(
                StringUtils.hasText(request.getDescription()) ? request.getDescription() : "");
        tutoringClass.setCategory(resolveOrCreateCategory(request.getCategoryName()));
        tutoringClass.setSubject(resolveOrCreateSubject(request.getSubjectName()));
        tutoringClass.setGrade(resolveOrCreateGrade(request.getGradeName()));
        tutoringClass.setLocation(resolveOrCreateLocation(
                request.getProvinceName(), request.getWardName(), request.getAddressDetail()));
        tutoringClass.setLessonMode(request.getLessonMode());
        tutoringClass.setNumberOfSessions(computeSessions(request)); // tự tính, không tin giá trị client
        // DB chỉ nhận ONCE/WEEKLY; "Hằng ngày" lưu dưới dạng WEEKLY (đã trải tiết ra mọi ngày trong khoảng).
        tutoringClass.setRecurringType(
                request.getRecurringType() == RecurringType.DAILY
                        ? RecurringType.WEEKLY
                        : request.getRecurringType());
        tutoringClass.setStartDate(request.getStartDate());
        tutoringClass.setEndDate(request.getEndDate());
        tutoringClass.setTuitionFee(request.getTuitionFee());
        tutoringClass.setMaxStudents(request.getMaxStudents());
    }

    private void replaceScheduleSlots(TutoringClass tutoringClass, SaveClassRequest request) {
        List<ScheduleSlot> existing =
                scheduleSlotRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
        if (!existing.isEmpty()) {
            scheduleSlotRepository.deleteAll(existing);
        }
        List<ScheduleSlotRequest> reqSlots = request.getSchedule();
        if (reqSlots == null) {
            return;
        }
        List<ScheduleSlot> toSave = new ArrayList<>();
        if (request.getRecurringType() == RecurringType.DAILY) {
            // Hằng ngày: mỗi tiết được áp cho tất cả các thứ có trong khoảng ngày.
            Set<Integer> days = weekdaysInRange(request.getStartDate(), request.getEndDate());
            for (ScheduleSlotRequest req : reqSlots) {
                for (Integer day : days) {
                    toSave.add(newSlot(tutoringClass, day, req.getStartTime(), req.getEndTime()));
                }
            }
        } else {
            for (ScheduleSlotRequest req : reqSlots) {
                toSave.add(newSlot(tutoringClass, req.getDayOfWeek(), req.getStartTime(), req.getEndTime()));
            }
        }
        scheduleSlotRepository.saveAll(toSave);
    }

    private ScheduleSlot newSlot(TutoringClass tutoringClass, Integer day, LocalTime start, LocalTime end) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setTutoringClass(tutoringClass);
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return slot;
    }

    private void requireOwner(TutoringClass tutoringClass) {
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa lớp học này");
        }
    }

    private TutoringClass findClass(Long classId) {
        return tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
    }

    // Tìm-hoặc-tạo theo tên người dùng tự nhập (giữ FK toàn vẹn mà không cần dropdown).
    private Category resolveOrCreateCategory(String name) {
        String n = name.trim();
        return categoryRepository.findByNameIgnoreCase(n).orElseGet(() -> {
            Category c = new Category();
            c.setName(n);
            return categoryRepository.save(c);
        });
    }

    private Subject resolveOrCreateSubject(String name) {
        String n = name.trim();
        return subjectRepository.findFirstBySubjectNameIgnoreCase(n).orElseGet(() -> {
            Subject s = new Subject();
            s.setSubjectName(n);
            return subjectRepository.save(s);
        });
    }

    private Grade resolveOrCreateGrade(String name) {
        String n = name.trim();
        return gradeRepository.findFirstByGradeNameIgnoreCase(n).orElseGet(() -> {
            Grade g = new Grade();
            g.setGradeName(n);
            return gradeRepository.save(g);
        });
    }

    // Tìm-hoặc-tạo địa điểm: khớp theo (tỉnh + phường + địa chỉ cụ thể), chỉ tạo mới khi chưa có
    // (UC14: match/free-text catalog rồi mới tạo mới — tránh sinh Location trùng lặp mỗi lần lưu lớp).
    private Location resolveOrCreateLocation(String provinceName, String wardName, String addressDetail) {
        String ward = wardName.trim();
        String detail = addressDetail.trim();
        // Mô hình 2 cấp: address_line lưu địa chỉ cụ thể; ward_name/province lưu cấp hành chính.
        Province province = resolveOrCreateProvince(provinceName.trim());
        return locationRepository
                .findFirstByProvince_ProvinceIdAndWardNameIgnoreCaseAndAddressLineIgnoreCase(
                        province.getProvinceId(), ward, detail)
                .orElseGet(() -> {
                    Location loc = new Location();
                    loc.setAddressLine(detail);
                    loc.setWardName(ward);
                    loc.setDistrictName(null);
                    loc.setProvince(province);
                    return locationRepository.save(loc);
                });
    }

    private Province resolveOrCreateProvince(String name) {
        String n = name.trim();
        return provinceRepository.findFirstByProvinceNameIgnoreCase(n).orElseGet(() -> {
            Province p = new Province();
            p.setProvinceName(n);
            return provinceRepository.save(p);
        });
    }

    private CenterClassResponse toClassResponse(TutoringClass c) {
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(c.getClassId(), ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        Long assistantId = substitutionService.findAssistant(c.getClassId()).orElse(null);
        Tutor assistant = assistantId != null ? tutorRepository.findById(assistantId).orElse(null) : null;
        List<ScheduleSlotResponse> schedule =
                scheduleSlotRepository.findByTutoringClass_ClassId(c.getClassId()).stream()
                        .map(s -> ScheduleSlotResponse.builder()
                                .slotId(s.getSlotId())
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .toList();
        List<StudentAttendanceResponse> students = classStudentRepository
                .findByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED).stream()
                .map(s -> StudentAttendanceResponse.builder()
                        .classStudentId(s.getClassStudentId())
                        .studentName(s.getStudentName())
                        .studentPhone(s.getStudentPhone())
                        .status(null)
                        .build())
                .toList();
        return CenterClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .description(c.getDescription())
                .creatorId(c.getCreator().getUserId())
                .centerId(c.getCenter() != null ? c.getCenter().getCenterId() : null)
                .categoryId(c.getCategory() != null ? c.getCategory().getCategoryId() : null)
                .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeId(c.getGrade() != null ? c.getGrade().getGradeId() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .locationId(c.getLocation() != null ? c.getLocation().getLocationId() : null)
                .locationLabel(locationLabel(c.getLocation()))
                .locationText(c.getLocation() != null ? c.getLocation().getAddressLine() : null)
                .provinceName(c.getLocation() != null && c.getLocation().getProvince() != null
                        ? c.getLocation().getProvince().getProvinceName() : null)
                .wardName(c.getLocation() != null ? c.getLocation().getWardName() : null)
                .addressDetail(c.getLocation() != null ? c.getLocation().getAddressLine() : null)
                .lessonMode(c.getLessonMode())
                .numberOfSessions(c.getNumberOfSessions())
                .recurringType(c.getRecurringType())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .tuitionFee(c.getTuitionFee())
                .maxStudents(c.getMaxStudents())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .schedule(schedule)
                .assignedTutorId(assignment != null ? assignment.getTutor().getTutorId() : null)
                .assignedTutorName(assignment != null ? assignment.getTutor().getFullName() : null)
                .assistantTutorId(assistant != null ? assistant.getTutorId() : null)
                .assistantTutorName(assistant != null ? assistant.getFullName() : null)
                .students(students)
                .build();
    }

    private String locationLabel(Location location) {
        if (location == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(location.getAddressLine());
        if (StringUtils.hasText(location.getWardName())) {
            sb.append(", ").append(location.getWardName());
        }
        if (location.getProvince() != null && StringUtils.hasText(location.getProvince().getProvinceName())) {
            sb.append(", ").append(location.getProvince().getProvinceName());
        }
        return sb.toString();
    }

    private TutorCenter requireCenter() {
        authHelper.requireRole(UserRole.TUTOR_CENTER);
        return tutorCenterRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ trung tâm"));
    }

    private Tutor requireTutor() {
        authHelper.requireRole(UserRole.TUTOR);
        return tutorRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
    }

    private RecruitmentPost findPost(Long recruitmentId) {
        return recruitmentPostRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
    }

    private RecruitmentPostResponse toResponse(RecruitmentPost post) {
        return RecruitmentPostResponse.builder()
                .recruitmentId(post.getRecruitmentId())
                .centerId(post.getCenter().getCenterId())
                .centerName(post.getCenter().getCompanyName())
                .title(post.getTitle())
                .description(post.getDescription())
                .maxPositions(post.getMaxPositions())
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
