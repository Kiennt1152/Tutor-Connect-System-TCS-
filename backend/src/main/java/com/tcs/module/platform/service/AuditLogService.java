package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.PageAuditLogResponse;

import java.time.LocalDateTime;

public interface AuditLogService {
    void record(String action, String entityType, Long entityId, Object oldValue, Object newValue);

    /**
     * Ghi log voi actor duoc chi dinh ro (dung cho cac hanh dong xay ra truoc khi
     * co JWT trong request, vi du: dang ky, dang nhap).
     */
    void record(Long actorUserId, String action, String entityType, Long entityId, Object oldValue, Object newValue);

    PageAuditLogResponse search(Long actorId, String actorRole, String action, String entityType, String keyword,
            LocalDateTime from, LocalDateTime to, int page, int size);
}
