package com.tcs.module.marketplace.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.exception.VerificationRequiredException;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.ExtraLessonRequest;
import com.tcs.module.marketplace.dto.request.RescheduleDecisionRequest;
import com.tcs.module.marketplace.dto.request.RescheduleLessonRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.AssignmentResponse;
import com.tcs.module.marketplace.dto.response.ContractViewResponse;
import com.tcs.module.marketplace.dto.response.CenterSummaryResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.LessonResponse;
import com.tcs.module.marketplace.dto.response.RescheduleRequestResponse;
import com.tcs.module.marketplace.dto.response.ScheduleSlotResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonRescheduleRequest;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.enums.RescheduleRequestStatus;
import com.tcs.module.marketplace.enums.RescheduleRequestType;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.LessonRescheduleRequestRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.MarketplaceService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.module.contract.service.ContractService;
import org.springframework.context.event.EventListener;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.response.CenterSummaryResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceServiceImpl implements MarketplaceService {

    private static final String ESCROW_BANK_NAME = "TPBank";
    private static final String ESCROW_BANK_BIN = "970423";
    private static final String ESCROW_ACCOUNT_NUMBER = "02660559201";
    private static final String ESCROW_ACCOUNT_NAME = "TUTOR CONNECT SYSTEM";

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CccdService cccdService;
    private final ClientLegalAccountService clientLegalAccountService;
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
    private final LessonRescheduleRequestRepository rescheduleRequestRepository;
    private final com.tcs.module.messaging.repository.NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClassRequestStore classRequestStore;
    private final ContractService contractService;
    private final EmailOtpRepository emailOtpRepository;
    private final com.tcs.module.notification.service.EmailService contractEmailService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final SecureRandom SIGN_OTP_RANDOM = new SecureRandom();
    private static final int SIGN_OTP_EXPIRE_SECONDS = 30;
    private static final int SIGN_OTP_MAX_ATTEMPTS = 5;
    private static final int SIGN_OTP_LOCK_MINUTES = 5;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private static final Map<String, Integer> DAY_CODE_TO_ISO = Map.of(
            "T2", 1, "T3", 2, "T4", 3, "T5", 4, "T6", 5, "T7", 6, "CN", 7);

    private static final BigDecimal MIN_RATE_PER_HOUR = BigDecimal.valueOf(50_000);

    private static final String OTHER_SUBJECT_KEY = "other";

    private static final int TITLE_MAX_LENGTH = 150;

    private static final Pattern GRADE_NUMBER_PATTERN =
            Pattern.compile("^Lớp\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

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
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        auditLogService.record(creator.getUserId(), "CREATE_CLASS", "TutoringClass", saved.getClassId(), null, request);
        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long classId, CreateClassRequest request) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền sửa lớp này");
        }
        boolean isDraft = tutoringClass.getStatus() == TutoringClassStatus.DRAFT;
        boolean isOpen = tutoringClass.getStatus() == TutoringClassStatus.OPEN;
        long applicationCount = tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                classId, TutorApplicationStatus.REJECTED);
        if (!isDraft && !(isOpen && applicationCount == 0)) {
            throw new IllegalArgumentException(
                    applicationCount > 0
                            ? "Lớp đã có gia sư ứng tuyển nên không thể sửa nữa"
                            : "Lớp ở trạng thái này không thể sửa");
        }
        if (request.getSubjectId() == null && !StringUtils.hasText(request.getDetailsJson())) {
            throw new IllegalArgumentException("Vui lòng chọn môn học");
        }
        applyRequest(tutoringClass, request);
        if (request.getBudget() != null) tutoringClass.setBudget(request.getBudget());
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

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

    private String resolveTitle(CreateClassRequest request, Subject subject, Grade grade) {
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle().trim();
        }
        return autoTitle(request.getDetailsJson(), subject, grade);
    }

    private String autoTitle(String detailsJson, Subject subject, Grade grade) {
        List<String> names = subjectNamesFromJson(detailsJson);
        if (names.isEmpty() && subject != null) {
            names = List.of(subject.getSubjectName());
        }
        StringBuilder sb = new StringBuilder("Cần tìm gia sư");
        if (!names.isEmpty()) {
            sb.append(" môn ").append(String.join(", ", names));
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
    public ClassResponse unpublishClass(Long classId) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền gỡ đăng lớp này");
        }
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Chỉ có thể gỡ đăng lớp đang mở");
        }
        long applicationCount = tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                classId, TutorApplicationStatus.REJECTED);
        if (applicationCount > 0) {
            throw new IllegalArgumentException("Lớp đã có gia sư ứng tuyển nên không thể gỡ đăng");
        }
        tutoringClass.setStatus(TutoringClassStatus.DRAFT);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
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
        TutorApplication existing = tutorApplicationRepository
                .findFirstByTutoringClass_ClassIdAndTutor_TutorId(classId, tutor.getTutorId())
                .orElse(null);
        if (existing != null && existing.getStatus() != TutorApplicationStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Bạn đã ứng tuyển lớp này rồi. Mỗi lớp chỉ nộp được một đơn.");
        }
        Map<String, BigDecimal> rates = resolveProposedRates(request, tutoringClass);

        TutorApplication application = existing != null ? existing : new TutorApplication();
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setProposedRatesJson(rates.isEmpty() ? null : writeJson(rates));
        application.setProposedRate(highestRate(rates, request.getProposedRate()));
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(TutorApplicationStatus.SUBMITTED);
        TutorApplication saved = tutorApplicationRepository.save(application);
        auditLogService.record(tutor.getUser().getUserId(), "APPLY_CLASS", "TutorApplication",
                saved.getApplicationId(), null, request);
        notifyClientNewApplication(tutoringClass, tutor);
    }

    private void notifyClientNewApplication(TutoringClass tutoringClass, Tutor tutor) {
        if (tutoringClass.getCreator() == null) {
            return;
        }
        String tutorName = tutor != null && StringUtils.hasText(tutor.getFullName())
                ? tutor.getFullName()
                : "Một gia sư";
        com.tcs.module.messaging.entity.Notification notification =
                new com.tcs.module.messaging.entity.Notification();
        notification.setUser(tutoringClass.getCreator());
        notification.setType(com.tcs.module.messaging.enums.NotificationType.APPLICATION);
        notification.setTitle("Có gia sư ứng tuyển");
        notification.setContent(
                tutorName + " vừa ứng tuyển vào lớp \"" + tutoringClass.getTitle()
                        + "\". Xem chi tiết để chọn gia sư.");
        notification.setReferenceType("TUTORING_CLASS");
        notification.setReferenceId(tutoringClass.getClassId());
        notification.setStatus(com.tcs.module.messaging.enums.NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

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
                continue;
            }
            requireValidRate(rate, key);
            resolved.put(key, rate);
        }
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một môn và nhập học phí đề xuất");
        }
        return resolved;
    }

    private void requireValidRate(BigDecimal rate, String subjectKey) {
        String subject = subjectKey == null || isOtherSubjectKey(subjectKey)
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

    private List<String> classSubjectKeys(TutoringClass tutoringClass) {
        List<String> keys = subjectKeysFromJson(tutoringClass.getDetailsJson());
        if (!keys.isEmpty()) {
            return keys;
        }
        return tutoringClass.getSubject() != null
                ? List.of(String.valueOf(tutoringClass.getSubject().getSubjectId()))
                : List.of();
    }

    private String gradeSuffix(String gradeName) {
        String name = gradeName.trim();
        Matcher m = GRADE_NUMBER_PATTERN.matcher(name);
        return m.matches() ? " lớp " + m.group(1) : " - " + name;
    }

    private List<String> subjectNamesFromJson(String detailsJson) {
        List<String> keys = subjectKeysFromJson(detailsJson);
        if (keys.isEmpty()) {
            return List.of();
        }
        JsonNode root = null;
        try {
            root = objectMapper.readTree(detailsJson);
        } catch (JsonProcessingException ignored) {
        }
        String legacyOther = root != null ? root.path("subjectOther").asText("").trim() : "";
        JsonNode subjectOthers = root != null ? root.path("subjectOthers") : null;
        List<String> names = new ArrayList<>();
        for (String key : keys) {
            if (isOtherSubjectKey(key)) {
                String name = "";
                if (subjectOthers != null && subjectOthers.isObject()) {
                    name = subjectOthers.path(key).asText("").trim();
                }
                if (!StringUtils.hasText(name)) {
                    name = legacyOther;
                }
                names.add(StringUtils.hasText(name) ? name : "Môn học khác");
                continue;
            }
            try {
                subjectRepository.findById(Long.valueOf(key))
                        .map(Subject::getSubjectName)
                        .ifPresent(names::add);
            } catch (NumberFormatException ignored) {
            }
        }
        return names;
    }

    private boolean isOtherSubjectKey(String key) {
        return OTHER_SUBJECT_KEY.equals(key)
                || (key != null && key.startsWith(OTHER_SUBJECT_KEY + ":"));
    }

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
                .filter(app -> app.getStatus() != TutorApplicationStatus.REJECTED)
                .map(app -> app.getTutoringClass().getClassId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicantResponse> listApplicants(Long classId) {
        TutoringClass tutoringClass = requireOwnedClass(classId);
        List<TutorApplication> applications =
                tutorApplicationRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
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
        if (!StringUtils.hasText(cccdNumberOf(tutoringClass.getCreator().getUserId()))) {
            throw new IllegalArgumentException(
                    "Bạn cần cập nhật Căn cước công dân (CCCD) trong hồ sơ trước khi chọn gia sư và lập hợp đồng.");
        }
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
        applyTutorRatesToClass(tutoringClass, chosen);
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        tutoringClassRepository.save(tutoringClass);

        ClassAssignment assignment = classAssignmentRepository
                .findByApplication_ApplicationId(applicationId)
                .orElseGet(ClassAssignment::new);
        assignment.setTutor(chosen.getTutor());
        assignment.setApplication(chosen);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        classAssignmentRepository.save(assignment);
        notifyTutorInvited(tutoringClass, chosen);
    }

    private void applyTutorRatesToClass(TutoringClass tutoringClass, TutorApplication chosen) {
        Map<String, BigDecimal> rates = readRates(chosen.getProposedRatesJson());
        if (rates == null || rates.isEmpty()) {
            if (chosen.getProposedRate() != null) {
                tutoringClass.setTuitionFee(chosen.getProposedRate());
            }
            return;
        }
        JsonNode root = readTree(tutoringClass.getDetailsJson());
        if (root != null && root.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode obj =
                    (com.fasterxml.jackson.databind.node.ObjectNode) root;
            JsonNode existingFees = obj.get("subjectFees");
            com.fasterxml.jackson.databind.node.ObjectNode fees =
                    existingFees != null && existingFees.isObject()
                            ? (com.fasterxml.jackson.databind.node.ObjectNode) existingFees
                            : objectMapper.createObjectNode();
            for (Map.Entry<String, BigDecimal> e : rates.entrySet()) {
                fees.put(e.getKey(), e.getValue().toPlainString());
            }
            obj.set("subjectFees", fees);
            try {
                tutoringClass.setDetailsJson(objectMapper.writeValueAsString(obj));
            } catch (JsonProcessingException ignored) {
            }
        }
        tutoringClass.setTuitionFee(highestRate(rates, chosen.getProposedRate()));
    }

    /**
     * Tập môn gia sư THỰC SỰ nhận dạy của một phân công (theo giá đề xuất trong đơn).
     * Trả về {@code null} khi gia sư nhận tất cả môn (không lọc) — giữ nguyên lớp.
     */
    private java.util.Set<String> acceptedSubjectKeys(ClassAssignment assignment) {
        if (assignment == null || assignment.getApplication() == null) {
            return null;
        }
        Map<String, BigDecimal> rates = readRates(assignment.getApplication().getProposedRatesJson());
        if (rates == null || rates.isEmpty()) {
            return null;
        }
        return rates.keySet();
    }

    /**
     * Lọc detailsJson của LỚP về đúng tập môn gia sư nhận dạy (không sửa dữ liệu lớp gốc, để
     * còn mở lại cho gia sư khác nếu huỷ chọn). Dùng cho hợp đồng và sinh lịch. Nếu keptKeys
     * rỗng/null thì trả nguyên bản (gia sư nhận tất cả môn).
     */
    private String filterDetailsToSubjects(String detailsJson, java.util.Set<String> keptKeys) {
        if (keptKeys == null || keptKeys.isEmpty() || !StringUtils.hasText(detailsJson)) {
            return detailsJson;
        }
        JsonNode root = readTree(detailsJson);
        if (root == null || !root.isObject()) {
            return detailsJson;
        }
        com.fasterxml.jackson.databind.node.ObjectNode obj =
                (com.fasterxml.jackson.databind.node.ObjectNode) root;

        JsonNode ids = obj.get("subjectIds");
        if (ids != null && ids.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode keptIds = objectMapper.createArrayNode();
            for (JsonNode id : ids) {
                if (keptKeys.contains(id.asText())) {
                    keptIds.add(id);
                }
            }
            if (!keptIds.isEmpty()) {
                obj.set("subjectIds", keptIds);
            }
        }

        JsonNode feesNode = obj.get("subjectFees");
        if (feesNode != null && feesNode.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode fees = objectMapper.createObjectNode();
            for (String key : keptKeys) {
                if (feesNode.has(key)) {
                    fees.set(key, feesNode.get(key));
                }
            }
            obj.set("subjectFees", fees);
        }

        JsonNode slots = obj.get("slots");
        if (slots != null && slots.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode keptSlots = objectMapper.createArrayNode();
            for (JsonNode slot : slots) {
                String slotKey = slot.path("subjectId").asText("");
                if (slotKey.isEmpty() || keptKeys.contains(slotKey)) {
                    keptSlots.add(slot);
                }
            }
            obj.set("slots", keptSlots);
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return detailsJson;
        }
    }

    private void notifyTutorInvited(TutoringClass tutoringClass, TutorApplication chosen) {
        if (chosen.getTutor() == null || chosen.getTutor().getUser() == null) {
            return;
        }
        com.tcs.module.messaging.entity.Notification notification =
                new com.tcs.module.messaging.entity.Notification();
        notification.setUser(chosen.getTutor().getUser());
        notification.setType(com.tcs.module.messaging.enums.NotificationType.APPLICATION);
        notification.setTitle("Bạn được mời nhận lớp");
        notification.setContent(
                "Bạn được chọn cho lớp \"" + tutoringClass.getTitle()
                        + "\". Vào mục Lịch dạy để bấm nhận lớp và bắt đầu lịch học.");
        notification.setReferenceType("TUTORING_CLASS");
        notification.setReferenceId(tutoringClass.getClassId());
        notification.setStatus(com.tcs.module.messaging.enums.NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void rejectApplicant(Long classId, Long applicationId, String reason) {
        TutoringClass tutoringClass = requireOwnedClass(classId);
        TutorApplication application = tutorApplicationRepository
                .findById(applicationId)
                .filter(a -> a.getTutoringClass().getClassId().equals(tutoringClass.getClassId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp không còn ở trạng thái đang mở để bỏ chọn gia sư");
        }
        if (application.getStatus() == TutorApplicationStatus.ACCEPTED) {
            throw new IllegalArgumentException("Không thể bỏ chọn gia sư đã được chọn");
        }
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do bỏ chọn gia sư");
        }
        notifyApplicantRejected(tutoringClass, application, trimmedReason);
        application.setStatus(TutorApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        tutorApplicationRepository.save(application);
    }

    private void notifyApplicantRejected(
            TutoringClass tutoringClass, TutorApplication application, String reason) {
        if (application.getTutor() == null || application.getTutor().getUser() == null) {
            return;
        }
        com.tcs.module.messaging.entity.Notification notification =
                new com.tcs.module.messaging.entity.Notification();
        notification.setUser(application.getTutor().getUser());
        notification.setType(com.tcs.module.messaging.enums.NotificationType.APPLICATION);
        notification.setTitle("Đơn ứng tuyển không được chọn");
        notification.setContent(
                "Lớp \"" + tutoringClass.getTitle() + "\" đã bỏ chọn đơn ứng tuyển của bạn. Lý do: \""
                        + reason + "\". Bạn có thể điều chỉnh điều kiện lại để ứng tuyển lại.");
        notification.setReferenceType("TUTORING_CLASS");
        notification.setReferenceId(tutoringClass.getClassId());
        notification.setStatus(com.tcs.module.messaging.enums.NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> listMyAssignments() {
        List<ClassAssignment> assignments = isClient()
                ? classAssignmentRepository.findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(
                        authHelper.currentUserId())
                : classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(
                        requireTutor().getTutorId());
        return assignments.stream()
                .filter(a -> a.getApplication() != null)
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
        if (assignment.getTutorSignedAt() == null || assignment.getClientSignedAt() == null) {
            throw new IllegalArgumentException("Vui lòng ký hợp đồng và thanh toán escrow trước khi nhận lớp");
        }
        if (escrowTransactionRepository.findByAssignment_AssignmentId(assignmentId).isEmpty()) {
            throw new IllegalArgumentException("Chưa có escrow hợp lệ cho lớp này");
        }
        activateAssignment(assignment);
    }

    private void activateAssignment(ClassAssignment assignment) {
        TutoringClass tutoringClass = assignment.getApplication().getTutoringClass();
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        classAssignmentRepository.save(assignment);
        generateSchedule(tutoringClass, assignment.getTutor(), acceptedSubjectKeys(assignment));
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClassRepository.save(tutoringClass);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractViewResponse getAssignmentContract(Long assignmentId) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời nhận lớp"));
        TutoringClass c = requireAssignmentClass(assignment);
        Contract contract = contractRepository.findByAssignment_AssignmentId(assignmentId).orElse(null);
        String role = contractRoleOf(assignment, c);
        Tutor tutor = assignment.getTutor();
        Client client = clientRepository.findByUser_UserId(c.getCreator().getUserId()).orElse(null);
        CccdInfoDto clientCccd = cccdInfoOf(c.getCreator().getUserId());
        CccdInfoDto tutorCccd = cccdInfoOf(tutor.getUser().getUserId());
        // Hợp đồng chỉ gồm môn gia sư nhận dạy (lọc theo giá đề xuất trong đơn), không sửa lớp gốc.
        String contractDetailsJson = filterDetailsToSubjects(c.getDetailsJson(), acceptedSubjectKeys(assignment));
        return ContractViewResponse.builder()
                .contractId(contract != null ? contract.getContractId() : null)
                .assignmentId(assignment.getAssignmentId())
                .classId(c.getClassId())
                .classTitle(c.getTitle())
                .detailsJson(contractDetailsJson)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .address(c.getAddress())
                .lessonMode(c.getLessonMode() != null ? c.getLessonMode().name() : null)
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .numberOfSessions(c.getNumberOfSessions() != null ? c.getNumberOfSessions() : 0)
                .subjectNames(subjectNamesFromJson(contractDetailsJson))
                .tuitionFee(c.getTuitionFee())
                .clientName(firstText(
                        clientCccd != null ? clientCccd.getFullName() : null,
                        client != null ? client.getFullName() : null,
                        c.getCreator().getEmail()))
                .clientPhone(client != null ? client.getPhone() : c.getCreator().getPhone())
                .clientAddress(firstAddress(client != null ? client.getAddress() : null, clientCccd))
                .clientDob(firstDob(client != null ? client.getDateOfBirth() : null, clientCccd))
                .clientCccd(clientCccd != null ? clientCccd.getCccdNumber() : null)
                .tutorName(firstText(
                        tutorCccd != null ? tutorCccd.getFullName() : null,
                        tutor.getFullName()))
                .tutorPhone(tutor.getPhone())
                .tutorAddress(firstAddress(tutor.getAddress(), tutorCccd))
                .tutorDob(firstDob(tutor.getDateOfBirth(), tutorCccd))
                .tutorCccd(tutorCccd != null ? tutorCccd.getCccdNumber() : null)
                .tutorSigned(assignment.getTutorSignedAt() != null)
                .clientSigned(assignment.getClientSignedAt() != null)
                .paymentMethod(assignment.getPaymentMethod())
                .myRole(role)
                .escrowPayment(toEscrowPaymentInfo(resolveAssignmentEscrow(assignment)))
                .refundPayoutInfo(toRefundPayoutInfoView(contract))
                .termsB(RefundPayoutInfoCodec.stripFromReason(assignment.getTermsB()))
                .build();
    }

    private EscrowTransaction resolveAssignmentEscrow(ClassAssignment assignment) {
        if (assignment == null || assignment.getAssignmentId() == null) {
            return null;
        }
        return escrowTransactionRepository.findByAssignment_AssignmentId(assignment.getAssignmentId()).orElse(null);
    }

    private ContractResponse.EscrowPaymentInfo toEscrowPaymentInfo(EscrowTransaction escrow) {
        if (escrow == null) {
            return null;
        }
        PaymentTransaction payment = escrow.getPayment();
        BigDecimal amount = payment != null && payment.getAmount() != null
                ? payment.getAmount()
                : escrow.getAmount();
        String reference = payment != null ? payment.getReferenceCode() : null;
        return ContractResponse.EscrowPaymentInfo.builder()
                .escrowId(escrow.getEscrowId())
                .escrowStatus(escrow.getStatus())
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .amount(amount)
                .referenceCode(reference)
                .bankName(ESCROW_BANK_NAME)
                .bankBin(ESCROW_BANK_BIN)
                .accountNumber(ESCROW_ACCOUNT_NUMBER)
                .accountName(ESCROW_ACCOUNT_NAME)
                .transferContent(reference)
                .qrUrl(buildEscrowQrUrl(amount, reference))
                .depositedAt(escrow.getDepositedAt())
                .processedAt(payment != null ? payment.getProcessedAt() : null)
                .build();
    }

    private ContractResponse.RefundPayoutInfoView toRefundPayoutInfoView(Contract contract) {
        RefundPayoutInfo payoutInfo = null;
        if (contract != null) {
            if (contract.getClassStudent() != null) {
                payoutInfo = RefundPayoutInfoCodec.parseFromReason(contract.getClassStudent().getNotes());
            } else if (contract.getAssignment() != null) {
                payoutInfo = RefundPayoutInfoCodec.parseFromReason(contract.getAssignment().getTermsB());
            }
        }
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            return null;
        }
        return ContractResponse.RefundPayoutInfoView.builder()
                .bankName(RefundPayoutInfoCodec.normalize(payoutInfo.bankName()))
                .accountNoMasked(RefundPayoutInfoCodec.maskAccountNo(payoutInfo.accountNo()))
                .accountHolderName(RefundPayoutInfoCodec.normalize(payoutInfo.accountHolderName()))
                .build();
    }

    private String buildEscrowQrUrl(BigDecimal amount, String transferContent) {
        if (amount == null || transferContent == null || transferContent.isBlank()) {
            return null;
        }
        return "https://img.vietqr.io/image/"
                + ESCROW_BANK_BIN
                + "-"
                + ESCROW_ACCOUNT_NUMBER
                + "-compact2.png"
                + "?amount="
                + amount.setScale(0, RoundingMode.DOWN).toPlainString()
                + "&addInfo="
                + urlEncode(transferContent)
                + "&accountName="
                + urlEncode(ESCROW_ACCOUNT_NAME);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public void saveContractTermsB(Long assignmentId, String termsB) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời nhận lớp"));
        TutoringClass c = requireAssignmentClass(assignment);
        if (!"CLIENT".equals(contractRoleOf(assignment, c))) {
            throw new ForbiddenException("Chỉ Bên A (phụ huynh/học sinh) mới được chỉnh điều khoản này");
        }
        if (assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Hợp đồng đã hoàn tất, không thể chỉnh điều khoản");
        }
        if (!StringUtils.hasText(cccdNumberOf(c.getCreator().getUserId()))) {
            throw new IllegalArgumentException(
                    "Bạn cần cập nhật Căn cước công dân (CCCD) trong hồ sơ trước khi chỉnh điều khoản hợp đồng.");
        }
        assignment.setTermsB(StringUtils.hasText(termsB) ? termsB.trim() : null);
        classAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void requestSignOtp(Long assignmentId) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời nhận lớp"));
        TutoringClass c = requireAssignmentClass(assignment);
        if (assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời đã được xử lý hoặc hợp đồng đã hoàn tất");
        }
        String role = contractRoleOf(assignment, c);
        if ("TUTOR".equals(role) && assignment.getClientSignedAt() == null) {
            throw new IllegalArgumentException(
                    "Bên A (phụ huynh/học sinh) phải ký hợp đồng trước. Vui lòng chờ Bên A ký.");
        }
        User signer = "TUTOR".equals(role)
                ? assignment.getTutor().getUser()
                : c.getCreator();
        if (!StringUtils.hasText(cccdNumberOf(signer.getUserId()))) {
            throw new IllegalArgumentException(
                    "Bạn cần cập nhật Căn cước công dân (CCCD) trong hồ sơ trước khi ký hợp đồng.");
        }
        String email = signer.getEmail();
        emailOtpRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.CONTRACT_SIGNING)
                .ifPresent(last -> {
                    if (last.getAttempts() >= SIGN_OTP_MAX_ATTEMPTS && last.getConsumedAt() != null) {
                        long waitSecs = Duration.between(
                                        LocalDateTime.now(),
                                        last.getConsumedAt().plusMinutes(SIGN_OTP_LOCK_MINUTES))
                                .getSeconds();
                        if (waitSecs > 0) {
                            long waitMins = (waitSecs + 59) / 60;
                            throw new IllegalArgumentException(
                                    "Bạn đã nhập sai mã quá " + SIGN_OTP_MAX_ATTEMPTS
                                            + " lần. Vui lòng đợi khoảng " + waitMins
                                            + " phút rồi mới gửi lại mã OTP.");
                        }
                    }
                });
        emailOtpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, OtpPurpose.CONTRACT_SIGNING)
                .ifPresent(prev -> {
                    prev.setConsumedAt(LocalDateTime.now());
                    emailOtpRepository.save(prev);
                });
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setCode(String.format("%06d", SIGN_OTP_RANDOM.nextInt(1_000_000)));
        otp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        otp.setExpiresAt(LocalDateTime.now().plusSeconds(SIGN_OTP_EXPIRE_SECONDS));
        otp.setAttempts(0);
        otp.setLastSentAt(LocalDateTime.now());
        emailOtpRepository.save(otp);
        try {
            contractEmailService.sendEmail(
                    email,
                    "Mã OTP ký hợp đồng - HĐ-" + c.getClassId(),
                    buildSignOtpEmailHtml(otp.getCode()));
        } catch (RuntimeException ex) {
            if (mailEnabled) {
                throw ex;
            }
            log.warn("[OTP-DEV] Khong gui duoc email OTP ({}). Ma OTP ky hop dong cho {} la: {}",
                    ex.getMessage(), email, otp.getCode());
        }
    }

    private String buildSignOtpEmailHtml(String code) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;\
                border:1px solid #e2e8f0;border-radius:16px;overflow:hidden">
                  <div style="background:#1565c0;padding:24px;text-align:center">
                    <h1 style="color:#fff;margin:0;font-size:20px">Tutor Connect System</h1>
                  </div>
                  <div style="padding:28px 24px;color:#0f172a">
                    <p style="margin:0 0 16px">Vui lòng dùng mã OTP bên dưới để xác nhận ký hợp đồng:</p>
                    <div style="text-align:center;margin:24px 0">
                      <span style="display:inline-block;font-size:32px;font-weight:700;letter-spacing:8px;\
                color:#1565c0;background:#eff6ff;padding:14px 24px;border-radius:12px">%s</span>
                    </div>
                    <p style="margin:0 0 8px;color:#dc2626;font-weight:600">Mã chỉ có hiệu lực trong 30 giây.</p>
                    <p style="margin:0;color:#64748b">Nếu hết hạn, hãy bấm "Gửi lại mã" để nhận mã mới. Không chia sẻ mã cho bất kỳ ai.</p>
                  </div>
                </div>
                """.formatted(code);
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void signAssignmentContract(Long assignmentId, String otp) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời nhận lớp"));
        TutoringClass c = requireAssignmentClass(assignment);
        if (assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời đã được xử lý hoặc hợp đồng đã hoàn tất");
        }
        String role = contractRoleOf(assignment, c);
        if ("TUTOR".equals(role) && assignment.getClientSignedAt() == null) {
            throw new IllegalArgumentException(
                    "Bên A (phụ huynh/học sinh) phải ký hợp đồng trước. Vui lòng chờ Bên A ký.");
        }
        User signer = "TUTOR".equals(role)
                ? assignment.getTutor().getUser()
                : c.getCreator();
        if (!StringUtils.hasText(cccdNumberOf(signer.getUserId()))) {
            throw new IllegalArgumentException(
                    "Bạn cần cập nhật Căn cước công dân (CCCD) trong hồ sơ trước khi ký hợp đồng.");
        }
        verifySignOtp(signer.getEmail(), otp);
        if ("TUTOR".equals(role)) {
            if (assignment.getTutorSignedAt() == null) {
                assignment.setTutorSignedAt(LocalDateTime.now());
            }
        } else {
            if (assignment.getClientSignedAt() == null) {
                assignment.setClientSignedAt(LocalDateTime.now());
            }
        }
        if (assignment.getTutorSignedAt() != null && assignment.getClientSignedAt() != null) {
            assignment.setPaymentMethod(resolvePrivatePaymentMethod(c));
            classAssignmentRepository.save(assignment);
            ensurePrivateEscrowPayment(assignment, c);
            if ("TUTOR".equals(role)) {
                notifyClientContractPaymentReady(c);
            }
        } else {
            classAssignmentRepository.save(assignment);
            if ("CLIENT".equals(role)) {
                notifyTutorContractReady(assignment, c);
            }
        }
    }

    private void notifyTutorContractReady(ClassAssignment assignment, TutoringClass c) {
        if (assignment.getTutor() == null || assignment.getTutor().getUser() == null) {
            return;
        }
        com.tcs.module.messaging.entity.Notification n =
                new com.tcs.module.messaging.entity.Notification();
        n.setUser(assignment.getTutor().getUser());
        n.setType(com.tcs.module.messaging.enums.NotificationType.APPLICATION);
        n.setTitle("Bên A đã ký hợp đồng — mời bạn ký");
        n.setContent("Phụ huynh/học sinh đã ký hợp đồng lớp \"" + c.getTitle()
                + "\". Vui lòng vào mục Lịch dạy để ký xác nhận và bắt đầu lớp.");
        n.setReferenceType("TUTORING_CLASS");
        n.setReferenceId(c.getClassId());
        n.setStatus(com.tcs.module.messaging.enums.NotificationStatus.SENT);
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    private void notifyClientContractPaymentReady(TutoringClass c) {
        if (c.getCreator() == null) {
            return;
        }
        com.tcs.module.messaging.entity.Notification n =
                new com.tcs.module.messaging.entity.Notification();
        n.setUser(c.getCreator());
        n.setType(com.tcs.module.messaging.enums.NotificationType.APPLICATION);
        n.setTitle("Hợp đồng đã hoàn tất — vui lòng thanh toán escrow");
        n.setContent("Hợp đồng lớp \"" + c.getTitle()
                + "\" đã được ký xong. Vui lòng vào mục Lịch học/Hợp đồng để quét mã thanh toán escrow.");
        n.setReferenceType("TUTORING_CLASS");
        n.setReferenceId(c.getClassId());
        n.setStatus(com.tcs.module.messaging.enums.NotificationStatus.SENT);
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    private void ensurePrivateEscrowPayment(ClassAssignment assignment, TutoringClass tutoringClass) {
        if (assignment.getAssignmentId() == null
                || tutoringClass == null
                || tutoringClass.getCreator() == null
                || tutoringClass.getCreator().getUserId() == null) {
            return;
        }
        if (escrowTransactionRepository.findByAssignment_AssignmentId(assignment.getAssignmentId()).isPresent()) {
            return;
        }
        BigDecimal amount = resolvePrivateEscrowAmount(tutoringClass, acceptedSubjectKeys(assignment));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Không xác định được số tiền escrow cần thanh toán");
        }
        escrowService.lock(new EscrowLockCommand(
                tutoringClass.getCreator().getUserId(),
                amount,
                assignment.getAssignmentId(),
                null));
    }

    private String resolvePrivatePaymentMethod(TutoringClass tutoringClass) {
        return plannedPrivateClassMonths(tutoringClass) > 1 ? "DEPOSIT_1M" : "FULL";
    }

    private BigDecimal resolvePrivateEscrowAmount(
            TutoringClass tutoringClass, java.util.Set<String> acceptedSubjectKeys) {
        // Ưu tiên: tổng học phí tính theo ĐÚNG lịch sẽ dạy và chỉ các môn gia sư nhận
        // (khớp hợp đồng). Chỉ khi không tính được mới rơi về budget/tuitionFee của lớp.
        BigDecimal totalAmount = courseFeeFromSchedule(tutoringClass, acceptedSubjectKeys);
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            totalAmount = positiveAmount(tutoringClass.getBudget());
            if (totalAmount == null && tutoringClass.getTuitionFee() != null
                    && tutoringClass.getNumberOfSessions() != null) {
                totalAmount = tutoringClass.getTuitionFee()
                        .multiply(BigDecimal.valueOf(tutoringClass.getNumberOfSessions()));
            }
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int plannedMonths = plannedPrivateClassMonths(tutoringClass);
        if (plannedMonths <= 1) {
            return totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return totalAmount.divide(BigDecimal.valueOf(plannedMonths), 2, RoundingMode.HALF_UP);
    }

    /**
     * Tổng học phí toàn khoá tính theo đúng các buổi sẽ được sinh (cùng logic với
     * {@link #generateSchedule}) và chỉ gồm môn gia sư nhận dạy. Trả {@code null} nếu
     * lớp chưa đủ dữ liệu lịch để tính (khi đó dùng cách tính dự phòng).
     */
    private BigDecimal courseFeeFromSchedule(
            TutoringClass tutoringClass, java.util.Set<String> acceptedSubjectKeys) {
        if (tutoringClass.getStartDate() == null || tutoringClass.getEndDate() == null) {
            return null;
        }
        JsonNode form = readTree(
                filterDetailsToSubjects(tutoringClass.getDetailsJson(), acceptedSubjectKeys));
        if (form == null) {
            return null;
        }
        List<SlotSpec> specs = slotSpecs(form);
        if (specs.isEmpty()) {
            return null;
        }
        List<Map.Entry<LocalDate, SlotSpec>> occurrences = expandOccurrences(form, specs, tutoringClass);
        if (occurrences.isEmpty()) {
            return null;
        }

        // Ánh xạ từng buổi (SlotSpec) -> phí/giờ theo môn (đọc theo key môn trong form.slots).
        JsonNode fees = form.path("subjectFees");
        boolean custom = "CUSTOM".equals(form.path("scheduleMode").asText("WEEKLY"));
        Map<SlotSpec, BigDecimal> ratePerHour = new java.util.HashMap<>();
        JsonNode slots = form.path("slots");
        if (slots.isArray()) {
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
                String key = slot.path("subjectId").asText("");
                SlotSpec spec = new SlotSpec(day, start, end, subjectIdOf(key));
                BigDecimal rate = fees.has(key)
                        ? new BigDecimal(fees.get(key).asText("0"))
                        : BigDecimal.ZERO;
                ratePerHour.putIfAbsent(spec, rate);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, SlotSpec> occurrence : occurrences) {
            SlotSpec spec = occurrence.getValue();
            BigDecimal rate = ratePerHour.getOrDefault(spec, BigDecimal.ZERO);
            double hours = java.time.Duration.between(spec.start(), spec.end()).toMinutes() / 60.0;
            total = total.add(rate.multiply(BigDecimal.valueOf(hours)));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int plannedPrivateClassMonths(TutoringClass tutoringClass) {
        if (tutoringClass.getStartDate() == null || tutoringClass.getEndDate() == null) {
            return 1;
        }
        if (tutoringClass.getEndDate().isBefore(tutoringClass.getStartDate())) {
            return 1;
        }
        long inclusiveDays = ChronoUnit.DAYS.between(
                tutoringClass.getStartDate(),
                tutoringClass.getEndDate().plusDays(1));
        return Math.max(1, (int) Math.ceil(inclusiveDays / 30.0));
    }

    private BigDecimal positiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
    }

    private void verifySignOtp(String email, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP đã gửi tới email của bạn.");
        }
        EmailOtp otp = emailOtpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, OtpPurpose.CONTRACT_SIGNING)
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không tồn tại. Vui lòng bấm gửi lại mã."));
        if (otp.isExpired()) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng bấm gửi lại mã.");
        }
        if (otp.getAttempts() >= SIGN_OTP_MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Bạn đã nhập sai mã quá " + SIGN_OTP_MAX_ATTEMPTS
                    + " lần. Vui lòng đợi " + SIGN_OTP_LOCK_MINUTES + " phút rồi gửi lại mã.");
        }
        if (!otp.getCode().equals(code.trim())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= SIGN_OTP_MAX_ATTEMPTS) {
                otp.setConsumedAt(LocalDateTime.now());
            }
            emailOtpRepository.save(otp);
            int remaining = SIGN_OTP_MAX_ATTEMPTS - otp.getAttempts();
            throw new IllegalArgumentException(remaining > 0
                    ? "Mã OTP không đúng. Bạn còn " + remaining + " lần thử."
                    : "Bạn đã nhập sai mã quá " + SIGN_OTP_MAX_ATTEMPTS + " lần. Vui lòng đợi "
                            + SIGN_OTP_LOCK_MINUTES + " phút rồi gửi lại mã.");
        }
        otp.setConsumedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);
    }

    private TutoringClass requireAssignmentClass(ClassAssignment assignment) {
        if (assignment.getApplication() == null) {
            throw new IllegalArgumentException("Lời mời không gắn với lớp nào");
        }
        return assignment.getApplication().getTutoringClass();
    }

    private String contractRoleOf(ClassAssignment assignment, TutoringClass c) {
        Long uid = authHelper.currentUserId();
        if (assignment.getTutor().getUser().getUserId().equals(uid)) {
            return "TUTOR";
        }
        if (c.getCreator().getUserId().equals(uid)) {
            return "CLIENT";
        }
        throw new ForbiddenException("Bạn không thuộc hợp đồng này");
    }

    private CccdInfoDto cccdInfoOf(Long userId) {
        try {
            return cccdService.getByUserId(userId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String cccdNumberOf(Long userId) {
        CccdInfoDto dto = cccdInfoOf(userId);
        return dto != null ? dto.getCccdNumber() : null;
    }

    private LocalDate firstDob(LocalDate profileDob, CccdInfoDto cccd) {
        if (profileDob != null) {
            return profileDob;
        }
        if (cccd != null && StringUtils.hasText(cccd.getDateOfBirth())) {
            try {
                return LocalDate.parse(cccd.getDateOfBirth(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String firstText(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private String firstAddress(String profileAddress, CccdInfoDto cccd) {
        if (StringUtils.hasText(profileAddress)) {
            return profileAddress;
        }
        return cccd != null ? cccd.getPermanentAddress() : null;
    }

    @Override
    @Transactional
    public void declineAssignment(Long assignmentId) {
        ClassAssignment assignment = requireMyPendingAssignment(assignmentId);
        TutoringClass tutoringClass = assignment.getApplication().getTutoringClass();

        assignment.setStatus(ClassAssignmentStatus.DECLINED);
        classAssignmentRepository.save(assignment);

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
    public void markAttendance(Long lessonId, boolean present) {
        Lesson lesson = requireMyLesson(lessonId);
        if (lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED) {
            throw new IllegalArgumentException("Buổi học này đã điểm danh xong");
        }
        requireLessonIsToday(lesson);
        if (present) {
            LocalDateTime now = LocalDateTime.now();
            if (lesson.getTutorCheckInAt() == null) {
                lesson.setTutorCheckInAt(now);
            }
            lesson.setTutorCheckOutAt(now);
            lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
        } else {
            lesson.setAttendanceStatus(AttendanceStatus.ABSENT);
        }
        lessonRepository.save(lesson);
    }

    private void requireLessonIsToday(Lesson lesson) {
        LocalDate today = LocalDate.now();
        if (!today.equals(lesson.getLessonDate())) {
            throw new IllegalArgumentException(
                    "Chỉ điểm danh được trong ngày diễn ra buổi học ("
                            + lesson.getLessonDate() + "). Hôm nay là " + today + ".");
        }
    }

    private void sendClassNotification(User user, String title, String content, Long classId) {
        if (user == null) {
            return;
        }
        com.tcs.module.messaging.entity.Notification n =
                new com.tcs.module.messaging.entity.Notification();
        n.setUser(user);
        n.setType(com.tcs.module.messaging.enums.NotificationType.CLASS);
        n.setTitle(title);
        n.setContent(content);
        n.setReferenceType("TUTORING_CLASS");
        n.setReferenceId(classId);
        n.setStatus(com.tcs.module.messaging.enums.NotificationStatus.SENT);
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    private void notifyStudentEnrollmentSuccess(ClassStudent classStudent) {
        if (classStudent == null || classStudent.getEnrolledByUser() == null || classStudent.getTutoringClass() == null) {
            return;
        }
        TutoringClass tutoringClass = classStudent.getTutoringClass();
        String classTitle = StringUtils.hasText(tutoringClass.getTitle()) ? tutoringClass.getTitle() : "lớp học";
        String studentName = StringUtils.hasText(classStudent.getStudentName()) ? classStudent.getStudentName() : "Học viên";
        sendClassNotification(
                classStudent.getEnrolledByUser(),
                "Ghi danh thành công",
                studentName + " đã được ghi danh thành công vào lớp \"" + classTitle
                        + "\" sau khi hệ thống xác nhận thanh toán.",
                tutoringClass.getClassId());
    }

    private User classCounterpart(TutoringClass tc, User me) {
        User client = tc.getCreator();
        if (client != null && !client.getUserId().equals(me.getUserId())) {
            return client;
        }
        try {
            Tutor tutor = activeTutorOf(tc);
            return tutor != null ? tutor.getUser() : null;
        } catch (RuntimeException e) {
            return null;
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
        requireNotPastTimeToday(date, start);
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
        LessonRescheduleRequest saved = rescheduleRequestRepository.save(row);
        sendClassNotification(
                classCounterpart(lesson.getTutoringClass(), me),
                "Yêu cầu đổi lịch buổi học",
                "Có yêu cầu đổi lịch buổi học ở lớp \"" + lesson.getTutoringClass().getTitle()
                        + "\". Vào mục Lịch dạy để duyệt.",
                lesson.getTutoringClass().getClassId());
        return toRescheduleResponse(saved, me);
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

        Tutor tutor = activeTutorOf(tutoringClass);

        LocalDate date = requireUpcomingDate(request.getLessonDate());
        LocalTime start = request.getStartTime();
        LocalTime end = request.getEndTime();
        requireTimeRange(start, end);
        requireNotPastTimeToday(date, start);
        requireSlotFree(tutoringClass, tutor, date, start, end, null);

        LessonRescheduleRequest row = new LessonRescheduleRequest();
        row.setTutoringClass(tutoringClass);
        row.setRequestType(RescheduleRequestType.EXTRA);
        row.setNewDate(date);
        row.setNewStartTime(start);
        row.setNewEndTime(end);
        String reason = trimToNull(request.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("Vui lòng nhập lý do thêm buổi");
        }
        row.setSubject(resolveSubject(request.getSubjectId()));
        row.setReason(reason);
        row.setRequestedBy(me);
        LessonRescheduleRequest saved = rescheduleRequestRepository.save(row);
        sendClassNotification(
                classCounterpart(tutoringClass, me),
                "Yêu cầu thêm buổi học",
                "Có yêu cầu thêm buổi học ở lớp \"" + tutoringClass.getTitle()
                        + "\". Vào mục Lịch dạy để duyệt.",
                tutoringClass.getClassId());
        return toRescheduleResponse(saved, me);
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
        String kind = row.getRequestType() == RescheduleRequestType.EXTRA ? "thêm buổi" : "đổi lịch";
        String verb = decision.getApprove() ? "được duyệt" : "bị từ chối";
        String note = trimToNull(decision.getNote());
        sendClassNotification(
                row.getRequestedBy(),
                "Yêu cầu " + kind + " " + verb,
                "Yêu cầu " + kind + " ở lớp \"" + row.getTutoringClass().getTitle() + "\" đã " + verb
                        + (note != null ? ". Ghi chú: " + note : "") + ".",
                row.getTutoringClass().getClassId());
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
            lesson.setSequenceNo(0);
            lessonRepository.save(lesson);
        }
        resequenceLessons(tutoringClass.getClassId());
    }

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

    private void resequenceLessons(Long classId) {
        List<Lesson> lessons =
                new ArrayList<>(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(classId));
        assignSequenceNumbers(lessons);
        lessonRepository.saveAll(lessons);
    }

    private void assignSequenceNumbers(List<Lesson> lessons) {
        lessons.sort(Comparator.comparing(Lesson::getLessonDate)
                .thenComparing(l -> l.getSlot().getStartTime()));
        int seq = 1;
        for (Lesson lesson : lessons) {
            lesson.setSequenceNo(seq++);
        }
    }

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

    private boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    private Tutor activeTutorOf(TutoringClass tutoringClass) {
        return lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(tutoringClass.getClassId())
                .stream()
                .findFirst()
                .map(Lesson::getTutor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lớp chưa có lịch dạy — cần gia sư nhận lớp trước khi thêm buổi"));
    }

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

    private List<Lesson> myLessons() {
        return isClient()
                ? lessonRepository.findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(
                        authHelper.currentUserId())
                : lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(
                        requireTutor().getTutorId());
    }

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

    private void requireNotPastTimeToday(LocalDate date, LocalTime start) {
        if (date != null && start != null
                && date.isEqual(LocalDate.now())
                && start.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Giờ học hôm nay đã qua — chọn giờ muộn hơn");
        }
    }

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

    private void generateSchedule(
            TutoringClass tutoringClass, Tutor tutor, java.util.Set<String> acceptedSubjectKeys) {
        if (lessonRepository.countByTutoringClass_ClassId(tutoringClass.getClassId()) > 0) {
            return;
        }
        // Chỉ sinh buổi cho môn gia sư nhận dạy; lọc trên bản sao detailsJson, không sửa lớp gốc.
        JsonNode form = readTree(filterDetailsToSubjects(tutoringClass.getDetailsJson(), acceptedSubjectKeys));
        List<SlotSpec> specs = slotSpecs(form);
        if (specs.isEmpty() || tutoringClass.getStartDate() == null || tutoringClass.getEndDate() == null) {
            return;
        }

        List<Map.Entry<LocalDate, SlotSpec>> occurrences = expandOccurrences(form, specs, tutoringClass);
        requireNoScheduleConflict(tutoringClass, tutor, occurrences);

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

    private void requireNoScheduleConflict(
            TutoringClass tutoringClass, Tutor tutor, List<Map.Entry<LocalDate, SlotSpec>> occurrences) {
        Long classId = tutoringClass.getClassId();
        Collection<Lesson> existing = busyLessonsOf(tutoringClass, tutor);

        for (Map.Entry<LocalDate, SlotSpec> occurrence : occurrences) {
            LocalDate date = occurrence.getKey();
            SlotSpec spec = occurrence.getValue();
            for (Lesson lesson : existing) {
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
        LocalDate anchorMonday = startDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int weekIndex = 0; ; weekIndex++) {
            LocalDate weekStart = anchorMonday.plusWeeks(weekIndex);
            if (weekStart.isAfter(endDate)) {
                break;
            }
            if (!studyWeeks.contains((weekIndex % cycleWeeks) + 1)) {
                continue;
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
            weeks.add(1);
        }
        return weeks;
    }

    private Long subjectIdOf(String key) {
        if (!StringUtils.hasText(key) || isOtherSubjectKey(key)) {
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
                .tutorSignedAt(assignment.getTutorSignedAt())
                .clientSignedAt(assignment.getClientSignedAt())
                .paymentMethod(assignment.getPaymentMethod())
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

    private double priceFit(TutorApplication app, Tutor tutor, TutoringClass tutoringClass) {
        BigDecimal expected = tutoringClass.getTuitionFee();
        BigDecimal rate = app.getProposedRate() != null ? app.getProposedRate() : tutor.getHourlyRate();
        if (expected == null || expected.signum() <= 0 || rate == null || rate.signum() <= 0) {
            return 0.7;
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
        RefundPayoutInfo payoutInfo = validateTerminationRefundPayoutInfo(request);

        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setAssignment(target.assignment());
        termination.setClassStudent(target.classStudent());
        termination.setRequestedBy(requester);
        termination.setReason(RefundPayoutInfoCodec.appendToReason(request.getReason().trim(), payoutInfo));
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
                buildEarlyTerminationSettlementReason(request.getReason(), settlement),
                payoutInfo));

        completeTerminationTarget(target);

        tutoringClass.setStatus(TutoringClassStatus.CANCELLED);
        tutoringClassRepository.save(tutoringClass);

        return toTerminationResponse(classTerminationRequestRepository.save(termination), tutoringClass);
    }

    private RefundPayoutInfo validateTerminationRefundPayoutInfo(CreateClassTerminationRequest request) {
        RefundPayoutInfo payoutInfo = new RefundPayoutInfo(
                RefundPayoutInfoCodec.normalize(request.getBankName()),
                RefundPayoutInfoCodec.normalizeAccountNo(request.getAccountNo()),
                RefundPayoutInfoCodec.normalize(request.getAccountHolderName()));
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền");
        }
        return payoutInfo;
    }

    @Override
    @Transactional
    public String registerToClass(Long classId) {
        User user = requireUser();
        TutoringClass tutoringClass = findClass(classId);
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp chưa mở đăng ký");
        }
        Long userId = user.getUserId();

        Tutor tutor = tutorRepository.findByUser_UserId(userId).orElse(null);
        if (tutor != null) {
            // Lớp của trung tâm do chính trung tâm tự bố trí gia sư (nội bộ / tin tuyển BF-03),
            // không nhận gia sư tự đăng ký qua marketplace.
            if (tutoringClass.getClassType() == ClassType.CENTER) {
                throw new ForbiddenException(
                        "Lớp của trung tâm do trung tâm tự bố trí gia sư — gia sư không thể tự đăng ký.");
            }
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
            return "Đã gửi đơn ứng tuyển dạy lớp. Vui lòng chờ trung tâm/phụ huynh duyệt.";
        }

        Client client = clientRepository.findByUser_UserId(userId).orElse(null);
        if (client != null) {
            // #3: bắt buộc có ngày sinh để xác minh tuổi (thiếu -> không xác định được <18).
            if (client.getDateOfBirth() == null) {
                throw new IllegalArgumentException(
                        "Vui lòng cập nhật ngày sinh trong hồ sơ trước khi đăng ký lớp.");
            }
            // #1: check trùng theo CHÍNH học sinh (email tài khoản đăng ký), không theo phụ huynh
            // -> 2 con của cùng một phụ huynh vẫn đăng ký được cùng lớp.
            if (classStudentRepository
                    .existsByTutoringClass_ClassIdAndStudentEmail(classId, user.getEmail())) {
                throw new IllegalArgumentException("Bạn đã đăng ký lớp này rồi");
            }
            // #2 & #3: học sinh dưới 18 phải liên kết phụ huynh; và PHỤ HUYNH (người pháp lý) là
            // bên ký hợp đồng, không phải học sinh. resolveForClient() ném lỗi nếu minor chưa liên kết.
            ClientLegalAccountService.LegalAccountContext legal =
                    clientLegalAccountService.resolveForClient(client);
            User payer = legal.getLegalUserId().equals(userId)
                    ? user
                    : userRepository.findById(legal.getLegalUserId()).orElse(user);
            ClassStudent student = new ClassStudent();
            student.setTutoringClass(tutoringClass);
            // Người ký/chịu trách nhiệm hợp đồng: phụ huynh nếu là minor, ngược lại chính client.
            student.setEnrolledByUser(payer);
            student.setStudentName(client.getFullName()); // tên học viên thực (kể cả minor)
            student.setStudentPhone(client.getPhone());
            student.setStudentEmail(user.getEmail());
            // BF-04: CHỜ KÝ hợp đồng -> chưa chính thức vào lớp, chưa tính sĩ số.
            student.setStatus(ClassStudentStatus.PENDING_SIGNATURE);
            ClassStudent savedStudent = classStudentRepository.save(student);
            // BF-04 bước 7: sinh hợp đồng theo học viên để phụ huynh/học viên ký (OTP).
            // Ký xong chỉ mở bước thanh toán; SePay xác nhận escrow mới chuyển ENROLLED.
            contractService.generateStudentContract(savedStudent.getClassStudentId());
            auditLogService.record(userId, "REGISTER_CLASS", "ClassStudent", savedStudent.getClassStudentId(),
                    null, java.util.Map.of("classId", classId));
            // #2: thông báo đúng ngữ cảnh — minor thì phụ huynh ký thay.
            if (legal.isDelegatedToParent()) {
                return "Đã ghi nhận đăng ký. Vì bạn dưới 18 tuổi, hợp đồng đã được gửi cho phụ huynh"
                        + (legal.getLegalHolderName() != null ? " (" + legal.getLegalHolderName() + ")" : "")
                        + " ký. Sau khi ký và thanh toán được xác nhận, bạn mới chính thức vào lớp.";
            }
            return "Đã ghi nhận đăng ký. Vui lòng vào mục Hợp đồng để ký và thanh toán — khi SePay xác nhận, bạn mới chính thức vào lớp.";
        }

        throw new ForbiddenException("Chỉ gia sư hoặc phụ huynh/học viên mới đăng ký lớp");
    }

    /**
     * BF-04: ký xong hợp đồng chỉ mở bước thanh toán escrow; học viên chính thức vào lớp khi SePay xác nhận.
     */
    @EventListener
    @Transactional
    public void onStudentContractSigned(StudentContractSigned event) {
        ClassStudent cs = classStudentRepository.findById(event.classStudentId()).orElse(null);
        if (cs == null || cs.getStatus() != ClassStudentStatus.PENDING_SIGNATURE) {
            return;
        }
        auditLogService.record(
                cs.getEnrolledByUser() != null ? cs.getEnrolledByUser().getUserId() : null,
                "STUDENT_CONTRACT_SIGNED_WAIT_PAYMENT",
                "ClassStudent",
                cs.getClassStudentId(),
                null,
                java.util.Map.of("contractId", event.contractId()));
    }

    /**
     * BF-04: SePay xác nhận thanh toán escrow -> học viên mới được tính vào sĩ số.
     */
    @EventListener
    @Transactional
    public void onEscrowFunded(EscrowFunded event) {
        if (event.assignmentId() != null) {
            handlePrivateAssignmentEscrowFunded(event.assignmentId());
        }
        if (event.classStudentId() == null) {
            return;
        }
        ClassStudent cs = classStudentRepository.findById(event.classStudentId()).orElse(null);
        if (cs == null || cs.getStatus() == ClassStudentStatus.ENROLLED) {
            return;
        }
        if (cs.getStatus() != ClassStudentStatus.PENDING_SIGNATURE) {
            return;
        }
        cs.setStatus(ClassStudentStatus.ENROLLED);
        classStudentRepository.save(cs);
        notifyStudentEnrollmentSuccess(cs);

        TutoringClass cls = cs.getTutoringClass();
        Integer max = cls.getMaxStudents();
        if (max != null && max > 0) {
            long enrolled = classStudentRepository
                    .countByTutoringClass_ClassIdAndStatus(cls.getClassId(), ClassStudentStatus.ENROLLED);
            if (enrolled >= max) {
                cls.setStatus(TutoringClassStatus.MATCHED);
                tutoringClassRepository.save(cls);
            }
        }
    }

    private void handlePrivateAssignmentEscrowFunded(Long assignmentId) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || assignment.getStatus() == ClassAssignmentStatus.ACTIVE) {
            return;
        }
        if (assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            return;
        }
        if (assignment.getTutorSignedAt() == null || assignment.getClientSignedAt() == null) {
            return;
        }
        activateAssignment(assignment);
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
        // Phải nhập đủ CCCD trong hồ sơ mới được gửi yêu cầu tới trung tâm.
        if (!cccdService.isComplete(creator.getUserId())) {
            throw new IllegalArgumentException(
                    "Bạn cần nhập đầy đủ thông tin CCCD trong Hồ sơ trước khi gửi yêu cầu tới trung tâm.");
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
                request.getDesiredBudget(),
                request.getDetailsJson());
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

    // ObjectMapper riêng để đọc payload form "tìm gia sư" (có LocalDate) từ detailsJson.
    private static final com.fasterxml.jackson.databind.ObjectMapper REQUEST_DETAILS_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .configure(
                            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false);

    @Override
    @Transactional
    public ClassResponse fulfillClassRequest(String requestId, Long tutorId) {
        User creator = requireUser();
        requireClient(creator.getUserId());
        ClassRequestStore.ClassRequestData data = classRequestStore.find(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (!creator.getUserId().equals(data.clientUserId())) {
            throw new ForbiddenException("Không có quyền với yêu cầu này");
        }
        if (ClassRequestStore.STATUS_ACCEPTED.equals(data.status())) {
            throw new IllegalArgumentException("Yêu cầu này đã hoàn tất.");
        }
        if (!classRequestStore.candidatesOf(data).contains(tutorId)) {
            throw new IllegalArgumentException("Gia sư này không nằm trong danh sách trung tâm đề cử.");
        }
        if (!StringUtils.hasText(data.detailsJson())) {
            throw new IllegalArgumentException("Yêu cầu thiếu thông tin lớp để tạo.");
        }
        CreateClassRequest req;
        try {
            req = REQUEST_DETAILS_MAPPER.readValue(data.detailsJson(), CreateClassRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không đọc được thông tin yêu cầu.");
        }
        // 1) Tạo lớp private của phụ huynh từ nội dung yêu cầu (createClass owner = phụ huynh).
        ClassResponse created = createClass(req);
        Long classId = created.getClassId();
        // 2) Mở lớp để có thể chọn gia sư.
        TutoringClass cls = tutoringClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp vừa tạo"));
        cls.setStatus(TutoringClassStatus.OPEN);
        tutoringClassRepository.save(cls);
        // 3) Tạo đơn ứng tuyển cho gia sư được chọn.
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));
        TutorApplication app = new TutorApplication();
        app.setTutoringClass(cls);
        app.setTutor(tutor);
        app.setStatus(TutorApplicationStatus.SUBMITTED);
        app.setAppliedAt(LocalDateTime.now());
        TutorApplication savedApp = tutorApplicationRepository.save(app);
        // 4) Chọn gia sư -> assignment PENDING + thông báo cho gia sư (tái dùng chooseApplicant).
        chooseApplicant(classId, savedApp.getApplicationId());
        // 5) Đánh dấu yêu cầu hoàn tất.
        classRequestStore.save(
                classRequestStore.withStatus(data, ClassRequestStore.STATUS_ACCEPTED, null));
        // 6) Đóng tin tuyển dụng đã đăng cho yêu cầu này (nếu có) — module center lắng nghe.
        eventPublisher.publishEvent(
                new com.tcs.common.event.ClassRequestFulfilled(requestId));
        return getClass(classId, null, null);
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
        return toClassResponse(c, null, null);
    }

    private ClassResponse toClassResponse(TutoringClass c, Long assignmentId, Long classStudentId) {
        Client client = clientRepository.findByUser_UserId(c.getCreator().getUserId()).orElse(null);
        TerminationTarget terminationTarget = canRequestTerminationTarget(c, assignmentId, classStudentId);
        RefundPolicy refundPolicy = resolveClassRefundPolicy(c, terminationTarget);
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
                .classType(c.getClassType())
                .maxStudents(c.getMaxStudents())
                .enrolledCount(classStudentRepository
                        .countByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED))
                .canRequestTermination(terminationTarget != null)
                .refundAllowed(refundPolicy.allowed())
                .refundBlockedReason(refundPolicy.blockedReason())
                .totalSessions(refundPolicy.totalSessions())
                .completedSessions(refundPolicy.completedSessions())
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
                .applicationCount(tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                        c.getClassId(), TutorApplicationStatus.REJECTED))
                .assignmentId(classAssignmentRepository
                        .findFirstByApplication_TutoringClass_ClassIdOrderByAssignedDateDesc(c.getClassId())
                        .map(ClassAssignment::getAssignmentId)
                        .orElse(null))
                .build();
    }

    private RefundPolicy resolveClassRefundPolicy(TutoringClass tutoringClass, TerminationTarget target) {
        if (target == null) {
            return new RefundPolicy(null, null, false, null);
        }
        try {
            int total = totalSessions(tutoringClass);
            int completed = completedSessions(tutoringClass, target);
            boolean allowed = tutoringClass.getClassType() != ClassType.CENTER
                    || total <= 0
                    || completed * 2 <= total;
            return new RefundPolicy(
                    total,
                    completed,
                    allowed,
                    allowed ? null : "Lớp trung tâm đã học quá 50% số buổi nên không thể yêu cầu hoàn tiền.");
        } catch (RuntimeException e) {
            return new RefundPolicy(null, null, true, null);
        }
    }

    private ClassTerminationResponse toTerminationResponse(
            ClassTerminationRequest request,
            TutoringClass tutoringClass) {
        RefundPayoutInfo payoutInfo = RefundPayoutInfoCodec.parseFromReason(request.getReason());
        return ClassTerminationResponse.builder()
                .terminationId(request.getTerminationId())
                .classId(tutoringClass.getClassId())
                .assignmentId(request.getAssignment() != null ? request.getAssignment().getAssignmentId() : null)
                .classStudentId(request.getClassStudent() != null
                        ? request.getClassStudent().getClassStudentId()
                        : null)
                .requestedByUserId(request.getRequestedBy().getUserId())
                .reason(RefundPayoutInfoCodec.stripFromReason(request.getReason()))
                .effectiveDate(request.getEffectiveDate())
                .bankName(payoutInfo != null ? payoutInfo.bankName() : null)
                .accountNoMasked(payoutInfo != null ? RefundPayoutInfoCodec.maskAccountNo(payoutInfo.accountNo()) : null)
                .accountHolderName(payoutInfo != null ? payoutInfo.accountHolderName() : null)
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

    private record RefundPolicy(
            Integer totalSessions,
            Integer completedSessions,
            boolean allowed,
            String blockedReason) {
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
