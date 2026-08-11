package com.tcs.module.platform.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.entity.UserPenalty;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyService;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final UserPenaltyRepository userPenaltyRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;
    private final NotificationDispatchService notificationDispatchService;

    private static final Set<UserPenaltyType> BAN_TYPES = Set.of(
            UserPenaltyType.TEMPORARY_BAN, UserPenaltyType.PERMANENT_BAN);

    private PlatformAdmin currentAdminOrThrow() {
        Long adminUserId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        return platformAdminRepository.findByUser_UserId(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
    }

    /**
     * Runs periodically to auto-expire temporary bans whose expiresAt has passed,
     * restoring the user's status to ACTIVE when they have no other active penalty.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void expireOverduePenalties() {
        LocalDateTime now = LocalDateTime.now();
        List<UserPenalty> overdue = userPenaltyRepository.findByStatusAndExpiresAtBefore(UserPenaltyStatus.ACTIVE, now);
        if (overdue.isEmpty()) {
            return;
        }

        for (UserPenalty penalty : overdue) {
            penalty.setStatus(UserPenaltyStatus.EXPIRED);
            userPenaltyRepository.save(penalty);

            User user = penalty.getUser();
            if (!hasActiveBan(user.getUserId())) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
            }
        }
        log.info("Expired {} overdue user penalties", overdue.size());
    }

    @Override
    @Transactional
    public PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserPenalty> penaltyPage = userPenaltyRepository.search(userId, status, type, pageable);
        
        List<PenaltyResponse> content = penaltyPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagePenaltyResponse.builder()
                .content(content)
                .page(penaltyPage.getNumber())
                .size(penaltyPage.getSize())
                .totalElements(penaltyPage.getTotalElements())
                .totalPages(penaltyPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public PenaltyResponse issuePenalty(IssuePenaltyRequest request) {
        PlatformAdmin issuingAdmin = currentAdminOrThrow();
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (issuingAdmin.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Quản trị viên không thể tự áp dụng hình phạt.");
        }
        if (platformAdminRepository.findByUser_UserId(user.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Không thể áp dụng hình phạt cho tài khoản quản trị viên khác.");
        }

        UserPenaltyType penaltyType;
        try {
            penaltyType = UserPenaltyType.valueOf(request.getPenaltyType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại hình phạt không hợp lệ: " + request.getPenaltyType());
        }
        
        if (penaltyType == UserPenaltyType.TEMPORARY_BAN) {
            if (request.getExpiresAt() == null || request.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Thời gian hết hạn không hợp lệ cho cấm tạm thời");
            }
        }
        
        if (penaltyType == UserPenaltyType.PERMANENT_BAN) {
            request.setExpiresAt(null);
        }
        if (penaltyType == UserPenaltyType.FEATURE_RESTRICTION) {
            validateRestrictionCodes(request.getRestrictionDetails());
        }

        UserPenalty penalty = new UserPenalty();
        penalty.setUser(user);
        penalty.setIssuedBy(issuingAdmin);
        penalty.setPenaltyType(penaltyType);
        penalty.setReason(request.getReason());
        penalty.setEvidenceUrls(request.getEvidenceUrls());
        penalty.setRestrictionDetails(request.getRestrictionDetails());
        penalty.setStartsAt(LocalDateTime.now());
        penalty.setExpiresAt(request.getExpiresAt());
        penalty.setStatus(UserPenaltyStatus.ACTIVE);
        penalty.setCreatedAt(LocalDateTime.now());

        if (penaltyType == UserPenaltyType.TEMPORARY_BAN || penaltyType == UserPenaltyType.PERMANENT_BAN) {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
        }

        userPenaltyRepository.save(penalty);
        auditLogService.record("ISSUE_PENALTY", "UserPenalty", penalty.getPenaltyId(), null, request);
        notificationDispatchService.notifyUserFromTemplate(
                user, NotificationType.SYSTEM, "PENALTY_ISSUED",
                Map.of(
                        "penaltyType", penaltyType.name(),
                        "reason", request.getReason(),
                        "expiresAt", request.getExpiresAt() == null ? "Không thời hạn" : request.getExpiresAt()),
                "Tài khoản của bạn vừa nhận một hình phạt",
                "Loại: " + penaltyType.name() + ". Lý do: " + request.getReason(),
                "PENALTY", penalty.getPenaltyId());
        return toResponse(penalty);
    }

    @Override
    @Transactional
    public PenaltyResponse revokePenalty(Long penaltyId, RevokePenaltyRequest request) {
        UserPenalty penalty = userPenaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hình phạt"));

        if (penalty.getStatus() != UserPenaltyStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể thu hồi hình phạt đang hoạt động");
        }

        penalty.setStatus(UserPenaltyStatus.REVOKED);
        penalty.setRevokedAt(LocalDateTime.now());
        penalty.setRevokedReason(request.getRevokedReason());
        userPenaltyRepository.save(penalty);
        auditLogService.record("REVOKE_PENALTY", "UserPenalty", penalty.getPenaltyId(), null, request);

        User user = penalty.getUser();
        if (!hasActiveBan(user.getUserId())) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        return toResponse(penalty);
    }

    private boolean hasActiveBan(Long userId) {
        return userPenaltyRepository.existsByUser_UserIdAndStatusAndPenaltyTypeIn(
                userId, UserPenaltyStatus.ACTIVE, BAN_TYPES);
    }

    private void validateRestrictionCodes(String details) {
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Hạn chế tính năng phải có mã tính năng.");
        }
        Set<String> allowed = Set.of("MESSAGING", "CLASS_POSTING", "CLASS_APPLICATION", "WITHDRAWAL");
        String normalized = details.toUpperCase(java.util.Locale.ROOT).trim();
        if (!(normalized.startsWith("{") || normalized.startsWith("["))
                || allowed.stream().noneMatch(normalized::contains)) {
            throw new IllegalArgumentException("restrictionDetails phải là JSON chứa mã tính năng hợp lệ: " + allowed);
        }
    }

    private PenaltyResponse toResponse(UserPenalty penalty) {
        User user = penalty.getUser();
        String displayName = user.getEmail(); // Fallback to email as requested
        
        PlatformAdmin admin = penalty.getIssuedBy();
        String adminName = admin != null && admin.getFullName() != null ? admin.getFullName() : (admin != null && admin.getUser() != null ? admin.getUser().getEmail() : "Unknown");
        
        return PenaltyResponse.builder()
                .penaltyId(penalty.getPenaltyId())
                .userId(user.getUserId())
                .userEmail(user.getEmail())
                .userName(displayName)
                .penaltyType(penalty.getPenaltyType().name())
                .reason(penalty.getReason())
                .evidenceUrls(penalty.getEvidenceUrls())
                .restrictionDetails(penalty.getRestrictionDetails())
                .startsAt(penalty.getStartsAt())
                .expiresAt(penalty.getExpiresAt())
                .status(penalty.getStatus().name())
                .revokedAt(penalty.getRevokedAt())
                .revokedReason(penalty.getRevokedReason())
                .createdAt(penalty.getCreatedAt())
                .issuedByName(adminName)
                .build();
    }
}
