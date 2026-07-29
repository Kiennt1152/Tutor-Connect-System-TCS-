package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.PageAuditLogResponse;

import java.time.LocalDateTime;

public interface AuditLogService {
    void record(String action, String entityType, Long entityId, Object oldValue, Object newValue);
    PageAuditLogResponse search(Long actorId, String action, String entityType, LocalDateTime from, LocalDateTime to, int page, int size);
}
