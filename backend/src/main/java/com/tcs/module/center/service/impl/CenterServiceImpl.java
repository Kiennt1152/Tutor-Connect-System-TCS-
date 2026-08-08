package com.tcs.module.center.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.exception.VerificationRequiredException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.RescheduleDecisionBody;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.SaveRecruitmentPostRequest;
import com.tcs.module.center.dto.request.ScheduleSlotRequest;
import com.tcs.module.center.dto.request.SubstitutionDecisionBody;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.CenterTutorResponse;
import com.tcs.module.center.dto.response.RecruitmentApplicationResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.center.dto.response.ScheduleSlotResponse;
import com.tcs.module.center.dto.response.StudentAttendanceResponse;
import com.tcs.module.center.dto.response.SubstitutionResponse;
import com.tcs.module.center.dto.response.TutorOptionResponse;
import com.tcs.module.center.entity.CenterTutorMembership;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import com.tcs.module.center.enums.RecruitmentPostStatus;
import com.tcs.module.center.repository.CenterTutorMembershipRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.center.service.CenterService;
import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.CooperationContractSigned;
import com.tcs.module.center.dto.request.SaveContractTemplateRequest;
import com.tcs.module.center.dto.response.ContractTemplateResponse;
import com.tcs.module.contract.entity.ContractTemplate;
import com.tcs.module.contract.enums.ContractTemplateStatus;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import org.springframework.context.event.EventListener;

@Service
@RequiredArgsConstructor
public class CenterServiceImpl implements CenterService {

    private final AuthHelper authHelper;
    private final RecruitmentPostRepository recruitmentPostRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final CenterTutorMembershipRepository membershipRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final LocationRepository locationRepository;
    private final ProvinceRepository provinceRepository;
    private final CategoryRepository categoryRepository;
    private final GradeRepository gradeRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
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
    private final SystemParameterRepository systemParameterRepository;
    private final ClassRequestStore classRequestStore;
    private final ContractService contractService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final com.tcs.module.profile.service.CccdService cccdService;

    private static final DateTimeFormatter D_MM = DateTimeFormatter.ofPattern("dd/MM");

    /** Liên kết tin tuyển dụng -> lớp, lưu trong system_parameters để khỏi cần cột/migration mới. */
    private static final String RECRUIT_CLASS_PREFIX = "recruitclass:"; // recruitclass:{recruitmentId} -> classId
    /** Loại lớp (EXTERNAL/SELF), lưu trong system_parameters để khỏi cần cột/migration mới. */
    private static final String CLASS_ORIGIN_PREFIX = "classorigin:"; // classorigin:{classId} -> EXTERNAL|SELF
    private static final String ORIGIN_EXTERNAL = "EXTERNAL";
    private static final String ORIGIN_SELF = "SELF";
    /** Mẫu hợp đồng đã chọn cho lớp: classtpl:{classId} -> templateId. */
    private static final String CLASS_TEMPLATE_PREFIX = "classtpl:";
    /** Loại của mẫu hợp đồng: tpltype:{templateId} -> RECRUITMENT|CLASS. */
    private static final String TEMPLATE_TYPE_PREFIX = "tpltype:";
    private static final String TEMPLATE_TYPE_RECRUITMENT = "RECRUITMENT";
    private static final String TEMPLATE_TYPE_CLASS = "CLASS";

    // ===================== Tin tuyển gia sư — phía gia sư / công khai =====================

    /** Tin tuyển dụng đăng quá số ngày này sẽ tự động gỡ về nháp. */
    private static final int RECRUIT_MAX_ACTIVE_DAYS = 30;
    /** Số ngày mở ghi danh cho lớp tự tạo (BF-04 bước 5). */
    private static final int CLASS_ENROLLMENT_DAYS = 30;

    /** Gỡ các tin đang mở đã đăng quá {@value #RECRUIT_MAX_ACTIVE_DAYS} ngày về trạng thái nháp. */
    private void autoRevertStalePosts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RECRUIT_MAX_ACTIVE_DAYS);
        List<RecruitmentPost> stale = recruitmentPostRepository
                .findByStatusAndPublishedAtBefore(RecruitmentPostStatus.ACTIVE, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (RecruitmentPost post : stale) {
            post.setStatus(RecruitmentPostStatus.DRAFT);
            post.setPublishedAt(null);
        }
        recruitmentPostRepository.saveAll(stale);
    }

    @Override
    @Transactional
    public List<RecruitmentPostResponse> listOpenRecruitmentPosts() {
        autoRevertStalePosts();
        return recruitmentPostRepository
                .findByStatusOrderByPublishedAtDesc(RecruitmentPostStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request) {
        Tutor tutor = requireTutor();
        // Mức 1: chặn cứng — chỉ gia sư đã được admin xác minh mới được ứng tuyển.
        // Chặn ở service để caller gọi API trực tiếp cũng không lách được.
        if (tutor.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
            throw new VerificationRequiredException(
                    "Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển.");
        }
        RecruitmentPost post = findPost(recruitmentId);
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Tin tuyển dụng chưa mở hoặc đã đóng");
        }
        // Mỗi gia sư chỉ nộp một đơn cho mỗi tin.
        recruitmentApplicationRepository
                .findFirstByRecruitmentPost_RecruitmentIdAndTutor_TutorId(recruitmentId, tutor.getTutorId())
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Bạn đã ứng tuyển tin này rồi");
                });
        RecruitmentApplication application = new RecruitmentApplication();
        application.setRecruitmentPost(post);
        application.setTutor(tutor);
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(RecruitmentApplicationStatus.APPLIED);
        recruitmentApplicationRepository.save(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplicationResponse> listMyApplications() {
        Tutor tutor = requireTutor();
        return recruitmentApplicationRepository
                .findByTutor_TutorIdOrderByAppliedAtDesc(tutor.getTutorId())
                .stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    // ===================== Phía trung tâm =====================

    @Override
    @Transactional
    public List<RecruitmentPostResponse> listMyRecruitmentPosts() {
        TutorCenter center = requireCenter();
        autoRevertStalePosts();
        return recruitmentPostRepository
                .findByCenter_CenterIdOrderByCreatedAtDesc(center.getCenterId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecruitmentPostResponse createRecruitmentPost(SaveRecruitmentPostRequest request) {
        TutorCenter center = requireCenter();
        requireVerifiedCenter(center);
        validate(request);
        if (request.getClassId() != null) {
            validateClassRecruitable(request.getClassId());
        }
        RecruitmentPost post = new RecruitmentPost();
        post.setCenter(center);
        post.setStatus(RecruitmentPostStatus.DRAFT); // tin mới luôn là nháp
        applyFields(post, request);
        RecruitmentPost saved = recruitmentPostRepository.save(post);
        savePostClassLink(saved.getRecruitmentId(), request.getClassId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecruitmentPostResponse updateRecruitmentPost(
            Long recruitmentId, SaveRecruitmentPostRequest request) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post);
        if (post.getStatus() == RecruitmentPostStatus.CLOSED) {
            throw new IllegalArgumentException("Tin đã đóng, không thể chỉnh sửa.");
        }
        // Cho phép sửa tin (kể cả đã đăng) đến khi có gia sư ứng tuyển; có đơn rồi thì khoá sửa.
        if (recruitmentApplicationRepository.countByRecruitmentPost_RecruitmentId(recruitmentId) > 0) {
            throw new IllegalArgumentException(
                    "Đã có gia sư ứng tuyển, không thể chỉnh sửa tin nữa.");
        }
        validate(request);
        if (request.getClassId() != null) {
            validateClassRecruitable(request.getClassId());
        }
        applyFields(post, request);
        RecruitmentPost saved = recruitmentPostRepository.save(post);
        savePostClassLink(saved.getRecruitmentId(), request.getClassId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId) {
        TutorCenter center = requireCenter();
        requireVerifiedCenter(center);
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post);
        if (post.getStatus() != RecruitmentPostStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ tin ở trạng thái nháp mới có thể đăng tải");
        }
        // Kiểm tra lại tại thời điểm đăng: lớp gắn kèm vẫn phải là lớp "theo yêu cầu" và chưa có
        // gia sư chính (lớp có thể đã được gán gia sư sau khi tạo/sửa nháp).
        findPostClassId(recruitmentId).ifPresent(this::validateClassRecruitable);
        post.setStatus(RecruitmentPostStatus.ACTIVE);
        post.setPublishedAt(LocalDateTime.now());
        RecruitmentPost saved = recruitmentPostRepository.save(post);
        auditLogService.record(center.getUser().getUserId(), "PUBLISH_RECRUITMENT_POST", "RecruitmentPost", saved.getRecruitmentId(), null, null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecruitmentPostResponse closeRecruitmentPost(Long recruitmentId) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post);
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ tin đang mở mới có thể đóng");
        }
        post.setStatus(RecruitmentPostStatus.CLOSED);
        post.setClosedAt(LocalDateTime.now());
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplicationResponse> listApplications(Long recruitmentId) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post); // chỉ xem đơn của tin do mình đăng
        return recruitmentApplicationRepository
                .findByRecruitmentPost_RecruitmentIdOrderByAppliedAtDesc(recruitmentId)
                .stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecruitmentApplicationResponse decideApplication(
            Long recruitmentAppId, boolean approve, Long contractTemplateId, String contractContent) {
        requireCenter();
        RecruitmentApplication application = recruitmentApplicationRepository
                .findById(recruitmentAppId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
        requireOwner(application.getRecruitmentPost());
        if (application.getStatus() != RecruitmentApplicationStatus.APPLIED) {
            throw new IllegalArgumentException("Đơn này đã được xử lý");
        }
        // BF-03: duyệt -> PASSED (chờ ký hợp đồng), CHƯA phải HIRED. Chỉ khi ký xong mới HIRED.
        application.setStatus(
                approve ? RecruitmentApplicationStatus.PASSED : RecruitmentApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        RecruitmentApplication saved = recruitmentApplicationRepository.save(application);
        if (approve) {
            // BF-03 bước 7: duyệt -> CHỈ tạo thỏa thuận hợp tác (e-contract) để gia sư ký.
            // CHƯA tạo thành viên: gia sư chưa ký thì CHƯA phải gia sư của trung tâm.
            // Khi ký OTP xong (bước 8) mới HIRED + tạo/kích hoạt thành viên ACTIVE + gán lớp
            // + đóng tin (bước 9-10) — xử lý trong onCooperationContractSigned.
            contractService.generateCooperationContract(
                    saved.getRecruitmentAppId(), contractTemplateId, contractContent);
        }
        return toApplicationResponse(saved);
    }

    // ===================== Quản lý danh sách gia sư của trung tâm =====================

    @Override
    @Transactional(readOnly = true)
    public List<CenterTutorResponse> listMyTutors() {
        TutorCenter center = requireCenter();
        return membershipRepository
                .findByCenter_CenterIdOrderByJoinedAtDesc(center.getCenterId())
                .stream()
                .map(this::toTutorResponse)
                .toList();
    }

    @Override
    @Transactional
    public CenterTutorResponse updateMembershipStatus(
            Long membershipId, CenterTutorMembershipStatus status) {
        TutorCenter center = requireCenter();
        if (status == null) {
            throw new IllegalArgumentException("Thiếu trạng thái cần cập nhật");
        }
        CenterTutorMembership membership = membershipRepository
                .findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên"));
        // Chỉ trung tâm sở hữu mới được thao tác.
        if (!membership.getCenter().getCenterId().equals(center.getCenterId())) {
            throw new ForbiddenException("Bạn không có quyền với thành viên này");
        }
        membership.setStatus(status);
        return toTutorResponse(membershipRepository.save(membership));
    }

    /** Tạo mới hoặc khôi phục (ACTIVE) membership khi trung tâm nhận gia sư qua đơn ứng tuyển. */
    private void addOrReactivateMembership(
            RecruitmentApplication application, CenterTutorMembershipStatus status) {
        TutorCenter center = application.getRecruitmentPost().getCenter();
        Tutor tutor = application.getTutor();
        CenterTutorMembership membership = membershipRepository
                .findFirstByCenter_CenterIdAndTutor_TutorId(center.getCenterId(), tutor.getTutorId())
                .orElseGet(CenterTutorMembership::new);
        membership.setCenter(center);
        membership.setTutor(tutor);
        membership.setRecruitmentApplication(application);
        membership.setStatus(status);
        if (membership.getJoinedAt() == null) {
            membership.setJoinedAt(LocalDateTime.now());
        }
        membershipRepository.save(membership);
    }

    /**
     * BF-03 bước 9-10: gia sư đã ký thỏa thuận hợp tác -> kích hoạt thành viên ACTIVE,
     * tự gán vào lớp nếu tin gắn lớp, và đóng tin khi đủ số gia sư đã ký.
     */
    @EventListener
    @Transactional
    public void onCooperationContractSigned(CooperationContractSigned event) {
        RecruitmentApplication app = recruitmentApplicationRepository
                .findById(event.recruitmentApplicationId())
                .orElse(null);
        if (app == null) {
            return;
        }
        // BF-03 bước 9: ký xong -> đơn chính thức HIRED ("Đã được nhận").
        app.setStatus(RecruitmentApplicationStatus.HIRED);
        recruitmentApplicationRepository.save(app);
        // Ký xong mới CHÍNH THỨC là gia sư của trung tâm: tạo/kích hoạt thành viên ACTIVE.
        addOrReactivateMembership(app, CenterTutorMembershipStatus.ACTIVE);
        // Nếu tin gắn với lớp -> tự gán gia sư vào lớp đó.
        findPostClassId(app.getRecruitmentPost().getRecruitmentId())
                .flatMap(tutoringClassRepository::findById)
                .ifPresent(cls -> autoAssignHiredTutor(cls, app.getTutor()));
        // Đóng tin khi đủ số gia sư đã ký hợp đồng.
        closePostIfEnoughSigned(app.getRecruitmentPost());
    }

    private void closePostIfEnoughSigned(RecruitmentPost post) {
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            return;
        }
        Integer max = post.getMaxPositions();
        if (max == null || max <= 0) {
            return;
        }
        long signed = membershipRepository
                .findByCenter_CenterIdOrderByJoinedAtDesc(post.getCenter().getCenterId()).stream()
                .filter(m -> m.getStatus() == CenterTutorMembershipStatus.ACTIVE)
                .filter(m -> m.getRecruitmentApplication() != null
                        && m.getRecruitmentApplication().getRecruitmentPost() != null
                        && post.getRecruitmentId().equals(
                                m.getRecruitmentApplication().getRecruitmentPost().getRecruitmentId()))
                .count();
        if (signed >= max) {
            post.setStatus(RecruitmentPostStatus.CLOSED);
            post.setClosedAt(LocalDateTime.now());
            recruitmentPostRepository.save(post);
        }
    }

    private CenterTutorResponse toTutorResponse(CenterTutorMembership membership) {
        Tutor tutor = membership.getTutor();
        Long centerId = membership.getCenter().getCenterId();
        List<CenterTutorResponse.AppliedPost> appliedPosts = recruitmentApplicationRepository
                .findByTutor_TutorIdAndRecruitmentPost_Center_CenterIdOrderByAppliedAtDesc(
                        tutor.getTutorId(), centerId)
                .stream()
                .map(app -> CenterTutorResponse.AppliedPost.builder()
                        .recruitmentId(app.getRecruitmentPost().getRecruitmentId())
                        .postTitle(app.getRecruitmentPost().getTitle())
                        .applicationStatus(app.getStatus())
                        .appliedAt(app.getAppliedAt())
                        .build())
                .toList();
        return CenterTutorResponse.builder()
                .membershipId(membership.getMembershipId())
                .tutorId(tutor.getTutorId())
                .tutorName(tutor.getFullName())
                .tutorPhone(tutor.getPhone())
                .tutorAvatar(tutor.getAvatar())
                .experienceYears(tutor.getExperienceYears())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(
                        tutor.getVerificationStatus() != null
                                ? tutor.getVerificationStatus().name() : null)
                .joinedAt(membership.getJoinedAt())
                .status(membership.getStatus())
                .appliedPosts(appliedPosts)
                .build();
    }

    // ===================== Helper =====================

    private void validate(SaveRecruitmentPostRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Tiêu đề là bắt buộc");
        }
        if (!StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Mô tả công việc là bắt buộc");
        }
        if (request.getMaxPositions() != null && request.getMaxPositions() <= 0) {
            throw new IllegalArgumentException("Số lượng cần tuyển phải là số nguyên dương");
        }
        if (request.getRequiredExperience() != null && request.getRequiredExperience() < 0) {
            throw new IllegalArgumentException("Số năm kinh nghiệm không được âm");
        }
        // Địa điểm là tuỳ chọn, nhưng đã nhập địa chỉ thì phải có tỉnh/thành (Location cần cả hai).
        if (StringUtils.hasText(request.getAddressDetail())
                && !StringUtils.hasText(request.getProvinceName())) {
            throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố cho địa chỉ đã nhập");
        }
    }

    private void applyFields(RecruitmentPost post, SaveRecruitmentPostRequest request) {
        post.setTitle(request.getTitle().trim());
        post.setDescription(request.getDescription().trim());
        post.setRequirements(request.getRequirements());
        post.setBenefits(request.getBenefits());
        post.setRequiredExperience(
                request.getRequiredExperience() != null ? request.getRequiredExperience() : 0);
        post.setMaxPositions(request.getMaxPositions() != null ? request.getMaxPositions() : 1);
        post.setSubject(
                StringUtils.hasText(request.getSubjectName())
                        ? resolveOrCreateSubject(request.getSubjectName())
                        : null);
        // Tin gắn với một lớp -> địa điểm làm việc lấy cố định theo địa điểm của lớp,
        // bỏ qua địa điểm client gửi lên. Tin tuyển chung -> lấy theo dữ liệu nhập.
        if (request.getClassId() != null) {
            TutoringClass linked = findClass(request.getClassId());
            requireOwner(linked);
            post.setLocation(linked.getLocation());
        } else {
            post.setLocation(
                    StringUtils.hasText(request.getProvinceName())
                                    && StringUtils.hasText(request.getAddressDetail())
                            ? resolveOrCreateLocation(
                                    request.getProvinceName(),
                                    request.getWardName(),
                                    request.getAddressDetail())
                            : null);
        }

    }

    // ===================== UC-14-B: Manage Classes (Tutor Center) =====================

    @Override
    @Transactional
    public List<CenterClassResponse> listMyClasses() {
        requireCenter();
        autoCloseExpiredClasses();
        return tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId()).stream()
                .map(this::toClassResponse)
                .toList();
    }

    /**
     * BF-04 bước 5/9: lớp đang mở ghi danh quá hạn (enrollmentDeadline) -> tự đóng ghi danh.
     * Đủ gia sư + đủ sĩ số tối thiểu -> MATCHED; ngược lại -> ENROLLMENT_CLOSED (trung tâm xử lý tiếp).
     */
    private void autoCloseExpiredClasses() {
        LocalDate today = LocalDate.now();
        for (TutoringClass c : tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId())) {
            if (c.getStatus() != TutoringClassStatus.OPEN
                    || c.getEnrollmentDeadline() == null
                    || !c.getEnrollmentDeadline().isBefore(today)) {
                continue;
            }
            long enrolled = classStudentRepository
                    .countByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED);
            int required = c.getMinStudents() != null ? c.getMinStudents() : 1;
            boolean hasMain = classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                            c.getClassId(), ClassAssignmentStatus.ACTIVE)
                    .isPresent();
            c.setStatus(hasMain && enrolled >= required
                    ? TutoringClassStatus.MATCHED : TutoringClassStatus.ENROLLMENT_CLOSED);
            tutoringClassRepository.save(c);
        }
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
        requireVerifiedCenter(center);
        validate(request, true);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(center.getUser());
        tutoringClass.setCenter(center);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.DRAFT); // BR-05
        applyFields(tutoringClass, request);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        saveClassOrigin(saved.getClassId(), request.getOriginType());
        saveClassTemplate(saved.getClassId(), request.getContractTemplateId());
        saveClassTerms(saved.getClassId(), request.getContractContent());

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
        // Cho phép sửa lớp đến khi có học viên đăng ký; có người đăng ký rồi thì khoá sửa.
        if (classStudentRepository.existsByTutoringClass_ClassId(classId)) {
            throw new IllegalArgumentException(
                    "Đã có học viên đăng ký, không thể chỉnh sửa lớp nữa.");
        }
        validate(request, false);
        applyFields(tutoringClass, request);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        saveClassOrigin(saved.getClassId(), request.getOriginType());
        saveClassTemplate(saved.getClassId(), request.getContractTemplateId());
        saveClassTerms(saved.getClassId(), request.getContractContent());

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
        boolean external = ORIGIN_EXTERNAL.equals(findClassOrigin(classId));
        if (external) {
            // Lớp "theo yêu cầu" đã có sẵn học sinh -> không mở ghi danh, đăng tải để bố trí gia sư.
            tutoringClass.setStatus(TutoringClassStatus.ENROLLMENT_CLOSED);
        } else {
            // Lớp "tự tạo": phải gán ĐỦ 2 gia sư (gia sư chính + gia sư phụ) TRƯỚC,
            // rồi mới được mở ghi danh (đăng tải) cho học sinh.
            boolean hasMain = classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                            classId, ClassAssignmentStatus.ACTIVE)
                    .isPresent();
            if (!hasMain) {
                throw new IllegalArgumentException(
                        "Cần gán gia sư chính cho lớp trước khi mở ghi danh (đăng tải).");
            }
            boolean hasAssistant = substitutionService.findAssistant(classId).isPresent();
            if (!hasAssistant) {
                throw new IllegalArgumentException(
                        "Cần gán đủ 2 gia sư (gia sư chính + gia sư phụ) trước khi mở ghi danh.");
            }
            tutoringClass.setStatus(TutoringClassStatus.OPEN);
            // BF-04 bước 5: mở ghi danh trong 30 ngày kể từ khi đăng.
            tutoringClass.setEnrollmentDeadline(LocalDate.now().plusDays(CLASS_ENROLLMENT_DAYS));
        }
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public CenterClassResponse closeEnrollment(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        // Chỉ đóng ghi danh lớp đang mở.
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Chỉ lớp đang mở tuyển sinh mới có thể đóng ghi danh.");
        }
        // Điều kiện đóng tay: đủ số học sinh tối thiểu (min_students; trống thì cần ≥ 1).
        long enrolled = classStudentRepository
                .countByTutoringClass_ClassIdAndStatus(classId, ClassStudentStatus.ENROLLED);
        int required = tutoringClass.getMinStudents() != null ? tutoringClass.getMinStudents() : 1;
        if (enrolled < required) {
            throw new IllegalArgumentException(
                    "Chưa đủ học sinh tối thiểu để đóng lớp (" + enrolled + "/" + required + ").");
        }
        // Đã gán gia sư trước (lớp tự tạo) + đủ học sinh -> đã ghép (MATCHED). Chưa có gia sư -> chỉ đóng ghi danh.
        boolean hasMain = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .isPresent();
        tutoringClass.setStatus(
                hasMain ? TutoringClassStatus.MATCHED : TutoringClassStatus.ENROLLMENT_CLOSED);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public CenterClassResponse activateClass(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        // BF-04 bước 10: kích hoạt lớp (bắt đầu học) từ đã ghép / đã đóng ghi danh -> IN_PROGRESS.
        if (tutoringClass.getStatus() != TutoringClassStatus.MATCHED
                && tutoringClass.getStatus() != TutoringClassStatus.ENROLLMENT_CLOSED) {
            throw new IllegalArgumentException(
                    "Chỉ kích hoạt lớp đã đóng ghi danh / đã ghép gia sư.");
        }
        boolean hasMain = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .isPresent();
        if (!hasMain) {
            throw new IllegalArgumentException("Cần gán gia sư chính trước khi kích hoạt lớp.");
        }
        long enrolled = classStudentRepository
                .countByTutoringClass_ClassIdAndStatus(classId, ClassStudentStatus.ENROLLED);
        int required = tutoringClass.getMinStudents() != null ? tutoringClass.getMinStudents() : 1;
        if (enrolled < required) {
            throw new IllegalArgumentException(
                    "Chưa đủ sĩ số tối thiểu để kích hoạt lớp (" + enrolled + "/" + required + ").");
        }
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorOptionResponse> listTutors(Long classId) {
        TutorCenter center = requireCenter();
        // Nếu có classId: nạp lớp (đảm bảo quyền sở hữu) để đánh dấu gia sư trùng lịch.
        TutoringClass target = null;
        if (classId != null) {
            target = findClass(classId);
            requireOwner(target);
        }
        final TutoringClass targetClass = target;
        // Chỉ gán được gia sư trong danh sách gia sư (thành viên ACTIVE) của chính trung tâm.
        return membershipRepository
                .findByCenter_CenterIdOrderByJoinedAtDesc(center.getCenterId()).stream()
                .filter(m -> m.getStatus() == CenterTutorMembershipStatus.ACTIVE)
                .map(CenterTutorMembership::getTutor)
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
        TutorCenter center = requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        requireStaffable(tutoringClass);
        if (tutorId == null) {
            throw new IllegalArgumentException("Vui lòng chọn gia sư");
        }
        requireActiveCenterTutor(center.getCenterId(), tutorId);
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

        assignMainTutorInternal(tutoringClass, tutor);
        // Đã đóng ghi danh (đủ học sinh) mà gán gia sư -> đã ghép (MATCHED).
        // Còn đang tuyển học sinh (DRAFT/OPEN) -> giữ nguyên, MATCHED khi đóng ghi danh.
        if (tutoringClass.getStatus() == TutoringClassStatus.ENROLLMENT_CLOSED) {
            tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        }
        tutoringClassRepository.save(tutoringClass);
        // Đã có gia sư -> tự đóng tin tuyển gia sư đang mở của lớp (không cần tuyển nữa).
        closeRecruitmentPostsForClass(classId);
        return toClassResponse(tutoringClass);
    }

    /** Cho gán gia sư khi lớp chưa kết thúc (nháp / đang tuyển / đã đóng ghi danh / đã ghép). */
    private void requireStaffable(TutoringClass tutoringClass) {
        switch (tutoringClass.getStatus()) {
            case DRAFT, OPEN, ENROLLMENT_CLOSED, MATCHED -> {
                // ok
            }
            default -> throw new IllegalArgumentException("Lớp này không thể gán gia sư nữa.");
        }
    }

    /**
     * Chỉ được gán gia sư là thành viên ĐANG HOẠT ĐỘNG (ACTIVE) trong danh sách gia sư của
     * chính trung tâm. Chặn việc gọi API thủ công với tutorId bất kỳ (gia sư ngoài / đã rời trung tâm).
     */
    private void requireActiveCenterTutor(Long centerId, Long tutorId) {
        CenterTutorMembership membership = membershipRepository
                .findFirstByCenter_CenterIdAndTutor_TutorId(centerId, tutorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gia sư này không thuộc danh sách gia sư của trung tâm."));
        if (membership.getStatus() != CenterTutorMembershipStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Gia sư này không còn là thành viên đang hoạt động của trung tâm.");
        }
    }

    /**
     * Gán gia sư chính cho lớp (không kiểm tra quyền/trùng lịch — caller tự đảm bảo).
     * Kết thúc lượt gán cũ rồi tạo/khôi phục lượt gán mới ACTIVE.
     */
    private void assignMainTutorInternal(TutoringClass tutoringClass, Tutor tutor) {
        Long classId = tutoringClass.getClassId();
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
    }

    /**
     * Sau khi HIRE ứng viên của tin gắn với lớp: tự gán gia sư vào lớp.
     * Ưu tiên gia sư chính (nếu lớp chưa có), kế đó gia sư phụ; đủ cả hai thì chỉ giữ thành viên.
     * Bỏ qua nếu trùng lịch để không làm hỏng thao tác duyệt (trung tâm gán tay sau).
     */
    private void autoAssignHiredTutor(TutoringClass tutoringClass, Tutor tutor) {
        if (tutoringClass == null) {
            return;
        }
        // Chỉ tự gán khi lớp chưa kết thúc (cùng quy tắc với gán tay).
        switch (tutoringClass.getStatus()) {
            case DRAFT, OPEN, ENROLLMENT_CLOSED, MATCHED -> {
                // ok
            }
            default -> {
                return;
            }
        }
        Long classId = tutoringClass.getClassId();
        ClassAssignment main = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .orElse(null);
        if (main == null) {
            if (findScheduleConflict(tutoringClass, tutor.getTutorId()) != null) {
                return; // trùng lịch -> để trung tâm gán tay
            }
            assignMainTutorInternal(tutoringClass, tutor);
            // Đã đóng ghi danh mà mới có gia sư -> đã ghép; còn đang tuyển học sinh thì giữ nguyên.
            if (tutoringClass.getStatus() == TutoringClassStatus.ENROLLMENT_CLOSED) {
                tutoringClass.setStatus(TutoringClassStatus.MATCHED);
            }
            tutoringClassRepository.save(tutoringClass);
            closeRecruitmentPostsForClass(classId);
            return;
        }
        // Đã có gia sư chính -> thử gán làm gia sư phụ (phải khác chính, chưa có phụ).
        if (main.getTutor().getTutorId().equals(tutor.getTutorId())) {
            return;
        }
        if (substitutionService.findAssistant(classId).isEmpty()) {
            substitutionService.assignAssistant(classId, tutor.getTutorId());
        }
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
        // Gỡ gia sư chính -> lớp quay lại "đã đóng ghi danh" (chưa ghép).
        if (tutoringClass.getStatus() == TutoringClassStatus.MATCHED) {
            tutoringClass.setStatus(TutoringClassStatus.ENROLLMENT_CLOSED);
            tutoringClassRepository.save(tutoringClass);
        }
        auditLogService.record(authHelper.currentUserId(), "UNASSIGN_TUTOR", "TutoringClass", classId, null, null);
        return toClassResponse(tutoringClass);
    }

    @Override
    @Transactional
    public CenterClassResponse assignAssistant(Long classId, Long tutorId) {
        TutorCenter center = requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass);
        requireStaffable(tutoringClass);
        if (tutorId == null) {
            throw new IllegalArgumentException("Vui lòng chọn gia sư phụ");
        }
        requireActiveCenterTutor(center.getCenterId(), tutorId);
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
        // Số học sinh tối thiểu (nếu có): số nguyên dương và không lớn hơn tối đa.
        if (request.getMinStudents() != null) {
            if (request.getMinStudents() <= 0) {
                throw new IllegalArgumentException("Số học sinh tối thiểu phải là số nguyên dương");
            }
            if (request.getMinStudents() > request.getMaxStudents()) {
                throw new IllegalArgumentException(
                        "Số học sinh tối thiểu không được lớn hơn số học sinh tối đa");
            }
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
        tutoringClass.setMinStudents(request.getMinStudents());
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
                .minStudents(c.getMinStudents())
                .enrolledCount(students.size())
                .originType(findClassOrigin(c.getClassId()))
                .contractTemplateId(findClassTemplateId(c.getClassId()))
                .contractContent(findClassTerms(c.getClassId()))
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

    /**
     * Mức 1: chặn cứng — chỉ trung tâm đã được admin xác minh mới được tạo lớp.
     * Ném {@link VerificationRequiredException} (code VERIFICATION_REQUIRED) để frontend điều hướng
     * sang trang Xác minh thay vì hiện lỗi cấm truy cập.
     */
    private void requireVerifiedCenter(TutorCenter center) {
        if (center.getVerificationStatus() != ProfileVerificationStatus.VERIFIED) {
            throw new VerificationRequiredException(
                    "Trung tâm của bạn cần được xác minh trước khi thực hiện thao tác này.");
        }
    }

    private Tutor requireTutor() {
        authHelper.requireRole(UserRole.TUTOR);
        return tutorRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
    }

    /** Chỉ trung tâm sở hữu tin mới được thao tác. */
    private void requireOwner(RecruitmentPost post) {
        if (!post.getCenter().getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Bạn không có quyền với tin tuyển dụng này");
        }
    }

    private RecruitmentPost findPost(Long recruitmentId) {
        return recruitmentPostRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
    }

    // ---- Liên kết tin tuyển dụng <-> lớp qua system_parameters (không dùng cột/migration) ----

    private String recruitClassKey(Long recruitmentId) {
        return RECRUIT_CLASS_PREFIX + recruitmentId;
    }

    /** Lưu/cập nhật/xoá liên kết tin -> lớp. classId=null -> xoá liên kết (tin tuyển chung). */
    private void savePostClassLink(Long recruitmentId, Long classId) {
        String key = recruitClassKey(recruitmentId);
        if (classId == null) {
            systemParameterRepository.findByParamKey(key)
                    .ifPresent(systemParameterRepository::delete);
            return;
        }
        // Chỉ nhận lớp thuộc chính trung tâm này.
        TutoringClass linked = findClass(classId);
        requireOwner(linked);
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(String.valueOf(classId));
        param.setDescription("Lớp mà tin tuyển dụng này tuyển cho");
        systemParameterRepository.save(param);
    }

    private Optional<Long> findPostClassId(Long recruitmentId) {
        return systemParameterRepository.findByParamKey(recruitClassKey(recruitmentId))
                .map(p -> {
                    try {
                        return Long.valueOf(p.getParamValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
    }

    /**
     * Chỉ lớp "theo yêu cầu" mới được đăng tin tuyển gia sư, và chỉ khi lớp chưa có gia sư chính.
     * Lớp "tự tạo" chỉ gán gia sư từ danh sách trung tâm.
     */
    private void validateClassRecruitable(Long classId) {
        TutoringClass cls = findClass(classId);
        requireOwner(cls);
        if (ORIGIN_EXTERNAL.equals(findClassOrigin(classId))) {
            // ok - lớp theo yêu cầu được đăng tin
        } else {
            throw new IllegalArgumentException(
                    "Lớp tự tạo chỉ gán gia sư từ danh sách trung tâm, không đăng tin tuyển.");
        }
        boolean hasMain = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .isPresent();
        if (hasMain) {
            throw new IllegalArgumentException("Lớp đã có gia sư — không cần đăng tin tuyển.");
        }
    }

    /** Đã gán được gia sư cho lớp -> tự đóng các tin tuyển gia sư đang mở gắn với lớp đó. */
    private void closeRecruitmentPostsForClass(Long classId) {
        for (SystemParameter p :
                systemParameterRepository.findByParamKeyStartingWith(RECRUIT_CLASS_PREFIX)) {
            Long linked;
            try {
                linked = Long.valueOf(p.getParamValue().trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (!classId.equals(linked)) {
                continue;
            }
            Long recruitmentId;
            try {
                recruitmentId = Long.valueOf(p.getParamKey().substring(RECRUIT_CLASS_PREFIX.length()));
            } catch (NumberFormatException e) {
                continue;
            }
            recruitmentPostRepository.findById(recruitmentId).ifPresent(post -> {
                if (post.getStatus() == RecruitmentPostStatus.ACTIVE) {
                    post.setStatus(RecruitmentPostStatus.CLOSED);
                    post.setClosedAt(LocalDateTime.now());
                    recruitmentPostRepository.save(post);
                }
            });
        }
    }

    // ---- Loại lớp (EXTERNAL/SELF) qua system_parameters (không dùng cột/migration) ----

    private void saveClassOrigin(Long classId, String originType) {
        String value = ORIGIN_EXTERNAL.equalsIgnoreCase(originType) ? ORIGIN_EXTERNAL : ORIGIN_SELF;
        String key = CLASS_ORIGIN_PREFIX + classId;
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(value);
        param.setDescription("Loại lớp: EXTERNAL (yêu cầu ngoài) / SELF (tự tạo)");
        systemParameterRepository.save(param);
    }

    private String findClassOrigin(Long classId) {
        return systemParameterRepository.findByParamKey(CLASS_ORIGIN_PREFIX + classId)
                .map(SystemParameter::getParamValue)
                .orElse(ORIGIN_SELF);
    }

    // ---- Mẫu hợp đồng đã chọn cho lớp (qua system_parameters) ----

    private void saveClassTemplate(Long classId, Long templateId) {
        String key = CLASS_TEMPLATE_PREFIX + classId;
        if (templateId == null) {
            // null = không thay đổi mẫu đã chọn (tránh sửa lớp làm mất mẫu khi form không gửi lại).
            return;
        }
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(String.valueOf(templateId));
        param.setDescription("Mẫu hợp đồng đã chọn cho lớp");
        systemParameterRepository.save(param);
    }

    /** Mẫu hợp đồng đã chọn cho lớp (classtpl:{classId}), để đổ lại form khi sửa. */
    private Long findClassTemplateId(Long classId) {
        return systemParameterRepository.findByParamKey(CLASS_TEMPLATE_PREFIX + classId)
                .map(p -> {
                    try {
                        return Long.valueOf(p.getParamValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /** Nội dung điều khoản HĐ học viên đã lưu cho lớp (classterms:{classId}). */
    private String findClassTerms(Long classId) {
        return systemParameterRepository.findByParamKey("classterms:" + classId)
                .map(SystemParameter::getParamValue)
                .orElse(null);
    }

    /** Lưu nội dung điều khoản HĐ học viên center nhập khi tạo/sửa lớp (classterms:{classId}). */
    private void saveClassTerms(Long classId, String content) {
        if (content == null) {
            // null = không thay đổi (form không gửi lại thì giữ nội dung cũ).
            return;
        }
        String key = "classterms:" + classId;
        if (content.isBlank()) {
            // chuỗi rỗng = xoá nội dung đã lưu -> quay về dùng mẫu.
            systemParameterRepository.findByParamKey(key).ifPresent(systemParameterRepository::delete);
            return;
        }
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(content);
        param.setDescription("Nội dung điều khoản HĐ học viên đã nhập cho lớp");
        systemParameterRepository.save(param);
    }

    private RecruitmentPostResponse toResponse(RecruitmentPost post) {
        Location location = post.getLocation();
        Subject subject = post.getSubject();
        TutoringClass linked = findPostClassId(post.getRecruitmentId())
                .flatMap(tutoringClassRepository::findById)
                .orElse(null);
        return RecruitmentPostResponse.builder()
                .recruitmentId(post.getRecruitmentId())
                .centerId(post.getCenter().getCenterId())
                .centerName(post.getCenter().getCompanyName())
                .classId(linked != null ? linked.getClassId() : null)
                .classTitle(linked != null ? linked.getTitle() : null)
                .title(post.getTitle())
                .description(post.getDescription())
                .requirements(post.getRequirements())
                .benefits(post.getBenefits())
                .requiredExperience(post.getRequiredExperience())
                .maxPositions(post.getMaxPositions())
                .subjectId(subject != null ? subject.getSubjectId() : null)
                .subjectName(subject != null ? subject.getSubjectName() : null)
                .locationId(location != null ? location.getLocationId() : null)
                .locationLabel(locationLabel(location))
                .provinceName(location != null && location.getProvince() != null
                        ? location.getProvince().getProvinceName() : null)
                .wardName(location != null ? location.getWardName() : null)
                .addressDetail(location != null ? location.getAddressLine() : null)
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .closedAt(post.getClosedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .applicationCount(
                        recruitmentApplicationRepository.countByRecruitmentPost_RecruitmentId(
                                post.getRecruitmentId()))
                .build();
    }

    private RecruitmentApplicationResponse toApplicationResponse(RecruitmentApplication application) {
        Tutor tutor = application.getTutor();
        RecruitmentPost post = application.getRecruitmentPost();
        return RecruitmentApplicationResponse.builder()
                .recruitmentAppId(application.getRecruitmentAppId())
                .recruitmentId(post.getRecruitmentId())
                .postTitle(post.getTitle())
                .centerName(post.getCenter().getCompanyName())
                .tutorId(tutor.getTutorId())
                .tutorName(tutor.getFullName())
                .tutorPhone(tutor.getPhone())
                .tutorAvatar(tutor.getAvatar())
                .experienceYears(tutor.getExperienceYears())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(
                        tutor.getVerificationStatus() != null
                                ? tutor.getVerificationStatus().name() : null)
                .coverLetter(application.getCoverLetter())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .reviewedAt(application.getReviewedAt())
                .certificates(loadCertificates(tutor))
                .build();
    }

    /**
     * Bằng cấp / chứng chỉ đã được xác minh của gia sư (chỉ loại CERTIFICATE) — để trung tâm
     * tham khảo khi duyệt đơn. Chỉ lấy khi hồ sơ TUTOR_PROFILE đã VERIFIED; bỏ qua ảnh CCCD.
     */
    private List<RecruitmentApplicationResponse.CertificateInfo> loadCertificates(Tutor tutor) {
        if (tutor.getUser() == null) {
            return List.of();
        }
        return verificationRequestRepository
                .findFirstByUser_UserIdAndVerificationTypeAndStatusOrderByVerificationIdDesc(
                        tutor.getUser().getUserId(), VerificationType.TUTOR_PROFILE,
                        VerificationStatus.VERIFIED)
                .map(req -> verificationDocumentRepository
                        .findByVerificationRequest_VerificationId(req.getVerificationId()).stream()
                        .filter(doc -> doc.getDocumentType() == VerificationDocumentType.CERTIFICATE)
                        .map(doc -> RecruitmentApplicationResponse.CertificateInfo.builder()
                                .documentType(doc.getDocumentType() == null
                                        ? null : doc.getDocumentType().name())
                                .fileName(doc.getFile().getFileName())
                                .fileUrl(doc.getFile().getFileUrl())
                                .mimeType(doc.getFile().getMimeType())
                                .fileSize(doc.getFile().getFileSize())
                                .build())
                        .toList())
                .orElseGet(List::of);
    }

    // ===== Yêu cầu mở lớp do phụ huynh gửi tới trung tâm =====

    @Override
    @Transactional(readOnly = true)
    public List<ClassRequestResponse> listIncomingClassRequests() {
        TutorCenter center = requireCenter();
        return classRequestStore.findByCenter(center.getCenterId()).stream()
                .map(classRequestStore::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CenterClassResponse acceptClassRequest(String requestId, SaveClassRequest body) {
        TutorCenter center = requireCenter();
        ClassRequestStore.ClassRequestData data = classRequestStore.find(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu mở lớp"));
        if (!center.getCenterId().equals(data.centerId())) {
            throw new ForbiddenException("Không có quyền xử lý yêu cầu này");
        }
        if (!ClassRequestStore.STATUS_PENDING.equals(data.status())) {
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý");
        }

        // Lớp sinh ra từ yêu cầu của phụ huynh luôn cố định là "yêu cầu ngoài" (EXTERNAL),
        // bất kể client gửi originType gì — không cho biến thành lớp tự tạo (SELF).
        body.setOriginType(ORIGIN_EXTERNAL);
        CenterClassResponse classResponse = createClass(body);

        ClassRequestStore.ClassRequestData updated = new ClassRequestStore.ClassRequestData(
                data.requestId(), data.clientUserId(), data.centerId(), data.categoryId(),
                data.note(), data.desiredBudget(), ClassRequestStore.STATUS_ACCEPTED, null, data.createdAt());
        classRequestStore.save(updated);

        return classResponse;
    }

    @Override
    @Transactional
    public void rejectClassRequest(String requestId, String reason) {
        TutorCenter center = requireCenter();
        ClassRequestStore.ClassRequestData data = classRequestStore.find(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu mở lớp"));
        if (!center.getCenterId().equals(data.centerId())) {
            throw new ForbiddenException("Không có quyền xử lý yêu cầu này");
        }
        if (!ClassRequestStore.STATUS_PENDING.equals(data.status())) {
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý");
        }

        ClassRequestStore.ClassRequestData updated = new ClassRequestStore.ClassRequestData(
                data.requestId(), data.clientUserId(), data.centerId(), data.categoryId(),
                data.note(), data.desiredBudget(), ClassRequestStore.STATUS_REJECTED, reason, data.createdAt());
        classRequestStore.save(updated);
    }

    // ===================== Quản lý mẫu hợp đồng =====================

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplateResponse> listContractTemplates() {
        TutorCenter center = requireCenter();
        return contractTemplateRepository.findUsableByCenter(center.getCenterId()).stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Override
    @Transactional
    public ContractTemplateResponse createContractTemplate(SaveContractTemplateRequest request) {
        TutorCenter center = requireCenter();
        // Phải xác minh trung tâm trước khi được tạo mẫu hợp đồng.
        requireVerifiedCenter(center);
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Tên mẫu là bắt buộc");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Nội dung mẫu là bắt buộc");
        }
        ContractTemplate t = new ContractTemplate();
        t.setName(request.getName().trim());
        t.setContent(request.getContent().trim());
        t.setCreatedBy(center.getUser());
        t.setCenter(center);
        t.setDefaultTemplate(false);
        t.setStatus(ContractTemplateStatus.ACTIVE);
        ContractTemplate saved = contractTemplateRepository.save(t);
        saveTemplateType(saved.getTemplateId(), request.getContractType());
        return toTemplateResponse(saved);
    }

    @Override
    @Transactional
    public ContractTemplateResponse updateContractTemplate(
            Long templateId, SaveContractTemplateRequest request) {
        TutorCenter center = requireCenter();
        requireVerifiedCenter(center);
        ContractTemplate t = contractTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mẫu hợp đồng"));
        if (t.getCenter() == null || !t.getCenter().getCenterId().equals(center.getCenterId())) {
            throw new ForbiddenException("Chỉ được sửa mẫu của chính trung tâm (không sửa mẫu hệ thống).");
        }
        if (StringUtils.hasText(request.getName())) {
            t.setName(request.getName().trim());
        }
        if (StringUtils.hasText(request.getContent())) {
            t.setContent(request.getContent().trim());
        }
        ContractTemplate saved = contractTemplateRepository.save(t);
        if (StringUtils.hasText(request.getContractType())) {
            saveTemplateType(saved.getTemplateId(), request.getContractType());
        }
        return toTemplateResponse(saved);
    }

    private ContractTemplateResponse toTemplateResponse(ContractTemplate t) {
        return ContractTemplateResponse.builder()
                .templateId(t.getTemplateId())
                .name(t.getName())
                .content(t.getContent())
                .contractType(findTemplateType(t.getTemplateId()))
                .defaultTemplate(Boolean.TRUE.equals(t.getDefaultTemplate()))
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .system(t.getCenter() == null)
                .build();
    }

    // ===================== Thông tin BÊN A của hợp đồng (thông tin trung tâm) =====================

    private static final com.fasterxml.jackson.databind.ObjectMapper CONTRACT_INFO_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public com.tcs.module.center.dto.response.CenterContractInfoResponse getContractInfo() {
        TutorCenter center = requireCenter();
        Map<String, String> extra = readCenterContractExtra(center.getCenterId());
        return com.tcs.module.center.dto.response.CenterContractInfoResponse.builder()
                .companyName(center.getCompanyName())
                .address(center.getAddress())
                .phone(center.getPhone())
                .email(center.getUser() != null ? center.getUser().getEmail() : null)
                .website(extra.get("website"))
                // Người đại diện lấy từ CCCD người đại diện pháp luật (đã quét), không nhập tay.
                .representativeName(legalRepName(center))
                .representativePosition(extra.get("representativePosition"))
                .verificationStatus(
                        center.getVerificationStatus() != null
                                ? center.getVerificationStatus().name()
                                : null)
                .build();
    }

    /** Họ tên người đại diện pháp luật lấy từ CCCD đã quét (cccd:{userId}). */
    private String legalRepName(TutorCenter center) {
        if (center.getUser() == null) {
            return null;
        }
        return cccdService.getByUserId(center.getUser().getUserId()).getFullName();
    }

    @Override
    @Transactional
    public com.tcs.module.center.dto.response.CenterContractInfoResponse saveContractInfo(
            com.tcs.module.center.dto.request.SaveCenterContractInfoRequest request) {
        TutorCenter center = requireCenter();
        Map<String, String> map = new HashMap<>();
        map.put("website", request.getWebsite() != null ? request.getWebsite().trim() : null);
        // Người đại diện KHÔNG lưu ở đây — luôn lấy từ CCCD người đại diện pháp luật (nguồn chuẩn).
        map.put("representativePosition",
                request.getRepresentativePosition() != null ? request.getRepresentativePosition().trim() : null);
        String key = "centercontract:" + center.getCenterId();
        try {
            SystemParameter param = systemParameterRepository.findByParamKey(key)
                    .orElseGet(SystemParameter::new);
            param.setParamKey(key);
            param.setParamValue(CONTRACT_INFO_MAPPER.writeValueAsString(map));
            param.setDescription("Thông tin BÊN A của trung tâm cho hợp đồng");
            systemParameterRepository.save(param);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không lưu được thông tin hợp đồng của trung tâm.");
        }
        return getContractInfo();
    }

    private Map<String, String> readCenterContractExtra(Long centerId) {
        return systemParameterRepository.findByParamKey("centercontract:" + centerId)
                .map(p -> {
                    try {
                        return CONTRACT_INFO_MAPPER.readValue(p.getParamValue(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        return new HashMap<String, String>();
                    }
                })
                .orElseGet(HashMap::new);
    }

    /** Lưu loại mẫu hợp đồng (RECRUITMENT/CLASS) qua system_parameters (không cần cột/migration). */
    private void saveTemplateType(Long templateId, String contractType) {
        String value = TEMPLATE_TYPE_RECRUITMENT.equalsIgnoreCase(contractType)
                ? TEMPLATE_TYPE_RECRUITMENT : TEMPLATE_TYPE_CLASS;
        String key = TEMPLATE_TYPE_PREFIX + templateId;
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(value);
        param.setDescription("Loại mẫu hợp đồng: RECRUITMENT / CLASS");
        systemParameterRepository.save(param);
    }

    private String findTemplateType(Long templateId) {
        return systemParameterRepository.findByParamKey(TEMPLATE_TYPE_PREFIX + templateId)
                .map(SystemParameter::getParamValue)
                .orElse(TEMPLATE_TYPE_CLASS);
    }
}
