package com.tcs.module.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.dto.response.AuditLogResponse;
import com.tcs.module.platform.dto.response.PageAuditLogResponse;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.security.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.ClientRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;
    private final ObjectMapper objectMapper;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final PlatformMapper platformMapper;

    @Override
    public void record(String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        Long userId = null;
        try {
            userId = authHelper.currentUserId();
        } catch (Exception e) {
            log.warn("Could not retrieve current user for audit log", e);
        }
        record(userId, action, entityType, entityId, oldValue, newValue);
    }

    @Override
    public void record(Long actorUserId, String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setCreatedAt(LocalDateTime.now());

        try {
            if (actorUserId != null) {
                userRepository.findById(actorUserId).ifPresent(auditLog::setActor);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve actor for audit log", e);
        }

        try {
            if (oldValue != null) {
                auditLog.setOldValue(objectMapper.writeValueAsString(oldValue));
            }
            if (newValue != null) {
                auditLog.setNewValue(objectMapper.writeValueAsString(newValue));
            }
        } catch (Exception e) {
            log.warn("Could not serialize audit log values", e);
        }

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(request.getRemoteAddr());
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.warn("Could not retrieve request attributes for audit log", e);
        }

        auditLogRepository.save(auditLog);
    }

    @Override
    public PageAuditLogResponse search(Long actorId, String actorRole, String action, String entityType,
            String keyword, LocalDateTime from, LocalDateTime to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        String normalizedRole = (actorRole == null || actorRole.isBlank()) ? null : actorRole.trim().toUpperCase();
        Page<AuditLog> auditLogs = auditLogRepository.search(
                actorId, action, entityType, normalizedKeyword, normalizedRole, from, to, pageable);

        List<AuditLogResponse> content = auditLogs.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageAuditLogResponse.builder()
                .content(content)
                .page(auditLogs.getNumber())
                .size(auditLogs.getSize())
                .totalElements(auditLogs.getTotalElements())
                .totalPages(auditLogs.getTotalPages())
                .build();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User actor = auditLog.getActor();
        String actorRole = null;
        
        if (actor != null) {
            UserProfileBundle profiles = loadProfiles(actor.getUserId());
            actorRole = platformMapper.resolveRole(profiles).name();
        }

        return AuditLogResponse.builder()
                .auditId(auditLog.getAuditId())
                .actorId(actor != null ? actor.getUserId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .actorRole(actorRole)
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null)
        );
    }
}
