package com.tcs.module.identity.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationHistory;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.mapper.VerificationMapper;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    /**
     * Review flow (start-review + review + sync status) is currently disabled.
     * Re-enable after the verification review flow is ready to ship.
     */
    private static final boolean REVIEW_FLOW_ENABLED = false;

    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final VerificationMapper verificationMapper;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;
    private final NotificationDispatchService notificationDispatchService;

    @Override
    @Transactional
    public VerificationResponse submitVerification(VerificationRequestDto request) {
        Long userId = authHelper.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserRole role = authHelper.requireAuthenticated().getRole();
        if (role != UserRole.CLIENT && role != UserRole.TUTOR && role != UserRole.TUTOR_CENTER) {
            throw new ForbiddenException("Chỉ phụ huynh, gia sư hoặc trung tâm mới được nộp xác minh");
        }

        if ((role == UserRole.CLIENT || role == UserRole.TUTOR)
                && request.getVerificationType() != VerificationType.TUTOR_PROFILE) {
            throw new BusinessException("Tài khoản này chỉ được nộp hồ sơ xác minh danh tính");
        }
        if (role == UserRole.TUTOR_CENTER
                && request.getVerificationType() != VerificationType.TUTOR_CENTER_LICENSE) {
            throw new BusinessException(
                    "Tài khoản trung tâm chỉ được nộp hồ sơ loại TUTOR_CENTER_LICENSE");
        }

        if (!canResubmit(userId, request.getVerificationType())) {
            List<VerificationRequest> existing = verificationRequestRepository
                    .findByUser_UserIdOrderBySubmittedAtDesc(userId).stream()
                    .filter(v -> v.getVerificationType() == request.getVerificationType())
                    .filter(v -> v.getStatus() == VerificationStatus.SUBMITTED
                            || v.getStatus() == VerificationStatus.UNDER_REVIEW
                            || v.getStatus() == VerificationStatus.VERIFIED)
                    .toList();
            String detail = existing.isEmpty()
                    ? ""
                    : " (hồ sơ #" + existing.get(0).getVerificationId()
                            + " hiện ở trạng thái " + existing.get(0).getStatus().name() + ")";
            throw new BusinessException(
                    "Bạn đã có hồ sơ xác minh đang xử lý hoặc đã được duyệt"
                            + detail
                            + ". Hãy hủy hồ sơ cũ trước khi nộp mới.");
        }

        if (request.getDocuments() == null || request.getDocuments().isEmpty()) {
            throw new IllegalArgumentException("At least one document is required");
        }

        validateRequiredDocuments(request, role);

        VerificationRequest verification = new VerificationRequest();
        verification.setUser(user);
        verification.setVerificationType(request.getVerificationType());
        verification.setStatus(VerificationStatus.SUBMITTED);
        verification.setSubmittedAt(LocalDateTime.now());

        VerificationRequest saved = verificationRequestRepository.save(verification);

        for (VerificationRequestDto.DocumentUpload docUpload : request.getDocuments()) {
            if (docUpload.getFileId() == null) {
                throw new BusinessException("Mỗi tài liệu xác minh phải có file đã tải lên");
            }
            var fileOpt = mediaFileRepository.findById(docUpload.getFileId());
            if (fileOpt.isEmpty()) {
                throw new ResourceNotFoundException("File not found: " + docUpload.getFileId());
            }
            if (!fileOpt.get().getUploadedBy().getUserId().equals(userId)) {
                throw new ForbiddenException("File không thuộc sở hữu của bạn");
            }

            VerificationDocument doc = new VerificationDocument();
            doc.setVerificationRequest(saved);
            doc.setFile(fileOpt.get());
            doc.setDocumentType(docUpload.getDocumentType());
            verificationDocumentRepository.save(doc);
        }

        recordHistory(saved, null, VerificationStatus.SUBMITTED, user);

        auditLogService.record(userId, "SUBMIT_VERIFICATION", "VerificationRequest", saved.getVerificationId(),
                null, Map.of("verificationType", request.getVerificationType().name()));

        log.info("Verification submitted: userId={}, type={}, verificationId={}",
                userId, request.getVerificationType(), saved.getVerificationId());

        return getVerificationById(saved.getVerificationId());
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResponse getVerificationById(Long verificationId) {
        VerificationRequest request = loadVerificationOrThrow(verificationId);
        verifyOwnerOrAdmin(request);

        List<VerificationDocument> docs = verificationDocumentRepository
                .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(verificationId);

        return verificationMapper.toResponse(request, docs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getVerificationsByUser(Long userId) {
        UserPrincipal principal = authHelper.requireAuthenticated();
        boolean isAdmin = principal.getRole() == UserRole.PLATFORM_ADMIN;
        boolean isOwner = principal.getUserId().equals(userId);
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xem hồ sơ xác minh của người khác");
        }
        return verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(v -> {
                    List<VerificationDocument> docs = verificationDocumentRepository
                            .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(v.getVerificationId());
                    return verificationMapper.toResponse(v, docs);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getVerificationsByStatus(VerificationStatus status) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        return verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(status)
                .stream()
                .map(v -> {
                    List<VerificationDocument> docs = verificationDocumentRepository
                            .findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(v.getVerificationId());
                    return verificationMapper.toResponse(v, docs);
                })
                .toList();
    }

    @Override
    @Transactional
    public VerificationResponse startReview(Long verificationId) {
        guardReviewFlow();
        Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        VerificationRequest verification = loadVerificationOrThrow(verificationId);

        if (verification.getStatus() != VerificationStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể bắt đầu duyệt khi hồ sơ ở trạng thái SUBMITTED");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));

        VerificationStatus oldStatus = verification.getStatus();
        verification.setStatus(VerificationStatus.UNDER_REVIEW);
        VerificationRequest saved = verificationRequestRepository.save(verification);

        recordHistory(saved, oldStatus, VerificationStatus.UNDER_REVIEW, admin);

        log.info("Verification review started: verificationId={}, adminId={}", verificationId, adminId);
        return verificationMapper.toResponse(saved,
                verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(verificationId));
    }

    @Override
    @Transactional
    public VerificationResponse reviewVerification(Long verificationId, VerificationDecisionDto decision) {
        guardReviewFlow();
        Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        VerificationRequest verification = loadVerificationOrThrow(verificationId);

        if (verification.getStatus() != VerificationStatus.SUBMITTED
                && verification.getStatus() != VerificationStatus.UNDER_REVIEW) {
            throw new BusinessException("Chỉ có thể duyệt hồ sơ ở trạng thái SUBMITTED hoặc UNDER_REVIEW");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));

        VerificationStatus oldStatus = verification.getStatus();
        verification.setReviewedBy(adminId);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setAdminNotes(decision.getNote());

        VerificationStatus newStatus;
        if ("APPROVE".equalsIgnoreCase(decision.getDecision())) {
            newStatus = VerificationStatus.VERIFIED;
            verification.setRejectionReason(null);
        } else if ("REJECT".equalsIgnoreCase(decision.getDecision())) {
            newStatus = VerificationStatus.REJECTED;
            verification.setRejectionReason(decision.getNote());
        } else {
            throw new IllegalArgumentException("Decision must be APPROVE or REJECT");
        }
        verification.setStatus(newStatus);

        VerificationRequest saved = verificationRequestRepository.save(verification);
        recordHistory(saved, oldStatus, newStatus, admin);
        syncProfileStatus(saved.getUser().getUserId(), newStatus);
        sendResultNotification(saved, newStatus);

        log.info("Verification reviewed: verificationId={}, adminId={}, decision={}",
                verificationId, adminId, decision.getDecision());

        return verificationMapper.toResponse(saved,
                verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(verificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getModerationQueue() {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        return getVerificationsByStatus(VerificationStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getMyVerifications() {
        Long userId = authHelper.currentUserId();
        return verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(v -> verificationMapper.toResponse(v,
                        verificationDocumentRepository.findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(v.getVerificationId())))
                .toList();
    }

    @Override
    @Transactional
    public void cancelVerification(Long verificationId) {
        Long userId = authHelper.currentUserId();
        VerificationRequest verification = loadVerificationOrThrow(verificationId);

        if (verification.getUser() == null
                || !verification.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền hủy hồ sơ xác minh này");
        }

        if (verification.getStatus() != VerificationStatus.SUBMITTED) {
            throw new BusinessException(
                    "Chỉ có thể hủy hồ sơ ở trạng thái SUBMITTED. Trạng thái hiện tại: "
                            + verification.getStatus().name());
        }

        verificationDocumentRepository.deleteAllByVerificationId(verificationId);
        verificationHistoryRepository.deleteAllByVerificationId(verificationId);
        verificationRequestRepository.delete(verification);

        auditLogService.record(userId, "CANCEL_VERIFICATION", "VerificationRequest", verificationId, null, null);

        log.info("Verification cancelled: verificationId={}, userId={}", verificationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canResubmit(Long userId, VerificationType verificationType) {
        return !verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                userId,
                verificationType,
                List.of(VerificationStatus.SUBMITTED, VerificationStatus.UNDER_REVIEW, VerificationStatus.VERIFIED)
        );
    }

    private VerificationRequest loadVerificationOrThrow(Long verificationId) {
        return verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationId));
    }

    private void verifyOwnerOrAdmin(VerificationRequest request) {
        UserPrincipal principal = authHelper.requireAuthenticated();
        boolean isAdmin = principal.getRole() == UserRole.PLATFORM_ADMIN;
        boolean isOwner = request.getUser() != null
                && request.getUser().getUserId().equals(principal.getUserId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xem hồ sơ xác minh này");
        }
    }

    private void recordHistory(VerificationRequest request, VerificationStatus oldStatus,
                               VerificationStatus newStatus, User changedBy) {
        VerificationHistory history = new VerificationHistory();
        history.setVerificationRequest(request);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(changedBy);
        verificationHistoryRepository.save(history);
    }

    private void syncProfileStatus(Long userId, VerificationStatus status) {
        if (status != VerificationStatus.VERIFIED && status != VerificationStatus.REJECTED) {
            return;
        }
        ProfileVerificationStatus profileStatus = status == VerificationStatus.VERIFIED
                ? ProfileVerificationStatus.VERIFIED
                : ProfileVerificationStatus.REJECTED;
        tutorRepository.findByUser_UserId(userId).ifPresent(tutor -> {
            tutor.setVerificationStatus(profileStatus);
            tutorRepository.save(tutor);
        });
        tutorCenterRepository.findByUser_UserId(userId).ifPresent(center -> {
            center.setVerificationStatus(profileStatus);
            tutorCenterRepository.save(center);
        });
    }

    private void sendResultNotification(VerificationRequest request, VerificationStatus status) {
        String title;
        String content;
        if (status == VerificationStatus.VERIFIED) {
            title = "Hồ sơ xác minh được duyệt";
            content = "Hồ sơ xác minh của bạn đã được duyệt. Bạn có thể đăng lớp ngay bây giờ.";
        } else if (status == VerificationStatus.REJECTED) {
            title = "Hồ sơ xác minh bị từ chối";
            content = "Lý do: "
                    + (request.getRejectionReason() != null ? request.getRejectionReason() : "không rõ")
                    + ". Bạn có thể nộp lại sau khi bổ sung giấy tờ.";
        } else {
            return;
        }
        String templateCode = status == VerificationStatus.VERIFIED
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

    private void guardReviewFlow() {
        if (!REVIEW_FLOW_ENABLED) {
            throw new BusinessException(
                    "Review flow is temporarily disabled.");
        }
    }

    /**
     * Reuse document_type values to avoid a schema migration:
     * ID_CARD = CCCD/CMND mặt trước, DEGREE = CCCD/CMND mặt sau,
     * CERTIFICATE = chứng chỉ/MST, LICENSE = giấy phép.
     */
    private void validateRequiredDocuments(VerificationRequestDto request, UserRole role) {
        if (request.getDocuments().stream().anyMatch(doc -> doc.getDocumentType() == null)) {
            throw new BusinessException("Mỗi tài liệu xác minh phải có loại tài liệu");
        }

        Map<VerificationDocumentType, Long> counts = request.getDocuments().stream()
                .collect(Collectors.groupingBy(
                        VerificationRequestDto.DocumentUpload::getDocumentType,
                        Collectors.counting()));

        if (request.getVerificationType() == VerificationType.TUTOR_CENTER_LICENSE) {
            long licenseCount = counts.getOrDefault(VerificationDocumentType.LICENSE, 0L);
            long idCardCount = counts.getOrDefault(VerificationDocumentType.ID_CARD, 0L);
            long idCardBackCount = counts.getOrDefault(VerificationDocumentType.DEGREE, 0L);
            long certificateCount = counts.getOrDefault(VerificationDocumentType.CERTIFICATE, 0L);
            long total = request.getDocuments().size();

            if (total != 5
                    || licenseCount != 2
                    || idCardCount != 1
                    || idCardBackCount != 1
                    || certificateCount != 1) {
                throw new BusinessException(
                        "Hồ sơ trung tâm cần đủ 5 chứng từ bắt buộc: "
                                + "Giấy ĐKKD (LICENSE), Giấy phép hoạt động giáo dục (LICENSE), "
                                + "Mã số thuế / Đăng ký thuế (CERTIFICATE), "
                                + "CCCD mặt trước người đại diện (ID_CARD), "
                                + "CCCD mặt sau người đại diện (DEGREE).");
            }
            return;
        }

        long idCardFrontCount = counts.getOrDefault(VerificationDocumentType.ID_CARD, 0L);
        long idCardBackCount = counts.getOrDefault(VerificationDocumentType.DEGREE, 0L);
        long total = request.getDocuments().size();
        long certificateCount = counts.getOrDefault(VerificationDocumentType.CERTIFICATE, 0L);
        boolean hasOnlyAllowedTypes =
                idCardFrontCount + idCardBackCount + certificateCount == total;

        if (role == UserRole.CLIENT) {
            if (total != 2 || idCardFrontCount != 1 || idCardBackCount != 1) {
                throw new BusinessException(
                        "Hồ sơ xác minh phụ huynh cần đúng 2 ảnh: CCCD/CMND mặt trước và mặt sau.");
            }
            return;
        }

        if (role == UserRole.TUTOR
                && (!hasOnlyAllowedTypes || idCardFrontCount != 1 || idCardBackCount != 1)) {
            throw new BusinessException(
                    "Hồ sơ xác minh gia sư cần CCCD/CMND mặt trước, mặt sau; bằng cấp/chứng chỉ là không bắt buộc.");
        }
    }
}
