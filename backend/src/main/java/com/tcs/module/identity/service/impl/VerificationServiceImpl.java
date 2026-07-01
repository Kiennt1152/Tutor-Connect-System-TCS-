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
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.mapper.VerificationMapper;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final VerificationMapper verificationMapper;
    private final AuthHelper authHelper;

    @Override
    @Transactional
    public VerificationResponse submitVerification(VerificationRequestDto request) {
        Long userId = authHelper.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserRole role = authHelper.requireAuthenticated().getRole();
        if (role != UserRole.TUTOR && role != UserRole.TUTOR_CENTER) {
            throw new ForbiddenException("Chỉ gia sư hoặc trung tâm mới được nộp xác minh");
        }

        if (!canResubmit(userId, request.getVerificationType())) {
            throw new BusinessException("A verification request is already pending or approved for this type");
        }

        if (request.getDocuments() == null || request.getDocuments().isEmpty()) {
            throw new IllegalArgumentException("At least one document is required");
        }

        VerificationRequest verification = new VerificationRequest();
        verification.setUser(user);
        verification.setVerificationType(request.getVerificationType());
        verification.setStatus(VerificationStatus.SUBMITTED);
        verification.setSubmittedAt(LocalDateTime.now());

        VerificationRequest saved = verificationRequestRepository.save(verification);

        for (VerificationRequestDto.DocumentUpload docUpload : request.getDocuments()) {
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

        recordHistory(saved, null, VerificationStatus.SUBMITTED, user, "Tutor nộp hồ sơ xác minh");

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
                .findByVerificationRequest_VerificationId(verificationId);

        return verificationMapper.toResponse(request, docs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getVerificationsByUser(Long userId) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        return verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(v -> {
                    List<VerificationDocument> docs = verificationDocumentRepository
                            .findByVerificationRequest_VerificationId(v.getVerificationId());
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
                            .findByVerificationRequest_VerificationId(v.getVerificationId());
                    return verificationMapper.toResponse(v, docs);
                })
                .toList();
    }

    @Override
    @Transactional
    public VerificationResponse startReview(Long verificationId) {
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

        recordHistory(saved, oldStatus, VerificationStatus.UNDER_REVIEW, admin,
                "Admin bắt đầu duyệt hồ sơ");

        log.info("Verification review started: verificationId={}, adminId={}", verificationId, adminId);
        return verificationMapper.toResponse(saved,
                verificationDocumentRepository.findByVerificationRequest_VerificationId(verificationId));
    }

    @Override
    @Transactional
    public VerificationResponse reviewVerification(Long verificationId, VerificationDecisionDto decision) {
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
        String noteForHistory;
        if ("APPROVE".equalsIgnoreCase(decision.getDecision())) {
            newStatus = VerificationStatus.VERIFIED;
            verification.setRejectionReason(null);
            noteForHistory = "Duyệt hồ sơ: " + decision.getNote();
        } else if ("REJECT".equalsIgnoreCase(decision.getDecision())) {
            newStatus = VerificationStatus.REJECTED;
            verification.setRejectionReason(decision.getNote());
            noteForHistory = "Từ chối hồ sơ: " + decision.getNote();
        } else {
            throw new IllegalArgumentException("Decision must be APPROVE or REJECT");
        }
        verification.setStatus(newStatus);

        VerificationRequest saved = verificationRequestRepository.save(verification);
        recordHistory(saved, oldStatus, newStatus, admin, noteForHistory);
        syncProfileStatus(saved.getUser().getUserId(), newStatus);
        sendResultNotification(saved, newStatus);

        log.info("Verification reviewed: verificationId={}, adminId={}, decision={}",
                verificationId, adminId, decision.getDecision());

        return verificationMapper.toResponse(saved,
                verificationDocumentRepository.findByVerificationRequest_VerificationId(verificationId));
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
                        verificationDocumentRepository.findByVerificationRequest_VerificationId(v.getVerificationId())))
                .toList();
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
                               VerificationStatus newStatus, User changedBy, String note) {
        VerificationHistory history = new VerificationHistory();
        history.setVerificationRequest(request);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(changedBy);
        history.setNote(note);
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
}
