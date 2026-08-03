package com.tcs.module.platform.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
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
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
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
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.ResolveClassIssueRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.VerificationDetailResponse;
import com.tcs.module.platform.dto.response.VerificationDocumentResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ClassIssueResolutionAction;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.repository.ReportRepository;
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
    /** BR-03: lý do từ chối tối thiểu 10 ký tự. */
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
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final DisputeRepository disputeRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final EscrowService escrowService;
    private final TutoringClassRepository tutoringClassRepository;
    private final AuthHelper authHelper;

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

        UserStatus newStatus = request.getStatus();
        if (newStatus != UserStatus.ACTIVE
                && newStatus != UserStatus.SUSPENDED
                && newStatus != UserStatus.BANNED) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }

        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        return platformMapper.toUserListItem(saved, profiles);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long pendingVerifications = verificationRequestRepository.findAll().stream()
                .filter(v -> v.getStatus() == VerificationStatus.SUBMITTED
                        || v.getStatus() == VerificationStatus.UNDER_REVIEW)
                .count();
        long openReports = reportRepository.findAll().stream()
                .filter(r -> r.getStatus() == com.tcs.module.platform.enums.ReportStatus.PENDING)
                .count();
        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalTutors(tutorRepository.count())
                .totalClasses(tutoringClassRepository.count())
                .pendingVerifications(pendingVerifications)
                .openReports(openReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRequestResponse> listVerificationRequests() {
        return verificationRequestRepository.findAll().stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public VerificationDetailResponse getVerificationDetail(Long verificationId) {
        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        // BR-01: mở hồ sơ đang SUBMITTED sẽ tự động chuyển sang UNDER_REVIEW và ghi lịch sử.
        if (verification.getStatus() == VerificationStatus.SUBMITTED) {
            Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
            VerificationStatus oldStatus = verification.getStatus();
            verification.setStatus(VerificationStatus.UNDER_REVIEW);
            verification = verificationRequestRepository.save(verification);
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
        // Decision chỉ được là VERIFIED (Duyệt) hoặc REJECTED (Từ chối).
        if (decision != VerificationStatus.VERIFIED && decision != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ. Chỉ chấp nhận Duyệt hoặc Từ chối.");
        }

        // Optimistic locking: nếu hồ sơ đã bị người khác cập nhật kể từ lúc admin mở xem,
        // chặn để tránh ghi đè quyết định của người kia (so khớp theo giây để tránh lệch nano/DB).
        LocalDateTime expectedUpdatedAt = request.getExpectedUpdatedAt();
        if (expectedUpdatedAt != null && verification.getUpdatedAt() != null
                && !verification.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS)
                        .equals(expectedUpdatedAt.truncatedTo(ChronoUnit.SECONDS))) {
            throw new IllegalArgumentException(
                    "Hồ sơ vừa được cập nhật bởi người khác, vui lòng tải lại trước khi sửa.");
        }
        // Cho phép Duyệt/Từ chối khi hồ sơ đang xem xét (UNDER_REVIEW),
        // hoặc SỬA LẠI quyết định đã có (VERIFIED/REJECTED). Chỉ chặn DRAFT/SUBMITTED
        // vì phải mở hồ sơ để xem xét trước (BR-01 chuyển SUBMITTED -> UNDER_REVIEW).
        VerificationStatus current = verification.getStatus();
        if (current != VerificationStatus.UNDER_REVIEW
                && current != VerificationStatus.VERIFIED
                && current != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Hồ sơ chưa sẵn sàng để duyệt. Vui lòng mở hồ sơ để xem xét trước.");
        }
        // BR-03 / AF-01: khi Từ chối bắt buộc nhập lý do (>= 10 ký tự).
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
        Notification notification = new Notification();
        notification.setUser(request.getUser());
        notification.setType(NotificationType.VERIFICATION);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceType("VERIFICATION_REQUEST");
        notification.setReferenceId(request.getVerificationId());
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private VerificationDetailResponse buildDetail(VerificationRequest v) {
        Long userId = v.getUser().getUserId();
        Map<String, String> details = new LinkedHashMap<>();
        String submitterName = null;
        String submitterPhone = v.getUser().getPhone();

        if (v.getVerificationType() == VerificationType.TUTOR_PROFILE) {
            Tutor tutor = tutorRepository.findByUser_UserId(userId).orElse(null);
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
        } else {
            TutorCenter center = tutorCenterRepository.findByUser_UserId(userId).orElse(null);
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

        List<VerificationDocumentResponse> documents = verificationDocumentRepository
                .findByVerificationRequest_VerificationId(v.getVerificationId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        boolean hasUnreadable = documents.stream().anyMatch(doc -> !doc.isAvailable());

        return VerificationDetailResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(userId)
                .userEmail(v.getUser().getEmail())
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
        return reportRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listCenterReports() {
        Long centerUserId = authHelper.requireRole(UserRole.TUTOR_CENTER).getUserId();
        return reportRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
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

    private VerificationRequestResponse toVerificationResponse(VerificationRequest v) {
        return VerificationRequestResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(v.getUser().getUserId())
                .userEmail(v.getUser().getEmail())
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
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.CLASS);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceType("REPORT");
        notification.setReferenceId(report.getReportId());
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
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
            case PLATFORM_ADMIN -> platformAdminRepository.findAll().stream()
                    .map(admin -> admin.getUser().getUserId())
                    .toList();
            case TUTOR -> tutorRepository.findAll().stream()
                    .map(tutor -> tutor.getUser().getUserId())
                    .toList();
            case TUTOR_CENTER -> tutorCenterRepository.findAll().stream()
                    .map(center -> center.getUser().getUserId())
                    .toList();
            case CLIENT -> clientRepository.findAll().stream()
                    .map(client -> client.getUser().getUserId())
                    .toList();
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
}
