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
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.CenterRequestFeeService;
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
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.identity.service.OtpVerifyPolicy;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
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
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.event.ClientReviewedClassEvent;
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
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceServiceImpl implements MarketplaceService {

    private static final String ESCROW_BANK_NAME = "TPBank";
    private static final String ESCROW_BANK_BIN = "970423";
    private static final String ESCROW_ACCOUNT_NUMBER = "02660559201";
    private static final String ESCROW_ACCOUNT_NAME = "TUTOR CONNECT SYSTEM";
    private static final String PRIVATE_ESCROW_REF_PREFIX = "ESCROW-A";
    /**
     * referenceType cho thông báo về hợp đồng. Tách khỏi "TUTORING_CLASS" vì chuông
     * điều hướng theo referenceType: việc cần làm ở đây là ký / thanh toán hợp đồng,
     * không phải xem lịch dạy.
     */
    private static final String CONTRACT_CONTEXT_TYPE = "CONTRACT";

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CccdService cccdService;
    private final ClientLegalAccountService clientLegalAccountService;
    private final TutorRepository tutorRepository;
    private final ContractRepository contractRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletRepository walletRepository;
    private final ReportRepository reportRepository;
    private final DisputeRepository disputeRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final EscrowService escrowService;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassStudentRepository classStudentRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final com.tcs.module.marketplace.service.RescheduleService rescheduleService;
    private final FavoriteTutorRepository favoriteTutorRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocationRepository locationRepository;
    private final LessonRescheduleRequestRepository rescheduleRequestRepository;
    private final com.tcs.module.messaging.service.NotificationDispatchService notificationDispatchService;
    private final AuditLogService auditLogService;
    private final PenaltyAccessService penaltyAccessService;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClassRequestStore classRequestStore;
    private final CenterRequestFeeService centerRequestFeeService;
    private final ContractService contractService;
    private final LessonReminderService lessonReminderService;
    private final EmailOtpRepository emailOtpRepository;
    private final OtpService otpService;
    private final com.tcs.module.notification.service.EmailService contractEmailService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Số ngày hiển thị lớp OPEN trước khi hết hạn và bị xóa. */
    private static final long CLASS_DISPLAY_DAYS = 30;

    private static final int SIGN_OTP_EXPIRE_SECONDS = 300;
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

    /**
     * Danh sách tin hiển thị ở bảng "Danh sách tin đã đăng".
     *
     * <p>Tin chỉ được gỡ xuống khi thủ tục nhận lớp đã HOÀN TẤT: hai bên ký xong hợp đồng VÀ
     * khoản cọc (học phí tháng đầu) đã vào escrow. Vì vậy ngoài lớp đang mở (OPEN), danh sách còn
     * giữ lại lớp đã chọn gia sư (MATCHED) nhưng chưa ký đủ / chưa chuyển cọc.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> listBoardClasses() {
        return tutoringClassRepository
                .findByStatusIn(List.of(TutoringClassStatus.OPEN, TutoringClassStatus.MATCHED)).stream()
                .filter(c -> c.getStatus() == TutoringClassStatus.OPEN || !handoverCompleted(c))
                .map(c -> toClassResponse(c, null, null))
                .toList();
    }

    /** Hai bên đã ký hợp đồng và tiền cọc đã nằm trong escrow hay chưa. */
    private boolean handoverCompleted(TutoringClass c) {
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdOrderByAssignedDateDesc(c.getClassId())
                .orElse(null);
        if (assignment == null
                || assignment.getTutorSignedAt() == null
                || assignment.getClientSignedAt() == null) {
            return false;
        }
        return escrowTransactionRepository
                .findByAssignment_AssignmentId(assignment.getAssignmentId())
                .filter(e -> e.getStatus() != EscrowStatus.PENDING)
                .isPresent();
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
        penaltyAccessService.requireFeature(creator.getUserId(), "CLASS_POSTING");
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
        // Thời gian hiển thị 30 ngày kể từ lúc đăng; đăng lại sẽ làm mới hạn.
        tutoringClass.setExpiresAt(java.time.LocalDateTime.now().plusDays(CLASS_DISPLAY_DAYS));
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
        tutoringClass.setExpiresAt(null); // Về nháp thì không tính hạn hiển thị nữa.
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public void applyToClass(Long classId, ApplyClassRequest request) {
        Tutor tutor = requireTutor();
        penaltyAccessService.requireFeature(tutor.getUser().getUserId(), "CLASS_APPLICATION");
        // Chặn cứng: chỉ gia sư đã được xác minh mới được ứng tuyển vào lớp.
        if (tutor.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
            throw new VerificationRequiredException(
                    "Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển vào lớp.");
        }
        requireActiveWallet(tutor.getUser().getUserId());
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

        // Không cho nộp đơn khi lịch lớp chồng giờ với buổi dạy sẵn có của chính gia sư này.
        // Chỉ tính các buổi CHƯA kết thúc — qua giờ buổi cũ là gia sư ứng tuyển lại được.
        String conflict = scheduleConflictOf(
                tutoringClass, tutor, rates.isEmpty() ? null : rates.keySet());
        if (conflict != null) {
            throw new IllegalArgumentException("Bạn đã có lịch dạy " + conflict
                    + " trùng với lịch của lớp này nên chưa thể ứng tuyển."
                    + " Sau khi buổi dạy đó kết thúc, bạn có thể ứng tuyển lại.");
        }

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
        String content = tutorName + " vừa ứng tuyển vào lớp \"" + tutoringClass.getTitle()
                + "\". Xem chi tiết để chọn gia sư.";
        notificationDispatchService.notifyUserFromTemplate(
                tutoringClass.getCreator(),
                com.tcs.module.messaging.enums.NotificationType.APPLICATION,
                "MARKETPLACE_NEW_APPLICATION",
                Map.of("tutorName", tutorName, "classTitle", tutoringClass.getTitle()),
                "Có gia sư ứng tuyển",
                content,
                "TUTORING_CLASS",
                tutoringClass.getClassId());
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
        // Chặn trùng lịch NGAY TẠI ĐÂY — lúc chưa có hợp đồng và chưa ai chuyển tiền. Nếu để lọt
        // xuống bước kích hoạt lớp (sau khi đã nạp escrow) thì lỗi sẽ làm hỏng cả giao dịch nạp tiền.
        Map<String, BigDecimal> chosenRates = readRates(chosen.getProposedRatesJson());
        String conflict = scheduleConflictOf(
                tutoringClass,
                chosen.getTutor(),
                chosenRates == null || chosenRates.isEmpty() ? null : chosenRates.keySet());
        if (conflict != null) {
            throw new IllegalArgumentException("Gia sư này đã bận dạy " + conflict
                    + ", trùng với lịch của lớp. Vui lòng chọn gia sư khác hoặc đổi lịch lớp.");
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
            List<String> selectedSubjects = classSubjectKeys(tutoringClass).stream()
                    .filter(rates::containsKey)
                    .toList();
            if (!selectedSubjects.isEmpty()) {
                com.fasterxml.jackson.databind.node.ArrayNode subjectIds = objectMapper.createArrayNode();
                for (String subjectId : selectedSubjects) {
                    subjectIds.add(subjectId);
                }
                obj.set("subjectIds", subjectIds);
            }
            com.fasterxml.jackson.databind.node.ObjectNode fees = objectMapper.createObjectNode();
            Set<String> feeKeys = selectedSubjects.isEmpty() ? rates.keySet() : new LinkedHashSet<>(selectedSubjects);
            for (String key : feeKeys) {
                BigDecimal value = rates.get(key);
                if (value == null) {
                    continue;
                }
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                fees.put(key, value.toPlainString());
            }
            if (fees.isEmpty()) {
                for (Map.Entry<String, BigDecimal> e : rates.entrySet()) {
                    if (e.getValue() == null || e.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    fees.put(e.getKey(), e.getValue().toPlainString());
                }
            }
            if (!fees.isEmpty()) {
                obj.set("subjectFees", fees);
            }
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
        String content = "Bạn được chọn cho lớp \"" + tutoringClass.getTitle()
                + "\". Vào mục Lịch dạy để bấm nhận lớp và bắt đầu lịch học.";
        notificationDispatchService.notifyUserFromTemplate(
                chosen.getTutor().getUser(),
                com.tcs.module.messaging.enums.NotificationType.APPLICATION,
                "MARKETPLACE_TUTOR_INVITED",
                Map.of("classTitle", tutoringClass.getTitle()),
                "Bạn được mời nhận lớp",
                content,
                "TUTORING_CLASS",
                tutoringClass.getClassId());
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
        String content = "Lớp \"" + tutoringClass.getTitle() + "\" đã bỏ chọn đơn ứng tuyển của bạn. Lý do: \""
                + reason + "\". Bạn có thể điều chỉnh điều kiện lại để ứng tuyển lại.";
        notificationDispatchService.notifyUserFromTemplate(
                application.getTutor().getUser(),
                com.tcs.module.messaging.enums.NotificationType.APPLICATION,
                "MARKETPLACE_APPLICATION_REJECTED",
                Map.of("classTitle", tutoringClass.getTitle(), "reason", reason),
                "Đơn ứng tuyển không được chọn",
                content,
                "TUTORING_CLASS",
                tutoringClass.getClassId());
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
        generateSchedule(tutoringClass, assignment.getTutor(), acceptedSubjectKeys(assignment));
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        classAssignmentRepository.save(assignment);
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
        BigDecimal totalTuitionAmount = resolvePrivateContractTotalAmount(c, assignment);
        BigDecimal escrowAmount = resolvePrivateEscrowAmount(c, assignment);
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
                .tuitionFee(totalTuitionAmount)
                .totalTuitionAmount(totalTuitionAmount)
                .escrowAmount(escrowAmount)
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
                .tutorSignedAt(assignment.getTutorSignedAt())
                .clientSignedAt(assignment.getClientSignedAt())
                .paymentMethod(assignment.getPaymentMethod())
                .myRole(role)
                .escrowPayment(toEscrowPaymentInfo(resolveAssignmentEscrow(assignment), resolveAssignmentEscrowPayment(assignment)))
                .refundPayoutInfo(toRefundPayoutInfoView(contract, assignment))
                .termsB(RefundPayoutInfoCodec.stripFromReason(assignment.getTermsB()))
                .build();
    }

    private EscrowTransaction resolveAssignmentEscrow(ClassAssignment assignment) {
        if (assignment == null || assignment.getAssignmentId() == null) {
            return null;
        }
        return escrowTransactionRepository.findByAssignment_AssignmentId(assignment.getAssignmentId()).orElse(null);
    }

    private PaymentTransaction resolveAssignmentEscrowPayment(ClassAssignment assignment) {
        if (assignment == null || assignment.getAssignmentId() == null) {
            return null;
        }
        return paymentTransactionRepository
                .findByReferenceCode(PRIVATE_ESCROW_REF_PREFIX + assignment.getAssignmentId())
                .orElse(null);
    }

    private ContractResponse.EscrowPaymentInfo toEscrowPaymentInfo(EscrowTransaction escrow, PaymentTransaction fallbackPayment) {
        if (escrow == null && fallbackPayment == null) {
            return null;
        }
        PaymentTransaction payment = escrow != null ? escrow.getPayment() : fallbackPayment;
        BigDecimal amount = payment != null && payment.getAmount() != null
                ? payment.getAmount()
                : escrow != null ? escrow.getAmount() : null;
        String reference = payment != null ? payment.getReferenceCode() : null;
        return ContractResponse.EscrowPaymentInfo.builder()
                .escrowId(escrow != null ? escrow.getEscrowId() : null)
                .escrowStatus(escrow != null ? escrow.getStatus() : EscrowStatus.PENDING)
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
                .depositedAt(escrow != null ? escrow.getDepositedAt() : null)
                .processedAt(payment != null ? payment.getProcessedAt() : null)
                .build();
    }

    private ContractResponse.RefundPayoutInfoView toRefundPayoutInfoView(Contract contract) {
        return toRefundPayoutInfoView(contract, null);
    }

    private ContractResponse.RefundPayoutInfoView toRefundPayoutInfoView(
            Contract contract,
            ClassAssignment fallbackAssignment) {
        RefundPayoutInfo payoutInfo = null;
        if (contract != null) {
            if (contract.getClassStudent() != null) {
                payoutInfo = RefundPayoutInfoCodec.parseFromReason(contract.getClassStudent().getNotes());
            } else if (contract.getAssignment() != null) {
                payoutInfo = RefundPayoutInfoCodec.parseFromReason(contract.getAssignment().getTermsB());
            }
        }
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo) && fallbackAssignment != null) {
            payoutInfo = RefundPayoutInfoCodec.parseFromReason(fallbackAssignment.getTermsB());
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
    public void saveAssignmentRefundPayoutInfo(Long assignmentId, SaveRefundPayoutRequest request) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời nhận lớp"));
        TutoringClass c = requireAssignmentClass(assignment);
        if (!"CLIENT".equals(contractRoleOf(assignment, c))) {
            throw new ForbiddenException("Chỉ phụ huynh/học viên mới được lưu tài khoản nhận hoàn tiền");
        }
        if (assignment.getClientSignedAt() == null || assignment.getTutorSignedAt() == null) {
            throw new IllegalArgumentException("Vui lòng hoàn tất ký hợp đồng trước khi lưu thông tin thanh toán");
        }
        if (assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Lớp đã được xử lý, không thể cập nhật thông tin thanh toán");
        }

        RefundPayoutInfo payoutInfo = new RefundPayoutInfo(
                RefundPayoutInfoCodec.normalize(request != null ? request.getBankName() : null),
                RefundPayoutInfoCodec.normalizeAccountNo(request != null ? request.getAccountNo() : null),
                RefundPayoutInfoCodec.normalize(request != null ? request.getAccountHolderName() : null));
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản");
        }

        assignment.setTermsB(RefundPayoutInfoCodec.appendToReason(assignment.getTermsB(), payoutInfo));
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
        EmailOtp otp = otpService.issue(
                email, OtpPurpose.CONTRACT_SIGNING, 6, Duration.ofSeconds(SIGN_OTP_EXPIRE_SECONDS));
        try {
            contractEmailService.sendEmail(
                    email,
                    "Mã OTP ký hợp đồng - HĐ-" + c.getClassId(),
                    buildSignOtpEmailHtml(otp.getCode()));
            if (!mailEnabled) {
                log.warn("[OTP-DEV] Mail dang tat. Ma OTP ky hop dong cho {} la: {}",
                        email, otp.getCode());
            }
        } catch (RuntimeException ex) {
            log.warn("[OTP-DEV] Khong gui duoc email OTP (mailEnabled={}). Ly do: {}. Ma OTP ky hop dong cho {} la: {}",
                    mailEnabled, ex.getMessage(), email, otp.getCode());
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
                    <p style="margin:0 0 8px;color:#dc2626;font-weight:600">Mã chỉ có hiệu lực trong 5 phút.</p>
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
            ensurePrivateContractSnapshot(assignment, c);
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
        String content = "Phụ huynh/học sinh đã ký hợp đồng lớp \"" + c.getTitle()
                + "\". Vui lòng mở mục Hợp đồng để ký xác nhận và bắt đầu lớp.";
        notificationDispatchService.notifyUserFromTemplate(
                assignment.getTutor().getUser(),
                com.tcs.module.messaging.enums.NotificationType.APPLICATION,
                "MARKETPLACE_CONTRACT_TUTOR_SIGN",
                Map.of("classTitle", c.getTitle()),
                "Bên A đã ký hợp đồng — mời bạn ký",
                content,
                CONTRACT_CONTEXT_TYPE,
                c.getClassId());
    }

    private void notifyClientContractPaymentReady(TutoringClass c) {
        if (c.getCreator() == null) {
            return;
        }
        String content = "Hợp đồng lớp \"" + c.getTitle()
                + "\" đã được ký xong. Vui lòng mở mục Hợp đồng để quét mã thanh toán ký quỹ.";
        notificationDispatchService.notifyUserFromTemplate(
                c.getCreator(),
                com.tcs.module.messaging.enums.NotificationType.APPLICATION,
                "MARKETPLACE_ESCROW_PAYMENT_READY",
                Map.of("classTitle", c.getTitle()),
                "Hợp đồng đã hoàn tất - vui lòng thanh toán ký quỹ",
                content,
                CONTRACT_CONTEXT_TYPE,
                c.getClassId());
    }

    private Contract ensurePrivateContractSnapshot(ClassAssignment assignment, TutoringClass tutoringClass) {
        Contract contract = contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                .orElseGet(() -> contractService.generateForAssignment(assignment.getAssignmentId()));
        LocalDateTime signedAt = latestTime(assignment.getClientSignedAt(), assignment.getTutorSignedAt());
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(signedAt);
        contract.setConfirmedAt(signedAt);
        contract = contractRepository.save(contract);

        syncPrivateContractSignature(contract, PartyRole.CLIENT, tutoringClass.getCreator(), assignment.getClientSignedAt());
        if (assignment.getTutor() != null) {
            syncPrivateContractSignature(contract, PartyRole.TUTOR, assignment.getTutor().getUser(), assignment.getTutorSignedAt());
        }
        return contract;
    }

    private void syncPrivateContractSignature(
            Contract contract,
            PartyRole partyRole,
            User signer,
            LocalDateTime signedAt) {
        if (contract == null || contract.getContractId() == null || signer == null || signedAt == null) {
            return;
        }
        ContractSignature signature = contractSignatureRepository
                .findByContractIdAndPartyRole(contract.getContractId(), partyRole)
                .orElseGet(() -> {
                    ContractSignature created = new ContractSignature();
                    created.setContract(contract);
                    created.setPartyRole(partyRole);
                    return created;
                });
        signature.setSigner(signer);
        signature.setEmail(signer.getEmail());
        signature.setSignedAt(signedAt);
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignatureData("MARKETPLACE_OTP_VERIFIED:" + signer.getEmail() + ":" + signedAt);
        contractSignatureRepository.save(signature);
    }

    private LocalDateTime latestTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
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
        BigDecimal amount = resolvePrivateEscrowAmount(tutoringClass, assignment);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Không xác định được số tiền escrow cần thanh toán");
        }
        escrowService.preparePayment(new EscrowLockCommand(
                tutoringClass.getCreator().getUserId(),
                amount,
                assignment.getAssignmentId(),
                null));
    }

    private String resolvePrivatePaymentMethod(TutoringClass tutoringClass) {
        return plannedPrivateClassMonths(tutoringClass) > 1 ? "DEPOSIT_1M" : "FULL";
    }

    private BigDecimal resolvePrivateEscrowAmount(TutoringClass tutoringClass, ClassAssignment assignment) {
        BigDecimal totalAmount = resolvePrivateContractTotalAmount(tutoringClass, assignment);
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int plannedMonths = plannedPrivateClassMonths(tutoringClass);
        if (plannedMonths <= 1) {
            return totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return totalAmount.divide(BigDecimal.valueOf(plannedMonths), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePrivateContractTotalAmount(TutoringClass tutoringClass, ClassAssignment assignment) {
        BigDecimal totalAmount = courseFeeFromSchedule(tutoringClass, acceptedSubjectKeys(assignment));
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            totalAmount = resolvePrivateDealTotalAmount(tutoringClass, assignment);
        }
        return totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalAmount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
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

    private BigDecimal resolvePrivateDealTotalAmount(TutoringClass tutoringClass, ClassAssignment assignment) {
        BigDecimal totalAmount = resolveFromDealDetails(tutoringClass, acceptedSubjectKeys(assignment));
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmount;
        }
        BigDecimal tuitionFee = positiveAmount(tutoringClass != null ? tutoringClass.getTuitionFee() : null);
        if (tuitionFee != null && tutoringClass != null && tutoringClass.getNumberOfSessions() != null) {
            return tuitionFee.multiply(BigDecimal.valueOf(tutoringClass.getNumberOfSessions()));
        }
        return positiveAmount(tutoringClass != null ? tutoringClass.getBudget() : null);
    }

    private BigDecimal resolveFromDealDetails(TutoringClass tutoringClass, java.util.Set<String> acceptedSubjectKeys) {
        if (tutoringClass == null || !StringUtils.hasText(tutoringClass.getDetailsJson())) {
            return null;
        }
        JsonNode root = readTree(filterDetailsToSubjects(tutoringClass.getDetailsJson(), acceptedSubjectKeys));
        JsonNode subjectFeesNode = root.path("subjectFees");
        JsonNode slots = root.path("slots");
        if (subjectFeesNode == null || !subjectFeesNode.isObject() || slots == null || !slots.isArray()) {
            return null;
        }
        Map<String, BigDecimal> fees = readRates(subjectFeesNode.toString());
        if (fees == null || fees.isEmpty()) {
            return null;
        }
        int repeats = Math.max(1, patternRepeats(root));
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : fees.entrySet()) {
            BigDecimal fee = entry.getValue();
            if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal hours = hoursPerRepeatForSubject(root, entry.getKey());
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(fee.multiply(hours).multiply(BigDecimal.valueOf(repeats)));
        }
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : null;
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

    private BigDecimal slotHours(String start, String end) {
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            return BigDecimal.ZERO;
        }
        try {
            LocalTime s = LocalTime.parse(start);
            LocalTime e = LocalTime.parse(end);
            int startMin = s.getHour() * 60 + s.getMinute();
            int endMin = e.getHour() * 60 + e.getMinute();
            int diff = endMin - startMin;
            return diff > 0 ? BigDecimal.valueOf(diff).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private void verifySignOtp(String email, String code) {
        otpService.verify(email, OtpPurpose.CONTRACT_SIGNING, code, signOtpVerifyPolicy());
    }

    /** Cấu hình xác minh OTP ký hợp đồng (marketplace) — giữ nguyên thông báo & hành vi khoá cũ. */
    private OtpVerifyPolicy signOtpVerifyPolicy() {
        return OtpVerifyPolicy.builder()
                .maxAttempts(SIGN_OTP_MAX_ATTEMPTS)
                .lockOnMaxAttempts(true)
                .throwMaxOnReach(true)
                .showRemaining(true)
                .missingMessage("Vui lòng nhập mã OTP đã gửi tới email của bạn.")
                .notFoundMessage("Mã OTP không tồn tại. Vui lòng bấm gửi lại mã.")
                .expiredMessage("Mã OTP đã hết hạn. Vui lòng bấm gửi lại mã.")
                .maxAttemptsMessage("Bạn đã nhập sai mã quá " + SIGN_OTP_MAX_ATTEMPTS
                        + " lần. Vui lòng đợi " + SIGN_OTP_LOCK_MINUTES + " phút rồi gửi lại mã.")
                .wrongRemainingTemplate("Mã OTP không đúng. Bạn còn %d lần thử.")
                .build();
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
        // Lớp mở lại sau khi gia sư từ chối -> làm mới hạn hiển thị 30 ngày.
        tutoringClass.setExpiresAt(java.time.LocalDateTime.now().plusDays(CLASS_DISPLAY_DAYS));
        tutoringClassRepository.save(tutoringClass);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> listMyLessons() {
        LocalDate today = LocalDate.now();
        List<Lesson> lessons = myLessons();
        // Lớp đã điểm danh buổi cuối => khóa đổi lịch cho mọi buổi của lớp đó.
        Set<Long> lockedClassIds = lessons.stream()
                .collect(Collectors.groupingBy(l -> l.getTutoringClass().getClassId()))
                .entrySet().stream()
                .filter(e -> isLastLessonAttended(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return lessons.stream()
                .map(lesson -> toLesson(lesson, today,
                        lockedClassIds.contains(lesson.getTutoringClass().getClassId())))
                .toList();
    }

    /**
     * Lịch học các lớp TRUNG TÂM mà client (phụ huynh) đã GHI DANH, theo từng ngày —
     * để client xem thời khóa biểu như phía gia sư (chỉ xem, không thao tác).
     */
    @Override
    @Transactional(readOnly = true)
    public List<CenterScheduleClassResponse> getMyEnrolledSchedule(LocalDate date) {
        Long userId = authHelper.currentUserId();
        LocalDate d = date != null ? date : LocalDate.now();
        int weekday = d.getDayOfWeek().getValue();

        Map<Long, TutoringClass> classes = new LinkedHashMap<>();
        Map<Long, List<ClassStudent>> studentsByClass = new HashMap<>();
        // Người HỌC là child, nhưng bản ghi ghi danh do phụ huynh (enrolledByUser). Cho CẢ HAI thấy lịch:
        //  - phụ huynh: các bản ghi mình ghi danh (enrolledByUser = mình)
        //  - child: các bản ghi mà tài khoản học viên là mình (studentEmail = email mình)
        String myEmail = userRepository.findById(userId).map(User::getEmail).orElse(null);
        List<ClassStudent> myRecords = new ArrayList<>(classStudentRepository
                .findByEnrolledByUser_UserIdAndStatus(userId, ClassStudentStatus.ENROLLED));
        if (StringUtils.hasText(myEmail)) {
            myRecords.addAll(classStudentRepository
                    .findByStudentEmailAndStatus(myEmail, ClassStudentStatus.ENROLLED));
        }
        Set<Long> seenRecords = new LinkedHashSet<>();
        for (ClassStudent cs : myRecords) {
            if (cs.getClassStudentId() != null && !seenRecords.add(cs.getClassStudentId())) {
                continue; // dedup: acc người lớn tự ghi danh khớp cả 2 truy vấn
            }
            TutoringClass c = cs.getTutoringClass();
            if (c == null || c.getClassType() != ClassType.CENTER) {
                continue;
            }
            classes.put(c.getClassId(), c);
            studentsByClass.computeIfAbsent(c.getClassId(), k -> new ArrayList<>()).add(cs);
        }

        // Buổi đã được duyệt dời: bỏ khỏi ngày gốc, hiện ở ngày mới — giống hệt cách
        // màn lịch của trung tâm dựng, để học sinh và trung tâm không nhìn thấy hai lịch khác nhau.
        List<com.tcs.module.marketplace.dto.RescheduleEntry> approvedMoves =
                rescheduleService.listApprovedByClassIds(classes.keySet());
        Set<Long> movedAway = approvedMoves.stream()
                .filter(e -> e.originalDate().equals(d))
                .map(com.tcs.module.marketplace.dto.RescheduleEntry::classId)
                .collect(Collectors.toSet());

        List<CenterScheduleClassResponse> result = new ArrayList<>();
        for (TutoringClass c : classes.values()) {
            if (c.getStartDate() == null || c.getEndDate() == null
                    || d.isBefore(c.getStartDate()) || d.isAfter(c.getEndDate())
                    || movedAway.contains(c.getClassId())) {
                continue;
            }
            CenterScheduleClassResponse item = buildEnrolledScheduleItem(c, d, weekday, studentsByClass);
            if (item != null) {
                result.add(item);
            }
        }

        // Buổi được dời TỚI ngày này: lấy khung tiết theo thứ của ngày GỐC, rồi ghi đè giờ mới.
        for (com.tcs.module.marketplace.dto.RescheduleEntry e : approvedMoves) {
            if (!e.newDate().equals(d)) {
                continue;
            }
            TutoringClass c = classes.get(e.classId());
            if (c == null) {
                continue;
            }
            CenterScheduleClassResponse item = buildEnrolledScheduleItem(
                    c, d, e.originalDate().getDayOfWeek().getValue(), studentsByClass);
            if (item != null) {
                if (e.newStartTime() != null && e.newEndTime() != null) {
                    item.setSlots(List.of(
                            com.tcs.module.center.dto.response.ScheduleSlotResponse.builder()
                                    .dayOfWeek(d.getDayOfWeek().getValue())
                                    .startTime(e.newStartTime())
                                    .endTime(e.newEndTime())
                                    .build()));
                }
                item.setRescheduled(true);
                item.setRescheduleNote("Dời từ " + e.originalDate().format(SCHEDULE_DAY_MONTH));
                result.add(item);
            }
        }
        return result;
    }

    private static final DateTimeFormatter SCHEDULE_DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    /** Dựng một dòng lịch của lớp trung tâm cho học viên đang xem. Null nếu ngày đó lớp không có tiết. */
    private CenterScheduleClassResponse buildEnrolledScheduleItem(
            TutoringClass c, LocalDate d, int weekday, Map<Long, List<ClassStudent>> studentsByClass) {
            List<ScheduleSlot> slotsToday = scheduleSlotRepository
                    .findByTutoringClass_ClassId(c.getClassId()).stream()
                    .filter(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == weekday)
                    .sorted(Comparator.comparing(ScheduleSlot::getStartTime))
                    .toList();
            if (slotsToday.isEmpty()) {
                return null;
            }

            Map<Long, String> attendanceByStudent = new HashMap<>();
            // Tra buổi học theo NGÀY trước: lesson_date là dữ liệu buổi tự lưu, không phải thứ
            // được dựng lại. Cách cũ chỉ dò theo (slot, sequenceNo) — hai bên phải tính ra y hệt
            // nhau mới khớp, lệch một chút là học viên không thấy điểm danh dù gia sư đã điểm.
            ScheduleSlot repSlot = slotsToday.get(0);
            int seq = (int) Math.max(0, ChronoUnit.DAYS.between(c.getStartDate(), d));
            Lesson lesson = lessonRepository
                    .findByTutoringClass_ClassIdAndLessonDateOrderBySequenceNoAsc(c.getClassId(), d)
                    .stream()
                    .findFirst()
                    .orElseGet(() -> lessonRepository
                            .findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
                                    c.getClassId(), repSlot.getSlotId(), seq)
                            .orElse(null));
            if (lesson != null) {
                lessonAttendanceRepository.findByLesson_LessonId(lesson.getLessonId())
                        .forEach(a -> attendanceByStudent.put(
                                a.getClassStudent().getClassStudentId(), a.getStatus().name()));
            }

            String tutorName = classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                            c.getClassId(), ClassAssignmentStatus.ACTIVE)
                    .map(a -> a.getTutor() != null ? a.getTutor().getFullName() : null)
                    .orElse(null);

            List<com.tcs.module.center.dto.response.StudentAttendanceResponse> studentItems = studentsByClass
                    .getOrDefault(c.getClassId(), List.of()).stream()
                    .map(s -> com.tcs.module.center.dto.response.StudentAttendanceResponse.builder()
                            .classStudentId(s.getClassStudentId())
                            .studentName(s.getStudentName())
                            .studentPhone(s.getStudentPhone())
                            .status(attendanceByStudent.get(s.getClassStudentId()))
                            .build())
                    .toList();

            List<com.tcs.module.center.dto.response.ScheduleSlotResponse> slotResponses = slotsToday.stream()
                    .map(s -> com.tcs.module.center.dto.response.ScheduleSlotResponse.builder()
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
                    .assignedTutorName(tutorName)
                    .studentCount(studentItems.size())
                    .students(studentItems)
                    .attendanceTaken(!attendanceByStudent.isEmpty())
                    .classCompleted(c.getStatus() == TutoringClassStatus.COMPLETED)
                    .build();
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
        maybeAutoReleasePrivateFirstMonthEscrow(lesson);
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
        if (present) {
            maybeAutoReleasePrivateFirstMonthEscrow(lesson);
        }
    }

    private void requireLessonIsToday(Lesson lesson) {
        LocalDate today = LocalDate.now();
        if (!today.equals(lesson.getLessonDate())) {
            throw new IllegalArgumentException(
                    "Chỉ điểm danh được trong ngày diễn ra buổi học ("
                            + lesson.getLessonDate() + "). Hôm nay là " + today + ".");
        }
        // Lớp private cho phép gia sư điểm danh bất cứ lúc nào trong đúng ngày học.
    }

    private void sendClassNotification(User user, String title, String content, Long classId) {
        if (user == null) {
            return;
        }
        notificationDispatchService.notifyUserFromTemplate(
                user,
                com.tcs.module.messaging.enums.NotificationType.CLASS,
                "MARKETPLACE_CLASS_EVENT",
                Map.of("title", title, "content", content),
                title,
                content,
                "TUTORING_CLASS",
                classId);
    }

    /**
     * Thông báo mà việc cần làm nằm ở HỢP ĐỒNG (ký, thanh toán ký quỹ), không phải ở lớp.
     * Khác {@link #sendClassNotification} đúng ở referenceType — chuông điều hướng theo trường
     * này, nên dùng "TUTORING_CLASS" sẽ đẩy người dùng sang Lịch học thay vì trang Hợp đồng.
     */
    private void sendContractNotification(User user, String title, String content, Long classId) {
        if (user == null) {
            return;
        }
        notificationDispatchService.notifyUserFromTemplate(
                user,
                com.tcs.module.messaging.enums.NotificationType.CLASS,
                "MARKETPLACE_CLASS_EVENT",
                Map.of("title", title, "content", content),
                title,
                content,
                CONTRACT_CONTEXT_TYPE,
                classId);
    }

    private void sendReviewNotification(User user, String title, String content, Long classId) {
        if (user == null) {
            return;
        }
        notificationDispatchService.notifyUserFromTemplate(
                user,
                com.tcs.module.messaging.enums.NotificationType.REVIEW,
                "MARKETPLACE_CLASS_REVIEW_REQUIRED",
                Map.of("title", title, "content", content),
                title,
                content,
                "TUTORING_CLASS",
                classId);
    }

    private void maybeAutoReleasePrivateFirstMonthEscrow(Lesson lesson) {
        if (lesson == null || lesson.getTutoringClass() == null) {
            return;
        }
        TutoringClass tutoringClass = lesson.getTutoringClass();
        if (tutoringClass.getClassId() == null
                || tutoringClass.getClassType() != ClassType.PRIVATE
                || tutoringClass.getStartDate() == null
                || tutoringClass.getStatus() != TutoringClassStatus.IN_PROGRESS) {
            return;
        }

        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        tutoringClass.getClassId(), ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        if (assignment == null || assignment.getAssignmentId() == null) {
            return;
        }

        EscrowTransaction escrow = escrowTransactionRepository
                .findByAssignment_AssignmentId(assignment.getAssignmentId())
                .orElse(null);
        if (escrow == null || escrow.getStatus() != EscrowStatus.FUNDED) {
            return;
        }

        if (!isPrivateFirstMonthEscrowReadyForRelease(tutoringClass, assignment, escrow)) {
            return;
        }

        escrowService.apply(new ReleaseInstruction(
                escrow.getEscrowId(),
                escrow.getAmount(),
                BigDecimal.ZERO,
                "Đã hoàn tất đủ buổi của tháng đầu, giải ngân khoản ký quỹ cho gia sư."));

        String title = "Ký quỹ tháng đầu đã được giải ngân";
        String content = "Lớp \"" + tutoringClass.getTitle()
                + "\" đã hoàn thành đủ buổi của tháng đầu và không có khiếu nại đang xử lý. "
                + "Hệ thống đã giải ngân khoản ký quỹ tháng đầu cho gia sư.";
        sendClassNotification(assignment.getTutor().getUser(), title, content, tutoringClass.getClassId());
        sendClassNotification(tutoringClass.getCreator(), title, content, tutoringClass.getClassId());
    }

    private boolean isPrivateFirstMonthEscrowReadyForRelease(
            TutoringClass tutoringClass,
            ClassAssignment assignment,
            EscrowTransaction escrow) {
        if (tutoringClass == null
                || assignment == null
                || escrow == null
                || tutoringClass.getClassId() == null
                || assignment.getAssignmentId() == null
                || escrow.getEscrowId() == null) {
            return false;
        }
        if (tutoringClass.getStatus() == TutoringClassStatus.DISPUTED
                || tutoringClass.getStatus() == TutoringClassStatus.CANCELLED
                || tutoringClass.getStatus() == TutoringClassStatus.COMPLETED) {
            return false;
        }

        if (reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.CLASS,
                tutoringClass.getClassId(),
                ReportStatus.PENDING)) {
            return false;
        }
        if (classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                assignment.getAssignmentId(), ClassTerminationStatus.PENDING)
                || classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        assignment.getAssignmentId(), ClassTerminationStatus.APPROVED)) {
            return false;
        }
        if (escrow.getStatus() == EscrowStatus.DISPUTED || escrow.getStatus() == EscrowStatus.ON_HOLD) {
            return false;
        }
        if (disputeRepository.existsByEscrowTransaction_EscrowIdAndStatusNot(
                escrow.getEscrowId(), DisputeStatus.RESOLVED)
                || refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                        escrow.getEscrowId(), RefundRequestStatus.PENDING)
                || refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                        escrow.getEscrowId(), RefundRequestStatus.APPROVED)) {
            return false;
        }

        List<Lesson> lessons = lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(tutoringClass.getClassId());
        if (lessons.isEmpty()) {
            return false;
        }

        LocalDate firstMonthEndExclusive = tutoringClass.getStartDate().plusMonths(1);
        boolean hasFirstMonthLesson = false;
        for (Lesson currentLesson : lessons) {
            LocalDate lessonDate = currentLesson.getLessonDate();
            if (lessonDate == null
                    || lessonDate.isBefore(tutoringClass.getStartDate())
                    || !lessonDate.isBefore(firstMonthEndExclusive)) {
                continue;
            }
            hasFirstMonthLesson = true;
            if (currentLesson.getAttendanceStatus() != AttendanceStatus.COMPLETED) {
                return false;
            }
        }
        return hasFirstMonthLesson;
    }

    private void notifyStudentEnrollmentSuccess(ClassStudent classStudent) {
        if (classStudent == null || classStudent.getEnrolledByUser() == null || classStudent.getTutoringClass() == null) {
            return;
        }
        TutoringClass tutoringClass = classStudent.getTutoringClass();
        String classTitle = StringUtils.hasText(tutoringClass.getTitle()) ? tutoringClass.getTitle() : "lớp học";
        String studentName = StringUtils.hasText(classStudent.getStudentName()) ? classStudent.getStudentName() : "Học viên";
        // Người ký/thanh toán (phụ huynh nếu là minor, hoặc chính học viên).
        User enroller = classStudent.getEnrolledByUser();
        sendClassNotification(
                enroller,
                "Ghi danh thành công",
                studentName + " đã được ghi danh thành công vào lớp \"" + classTitle
                        + "\" sau khi hệ thống xác nhận thanh toán.",
                tutoringClass.getClassId());
        // Người HỌC là child: nếu tài khoản học viên khác tài khoản người ghi danh (phụ huynh) thì báo cho child.
        if (StringUtils.hasText(classStudent.getStudentEmail())) {
            userRepository.findByEmail(classStudent.getStudentEmail())
                    .filter(child -> enroller == null || !child.getUserId().equals(enroller.getUserId()))
                    .ifPresent(child -> sendClassNotification(
                            child,
                            "Bạn đã chính thức vào lớp",
                            "Bạn đã chính thức vào lớp \"" + classTitle
                                    + "\" sau khi phụ huynh hoàn tất ký hợp đồng và thanh toán.",
                            tutoringClass.getClassId()));
        }
    }

    private void notifyClassTerminationSubmitted(
            ClassTerminationRequest termination,
            TutoringClass tutoringClass,
            TerminationTarget target) {

        if (termination == null || tutoringClass == null) {
            return;
        }
        String classTitle = classNotificationTitle(tutoringClass);
        sendClassNotification(
                termination.getRequestedBy(),
                "Đã gửi yêu cầu chấm dứt sớm",
                "Yêu cầu chấm dứt sớm lớp \"" + classTitle
                        + "\" đã được ghi nhận. Khoản ký quỹ liên quan đang được giữ để chờ xử lý.",
                tutoringClass.getClassId());

        notifyOtherTerminationUsers(
                termination,
                tutoringClass,
                target,
                "Có yêu cầu chấm dứt sớm lớp",
                "Lớp \"" + classTitle
                        + "\" vừa có yêu cầu chấm dứt sớm. Khoản ký quỹ liên quan đang được giữ để chờ xử lý.");
    }

    private void notifyClassTerminationCompleted(
            ClassTerminationRequest termination,
            TutoringClass tutoringClass,
            TerminationTarget target) {

        if (termination == null || tutoringClass == null) {
            return;
        }
        String classTitle = classNotificationTitle(tutoringClass);
        sendClassNotification(
                termination.getRequestedBy(),
                "Lớp đã chấm dứt sớm",
                "Yêu cầu chấm dứt sớm lớp \"" + classTitle
                        + "\" đã được xử lý. Khoản ký quỹ liên quan đã được tất toán theo quy tắc của hệ thống.",
                tutoringClass.getClassId());

        notifyOtherTerminationUsers(
                termination,
                tutoringClass,
                target,
                "Lớp đã chấm dứt sớm",
                "Lớp \"" + classTitle
                        + "\" đã chấm dứt sớm. Khoản ký quỹ liên quan đã được tất toán theo quy tắc của hệ thống.");
    }

    private void notifyOtherTerminationUsers(
            ClassTerminationRequest termination,
            TutoringClass tutoringClass,
            TerminationTarget target,
            String title,
            String content) {

        Long requesterId = termination.getRequestedBy() != null ? termination.getRequestedBy().getUserId() : null;
        for (User user : classTerminationNotificationRecipients(termination, tutoringClass, target)) {
            if (user == null || Objects.equals(user.getUserId(), requesterId)) {
                continue;
            }
            sendClassNotification(user, title, content, tutoringClass.getClassId());
        }
    }

    private List<User> classTerminationNotificationRecipients(
            ClassTerminationRequest termination,
            TutoringClass tutoringClass,
            TerminationTarget target) {

        List<User> users = new ArrayList<>();
        Set<Long> seenUserIds = new LinkedHashSet<>();
        addTerminationNotificationUser(users, seenUserIds, termination.getRequestedBy());
        addTerminationNotificationUser(users, seenUserIds, tutoringClass.getCreator());
        if (tutoringClass.getCenter() != null) {
            addTerminationNotificationUser(users, seenUserIds, tutoringClass.getCenter().getUser());
        }
        if (target != null && target.assignment() != null && target.assignment().getTutor() != null) {
            addTerminationNotificationUser(users, seenUserIds, target.assignment().getTutor().getUser());
        }
        if (target != null && target.classStudent() != null) {
            addTerminationNotificationUser(users, seenUserIds, target.classStudent().getEnrolledByUser());
        }
        return users;
    }

    private void addTerminationNotificationUser(List<User> users, Set<Long> seenUserIds, User user) {
        if (user == null || user.getUserId() == null || seenUserIds.contains(user.getUserId())) {
            return;
        }
        seenUserIds.add(user.getUserId());
        users.add(user);
    }

    private String classNotificationTitle(TutoringClass tutoringClass) {
        if (tutoringClass != null && StringUtils.hasText(tutoringClass.getTitle())) {
            return tutoringClass.getTitle();
        }
        return "lớp học";
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
        // Đã điểm danh buổi cuối cùng => lớp coi như học xong, khóa toàn bộ đổi lịch.
        if (isLastLessonAttended(lessonRepository.findByTutoringClass_ClassId(
                lesson.getTutoringClass().getClassId()))) {
            throw new IllegalArgumentException(
                    "Lớp đã điểm danh buổi học cuối cùng nên không thể đổi lịch nữa.");
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
            // Đổi lịch được duyệt -> phát lại nhắc nhở vào 00:00 ngày học mới.
            lesson.setReminderSentAt(null);
            lessonRepository.save(lesson);
            // Nếu chuyển sang đúng hôm nay thì nhắc nhở ngay, không chờ 00:00 hôm sau.
            lessonReminderService.sendReminderIfToday(lesson);
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
            // Buổi mới thêm rơi vào hôm nay -> nhắc nhở ngay.
            lessonReminderService.sendReminderIfToday(lesson);
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

        // Buổi đã trôi qua trước khi gia sư nhận lớp thì không sinh: chiều thứ 5 mới nhận lớp thì
        // ca sáng thứ 5 tuần này bỏ qua, buổi đầu tiên rơi vào sáng thứ 5 tuần sau. Nhờ vậy cũng
        // không phát sinh buổi "ma" để điểm danh ngược về quá khứ.
        List<Map.Entry<LocalDate, SlotSpec>> occurrences = upcomingOnly(
                expandOccurrences(form, specs, tutoringClass));
        if (occurrences.isEmpty()) {
            return;
        }
        // Tới bước này hai bên đã ký và tiền cọc đã vào escrow -> KHÔNG được ném ngoại lệ nữa,
        // nếu không giao dịch webhook SePay sẽ rollback và khách coi như chuyển khoản mất trắng.
        // Trùng lịch chỉ cảnh báo để gia sư vào "Đổi lịch buổi học" xử lý.
        String conflict = findScheduleConflict(tutoringClass, tutor, occurrences);
        if (conflict != null) {
            log.warn("[Schedule] Lop {} bi trung lich khi kich hoat: trung {}",
                    tutoringClass.getClassId(), conflict);
            notifyScheduleOverlap(tutoringClass, tutor, conflict);
        }

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

    /** Bỏ các buổi đã kết thúc tính tới thời điểm hiện tại; giữ nguyên thứ tự. */
    private List<Map.Entry<LocalDate, SlotSpec>> upcomingOnly(
            List<Map.Entry<LocalDate, SlotSpec>> occurrences) {
        LocalDateTime now = LocalDateTime.now();
        return occurrences.stream()
                .filter(o -> o.getKey().atTime(o.getValue().end()).isAfter(now))
                .toList();
    }

    /** Thông báo trùng lịch đầu tiên tìm được, hoặc {@code null} nếu lịch sạch. */
    private String findScheduleConflict(
            TutoringClass tutoringClass, Tutor tutor, List<Map.Entry<LocalDate, SlotSpec>> occurrences) {
        Long classId = tutoringClass.getClassId();
        Collection<Lesson> existing = busyLessonsOf(tutoringClass, tutor);

        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<LocalDate, SlotSpec> occurrence : occurrences) {
            LocalDate date = occurrence.getKey();
            SlotSpec spec = occurrence.getValue();
            // Buổi của lớp mới đã trôi qua thì không còn tranh chấp giờ với ai nữa.
            if (!date.atTime(spec.end()).isAfter(now)) {
                continue;
            }
            for (Lesson lesson : existing) {
                if (lesson.getTutoringClass().getClassId().equals(classId)
                        || !date.equals(lesson.getLessonDate())) {
                    continue;
                }
                LocalTime otherStart = lesson.getSlot().getStartTime();
                LocalTime otherEnd = lesson.getSlot().getEndTime();
                // Buổi cũ đã dạy xong thì thôi — qua giờ đó là gia sư rảnh trở lại.
                if (!lesson.getLessonDate().atTime(otherEnd).isAfter(now)) {
                    continue;
                }
                if (overlaps(spec.start(), spec.end(), otherStart, otherEnd)) {
                    return "lớp \"" + lesson.getTutoringClass().getTitle() + "\" vào "
                            + date + " (" + otherStart + "–" + otherEnd + ")";
                }
            }
        }
        return null;
    }

    /**
     * Mô tả buổi dạy sẵn có của gia sư đang chồng giờ với lịch lớp này, hoặc {@code null} nếu rảnh.
     * Dùng khi gia sư ứng tuyển và khi khách chọn gia sư — cả hai đều xảy ra trước lúc có hợp đồng.
     */
    private String scheduleConflictOf(
            TutoringClass tutoringClass, Tutor tutor, java.util.Set<String> subjectKeys) {
        if (tutor == null) {
            return null;
        }
        JsonNode form = readTree(filterDetailsToSubjects(tutoringClass.getDetailsJson(), subjectKeys));
        List<SlotSpec> specs = slotSpecs(form);
        if (specs.isEmpty() || tutoringClass.getStartDate() == null || tutoringClass.getEndDate() == null) {
            return null;
        }
        return findScheduleConflict(tutoringClass, tutor, expandOccurrences(form, specs, tutoringClass));
    }

    /** Lớp đã kích hoạt nhưng có buổi chồng giờ — báo gia sư vào đổi lịch. */
    private void notifyScheduleOverlap(TutoringClass tutoringClass, Tutor tutor, String detail) {
        if (tutor == null || tutor.getUser() == null) {
            return;
        }
        String content = "Lớp \"" + tutoringClass.getTitle() + "\" đã bắt đầu nhưng có buổi chồng giờ với "
                + detail + ". Vui lòng vào Lịch dạy cá nhân để đổi lịch buổi bị trùng.";
        notificationDispatchService.notifyUserFromTemplate(
                tutor.getUser(),
                com.tcs.module.messaging.enums.NotificationType.APPLICATION,
                "MARKETPLACE_SCHEDULE_OVERLAP",
                Map.of("classTitle", tutoringClass.getTitle()),
                "Lớp mới có buổi trùng lịch — cần đổi lịch",
                content,
                "TUTORING_CLASS",
                tutoringClass.getClassId());
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

    private int durationCountOf(JsonNode form) {
        return Math.max(1, form.path("months").asInt(1));
    }

    private int totalMonthsOf(JsonNode form) {
        int n = durationCountOf(form);
        return "YEAR".equals(form.path("durationUnit").asText("MONTH")) ? n * 12 : n;
    }

    private int weeksForCycle(JsonNode form) {
        return switch (form.path("billingCycle").asText("MONTH")) {
            case "MONTH" -> totalMonthsOf(form) * 4;
            case "TERM" -> 12;
            case "QUARTER" -> 24;
            case "YEAR" -> 48;
            default -> 4;
        };
    }

    private int repeatWeeksOf(JsonNode form) {
        if (!"WEEKLY".equals(form.path("scheduleMode").asText("WEEKLY"))) {
            return 1;
        }
        int n = form.path("repeatEveryWeeks").asInt(1);
        return Math.min(4, Math.max(1, n));
    }

    private int patternRepeats(JsonNode form) {
        int weeks = weeksForCycle(form);
        int cycleWeeks = repeatWeeksOf(form);
        if (cycleWeeks <= 1) {
            return weeks;
        }
        Set<Integer> on = studyWeeksOf(form, cycleWeeks);
        int remainder = weeks % cycleWeeks;
        return (weeks / cycleWeeks) * on.size() + (int) on.stream().filter(w -> w <= remainder).count();
    }

    private BigDecimal hoursPerRepeatForSubject(JsonNode form, String subjectId) {
        JsonNode slots = form.path("slots");
        if (slots == null || !slots.isArray() || !StringUtils.hasText(subjectId)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode slot : slots) {
            if (!subjectId.equals(slot.path("subjectId").asText(""))) {
                continue;
            }
            total = total.add(slotHours(slot.path("start").asText(""), slot.path("end").asText("")));
        }
        return total;
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
        CompletionView completion = resolveCompletionView(c, assignment);
        return AssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .classId(c.getClassId())
                .classTitle(c.getTitle())
                .classStatus(c.getStatus().name())
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
                .classCompleted(c.getStatus() == TutoringClassStatus.COMPLETED)
                .completionState(completion.state())
                .completionBlockedReason(completion.blockedReason())
                .build();
    }

    private LessonResponse toLesson(Lesson lesson, LocalDate today, boolean rescheduleLocked) {
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
                // Chỉ mở điểm danh từ giờ bắt đầu slot đến hết ngày hôm đó (khớp requireLessonIsToday).
                .canCheckInToday(today.equals(lesson.getLessonDate())
                        && (slot.getStartTime() == null
                                || !LocalTime.now().isBefore(slot.getStartTime())))
                .rescheduleLocked(rescheduleLocked)
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
            ClassTerminationRequest savedTermination = classTerminationRequestRepository.save(termination);
            notifyClassTerminationSubmitted(savedTermination, tutoringClass, target);
            return toTerminationResponse(savedTermination, tutoringClass);
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

        ClassTerminationRequest savedTermination = classTerminationRequestRepository.save(termination);
        notifyClassTerminationCompleted(savedTermination, tutoringClass, target);
        return toTerminationResponse(savedTermination, tutoringClass);
    }

    // ===== UC "Xác nhận lớp đã hoàn thành" (lớp PRIVATE: 1 gia sư – 1 phụ huynh/học viên) =====

    @Override
    @Transactional
    public String confirmClassCompletion(Long classId) {
        requireUser();
        TutoringClass c = findClass(classId);
        if (c.getClassType() == ClassType.CENTER) {
            throw new IllegalArgumentException("Chức năng hoàn thành lớp chỉ áp dụng cho lớp gia sư riêng.");
        }
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Lớp chưa có gia sư nhận, không thể hoàn thành."));
        String role = contractRoleOf(assignment, c); // ném lỗi nếu không phải gia sư/người tạo lớp
        if (!"TUTOR".equals(role)) {
            throw new IllegalArgumentException("Chỉ gia sư mới có thể đánh dấu hoàn thành lớp.");
        }

        if (c.getStatus() == TutoringClassStatus.COMPLETED) {
            throw new IllegalArgumentException("Lớp đã hoàn thành.");
        }
        if (c.getStatus() != TutoringClassStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Chỉ hoàn thành khi lớp đang diễn ra.");
        }
        String blockedReason = completionBlockedReason(c);
        if (blockedReason != null) {
            throw new IllegalArgumentException(blockedReason);
        }

        if (assignment.getTutorCompletedAt() == null) {
            assignment.setTutorCompletedAt(LocalDateTime.now());
            classAssignmentRepository.save(assignment);
        }

        // Học viên đã đánh giá gia sư -> đóng lớp ngay + giải ngân.
        if (contractService.hasClientReviewedClass(classId)) {
            assignment.setClientCompletedAt(LocalDateTime.now());
            classAssignmentRepository.save(assignment);
            finalizeClassCompletion(c, assignment);
            return "Lớp đã hoàn thành. Học phí ký quỹ đã được giải ngân cho gia sư.";
        }

        // Chưa đánh giá -> mời học viên đánh giá; lớp đóng khi học viên đánh giá xong.
        sendReviewNotification(
                c.getCreator(),
                "Vui lòng đánh giá gia sư để hoàn thành lớp",
                "Gia sư đã đánh dấu lớp \"" + c.getTitle()
                        + "\" hoàn thành. Vui lòng đánh giá gia sư để hoàn tất lớp học,"
                        + " hệ thống sẽ giải ngân học phí cho gia sư sau khi bạn đánh giá.",
                classId);
        return "Đã gửi yêu cầu tới học viên. Lớp sẽ đóng và giải ngân sau khi học viên đánh giá gia sư.";
    }

    /**
     * Khi học viên đánh giá gia sư (sự kiện {@link ClientReviewedClassEvent}): nếu gia sư đã yêu cầu
     * hoàn thành lớp thì đóng lớp + giải ngân. Chạy sau khi giao dịch đánh giá đã commit.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClientReviewedClass(ClientReviewedClassEvent event) {
        completeClassAfterClientReview(event.classId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeClassAfterClientReview(Long classId) {
        TutoringClass c = tutoringClassRepository.findById(classId).orElse(null);
        if (c == null || c.getClassType() == ClassType.CENTER
                || c.getStatus() != TutoringClassStatus.IN_PROGRESS) {
            return;
        }
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        // Chỉ đóng khi gia sư đã yêu cầu hoàn thành trước đó.
        if (assignment == null || assignment.getTutorCompletedAt() == null
                || assignment.getClientCompletedAt() != null) {
            return;
        }
        assignment.setClientCompletedAt(LocalDateTime.now());
        classAssignmentRepository.save(assignment);
        finalizeClassCompletion(c, assignment);
    }

    private void finalizeClassCompletion(TutoringClass c, ClassAssignment assignment) {
        EscrowTransaction escrow = escrowTransactionRepository
                .findByAssignment_AssignmentId(assignment.getAssignmentId())
                .orElse(null);
        if (escrow != null && escrow.getStatus() == EscrowStatus.FUNDED) {
            escrowService.apply(new ReleaseInstruction(
                    escrow.getEscrowId(),
                    escrow.getAmount(),
                    BigDecimal.ZERO,
                    "Lớp \"" + c.getTitle() + "\" đã hoàn thành - giải ngân toàn bộ khoản ký quỹ cho gia sư."));
        }
        releaseCenterRequestFeeIfAny(c, assignment);

        c.setStatus(TutoringClassStatus.COMPLETED);
        tutoringClassRepository.save(c);

        contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
                .ifPresent(contract -> {
                    if (contract.getStatus() != ContractStatus.COMPLETED) {
                        contract.setStatus(ContractStatus.COMPLETED);
                        contract.setConfirmedAt(LocalDateTime.now());
                        contractRepository.save(contract);
                    }
                });

        String content = "Lớp \"" + c.getTitle()
                + "\" đã được cả hai bên xác nhận hoàn thành. Học phí đã được giải ngân cho gia sư.";
        sendClassNotification(assignment.getTutor().getUser(), "Lớp đã hoàn thành", content, c.getClassId());
        sendClassNotification(c.getCreator(), "Lớp đã hoàn thành", content, c.getClassId());
    }

    private void releaseCenterRequestFeeIfAny(TutoringClass c, ClassAssignment assignment) {
        if (assignment == null || assignment.getAssignmentId() == null) {
            return;
        }
        // Lớp không đến từ yêu cầu nhờ trung tâm sẽ được bỏ qua lặng lẽ bên trong service.
        // KHÔNG bọc try/catch ở đây: service chạy chung giao dịch, nuốt lỗi chỉ khiến giao dịch bị
        // đánh dấu rollback-only rồi thất bại lúc commit với thông báo khó hiểu.
        centerRequestFeeService.releaseForFulfilledAssignment(
                assignment.getAssignmentId(),
                "Lớp \"" + c.getTitle() + "\" đã hoàn thành — giải ngân phí xử lý yêu cầu cho trung tâm.");
    }

    /**
     * Đủ điều kiện xác nhận hoàn thành khi BUỔI HỌC CUỐI CÙNG đã được điểm danh
     * (trạng thái khác PENDING) và có ít nhất một buổi đã dạy (COMPLETED). Trả về null nếu đủ điều kiện.
     */
    private String completionBlockedReason(TutoringClass c) {
        List<Lesson> lessons = lessonRepository.findByTutoringClass_ClassId(c.getClassId());
        if (lessons.isEmpty()) {
            return "Lớp chưa có buổi học nào để xác nhận hoàn thành.";
        }
        if (!isLastLessonAttended(lessons)) {
            return "Cần điểm danh buổi học cuối cùng trước khi xác nhận hoàn thành lớp.";
        }
        boolean anyCompleted = lessons.stream()
                .anyMatch(l -> l.getAttendanceStatus() == AttendanceStatus.COMPLETED);
        if (!anyCompleted) {
            return "Chưa có buổi nào được điểm danh (đã dạy) nên chưa thể xác nhận hoàn thành lớp.";
        }
        return null;
    }

    /** Buổi học cuối cùng (theo ngày, rồi số thứ tự) đã được điểm danh (khác PENDING) hay chưa. */
    private boolean isLastLessonAttended(List<Lesson> lessons) {
        Lesson last = null;
        for (Lesson l : lessons) {
            if (l.getLessonDate() == null) {
                continue;
            }
            if (last == null
                    || l.getLessonDate().isAfter(last.getLessonDate())
                    || (l.getLessonDate().isEqual(last.getLessonDate())
                            && seqNo(l) > seqNo(last))) {
                last = l;
            }
        }
        return last != null && last.getAttendanceStatus() != AttendanceStatus.PENDING;
    }

    private int seqNo(Lesson l) {
        return l.getSequenceNo() == null ? 0 : l.getSequenceNo();
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
            // Chuông: phụ huynh (người KÝ + trả tiền) và học viên (child) được báo đúng vai trò.
            String enrollClassTitle = StringUtils.hasText(tutoringClass.getTitle())
                    ? tutoringClass.getTitle() : "lớp học";
            if (legal.isDelegatedToParent()) {
                // Người học là child; người ký hợp đồng + thanh toán là phụ huynh (payer).
                sendContractNotification(
                        payer,
                        "Con bạn vừa đăng ký lớp — cần ký hợp đồng",
                        client.getFullName() + " đã đăng ký lớp \"" + enrollClassTitle
                                + "\". Vui lòng vào mục Hợp đồng để ký và thanh toán để "
                                + client.getFullName() + " chính thức vào lớp.",
                        classId);
                sendContractNotification(
                        user,
                        "Đã ghi nhận đăng ký — chờ phụ huynh ký",
                        "Bạn đã đăng ký lớp \"" + enrollClassTitle + "\". Hợp đồng đã gửi cho phụ huynh"
                                + (legal.getLegalHolderName() != null ? " (" + legal.getLegalHolderName() + ")" : "")
                                + " ký và thanh toán. Bạn vào lớp sau khi phụ huynh hoàn tất.",
                        classId);
            } else {
                sendContractNotification(
                        payer,
                        "Cần ký hợp đồng lớp học",
                        "Bạn đã đăng ký lớp \"" + enrollClassTitle
                                + "\". Vui lòng vào mục Hợp đồng để ký và thanh toán để chính thức vào lớp.",
                        classId);
            }
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
                .filter(d -> ClassRequestStore.STATUS_PAYMENT_PENDING.equals(d.status())
                        || ClassRequestStore.STATUS_PENDING.equals(d.status())
                        || ClassRequestStore.STATUS_SEARCHING.equals(d.status()))
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
        ClassRequestStore.ClassRequestData pendingRequest =
                classRequestStore.withStatus(data, ClassRequestStore.STATUS_PAYMENT_PENDING, null);
        classRequestStore.save(pendingRequest);
        CenterRequestFeePaymentResponse payment = centerRequestFeeService.createPayment(
                pendingRequest.requestId(),
                creator.getUserId(),
                center.getUser() != null ? center.getUser().getUserId() : null,
                center.getCompanyName(),
                request.getDesiredBudget(),
                request.getRefundPayoutInfo());
        return classRequestStore.toResponse(pendingRequest).toBuilder()
                .centerRequestFeePayment(payment)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRequestResponse> listMyClassRequests() {
        Long userId = requireUser().getUserId();
        return classRequestStore.findByClient(userId).stream()
                .map(this::toClassRequestResponse)
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
        if (ClassRequestStore.STATUS_ACCEPTED.equals(data.status())
                || ClassRequestStore.STATUS_REJECTED.equals(data.status())
                || ClassRequestStore.STATUS_CANCELLED.equals(data.status())
                || ClassRequestStore.STATUS_PAYMENT_PENDING.equals(data.status())) {
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
        classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdOrderByAssignedDateDesc(classId)
                .ifPresent(assignment -> centerRequestFeeService.linkFulfilledAssignment(
                        requestId,
                        classId,
                        assignment.getAssignmentId()));
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
        if (!ClassRequestStore.STATUS_PAYMENT_PENDING.equals(data.status())) {
            throw new IllegalArgumentException("Chỉ hủy được yêu cầu đang chờ thanh toán");
        }
        centerRequestFeeService.cancelUnpaid(requestId);
    }

    private ClassRequestResponse toClassRequestResponse(ClassRequestStore.ClassRequestData data) {
        return classRequestStore.toResponse(data).toBuilder()
                .centerRequestFeePayment(centerRequestFeeService.getPayment(data.requestId()).orElse(null))
                .build();
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

    private void requireActiveWallet(Long userId) {
        boolean walletReady = walletRepository.findByUser_UserId(userId)
                .filter(wallet -> wallet.getStatus() == WalletStatus.ACTIVE)
                .isPresent();
        if (!walletReady) {
            throw new BusinessException(
                    "Bạn cần tạo ví trước khi tiếp tục. Vui lòng vào Ví của tôi để tạo ví.");
        }
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
        CompletionView completion = resolveCompletionView(c);
        // Gia sư đang thực dạy = phân công ACTIVE (cùng luật với màn quản lý lớp của trung tâm).
        Tutor activeTutor = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        c.getClassId(), ClassAssignmentStatus.ACTIVE)
                .map(ClassAssignment::getTutor)
                .orElse(null);
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
                .completionAssignmentId(completion.assignmentId())
                .completionState(completion.state())
                .completionBlockedReason(completion.blockedReason())
                .schedule(scheduleSlotRepository.findByTutoringClass_ClassId(c.getClassId()).stream()
                        .map(s -> ScheduleSlotResponse.builder()
                                .slotId(s.getSlotId())
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .toList())
                .createdAt(c.getCreatedAt())
                .expiresAt(c.getExpiresAt())
                .applicationCount(tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                        c.getClassId(), TutorApplicationStatus.REJECTED))
                .assignmentId(classAssignmentRepository
                        .findFirstByApplication_TutoringClass_ClassIdOrderByAssignedDateDesc(c.getClassId())
                        .map(ClassAssignment::getAssignmentId)
                        .orElse(null))
                .assignedTutorId(activeTutor != null ? activeTutor.getTutorId() : null)
                .assignedTutorName(activeTutor != null ? activeTutor.getFullName() : null)
                .build();
    }

    /** Trạng thái nút "Hoàn thành lớp" theo góc nhìn người dùng hiện tại. */
    private record CompletionView(
            Long assignmentId,
            String state,
            String blockedReason) {
        static CompletionView none() {
            return new CompletionView(null, "NONE", null);
        }
    }

    private CompletionView resolveCompletionView(TutoringClass c) {
        if (c.getClassType() == ClassType.CENTER) {
            return CompletionView.none();
        }
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdOrderByAssignedDateDesc(c.getClassId())
                .filter(a -> a.getStatus() == ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        if (assignment == null) {
            return CompletionView.none();
        }
        return resolveCompletionView(c, assignment);
    }

    private CompletionView resolveCompletionView(TutoringClass c, ClassAssignment assignment) {
        if (c.getClassType() == ClassType.CENTER) {
            return CompletionView.none();
        }
        Long uid = currentUserIdOrNull();
        if (uid == null) {
            return CompletionView.none();
        }
        boolean isTutor = assignment.getTutor().getUser().getUserId().equals(uid);
        boolean isCreator = c.getCreator().getUserId().equals(uid);
        if (!isTutor && !isCreator) {
            return CompletionView.none();
        }
        Long aid = assignment.getAssignmentId();

        if (c.getStatus() == TutoringClassStatus.COMPLETED) {
            return new CompletionView(aid, "COMPLETED", null);
        }
        if (c.getStatus() != TutoringClassStatus.IN_PROGRESS) {
            return new CompletionView(aid, "NONE", null);
        }

        boolean tutorRequested = assignment.getTutorCompletedAt() != null;
        if (isTutor) {
            if (!tutorRequested) {
                String blockedReason = completionBlockedReason(c);
                return new CompletionView(aid,
                        blockedReason == null ? "TUTOR_CAN_CONFIRM" : "TUTOR_BLOCKED",
                        blockedReason);
            }
            // Đã yêu cầu hoàn thành, đang chờ học viên đánh giá gia sư.
            return new CompletionView(aid, "TUTOR_WAITING", null);
        }

        // Phụ huynh/học viên: gia sư chưa yêu cầu -> chờ; đã yêu cầu -> cần đánh giá để đóng lớp.
        return new CompletionView(aid,
                tutorRequested ? "CLIENT_MUST_REVIEW" : "CLIENT_WAITING_TUTOR", null);
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
