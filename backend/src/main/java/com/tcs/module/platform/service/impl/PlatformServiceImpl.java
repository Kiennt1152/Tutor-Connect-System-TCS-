package com.tcs.module.platform.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.enums.ReviewType;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationHistory;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.dto.request.ModerateReviewRequest;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.AdminReviewResponse;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PlatformService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.CloseTicketRequest;
import com.tcs.module.platform.dto.request.RespondTicketRequest;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.ResolveClassIssueRequest;
import com.tcs.module.platform.dto.request.UpdateTicketRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageSupportTicketResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.SupportTicketListItemResponse;
import com.tcs.module.platform.dto.response.VerificationDetailResponse;
import com.tcs.module.platform.dto.response.VerificationDocumentResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ClassIssueResolutionAction;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_REJECT_NOTES_LENGTH = 10;

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final PlatformMapper platformMapper;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final DisputeRepository disputeRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final com.tcs.module.finance.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowService escrowService;
    private final TutoringClassRepository tutoringClassRepository;
    private final ReviewRepository reviewRepository;
    private final ContractService contractService;
    private final AuthHelper authHelper;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final AuditLogService auditLogService;
    private final com.tcs.module.platform.service.PlatformTaskQueueService taskQueueService;
    private final com.tcs.module.platform.service.PlatformAnalyticsService analyticsService;
    private final com.tcs.module.profile.service.CccdService cccdService;
    private final NotificationDispatchService notificationDispatchService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final String TICKET_CONTEXT_TYPE = "SUPPORT_TICKET";

    private static final String DEFAULT_ANONYMOUS_NAME = "Người dùng ẩn danh";

    @Override
    @Transactional(readOnly = true)
    public PageUserListResponse getUsers(
            int page, int size, UserStatus status, UserRole role, String keyword) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "userId"));

        Page<User> users = queryUsers(status, role, keyword, pageable);
        List<Long> userIds = users.getContent().stream().map(User::getUserId).toList();
        ProfileMaps profileMaps = loadProfileMaps(userIds);

        List<UserListItemResponse> content = users.getContent().stream()
                .map(user -> platformMapper.toUserListItem(user, profileMaps.bundleFor(user.getUserId())))
                .sorted(Comparator.comparing(UserListItemResponse::getUserId))
                .toList();

        return PageUserListResponse.builder()
                .content(content)
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        User user = findUserOrThrow(userId);
        UserProfileBundle profiles = loadProfiles(userId);

        if (platformMapper.resolveRole(profiles) == UserRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái tài khoản quản trị viên");
        }

        UserStatus oldStatus = user.getStatus();
        UserStatus newStatus = request.getStatus();
        if (newStatus != UserStatus.ACTIVE
                && newStatus != UserStatus.SUSPENDED
                && newStatus != UserStatus.BANNED) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }

        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        auditLogService.record("UPDATE_USER_STATUS", "User", userId, java.util.Map.of("oldStatus", oldStatus), java.util.Map.of("newStatus", request.getStatus()));
        return platformMapper.toUserListItem(saved, profiles);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate from, LocalDate to, String granularity) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        
        com.tcs.module.platform.dto.response.TaskQueueSummaryResponse taskSummary = taskQueueService.getSummary();
        com.tcs.module.platform.dto.response.AnalyticsSummaryResponse analyticsSummary = analyticsService.getSummary(effectiveFrom, effectiveTo);

        com.tcs.module.platform.dto.response.PageTaskItemResponse queueResponse = taskQueueService.listTasks("ALL", null, null, 0, 10);
        java.util.List<com.tcs.module.platform.dto.response.TaskItemResponse> queuePreview = queueResponse.getContent();

        com.tcs.module.platform.dto.response.RiskSummaryResponse riskSummary = com.tcs.module.platform.dto.response.RiskSummaryResponse.builder()
                .overdueTickets(taskSummary.getOverdueCount())
                .openDisputes(taskSummary.getOpenDisputes())
                .pendingRefunds(taskSummary.getPendingRefunds())
                .escrowExposure(taskSummary.getMoneyAtRisk())
                .unhandledReports(taskSummary.getOpenReports())
                .build();
        
        com.tcs.module.platform.dto.response.FinancialFlowResponse financialFlow = com.tcs.module.platform.dto.response.FinancialFlowResponse.builder()
                .moneyIn(analyticsSummary.getMoneyIn())
                .moneyOut(analyticsSummary.getMoneyOut())
                .netMovement(analyticsSummary.getNetMovement())
                .escrowHeld(analyticsSummary.getEscrowHeld())
                .deposits(analyticsSummary.getDeposits())
                .escrowDeposits(analyticsSummary.getEscrowHeld())
                .withdrawals(analyticsSummary.getWithdrawals())
                .refunds(analyticsSummary.getEscrowRefunded())
                .platformFeeRevenue(analyticsSummary.getPlatformFeeRevenue())
                .feeRate(analyticsSummary.getPlatformFeeRate() != null ? analyticsSummary.getPlatformFeeRate().doubleValue() : 10.0)
                .build();
        LocalDateTime rangeStart = effectiveFrom.atStartOfDay();
        LocalDateTime rangeEnd = effectiveTo.plusDays(1).atStartOfDay();

        // --- Active Tutors ---
        long activeTutors = 0;
        long newTutors = 0;
        long verifiedTutors = 0;
        long recentlyActiveTutors = 0;
        try {
            newTutors = tutorRepository.countByCreatedAtBetween(rangeStart, rangeEnd);
            activeTutors = tutorRepository.countByUserStatus(com.tcs.module.identity.enums.UserStatus.ACTIVE);
            verifiedTutors = tutorRepository.countByVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            recentlyActiveTutors = tutorRepository.countByRecentlyActive(rangeStart);
        } catch (Exception ignored) {}

        // --- Active Centers ---
        long activeCenters = 0;
        long newCenters = 0;
        long verifiedCenters = 0;
        long recentlyActiveCenters = 0;
        try {
            newCenters = tutorCenterRepository.countByCreatedAtBetween(rangeStart, rangeEnd);
            activeCenters = tutorCenterRepository.countByUserStatus(com.tcs.module.identity.enums.UserStatus.ACTIVE);
            verifiedCenters = tutorCenterRepository.countByVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            recentlyActiveCenters = tutorCenterRepository.countByRecentlyActive(rangeStart);
        } catch (Exception ignored) {}

        // --- Classes ---
        long activeClasses = 0;
        long completedClasses = 0;
        long cancelledClasses = 0;
        long totalClassCount = 0;
        try {
            totalClassCount = tutoringClassRepository.count();
            activeClasses = tutoringClassRepository.countByStatus(com.tcs.module.marketplace.enums.TutoringClassStatus.IN_PROGRESS);
            completedClasses = tutoringClassRepository.countByStatus(com.tcs.module.marketplace.enums.TutoringClassStatus.COMPLETED);
            cancelledClasses = tutoringClassRepository.countByStatus(com.tcs.module.marketplace.enums.TutoringClassStatus.CANCELLED);
        } catch (Exception ignored) {}

        List<com.tcs.module.platform.dto.response.ActivityTimelineEntry> activityTimeline = buildActivityTimeline(effectiveFrom, effectiveTo, granularity);
        
        com.tcs.module.platform.dto.response.HealthMetricsResponse tutorHealth = com.tcs.module.platform.dto.response.HealthMetricsResponse.builder()
                .totalCount(analyticsSummary.getTotalTutors())
                .activeCount(activeTutors)
                .verifiedCount(verifiedTutors)
                .newCount(newTutors)
                .recentlyActiveCount(recentlyActiveTutors)
                .build();
                
        com.tcs.module.platform.dto.response.HealthMetricsResponse centerHealth = com.tcs.module.platform.dto.response.HealthMetricsResponse.builder()
                .totalCount(analyticsSummary.getTotalCenters())
                .activeCount(activeCenters)
                .verifiedCount(verifiedCenters)
                .newCount(newCenters)
                .recentlyActiveCount(recentlyActiveCenters)
                .build();
                
        com.tcs.module.platform.dto.response.HealthMetricsResponse classHealth = com.tcs.module.platform.dto.response.HealthMetricsResponse.builder()
                .totalCount(totalClassCount)
                .activeCount(activeClasses)
                .verifiedCount(completedClasses)
                .newCount(cancelledClasses)
                .recentlyActiveCount(0)
                .build();

        return DashboardResponse.builder()
                .totalUsers(analyticsSummary.getTotalUsers())
                .totalClasses(analyticsSummary.getTotalClasses())
                .riskSummary(riskSummary)
                .financialFlow(financialFlow)
                .tutorHealth(tutorHealth)
                .centerHealth(centerHealth)
                .classHealth(classHealth)
                .activityTimeline(activityTimeline)
                .queuePreview(queuePreview)
                .alerts(new java.util.ArrayList<>()) // Keep empty for backward compatibility if needed, or remove entirely in DTO later
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRequestResponse> listVerificationRequests() {
        return verificationRequestRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public VerificationDetailResponse getVerificationDetail(Long verificationId) {
        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        if (verification.getStatus() == VerificationStatus.SUBMITTED) {
            Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
            VerificationStatus oldStatus = verification.getStatus();
            verification.setStatus(VerificationStatus.UNDER_REVIEW);
            // saveAndFlush + refresh: ghi xuống DB rồi ĐỌC LẠI đúng giá trị updated_at đã lưu
            // (DB DATETIME làm tròn phần mili-giây của @UpdateTimestamp). Nhờ vậy mốc thời gian
            // detail trả về khớp chính xác giá trị DB -> bước duyệt không báo "đã cập nhật bởi người khác".
            verification = verificationRequestRepository.saveAndFlush(verification);
            entityManager.refresh(verification);
            recordVerificationHistory(verification, oldStatus, VerificationStatus.UNDER_REVIEW, adminId);
        }
        return buildDetail(verification);
    }

    @Override
    @Transactional
    public VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái xác minh không được để trống");
        }
        Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();

        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        VerificationStatus decision = request.getStatus();
        if (decision != VerificationStatus.VERIFIED && decision != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ. Chỉ chấp nhận Duyệt hoặc Từ chối.");
        }

        LocalDateTime expectedUpdatedAt = request.getExpectedUpdatedAt();
        if (expectedUpdatedAt != null && verification.getUpdatedAt() != null
                && !verification.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS)
                        .equals(expectedUpdatedAt.truncatedTo(ChronoUnit.SECONDS))) {
            throw new IllegalArgumentException(
                    "Hồ sơ vừa được cập nhật bởi người khác, vui lòng tải lại trước khi sửa.");
        }
        VerificationStatus current = verification.getStatus();
        if (current != VerificationStatus.UNDER_REVIEW
                && current != VerificationStatus.VERIFIED
                && current != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Hồ sơ chưa sẵn sàng để duyệt. Vui lòng mở hồ sơ để xem xét trước.");
        }
        if (decision == VerificationStatus.REJECTED) {
            String notes = request.getAdminNotes() == null ? "" : request.getAdminNotes().trim();
            if (notes.length() < MIN_REJECT_NOTES_LENGTH) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).");
            }
        }

        VerificationStatus oldStatus = verification.getStatus();
        verification.setStatus(request.getStatus());
        verification.setAdminNotes(request.getAdminNotes());
        verification.setReviewedBy(adminId);
        verification.setReviewedAt(java.time.LocalDateTime.now());
        if (request.getStatus() == VerificationStatus.REJECTED) {
            verification.setRejectionReason(request.getAdminNotes());
        } else {
            verification.setRejectionReason(null);
        }
        VerificationRequest saved = verificationRequestRepository.save(verification);
        auditLogService.record("REVIEW_VERIFICATION", "VerificationRequest", verificationId, null, java.util.Map.of("status", request.getStatus(), "notes", request.getAdminNotes() != null ? request.getAdminNotes() : ""));

        recordVerificationHistory(saved, oldStatus, request.getStatus(), adminId);
        if (request.getStatus() == VerificationStatus.VERIFIED
                || request.getStatus() == VerificationStatus.REJECTED) {
            ProfileVerificationStatus profileStatus = request.getStatus() == VerificationStatus.VERIFIED
                    ? ProfileVerificationStatus.VERIFIED
                    : ProfileVerificationStatus.REJECTED;
            Long userId = saved.getUser().getUserId();
            tutorRepository.findByUser_UserId(userId).ifPresent(tutor -> {
                tutor.setVerificationStatus(profileStatus);
                tutorRepository.save(tutor);
            });
            tutorCenterRepository.findByUser_UserId(userId).ifPresent(center -> {
                center.setVerificationStatus(profileStatus);
                tutorCenterRepository.save(center);
            });
            sendVerificationNotification(saved, request.getStatus());
        }
        return toVerificationResponse(saved);
    }

    private void recordVerificationHistory(VerificationRequest request,
                                           VerificationStatus oldStatus,
                                           VerificationStatus newStatus,
                                           Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));
        VerificationHistory history = new VerificationHistory();
        history.setVerificationRequest(request);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(admin);
        verificationHistoryRepository.save(history);
    }

    private void sendVerificationNotification(VerificationRequest request, VerificationStatus status) {
        String title;
        String content;
        if (status == VerificationStatus.VERIFIED) {
            title = "Hồ sơ xác minh được duyệt";
            content = "Hồ sơ xác minh của bạn đã được duyệt.";
        } else if (status == VerificationStatus.REJECTED) {
            title = "Hồ sơ xác minh bị từ chối";
            content = "Lý do: "
                    + (request.getRejectionReason() != null ? request.getRejectionReason() : "không rõ");
        } else {
            return;
        }
        String templateCode = request.getStatus() == VerificationStatus.VERIFIED
                ? "VERIFICATION_APPROVED"
                : "VERIFICATION_REJECTED";
        notificationDispatchService.notifyUserFromTemplate(
                request.getUser(),
                NotificationType.VERIFICATION,
                templateCode,
                Map.of("reason", request.getRejectionReason() == null ? "" : request.getRejectionReason()),
                title,
                content,
                "VERIFICATION_REQUEST",
                request.getVerificationId());
    }

    private VerificationDetailResponse buildDetail(VerificationRequest v) {
        Long userId = v.getUser().getUserId();
        UserProfileBundle profiles = loadProfiles(userId);
        UserRole userRole = platformMapper.resolveRole(profiles);
        Map<String, String> details = new LinkedHashMap<>();
        String submitterName = null;
        String submitterPhone = v.getUser().getPhone();

        if (v.getVerificationType() == VerificationType.TUTOR_PROFILE && profiles.tutor() != null) {
            Tutor tutor = profiles.tutor();
            if (tutor != null) {
                submitterName = tutor.getFullName();
                if (StringUtils.hasText(tutor.getPhone())) {
                    submitterPhone = tutor.getPhone();
                }
                details.put("Giới tính", tutor.getGender() == null ? "—" : tutor.getGender().name());
                details.put("Số năm kinh nghiệm", String.valueOf(tutor.getExperienceYears()));
                details.put("Địa chỉ", orDash(tutor.getAddress()));
                details.put("Giới thiệu", orDash(tutor.getBio()));
                details.put("Trạng thái xác minh", tutor.getVerificationStatus().name());
            }
        } else if (v.getVerificationType() == VerificationType.TUTOR_PROFILE && profiles.client() != null) {
            Client client = profiles.client();
            submitterName = client.getFullName();
            if (StringUtils.hasText(client.getPhone())) {
                submitterPhone = client.getPhone();
            }
            details.put("Vai trò", "Phụ huynh / học viên");
            details.put("Giới tính", client.getGender() == null ? "—" : client.getGender().name());
            details.put("Ngày sinh", client.getDateOfBirth() == null ? "—" : client.getDateOfBirth().toString());
            details.put("Địa chỉ", orDash(client.getAddress()));
        } else {
            TutorCenter center = profiles.tutorCenter();
            if (center != null) {
                submitterName = center.getCompanyName();
                if (StringUtils.hasText(center.getPhone())) {
                    submitterPhone = center.getPhone();
                }
                details.put("Số giấy phép", orDash(center.getLicenseNo()));
                details.put("Địa chỉ", orDash(center.getAddress()));
                details.put("Mô tả", orDash(center.getDescription()));
                details.put("Trạng thái xác minh", center.getVerificationStatus().name());
            }
        }

        // Thông tin CCCD (đọc từ QR) của người nộp -> để admin đối chiếu khi duyệt.
        com.tcs.module.profile.dto.CccdInfoDto cccd = cccdService.getByUserId(userId);
        if (cccd != null && Boolean.TRUE.equals(cccd.getComplete())) {
            details.put("CCCD — Họ tên", orDash(cccd.getFullName()));
            details.put("CCCD — Số", orDash(cccd.getCccdNumber()));
            details.put("CCCD — Ngày sinh", orDash(cccd.getDateOfBirth()));
            details.put("CCCD — Giới tính", orDash(cccd.getGender()));
            details.put("CCCD — Ngày cấp", orDash(cccd.getIssueDate()));
            details.put("CCCD — Nơi cấp", orDash(cccd.getIssuePlace()));
            details.put("CCCD — Thường trú", orDash(cccd.getPermanentAddress()));
        } else {
            details.put("CCCD", "Chưa quét/hoàn thành thông tin CCCD");
        }

        List<VerificationDocumentResponse> documents = verificationDocumentRepository
                .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(v.getVerificationId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        boolean hasUnreadable = documents.stream().anyMatch(doc -> !doc.isAvailable());

        return VerificationDetailResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(userId)
                .userEmail(v.getUser().getEmail())
                .userRole(userRole)
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .adminNotes(v.getAdminNotes())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .submitterName(submitterName)
                .submitterPhone(submitterPhone)
                .submitterDetails(details)
                .documents(documents)
                .hasUnreadableDocument(hasUnreadable)
                .build();
    }

    private VerificationDocumentResponse toDocumentResponse(VerificationDocument doc) {
        MediaFile file = doc.getFile();
        boolean available = file != null && StringUtils.hasText(file.getFileUrl());
        return VerificationDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .documentType(doc.getDocumentType())
                .fileId(file == null ? null : file.getFileId())
                .fileName(file == null ? null : file.getFileName())
                .fileUrl(file == null ? null : file.getFileUrl())
                .mimeType(file == null ? null : file.getMimeType())
                .fileSize(file == null ? null : file.getFileSize())
                .available(available)
                .build();
    }

    private String orDash(String value) {
        return StringUtils.hasText(value) ? value : "—";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listCenterReports() {
        Long centerUserId = authHelper.requireRole(UserRole.TUTOR_CENTER).getUserId();
        return reportRepository.findByTargetTypeOrderByCreatedAtDesc(ReportTargetType.CLASS)
                .stream()
                .filter(report -> isOwnedCenterClassReport(report, centerUserId))
                .map(this::toReportResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReportResponse resolveClassIssue(Long reportId, ResolveClassIssueRequest request) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        if (reportId == null) {
            throw new IllegalArgumentException("reportId là bắt buộc");
        }
        if (request == null || request.getAction() == null) {
            throw new IllegalArgumentException("Hành động xử lý là bắt buộc");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        if (report.getTargetType() != ReportTargetType.CLASS) {
            throw new BusinessException("Chỉ hỗ trợ xử lý báo cáo sự cố lớp học trong luồng này");
        }
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BusinessException("Báo cáo đã được xử lý");
        }

        return resolveClassIssueReport(report, request);
    }

    @Override
    @Transactional
    public ReportResponse resolveCenterClassIssue(Long reportId, ResolveClassIssueRequest request) {
        Long centerUserId = authHelper.requireRole(UserRole.TUTOR_CENTER).getUserId();
        if (reportId == null) {
            throw new IllegalArgumentException("reportId là bắt buộc");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        if (!isOwnedCenterClassReport(report, centerUserId)) {
            throw new ForbiddenException("Bạn chỉ có quyền xử lý báo cáo của lớp trung tâm do mình quản lý");
        }
        if (request == null || request.getAction() == null) {
            throw new IllegalArgumentException("Hành động xử lý là bắt buộc");
        }
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BusinessException("Báo cáo đã được xử lý");
        }

        return resolveClassIssueReport(report, request);
    }

    private ReportResponse resolveClassIssueReport(Report report, ResolveClassIssueRequest request) {
        if (request == null || request.getAction() == null) {
            throw new IllegalArgumentException("Hành động xử lý là bắt buộc");
        }
        if (report.getTargetType() != ReportTargetType.CLASS) {
            throw new BusinessException("Chỉ hỗ trợ xử lý báo cáo sự cố lớp học trong luồng này");
        }
        String notes = normalizeClassIssueNotes(request.getNotes());

        String oldDescription = report.getDescription();
        ReportStatus oldStatus = report.getStatus();
        ClassIssueResolutionAction action = request.getAction();
        Dispute escalatedDispute = null;
        if (action == ClassIssueResolutionAction.ESCALATE_TO_DISPUTE
                || action == ClassIssueResolutionAction.TERMINATE_CLASS) {
            escalatedDispute = escalateClassIssueReport(report, action, notes);
            report.setDescription(appendClassIssueHandlingNote(
                    oldDescription,
                    action,
                    notes + "\nMã tranh chấp: #" + escalatedDispute.getDisputeId()));
        } else {
            report.setDescription(appendClassIssueHandlingNote(oldDescription, action, notes));
            if (action != ClassIssueResolutionAction.REQUEST_MORE_INFORMATION) {
                report.setStatus(ReportStatus.RESOLVED);
            }
        }

        Report saved = reportRepository.save(report);
        auditReportResolution(saved, action, oldStatus, oldDescription, saved.getDescription(), escalatedDispute);
        notifyClassIssueResolution(saved, action, escalatedDispute);
        return toReportResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminReviewResponse> listReviews(ReviewStatus status) {
        List<Review> reviews = status == null
                ? reviewRepository.findByReviewTypeOrderByCreatedAtDesc(ReviewType.CLIENT_TO_TUTOR)
                : reviewRepository.findByReviewTypeAndStatusOrderByCreatedAtDesc(
                        ReviewType.CLIENT_TO_TUTOR, status);
        return reviews.stream().map(this::toAdminReviewResponse).toList();
    }

    @Override
    @Transactional
    public AdminReviewResponse moderateReview(Long reviewId, ModerateReviewRequest request) {
        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        review.setStatus(request.getStatus());
        Review saved = reviewRepository.save(review);

        contractService.recomputeReputationByTutorUser(review.getReviewee().getUserId());

        return toAdminReviewResponse(saved);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        Long tutorUserId = review.getReviewee().getUserId();
        reviewRepository.delete(review);

        contractService.recomputeReputationByTutorUser(tutorUserId);
    }

    private AdminReviewResponse toAdminReviewResponse(Review review) {
        Long reviewerId = review.getReviewer().getUserId();
        String reviewerName = clientRepository
                .findByUser_UserId(reviewerId)
                .map(Client::getFullName)
                .orElse(null);

        String publicDisplayName;
        if (review.isAnonymous()) {
            String custom = review.getDisplayName() == null ? null : review.getDisplayName().trim();
            publicDisplayName = (custom == null || custom.isEmpty()) ? DEFAULT_ANONYMOUS_NAME : custom;
        } else {
            publicDisplayName = reviewerName;
        }

        Long tutorUserId = review.getReviewee().getUserId();
        String tutorName = tutorRepository
                .findByUser_UserId(tutorUserId)
                .map(Tutor::getFullName)
                .orElse(null);

        TutoringClass reviewClass = review.getTutoringClass();

        return AdminReviewResponse.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .comment(review.getComment())
                .criteriaJson(review.getCriteriaJson())
                .status(review.getStatus())
                .reviewerId(reviewerId)
                .reviewerName(reviewerName)
                .reviewerEmail(review.getReviewer().getEmail())
                .anonymous(review.isAnonymous())
                .publicDisplayName(publicDisplayName)
                .tutorUserId(tutorUserId)
                .tutorName(tutorName)
                .classId(reviewClass != null ? reviewClass.getClassId() : null)
                .classTitle(reviewClass != null ? reviewClass.getTitle() : null)
                .subjectName(
                        reviewClass != null && reviewClass.getSubject() != null
                                ? reviewClass.getSubject().getSubjectName()
                                : null)
                .tutorReply(review.getTutorReply())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ReportResponse resolveReport(Long reportId, com.tcs.module.platform.dto.request.ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        if (report.getTargetType() == ReportTargetType.CLASS) {
            throw new BusinessException("Báo cáo lớp phải được xử lý bằng luồng sự cố lớp học");
        }
        if (request == null || request.getStatus() != ReportStatus.RESOLVED) {
            throw new IllegalArgumentException("Trạng thái xử lý báo cáo phải là RESOLVED");
        }
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BusinessException("Báo cáo đã được xử lý");
        }
        com.tcs.module.platform.enums.ReportStatus oldStatus = report.getStatus();
        report.setStatus(request.getStatus());
        Report saved = reportRepository.save(report);
        auditLogService.record("RESOLVE_REPORT", "Report", reportId, java.util.Map.of("oldStatus", oldStatus), java.util.Map.of("newStatus", request.getStatus(), "adminNotes", request.getAdminNotes() != null ? request.getAdminNotes() : ""));
        createReportNotification(
                saved.getReporter(),
                "Báo cáo của bạn đã được xử lý",
                request.getAdminNotes() == null || request.getAdminNotes().isBlank()
                        ? "Quản trị viên đã hoàn tất xử lý báo cáo #" + saved.getReportId() + "."
                        : request.getAdminNotes().trim(),
                saved);
        return toReportResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageSupportTicketResponse getTickets(
            int page, int size, SupportTicketStatus status,
            SupportTicketCategory category, SupportTicketPriority priority, String keyword) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<SupportTicket> tickets =
                supportTicketRepository.search(status, category, priority, trimmedKeyword, pageable);

        List<SupportTicketListItemResponse> content =
                tickets.getContent().stream().map(this::toTicketListItem).toList();

        return PageSupportTicketResponse.builder()
                .content(content)
                .page(tickets.getNumber())
                .size(tickets.getSize())
                .totalElements(tickets.getTotalElements())
                .totalPages(tickets.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse getTicketDetail(Long ticketId) {
        SupportTicket ticket = findTicketOrThrow(ticketId);

        // Mở ticket lần đầu: tự động gán admin hiện tại và chuyển OPEN -> IN_PROGRESS.
        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            PlatformAdmin admin = currentAdminOrThrow();
            ticket.setAssignedAdmin(admin);
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            ticket = supportTicketRepository.save(ticket);
        }
        return toTicketDetail(ticket);
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        SupportTicket ticket = findTicketOrThrow(ticketId);

        if (ticket.getStatus() == SupportTicketStatus.RESOLVED
                || ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new IllegalArgumentException("Không thể chỉnh ticket đã kết thúc");
        }
        if (request == null || (request.getCategory() == null && request.getPriority() == null)) {
            throw new IllegalArgumentException("Hãy chọn category hoặc priority cần cập nhật");
        }

        boolean categoryChanged = request.getCategory() != null
                && request.getCategory() != ticket.getCategory();
        boolean priorityChanged = request.getPriority() != null
                && request.getPriority() != ticket.getPriority();
        if (!categoryChanged && !priorityChanged) {
            throw new IllegalArgumentException("Ticket không có thay đổi để lưu");
        }

        Map<String, Object> oldValue = ticketAuditSnapshot(ticket);

        if (categoryChanged) {
            ticket.setCategory(request.getCategory());
        }
        if (priorityChanged) {
            ticket.setPriority(request.getPriority());
            LocalDateTime baseTime = ticket.getCreatedAt() != null
                    ? ticket.getCreatedAt()
                    : LocalDateTime.now();
            LocalDateTime dueAt = baseTime.plusHours(calculateTicketSlaHours(request.getPriority()));
            ticket.setDueAt(dueAt);
            ticket.setSlaBreached(LocalDateTime.now().isAfter(dueAt));
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        auditLogService.record(
                "UPDATE_TICKET", "SupportTicket", ticketId, oldValue, ticketAuditSnapshot(saved));
        return toTicketDetail(saved);
    }

    private Map<String, Object> ticketAuditSnapshot(SupportTicket ticket) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("category", ticket.getCategory());
        value.put("priority", ticket.getPriority());
        value.put("dueAt", ticket.getDueAt());
        return value;
    }

    private int calculateTicketSlaHours(SupportTicketPriority priority) {
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 12;
            case MEDIUM -> 24;
            case LOW -> 48;
        };
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse respondToTicket(Long ticketId, RespondTicketRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Nội dung phản hồi là bắt buộc");
        }
        SupportTicket ticket = findTicketOrThrow(ticketId);
        PlatformAdmin admin = currentAdminOrThrow();

        if (ticket.getAssignedAdmin() == null) {
            ticket.setAssignedAdmin(admin);
        }

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(admin.getUser());
        message.setIsFromAdmin(true);
        message.setContent(request.getContent());
        ticketMessageRepository.save(message);

        if (ticket.getStatus() != SupportTicketStatus.RESOLVED && ticket.getStatus() != SupportTicketStatus.CLOSED) {
            ticket.setStatus(SupportTicketStatus.IN_REVIEW);
        }
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getResponseSlaMs() == null && ticket.getCreatedAt() != null) {
            ticket.setResponseSlaMs(java.time.Duration.between(ticket.getCreatedAt(), now).toMillis());
        }
        if (ticket.getDueAt() != null && now.isAfter(ticket.getDueAt())) {
            ticket.setSlaBreached(true);
        }
        SupportTicket saved = supportTicketRepository.save(ticket);

        notifyUserOfTicketResponse(saved, request.getContent());
        auditLogService.record("RESPOND_TICKET", "SupportTicket", ticketId, null, request);
        return toTicketDetail(saved);
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse closeTicket(Long ticketId, CloseTicketRequest request) {
        if (request.getStatus() != SupportTicketStatus.RESOLVED && request.getStatus() != SupportTicketStatus.CLOSED) {
            throw new IllegalArgumentException("Chỉ chấp nhận trạng thái RESOLVED hoặc CLOSED");
        }
        SupportTicket ticket = findTicketOrThrow(ticketId);
        SupportTicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(request.getStatus());
        LocalDateTime now = LocalDateTime.now();
        if (request.getStatus() == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        } else {
            ticket.setClosedAt(now);
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(now);
            }
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        auditLogService.record("CLOSE_TICKET", "SupportTicket", ticketId,
                java.util.Map.of("oldStatus", oldStatus), java.util.Map.of("newStatus", request.getStatus()));

        String note = StringUtils.hasText(request.getAdminNotes())
                ? request.getAdminNotes()
                : (request.getStatus() == SupportTicketStatus.RESOLVED
                        ? "Yêu cầu hỗ trợ của bạn đã được giải quyết."
                        : "Yêu cầu hỗ trợ của bạn đã được đóng.");
        notifyUserOfTicketResponse(saved, note);
        return toTicketDetail(saved);
    }

    private SupportTicket findTicketOrThrow(Long ticketId) {
        return supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hỗ trợ"));
    }

    private PlatformAdmin currentAdminOrThrow() {
        Long adminUserId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        return platformAdminRepository
                .findByUser_UserId(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
    }


    private void notifyUserOfTicketResponse(SupportTicket ticket, String content) {
        String title = "Phản hồi yêu cầu hỗ trợ #" + ticket.getTicketId();
        notificationDispatchService.notifyUserFromTemplate(
                ticket.getUser(),
                NotificationType.SYSTEM,
                "SUPPORT_TICKET_RESPONSE",
                Map.of("ticketId", ticket.getTicketId(), "content", content),
                title,
                content,
                "SUPPORT_TICKET",
                ticket.getTicketId());
    }

    private SupportTicketListItemResponse toTicketListItem(SupportTicket ticket) {
        PlatformAdmin admin = ticket.getAssignedAdmin();
        return SupportTicketListItemResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUser().getUserId())
                .userEmail(ticket.getUser().getEmail())
                .assignedAdminId(admin != null ? admin.getAdminId() : null)
                .assignedAdminName(admin != null ? admin.getFullName() : null)
                .category(ticket.getCategory())
                .subject(ticket.getSubject())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .dueAt(ticket.getDueAt())
                .slaBreached(ticket.getSlaBreached())
                .responseSlaMs(ticket.getResponseSlaMs())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private SupportTicketDetailResponse toTicketDetail(SupportTicket ticket) {
        List<TicketMessageResponse> messages = ticketMessageRepository
                .findByTicket_TicketIdOrderByCreatedAtAsc(ticket.getTicketId())
                .stream()
                .map(msg -> toTicketMessage(msg, ticket))
                .toList();

        return SupportTicketDetailResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUser().getUserId())
                .targetClassId(ticket.getTargetClass() != null ? ticket.getTargetClass().getClassId() : null)
                .assignedAdminId(ticket.getAssignedAdmin() != null ? ticket.getAssignedAdmin().getAdminId() : null)
                .category(ticket.getCategory())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .evidenceUrls(ticket.getEvidenceUrls())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .dueAt(ticket.getDueAt())
                .slaBreached(ticket.getSlaBreached())
                .responseSlaMs(ticket.getResponseSlaMs())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private TicketMessageResponse toTicketMessage(TicketMessage message, SupportTicket ticket) {
        return TicketMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserId())
                .senderName(resolveSenderName(message.getSender()))
                .fromAdmin(message.getIsFromAdmin())
                .content(message.getContent())
                .sentAt(message.getCreatedAt())
                .build();
    }

    private String resolveSenderName(User sender) {
        String email = sender.getEmail();
        return StringUtils.hasText(email) ? email : "Người dùng #" + sender.getUserId();
    }

    private VerificationRequestResponse toVerificationResponse(VerificationRequest v) {
        UserRole userRole = platformMapper.resolveRole(loadProfiles(v.getUser().getUserId()));
        return VerificationRequestResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(v.getUser().getUserId())
                .userEmail(v.getUser().getEmail())
                .userRole(userRole)
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .adminNotes(v.getAdminNotes())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .build();
    }

    private String normalizeClassIssueNotes(String notes) {
        if (!StringUtils.hasText(notes)) {
            throw new IllegalArgumentException("Ghi chú xử lý là bắt buộc");
        }
        String trimmed = notes.trim();
        if (trimmed.length() < 10) {
            throw new IllegalArgumentException("Ghi chú xử lý phải có ít nhất 10 ký tự");
        }
        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException("Ghi chú xử lý không được vượt quá 2000 ký tự");
        }
        return trimmed;
    }

    private Dispute escalateClassIssueReport(
            Report report,
            ClassIssueResolutionAction action,
            String notes) {

        Dispute existing = disputeRepository.findByReport_ReportId(report.getReportId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        EscrowTransaction escrow = resolveSingleEscrowForClassIssue(report);
        EscrowTransaction heldEscrow = escrowService.holdForDispute(
                escrow.getEscrowId(),
                "UC30 " + resolutionActionLabel(action) + ": " + notes);

        Dispute dispute = new Dispute();
        dispute.setReport(report);
        dispute.setEscrowTransaction(heldEscrow);
        dispute.setStatus(DisputeStatus.OPEN);
        return disputeRepository.save(dispute);
    }

    private EscrowTransaction resolveSingleEscrowForClassIssue(Report report) {
        Long classId = report.getTargetId();
        List<EscrowTransaction> candidates = new ArrayList<>();
        candidates.addAll(escrowTransactionRepository.findByAssignment_Application_TutoringClass_ClassId(classId));
        candidates.addAll(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(classId));

        List<EscrowTransaction> distinct = candidates.stream()
                .filter(Objects::nonNull)
                .filter(escrow -> escrow.getEscrowId() != null)
                .filter(escrow -> escrow.getStatus() == EscrowStatus.FUNDED
                        || escrow.getStatus() == EscrowStatus.ON_HOLD
                        || escrow.getStatus() == EscrowStatus.DISPUTED)
                .collect(Collectors.toMap(
                        EscrowTransaction::getEscrowId,
                        escrow -> escrow,
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();

        if (distinct.isEmpty()) {
            throw new BusinessException("Không tìm thấy escrow còn hiệu lực để chuyển báo cáo thành tranh chấp");
        }
        if (distinct.size() > 1) {
            throw new BusinessException("Lớp có nhiều escrow, cần chuyển tranh chấp từ giao dịch/escrow cụ thể");
        }
        return distinct.get(0);
    }

    private String appendClassIssueHandlingNote(
            String currentDescription,
            ClassIssueResolutionAction action,
            String notes) {

        String prefix = StringUtils.hasText(currentDescription) ? currentDescription.trim() : "";
        String handlingNote = "[UC-30] Xử lý sự cố lớp học\n"
                + "Hành động: " + resolutionActionLabel(action) + "\n"
                + "Ghi chú: " + notes.trim() + "\n"
                + "Thời gian xử lý: " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return StringUtils.hasText(prefix) ? prefix + "\n\n" + handlingNote : handlingNote;
    }

    private void auditReportResolution(
            Report report,
            ClassIssueResolutionAction action,
            ReportStatus oldStatus,
            String oldDescription,
            String newDescription,
            Dispute escalatedDispute) {

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(currentActorOrNull());
        auditLog.setAction("Xử lý báo cáo sự cố lớp học");
        auditLog.setEntityType("REPORT");
        auditLog.setEntityId(report.getReportId());
        auditLog.setOldValue(jsonObject(
                "status", oldStatus,
                "description", oldDescription));
        auditLog.setNewValue(jsonObject(
                "status", report.getStatus(),
                "action", action,
                "description", newDescription,
                "linkedDisputeId", escalatedDispute != null ? escalatedDispute.getDisputeId() : null));
        auditLogRepository.save(auditLog);
    }

    private void notifyClassIssueResolution(
            Report report,
            ClassIssueResolutionAction action,
            Dispute escalatedDispute) {

        Set<Long> notifiedUserIds = new LinkedHashSet<>();
        User reporter = report.getReporter();
        if (reporter != null) {
            createReportNotification(
                    reporter,
                    reportResolutionTitle(action),
                    reportResolutionContent(report, action, escalatedDispute),
                    report);
            notifiedUserIds.add(reporter.getUserId());
        }

        if (report.getTargetType() == ReportTargetType.CLASS && report.getTargetId() != null) {
            tutoringClassRepository.findById(report.getTargetId()).ifPresent(tutoringClass -> {
                addReportNotificationForUser(
                        tutoringClass.getCreator(),
                        notifiedUserIds,
                        report,
                        action,
                        escalatedDispute);
                if (tutoringClass.getCenter() != null) {
                    addReportNotificationForUser(
                            tutoringClass.getCenter().getUser(),
                            notifiedUserIds,
                            report,
                            action,
                            escalatedDispute);
                }
            });
        }
    }

    private void addReportNotificationForUser(
            User user,
            Set<Long> notifiedUserIds,
            Report report,
            ClassIssueResolutionAction action,
            Dispute escalatedDispute) {

        if (user == null || user.getUserId() == null || notifiedUserIds.contains(user.getUserId())) {
            return;
        }
        createReportNotification(
                user,
                reportResolutionTitle(action),
                reportResolutionContent(report, action, escalatedDispute),
                report);
        notifiedUserIds.add(user.getUserId());
    }

    private String reportResolutionTitle(ClassIssueResolutionAction action) {
        return action == ClassIssueResolutionAction.REQUEST_MORE_INFORMATION
                ? "Cần bổ sung thông tin sự cố lớp học"
                : action == ClassIssueResolutionAction.ESCALATE_TO_DISPUTE
                        || action == ClassIssueResolutionAction.TERMINATE_CLASS
                ? "Báo cáo sự cố đã được chuyển thành tranh chấp"
                : "Báo cáo sự cố lớp học đã được xử lý";
    }

    private String reportResolutionContent(
            Report report,
            ClassIssueResolutionAction action,
            Dispute escalatedDispute) {

        String base = "Báo cáo #" + report.getReportId() + " đã được cập nhật: " + resolutionActionLabel(action) + ".";
        if (escalatedDispute != null) {
            return base + " Mã tranh chấp #" + escalatedDispute.getDisputeId() + ".";
        }
        return base;
    }

    private void createReportNotification(User user, String title, String content, Report report) {
        notificationDispatchService.notifyUserFromTemplate(
                user,
                NotificationType.REPORT,
                "REPORT_RESOLVED",
                Map.of("title", title, "content", content, "reportId", report.getReportId()),
                title,
                content,
                "REPORT",
                report.getReportId());
    }

    private User currentActorOrNull() {
        Long actorId = authHelper.currentUserId();
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).orElse(null);
    }

    private String resolutionActionLabel(ClassIssueResolutionAction action) {
        return switch (action) {
            case REQUEST_MORE_INFORMATION -> "Yêu cầu bổ sung thông tin";
            case CONTINUE_CLASS -> "Tiếp tục lớp";
            case RESCHEDULE -> "Dời lịch/bù buổi";
            case REPLACE_TUTOR -> "Đổi gia sư";
            case TERMINATE_CLASS -> "Chuyển xử lý chấm dứt lớp";
            case ESCALATE_TO_DISPUTE -> "Chuyển thành tranh chấp";
            case CLOSE_NO_ACTION -> "Đóng báo cáo";
        };
    }

    private String jsonObject(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return "{}";
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("JSON audit payload cần theo cặp key/value");
        }

        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"')
                    .append(escapeJson(String.valueOf(keyValues[i])))
                    .append("\":")
                    .append(jsonValue(keyValues[i + 1]));
        }
        return builder.append('}').toString();
    }

    private String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private ReportResponse toReportResponse(Report report) {
        ReportMetadata metadata = parseReportMetadata(report.getDescription());
        Long linkedDisputeId = disputeRepository.findByReport_ReportId(report.getReportId())
                .map(Dispute::getDisputeId)
                .orElse(null);
        ClassReportContext classContext = resolveClassReportContext(report);
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporter().getUserId())
                .reporterEmail(report.getReporter().getEmail())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .classTitle(classContext.title())
                .classStatus(classContext.status())
                .category(report.getCategory())
                .description(report.getDescription())
                .evidenceUrls(report.getEvidenceUrls())
                .evidenceUrlList(parseEvidenceUrls(report.getEvidenceUrls()))
                .status(report.getStatus())
                .issueType(metadata.issueType())
                .issueTypeLabel(metadata.issueTypeLabel())
                .lessonRef(metadata.lessonRef())
                .occurredAt(metadata.occurredAt())
                .requestedAction(metadata.requestedAction())
                .requestedActionLabel(metadata.requestedActionLabel())
                .linkedDisputeId(linkedDisputeId)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private ReportMetadata parseReportMetadata(String description) {
        String issueType = extractLineValue(description, "Mã loại sự cố:");
        String issueTypeLabel = extractLineValue(description, "Loại sự cố:");
        String lessonRef = extractLineValue(description, "Buổi/ngày liên quan:");
        String occurredAtText = extractLineValue(description, "Ngày xảy ra:");
        String requestedAction = extractLineValue(description, "Mã hướng xử lý:");
        String requestedActionLabel = extractLineValue(description, "Hướng xử lý mong muốn:");
        LocalDate occurredAt = null;
        if (StringUtils.hasText(occurredAtText) && !"Không xác định".equalsIgnoreCase(occurredAtText)) {
            try {
                occurredAt = LocalDate.parse(occurredAtText);
            } catch (RuntimeException ignored) {
                occurredAt = null;
            }
        }
        return new ReportMetadata(
                blankToNull(issueType),
                blankToNull(issueTypeLabel),
                blankToNull(lessonRef),
                occurredAt,
                blankToNull(requestedAction),
                blankToNull(requestedActionLabel));
    }

    private String extractLineValue(String text, String prefix) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return java.util.Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .findFirst()
                .orElse(null);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> parseEvidenceUrls(String evidenceUrls) {
        if (!StringUtils.hasText(evidenceUrls)) {
            return List.of();
        }
        return java.util.Arrays.stream(evidenceUrls.split("[\\r\\n,;]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private ClassReportContext resolveClassReportContext(Report report) {
        if (report.getTargetType() != ReportTargetType.CLASS || report.getTargetId() == null) {
            return new ClassReportContext(null, null);
        }
        return tutoringClassRepository.findById(report.getTargetId())
                .map(tutoringClass -> new ClassReportContext(
                        tutoringClass.getTitle(),
                        tutoringClass.getStatus() != null ? tutoringClass.getStatus().name() : null))
                .orElse(new ClassReportContext(null, null));
    }

    private boolean isOwnedCenterClassReport(Report report, Long centerUserId) {
        if (report == null
                || report.getTargetType() != ReportTargetType.CLASS
                || report.getTargetId() == null
                || centerUserId == null) {
            return false;
        }
        return tutoringClassRepository.findById(report.getTargetId())
                .filter(tutoringClass -> tutoringClass.getClassType() == ClassType.CENTER)
                .filter(tutoringClass -> isOwnedByCenter(tutoringClass, centerUserId))
                .isPresent();
    }

    private boolean isOwnedByCenter(TutoringClass tutoringClass, Long centerUserId) {
        if (tutoringClass == null || centerUserId == null) {
            return false;
        }
        boolean ownsByCenterProfile = tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null
                && Objects.equals(tutoringClass.getCenter().getUser().getUserId(), centerUserId);
        boolean ownsByCreator = tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), centerUserId);
        return ownsByCenterProfile || ownsByCreator;
    }

    private Page<User> queryUsers(UserStatus status, UserRole role, String keyword, PageRequest pageable) {
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        if (role != null && role != UserRole.UNKNOWN) {
            List<Long> roleUserIds = findUserIdsByRole(role);
            if (roleUserIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return userRepository.searchUsersByIds(roleUserIds, status, trimmedKeyword, pageable);
        }

        return userRepository.searchUsers(status, trimmedKeyword, pageable);
    }

    private List<Long> findUserIdsByRole(UserRole role) {
        return switch (role) {
            case PLATFORM_ADMIN -> platformAdminRepository.findAllUserIds();
            case TUTOR -> tutorRepository.findAllUserIds();
            case TUTOR_CENTER -> tutorCenterRepository.findAllUserIds();
            case CLIENT -> clientRepository.findAllUserIds();
            default -> List.of();
        };
    }

    private User findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
    }

    private ProfileMaps loadProfileMaps(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return ProfileMaps.empty();
        }

        Map<Long, PlatformAdmin> adminMap = toMapByUserId(
                platformAdminRepository.findByUser_UserIdIn(userIds), PlatformAdmin::getUser);
        Map<Long, Tutor> tutorMap =
                toMapByUserId(tutorRepository.findByUser_UserIdIn(userIds), Tutor::getUser);
        Map<Long, TutorCenter> centerMap = toMapByUserId(
                tutorCenterRepository.findByUser_UserIdIn(userIds), TutorCenter::getUser);
        Map<Long, Client> clientMap =
                toMapByUserId(clientRepository.findByUser_UserIdIn(userIds), Client::getUser);

        return new ProfileMaps(adminMap, tutorMap, centerMap, clientMap);
    }

    private <T> Map<Long, T> toMapByUserId(List<T> items, Function<T, User> userExtractor) {
        return items.stream()
                .collect(Collectors.toMap(item -> userExtractor.apply(item).getUserId(), Function.identity()));
    }

    private record ProfileMaps(
            Map<Long, PlatformAdmin> admins,
            Map<Long, Tutor> tutors,
            Map<Long, TutorCenter> centers,
            Map<Long, Client> clients) {

        static ProfileMaps empty() {
            return new ProfileMaps(Map.of(), Map.of(), Map.of(), Map.of());
        }

        UserProfileBundle bundleFor(Long userId) {
            return UserProfileBundle.of(
                    admins.get(userId), tutors.get(userId), centers.get(userId), clients.get(userId));
        }
    }

    private record ReportMetadata(
            String issueType,
            String issueTypeLabel,
            String lessonRef,
            LocalDate occurredAt,
            String requestedAction,
            String requestedActionLabel) {
    }

    private record ClassReportContext(String title, String status) {
    }

    private List<com.tcs.module.platform.dto.response.ActivityTimelineEntry> buildActivityTimeline(
            LocalDate from, LocalDate to, String granularity) {
        List<com.tcs.module.platform.dto.response.ActivityTimelineEntry> timeline = new ArrayList<>();
        LocalDate cursor = from;
        
        while (!cursor.isAfter(to)) {
            LocalDate periodEnd;
            String label;
            
            if ("MONTH".equalsIgnoreCase(granularity)) {
                periodEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
                if (periodEnd.isAfter(to)) periodEnd = to;
                label = "T" + cursor.getMonthValue() + "/" + cursor.getYear();
            } else if ("WEEK".equalsIgnoreCase(granularity)) {
                periodEnd = cursor.plusDays(6);
                if (periodEnd.isAfter(to)) periodEnd = to;
                label = cursor.toString() + " \u2192 " + periodEnd.toString();
            } else {
                periodEnd = cursor;
                label = cursor.toString();
            }
            
            LocalDateTime start = cursor.atStartOfDay();
            LocalDateTime end = periodEnd.plusDays(1).atStartOfDay();
            
            long newUsers = userRepository.countByCreatedAtBetween(start, end);
            long newTutors = tutorRepository.countByCreatedAtBetween(start, end);
            long newCenters = tutorCenterRepository.countByCreatedAtBetween(start, end);
            long newClasses = tutoringClassRepository.countByCreatedAtBetween(start, end);
            long newTickets = supportTicketRepository.countByCreatedAtBetween(start, end);
            
            long activeTutors = tutorRepository.countByUserStatus(com.tcs.module.identity.enums.UserStatus.ACTIVE);
            long activeCenters = tutorCenterRepository.countByUserStatus(com.tcs.module.identity.enums.UserStatus.ACTIVE);
            
            java.math.BigDecimal moneyIn = paymentTransactionRepository.sumAmountByStatusAndTypeInAndCreatedAtBetween(
                    com.tcs.module.finance.enums.PaymentTransactionStatus.SUCCESS,
                    java.util.List.of(
                            com.tcs.module.finance.enums.PaymentTransactionType.DEPOSIT,
                            com.tcs.module.finance.enums.PaymentTransactionType.ESCROW_DEPOSIT,
                            com.tcs.module.finance.enums.PaymentTransactionType.PLATFORM_FEE
                    ),
                    start,
                    end
            );

            java.math.BigDecimal moneyOut = paymentTransactionRepository.sumAmountByStatusAndTypeInAndCreatedAtBetween(
                    com.tcs.module.finance.enums.PaymentTransactionStatus.SUCCESS,
                    java.util.List.of(
                            com.tcs.module.finance.enums.PaymentTransactionType.WITHDRAWAL,
                            com.tcs.module.finance.enums.PaymentTransactionType.REFUND,
                            com.tcs.module.finance.enums.PaymentTransactionType.ESCROW_RELEASE
                    ),
                    start,
                    end
            );

            java.math.BigDecimal platformFeeRevenue = paymentTransactionRepository.sumAmountByStatusAndTypeAndCreatedAtBetween(
                    com.tcs.module.finance.enums.PaymentTransactionStatus.SUCCESS,
                    com.tcs.module.finance.enums.PaymentTransactionType.PLATFORM_FEE,
                    start,
                    end
            );
            
            java.math.BigDecimal netMovement = moneyIn.subtract(moneyOut);
            
            timeline.add(com.tcs.module.platform.dto.response.ActivityTimelineEntry.builder()
                    .label(label).newUsers(newUsers).newTutors(newTutors).newCenters(newCenters)
                    .newClasses(newClasses).newTickets(newTickets).activeTutors(activeTutors)
                    .activeCenters(activeCenters).moneyIn(moneyIn).moneyOut(moneyOut)
                    .netMovement(netMovement).platformFeeRevenue(platformFeeRevenue)
                    .build());
            
            if ("MONTH".equalsIgnoreCase(granularity)) {
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            } else if ("WEEK".equalsIgnoreCase(granularity)) {
                cursor = cursor.plusWeeks(1);
            } else {
                cursor = cursor.plusDays(1);
            }
        }
        return timeline;
    }
}
