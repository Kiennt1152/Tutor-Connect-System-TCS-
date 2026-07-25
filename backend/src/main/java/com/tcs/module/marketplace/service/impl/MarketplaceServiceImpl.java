package com.tcs.module.marketplace.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tcs.module.marketplace.dto.request.ExtraLessonRequest;
import com.tcs.module.marketplace.dto.request.RescheduleDecisionRequest;
import com.tcs.module.marketplace.dto.request.RescheduleLessonRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.AssignmentResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.LessonResponse;
import com.tcs.module.marketplace.dto.response.RescheduleRequestResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonRescheduleRequest;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.RescheduleRequestStatus;
import com.tcs.module.marketplace.enums.RescheduleRequestType;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.LessonRescheduleRequestRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final FavoriteTutorRepository favoriteTutorRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocationRepository locationRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final LessonRepository lessonRepository;
    private final LessonRescheduleRequestRepository rescheduleRequestRepository;
    // Khởi tạo trực tiếp (không có bean ObjectMapper trong context) — giống GoogleTokenVerifier.
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Mã thứ của form ({@code detailsJson.slots[].day}) → ISO day-of-week (Thứ 2 = 1 … CN = 7),
     * khớp {@link DayOfWeek#getValue()}. Lưu ý KHÔNG phải quy ước DAYOFWEEK() của MySQL (CN = 1).
     */
    private static final Map<String, Integer> DAY_CODE_TO_ISO = Map.of(
            "T2", 1, "T3", 2, "T4", 3, "T5", 4, "T6", 5, "T7", 6, "CN", 7);

    /** Học phí/giờ thấp nhất chấp nhận được (đ) — khớp FEE_PER_HOUR_MIN của form phía client. */
    private static final BigDecimal MIN_RATE_PER_HOUR = BigDecimal.valueOf(50_000);

    /** Khóa của môn "Khác" (tự nhập) trong detailsJson.subjectIds. */
    private static final String OTHER_SUBJECT_KEY = "other";

    /** Giới hạn cột tutoring_classes.title. */
    private static final int TITLE_MAX_LENGTH = 150;

    /** Khối phổ thông dạng "Lớp 12" — bắt lấy phần số để ghép " lớp 12". */
    private static final Pattern GRADE_NUMBER_PATTERN =
            Pattern.compile("^Lớp\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

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
    @Transactional(readOnly = true)
    public List<ClassResponse> listMyClasses() {
        return tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId()).stream()
                .map(this::toClassResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        User creator = requireUser();
        requireClient(creator.getUserId());
        if (request.getSubjectId() == null && !StringUtils.hasText(request.getDetailsJson())) {
            throw new IllegalArgumentException("Vui lòng chọn môn học");
        }
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(creator);
        applyRequest(tutoringClass, request);
        tutoringClass.setBudget(request.getBudget() != null ? request.getBudget() : BigDecimal.ZERO);
        tutoringClass.setStatus(TutoringClassStatus.DRAFT);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long classId, CreateClassRequest request) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền sửa lớp này");
        }
        if (tutoringClass.getStatus() != TutoringClassStatus.DRAFT) {
            throw new IllegalArgumentException("Lớp đã đăng thì không thể sửa nữa");
        }
        if (request.getSubjectId() == null && !StringUtils.hasText(request.getDetailsJson())) {
            throw new IllegalArgumentException("Vui lòng chọn môn học");
        }
        applyRequest(tutoringClass, request);
        if (request.getBudget() != null) tutoringClass.setBudget(request.getBudget());
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    /** Áp các trường từ request vào entity; tự sinh tiêu đề/mô tả khi bỏ trống. */
    private void applyRequest(TutoringClass tutoringClass, CreateClassRequest request) {
        Subject subject = resolveSubject(request.getSubjectId());
        Grade grade = resolveGrade(request.getGradeId());
        tutoringClass.setCategory(resolveCategory(request.getCategoryId()));
        tutoringClass.setSubject(subject);
        tutoringClass.setGrade(grade);
        tutoringClass.setLearningGoal(trimToNull(request.getLearningGoal()));
        tutoringClass.setTutorRequirement(trimToNull(request.getTutorRequirement()));
        tutoringClass.setLocation(resolveLocation(request.getLocationId()));
        tutoringClass.setAddress(trimToNull(request.getAddress()));
        tutoringClass.setTitle(resolveTitle(request, subject, grade));
        tutoringClass.setDescription(resolveDescription(request, subject, grade));
        tutoringClass.setDetailsJson(request.getDetailsJson());
        if (request.getLessonMode() != null) tutoringClass.setLessonMode(request.getLessonMode());
        if (request.getNumberOfSessions() != null) tutoringClass.setNumberOfSessions(request.getNumberOfSessions());
        if (request.getStartDate() != null) tutoringClass.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) tutoringClass.setEndDate(request.getEndDate());
        if (request.getTuitionFee() != null) tutoringClass.setTuitionFee(request.getTuitionFee());
        if (request.getRecurringType() != null) tutoringClass.setRecurringType(request.getRecurringType());
    }

    /**
     * Tiêu đề tự sinh nêu ĐỦ các môn của lớp (lấy từ detailsJson), không chỉ môn chính —
     * lớp Toán + Lý + Hóa phải hiện cả 3 để gia sư biết mình có dạy đủ hay không.
     */
    private String resolveTitle(CreateClassRequest request, Subject subject, Grade grade) {
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle().trim();
        }
        return autoTitle(request.getDetailsJson(), subject, grade);
    }

    /** Tiêu đề tự sinh, bỏ qua tiêu đề người dùng nhập (dùng cả cho mô tả mặc định). */
    private String autoTitle(String detailsJson, Subject subject, Grade grade) {
        List<String> names = subjectNamesFromJson(detailsJson);
        if (names.isEmpty() && subject != null) {
            names = List.of(subject.getSubjectName());
        }
        StringBuilder sb = new StringBuilder("Cần tìm gia sư");
        if (!names.isEmpty()) {
            sb.append(names.size() > 1 ? " các môn " : " môn ").append(String.join(", ", names));
        }
        if (grade != null) {
            sb.append(gradeSuffix(grade.getGradeName()));
        }
        String title = sb.toString();
        return title.length() > TITLE_MAX_LENGTH
                ? title.substring(0, TITLE_MAX_LENGTH - 1) + "…"
                : title;
    }

    private String resolveDescription(CreateClassRequest request, Subject subject, Grade grade) {
        if (StringUtils.hasText(request.getDescription())) {
            return request.getDescription().trim();
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.getLearningGoal())) {
            sb.append("Mục tiêu: ").append(request.getLearningGoal().trim());
        }
        if (StringUtils.hasText(request.getTutorRequirement())) {
            if (sb.length() > 0) sb.append(". ");
            sb.append("Yêu cầu gia sư: ").append(request.getTutorRequirement().trim());
        }
        if (sb.length() == 0) {
            sb.append(autoTitle(request.getDetailsJson(), subject, grade));
        }
        return sb.toString();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
        // Bắt trùng ở đây để báo rõ lý do, thay vì để uq_tutor_applications ném lỗi chung chung.
        if (tutorApplicationRepository.existsByTutoringClass_ClassIdAndTutor_TutorId(
                classId, tutor.getTutorId())) {
            throw new IllegalArgumentException(
                    "Bạn đã ứng tuyển lớp này rồi. Mỗi lớp chỉ nộp được một đơn.");
        }
        Map<String, BigDecimal> rates = resolveProposedRates(request, tutoringClass);

        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setProposedRatesJson(rates.isEmpty() ? null : writeJson(rates));
        application.setProposedRate(highestRate(rates, request.getProposedRate()));
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(TutorApplicationStatus.SUBMITTED);
        tutorApplicationRepository.save(application);
    }

    /**
     * Gia sư phải báo giá cho MỌI môn của lớp. Đơn gửi từ client cũ (chỉ có một mức chung)
     * được quy về mức đó cho từng môn để dữ liệu đồng nhất.
     */
    private Map<String, BigDecimal> resolveProposedRates(
            ApplyClassRequest request, TutoringClass tutoringClass) {
        List<String> subjectKeys = classSubjectKeys(tutoringClass);
        Map<String, BigDecimal> requested = request.getProposedRates();
        Map<String, BigDecimal> resolved = new LinkedHashMap<>();

        if (requested == null || requested.isEmpty()) {
            BigDecimal flat = request.getProposedRate();
            if (flat == null) {
                throw new IllegalArgumentException("Vui lòng nhập học phí đề xuất cho từng môn của lớp");
            }
            requireValidRate(flat, null);
            for (String key : subjectKeys) {
                resolved.put(key, flat);
            }
            return resolved;
        }

        for (String key : subjectKeys) {
            BigDecimal rate = requested.get(key);
            if (rate == null) {
                throw new IllegalArgumentException("Vui lòng nhập học phí đề xuất cho tất cả các môn của lớp");
            }
            requireValidRate(rate, key);
            resolved.put(key, rate);
        }
        return resolved;
    }

    private void requireValidRate(BigDecimal rate, String subjectKey) {
        String subject = subjectKey == null || OTHER_SUBJECT_KEY.equals(subjectKey)
                ? ""
                : " cho môn #" + subjectKey;
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("Học phí đề xuất" + subject + " phải lớn hơn 0");
        }
        if (rate.compareTo(MIN_RATE_PER_HOUR) < 0) {
            throw new IllegalArgumentException(
                    "Học phí đề xuất" + subject + " tối thiểu " + MIN_RATE_PER_HOUR.longValue() + "đ/giờ");
        }
    }

    /** Khóa các môn trong detailsJson.subjectIds ("other" = môn tự nhập); rỗng nếu JSON thiếu/hỏng. */
    private List<String> subjectKeysFromJson(String detailsJson) {
        if (!StringUtils.hasText(detailsJson)) {
            return List.of();
        }
        try {
            JsonNode ids = objectMapper.readTree(detailsJson).path("subjectIds");
            if (!ids.isArray()) {
                return List.of();
            }
            List<String> keys = new ArrayList<>();
            for (JsonNode id : ids) {
                keys.add(id.asText());
            }
            return keys;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** Các môn của lớp theo khóa dùng trong detailsJson.subjectIds; lớp cũ → suy từ cột phẳng. */
    private List<String> classSubjectKeys(TutoringClass tutoringClass) {
        List<String> keys = subjectKeysFromJson(tutoringClass.getDetailsJson());
        if (!keys.isEmpty()) {
            return keys;
        }
        return tutoringClass.getSubject() != null
                ? List.of(String.valueOf(tutoringClass.getSubject().getSubjectId()))
                : List.of();
    }

    /**
     * Phần khối lớp trong tiêu đề: "Lớp 12" → " lớp 12" (tránh lặp thành "lớp Lớp 12");
     * khối không phải lớp phổ thông ("Luyện thi Đại học") thì nối bằng dấu gạch.
     */
    private String gradeSuffix(String gradeName) {
        String name = gradeName.trim();
        Matcher m = GRADE_NUMBER_PATTERN.matcher(name);
        return m.matches() ? " lớp " + m.group(1) : " - " + name;
    }

    /** Tên đầy đủ các môn của lớp (kể cả môn "Khác" tự nhập) để nêu trong tiêu đề. */
    private List<String> subjectNamesFromJson(String detailsJson) {
        List<String> keys = subjectKeysFromJson(detailsJson);
        if (keys.isEmpty()) {
            return List.of();
        }
        String otherName = "";
        try {
            otherName = objectMapper.readTree(detailsJson).path("subjectOther").asText("").trim();
        } catch (JsonProcessingException ignored) {
            // Đã parse được subjectIds ở trên nên nhánh này gần như không xảy ra.
        }
        List<String> names = new ArrayList<>();
        for (String key : keys) {
            if (OTHER_SUBJECT_KEY.equals(key)) {
                names.add(StringUtils.hasText(otherName) ? otherName : "Môn học khác");
                continue;
            }
            try {
                subjectRepository.findById(Long.valueOf(key))
                        .map(Subject::getSubjectName)
                        .ifPresent(names::add);
            } catch (NumberFormatException ignored) {
                // Khóa môn lạ → bỏ qua, tiêu đề vẫn nêu các môn còn lại.
            }
        }
        return names;
    }

    /** Mức cao nhất trong các môn — dùng cho cột proposed_rate cũ và chấm điểm AI. */
    private BigDecimal highestRate(Map<String, BigDecimal> rates, BigDecimal fallback) {
        return rates.values().stream().max(BigDecimal::compareTo).orElse(fallback);
    }

    private String writeJson(Map<String, BigDecimal> rates) {
        try {
            return objectMapper.writeValueAsString(rates);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Không đọc được học phí đề xuất", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listMyAppliedClassIds() {
        Tutor tutor = requireTutor();
        return tutorApplicationRepository.findByTutor_TutorId(tutor.getTutorId()).stream()
                .map(app -> app.getTutoringClass().getClassId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicantResponse> listApplicants(Long classId) {
        TutoringClass tutoringClass = requireOwnedClass(classId);
        List<TutorApplication> applications =
                tutorApplicationRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
        // Chấm điểm AI rồi xếp hạng giảm dần; Top 5 điểm cao nhất được đánh dấu "gợi ý".
        List<ApplicantResponse> ranked = applications.stream()
                .map(app -> toApplicant(app, tutoringClass))
                .sorted(Comparator.comparingInt(ApplicantResponse::getMatchScore).reversed())
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        for (int i = 0; i < ranked.size() && i < 5; i++) {
            ranked.get(i).setRecommended(true);
        }
        return ranked;
    }

    @Override
    @Transactional
    public void chooseApplicant(Long classId, Long applicationId) {
        TutoringClass tutoringClass = requireOwnedClass(classId);
        TutorApplication chosen = tutorApplicationRepository
                .findById(applicationId)
                .filter(a -> a.getTutoringClass().getClassId().equals(tutoringClass.getClassId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp không còn ở trạng thái đang mở để chọn gia sư");
        }
        for (TutorApplication app :
                tutorApplicationRepository.findByTutoringClass_ClassId(tutoringClass.getClassId())) {
            app.setStatus(
                    app.getApplicationId().equals(applicationId)
                            ? TutorApplicationStatus.ACCEPTED
                            : TutorApplicationStatus.REJECTED);
            app.setReviewedAt(LocalDateTime.now());
        }
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        tutoringClassRepository.save(tutoringClass);

        // Chọn xong mới là lời mời — gia sư phải bấm nhận thì lớp mới chạy.
        // Dùng lại phân công cũ nếu gia sư này từng bị từ chối trước đó (uq theo application_id).
        ClassAssignment assignment = classAssignmentRepository
                .findByApplication_ApplicationId(applicationId)
                .orElseGet(ClassAssignment::new);
        assignment.setTutor(chosen.getTutor());
        assignment.setApplication(chosen);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        classAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> listMyAssignments() {
        // Gia sư xem lớp mình được mời/đang dạy; Client xem ai đang dạy lớp của mình.
        List<ClassAssignment> assignments = isClient()
                ? classAssignmentRepository.findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(
                        authHelper.currentUserId())
                : classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(
                        requireTutor().getTutorId());
        return assignments.stream()
                .filter(a -> a.getApplication() != null) // lớp gán nội bộ (CENTER) không có đơn
                .map(this::toAssignment)
                .toList();
    }

    private boolean isClient() {
        return authHelper.requireAuthenticated().getRole() == UserRole.CLIENT;
    }

    @Override
    @Transactional
    public void acceptAssignment(Long assignmentId) {
        ClassAssignment assignment = requireMyPendingAssignment(assignmentId);
        TutoringClass tutoringClass = assignment.getApplication().getTutoringClass();

        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        classAssignmentRepository.save(assignment);

        generateSchedule(tutoringClass, assignment.getTutor());

        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClassRepository.save(tutoringClass);
    }

    @Override
    @Transactional
    public void declineAssignment(Long assignmentId) {
        ClassAssignment assignment = requireMyPendingAssignment(assignmentId);
        TutoringClass tutoringClass = assignment.getApplication().getTutoringClass();

        assignment.setStatus(ClassAssignmentStatus.DECLINED);
        classAssignmentRepository.save(assignment);

        // Mở lại lớp: đơn của gia sư từ chối giữ REJECTED, các đơn còn lại quay về SUBMITTED
        // để Client có người mà chọn lại.
        Long declinedId = assignment.getApplication().getApplicationId();
        for (TutorApplication app :
                tutorApplicationRepository.findByTutoringClass_ClassId(tutoringClass.getClassId())) {
            if (app.getApplicationId().equals(declinedId)) {
                app.setStatus(TutorApplicationStatus.REJECTED);
            } else {
                app.setStatus(TutorApplicationStatus.SUBMITTED);
                app.setReviewedAt(null);
            }
        }
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        tutoringClassRepository.save(tutoringClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> listMyLessons() {
        LocalDate today = LocalDate.now();
        return myLessons().stream().map(lesson -> toLesson(lesson, today)).toList();
    }

    @Override
    @Transactional
    public void checkInLesson(Long lessonId) {
        Lesson lesson = requireMyLesson(lessonId);
        if (lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED) {
            throw new IllegalArgumentException("Buổi học này đã điểm danh xong");
        }
        requireLessonIsToday(lesson);
        if (lesson.getTutorCheckInAt() != null) {
            throw new IllegalArgumentException("Bạn đã điểm danh vào buổi này rồi");
        }
        lesson.setTutorCheckInAt(LocalDateTime.now());
        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void checkOutLesson(Long lessonId) {
        Lesson lesson = requireMyLesson(lessonId);
        if (lesson.getTutorCheckInAt() == null) {
            throw new IllegalArgumentException("Cần điểm danh vào buổi trước khi kết thúc buổi");
        }
        if (lesson.getTutorCheckOutAt() != null) {
            throw new IllegalArgumentException("Buổi học này đã kết thúc rồi");
        }
        requireLessonIsToday(lesson);
        lesson.setTutorCheckOutAt(LocalDateTime.now());
        lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void markAttendance(Long lessonId) {
        Lesson lesson = requireMyLesson(lessonId);
        if (lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED) {
            throw new IllegalArgumentException("Buổi học này đã điểm danh xong");
        }
        requireLessonIsToday(lesson);
        // Một cú bấm: ghi giờ vào/ra (nếu chưa có) rồi đánh dấu hoàn thành.
        LocalDateTime now = LocalDateTime.now();
        if (lesson.getTutorCheckInAt() == null) {
            lesson.setTutorCheckInAt(now);
        }
        lesson.setTutorCheckOutAt(now);
        lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
        lessonRepository.save(lesson);
    }

    /** Điểm danh chỉ trong đúng ngày buổi học diễn ra (chốt theo quyết định nghiệp vụ). */
    private void requireLessonIsToday(Lesson lesson) {
        LocalDate today = LocalDate.now();
        if (!today.equals(lesson.getLessonDate())) {
            throw new IllegalArgumentException(
                    "Chỉ điểm danh được trong ngày diễn ra buổi học ("
                            + lesson.getLessonDate() + "). Hôm nay là " + today + ".");
        }
    }

    @Override
    @Transactional
    public RescheduleRequestResponse requestReschedule(Long lessonId, RescheduleLessonRequest request) {
        User me = requireUser();
        Lesson lesson = lessonRepository
                .findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
        requireClassParticipant(lesson.getTutoringClass(), me);

        if (lesson.getAttendanceStatus() != AttendanceStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Buổi này đã điểm danh xong nên không đổi lịch được nữa");
        }
        if (rescheduleRequestRepository.existsByLesson_LessonIdAndStatus(
                lessonId, RescheduleRequestStatus.PENDING)) {
            throw new IllegalArgumentException("Buổi này đang có một yêu cầu đổi lịch chờ duyệt");
        }

        LocalDate date = requireUpcomingDate(request.getNewDate());
        LocalTime start = request.getNewStartTime();
        LocalTime end = request.getNewEndTime();
        requireTimeRange(start, end);
        // Báo trùng lịch ngay lúc gửi cho đỡ mất công chờ duyệt; lúc duyệt sẽ kiểm lại.
        requireSlotFree(lesson.getTutoringClass(), lesson.getTutor(), date, start, end, lessonId);

        LessonRescheduleRequest row = new LessonRescheduleRequest();
        row.setTutoringClass(lesson.getTutoringClass());
        row.setLesson(lesson);
        row.setRequestType(RescheduleRequestType.RESCHEDULE);
        row.setNewDate(date);
        row.setNewStartTime(start);
        row.setNewEndTime(end);
        row.setReason(trimToNull(request.getReason()));
        row.setRequestedBy(me);
        return toRescheduleResponse(rescheduleRequestRepository.save(row), me);
    }

    @Override
    @Transactional
    public RescheduleRequestResponse requestExtraLesson(ExtraLessonRequest request) {
        User me = requireUser();
        if (request.getClassId() == null) {
            throw new IllegalArgumentException("Thiếu lớp cần thêm buổi");
        }
        TutoringClass tutoringClass = tutoringClassRepository
                .findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        requireClassParticipant(tutoringClass, me);

        // Buổi thêm phải gắn với gia sư đang dạy — lấy từ chính lịch đã sinh của lớp.
        Tutor tutor = activeTutorOf(tutoringClass);

        LocalDate date = requireUpcomingDate(request.getLessonDate());
        LocalTime start = request.getStartTime();
        LocalTime end = request.getEndTime();
        requireTimeRange(start, end);
        requireSlotFree(tutoringClass, tutor, date, start, end, null);

        LessonRescheduleRequest row = new LessonRescheduleRequest();
        row.setTutoringClass(tutoringClass);
        row.setRequestType(RescheduleRequestType.EXTRA);
        row.setNewDate(date);
        row.setNewStartTime(start);
        row.setNewEndTime(end);
        row.setSubject(resolveSubject(request.getSubjectId()));
        row.setReason(trimToNull(request.getReason()));
        row.setRequestedBy(me);
        return toRescheduleResponse(rescheduleRequestRepository.save(row), me);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleRequestResponse> listMyRescheduleRequests() {
        User me = requireUser();
        Set<Long> classIds = myClassIds();
        if (classIds.isEmpty()) {
            return List.of();
        }
        return rescheduleRequestRepository.findByTutoringClass_ClassIdInOrderByCreatedAtDesc(classIds).stream()
                .map(row -> toRescheduleResponse(row, me))
                .toList();
    }

    @Override
    @Transactional
    public void decideRescheduleRequest(Long requestId, RescheduleDecisionRequest decision) {
        User me = requireUser();
        LessonRescheduleRequest row = rescheduleRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        requireClassParticipant(row.getTutoringClass(), me);

        if (row.getStatus() != RescheduleRequestStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý trước đó");
        }
        // Người gửi không tự duyệt được — phải là bên còn lại.
        if (row.getRequestedBy().getUserId().equals(me.getUserId())) {
            throw new ForbiddenException("Bên còn lại mới là người duyệt yêu cầu này");
        }
        if (decision == null || decision.getApprove() == null) {
            throw new IllegalArgumentException("Thiếu quyết định duyệt hay từ chối");
        }

        if (decision.getApprove()) {
            applyRescheduleRequest(row);
            row.setStatus(RescheduleRequestStatus.APPROVED);
        } else {
            row.setStatus(RescheduleRequestStatus.REJECTED);
        }
        row.setDecidedBy(me);
        row.setDecidedAt(LocalDateTime.now());
        row.setDecisionNote(trimToNull(decision.getNote()));
        rescheduleRequestRepository.save(row);
    }

    @Override
    @Transactional
    public void cancelRescheduleRequest(Long requestId) {
        User me = requireUser();
        LessonRescheduleRequest row = rescheduleRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (!row.getRequestedBy().getUserId().equals(me.getUserId())) {
            throw new ForbiddenException("Chỉ người gửi mới thu hồi được yêu cầu");
        }
        if (row.getStatus() != RescheduleRequestStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý, không thu hồi được");
        }
        row.setStatus(RescheduleRequestStatus.CANCELLED);
        rescheduleRequestRepository.save(row);
    }

    /** Áp lịch mới vào bảng {@code lessons}. Chỉ chạy khi yêu cầu được duyệt. */
    private void applyRescheduleRequest(LessonRescheduleRequest row) {
        TutoringClass tutoringClass = row.getTutoringClass();
        LocalDate date = row.getNewDate();
        LocalTime start = row.getNewStartTime();
        LocalTime end = row.getNewEndTime();

        if (row.getRequestType() == RescheduleRequestType.RESCHEDULE) {
            Lesson lesson = row.getLesson();
            if (lesson == null) {
                throw new IllegalArgumentException("Yêu cầu đổi lịch không gắn với buổi nào");
            }
            // Trạng thái có thể đã đổi trong lúc chờ duyệt — kiểm lại trước khi áp.
            if (lesson.getAttendanceStatus() != AttendanceStatus.PENDING) {
                throw new IllegalArgumentException(
                        "Buổi này đã điểm danh trong lúc chờ duyệt nên không đổi lịch được nữa");
            }
            requireSlotFree(tutoringClass, lesson.getTutor(), date, start, end, lesson.getLessonId());
            lesson.setSlot(resolveSlot(tutoringClass, date, start, end, lesson.getSlot().getSubject()));
            lesson.setLessonDate(date);
            lessonRepository.save(lesson);
        } else {
            Tutor tutor = activeTutorOf(tutoringClass);
            requireSlotFree(tutoringClass, tutor, date, start, end, null);
            Lesson lesson = new Lesson();
            lesson.setTutoringClass(tutoringClass);
            lesson.setTutor(tutor);
            lesson.setLessonDate(date);
            lesson.setSlot(resolveSlot(tutoringClass, date, start, end, row.getSubject()));
            lesson.setAttendanceStatus(AttendanceStatus.PENDING);
            lesson.setSequenceNo(0); // sẽ được đánh lại ngay bên dưới
            lessonRepository.save(lesson);
        }
        resequenceLessons(tutoringClass.getClassId());
    }

    /**
     * Dùng lại schedule_slot có sẵn nếu khớp (thứ, giờ, môn); không thì tạo mới.
     * Tránh sinh slot rác mỗi lần đổi lịch sang khung đã tồn tại.
     */
    private ScheduleSlot resolveSlot(
            TutoringClass tutoringClass, LocalDate date, LocalTime start, LocalTime end, Subject subject) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        Long subjectId = subject != null ? subject.getSubjectId() : null;
        for (ScheduleSlot slot : scheduleSlotRepository.findByTutoringClass_ClassId(tutoringClass.getClassId())) {
            Long slotSubjectId = slot.getSubject() != null ? slot.getSubject().getSubjectId() : null;
            if (slot.getDayOfWeek() == dayOfWeek
                    && start.equals(slot.getStartTime())
                    && end.equals(slot.getEndTime())
                    && Objects.equals(subjectId, slotSubjectId)) {
                return slot;
            }
        }
        ScheduleSlot slot = new ScheduleSlot();
        slot.setTutoringClass(tutoringClass);
        slot.setSubject(subject);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return scheduleSlotRepository.save(slot);
    }

    /** Sau khi dời/thêm buổi, sequence_no phải chạy lại theo đúng thứ tự học. */
    private void resequenceLessons(Long classId) {
        List<Lesson> lessons =
                new ArrayList<>(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(classId));
        assignSequenceNumbers(lessons);
        lessonRepository.saveAll(lessons);
    }

    /** sequence_no đánh theo đúng thứ tự học: ngày rồi tới giờ bắt đầu. Sửa tại chỗ. */
    private void assignSequenceNumbers(List<Lesson> lessons) {
        lessons.sort(Comparator.comparing(Lesson::getLessonDate)
                .thenComparing(l -> l.getSlot().getStartTime()));
        int seq = 1;
        for (Lesson lesson : lessons) {
            lesson.setSequenceNo(seq++);
        }
    }

    /**
     * Một buổi mới không được đè lên buổi nào khác của phụ huynh hoặc của gia sư.
     * Bỏ qua chính buổi đang dời — nên vẫn bắt được va chạm với buổi khác trong cùng lớp.
     */
    private void requireSlotFree(
            TutoringClass tutoringClass,
            Tutor tutor,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            Long excludeLessonId) {
        for (Lesson lesson : busyLessonsOf(tutoringClass, tutor)) {
            if (lesson.getLessonId().equals(excludeLessonId) || !date.equals(lesson.getLessonDate())) {
                continue;
            }
            LocalTime otherStart = lesson.getSlot().getStartTime();
            LocalTime otherEnd = lesson.getSlot().getEndTime();
            if (overlaps(start, end, otherStart, otherEnd)) {
                throw new IllegalArgumentException("Khung giờ này trùng với buổi \""
                        + lesson.getTutoringClass().getTitle() + "\" ngày " + date + " ("
                        + otherStart + "–" + otherEnd + "). Vui lòng chọn giờ khác.");
            }
        }
    }

    /**
     * Mọi buổi đã chiếm chỗ của hai bên: một người không thể ở hai lớp cùng lúc, nên xét cả
     * lịch của phụ huynh (các lớp họ tạo) lẫn lịch của gia sư (các lớp họ dạy).
     */
    private Collection<Lesson> busyLessonsOf(TutoringClass tutoringClass, Tutor tutor) {
        Map<Long, Lesson> existing = new LinkedHashMap<>();
        for (Lesson lesson : lessonRepository.findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(
                tutoringClass.getCreator().getUserId())) {
            existing.put(lesson.getLessonId(), lesson);
        }
        for (Lesson lesson :
                lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(tutor.getTutorId())) {
            existing.put(lesson.getLessonId(), lesson);
        }
        return existing.values();
    }

    /** Hai khoảng [start, end) giao nhau khi start < otherEnd và otherStart < end. */
    private boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    /** Gia sư đang dạy lớp — suy từ lịch đã sinh, nên chỉ có sau khi gia sư nhận lớp. */
    private Tutor activeTutorOf(TutoringClass tutoringClass) {
        return lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(tutoringClass.getClassId())
                .stream()
                .findFirst()
                .map(Lesson::getTutor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lớp chưa có lịch dạy — cần gia sư nhận lớp trước khi thêm buổi"));
    }

    /** Chỉ phụ huynh tạo lớp và gia sư đang dạy lớp mới được đụng vào lịch của lớp đó. */
    private void requireClassParticipant(TutoringClass tutoringClass, User me) {
        if (tutoringClass.getCreator().getUserId().equals(me.getUserId())) {
            return;
        }
        boolean isTutorOfClass = lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(tutoringClass.getClassId())
                .stream()
                .anyMatch(l -> l.getTutor().getUser().getUserId().equals(me.getUserId()));
        if (!isTutorOfClass) {
            throw new ForbiddenException("Không có quyền thay đổi lịch của lớp này");
        }
    }

    /**
     * Cùng một thời khóa biểu: gia sư nhìn theo lớp mình dạy, Client theo lớp mình tạo.
     */
    private List<Lesson> myLessons() {
        return isClient()
                ? lessonRepository.findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(
                        authHelper.currentUserId())
                : lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(
                        requireTutor().getTutorId());
    }

    /** Các lớp mà người đang đăng nhập tham gia. */
    private Set<Long> myClassIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (Lesson lesson : myLessons()) {
            ids.add(lesson.getTutoringClass().getClassId());
        }
        return ids;
    }

    private LocalDate requireUpcomingDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Thiếu ngày học mới");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể xếp buổi học vào ngày đã qua");
        }
        return date;
    }

    /** Kiểm tra cả hai đầu giờ trong một lần — luôn đi cùng nhau ở mọi chỗ gọi. */
    private void requireTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thiếu giờ bắt đầu hoặc giờ kết thúc");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    private RescheduleRequestResponse toRescheduleResponse(LessonRescheduleRequest row, User me) {
        Lesson lesson = row.getLesson();
        boolean pending = row.getStatus() == RescheduleRequestStatus.PENDING;
        boolean mine = row.getRequestedBy().getUserId().equals(me.getUserId());
        return RescheduleRequestResponse.builder()
                .requestId(row.getRequestId())
                .classId(row.getTutoringClass().getClassId())
                .classTitle(row.getTutoringClass().getTitle())
                .requestType(row.getRequestType().name())
                .status(row.getStatus().name())
                .lessonId(lesson != null ? lesson.getLessonId() : null)
                .oldDate(lesson != null ? lesson.getLessonDate() : null)
                .oldStartTime(lesson != null ? lesson.getSlot().getStartTime() : null)
                .oldEndTime(lesson != null ? lesson.getSlot().getEndTime() : null)
                .newDate(row.getNewDate())
                .newStartTime(row.getNewStartTime())
                .newEndTime(row.getNewEndTime())
                .subjectName(subjectNameOf(row, lesson))
                .reason(row.getReason())
                .requestedByName(displayNameOf(row.getRequestedBy()))
                .createdAt(row.getCreatedAt())
                .decidedByName(row.getDecidedBy() != null ? displayNameOf(row.getDecidedBy()) : null)
                .decidedAt(row.getDecidedAt())
                .decisionNote(row.getDecisionNote())
                .canDecide(pending && !mine)
                .canCancel(pending && mine)
                .build();
    }

    /** Gia sư có tên hồ sơ; phụ huynh thì chỉ có email (giống chỗ dựng AssignmentResponse). */
    private String displayNameOf(User user) {
        return tutorRepository
                .findByUser_UserId(user.getUserId())
                .map(Tutor::getFullName)
                .filter(StringUtils::hasText)
                .orElseGet(user::getEmail);
    }

    private String subjectNameOf(LessonRescheduleRequest row, Lesson lesson) {
        if (row.getSubject() != null) {
            return row.getSubject().getSubjectName();
        }
        if (lesson != null && lesson.getSlot().getSubject() != null) {
            return lesson.getSlot().getSubject().getSubjectName();
        }
        return null;
    }

    private ClassAssignment requireMyPendingAssignment(Long assignmentId) {
        Tutor tutor = requireTutor();
        ClassAssignment assignment = classAssignmentRepository
                .findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời nhận lớp"));
        if (!assignment.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new ForbiddenException("Không có quyền xử lý lời mời của gia sư khác");
        }
        if (assignment.getApplication() == null) {
            throw new IllegalArgumentException("Lời mời không gắn với lớp nào");
        }
        if (assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời này đã được xử lý trước đó");
        }
        return assignment;
    }

    private Lesson requireMyLesson(Long lessonId) {
        Tutor tutor = requireTutor();
        Lesson lesson = lessonRepository
                .findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
        if (!lesson.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new ForbiddenException("Không có quyền điểm danh buổi học của gia sư khác");
        }
        return lesson;
    }

    // --- Sinh lịch dạy ---------------------------------------------------------------

    /**
     * Bung lịch trong detailsJson thành từng buổi CÓ NGÀY CỤ THỂ.
     * schedule_slots chưa bao giờ được ghi nên phải sinh cả slot lẫn lesson tại đây.
     */
    private void generateSchedule(TutoringClass tutoringClass, Tutor tutor) {
        if (lessonRepository.countByTutoringClass_ClassId(tutoringClass.getClassId()) > 0) {
            return; // đã sinh rồi (nhận lại lớp) — không nhân đôi lịch
        }
        JsonNode form = readTree(tutoringClass.getDetailsJson());
        List<SlotSpec> specs = slotSpecs(form);
        if (specs.isEmpty() || tutoringClass.getStartDate() == null || tutoringClass.getEndDate() == null) {
            return; // lớp cũ không có lịch trong JSON → không sinh buổi nào
        }

        List<Map.Entry<LocalDate, SlotSpec>> occurrences = expandOccurrences(form, specs, tutoringClass);
        // Không cho nhận lớp nếu bất kỳ buổi nào trùng giờ với buổi đã có (lớp khác của
        // cùng phụ huynh hoặc của cùng gia sư) — @Transactional nên throw sẽ rollback sạch.
        requireNoScheduleConflict(tutoringClass, tutor, occurrences);

        // Mỗi (thứ, giờ, môn) khác nhau = 1 schedule_slot dùng lại cho mọi tuần.
        Map<SlotSpec, ScheduleSlot> slotRows = new LinkedHashMap<>();
        for (SlotSpec spec : specs) {
            slotRows.computeIfAbsent(spec, s -> {
                ScheduleSlot row = new ScheduleSlot();
                row.setTutoringClass(tutoringClass);
                row.setSubject(s.subjectId() != null ? subjectRepository.findById(s.subjectId()).orElse(null) : null);
                row.setDayOfWeek(s.dayOfWeek());
                row.setStartTime(s.start());
                row.setEndTime(s.end());
                return scheduleSlotRepository.save(row);
            });
        }

        List<Lesson> lessons = new ArrayList<>();
        for (Map.Entry<LocalDate, SlotSpec> occurrence : occurrences) {
            Lesson lesson = new Lesson();
            lesson.setTutoringClass(tutoringClass);
            lesson.setSlot(slotRows.get(occurrence.getValue()));
            lesson.setTutor(tutor);
            lesson.setLessonDate(occurrence.getKey());
            lesson.setAttendanceStatus(AttendanceStatus.PENDING);
            lessons.add(lesson);
        }
        assignSequenceNumbers(lessons);
        lessonRepository.saveAll(lessons);
    }

    /**
     * Chặn nhận lớp khi buổi mới trùng giờ với buổi đã có. Một người không thể ở hai lớp cùng lúc,
     * nên xét cả lịch của phụ huynh (các lớp họ tạo) lẫn lịch của gia sư (các lớp họ dạy).
     */
    private void requireNoScheduleConflict(
            TutoringClass tutoringClass, Tutor tutor, List<Map.Entry<LocalDate, SlotSpec>> occurrences) {
        Long classId = tutoringClass.getClassId();
        Collection<Lesson> existing = busyLessonsOf(tutoringClass, tutor);

        for (Map.Entry<LocalDate, SlotSpec> occurrence : occurrences) {
            LocalDate date = occurrence.getKey();
            SlotSpec spec = occurrence.getValue();
            for (Lesson lesson : existing) {
                // Loại buổi của chính lớp đang xét (thường chưa có).
                if (lesson.getTutoringClass().getClassId().equals(classId)
                        || !date.equals(lesson.getLessonDate())) {
                    continue;
                }
                LocalTime otherStart = lesson.getSlot().getStartTime();
                LocalTime otherEnd = lesson.getSlot().getEndTime();
                if (overlaps(spec.start(), spec.end(), otherStart, otherEnd)) {
                    throw new IllegalArgumentException(
                            "Lịch bị trùng với lớp \"" + lesson.getTutoringClass().getTitle() + "\" vào "
                                    + date + " (" + otherStart + "–" + otherEnd
                                    + "). Vui lòng điều chỉnh lịch trước khi nhận lớp.");
                }
            }
        }
    }

    /** Một khung học lặp lại: thứ + giờ + môn. */
    private record SlotSpec(Integer dayOfWeek, LocalTime start, LocalTime end, Long subjectId) {}

    private List<SlotSpec> slotSpecs(JsonNode form) {
        JsonNode slots = form.path("slots");
        if (!slots.isArray()) {
            return List.of();
        }
        boolean custom = "CUSTOM".equals(form.path("scheduleMode").asText("WEEKLY"));
        List<SlotSpec> specs = new ArrayList<>();
        for (JsonNode slot : slots) {
            LocalTime start = parseTime(slot.path("start").asText(""));
            LocalTime end = parseTime(slot.path("end").asText(""));
            if (start == null || end == null) {
                continue;
            }
            Integer day = custom
                    ? dayOfWeekOf(parseDate(slot.path("date").asText("")))
                    : DAY_CODE_TO_ISO.get(slot.path("day").asText(""));
            if (day == null) {
                continue;
            }
            specs.add(new SlotSpec(day, start, end, subjectIdOf(slot.path("subjectId").asText(""))));
        }
        return specs;
    }

    /**
     * WEEKLY: lặp theo tuần, bỏ tuần nghỉ (studyWeeks trong chu kỳ repeatEveryWeeks).
     * CUSTOM: mỗi slot đã có ngày sẵn.
     */
    private List<Map.Entry<LocalDate, SlotSpec>> expandOccurrences(
            JsonNode form, List<SlotSpec> specs, TutoringClass tutoringClass) {
        LocalDate startDate = tutoringClass.getStartDate();
        LocalDate endDate = tutoringClass.getEndDate();
        List<Map.Entry<LocalDate, SlotSpec>> out = new ArrayList<>();

        if ("CUSTOM".equals(form.path("scheduleMode").asText("WEEKLY"))) {
            JsonNode slots = form.path("slots");
            for (int i = 0; i < slots.size() && i < specs.size(); i++) {
                LocalDate date = parseDate(slots.get(i).path("date").asText(""));
                if (date != null && !date.isBefore(startDate) && !date.isAfter(endDate)) {
                    out.add(Map.entry(date, specs.get(i)));
                }
            }
            return out;
        }

        int cycleWeeks = Math.min(4, Math.max(1, form.path("repeatEveryWeeks").asInt(1)));
        Set<Integer> studyWeeks = studyWeeksOf(form, cycleWeeks);
        // Neo vào thứ Hai của tuần chứa startDate để tính ngày theo thứ cho gọn.
        LocalDate anchorMonday = startDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int weekIndex = 0; ; weekIndex++) {
            LocalDate weekStart = anchorMonday.plusWeeks(weekIndex);
            if (weekStart.isAfter(endDate)) {
                break;
            }
            if (!studyWeeks.contains((weekIndex % cycleWeeks) + 1)) {
                continue; // tuần nghỉ
            }
            for (SlotSpec spec : specs) {
                LocalDate date = weekStart.plusDays(spec.dayOfWeek() - 1L);
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    out.add(Map.entry(date, spec));
                }
            }
        }
        return out;
    }

    private Set<Integer> studyWeeksOf(JsonNode form, int cycleWeeks) {
        Set<Integer> weeks = new LinkedHashSet<>();
        for (JsonNode w : form.path("studyWeeks")) {
            int v = w.asInt(0);
            if (v >= 1 && v <= cycleWeeks) {
                weeks.add(v);
            }
        }
        if (weeks.isEmpty()) {
            weeks.add(1); // lớp cũ chưa có studyWeeks → học tuần đầu mỗi chu kỳ
        }
        return weeks;
    }

    private Long subjectIdOf(String key) {
        if (!StringUtils.hasText(key) || OTHER_SUBJECT_KEY.equals(key)) {
            return null;
        }
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer dayOfWeekOf(LocalDate date) {
        return date != null ? date.getDayOfWeek().getValue() : null;
    }

    private LocalTime parseTime(String value) {
        try {
            return StringUtils.hasText(value) ? LocalTime.parse(value) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private JsonNode readTree(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private AssignmentResponse toAssignment(ClassAssignment assignment) {
        TutoringClass c = assignment.getApplication().getTutoringClass();
        return AssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .classId(c.getClassId())
                .classTitle(c.getTitle())
                .clientName(c.getCreator().getEmail())
                .tutorName(assignment.getTutor().getFullName())
                .status(assignment.getStatus().name())
                .assignedDate(assignment.getAssignedDate())
                .subjectNames(subjectNamesFromJson(c.getDetailsJson()))
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .address(c.getAddress())
                .lessonMode(c.getLessonMode().name())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .lessonCount(lessonRepository.countByTutoringClass_ClassId(c.getClassId()))
                .build();
    }

    private LessonResponse toLesson(Lesson lesson, LocalDate today) {
        ScheduleSlot slot = lesson.getSlot();
        return LessonResponse.builder()
                .lessonId(lesson.getLessonId())
                .classId(lesson.getTutoringClass().getClassId())
                .classTitle(lesson.getTutoringClass().getTitle())
                .sequenceNo(lesson.getSequenceNo())
                .lessonDate(lesson.getLessonDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .subjectId(slot.getSubject() != null ? slot.getSubject().getSubjectId() : null)
                .subjectName(slot.getSubject() != null ? slot.getSubject().getSubjectName() : null)
                .attendanceStatus(lesson.getAttendanceStatus().name())
                .tutorCheckInAt(lesson.getTutorCheckInAt())
                .tutorCheckOutAt(lesson.getTutorCheckOutAt())
                .canCheckInToday(today.equals(lesson.getLessonDate()))
                .build();
    }

    /** Lấy lớp và đảm bảo người gọi là Client đã tạo lớp đó. */
    private TutoringClass requireOwnedClass(Long classId) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền xem ứng viên của lớp này");
        }
        return tutoringClass;
    }

    private ApplicantResponse toApplicant(TutorApplication app, TutoringClass tutoringClass) {
        Tutor tutor = app.getTutor();
        int score = aiMatchScore(app, tutor, tutoringClass);
        return ApplicantResponse.builder()
                .applicationId(app.getApplicationId())
                .tutorId(tutor.getTutorId())
                .userId(tutor.getUser().getUserId())
                .fullName(tutor.getFullName())
                .avatar(tutor.getAvatar())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(tutor.getVerificationStatus().name())
                .proposedRate(app.getProposedRate())
                .proposedRates(readRates(app.getProposedRatesJson()))
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus().name())
                .appliedAt(app.getAppliedAt())
                .matchScore(score)
                .recommended(false)
                .build();
    }

    /** Đơn cũ (trước khi có báo giá theo môn) không có JSON → trả null, client hiện mức chung. */
    private Map<String, BigDecimal> readRates(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, BigDecimal>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Điểm gợi ý AI 0–100 cho một ứng viên, tổng hợp có trọng số:
     * đánh giá (40%), kinh nghiệm (25%), mức phí phù hợp ngân sách (20%), đã xác minh hồ sơ (15%).
     */
    private int aiMatchScore(TutorApplication app, Tutor tutor, TutoringClass tutoringClass) {
        double rating = tutor.getRatingAvg() != null ? tutor.getRatingAvg().doubleValue() / 5.0 : 0;
        double experience = Math.min((tutor.getExperienceYears() != null ? tutor.getExperienceYears() : 0) / 10.0, 1.0);
        double verified =
                switch (tutor.getVerificationStatus()) {
                    case VERIFIED -> 1.0;
                    case UNDER_VERIFY -> 0.5;
                    case REJECTED -> 0.0;
                };
        double priceFit = priceFit(app, tutor, tutoringClass);
        double total = 0.40 * clamp01(rating) + 0.25 * experience + 0.20 * priceFit + 0.15 * verified;
        return (int) Math.round(clamp01(total) * 100);
    }

    /** Mức phí gia sư đề xuất so với học phí Client mong muốn: đạt/thấp hơn = 1; cao hơn giảm dần. */
    private double priceFit(TutorApplication app, Tutor tutor, TutoringClass tutoringClass) {
        BigDecimal expected = tutoringClass.getTuitionFee();
        BigDecimal rate = app.getProposedRate() != null ? app.getProposedRate() : tutor.getHourlyRate();
        if (expected == null || expected.signum() <= 0 || rate == null || rate.signum() <= 0) {
            return 0.7; // thiếu dữ liệu giá → trung tính
        }
        if (rate.compareTo(expected) <= 0) {
            return 1.0;
        }
        return clamp01(expected.doubleValue() / rate.doubleValue());
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
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
            if (tutorApplicationRepository
                    .existsByTutoringClass_ClassIdAndTutor_TutorId(classId, tutor.getTutorId())) {
                throw new IllegalArgumentException("Bạn đã đăng ký lớp này rồi");
            }
            TutorApplication application = new TutorApplication();
            application.setTutoringClass(tutoringClass);
            application.setTutor(tutor);
            application.setStatus(TutorApplicationStatus.SUBMITTED);
            tutorApplicationRepository.save(application);
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
            classStudentRepository.save(student);

            // Đủ sĩ số tối đa -> tự động đóng lớp thành MATCHED (không nhận thêm ghi danh).
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

    private String formatLocation(Location location) {
        if (location == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(location.getDistrictName())) {
            sb.append(location.getDistrictName());
        }
        if (location.getProvince() != null && StringUtils.hasText(location.getProvince().getProvinceName())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(location.getProvince().getProvinceName());
        }
        return sb.length() > 0 ? sb.toString() : location.getAddressLine();
    }

    private ClassResponse toClassResponse(TutoringClass c) {
        Client client = clientRepository.findByUser_UserId(c.getCreator().getUserId()).orElse(null);
        return ClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .description(c.getDescription())
                .detailsJson(c.getDetailsJson())
                .creatorId(c.getCreator().getUserId())
                .creatorName(client != null ? client.getFullName() : c.getCreator().getEmail())
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeId(c.getGrade() != null ? c.getGrade().getGradeId() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .learningGoal(c.getLearningGoal())
                .tutorRequirement(c.getTutorRequirement())
                .locationId(c.getLocation() != null ? c.getLocation().getLocationId() : null)
                .locationName(formatLocation(c.getLocation()))
                .address(c.getAddress())
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
                .applicationCount(tutorApplicationRepository.countByTutoringClass_ClassId(c.getClassId()))
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
